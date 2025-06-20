# AnalysisService
gRPC + mTSLで解析系サービスを提供  
管理機能 : Java Spring Bootサーバー + gRPCサーバー  
クライアント(サンプル) : Flutter (Web、Windowsなど)

AndroidにOpenCVをフルスペックで入れると  
ファイルサイズ200MB超えるので、  
gRPCサービスで解析処理を行うパターンを考慮

[環境]  
DB MariaDB(MySQL) 10.4.32  (GPL-2.0ライセンス問題対応ならPostgreSQL)
JavaSDK 17  
.Net 9 (C#)  
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

# 使用ライブラリライセンス情報 2025年6月20日更新

Android  
| ライブラリ名                          | バージョン   | ライセンス         | 参考URL                                                                 |
|-------------------------------------|-------------|--------------------|-------------------------------------------------------------------------|
| io.grpc:grpc-okhttp                 | 1.63.0      | Apache-2.0         | https://search.maven.org/artifact/io.grpc/grpc-okhttp/1.63.0            |
| io.grpc:grpc-protobuf               | 1.63.0      | Apache-2.0         | https://search.maven.org/artifact/io.grpc/grpc-protobuf/1.63.0          |
| io.grpc:grpc-stub                   | 1.63.0      | Apache-2.0         | https://search.maven.org/artifact/io.grpc/grpc-stub/1.63.0              |
| javax.annotation:javax.annotation-api| 1.3.2      | CDDL-1.1, GPL-2.0  | https://search.maven.org/artifact/javax.annotation/javax.annotation-api/1.3.2 |
| com.google.protobuf:protoc          | 3.25.3      | BSD-3-Clause       | https://search.maven.org/artifact/com.google.protobuf/protoc/3.25.3     |
| io.grpc:protoc-gen-grpc-java        | 1.63.0      | Apache-2.0         | https://search.maven.org/artifact/io.grpc/protoc-gen-grpc-java/1.63.0   |
| com.diffplug.spotless               | 6.25.0      | Apache-2.0         | https://github.com/diffplug/spotless/blob/main/LICENSE                  |
| dev.flutter.flutter-gradle-plugin   | -           | BSD-3-Clause       | https://github.com/flutter/flutter/blob/master/LICENSE                  |
| org.jetbrains.kotlin:kotlin-android | -           | Apache-2.0         | https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt     |

Flutter(Dart)  
| パッケージ名                                   | バージョン     | ライセンス        | 参考URL                                                              |
|-----------------------------------------------|---------------|------------------|---------------------------------------------------------------------|
| args                                         | 2.7.0         | BSD-3-Clause     | https://pub.dev/packages/args                                       |
| async                                        | 2.12.0        | BSD-3-Clause     | https://pub.dev/packages/async                                      |
| boolean_selector                             | 2.1.2         | BSD-3-Clause     | https://pub.dev/packages/boolean_selector                           |
| characters                                   | 1.4.0         | BSD-3-Clause     | https://pub.dev/packages/characters                                 |
| clock                                        | 1.1.2         | BSD-3-Clause     | https://pub.dev/packages/clock                                      |
| collection                                   | 1.19.1        | BSD-3-Clause     | https://pub.dev/packages/collection                                 |
| cross_file                                   | 0.3.4+2       | MIT              | https://pub.dev/packages/cross_file                                 |
| crypto                                       | 3.0.6         | BSD-3-Clause     | https://pub.dev/packages/crypto                                     |
| cupertino_icons                              | 1.0.8         | MIT              | https://pub.dev/packages/cupertino_icons                            |
| fake_async                                   | 1.3.2         | BSD-3-Clause     | https://pub.dev/packages/fake_async                                 |
| ffi                                          | 2.1.4         | BSD-3-Clause     | https://pub.dev/packages/ffi                                        |
| file                                         | 7.0.1         | MIT              | https://pub.dev/packages/file                                       |
| file_picker                                  | 10.1.9        | MIT              | https://pub.dev/packages/file_picker                                |
| fixnum                                       | 1.1.1         | BSD-3-Clause     | https://pub.dev/packages/fixnum                                     |
| flutter                                      | 0.0.0         | BSD-3-Clause     | https://github.com/flutter/flutter/blob/master/LICENSE              |
| flutter_lints                                | 5.0.0         | BSD-3-Clause     | https://pub.dev/packages/flutter_lints                              |
| flutter_plugin_android_lifecycle             | 2.0.28        | BSD-3-Clause     | https://pub.dev/packages/flutter_plugin_android_lifecycle           |
| flutter_test                                 | 0.0.0         | BSD-3-Clause     | https://github.com/flutter/flutter/blob/master/LICENSE              |
| flutter_web_plugins                          | 0.0.0         | BSD-3-Clause     | https://github.com/flutter/flutter/blob/master/LICENSE              |
| google_identity_services_web                 | 0.3.3+1       | BSD-3-Clause     | https://pub.dev/packages/google_identity_services_web               |
| googleapis_auth                              | 1.6.0         | BSD-3-Clause     | https://pub.dev/packages/googleapis_auth                            |
| grpc                                         | 4.0.4         | BSD-3-Clause     | https://pub.dev/packages/grpc                                       |
| http                                         | 1.4.0         | BSD-3-Clause     | https://pub.dev/packages/http                                       |
| http2                                        | 2.3.1         | BSD-3-Clause     | https://pub.dev/packages/http2                                      |
| http_parser                                  | 4.1.2         | BSD-3-Clause     | https://pub.dev/packages/http_parser                                |
| intl                                         | 0.20.2        | BSD-3-Clause     | https://pub.dev/packages/intl                                       |
| leak_tracker                                 | 10.0.8        | BSD-3-Clause     | https://pub.dev/packages/leak_tracker                               |
| leak_tracker_flutter_testing                 | 3.0.9         | BSD-3-Clause     | https://pub.dev/packages/leak_tracker_flutter_testing               |
| leak_tracker_testing                         | 3.0.1         | BSD-3-Clause     | https://pub.dev/packages/leak_tracker_testing                       |
| lints                                        | 5.1.1         | BSD-3-Clause     | https://pub.dev/packages/lints                                      |
| local_auth                                   | 2.3.0         | BSD-3-Clause     | https://pub.dev/packages/local_auth                                 |
| local_auth_android                           | 1.0.49        | BSD-3-Clause     | https://pub.dev/packages/local_auth_android                         |
| local_auth_darwin                            | 1.4.3         | BSD-3-Clause     | https://pub.dev/packages/local_auth_darwin                          |
| local_auth_platform_interface                | 1.0.10        | BSD-3-Clause     | https://pub.dev/packages/local_auth_platform_interface              |
| local_auth_windows                           | 1.0.11        | BSD-3-Clause     | https://pub.dev/packages/local_auth_windows                         |
| matcher                                      | 0.12.17       | BSD-3-Clause     | https://pub.dev/packages/matcher                                    |
| material_color_utilities                     | 0.11.1        | Apache-2.0       | https://pub.dev/packages/material_color_utilities                   |
| meta                                         | 1.16.0        | BSD-3-Clause     | https://pub.dev/packages/meta                                       |
| path                                         | 1.9.1         | BSD-3-Clause     | https://pub.dev/packages/path                                       |
| path_provider_linux                          | 2.2.1         | BSD-3-Clause     | https://pub.dev/packages/path_provider_linux                        |
| path_provider_platform_interface             | 2.1.2         | BSD-3-Clause     | https://pub.dev/packages/path_provider_platform_interface           |
| path_provider_windows                        | 2.3.0         | BSD-3-Clause     | https://pub.dev/packages/path_provider_windows                      |
| platform                                     | 3.1.6         | BSD-3-Clause     | https://pub.dev/packages/platform                                   |
| plugin_platform_interface                    | 2.1.8         | BSD-3-Clause     | https://pub.dev/packages/plugin_platform_interface                  |
| protobuf                                     | 4.1.0         | BSD-3-Clause     | https://pub.dev/packages/protobuf                                   |
| shared_preferences                           | 2.5.3         | BSD-3-Clause     | https://pub.dev/packages/shared_preferences                         |
| shared_preferences_android                   | 2.4.10        | BSD-3-Clause     | https://pub.dev/packages/shared_preferences_android                 |
| shared_preferences_foundation                | 2.5.4         | BSD-3-Clause     | https://pub.dev/packages/shared_preferences_foundation              |
| shared_preferences_linux                     | 2.4.1         | BSD-3-Clause     | https://pub.dev/packages/shared_preferences_linux                   |
| shared_preferences_platform_interface        | 2.4.1         | BSD-3-Clause     | https://pub.dev/packages/shared_preferences_platform_interface      |
| shared_preferences_web                       | 2.4.3         | BSD-3-Clause     | https://pub.dev/packages/shared_preferences_web                     |
| shared_preferences_windows                   | 2.4.1         | BSD-3-Clause     | https://pub.dev/packages/shared_preferences_windows                 |
| source_span                                  | 1.10.1        | BSD-3-Clause     | https://pub.dev/packages/source_span                                |
| stack_trace                                  | 1.12.1        | BSD-3-Clause     | https://pub.dev/packages/stack_trace                                |
| stream_channel                               | 2.1.4         | BSD-3-Clause     | https://pub.dev/packages/stream_channel                             |
| string_scanner                               | 1.4.1         | BSD-3-Clause     | https://pub.dev/packages/string_scanner                             |
| term_glyph                                   | 1.2.2         | BSD-3-Clause     | https://pub.dev/packages/term_glyph                                 |
| test_api                                     | 0.7.4         | BSD-3-Clause     | https://pub.dev/packages/test_api                                   |
| typed_data                                   | 1.4.0         | BSD-3-Clause     | https://pub.dev/packages/typed_data                                 |
| vector_math                                  | 2.1.4         | Apache-2.0       | https://pub.dev/packages/vector_math                                |
| vm_service                                   | 14.3.1        | BSD-3-Clause     | https://pub.dev/packages/vm_service                                 |
| web                                          | 1.1.1         | BSD-3-Clause     | https://pub.dev/packages/web                                        |
| win32                                        | 5.13.0        | MIT              | https://pub.dev/packages/win32                                      |
| xdg_directories                              | 1.1.0         | BSD-3-Clause     | https://pub.dev/packages/xdg_directories                            |

C#  
| パッケージ名                                 | バージョン | ライセンス      | 参考URL                                                                                 |
|---------------------------------------------|------------|----------------|----------------------------------------------------------------------------------------|
| Google.Protobuf                            | 3.31.1     | BSD-3-Clause   | https://www.nuget.org/packages/Google.Protobuf/3.31.1                                  |
| Grpc.Net.Client                            | 2.71.0     | Apache-2.0     | https://www.nuget.org/packages/Grpc.Net.Client/2.71.0                                  |
| Grpc.Tools                                 | 2.72.0     | Apache-2.0     | https://www.nuget.org/packages/Grpc.Tools/2.72.0                                       |
| System.Security.Cryptography.X509Certificates | 4.3.2   | MIT            | https://www.nuget.org/packages/System.Security.Cryptography.X509Certificates/4.3.2      |

Java Spring Boot  
| ライブラリ名                               | バージョン         | ライセンス            | 参考URL                                                                                                        |
|--------------------------------------------|--------------------|-----------------------|---------------------------------------------------------------------------------------------------------------|
| spring-boot-starter-data-jpa               | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-data-jpa/3.5.0/jar             |
| spring-boot-starter-oauth2-client          | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-oauth2-client/3.5.0/jar        |
| spring-boot-starter-security               | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-security/3.5.0/jar             |
| spring-boot-starter-thymeleaf              | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-thymeleaf/3.5.0/jar            |
| spring-boot-starter-web                    | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-web/3.5.0/jar                  |
| thymeleaf-extras-springsecurity6           | 3.1.2.RELEASE      | Apache-2.0            | https://search.maven.org/artifact/org.thymeleaf.extras/thymeleaf-extras-springsecurity6/3.1.2.RELEASE/jar     |
| spring-boot-devtools                       | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-devtools/3.5.0/jar                     |
| mysql-connector-j                          | 8.4.0              | GPL-2.0               | https://search.maven.org/artifact/mysql/mysql-connector-java/8.4.0/jar                                        |
| spring-boot-configuration-processor        | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-configuration-processor/3.5.0/jar      |
| spring-boot-starter-test                   | 3.5.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.boot/spring-boot-starter-test/3.5.0/jar                 |
| spring-security-test                       | 6.3.0              | Apache-2.0            | https://search.maven.org/artifact/org.springframework.security/spring-security-test/6.3.0/jar                  |
| grpc-spring-boot-starter                   | 3.0.0.RELEASE      | Apache-2.0            | https://search.maven.org/artifact/net.devh/grpc-spring-boot-starter/3.0.0.RELEASE/jar                         |
| javacv-platform                            | 1.5.10             | Apache-2.0            | https://search.maven.org/artifact/org.bytedeco/javacv-platform/1.5.10/jar                                     |
| opencv-platform                            | 4.9.0-1.5.10       | Apache-2.0            | https://search.maven.org/artifact/org.bytedeco/opencv-platform/4.9.0-1.5.10/jar                               |
| grpc-netty-shaded                          | 1.73.0             | Apache-2.0            | https://search.maven.org/artifact/io.grpc/grpc-netty-shaded/1.73.0/jar                                        |
| grpc-protobuf                              | 1.73.0             | Apache-2.0            | https://search.maven.org/artifact/io.grpc/grpc-protobuf/1.73.0/jar                                            |
| grpc-stub                                  | 1.73.0             | Apache-2.0            | https://search.maven.org/artifact/io.grpc/grpc-stub/1.73.0/jar                                                |
| protobuf-java                              | 3.25.3             | BSD-3-Clause          | https://search.maven.org/artifact/com.google.protobuf/protobuf-java/3.25.3/jar                                |
| protobuf-java-util                         | 3.25.3             | BSD-3-Clause          | https://search.maven.org/artifact/com.google.protobuf/protobuf-java-util/3.25.3/jar                           |
| javax.annotation-api                       | 1.3.2              | CDDL-1.1, GPL-2.0     | https://search.maven.org/artifact/javax.annotation/javax.annotation-api/1.3.2/jar                            |
| jakarta.annotation-api                     | 2.1.1              | EPL-2.0               | https://search.maven.org/artifact/jakarta.annotation/jakarta.annotation-api/2.1.1/jar                        |
| flyway-mysql                               | 10.14.0            | Apache-2.0            | https://search.maven.org/artifact/org.flywaydb/flyway-mysql/10.14.0/jar                                      |
| flyway-core                                | 10.14.0            | Apache-2.0            | https://search.maven.org/artifact/org.flywaydb/flyway-core/10.14.0/jar                                       |

本プロジェクトは、MySQL Connector/J 8.4.0（Copyright (c) 2008, 2024, Oracle and/or its affiliates）が  
GPL-2.0ライセンスで配布されているため、GPL-2.0ライセンスのもとで公開しています。  
MySQL Connector/J 8.4.0のライセンス詳細については、  
[MySQL Connector/J公式ページ](https://dev.mysql.com/downloads/connector/j/)や、  
配布物に含まれるLICENSEファイルをご参照ください。  
javax.annotation:javax.annotation-apiのGPL-2.0はClasspath Exception付きですが、  
mysql-connector-jがGPL-2.0のため、ライセンスを緩くするには、  
PostgreSQL Licenseにするなどの対応が必要です。  
一先ず、勉強用のプロジェクトなのでGPL-2.0とします。
