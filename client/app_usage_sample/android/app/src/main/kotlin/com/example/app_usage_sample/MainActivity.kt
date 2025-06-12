package com.example.app_usage_sample

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.io.InputStream
import java.nio.charset.Charset
import io.grpc.ManagedChannel
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import kotlinx.coroutines.*
import android.content.Context
import analysis.AnalysisServiceGrpc
import analysis.ServiceInfoServiceGrpc
import analysis.AnalysisServiceOuterClass
import org.json.JSONObject
import org.conscrypt.Conscrypt
import java.security.Security

class MainActivity : FlutterActivity() {
    private val CHANNEL = "mtls_grpc"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = handleGrpcRequest(call.method, call.arguments as? Map<String, Any> ?: emptyMap())
                    withContext(Dispatchers.Main) { result.success(response) }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { result.error("GRPC_ERROR", e.message, null) }
                }
            }
        }
    }

    private suspend fun handleGrpcRequest(method: String, params: Map<String, Any>): String {
        android.util.Log.d("mtls_grpc", "handleGrpcRequest called: method=$method, params=$params")
        val caFile = extractAssetToFile("flutter_assets/assets/ca.crt")
        val certFile = extractAssetToFile("flutter_assets/assets/client.crt")
        val keyFile = extractAssetToFile("flutter_assets/assets/client.key")
        val channel = createMtlsChannel("localhost", 9090, caFile, certFile, keyFile)
        return try {
            when (method) {
                "getAvailableServices" -> {
                    val stub = ServiceInfoServiceGrpc.newBlockingStub(channel)
                    val req = AnalysisServiceOuterClass.ServiceInfoRequest.newBuilder()
                        .setAccessKey(params["accessKey"] as? String ?: "")
                        .build()
                    val resp = stub.getAvailableServices(req)
                    channel.shutdown()
                    // JSON化
                    JSONObject().apply {
                        put("analysisTypes", org.json.JSONArray(resp.getAnalysisTypesList().map {
                            JSONObject().apply {
                                put("displayName", it.getDisplayName())
                                put("type", it.getType())
                            }
                        }))
                    }.toString()
                }
                "analyze" -> {
                    val stub = AnalysisServiceGrpc.newBlockingStub(channel)
                    val req = AnalysisServiceOuterClass.AnalysisRequest.newBuilder()
                        .setAccessKey(params["accessKey"] as? String ?: "")
                        .setAnalysisType(params["analysisType"] as? String ?: "")
                        .setTemplateName(params["templateName"] as? String ?: "")
                        .setImageBase64(params["imageBase64"] as? String ?: "")
                        .build()
                    val resp = stub.analyze(req)
                    channel.shutdown()
                    // JSON化
                    JSONObject().apply {
                        put("results", org.json.JSONArray(resp.getResultsList().map {
                            JSONObject().apply {
                                put("fileName", it.getFileName())
                                put("similarity", it.getSimilarity())
                                put("imageBase64", it.getImageBase64())
                            }
                        }))
                    }.toString()
                }
                else -> {
                    channel.shutdown()
                    JSONObject().apply {
                        put("error", "Unknown method: $method")
                    }.toString()
                }
            }
        } catch (e: Exception) {
            channel.shutdown()
            JSONObject().apply {
                put("error", e.message ?: "Unknown error")
            }.toString()
        }
    }

    private fun extractAssetToFile(assetName: String): File {
        val inputStream: InputStream = assets.open(assetName) // 現状、BOMが付いてしまう(元ファイルにはついてない)
        val file = File.createTempFile(assetName.replace("/", "_"), null, cacheDir)

        val bytes = inputStream.readBytes()
        android.util.Log.d("extractAssetToFile", "First 4 bytes: " + bytes.take(4).joinToString(" ") { "%02x".format(it) })
        
        // assets経由だとBOMが付く(原因不明)ため、リソースから取得してみる
        String path = "/ca.crt";
        val inputStream2: InputStream = getClass().getResourceAsStream(path)
        
        val bytes2 = inputStream2.readBytes()
        android.util.Log.d("getResourceAsStream", "First 4 bytes: " + bytes2.take(4).joinToString(" ") { "%02x".format(it) })

        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun createMtlsChannel(host: String, port: Int, caFile: File, certFile: File, keyFile: File): ManagedChannel {
        // Conscryptを有効化
        //Security.insertProviderAt(Conscrypt.newProvider(), 1)

        val bytes = caFile.readBytes()
        android.util.Log.d("createMtlsChannel", "First 4 bytes: " + bytes.take(4).joinToString(" ") { "%02x".format(it) })

        val sslContext = GrpcSslContexts.forClient()
            .trustManager(caFile)
            .keyManager(certFile, keyFile)
            .build()
        return NettyChannelBuilder.forAddress(host, port)
            .sslContext(sslContext)
            .build()
    }
}
