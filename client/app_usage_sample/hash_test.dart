// ignore_for_file: avoid_print
import 'dart:convert';
import 'package:crypto/crypto.dart';
import 'lib/app_config.dart';

void main() {
  print(
    '${AppConfig.defaultUsername} : ${sha256.convert(utf8.encode(AppConfig.defaultUsername)).toString()}',
  );
}
