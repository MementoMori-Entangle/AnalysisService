# gRPC/protobuf
-keep class io.grpc.** { *; }
-keep class com.google.protobuf.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
    <methods>;
}
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageV3 {
    <fields>;
    <methods>;
}
-keepclassmembers class * extends com.google.protobuf.MessageLite {
    <fields>;
    <methods>;
}
-keepclassmembers class * extends com.google.protobuf.Message {
    <fields>;
    <methods>;
}
-keepclassmembers class * extends io.grpc.stub.AbstractStub {
    <fields>;
    <methods>;
}
-keepclassmembers class * extends io.grpc.BindableService {
    <fields>;
    <methods>;
}
-keepattributes *Annotation*

# Kotlin (optional, for reflection)
-keep class kotlin.** { *; }
-keepclassmembers class kotlin.Metadata { *; }

# protobuf
-keep class com.google.protobuf.** { *; }
-keep class com.google.protobuf.nano.** { *; }
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}

# file_picker
-keep class com.mr.flutter.plugin.filepicker.** { *; }
-keep class com.nononsenseapps.filepicker.** { *; }
-keep class androidx.documentfile.provider.DocumentFile { *; }

# BouncyCastle, Conscrypt, Jetty, JBoss, sun.security.x509
-keep class org.bouncycastle.** { *; }
-keep class org.conscrypt.** { *; }
-keep class org.eclipse.jetty.** { *; }
-keep class org.jboss.marshalling.** { *; }
-keep class sun.security.x509.** { *; }
-dontwarn sun.security.x509.**
-dontwarn com.aayushatharva.brotli4j.**
-dontwarn com.github.luben.zstd.**
-dontwarn com.google.protobuf.nano.**
-dontwarn com.jcraft.jzlib.**
-dontwarn com.ning.compress.**
-dontwarn lzma.sdk.**
-dontwarn net.jpountz.lz4.**
-dontwarn net.jpountz.xxhash.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.conscrypt.**
-dontwarn org.eclipse.jetty.**
-dontwarn org.jboss.marshalling.**
-dontwarn reactor.blockhound.**
-dontwarn javax.naming.**
-dontwarn javax.naming.directory.**
-dontwarn com.oracle.svm.core.annotate.**
-dontwarn com.squareup.okhttp.**
