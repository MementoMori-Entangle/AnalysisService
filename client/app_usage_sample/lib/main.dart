import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:crypto/crypto.dart';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:local_auth/local_auth.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'app_config.dart';
import 'generated/analysis_service.pbgrpc.dart';
import 'grpc_client_io.dart' if (dart.library.html) 'grpc_client_web.dart';

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'クライアントサンプル',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
      home: const RootPage(),
    );
  }
}

class RootPage extends StatefulWidget {
  const RootPage({super.key});
  @override
  State<RootPage> createState() => _RootPageState();
}

class _RootPageState extends State<RootPage> {
  Future<bool> _isLoggedIn() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('username') != null;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<bool>(
      future: _isLoggedIn(),
      builder: (context, snapshot) {
        if (!snapshot.hasData) {
          return const Scaffold(
            body: Center(child: CircularProgressIndicator()),
          );
        }
        return snapshot.data!
            ? MyHomePage(title: AppConfig.appTitle)
            : const LoginPage();
      },
    );
  }
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});

  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final _formKey = GlobalKey<FormState>();
  final _usernameController = TextEditingController();
  final _passwordController = TextEditingController();
  bool _loading = false;
  String? _error;
  final LocalAuthentication _auth = LocalAuthentication();

  Future<void> _tryBiometricAuth() async {
    try {
      final didAuthenticate = await _auth.authenticate(
        localizedReason: 'ローカル認証でログイン',
        options: const AuthenticationOptions(
          biometricOnly: true,
          stickyAuth: true,
        ),
      );
      if (!mounted) return;
      if (didAuthenticate) {
        final prefs = await SharedPreferences.getInstance();
        final savedUser = prefs.getString('username');
        if (savedUser != null && mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(
              builder: (_) => MyHomePage(title: AppConfig.appTitle),
            ),
          );
        }
      }
    } catch (e) {
      // 生体認証失敗時は何もしない
    }
  }

  Future<void> _login() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    // 入力パスワードをSHA-256でハッシュ化（前後の空白を除去）
    final passwordHash =
        sha256.convert(utf8.encode(_passwordController.text.trim())).toString();
    final isDefaultUser =
        _usernameController.text.trim() == AppConfig.defaultUsername &&
        passwordHash == AppConfig.defaultPasswordHash;
    if (isDefaultUser) {
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('username', _usernameController.text);
      if (!mounted) return;
      Navigator.of(context).pushReplacement(
        MaterialPageRoute(
          builder: (_) => MyHomePage(title: AppConfig.appTitle),
        ),
      );
    } else {
      setState(() {
        _error = 'ユーザーIDまたはパスワードが正しくありません';
      });
    }
    setState(() {
      _loading = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('ログイン')),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextFormField(
                  controller: _usernameController,
                  decoration: const InputDecoration(labelText: 'ユーザー名'),
                  validator: (v) => v == null || v.isEmpty ? '必須項目です' : null,
                ),
                TextFormField(
                  controller: _passwordController,
                  decoration: const InputDecoration(labelText: 'パスワード'),
                  obscureText: true,
                  validator: (v) => v == null || v.isEmpty ? '必須項目です' : null,
                ),
                if (_error != null) ...[
                  const SizedBox(height: 12),
                  Text(_error!, style: const TextStyle(color: Colors.red)),
                ],
                const SizedBox(height: 24),
                ElevatedButton(
                  onPressed: _loading ? null : _login,
                  child:
                      _loading
                          ? const CircularProgressIndicator()
                          : const Text('ログイン'),
                ),
                const SizedBox(height: 16),
                // 生体認証ボタン（Windows以外でのみ表示）
                if (!kIsWeb && !Platform.isWindows)
                  FutureBuilder<bool>(
                    future: _auth.canCheckBiometrics,
                    builder: (context, snapshot) {
                      if (snapshot.data == true) {
                        return ElevatedButton.icon(
                          icon: const Icon(Icons.fingerprint),
                          label: const Text('生体認証でログイン'),
                          onPressed: _tryBiometricAuth,
                        );
                      } else {
                        return const SizedBox.shrink();
                      }
                    },
                  ),
                // WindowsのみPIN認証ボタンを「保存済みユーザー名がある場合のみ」表示
                if (!kIsWeb &&
                    Platform.isWindows &&
                    _usernameController.text.isNotEmpty)
                  ElevatedButton.icon(
                    icon: const Icon(Icons.lock),
                    label: const Text('PIN認証でログイン'),
                    onPressed: _tryPinAuth,
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  // PIN認証用メソッドを追加
  Future<void> _tryPinAuth() async {
    try {
      final didAuthenticate = await _auth.authenticate(
        localizedReason: 'PINでログイン',
        options: const AuthenticationOptions(
          biometricOnly: false, // PINやパスワードも許可
          stickyAuth: true,
        ),
      );
      if (didAuthenticate) {
        final prefs = await SharedPreferences.getInstance();
        final savedUser = prefs.getString('username');
        if (savedUser != null && mounted) {
          Navigator.of(context).pushReplacement(
            MaterialPageRoute(
              builder: (_) => MyHomePage(title: AppConfig.appTitle),
            ),
          );
        } else if (mounted) {
          setState(() {
            _error = 'PIN認証のみではログインできません。先にユーザー名とパスワードでログインしてください。';
          });
        }
      }
    } catch (e) {
      debugPrint('PIN認証エラー: $e');
    }
  }

  @override
  void initState() {
    super.initState();
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});
  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  String _serviceInfoResult = '';

  Future<void> _getServiceInfo() async {
    try {
      final request = {
        'method': AppConfig.methodGetAvailableServices,
        'accessKey': AppConfig.defaultAccessKey,
      };
      final responseJson = await grpcClient.sendRequest(request);
      final response = jsonDecode(responseJson);
      if (response is Map && response.containsKey('error')) {
        setState(() {
          _serviceInfoResult = 'Error: ${response['error']}';
        });
        return;
      }
      final names =
          (response['analysisTypes'] as List<dynamic>? ?? [])
              .map((t) => t['displayName'] as String)
              .toList();
      if (!mounted) return;
      setState(() {
        _serviceInfoResult =
            names.isNotEmpty ? 'サービス名一覧: ${names.join(", ")}' : 'サービス情報がありません';
      });
    } catch (e) {
      debugPrint('getAvailableServices error: $e');
      if (!mounted) return;
      setState(() {
        _serviceInfoResult = 'サービス名一覧の取得に失敗しました';
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Text(widget.title),
        actions: [
          IconButton(
            icon: const Icon(Icons.logout),
            tooltip: 'ログアウト',
            onPressed: () async {
              final prefs = await SharedPreferences.getInstance();
              await prefs.remove('username');
              if (!mounted) return;
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginPage()),
                (route) => false,
              );
            },
          ),
        ],
      ),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: <Widget>[
            ElevatedButton(
              onPressed: _getServiceInfo,
              child: const Text('サービス情報取得'),
            ),
            Text(_serviceInfoResult),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: () {
                Navigator.of(
                  context,
                ).push(MaterialPageRoute(builder: (_) => AnalysisTestScreen()));
              },
              child: const Text('解析サービステスト'),
            ),
          ],
        ),
      ),
    );
  }
}

class AnalysisTestScreen extends StatefulWidget {
  const AnalysisTestScreen({super.key});

  @override
  State<AnalysisTestScreen> createState() => _AnalysisTestScreenState();
}

class _AnalysisTestScreenState extends State<AnalysisTestScreen> {
  String? selectedService;
  String? selectedFilePath;
  List<String> services = [];
  List<MatchResult> analysisResults = [];
  String? errorMessage;
  bool isLoadingServices = false;
  bool isAnalyzing = false;

  // 画像ファイルのバイト列を保持（Web用）
  Uint8List? selectedFileBytes;

  @override
  void initState() {
    super.initState();
    _fetchServices(); // 画面初期化時にサービスリストを取得
  }

  Future<void> _fetchServices() async {
    setState(() {
      isLoadingServices = true;
      errorMessage = null;
    });
    try {
      final request = {
        'method': AppConfig.methodGetAvailableServices,
        'accessKey': AppConfig.defaultAccessKey,
      };
      final responseJson = await grpcClient.sendRequest(request);
      final response = jsonDecode(responseJson);
      if (response is Map && response.containsKey('error')) {
        setState(() {
          errorMessage = 'サービス情報の取得に失敗しました: ${response['error']}';
          services = [];
        });
        return;
      }
      final serviceList =
          (response['analysisTypes'] as List<dynamic>? ?? [])
              .map((t) => t['displayName'] as String)
              .toList();
      if (!mounted) return;
      setState(() {
        services = serviceList;
        if (services.isNotEmpty) {
          selectedService = services.first;
        }
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        errorMessage = 'サービス情報の取得に失敗しました';
        services = [];
      });
    }
    if (mounted) {
      setState(() {
        isLoadingServices = false;
      });
    }
  }

  void pickFile() async {
    FilePickerResult? result = await FilePicker.platform.pickFiles(
      type: FileType.image, // 画像ファイルのみを選択できるようにする
    );
    if (result != null) {
      setState(() {
        if (kIsWeb) {
          selectedFilePath = result.files.single.name;
          selectedFileBytes = result.files.single.bytes;
        } else {
          selectedFilePath = result.files.single.path;
          selectedFileBytes = null;
        }
        analysisResults = []; // 新しいファイルが選択されたら結果をクリア
        errorMessage = null; // エラーメッセージもクリア
      });
    }
  }

  Future<void> executeAnalysis() async {
    if (selectedService == null || selectedFilePath == null) {
      if (mounted) {
        setState(() {
          errorMessage = "サービスと画像ファイルを選択してください。";
        });
      }
      return;
    }
    if (mounted) {
      setState(() {
        isAnalyzing = true;
        analysisResults = [];
        errorMessage = null;
      });
    }
    try {
      Uint8List? imageBytes;
      if (kIsWeb) {
        imageBytes = selectedFileBytes;
      } else {
        final file = File(selectedFilePath!);
        imageBytes = await file.readAsBytes();
      }
      if (imageBytes == null) {
        setState(() {
          errorMessage = '画像データの取得に失敗しました';
        });
        return;
      }
      final imageBase64 = base64Encode(imageBytes);
      final request = {
        'method': AppConfig.methodAnalyze,
        'analysisType': selectedService,
        'imageBase64': imageBase64,
        'accessKey': AppConfig.defaultAccessKey,
        'analysisName': selectedService,
      };
      final responseJson = await grpcClient.sendRequest(request);
      final response = jsonDecode(responseJson);
      if (response is Map && response.containsKey('error')) {
        setState(() {
          errorMessage = '解析の実行に失敗しました: ${response['error']}';
        });
        return;
      }
      setState(() {
        analysisResults =
            (response['results'] as List<dynamic>? ?? [])
                .map(
                  (e) => MatchResult(
                    fileName: e['fileName']?.toString() ?? '',
                    similarity:
                        (e['similarity'] is num)
                            ? (e['similarity'] as num).toDouble()
                            : double.tryParse(
                                  e['similarity']?.toString() ?? '0',
                                ) ??
                                0.0,
                    imageBase64: e['imageBase64']?.toString() ?? '',
                  ),
                )
                .toList();
        if (analysisResults.isEmpty && (response['message'] ?? '').isNotEmpty) {
          errorMessage = response['message'];
        } else {
          errorMessage = null;
        }
      });
    } catch (e) {
      debugPrint('analyze error: $e');
      if (!mounted) return;
      setState(() {
        errorMessage = '解析の実行に失敗しました';
      });
    }
    if (mounted) {
      setState(() {
        isAnalyzing = false;
      });
    }
  }

  String getFileName(String path) {
    if (kIsWeb) {
      return path; // Webではファイル名そのもの
    } else {
      return path.split(Platform.pathSeparator).last;
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text("AnalysisServiceImpl テスト")),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            if (isLoadingServices)
              Center(child: CircularProgressIndicator())
            else if (services.isNotEmpty) ...[
              Text("解析種別を選択:", style: TextStyle(fontSize: 16)),
              DropdownButton<String>(
                value: selectedService,
                isExpanded: true,
                items:
                    services.map((service) {
                      return DropdownMenuItem(
                        value: service,
                        child: Text(service),
                      );
                    }).toList(),
                onChanged: (value) {
                  setState(() {
                    selectedService = value;
                    analysisResults = []; // 解析種別が変わったら結果をクリア
                    errorMessage = null;
                  });
                },
              ),
            ] else if (errorMessage != null && services.isEmpty) ...[
              Text(errorMessage!, style: TextStyle(color: Colors.red)),
              ElevatedButton(onPressed: _fetchServices, child: Text("サービス再取得")),
            ] else ...[
              Text("利用可能なサービスがありません。"),
              ElevatedButton(onPressed: _fetchServices, child: Text("サービス再取得")),
            ],
            SizedBox(height: 16),
            Text("画像ファイルを選択:", style: TextStyle(fontSize: 16)),
            ElevatedButton(
              onPressed: pickFile,
              child: Text(selectedFilePath ?? "ファイルを選択"),
            ),
            if (selectedFilePath != null) ...[
              SizedBox(height: 8),
              Text("選択中: ${getFileName(selectedFilePath!)}"),
            ],
            SizedBox(height: 16),
            ElevatedButton(
              onPressed:
                  (selectedService != null &&
                          selectedFilePath != null &&
                          !isAnalyzing)
                      ? executeAnalysis
                      : null,
              child:
                  isAnalyzing
                      ? CircularProgressIndicator(color: Colors.white)
                      : Text("解析実行"),
            ),
            if (errorMessage != null && !isAnalyzing) ...[
              SizedBox(height: 16),
              Text(errorMessage!, style: TextStyle(color: Colors.red)),
            ],
            SizedBox(height: 16),
            Text(
              "解析結果:",
              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
            ),
            if (isAnalyzing && analysisResults.isEmpty)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 16.0),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (analysisResults.isNotEmpty)
              Expanded(
                child: ListView.builder(
                  itemCount: analysisResults.length,
                  itemBuilder: (context, index) {
                    final result = analysisResults[index];
                    return Card(
                      margin: EdgeInsets.symmetric(vertical: 8.0),
                      child: Padding(
                        padding: const EdgeInsets.all(8.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            if (result.imageBase64.isNotEmpty)
                              Image.memory(
                                base64Decode(result.imageBase64),
                                height: 100,
                                fit: BoxFit.contain,
                              ),
                            Text("ファイル名: ${result.fileName}"),
                            Text(
                              "類似度: ${result.similarity.toStringAsFixed(4)}",
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
              )
            else if (!isAnalyzing && errorMessage == null)
              Text("解析結果はここに表示されます。"),
          ],
        ),
      ),
    );
  }
}

// gRPCクライアントのインターフェースのみ利用
late GrpcClient grpcClient;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  // 各ファイル側で適切なGrpcClientを返すようにする
  grpcClient = createGrpcClient();
  await grpcClient.start();
  runApp(const MyApp());
}
