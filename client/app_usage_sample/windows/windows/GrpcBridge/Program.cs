using Analysis;
using Grpc.Net.Client;
using System.IO.Pipes;
using System.Security.Cryptography.X509Certificates;
using System.Text;

// ログ出力用メソッド
static void WriteLog(string message)
{
    try
    {
        // imageBase64フィールドをマスク
        string masked = message;
        // JSON形式のimageBase64フィールドを検出し、長さのみ記録
        masked = System.Text.RegularExpressions.Regex.Replace(
            masked,
            "(\\\"imageBase64\\\"\\s*:\\s*\\\")(.*?)(\\\")",
            m => $"\\\"imageBase64\\\":\\\"[length={m.Groups[2].Value.Length}]\\\"");
        var logPath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "GrpcBridge.log");
        File.AppendAllText(logPath, $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss.fff}] {masked}\n");
    }
    catch { /* ignore logging errors */ }
}

// UTF-8エンコーディングを明示的に指定
Console.InputEncoding = Encoding.UTF8;
Console.OutputEncoding = Encoding.UTF8;

// テストモード: --test <accessKey> <grpcServerUrl> <assetsDir>
if (args.Length >= 4 && args[0] == "--test")
{
    string accessKey = args[1];
    string grpcServerUrl = args[2];
    string assetsDir = args[3];
    WriteLog($"[TEST] accessKey={accessKey}, grpcServerUrl={grpcServerUrl}, assetsDir={assetsDir}");
    var response = await CallGrpcWithMtlsAsync(accessKey, grpcServerUrl, assetsDir);
    WriteLog($"[TEST] response={response}");
    Console.Error.WriteLine($"[GrpcBridge] テストモード: gRPCサーバーにリクエスト送信 (AccessKey: {accessKey}, URL: {grpcServerUrl}, assets: {assetsDir})");
    Console.Error.WriteLine($"[GrpcBridge] レスポンス: {response}");
    return;
}

// 標準入出力モード: <grpcServerUrl> <assetsDir> --stdio
if (args.Length >= 3 && args[2] == "--stdio")
{
    string stdioGrpcUrl = args[0];
    string stdioAssetsDir = args[1];
    Console.Error.WriteLine($"[GrpcBridge] 標準入出力モード起動 (gRPCサーバー: {stdioGrpcUrl}, assets: {stdioAssetsDir})");
    WriteLog($"[STDIO] start grpcServerUrl={stdioGrpcUrl}, assetsDir={stdioAssetsDir}");
    while (true)
    {
        var request = await Console.In.ReadLineAsync();
        if (request == null) break; // EOF
        WriteLog($"[STDIO] 受信: {request}");
        var response = await CallGrpcWithMtlsAsync(request, stdioGrpcUrl, stdioAssetsDir);
        WriteLog($"[STDIO] 送信: {response}");
        // UTF-8でエンコードして標準出力に書き込む
        var utf8Bytes = Encoding.UTF8.GetBytes(response + "\n");
        await Console.OpenStandardOutput().WriteAsync(utf8Bytes);
        await Console.Out.FlushAsync();
    }
    return;
}

// 通常モード: <grpcServerUrl> <assetsDir> 必須
if (args.Length < 2)
{
    Console.Error.WriteLine("[GrpcBridge] エラー: gRPCサーバーのURLとassetsディレクトリを引数で指定してください。");
    Console.Error.WriteLine("使い方: GrpcBridge.exe <grpcServerUrl> <assetsDir>");
    Environment.Exit(1);
}
string pipeGrpcUrl = args[0];
string assetsDirPath = args[1];

Console.Error.WriteLine($"[GrpcBridge] NamedPipeサーバー起動 (gRPCサーバー: {pipeGrpcUrl}, assets: {assetsDirPath})");
WriteLog($"[PIPE] start grpcServerUrl={pipeGrpcUrl}, assetsDir={assetsDirPath}");
var pipeName = "FlutterGrpcPipe";
while (true)
{
    try
    {
        using var server = new NamedPipeServerStream(pipeName, PipeDirection.InOut, 1, PipeTransmissionMode.Byte, PipeOptions.Asynchronous);
        await server.WaitForConnectionAsync();
        Console.Error.WriteLine("[GrpcBridge] Flutterから接続あり");
        using var reader = new StreamReader(server, Encoding.UTF8, false, 4096, true);
        using var writer = new StreamWriter(server, Encoding.UTF8, 4096, true) { AutoFlush = true };
        var request = await reader.ReadLineAsync();
        WriteLog($"[PIPE] 受信: {request}");

        if (request == null) return;

        var response = await CallGrpcWithMtlsAsync(request, pipeGrpcUrl, assetsDirPath);
        WriteLog($"[PIPE] 送信: {response}");
        await writer.WriteLineAsync(response);
    }
    catch (IOException ex) when (ex.Message.Contains("すべてのパイプ インスタンスがビジー") || ex.Message.Contains("All pipe instances are busy"))
    {
        WriteLog($"[PIPE] パイプがビジー: {ex.Message}");
        Console.Error.WriteLine("[GrpcBridge] パイプがビジーです。少し待ってリトライします...");
        await Task.Delay(500); // 0.5秒待つ
    }
}

async Task<string> CallGrpcWithMtlsAsync(string request, string grpcUrl, string assetsDir)
{
    // 証明書パス
    string caCrtPath = Path.GetFullPath(Path.Combine(assetsDir, "ca.crt"));
    string clientCrtPath = Path.GetFullPath(Path.Combine(assetsDir, "client.crt"));
    string clientKeyPath = Path.GetFullPath(Path.Combine(assetsDir, "client.key"));

    // PEM→PFX変換
    var clientCert = PemToX509Certificate2(clientCrtPath, clientKeyPath);
    //var caCert = new X509Certificate2(caCrtPath);
    var caCert = X509CertificateLoader.LoadCertificateFromFile(caCrtPath);

    var handler = new HttpClientHandler();
    handler.ClientCertificates.Add(clientCert);
    handler.ServerCertificateCustomValidationCallback = (httpRequestMessage, cert, chain, errors) =>
    {
        bool isValid = false;

        if (chain == null || cert == null) return isValid;

        // CA証明書でサーバー検証
        chain.ChainPolicy.ExtraStore.Add(caCert);
        chain.ChainPolicy.VerificationFlags = X509VerificationFlags.AllowUnknownCertificateAuthority;
        chain.ChainPolicy.RevocationMode = X509RevocationMode.NoCheck;
        isValid = chain.Build(cert);

        return isValid;
    };

    var channel = GrpcChannel.ForAddress(grpcUrl, new GrpcChannelOptions { HttpHandler = handler });
    var serviceInfoClient = new ServiceInfoService.ServiceInfoServiceClient(channel);
    var analysisClient = new AnalysisService.AnalysisServiceClient(channel);
    try
    {
        using var doc = System.Text.Json.JsonDocument.Parse(request);
        var root = doc.RootElement;
        var method = root.TryGetProperty("method", out var m) ? m.GetString() : "getAvailableServices";
        if (method == "getAvailableServices")
        {
            var accessKey = root.TryGetProperty("accessKey", out var k) ? k.GetString() : "";
            var grpcRequest = new ServiceInfoRequest { AccessKey = accessKey ?? "" };
            var grpcResponse = await serviceInfoClient.GetAvailableServicesAsync(grpcRequest);
            return grpcResponse.ToString();
        }
        else if (method == "analyze")
        {
            var accessKey = root.TryGetProperty("accessKey", out var k) ? k.GetString() : "";
            var analysisType = root.TryGetProperty("analysisType", out var t) ? t.GetString() : "";
            var templateName = root.TryGetProperty("templateName", out var n) ? n.GetString() : "";
            var imageBase64 = root.TryGetProperty("imageBase64", out var i) ? i.GetString() : "";
            var analysisName = root.TryGetProperty("analysisName", out var j) ? j.GetString() : "";
            var grpcRequest = new AnalysisRequest
            {
                AccessKey = accessKey ?? "",
                AnalysisType = analysisType ?? "",
                TemplateName = templateName ?? "",
                ImageBase64 = imageBase64 ?? "",
                AnalysisName = analysisName ?? ""
            };
            var grpcResponse = await analysisClient.AnalyzeAsync(grpcRequest);
            return grpcResponse.ToString();
        }
        else
        {
            return $"gRPC error: Unknown method '{method}'";
        }
    }
    catch (Exception ex)
    {
        return $"gRPC error: {ex.Message}";
    }
}

// PEM証明書＋秘密鍵→X509Certificate2
X509Certificate2 PemToX509Certificate2(string certPath, string keyPath)
{
    var certPem = File.ReadAllText(certPath);
    var keyPem = File.ReadAllText(keyPath);
    return X509Certificate2.CreateFromPem(certPem, keyPem);
}
