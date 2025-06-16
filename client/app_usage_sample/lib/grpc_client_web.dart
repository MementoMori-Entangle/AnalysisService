import 'dart:convert';
import 'package:grpc/grpc_web.dart';
import 'app_config.dart';
import 'generated/analysis_service.pbgrpc.dart';

abstract class GrpcClient {
  Future<void> start();
  Future<String> sendRequest(Map<String, dynamic> request);
  Future<void> stop();
}

GrpcClient createGrpcClient() => GrpcWebMtlsClient();

class GrpcWebMtlsClient implements GrpcClient {
  final String grpcWebUrl = AppConfig.webGrpcUrl;
  late final GrpcWebClientChannel _channel;

  @override
  Future<void> start() async {
    _channel = GrpcWebClientChannel.xhr(Uri.parse(grpcWebUrl));
  }

  @override
  Future<String> sendRequest(Map<String, dynamic> params) async {
    final method =
        params['method'] as String? ?? AppConfig.methodGetAvailableServices;
    if (method == AppConfig.methodGetAvailableServices) {
      final stub = ServiceInfoServiceClient(_channel);
      final req =
          ServiceInfoRequest()
            ..accessKey = params['accessKey'] as String? ?? '';
      final resp = await stub.getAvailableServices(req);
      final types =
          resp.analysisTypes
              .map((t) => {'displayName': t.displayName, 'type': t.type})
              .toList();
      return '{"analysisTypes":${jsonEncode(types)}}';
    } else if (method == AppConfig.methodAnalyze) {
      final stub = AnalysisServiceClient(_channel);
      final req =
          AnalysisRequest()
            ..accessKey = params['accessKey'] as String? ?? ''
            ..analysisType = params['analysisType'] as String? ?? ''
            ..templateName = params['templateName'] as String? ?? ''
            ..imageBase64 = params['imageBase64'] as String? ?? ''
            ..analysisName = params['analysisType'] as String? ?? '';
      final resp = await stub.analyze(req);
      final results =
          resp.results
              .map(
                (e) => {
                  'fileName': e.fileName,
                  'similarity': e.similarity,
                  'imageBase64': e.imageBase64,
                },
              )
              .toList();
      return '{"results":${jsonEncode(results)}}';
    } else {
      return '{"error":"Unknown method: $method"}';
    }
  }

  @override
  Future<void> stop() async {
    await _channel.shutdown();
  }
}
