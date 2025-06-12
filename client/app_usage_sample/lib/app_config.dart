class AppConfig {
  // ユーザー認証情報
  static const String defaultUsername = 'admin';
  // sha256
  static const String defaultPasswordHash =
      '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918';

  // ブリッジ(Windows)用gRPCサーバーURL
  static const String bridgeGrpcUrl = 'https://localhost:9090';
  // Web用gRPCサーバーURL (プロキシー経由 http port 8080, https port 9443)
  static const String webGrpcUrl = 'https://localhost:9443';
  // 証明書ディレクトリ（assets）
  static const String bridgeCertDir =
      'C:/workspace/AnalysisService/client/app_usage_sample/assets';
  // gRPC アクセスキー (DBに登録されていること)
  static const String defaultAccessKey = 'E84joLUDh7N8liBZihxwyWOmqcfiGreH';
  // Windowsネイティブ mTLS対応 GrpcBridge.exeのパス
  static const String bridgeExePath =
      'windows/windows/GrpcBridge/bin/Release/net9.0/GrpcBridge.exe';

  // アプリタイトル
  static const String appTitle = 'App Demo';

  // gRPCメソッド名定数
  static const String methodGetAvailableServices = 'getAvailableServices';
  static const String methodAnalyze = 'analyze';
}
