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

import 'dart:core' as $core;

import 'package:fixnum/fixnum.dart' as $fixnum;
import 'package:protobuf/protobuf.dart' as $pb;

export 'package:protobuf/protobuf.dart' show GeneratedMessageGenericExtensions;

/// サービス情報取得リクエスト
class ServiceInfoRequest extends $pb.GeneratedMessage {
  factory ServiceInfoRequest({
    $core.String? accessKey,
  }) {
    final result = create();
    if (accessKey != null) result.accessKey = accessKey;
    return result;
  }

  ServiceInfoRequest._();

  factory ServiceInfoRequest.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory ServiceInfoRequest.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'ServiceInfoRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'accessKey')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  ServiceInfoRequest clone() => ServiceInfoRequest()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  ServiceInfoRequest copyWith(void Function(ServiceInfoRequest) updates) => super.copyWith((message) => updates(message as ServiceInfoRequest)) as ServiceInfoRequest;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static ServiceInfoRequest create() => ServiceInfoRequest._();
  @$core.override
  ServiceInfoRequest createEmptyInstance() => create();
  static $pb.PbList<ServiceInfoRequest> createRepeated() => $pb.PbList<ServiceInfoRequest>();
  @$core.pragma('dart2js:noInline')
  static ServiceInfoRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<ServiceInfoRequest>(create);
  static ServiceInfoRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get accessKey => $_getSZ(0);
  @$pb.TagNumber(1)
  set accessKey($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasAccessKey() => $_has(0);
  @$pb.TagNumber(1)
  void clearAccessKey() => $_clearField(1);
}

/// サービス情報レスポンス
class ServiceInfoResponse extends $pb.GeneratedMessage {
  factory ServiceInfoResponse({
    $core.Iterable<AnalysisType>? analysisTypes,
  }) {
    final result = create();
    if (analysisTypes != null) result.analysisTypes.addAll(analysisTypes);
    return result;
  }

  ServiceInfoResponse._();

  factory ServiceInfoResponse.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory ServiceInfoResponse.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'ServiceInfoResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..pc<AnalysisType>(1, _omitFieldNames ? '' : 'analysisTypes', $pb.PbFieldType.PM, subBuilder: AnalysisType.create)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  ServiceInfoResponse clone() => ServiceInfoResponse()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  ServiceInfoResponse copyWith(void Function(ServiceInfoResponse) updates) => super.copyWith((message) => updates(message as ServiceInfoResponse)) as ServiceInfoResponse;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static ServiceInfoResponse create() => ServiceInfoResponse._();
  @$core.override
  ServiceInfoResponse createEmptyInstance() => create();
  static $pb.PbList<ServiceInfoResponse> createRepeated() => $pb.PbList<ServiceInfoResponse>();
  @$core.pragma('dart2js:noInline')
  static ServiceInfoResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<ServiceInfoResponse>(create);
  static ServiceInfoResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $pb.PbList<AnalysisType> get analysisTypes => $_getList(0);
}

/// 解析種類
class AnalysisType extends $pb.GeneratedMessage {
  factory AnalysisType({
    $core.String? type,
    $core.String? displayName,
    $core.Iterable<TemplateInfo>? templates,
  }) {
    final result = create();
    if (type != null) result.type = type;
    if (displayName != null) result.displayName = displayName;
    if (templates != null) result.templates.addAll(templates);
    return result;
  }

  AnalysisType._();

  factory AnalysisType.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory AnalysisType.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'AnalysisType', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'type')
    ..aOS(2, _omitFieldNames ? '' : 'displayName')
    ..pc<TemplateInfo>(3, _omitFieldNames ? '' : 'templates', $pb.PbFieldType.PM, subBuilder: TemplateInfo.create)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  AnalysisType clone() => AnalysisType()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  AnalysisType copyWith(void Function(AnalysisType) updates) => super.copyWith((message) => updates(message as AnalysisType)) as AnalysisType;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static AnalysisType create() => AnalysisType._();
  @$core.override
  AnalysisType createEmptyInstance() => create();
  static $pb.PbList<AnalysisType> createRepeated() => $pb.PbList<AnalysisType>();
  @$core.pragma('dart2js:noInline')
  static AnalysisType getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<AnalysisType>(create);
  static AnalysisType? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get type => $_getSZ(0);
  @$pb.TagNumber(1)
  set type($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasType() => $_has(0);
  @$pb.TagNumber(1)
  void clearType() => $_clearField(1);

  @$pb.TagNumber(2)
  $core.String get displayName => $_getSZ(1);
  @$pb.TagNumber(2)
  set displayName($core.String value) => $_setString(1, value);
  @$pb.TagNumber(2)
  $core.bool hasDisplayName() => $_has(1);
  @$pb.TagNumber(2)
  void clearDisplayName() => $_clearField(2);

  @$pb.TagNumber(3)
  $pb.PbList<TemplateInfo> get templates => $_getList(2);
}

/// テンプレート情報
class TemplateInfo extends $pb.GeneratedMessage {
  factory TemplateInfo({
    $core.String? templateName,
    $core.String? templateDir,
    $core.bool? enabled,
    $core.double? threshold,
  }) {
    final result = create();
    if (templateName != null) result.templateName = templateName;
    if (templateDir != null) result.templateDir = templateDir;
    if (enabled != null) result.enabled = enabled;
    if (threshold != null) result.threshold = threshold;
    return result;
  }

  TemplateInfo._();

  factory TemplateInfo.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory TemplateInfo.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'TemplateInfo', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'templateName')
    ..aOS(2, _omitFieldNames ? '' : 'templateDir')
    ..aOB(3, _omitFieldNames ? '' : 'enabled')
    ..a<$core.double>(4, _omitFieldNames ? '' : 'threshold', $pb.PbFieldType.OD)
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  TemplateInfo clone() => TemplateInfo()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  TemplateInfo copyWith(void Function(TemplateInfo) updates) => super.copyWith((message) => updates(message as TemplateInfo)) as TemplateInfo;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static TemplateInfo create() => TemplateInfo._();
  @$core.override
  TemplateInfo createEmptyInstance() => create();
  static $pb.PbList<TemplateInfo> createRepeated() => $pb.PbList<TemplateInfo>();
  @$core.pragma('dart2js:noInline')
  static TemplateInfo getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<TemplateInfo>(create);
  static TemplateInfo? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get templateName => $_getSZ(0);
  @$pb.TagNumber(1)
  set templateName($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasTemplateName() => $_has(0);
  @$pb.TagNumber(1)
  void clearTemplateName() => $_clearField(1);

  @$pb.TagNumber(2)
  $core.String get templateDir => $_getSZ(1);
  @$pb.TagNumber(2)
  set templateDir($core.String value) => $_setString(1, value);
  @$pb.TagNumber(2)
  $core.bool hasTemplateDir() => $_has(1);
  @$pb.TagNumber(2)
  void clearTemplateDir() => $_clearField(2);

  @$pb.TagNumber(3)
  $core.bool get enabled => $_getBF(2);
  @$pb.TagNumber(3)
  set enabled($core.bool value) => $_setBool(2, value);
  @$pb.TagNumber(3)
  $core.bool hasEnabled() => $_has(2);
  @$pb.TagNumber(3)
  void clearEnabled() => $_clearField(3);

  @$pb.TagNumber(4)
  $core.double get threshold => $_getN(3);
  @$pb.TagNumber(4)
  set threshold($core.double value) => $_setDouble(3, value);
  @$pb.TagNumber(4)
  $core.bool hasThreshold() => $_has(3);
  @$pb.TagNumber(4)
  void clearThreshold() => $_clearField(4);
}

/// 解析リクエスト
class AnalysisRequest extends $pb.GeneratedMessage {
  factory AnalysisRequest({
    $core.String? analysisType,
    $core.String? templateName,
    $core.String? imageBase64,
    $core.String? accessKey,
  }) {
    final result = create();
    if (analysisType != null) result.analysisType = analysisType;
    if (templateName != null) result.templateName = templateName;
    if (imageBase64 != null) result.imageBase64 = imageBase64;
    if (accessKey != null) result.accessKey = accessKey;
    return result;
  }

  AnalysisRequest._();

  factory AnalysisRequest.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory AnalysisRequest.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'AnalysisRequest', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'analysisType')
    ..aOS(2, _omitFieldNames ? '' : 'templateName')
    ..aOS(3, _omitFieldNames ? '' : 'imageBase64')
    ..aOS(4, _omitFieldNames ? '' : 'accessKey')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  AnalysisRequest clone() => AnalysisRequest()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  AnalysisRequest copyWith(void Function(AnalysisRequest) updates) => super.copyWith((message) => updates(message as AnalysisRequest)) as AnalysisRequest;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static AnalysisRequest create() => AnalysisRequest._();
  @$core.override
  AnalysisRequest createEmptyInstance() => create();
  static $pb.PbList<AnalysisRequest> createRepeated() => $pb.PbList<AnalysisRequest>();
  @$core.pragma('dart2js:noInline')
  static AnalysisRequest getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<AnalysisRequest>(create);
  static AnalysisRequest? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get analysisType => $_getSZ(0);
  @$pb.TagNumber(1)
  set analysisType($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasAnalysisType() => $_has(0);
  @$pb.TagNumber(1)
  void clearAnalysisType() => $_clearField(1);

  @$pb.TagNumber(2)
  $core.String get templateName => $_getSZ(1);
  @$pb.TagNumber(2)
  set templateName($core.String value) => $_setString(1, value);
  @$pb.TagNumber(2)
  $core.bool hasTemplateName() => $_has(1);
  @$pb.TagNumber(2)
  void clearTemplateName() => $_clearField(2);

  @$pb.TagNumber(3)
  $core.String get imageBase64 => $_getSZ(2);
  @$pb.TagNumber(3)
  set imageBase64($core.String value) => $_setString(2, value);
  @$pb.TagNumber(3)
  $core.bool hasImageBase64() => $_has(2);
  @$pb.TagNumber(3)
  void clearImageBase64() => $_clearField(3);

  @$pb.TagNumber(4)
  $core.String get accessKey => $_getSZ(3);
  @$pb.TagNumber(4)
  set accessKey($core.String value) => $_setString(3, value);
  @$pb.TagNumber(4)
  $core.bool hasAccessKey() => $_has(3);
  @$pb.TagNumber(4)
  void clearAccessKey() => $_clearField(4);
}

/// 解析レスポンス
class AnalysisResponse extends $pb.GeneratedMessage {
  factory AnalysisResponse({
    $core.String? analysisType,
    $core.String? templateName,
    $core.Iterable<MatchResult>? results,
    $core.String? message,
  }) {
    final result = create();
    if (analysisType != null) result.analysisType = analysisType;
    if (templateName != null) result.templateName = templateName;
    if (results != null) result.results.addAll(results);
    if (message != null) result.message = message;
    return result;
  }

  AnalysisResponse._();

  factory AnalysisResponse.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory AnalysisResponse.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'AnalysisResponse', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'analysisType')
    ..aOS(2, _omitFieldNames ? '' : 'templateName')
    ..pc<MatchResult>(3, _omitFieldNames ? '' : 'results', $pb.PbFieldType.PM, subBuilder: MatchResult.create)
    ..aOS(4, _omitFieldNames ? '' : 'message')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  AnalysisResponse clone() => AnalysisResponse()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  AnalysisResponse copyWith(void Function(AnalysisResponse) updates) => super.copyWith((message) => updates(message as AnalysisResponse)) as AnalysisResponse;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static AnalysisResponse create() => AnalysisResponse._();
  @$core.override
  AnalysisResponse createEmptyInstance() => create();
  static $pb.PbList<AnalysisResponse> createRepeated() => $pb.PbList<AnalysisResponse>();
  @$core.pragma('dart2js:noInline')
  static AnalysisResponse getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<AnalysisResponse>(create);
  static AnalysisResponse? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get analysisType => $_getSZ(0);
  @$pb.TagNumber(1)
  set analysisType($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasAnalysisType() => $_has(0);
  @$pb.TagNumber(1)
  void clearAnalysisType() => $_clearField(1);

  @$pb.TagNumber(2)
  $core.String get templateName => $_getSZ(1);
  @$pb.TagNumber(2)
  set templateName($core.String value) => $_setString(1, value);
  @$pb.TagNumber(2)
  $core.bool hasTemplateName() => $_has(1);
  @$pb.TagNumber(2)
  void clearTemplateName() => $_clearField(2);

  @$pb.TagNumber(3)
  $pb.PbList<MatchResult> get results => $_getList(2);

  @$pb.TagNumber(4)
  $core.String get message => $_getSZ(3);
  @$pb.TagNumber(4)
  set message($core.String value) => $_setString(3, value);
  @$pb.TagNumber(4)
  $core.bool hasMessage() => $_has(3);
  @$pb.TagNumber(4)
  void clearMessage() => $_clearField(4);
}

/// マッチング結果
class MatchResult extends $pb.GeneratedMessage {
  factory MatchResult({
    $core.String? fileName,
    $fixnum.Int64? fileSize,
    $core.int? width,
    $core.int? height,
    $core.double? similarity,
    $core.String? imageBase64,
  }) {
    final result = create();
    if (fileName != null) result.fileName = fileName;
    if (fileSize != null) result.fileSize = fileSize;
    if (width != null) result.width = width;
    if (height != null) result.height = height;
    if (similarity != null) result.similarity = similarity;
    if (imageBase64 != null) result.imageBase64 = imageBase64;
    return result;
  }

  MatchResult._();

  factory MatchResult.fromBuffer($core.List<$core.int> data, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromBuffer(data, registry);
  factory MatchResult.fromJson($core.String json, [$pb.ExtensionRegistry registry = $pb.ExtensionRegistry.EMPTY]) => create()..mergeFromJson(json, registry);

  static final $pb.BuilderInfo _i = $pb.BuilderInfo(_omitMessageNames ? '' : 'MatchResult', package: const $pb.PackageName(_omitMessageNames ? '' : 'analysis'), createEmptyInstance: create)
    ..aOS(1, _omitFieldNames ? '' : 'fileName')
    ..aInt64(2, _omitFieldNames ? '' : 'fileSize')
    ..a<$core.int>(3, _omitFieldNames ? '' : 'width', $pb.PbFieldType.O3)
    ..a<$core.int>(4, _omitFieldNames ? '' : 'height', $pb.PbFieldType.O3)
    ..a<$core.double>(5, _omitFieldNames ? '' : 'similarity', $pb.PbFieldType.OD)
    ..aOS(6, _omitFieldNames ? '' : 'imageBase64')
    ..hasRequiredFields = false
  ;

  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  MatchResult clone() => MatchResult()..mergeFromMessage(this);
  @$core.Deprecated('See https://github.com/google/protobuf.dart/issues/998.')
  MatchResult copyWith(void Function(MatchResult) updates) => super.copyWith((message) => updates(message as MatchResult)) as MatchResult;

  @$core.override
  $pb.BuilderInfo get info_ => _i;

  @$core.pragma('dart2js:noInline')
  static MatchResult create() => MatchResult._();
  @$core.override
  MatchResult createEmptyInstance() => create();
  static $pb.PbList<MatchResult> createRepeated() => $pb.PbList<MatchResult>();
  @$core.pragma('dart2js:noInline')
  static MatchResult getDefault() => _defaultInstance ??= $pb.GeneratedMessage.$_defaultFor<MatchResult>(create);
  static MatchResult? _defaultInstance;

  @$pb.TagNumber(1)
  $core.String get fileName => $_getSZ(0);
  @$pb.TagNumber(1)
  set fileName($core.String value) => $_setString(0, value);
  @$pb.TagNumber(1)
  $core.bool hasFileName() => $_has(0);
  @$pb.TagNumber(1)
  void clearFileName() => $_clearField(1);

  @$pb.TagNumber(2)
  $fixnum.Int64 get fileSize => $_getI64(1);
  @$pb.TagNumber(2)
  set fileSize($fixnum.Int64 value) => $_setInt64(1, value);
  @$pb.TagNumber(2)
  $core.bool hasFileSize() => $_has(1);
  @$pb.TagNumber(2)
  void clearFileSize() => $_clearField(2);

  @$pb.TagNumber(3)
  $core.int get width => $_getIZ(2);
  @$pb.TagNumber(3)
  set width($core.int value) => $_setSignedInt32(2, value);
  @$pb.TagNumber(3)
  $core.bool hasWidth() => $_has(2);
  @$pb.TagNumber(3)
  void clearWidth() => $_clearField(3);

  @$pb.TagNumber(4)
  $core.int get height => $_getIZ(3);
  @$pb.TagNumber(4)
  set height($core.int value) => $_setSignedInt32(3, value);
  @$pb.TagNumber(4)
  $core.bool hasHeight() => $_has(3);
  @$pb.TagNumber(4)
  void clearHeight() => $_clearField(4);

  @$pb.TagNumber(5)
  $core.double get similarity => $_getN(4);
  @$pb.TagNumber(5)
  set similarity($core.double value) => $_setDouble(4, value);
  @$pb.TagNumber(5)
  $core.bool hasSimilarity() => $_has(4);
  @$pb.TagNumber(5)
  void clearSimilarity() => $_clearField(5);

  @$pb.TagNumber(6)
  $core.String get imageBase64 => $_getSZ(5);
  @$pb.TagNumber(6)
  set imageBase64($core.String value) => $_setString(5, value);
  @$pb.TagNumber(6)
  $core.bool hasImageBase64() => $_has(5);
  @$pb.TagNumber(6)
  void clearImageBase64() => $_clearField(6);
}


const $core.bool _omitFieldNames = $core.bool.fromEnvironment('protobuf.omit_field_names');
const $core.bool _omitMessageNames = $core.bool.fromEnvironment('protobuf.omit_message_names');
