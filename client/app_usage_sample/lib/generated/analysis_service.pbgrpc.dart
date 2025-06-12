//
//  Generated code. Do not modify.
//  source: analysis_service.proto
//
// @dart = 3.3

// ignore_for_file: annotate_overrides, camel_case_types, comment_references
// ignore_for_file: constant_identifier_names
// ignore_for_file: curly_braces_in_flow_control_structures
// ignore_for_file: deprecated_member_use_from_same_package, library_prefixes
// ignore_for_file: non_constant_identifier_names

import 'dart:async' as $async;
import 'dart:core' as $core;

import 'package:grpc/service_api.dart' as $grpc;
import 'package:protobuf/protobuf.dart' as $pb;

import 'analysis_service.pb.dart' as $0;

export 'analysis_service.pb.dart';

/// サービス情報取得用
@$pb.GrpcServiceName('analysis.ServiceInfoService')
class ServiceInfoServiceClient extends $grpc.Client {
  /// The hostname for this service.
  static const $core.String defaultHost = '';

  /// OAuth scopes needed for the client.
  static const $core.List<$core.String> oauthScopes = [
    '',
  ];

  static final _$getAvailableServices = $grpc.ClientMethod<$0.ServiceInfoRequest, $0.ServiceInfoResponse>(
      '/analysis.ServiceInfoService/GetAvailableServices',
      ($0.ServiceInfoRequest value) => value.writeToBuffer(),
      ($core.List<$core.int> value) => $0.ServiceInfoResponse.fromBuffer(value));

  ServiceInfoServiceClient(super.channel, {super.options, super.interceptors});

  /// 利用可能な解析サービス情報を取得
  $grpc.ResponseFuture<$0.ServiceInfoResponse> getAvailableServices($0.ServiceInfoRequest request, {$grpc.CallOptions? options}) {
    return $createUnaryCall(_$getAvailableServices, request, options: options);
  }
}

@$pb.GrpcServiceName('analysis.ServiceInfoService')
abstract class ServiceInfoServiceBase extends $grpc.Service {
  $core.String get $name => 'analysis.ServiceInfoService';

  ServiceInfoServiceBase() {
    $addMethod($grpc.ServiceMethod<$0.ServiceInfoRequest, $0.ServiceInfoResponse>(
        'GetAvailableServices',
        getAvailableServices_Pre,
        false,
        false,
        ($core.List<$core.int> value) => $0.ServiceInfoRequest.fromBuffer(value),
        ($0.ServiceInfoResponse value) => value.writeToBuffer()));
  }

  $async.Future<$0.ServiceInfoResponse> getAvailableServices_Pre($grpc.ServiceCall $call, $async.Future<$0.ServiceInfoRequest> $request) async {
    return getAvailableServices($call, await $request);
  }

  $async.Future<$0.ServiceInfoResponse> getAvailableServices($grpc.ServiceCall call, $0.ServiceInfoRequest request);
}
/// 解析リクエスト用
@$pb.GrpcServiceName('analysis.AnalysisService')
class AnalysisServiceClient extends $grpc.Client {
  /// The hostname for this service.
  static const $core.String defaultHost = '';

  /// OAuth scopes needed for the client.
  static const $core.List<$core.String> oauthScopes = [
    '',
  ];

  static final _$analyze = $grpc.ClientMethod<$0.AnalysisRequest, $0.AnalysisResponse>(
      '/analysis.AnalysisService/Analyze',
      ($0.AnalysisRequest value) => value.writeToBuffer(),
      ($core.List<$core.int> value) => $0.AnalysisResponse.fromBuffer(value));

  AnalysisServiceClient(super.channel, {super.options, super.interceptors});

  /// 画像解析リクエスト
  $grpc.ResponseFuture<$0.AnalysisResponse> analyze($0.AnalysisRequest request, {$grpc.CallOptions? options}) {
    return $createUnaryCall(_$analyze, request, options: options);
  }
}

@$pb.GrpcServiceName('analysis.AnalysisService')
abstract class AnalysisServiceBase extends $grpc.Service {
  $core.String get $name => 'analysis.AnalysisService';

  AnalysisServiceBase() {
    $addMethod($grpc.ServiceMethod<$0.AnalysisRequest, $0.AnalysisResponse>(
        'Analyze',
        analyze_Pre,
        false,
        false,
        ($core.List<$core.int> value) => $0.AnalysisRequest.fromBuffer(value),
        ($0.AnalysisResponse value) => value.writeToBuffer()));
  }

  $async.Future<$0.AnalysisResponse> analyze_Pre($grpc.ServiceCall $call, $async.Future<$0.AnalysisRequest> $request) async {
    return analyze($call, await $request);
  }

  $async.Future<$0.AnalysisResponse> analyze($grpc.ServiceCall call, $0.AnalysisRequest request);
}
