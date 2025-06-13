package com.example.appusagesample

import analysis.AnalysisServiceGrpc
import analysis.AnalysisServiceOuterClass
import analysis.ServiceInfoServiceGrpc
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.KeyFactory
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class MainActivity : FlutterActivity() {
    private val channelName = "mtls_grpc"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, channelName).setMethodCallHandler { call, result ->
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

    private suspend fun handleGrpcRequest(
        method: String,
        params: Map<String, Any>,
    ): String {
        android.util.Log.d("mtls_grpc", "handleGrpcRequest called: method=$method, params=$params")
        val caFile = extractAssetToFile("flutter_assets/assets/ca.crt")
        val certFile = extractAssetToFile("flutter_assets/assets/client.crt")
        val keyFile = extractAssetToFile("flutter_assets/assets/client.key")
        val channel = createMtlsChannel("10.0.2.2", 9090, caFile, certFile, keyFile)
        return try {
            when (method) {
                "getAvailableServices" -> {
                    val stub = ServiceInfoServiceGrpc.newBlockingStub(channel)
                    val req =
                        AnalysisServiceOuterClass.ServiceInfoRequest.newBuilder()
                            .setAccessKey(params["accessKey"] as? String ?: "")
                            .build()
                    val resp = stub.getAvailableServices(req)
                    channel.shutdown()
                    // JSON化
                    JSONObject().apply {
                        put(
                            "analysisTypes",
                            org.json.JSONArray(
                                resp.getAnalysisTypesList().map {
                                    JSONObject().apply {
                                        put("displayName", it.getDisplayName())
                                        put("type", it.getType())
                                    }
                                },
                            ),
                        )
                    }.toString()
                }
                "analyze" -> {
                    val stub = AnalysisServiceGrpc.newBlockingStub(channel)
                    val req =
                        AnalysisServiceOuterClass.AnalysisRequest.newBuilder()
                            .setAccessKey(params["accessKey"] as? String ?: "")
                            .setAnalysisType(params["analysisType"] as? String ?: "")
                            .setTemplateName(params["templateName"] as? String ?: "")
                            .setImageBase64(params["imageBase64"] as? String ?: "")
                            .build()
                    val resp = stub.analyze(req)
                    channel.shutdown()
                    // JSON化
                    JSONObject().apply {
                        put(
                            "results",
                            org.json.JSONArray(
                                resp.getResultsList().map {
                                    JSONObject().apply {
                                        put("fileName", it.getFileName())
                                        put("similarity", it.getSimilarity())
                                        put("imageBase64", it.getImageBase64())
                                    }
                                },
                            ),
                        )
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
        val inputStream: InputStream = assets.open(assetName)
        val file = File.createTempFile(assetName.replace("/", "_"), null, cacheDir)

        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    private fun createMtlsChannel(
        host: String,
        port: Int,
        caFile: File,
        certFile: File,
        keyFile: File,
    ): ManagedChannel {
        // CA証明書ロード
        val cf = CertificateFactory.getInstance("X.509")
        val caInput = caFile.inputStream()
        val caCert = caInput.use { cf.generateCertificate(it) as X509Certificate }

        // クライアント証明書ロード
        val certInput = certFile.inputStream()
        val clientCert = certInput.use { cf.generateCertificate(it) as X509Certificate }

        // 秘密鍵ロード（PEM→DERデコード）
        val pem = keyFile.readText(Charsets.US_ASCII)
        val privateKeyPem =
            pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace("\\s".toRegex(), "")
        val keyBytes = java.util.Base64.getDecoder().decode(privateKeyPem)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey: PrivateKey = keyFactory.generatePrivate(keySpec)

        // キーストア作成
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStore.load(null, null)
        keyStore.setCertificateEntry("caCert", caCert)
        keyStore.setCertificateEntry("clientCert", clientCert)
        keyStore.setKeyEntry("privateKey", privateKey, null, arrayOf(clientCert))

        // TrustManager
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(keyStore)

        // KeyManager
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, null)

        // SSLContext
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(kmf.keyManagers, tmf.trustManagers, null)

        // OkHttpChannelBuilderでmTLS
        return OkHttpChannelBuilder.forAddress(host, port)
            .sslSocketFactory(sslContext.socketFactory)
            .build()
    }
}
