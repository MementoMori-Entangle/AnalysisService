package com.entangle.analysis.controller;

import com.entangle.analysis.AnalysisServiceImpl;
import com.entangle.analysis.repository.LogRepository;
import com.entangle.analysis.entity.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.List;
import java.util.Base64;
import java.io.IOException;
import analysis.ServiceInfoServiceGrpc;
import analysis.AnalysisServiceOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import java.util.Locale;
import javax.net.ssl.SSLException;

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
        // クライアントはdisplayName（例: マッチングA）を選択肢として表示し、値としても送信する
        model.addAttribute("analysisTypes", analysisTypes);
        model.addAttribute("accessKey", defaultAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen", null, request.getLocale()),
                  messageSource.getMessage("analysis.test.display.action", null, request.getLocale()));
        return "admin/analysis_test_form";
    }

    @PostMapping("")
    public String runAnalysis(@RequestParam("analysisType") String analysisType,
                             @RequestParam("imageFile") MultipartFile imageFile,
                             @RequestParam(value = "accessKey", required = false) String accessKey,
                             Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        String selectedType = analysisType;
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
                .setAnalysisType(selectedType) // displayNameをそのまま送信
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
        model.addAttribute("selectedAnalysisType", selectedType);
        model.addAttribute("results", responseHolder[0] != null ? responseHolder[0].getResultsList() : null);
        model.addAttribute("accessKey", useAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen", null, request.getLocale()),
                  messageSource.getMessage("analysis.test.run.action", new Object[]{selectedType, imageFile.getOriginalFilename()}, request.getLocale()));
        return "admin/analysis_test_form";
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
