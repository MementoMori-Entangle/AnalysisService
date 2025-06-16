# AnalysisService
gRPC + mTSLで解析系サービスを提供  
管理機能 : Java Spring Bootサーバー + gRPCサーバー  
クライアント(サンプル) : Flutter (Web、Windowsなど)

AndroidにOpenCVをフルスペックで入れると  
ファイルサイズ200MB超えるので、  
gRPCサービスで解析処理を行うパターンを考慮

[環境]  
DB MariaDB(MySQL) 10.4.32  
JavaSDK 17  
Flutter 3.29.3  
Spring Boot 3.5.0 (Project:Maven、Language:Java、Packaging:jar)

# 全プラットホーム共通問題
flutter + gRPCクライアントからサーバー Java Spring Boot + gRPCにmTLSで  
送受信するためには、公式がサポートしていないため、  
プラットホームに合わせて個別対応が必要です。

# Windows版 mTLS対応
Windowsネイティブアプリでブリッジする必要があります。  
C#.netでブリッジアプリを作成しました。(.netなのでLinux版でも流用可能)  
\AnalysisService\client\app_usage_sample\windows\windows\GrpcBridge\GrpcBridge.sln

NamedPipeモード  
C:\workspace\AnalysisService\client\app_usage_sample\windows\windows\GrpcBridge\bin\Release\net9.0  
GrpcBridge.exe https://localhost:9090 C:\workspace\AnalysisService\client\app_usage_sample\assets

標準入出力モード  
GrpcBridge.exe https://localhost:9090 C:\workspace\AnalysisService\client\app_usage_sample\assets --stdio

# Android版 mTLS対応
Platform Channelを使用してandroid(kotlin)側でgRPCを処理するようにしました。  
mTLSを使用する場合、2025年6月12日時点では、Nettyではなく  
gRPC-OkHttpを使用するしかないようです。(Nettyで対応する場合は、ソース修正ビルド必要?)  
Androidエミュレータからは「10.0.2.2」でアクセスするため、証明書のSANに「IP.1 = 10.0.2.2」が必須です。  
これを追加しないと、TLSハンドシェイク時に「証明書のホスト名が一致しない」ため接続できません。

# Web版 mTLS対応
2025年6月13日の時点では、一般的なブラウザはmTLSに対応していません。  
そのため、プロキシブリッジで対応することになります。  
今回は「grpcwebproxy」で対応しました。  
[Flutter Web] --gRPC-Web/HTTP1.1--> [grpcwebproxy:8080] --gRPC/HTTP2--> [Spring Boot gRPC:9090]

テスト環境での起動パラメーター例  
-- http  
Start-Process -NoNewWindow -FilePath "C:\tools\grpcwebproxy-v0.15.0-win64.exe" -WorkingDirectory "C:\tools" -ArgumentList @(  
  "--backend_addr=localhost:9090",  
  "--backend_tls=true",  
  "--backend_tls_ca_files=C:\workspace\AnalysisService\server\analysis\src\main\resources\ca.crt",  
  "--run_tls_server=false",  
  "--allow_all_origins"  
)

-- https  
Start-Process -NoNewWindow -FilePath "C:\tools\grpcwebproxy-v0.15.0-win64.exe" -WorkingDirectory "C:\tools" -ArgumentList @(  
  "--backend_addr=localhost:9090",  
  "--backend_tls=true",  
  "--backend_tls_ca_files=C:\workspace\AnalysisService\server\analysis\src\main\resources\ca.crt",  
  "--run_tls_server=true",  
  "--server_tls_cert_file=C:\workspace\AnalysisService\server\analysis\src\main\resources\client.crt",  
  "--server_tls_key_file=C:\workspace\AnalysisService\server\analysis\src\main\resources\client.key",  
  "--allow_all_origins",  
  "--server_http_tls_port=9443"  
)

"--allow_all_origins"は本番環境では、"--allow_origin=https://xxx.xxx.com"  
のように指定すること

# Linux版 mTLS対応
Windows版で作成したブリッジアプリで対応  
Linux x64向けにself-containedでビルド  
dotnet publish -c Release -r linux-x64 --self-contained true -p:PublishSingleFile=true  

注意  
NamedPipeServerStreamはLinuxではUnixドメインソケットとして動作しますが、  
Windowsと完全な互換性はありませんので、標準入出力モードで対応するのがベスト

