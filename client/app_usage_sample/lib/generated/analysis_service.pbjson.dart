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

import 'dart:convert' as $convert;
import 'dart:core' as $core;
import 'dart:typed_data' as $typed_data;

@$core.Deprecated('Use serviceInfoRequestDescriptor instead')
const ServiceInfoRequest$json = {
  '1': 'ServiceInfoRequest',
  '2': [
    {'1': 'access_key', '3': 1, '4': 1, '5': 9, '10': 'accessKey'},
  ],
};

/// Descriptor for `ServiceInfoRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List serviceInfoRequestDescriptor = $convert.base64Decode(
    'ChJTZXJ2aWNlSW5mb1JlcXVlc3QSHQoKYWNjZXNzX2tleRgBIAEoCVIJYWNjZXNzS2V5');

@$core.Deprecated('Use serviceInfoResponseDescriptor instead')
const ServiceInfoResponse$json = {
  '1': 'ServiceInfoResponse',
  '2': [
    {'1': 'analysis_types', '3': 1, '4': 3, '5': 11, '6': '.analysis.AnalysisType', '10': 'analysisTypes'},
  ],
};

/// Descriptor for `ServiceInfoResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List serviceInfoResponseDescriptor = $convert.base64Decode(
    'ChNTZXJ2aWNlSW5mb1Jlc3BvbnNlEj0KDmFuYWx5c2lzX3R5cGVzGAEgAygLMhYuYW5hbHlzaX'
    'MuQW5hbHlzaXNUeXBlUg1hbmFseXNpc1R5cGVz');

@$core.Deprecated('Use analysisTypeDescriptor instead')
const AnalysisType$json = {
  '1': 'AnalysisType',
  '2': [
    {'1': 'type', '3': 1, '4': 1, '5': 9, '10': 'type'},
    {'1': 'display_name', '3': 2, '4': 1, '5': 9, '10': 'displayName'},
    {'1': 'templates', '3': 3, '4': 3, '5': 11, '6': '.analysis.TemplateInfo', '10': 'templates'},
  ],
};

/// Descriptor for `AnalysisType`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List analysisTypeDescriptor = $convert.base64Decode(
    'CgxBbmFseXNpc1R5cGUSEgoEdHlwZRgBIAEoCVIEdHlwZRIhCgxkaXNwbGF5X25hbWUYAiABKA'
    'lSC2Rpc3BsYXlOYW1lEjQKCXRlbXBsYXRlcxgDIAMoCzIWLmFuYWx5c2lzLlRlbXBsYXRlSW5m'
    'b1IJdGVtcGxhdGVz');

@$core.Deprecated('Use templateInfoDescriptor instead')
const TemplateInfo$json = {
  '1': 'TemplateInfo',
  '2': [
    {'1': 'template_name', '3': 1, '4': 1, '5': 9, '10': 'templateName'},
    {'1': 'template_dir', '3': 2, '4': 1, '5': 9, '10': 'templateDir'},
    {'1': 'enabled', '3': 3, '4': 1, '5': 8, '10': 'enabled'},
    {'1': 'threshold', '3': 4, '4': 1, '5': 1, '10': 'threshold'},
  ],
};

/// Descriptor for `TemplateInfo`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List templateInfoDescriptor = $convert.base64Decode(
    'CgxUZW1wbGF0ZUluZm8SIwoNdGVtcGxhdGVfbmFtZRgBIAEoCVIMdGVtcGxhdGVOYW1lEiEKDH'
    'RlbXBsYXRlX2RpchgCIAEoCVILdGVtcGxhdGVEaXISGAoHZW5hYmxlZBgDIAEoCFIHZW5hYmxl'
    'ZBIcCgl0aHJlc2hvbGQYBCABKAFSCXRocmVzaG9sZA==');

@$core.Deprecated('Use analysisRequestDescriptor instead')
const AnalysisRequest$json = {
  '1': 'AnalysisRequest',
  '2': [
    {'1': 'analysis_type', '3': 1, '4': 1, '5': 9, '10': 'analysisType'},
    {'1': 'template_name', '3': 2, '4': 1, '5': 9, '10': 'templateName'},
    {'1': 'image_base64', '3': 3, '4': 1, '5': 9, '10': 'imageBase64'},
    {'1': 'access_key', '3': 4, '4': 1, '5': 9, '10': 'accessKey'},
    {'1': 'analysis_name', '3': 5, '4': 1, '5': 9, '10': 'analysisName'},
  ],
};

/// Descriptor for `AnalysisRequest`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List analysisRequestDescriptor = $convert.base64Decode(
    'Cg9BbmFseXNpc1JlcXVlc3QSIwoNYW5hbHlzaXNfdHlwZRgBIAEoCVIMYW5hbHlzaXNUeXBlEi'
    'MKDXRlbXBsYXRlX25hbWUYAiABKAlSDHRlbXBsYXRlTmFtZRIhCgxpbWFnZV9iYXNlNjQYAyAB'
    'KAlSC2ltYWdlQmFzZTY0Eh0KCmFjY2Vzc19rZXkYBCABKAlSCWFjY2Vzc0tleRIjCg1hbmFseX'
    'Npc19uYW1lGAUgASgJUgxhbmFseXNpc05hbWU=');

@$core.Deprecated('Use analysisResponseDescriptor instead')
const AnalysisResponse$json = {
  '1': 'AnalysisResponse',
  '2': [
    {'1': 'analysis_type', '3': 1, '4': 1, '5': 9, '10': 'analysisType'},
    {'1': 'template_name', '3': 2, '4': 1, '5': 9, '10': 'templateName'},
    {'1': 'results', '3': 3, '4': 3, '5': 11, '6': '.analysis.MatchResult', '10': 'results'},
    {'1': 'message', '3': 4, '4': 1, '5': 9, '10': 'message'},
  ],
};

/// Descriptor for `AnalysisResponse`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List analysisResponseDescriptor = $convert.base64Decode(
    'ChBBbmFseXNpc1Jlc3BvbnNlEiMKDWFuYWx5c2lzX3R5cGUYASABKAlSDGFuYWx5c2lzVHlwZR'
    'IjCg10ZW1wbGF0ZV9uYW1lGAIgASgJUgx0ZW1wbGF0ZU5hbWUSLwoHcmVzdWx0cxgDIAMoCzIV'
    'LmFuYWx5c2lzLk1hdGNoUmVzdWx0UgdyZXN1bHRzEhgKB21lc3NhZ2UYBCABKAlSB21lc3NhZ2'
    'U=');

@$core.Deprecated('Use matchResultDescriptor instead')
const MatchResult$json = {
  '1': 'MatchResult',
  '2': [
    {'1': 'file_name', '3': 1, '4': 1, '5': 9, '10': 'fileName'},
    {'1': 'file_size', '3': 2, '4': 1, '5': 3, '10': 'fileSize'},
    {'1': 'width', '3': 3, '4': 1, '5': 5, '10': 'width'},
    {'1': 'height', '3': 4, '4': 1, '5': 5, '10': 'height'},
    {'1': 'similarity', '3': 5, '4': 1, '5': 1, '10': 'similarity'},
    {'1': 'image_base64', '3': 6, '4': 1, '5': 9, '10': 'imageBase64'},
  ],
};

/// Descriptor for `MatchResult`. Decode as a `google.protobuf.DescriptorProto`.
final $typed_data.Uint8List matchResultDescriptor = $convert.base64Decode(
    'CgtNYXRjaFJlc3VsdBIbCglmaWxlX25hbWUYASABKAlSCGZpbGVOYW1lEhsKCWZpbGVfc2l6ZR'
    'gCIAEoA1IIZmlsZVNpemUSFAoFd2lkdGgYAyABKAVSBXdpZHRoEhYKBmhlaWdodBgEIAEoBVIG'
    'aGVpZ2h0Eh4KCnNpbWlsYXJpdHkYBSABKAFSCnNpbWlsYXJpdHkSIQoMaW1hZ2VfYmFzZTY0GA'
    'YgASgJUgtpbWFnZUJhc2U2NA==');

