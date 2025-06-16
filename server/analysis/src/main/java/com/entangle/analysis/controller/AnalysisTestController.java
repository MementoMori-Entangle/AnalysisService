package com.entangle.analysis.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.SSLException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.entangle.analysis.entity.Log;
import com.entangle.analysis.grpc.AnalysisServiceImpl;
import com.entangle.analysis.repository.LogRepository;

import analysis.AnalysisServiceImageDivOuterClass;
import analysis.AnalysisServiceOuterClass;
import analysis.ImageDivServiceGrpc;
import analysis.ServiceInfoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/admin/analysis-test")
public class AnalysisTestController {
    @Autowired
    private AnalysisServiceImpl analysisServiceImpl;
    @Autowired
    private LogRepository logRepository;

    @Value("${analysis.test.default-access-key}")
    private String defaultAccessKey;

    @Autowired
    private MessageSource messageSource;

    @Autowired
    private ResourceLoader resourceLoader;

    @Value("${custom.grpc.client.ca-cert}")
    private String caCertPath;
    @Value("${custom.grpc.client.cert}")
    private String clientCertPath;
    @Value("${custom.grpc.client.key}")
    private String clientKeyPath;
    @Value("${custom.grpc.client.port}")
    private int grpcPort;
    @Value("${custom.grpc.client.max-inbound-message-size}")
    private int grpcClientMaxInboundMessageSize;

    @GetMapping("")
    public String showTestForm(Model model, HttpServletRequest request) {
        // gRPC経由でサービス情報取得（TLS/CA/クライアント証明書対応）
        Resource caCertResource = resourceLoader.getResource(caCertPath);
        Resource clientCertResource = resourceLoader.getResource(clientCertPath);
        Resource clientKeyResource = resourceLoader.getResource(clientKeyPath);
        ManagedChannel channel = null;
        List<AnalysisServiceOuterClass.AnalysisType> analysisTypes = new java.util.ArrayList<>();
        Locale locale = request.getLocale();
        try {
            channel = NettyChannelBuilder.forAddress("localhost", grpcPort)
                .maxInboundMessageSize(grpcClientMaxInboundMessageSize)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .trustManager(caCertResource.getFile())
                        .keyManager(clientCertResource.getFile(), clientKeyResource.getFile())
                        .build()
                )
                .build();
            ServiceInfoServiceGrpc.ServiceInfoServiceBlockingStub stub = ServiceInfoServiceGrpc.newBlockingStub(channel);
            AnalysisServiceOuterClass.ServiceInfoRequest req = AnalysisServiceOuterClass.ServiceInfoRequest.newBuilder()
                .setAccessKey(defaultAccessKey)
                .build();
            AnalysisServiceOuterClass.ServiceInfoResponse resp = stub.getAvailableServices(req);
            analysisTypes = resp.getAnalysisTypesList();
        } catch (SSLException e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch", null, locale) + ": " + e.getMessage();
            model.addAttribute("error", msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch", null, locale) + ": " + e.getMessage();
            model.addAttribute("error", msg);
        } finally {
            if (channel != null) channel.shutdown();
        }
        // 解析種別リストは「type: pattern-matching」「displayName: マッチングA」などが入っている
        // クライアントはdisplayName（例: マッチングA）を選択肢として表示し、analysisName値としても送信する
        model.addAttribute("analysisTypes", analysisTypes);
        model.addAttribute("accessKey", defaultAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen", null, request.getLocale()),
                  messageSource.getMessage("analysis.test.display.action", null, request.getLocale()));
        return "admin/analysis_test_form";
    }

    @PostMapping("")
    public String runAnalysis(@RequestParam("analysisName") String analysisName,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             @RequestParam(value = "accessKey", required = false) String accessKey,
                             Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        byte[] imageBytes = imageFile.getBytes();
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        int imageWidth = 0;
        int imageHeight = 0;
        Locale locale = request.getLocale();
        try {
            org.bytedeco.opencv.opencv_core.Mat buf = new org.bytedeco.opencv.opencv_core.Mat(new org.bytedeco.javacpp.BytePointer(imageBytes));
            org.bytedeco.opencv.opencv_core.Mat img = org.bytedeco.opencv.global.opencv_imgcodecs.imdecode(buf, org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR);
            imageWidth = img.cols();
            imageHeight = img.rows();
        } catch (Exception e) { /* ignore */ }
        double imageFileSizeMB = Math.round((imageFile.getSize() / 1024.0 / 1024.0) * 1000.0) / 1000.0;
        model.addAttribute("imageFileName", imageFile.getOriginalFilename());
        model.addAttribute("imageFileSize", imageFile.getSize());
        model.addAttribute("imageFileSizeMB", String.format("%.3f", imageFileSizeMB));
        model.addAttribute("imageWidth", imageWidth);
        model.addAttribute("imageHeight", imageHeight);
        model.addAttribute("imageBase64", imageBase64);
        // ここでanalysisTypeには「マッチングA」などのdisplayNameが入る
        AnalysisServiceOuterClass.AnalysisRequest req = AnalysisServiceOuterClass.AnalysisRequest.newBuilder()
                .setAnalysisName(analysisName)
                .setTemplateName("")
                .setImageBase64(imageBase64)
                .setAccessKey(useAccessKey)
                .build();
        final AnalysisServiceOuterClass.AnalysisResponse[] responseHolder = new AnalysisServiceOuterClass.AnalysisResponse[1];
        analysisServiceImpl.analyze(req, new io.grpc.stub.StreamObserver<>() {
            @Override public void onNext(AnalysisServiceOuterClass.AnalysisResponse value) {
                responseHolder[0] = value;
                if (value.getMessage() != null && !value.getMessage().isEmpty()) {
                    model.addAttribute("message", value.getMessage());
                }
            }
            @Override public void onError(Throwable t) {
                String msg = messageSource.getMessage("error.analysis.failed", null, locale) + ": " + t.getMessage();
                model.addAttribute("error", msg);
            }
            @Override public void onCompleted() { }
        });
        // 再度gRPC経由で解析種別リストを取得し直す（TLS/CA/クライアント証明書対応）
        Resource caCertResource = resourceLoader.getResource(caCertPath);
        Resource clientCertResource = resourceLoader.getResource(clientCertPath);
        Resource clientKeyResource = resourceLoader.getResource(clientKeyPath);
        ManagedChannel channel2 = null;
        List<AnalysisServiceOuterClass.AnalysisType> analysisTypes = new java.util.ArrayList<>();
        try {
            channel2 = NettyChannelBuilder.forAddress("localhost", grpcPort)
                .maxInboundMessageSize(grpcClientMaxInboundMessageSize)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .trustManager(caCertResource.getFile())
                        .keyManager(clientCertResource.getFile(), clientKeyResource.getFile())
                        .build()
                )
                .build();
            ServiceInfoServiceGrpc.ServiceInfoServiceBlockingStub stub2 = ServiceInfoServiceGrpc.newBlockingStub(channel2);
            AnalysisServiceOuterClass.ServiceInfoRequest infoReq = AnalysisServiceOuterClass.ServiceInfoRequest.newBuilder()
                .setAccessKey(useAccessKey)
                .build();
            AnalysisServiceOuterClass.ServiceInfoResponse resp = stub2.getAvailableServices(infoReq);
            analysisTypes = resp.getAnalysisTypesList();
        } catch (SSLException e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch", null, locale) + ": " + e.getMessage();
            model.addAttribute("error", msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch", null, locale) + ": " + e.getMessage();
            model.addAttribute("error", msg);
        } finally {
            if (channel2 != null) channel2.shutdown();
        }
        model.addAttribute("analysisTypes", analysisTypes);
        model.addAttribute("selectedAnalysisType", analysisName);
        model.addAttribute("results", responseHolder[0] != null ? responseHolder[0].getResultsList() : null);
        model.addAttribute("accessKey", useAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen", null, request.getLocale()),
                  messageSource.getMessage("analysis.test.run.action", new Object[]{analysisName, imageFile.getOriginalFilename()}, request.getLocale()));
        return "admin/analysis_test_form";
    }
    
    @PostMapping("/image-division")
    public String runImageDivision(@RequestParam("imageFile") MultipartFile imageFile,
                                  @RequestParam(value = "embedData", required = false) String embedData,
                                  @RequestParam(value = "embedDataType", required = false, defaultValue = "TEXT") String embedDataType,
                                  @RequestParam(value = "embedImageFile", required = false) MultipartFile embedImageFile,
                                  @RequestParam(value = "accessKey", required = false) String accessKey,
                                  @RequestParam("analysisName") String analysisName,
                                  Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        byte[] imageBytes = imageFile.getBytes();
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        String embedDataToSend = "";
        if ("IMAGE".equalsIgnoreCase(embedDataType) && embedImageFile != null && !embedImageFile.isEmpty()) {
            byte[] embedImageBytes = embedImageFile.getBytes();
            embedDataToSend = Base64.getEncoder().encodeToString(embedImageBytes);
        } else if (embedData != null) {
            embedDataToSend = embedData;
        }
        AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest.Builder reqBuilder = AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest.newBuilder()
                .setAccessKey(useAccessKey)
                .setOriginalImageBase64(imageBase64)
                .setEmbedData(embedDataToSend)
                .setAnalysisName(analysisName);
        if ("IMAGE".equalsIgnoreCase(embedDataType)) {
            reqBuilder.setEmbedDataType(AnalysisServiceImageDivOuterClass.EmbedDataType.IMAGE);
        } else {
            reqBuilder.setEmbedDataType(AnalysisServiceImageDivOuterClass.EmbedDataType.TEXT);
        }
        AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest req = reqBuilder.build();
        // gRPC証明書リソース取得
        Resource caCertResource = resourceLoader.getResource(caCertPath);
        Resource clientCertResource = resourceLoader.getResource(clientCertPath);
        Resource clientKeyResource = resourceLoader.getResource(clientKeyPath);
        // gRPC呼び出し
        ManagedChannel channel = null;
        AnalysisServiceImageDivOuterClass.ImageDivEmbedResponse response = null;
        try {
            channel = NettyChannelBuilder.forAddress("localhost", grpcPort)
                .maxInboundMessageSize(grpcClientMaxInboundMessageSize)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .trustManager(caCertResource.getFile())
                        .keyManager(clientCertResource.getFile(), clientKeyResource.getFile())
                        .build()
                )
                .build();
            ImageDivServiceGrpc.ImageDivServiceBlockingStub stub = ImageDivServiceGrpc.newBlockingStub(channel);
            response = stub.divideAndEmbed(req);
        } catch (io.grpc.StatusRuntimeException e) {
            model.addAttribute("errorMessage", e.getStatus().getDescription());
        } finally {
            if (channel != null) channel.shutdown();
        }
        model.addAttribute("dividedImages", response != null ? response.getDividedImagesList() : null);
        model.addAttribute("accessKey", useAccessKey);
        return "admin/analysis_test_image_division";
    }

    @PostMapping("/image-division-restore")
    public String runImageDivisionRestore(
            @RequestParam("dividedImages") List<MultipartFile> dividedImages,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @RequestParam("analysisName") String analysisName,
            Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        List<String> dividedImagesBase64 = new java.util.ArrayList<>();
        for (MultipartFile file : dividedImages) {
            dividedImagesBase64.add(Base64.getEncoder().encodeToString(file.getBytes()));
        }
        AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest req = AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest.newBuilder()
                .setAccessKey(useAccessKey)
                .addAllDividedImages(dividedImagesBase64)
                .setAnalysisName(analysisName)
                .build();
        // gRPC証明書リソース取得
        Resource caCertResource = resourceLoader.getResource(caCertPath);
        Resource clientCertResource = resourceLoader.getResource(clientCertPath);
        Resource clientKeyResource = resourceLoader.getResource(clientKeyPath);
        ManagedChannel channel = null;
        AnalysisServiceImageDivOuterClass.ImageDivRestoreResponse response = null;
        try {
            channel = NettyChannelBuilder.forAddress("localhost", grpcPort)
                .maxInboundMessageSize(grpcClientMaxInboundMessageSize)
                .sslContext(
                    GrpcSslContexts.forClient()
                        .trustManager(caCertResource.getFile())
                        .keyManager(clientCertResource.getFile(), clientKeyResource.getFile())
                        .build()
                )
                .build();
            ImageDivServiceGrpc.ImageDivServiceBlockingStub stub = ImageDivServiceGrpc.newBlockingStub(channel);
            response = stub.restoreEmbedData(req);
            model.addAttribute("restoredEmbedData", response != null ? response.getEmbedData() : null);
            model.addAttribute("restoredEmbedDataType", response != null ? response.getEmbedDataType() : null);
        } catch (io.grpc.StatusRuntimeException e) {
            model.addAttribute("errorMessage", e.getStatus().getDescription());
        } finally {
            if (channel != null) channel.shutdown();
        }
        return "admin/analysis_test_image_division_restore";
    }

    // ログ記録用メソッド
    private void logAction(HttpServletRequest request, String screen, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ? auth.getName() : "anonymous";
        String path = request.getRequestURI();
        Log log = new Log();
        log.setUsername(username);
        log.setPath(path);
        log.setScreen(screen);
        log.setAction(action);
        log.setIpAddress(request.getRemoteAddr());
        logRepository.save(log);
    }
}
