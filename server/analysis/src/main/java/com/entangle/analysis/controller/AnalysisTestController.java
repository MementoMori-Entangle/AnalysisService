package com.entangle.analysis.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.SSLException;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.opencv_core.Mat;
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
import com.google.protobuf.ByteString;

import analysis.AnalysisServiceImageDivOuterClass;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivEmbedResponse;
import analysis.AnalysisServiceOuterClass.AnalysisRequest;
import analysis.AnalysisServiceOuterClass.AnalysisResponse;
import analysis.AnalysisServiceOuterClass.AnalysisType;
import analysis.AnalysisServiceOuterClass.ServiceInfoRequest;
import analysis.AnalysisServiceOuterClass.ServiceInfoResponse;
import analysis.AnalysisServiceVideoSubliminalOuterClass.SubliminalDetected;
import analysis.AnalysisServiceVideoSubliminalOuterClass.SubliminalImageInsert;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalCheckRequest;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalCheckResponse;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalGenerateRequest;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalGenerateResponse;
import analysis.ImageDivServiceGrpc;
import analysis.ImageDivServiceGrpc.ImageDivServiceBlockingStub;
import analysis.ServiceInfoServiceGrpc;
import analysis.ServiceInfoServiceGrpc.ServiceInfoServiceBlockingStub;
import analysis.VideoSubliminalServiceGrpc;
import analysis.VideoSubliminalServiceGrpc.VideoSubliminalServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 解析テスト用
 */
@Controller
@RequestMapping("/admin/analysis-test")
public class AnalysisTestController {
    private static final String LOCALHOST = "localhost";
    private static final String IMAGE = "IMAGE";
    private static final String TEXT = "TEXT";
    private static final String VIDEO_SUBLIMINAL = "video-subliminal";
    private static final String IMAGE_DIVISION = "image-division";
    private static final String PATTERN_MATCHING = "pattern-matching";
    @Autowired
    private AnalysisServiceImpl analysisServiceImpl;
    @Autowired
    private LogRepository logRepository;
    @Autowired
    private MessageSource messageSource;
    @Autowired
    private ResourceLoader resourceLoader;
    @Value("${analysis.test.default-access-key}")
    private String defaultAccessKey;
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

    /**
     * 解析テスト画面を表示する
     * @param model モデル
     * @param request HTTPリクエスト
     * @return 解析テスト画面のテンプレート名
     */
    @GetMapping("")
    public String showTestForm(Model model, HttpServletRequest request) {
        List<AnalysisType> analysisTypes = new ArrayList<>();
        Locale locale = request.getLocale();
        try {
            analysisTypes = getAnalysisTypes(defaultAccessKey);
        } catch (SSLException e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch",
                                    null, locale);
            model.addAttribute("error", msg);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch",
                                    null, locale);
            model.addAttribute("error", msg);
        }
        // 解析種別リストは「type: pattern-matching」「displayName: マッチングA」などが入っている
        // クライアントはdisplayName（例: マッチングA）を選択肢として表示し、analysisName値としても送信する
        model.addAttribute("analysisTypes", analysisTypes);
        model.addAttribute("accessKey", defaultAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen",
                                        null, request.getLocale()),
                  messageSource.getMessage("analysis.test.display.action",
                            null, request.getLocale()));
        return "admin/analysis_test_form";
    }

    /**
     * 解析を実行する
     * @param analysisName 解析名
     * @param imageFile 画像ファイル
     * @param videoFile 動画ファイル
     * @param accessKey アクセスキー
     * @param model モデル
     * @param request HTTPリクエスト
     * @return 解析結果を表示するテンプレート名
     * @throws IOException 入出力例外
     */
    @PostMapping("")
    public String runAnalysis(@RequestParam("analysisName") String analysisName,
                        @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                        @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                        @RequestParam(value = "accessKey", required = false) String accessKey,
                        Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null 
                                && !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        Locale locale = request.getLocale();
        // 解析名→解析種別の逆引き
        List<AnalysisType> analysisTypes = new ArrayList<>();
        String analysisType = null;
        try {
            analysisTypes = getAnalysisTypes(useAccessKey);
            
            for (AnalysisType aType : analysisTypes) {
                if (aType.getDisplayName().equals(analysisName)) {
                    analysisType = aType.getType();
                    break;
                }
            }
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch",
                                    null, locale);
            model.addAttribute("error", msg);
        }
        // 解析種別で分岐
        if (VIDEO_SUBLIMINAL.equals(analysisType)) {
            // 動画サブミナルgRPC呼び出し
            if (videoFile == null || videoFile.isEmpty()) {
                String msg = messageSource.getMessage("error.video.file.not.selected",
                                    null, locale);
                model.addAttribute("error", msg);
                return "admin/analysis_test_form";
            }
            byte[] videoBytes = videoFile.getBytes();
            try {
                VideoSubliminalGenerateResponse resp = getVideoSubliminalGenerateResponse(
                        useAccessKey, analysisName, videoBytes);
                model.addAttribute("message", resp.getMessage());
                if (resp.getProcessedVideoFile() != null && resp.getProcessedVideoFile().size() > 0) {
                    String videoBase64 = Base64.getEncoder().encodeToString(
                                                resp.getProcessedVideoFile().toByteArray());
                    model.addAttribute("processedVideoBase64", videoBase64);
                }
            } catch (Exception e) {
                String msg = messageSource.getMessage("error.subliminal.generate",
                                    null, locale);
                model.addAttribute("error", msg);
            }
            model.addAttribute("analysisTypes", analysisTypes);
            model.addAttribute("selectedAnalysisType", analysisName);
            model.addAttribute("accessKey", useAccessKey);
            logAction(request, messageSource.getMessage("analysis.test.screen",
                                            null, request.getLocale()),
                      messageSource.getMessage("analysis.test.run.action",
                                        new Object[]{analysisName, videoFile.getOriginalFilename()},
                                                    request.getLocale()));
        } else if (IMAGE_DIVISION.equals(analysisType)) {
            // 画像分割gRPC呼び出し
            byte[] imageBytes = imageFile.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            int imageWidth = 0;
            int imageHeight = 0;
            try {
                Mat buf = new Mat(new BytePointer(imageBytes));
                Mat img = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR);
                imageWidth = img.cols();
                imageHeight = img.rows();
            } catch (Exception e) {
                String msg = messageSource.getMessage("error.image.decode",
                                        null, locale) + ": " + e.getMessage();
                model.addAttribute("error", msg);
                return "admin/analysis_test_form";
             }
            double imageFileSizeMB = Math.round(
                                        (imageFile.getSize() / 1024.0 / 1024.0) * 1000.0
                                    ) / 1000.0;
            model.addAttribute("imageFileName", imageFile.getOriginalFilename());
            model.addAttribute("imageFileSize", imageFile.getSize());
            model.addAttribute("imageFileSizeMB",
                                            String.format("%.3f", imageFileSizeMB));
            model.addAttribute("imageWidth", imageWidth);
            model.addAttribute("imageHeight", imageHeight);
            model.addAttribute("imageBase64", imageBase64);
            AnalysisRequest req = AnalysisRequest.newBuilder()
                                    .setAnalysisName(analysisName)
                                    .setTemplateName("")
                                    .setImageBase64(imageBase64)
                                    .setAccessKey(useAccessKey)
                                    .build();
            final AnalysisResponse[] responseHolder = new AnalysisResponse[1];
            analysisServiceImpl.analyze(req, new StreamObserver<>() {
                @Override public void onNext(AnalysisResponse value) {
                    responseHolder[0] = value;
                    if (value.getMessage() != null && !value.getMessage().isEmpty()) {
                        model.addAttribute("message", value.getMessage());
                    }
                }
                @Override public void onError(Throwable t) {
                    String msg = messageSource.getMessage("error.analysis.failed",
                                null, locale);
                    model.addAttribute("error", msg);
                }
                @Override public void onCompleted() { }
            }); 
            model.addAttribute("analysisTypes", analysisTypes);
            model.addAttribute("selectedAnalysisType", analysisName);
            model.addAttribute("results", responseHolder[0] != null ?
                                                        responseHolder[0].getResultsList() : null);
            model.addAttribute("accessKey", useAccessKey);
            logAction(request, messageSource.getMessage("analysis.test.screen",
                                            null, request.getLocale()),
                      messageSource.getMessage("analysis.test.run.action",
                                        new Object[]{analysisName, imageFile.getOriginalFilename()},
                                                        request.getLocale()));
        } else if (PATTERN_MATCHING.equals(analysisType)) {
            // 画像マッチングの処理
            byte[] imageBytes = imageFile.getBytes();
            String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
            int imageWidth = 0;
            int imageHeight = 0;
            try {
                Mat buf = new Mat(new BytePointer(imageBytes));
                Mat img = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR);
                imageWidth = img.cols();
                imageHeight = img.rows();
            } catch (Exception e) {
                String msg = messageSource.getMessage("error.image.decode",
                                        null, locale) + ": " + e.getMessage();
                model.addAttribute("error", msg);
                return "admin/analysis_test_form";
            }
            double imageFileSizeMB = Math.round(
                                        (imageFile.getSize() / 1024.0 / 1024.0) * 1000.0
                                    ) / 1000.0;
            model.addAttribute("imageFileName", imageFile.getOriginalFilename());
            model.addAttribute("imageFileSize", imageFile.getSize());
            model.addAttribute("imageFileSizeMB",
                                        String.format("%.3f", imageFileSizeMB));
            model.addAttribute("imageWidth", imageWidth);
            model.addAttribute("imageHeight", imageHeight);
            model.addAttribute("imageBase64", imageBase64);
            AnalysisRequest req = AnalysisRequest.newBuilder()
                                    .setAnalysisName(analysisName)
                                    .setTemplateName("")
                                    .setImageBase64(imageBase64)
                                    .setAccessKey(useAccessKey)
                                    .build();
            final AnalysisResponse[] responseHolder = new AnalysisResponse[1];
            analysisServiceImpl.analyze(req, new StreamObserver<>() {
                @Override public void onNext(AnalysisResponse value) {
                    responseHolder[0] = value;
                    if (value.getMessage() != null && !value.getMessage().isEmpty()) {
                        model.addAttribute("message", value.getMessage());
                    }
                }
                @Override public void onError(Throwable t) {
                    String msg = messageSource.getMessage("error.analysis.failed",
                                            null, locale);
                    model.addAttribute("error", msg);
                }
                @Override public void onCompleted() { }
            });
            model.addAttribute("analysisTypes", analysisTypes);
            model.addAttribute("selectedAnalysisType", analysisName);
            model.addAttribute("results", responseHolder[0] != null ?
                                                        responseHolder[0].getResultsList() : null);
            model.addAttribute("accessKey", useAccessKey);
            logAction(request, messageSource.getMessage("analysis.test.screen",
                                            null, request.getLocale()),
                      messageSource.getMessage("analysis.test.run.action",
                                    new Object[]{analysisName, imageFile.getOriginalFilename()},
                                                request.getLocale()));
        }
        return "admin/analysis_test_form";
    }
    
    /**
     * 画像分割を実行する
     * @param imageFile 画像ファイル
     * @param embedData 埋め込みデータ（テキストまたはBase64エンコードされた画像）
     * @param embedDataType 埋め込みデータのタイプ（TEXTまたはIMAGE）
     * @param embedImageFile 埋め込み画像ファイル（embedDataTypeがIMAGEの場合に使用）
     * @param accessKey アクセスキー
     * @param analysisName 解析名
     * @param model モデル
     * @param request HTTPリクエスト
     * @return 画像分割結果を表示するテンプレート名
     * @throws IOException 入出力例外
     */
    @PostMapping("/image-division")
    public String runImageDivision(
            @RequestParam("imageFile") MultipartFile imageFile,
            @RequestParam(value = "embedData", required = false) String embedData,
            @RequestParam(value = "embedDataType", required = false, defaultValue = "TEXT") String embedDataType,
            @RequestParam(value = "embedImageFile", required = false) MultipartFile embedImageFile,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @RequestParam("analysisName") String analysisName,
        Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null &&
                                !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        byte[] imageBytes = imageFile.getBytes();
        String imageBase64 = Base64.getEncoder().encodeToString(imageBytes);
        String embedDataToSend = "";
        if ("IMAGE".equalsIgnoreCase(embedDataType) &&
                        embedImageFile != null && !embedImageFile.isEmpty()) {
            byte[] embedImageBytes = embedImageFile.getBytes();
            embedDataToSend = Base64.getEncoder().encodeToString(embedImageBytes);
        } else if (embedData != null) {
            embedDataToSend = embedData;
        }
        ImageDivEmbedRequest.Builder reqBuilder = ImageDivEmbedRequest.newBuilder()
                                                    .setAccessKey(useAccessKey)
                                                    .setOriginalImageBase64(imageBase64)
                                                    .setEmbedData(embedDataToSend)
                                                    .setAnalysisName(analysisName);
        if (IMAGE.equalsIgnoreCase(embedDataType)) {
            reqBuilder.setEmbedDataType(AnalysisServiceImageDivOuterClass.EmbedDataType.IMAGE);
        } else if (TEXT.equalsIgnoreCase(embedDataType)) {
            reqBuilder.setEmbedDataType(AnalysisServiceImageDivOuterClass.EmbedDataType.TEXT);
        }
        ImageDivEmbedRequest req = reqBuilder.build();
        ManagedChannel channel = null;
        ImageDivEmbedResponse response = null;
        try {
            channel = createSecureChannel(LOCALHOST, grpcPort,
                                            grpcClientMaxInboundMessageSize);
            ImageDivServiceBlockingStub stub = ImageDivServiceGrpc.newBlockingStub(channel);
            response = stub.divideAndEmbed(req);
        } catch (io.grpc.StatusRuntimeException e) {
            model.addAttribute("errorMessage", e.getStatus().getDescription());
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        } finally {
            if (channel != null) channel.shutdown();
        }
        model.addAttribute("dividedImages", response != null ?
                                                        response.getDividedImagesList() : null);
        model.addAttribute("accessKey", useAccessKey);
        return "admin/analysis_test_image_division";
    }

    /**
     * 画像分割の復元を実行する
     * @param dividedImages 分割された画像のリスト
     * @param accessKey アクセスキー
     * @param analysisName 解析名
     * @param model モデル
     * @param request HTTPリクエスト
     * @return 画像分割復元結果を表示するテンプレート名
     * @throws IOException 入出力例外
     */
    @PostMapping("/image-division-restore")
    public String runImageDivisionRestore(
            @RequestParam("dividedImages") List<MultipartFile> dividedImages,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @RequestParam("analysisName") String analysisName,
            Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ?
                                                    accessKey : defaultAccessKey;
        List<String> dividedImagesBase64 = new ArrayList<>();
        for (MultipartFile file : dividedImages) {
            dividedImagesBase64.add(Base64.getEncoder().encodeToString(file.getBytes()));
        }
        AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest req =
                        AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest.newBuilder()
                .setAccessKey(useAccessKey)
                .addAllDividedImages(dividedImagesBase64)
                .setAnalysisName(analysisName)
                .build();
        ManagedChannel channel = null;
        AnalysisServiceImageDivOuterClass.ImageDivRestoreResponse response = null;
        try {
            channel = createSecureChannel(LOCALHOST, grpcPort,
                                                    grpcClientMaxInboundMessageSize);
            ImageDivServiceBlockingStub stub = ImageDivServiceGrpc.newBlockingStub(channel);
            response = stub.restoreEmbedData(req);
            model.addAttribute("restoredEmbedData", response != null ?
                                                                response.getEmbedData() : null);
            model.addAttribute("restoredEmbedDataType", response != null ?
                                                            response.getEmbedDataType() : null);
        } catch (io.grpc.StatusRuntimeException e) {
            model.addAttribute("errorMessage", e.getStatus().getDescription());
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
        } finally {
            if (channel != null) channel.shutdown();
        }
        return "admin/analysis_test_image_division_restore";
    }

    /**
     * 動画サブミナル生成を実行する
     * @param videoFile 動画ファイル
     * @param analysisName 解析名
     * @param accessKey アクセスキー
     * @param insertImageFiles 挿入する画像ファイルのリスト
     * @param frameIndexes 挿入するフレームのインデックスリスト
     * @param durationFrames 挿入するフレームの持続時間リスト
     * @param insertX 挿入する画像のX座標リスト
     * @param insertY 挿入する画像のY座標リスト
     * @param model モデル
     * @param request HTTPリクエスト
     * @return 動画サブミナル生成結果を表示するテンプレート名
     * @throws IOException 入出力例外
     */
    @PostMapping("/video-subliminal-generate")
    public String runVideoSubliminalGenerate(
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam("analysisName") String analysisName,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            @RequestParam(value = "insertImageFiles", required = false) List<MultipartFile> insertImageFiles,
            @RequestParam(value = "frameIndexes", required = false) List<Integer> frameIndexes,
            @RequestParam(value = "durationFrames", required = false) List<Integer> durationFrames,
            @RequestParam(value = "insertX", required = false) List<Integer> insertX,
            @RequestParam(value = "insertY", required = false) List<Integer> insertY,
            Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ? accessKey : defaultAccessKey;
        if (videoFile == null || videoFile.isEmpty()) {
            String msg = messageSource.getMessage("error.video.file.not.selected",
                                    null, request.getLocale());
            model.addAttribute("error", msg);
            return "admin/analysis_test_form";
        }
        // サブミナル挿入情報を組み立て
        List<SubliminalImageInsert> inserts = new ArrayList<>();
        if (insertImageFiles != null && frameIndexes != null &&
            durationFrames != null && insertX != null && insertY != null) {
            int n = Math.min(insertImageFiles.size(),Math.min(frameIndexes.size(),
                        Math.min(durationFrames.size(), Math.min(insertX.size(), insertY.size()))));
            for (int i = 0; i < n; i++) {
                MultipartFile imgFile = insertImageFiles.get(i);
                if (imgFile == null || imgFile.isEmpty()) continue;
                byte[] imgBytes = imgFile.getBytes();
                inserts.add(SubliminalImageInsert.newBuilder()
                    .setImageFile(ByteString.copyFrom(imgBytes))
                    .setFrameIndex(frameIndexes.get(i))
                    .setDurationFrames(durationFrames.get(i))
                    .setX(insertX.get(i))
                    .setY(insertY.get(i))
                    .build());
            }
        }
        byte[] videoBytes = videoFile.getBytes();
        ManagedChannel channel = null;
        try {
            channel = createSecureChannel(LOCALHOST, grpcPort, grpcClientMaxInboundMessageSize);
            VideoSubliminalServiceBlockingStub stub =
                                            VideoSubliminalServiceGrpc.newBlockingStub(channel);
            VideoSubliminalGenerateRequest req = VideoSubliminalGenerateRequest.newBuilder()
                    .setAccessKey(useAccessKey)
                    .setVideoFile(ByteString.copyFrom(videoBytes))
                    .setAnalysisName(analysisName)
                    .addAllInserts(inserts)
                    .build();
            VideoSubliminalGenerateResponse resp = stub.generateSubliminal(req);
            model.addAttribute("message", resp.getMessage());
            if (resp.getProcessedVideoFile() != null && resp.getProcessedVideoFile().size() > 0) {
                String videoBase64 = Base64.getEncoder().encodeToString(
                                            resp.getProcessedVideoFile().toByteArray());
                model.addAttribute("processedVideoBase64", videoBase64);
            }
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.subliminal.generate",
                                    null, request.getLocale());
            model.addAttribute("error", msg);
        } finally {
            if (channel != null) channel.shutdown();
        }
        List<AnalysisType> analysisTypes = new ArrayList<>();
        try {
            analysisTypes = getAnalysisTypes(useAccessKey);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch",
                                    null, request.getLocale());
            model.addAttribute("error", msg);
        }
        model.addAttribute("analysisTypes", analysisTypes);
        model.addAttribute("selectedAnalysisType", analysisName);
        model.addAttribute("accessKey", useAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen",
                                        null, request.getLocale()),
                  messageSource.getMessage("analysis.test.run.action",
                                new Object[]{analysisName, videoFile.getOriginalFilename()},
                                            request.getLocale()));
        return "admin/analysis_test_form";
    }

    /**
     * 動画サブミナル検出を実行する
     * @param analysisName 解析名
     * @param videoFile 動画ファイル
     * @param accessKey アクセスキー
     * @param model モデル
     * @param request HTTPリクエスト
     * @return 動画サブミナル検出結果を表示するテンプレート名
     * @throws IOException 入出力例外
     */
    @PostMapping("/video-subliminal-check")
    public String runVideoSubliminalCheck(
            @RequestParam("analysisName") String analysisName,
            @RequestParam("videoFile") MultipartFile videoFile,
            @RequestParam(value = "accessKey", required = false) String accessKey,
            Model model, HttpServletRequest request) throws IOException {
        String useAccessKey = (accessKey != null && !accessKey.isEmpty()) ?
                                                        accessKey : defaultAccessKey;
        if (videoFile == null || videoFile.isEmpty()) {
            String msg = messageSource.getMessage("error.video.file.not.selected",
                                    null, request.getLocale());
            model.addAttribute("error", msg);
            return "admin/analysis_test_form";
        }
        byte[] videoBytes = videoFile.getBytes();
        ManagedChannel channel = null;
        try {
            channel = createSecureChannel(LOCALHOST, grpcPort, grpcClientMaxInboundMessageSize);
            VideoSubliminalServiceBlockingStub stub =
                                            VideoSubliminalServiceGrpc.newBlockingStub(channel);
            VideoSubliminalCheckRequest req = VideoSubliminalCheckRequest.newBuilder()
                                                .setAccessKey(useAccessKey)
                                                .setAnalysisName(analysisName)
                                                .setVideoFile(ByteString.copyFrom(videoBytes))
                                                .build();
            VideoSubliminalCheckResponse resp = stub.checkSubliminal(req);
            List<Map<String, Object>> detectedFrames = new ArrayList<>();
            StringBuilder summary = new StringBuilder();
            summary.append(messageSource.getMessage("info.subliminal.detected.frames",
                                null, request.getLocale())).append(resp.getDetectedCount());
            if (resp.getDetectedCount() > 0) {
                summary.append(messageSource.getMessage("info.subliminal.detected.frame.index",
                                null, request.getLocale()));
                for (int i = 0; i < resp.getDetectedCount(); i++) {
                    if (i > 0) summary.append(", ");
                    summary.append(resp.getDetected(i).getFrameIndex());
                }
                summary.append(")");
            }
            for (SubliminalDetected det : resp.getDetectedList()) {
                Map<String, Object> map = new HashMap<>();
                map.put("frameIndex", det.getFrameIndex());
                map.put("imageBase64", Base64.getEncoder().encodeToString(
                                                det.getDetectedImage().toByteArray()));
                detectedFrames.add(map);
            }
            model.addAttribute("detectedFrames", detectedFrames);
            model.addAttribute("subliminalCheckResult", summary.toString());
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.subliminal.check",
                                    null, request.getLocale());
            model.addAttribute("error", msg);
        } finally {
            if (channel != null) channel.shutdown();
        }
        List<AnalysisType> analysisTypes = new ArrayList<>();
        try { 
            analysisTypes = getAnalysisTypes(useAccessKey);
        } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.fetch",
                                    null, request.getLocale());
            model.addAttribute("error", msg);
        }
        model.addAttribute("analysisTypes", analysisTypes);
        model.addAttribute("accessKey", useAccessKey);
        logAction(request, messageSource.getMessage("analysis.test.screen",
                                            null, request.getLocale()),
                  messageSource.getMessage("analysis.test.run.action",
                                new Object[]{"サブミナル検出", videoFile.getOriginalFilename()},
                                                request.getLocale()));
        return "admin/analysis_test_form";
    }

    /**
     * アクションログを記録する
     * @param request HTTPリクエスト
     * @param screen 画面名
     * @param action アクション名
     */
    private void logAction(HttpServletRequest request, String screen, String action) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null && auth.isAuthenticated()) ?
                                            auth.getName() : "anonymous";
        String path = request.getRequestURI();
        Log log = new Log();
        log.setUsername(username);
        log.setPath(path);
        log.setScreen(screen);
        log.setAction(action);
        log.setIpAddress(request.getRemoteAddr());
        logRepository.save(log);
    }

    /**
     * 動画サブミナル生成のgRPCレスポンスを取得する
     * @param useAccessKey アクセスキー
     * @param analysisName 解析名
     * @param videoBytes 動画ファイルのバイト配列
     * @return VideoSubliminalGenerateResponse
     * @throws Exception gRPC呼び出し時の例外
     */
    private VideoSubliminalGenerateResponse getVideoSubliminalGenerateResponse(
            String useAccessKey, String analysisName, byte[] videoBytes) throws Exception {
        VideoSubliminalGenerateResponse response = null;
        ManagedChannel channel = null;
        try {
            channel = createSecureChannel(LOCALHOST, grpcPort,
                                            grpcClientMaxInboundMessageSize);
            VideoSubliminalServiceBlockingStub stub = 
                                VideoSubliminalServiceGrpc.newBlockingStub(channel);
            VideoSubliminalGenerateRequest req = VideoSubliminalGenerateRequest.newBuilder()
                        .setAccessKey(useAccessKey)
                        .setVideoFile(ByteString.copyFrom(videoBytes))
                        .setAnalysisName(analysisName)
                        .build();
            response = stub.generateSubliminal(req);
        } finally {
            if (channel != null) channel.shutdown();
        }
        return response;
    }

    /**
     * gRPCのサービス種別リストを取得する
     * @param useAccessKey アクセスキー
     * @param model モデル
     * @return サービス種別リスト
     * @throws Exception サービス情報取得時の例外
     */
    private List<AnalysisType> getAnalysisTypes(String useAccessKey) throws Exception {
        ManagedChannel channel = null;
        List<AnalysisType> analysisTypes = new ArrayList<>();
        try {
            channel = createSecureChannel(LOCALHOST, grpcPort,
                                            grpcClientMaxInboundMessageSize);
            ServiceInfoServiceBlockingStub stub = ServiceInfoServiceGrpc.newBlockingStub(channel);
            ServiceInfoRequest infoReq = ServiceInfoRequest.newBuilder()
                                            .setAccessKey(useAccessKey)
                                            .build();
            ServiceInfoResponse resp = stub.getAvailableServices(infoReq);
            analysisTypes = resp.getAnalysisTypesList();
        } finally {
            if (channel != null) channel.shutdown();
        }
        return analysisTypes;
    }

    /**
     * gRPCのセキュアチャネルを作成する
     * @param host ホスト
     * @param port ポート番号
     * @param maxInboundMessageSize gRPCの最大受信メッセージサイズ
     * @return ManagedChannel
     * @throws Exception チャネル作成時の例外
     */
    private ManagedChannel createSecureChannel(
                            String host, int port, int maxInboundMessageSize) throws Exception {
        Resource caCertResource = resourceLoader.getResource(caCertPath);
        Resource clientCertResource = resourceLoader.getResource(clientCertPath);
        Resource clientKeyResource = resourceLoader.getResource(clientKeyPath);
        return NettyChannelBuilder.forAddress(host, port)
            .maxInboundMessageSize(maxInboundMessageSize)
            .sslContext(
                GrpcSslContexts.forClient()
                    .trustManager(caCertResource.getFile())
                    .keyManager(clientCertResource.getFile(), clientKeyResource.getFile())
                    .build()
            )
            .build();
    }
}
