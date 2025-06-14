import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'app_config.dart';

abstract class GrpcClient {
  Future<void> start();
  Future<String> sendRequest(Map<String, dynamic> request);
  Future<void> stop();
}

GrpcClient createGrpcClient() {
  if (defaultTargetPlatform == TargetPlatform.android) {
    return GrpcAndroidClient();
  } else if (defaultTargetPlatform == TargetPlatform.windows) {
    return GrpcBridgeClient(
      bridgeExePath: AppConfig.bridgeExePath,
      grpcUrl: AppConfig.bridgeGrpcUrl,
      certDir: AppConfig.bridgeWindowsCertDir,
    );
  } else if (Platform.isLinux) {
    return GrpcBridgeClient(
      bridgeExePath: AppConfig.linuxBridgeExePath,
      grpcUrl: AppConfig.bridgeGrpcUrl,
      certDir: AppConfig.bridgeLinuxCertDir,
    );
  } else {
    return GrpcDirectClient();
  }
}

class GrpcDirectClient implements GrpcClient {
  final String grpcUrl = AppConfig.bridgeGrpcUrl;
  final String certDir = AppConfig.bridgeLinuxCertDir;

  @override
  Future<void> start() async {}
  @override
  Future<String> sendRequest(Map<String, dynamic> params) async => '';
  @override
  Future<void> stop() async {}
}

class GrpcAndroidClient implements GrpcClient {
  static const MethodChannel _channel = MethodChannel('mtls_grpc');
  @override
  Future<void> start() async {}
  @override
  Future<String> sendRequest(Map<String, dynamic> params) async {
    final method =
        params['method'] as String? ?? AppConfig.methodGetAvailableServices;
    final result = await _channel.invokeMethod<String>(method, params);
    if (result == null) throw Exception('gRPCネイティブクライアントからnullレスポンス');
    return result;
  }

  @override
  Future<void> stop() async {}
}

class GrpcBridgeClient implements GrpcClient {
  final String bridgeExePath;
  final String grpcUrl;
  final String certDir;
  Process? _process;
  IOSink? _stdin;
  final List<_GrpcBridgeRequest> _requestQueue = [];
  bool _isProcessing = false;
  StreamSubscription<String>? _stdoutSubscription;
  GrpcBridgeClient({
    required this.bridgeExePath,
    required this.grpcUrl,
    required this.certDir,
  });
  @override
  Future<void> start() async {
    final absCertDir = Directory(certDir).absolute.path;
    final exeDir = File(bridgeExePath).parent.path;
    _process = await Process.start(
      bridgeExePath,
      [grpcUrl, absCertDir, '--stdio'],
      mode: ProcessStartMode.normal,
      workingDirectory: exeDir,
    );
    _stdin = _process!.stdin;
    _stdoutSubscription = _process!.stdout
        .transform(utf8.decoder)
        .transform(const LineSplitter())
        .listen(
          _handleStdoutLine,
          onError: (e) {
            if (_requestQueue.isNotEmpty) {
              _requestQueue.removeAt(0).completer.completeError(e);
            }
          },
        );
  }

  @override
  Future<String> sendRequest(Map<String, dynamic> request) async {
    final completer = Completer<String>();
    _requestQueue.add(_GrpcBridgeRequest(jsonEncode(request), completer));
    _processQueue();
    return completer.future;
  }

  void _processQueue() {
    if (_isProcessing || _requestQueue.isEmpty || _stdin == null) return;
    _isProcessing = true;
    final req = _requestQueue.first;
    _stdin!.writeln(req.requestJson);
    _stdin!.flush();
  }

  void _handleStdoutLine(String line) {
    if (_requestQueue.isEmpty) return;
    if (line.isNotEmpty && line.startsWith('{') && line.endsWith('}')) {
      final req = _requestQueue.removeAt(0);
      req.completer.complete(line);
      _isProcessing = false;
      if (_requestQueue.isNotEmpty) {
        _processQueue();
      }
    }
  }

  @override
  Future<void> stop() async {
    await _stdin?.close();
    await _stdoutSubscription?.cancel();
    _process?.kill();
  }
}

class _GrpcBridgeRequest {
  final String requestJson;
  final Completer<String> completer;
  _GrpcBridgeRequest(this.requestJson, this.completer);
}
