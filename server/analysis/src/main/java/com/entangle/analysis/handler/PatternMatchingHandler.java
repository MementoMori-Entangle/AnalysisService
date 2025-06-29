package com.entangle.analysis.handler;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.global.opencv_core;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Point;
import org.springframework.context.MessageSource;

import com.entangle.analysis.entity.Pattern;
import com.entangle.analysis.entity.ServiceInfo;
import com.entangle.analysis.repository.PatternRepository;
import com.entangle.analysis.service.ServiceInfoService;
import com.entangle.analysis.util.FileSizeFormatUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import analysis.AnalysisServiceOuterClass.AnalysisRequest;
import analysis.AnalysisServiceOuterClass.AnalysisResponse;
import analysis.AnalysisServiceOuterClass.MatchResult;
import io.grpc.stub.StreamObserver;
/**
 * パターンマッチングを行うハンドラー
 */
public class PatternMatchingHandler implements AnalysisHandler {
    private final PatternRepository patternRepository;
    private final ServiceInfoService serviceInfoService;
    private final MessageSource messageSource;
    private final Locale locale;
    private final int defaultMaxUploadSize;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PatternMatchingHandler.class);

    /**
     * コンストラクタ
     * @param patternRepository パターンリポジトリ
     * @param serviceInfoService サービス情報サービス
     * @param messageSource メッセージソース
     * @param locale ロケール
     * @param defaultMaxUploadSize デフォルトの最大アップロードサイズ（バイト単位）
     */
    public PatternMatchingHandler(PatternRepository patternRepository,
                                ServiceInfoService serviceInfoService,
                                MessageSource messageSource, Locale locale,
                                int defaultMaxUploadSize) {
        this.patternRepository = patternRepository;
        this.serviceInfoService = serviceInfoService;
        this.messageSource = messageSource;
        this.locale = locale;
        this.defaultMaxUploadSize = defaultMaxUploadSize;
    }

    /**
     * パターンマッチングを実行する
     * @param request クライアントからのリクエスト
     * @param responseObserver レスポンスを送信するためのStreamObserver
     * @return AnalysisResponse 分析結果を含むレスポンス
     */
    @Override
    public AnalysisResponse handle(AnalysisRequest request,
                                StreamObserver<AnalysisResponse> responseObserver) {
        String analysisName = request.getAnalysisName(); // クライアントから送られる名称
        // 名称からServiceInfoを逆引き
        ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(info -> info.getAnalysisName().equals(analysisName))
            .findFirst().orElse(null);
        if (serviceInfo == null) {
            String msg = messageSource.getMessage("unsupported.analysisType",
                                        new Object[]{analysisName}, locale);
            AnalysisResponse errorResponse = AnalysisResponse.newBuilder()
                .setAnalysisType(analysisName)
                .setTemplateName(request.getTemplateName())
                .setMessage(msg)
                .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
            return errorResponse;
        }
        String dataProcessInfoJson = serviceInfo.getDataProcessInfoJson();
        List<Pattern> patterns = patternRepository.findAll();
        String imageBase64 = request.getImageBase64();
        List<MatchResult> results = new ArrayList<>();
        try {
            // enabledチェック
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(dataProcessInfoJson);
            if (node.has("enabled") &&
                !node.get("enabled").asBoolean(true)) {
                String msg = messageSource.getMessage("error.service.disabled",
                                        null, locale);
                AnalysisResponse response = AnalysisResponse.newBuilder()
                    .setAnalysisType(analysisName)
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return response;
            }
            // --- アップロードサイズ制限チェック ---
            int maxUploadSize = defaultMaxUploadSize;
            if (node.has("max_upload_size")) {
                maxUploadSize = node.get("max_upload_size").asInt(defaultMaxUploadSize);
            }
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            if (imageBytes.length > maxUploadSize) {
                log.warn("アップロード画像サイズ超過: アップロード値 > 制限値",
                            imageBytes.length, maxUploadSize);
                String msg = messageSource.getMessage("error.upload.size",
                                            new Object[]{
                                                FileSizeFormatUtil.formatBytes(
                                                        maxUploadSize, "MB")
                                            }, locale);
                AnalysisResponse errorResponse = AnalysisResponse.newBuilder()
                    .setAnalysisType(analysisName)
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
                responseObserver.onNext(errorResponse);
                responseObserver.onCompleted();
                return errorResponse;
            }
            Mat buf = new Mat(new BytePointer(imageBytes));
            Mat inputImg = opencv_imgcodecs.imdecode(buf, opencv_imgcodecs.IMREAD_COLOR);
            double threshold = 0.8;
            if (node.has("threshold")) {
                threshold = node.get("threshold").asDouble(0.8);
            }
            for (Pattern pattern : patterns) {
                if (!Files.exists(Paths.get(pattern.getImagePath()))) continue;
                Mat templateImg = opencv_imgcodecs.imread(pattern.getImagePath(), opencv_imgcodecs.IMREAD_COLOR);
                if (templateImg.empty()) continue;
                Mat result = new Mat();
                opencv_imgproc.matchTemplate(inputImg, templateImg, result, opencv_imgproc.TM_CCOEFF_NORMED);
                DoublePointer minVal = new DoublePointer(1);
                DoublePointer maxVal = new DoublePointer(1);
                Point minLoc = new Point();
                Point maxLoc = new Point();
                opencv_core.minMaxLoc(result, minVal, maxVal, minLoc, maxLoc, null);
                double similarity = maxVal.get();
                String templateBase64 = "";
                try {
                    byte[] templateBytes = Files.readAllBytes(Paths.get(pattern.getImagePath()));
                    templateBase64 = Base64.getEncoder().encodeToString(templateBytes);
                } catch (Exception e) {
                    log.error("Failed to read template image", e);
                    String msg = messageSource.getMessage("error.reading.template",
                                            new Object[]{pattern.getName()}, locale);
                    AnalysisResponse errorResponse = AnalysisResponse.newBuilder()
                        .setAnalysisType(analysisName)
                        .setTemplateName(request.getTemplateName())
                        .setMessage(msg)
                        .build();
                    responseObserver.onNext(errorResponse);
                    responseObserver.onCompleted();
                    return errorResponse;
                }
                if (similarity >= threshold) {
                    MatchResult match = MatchResult.newBuilder()
                        .setFileName(pattern.getName())
                        .setFileSize(Files.size(Paths.get(pattern.getImagePath())))
                        .setWidth(templateImg.cols())
                        .setHeight(templateImg.rows())
                        .setSimilarity(similarity)
                        .setImageBase64(templateBase64)
                        .build();
                    results.add(match);
                }
            }
            AnalysisResponse response = AnalysisResponse.newBuilder()
                    .setAnalysisType(analysisName)
                    .setTemplateName(request.getTemplateName())
                    .addAllResults(results)
                    .setMessage(messageSource.getMessage("info.pattern.matching.complete",
                                            null, locale))
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            return response;
        } catch (Exception e) {
            log.error("Pattern matching error", e);
            String msg = messageSource.getMessage("error.pattern.matching",
                                    null, locale);
            AnalysisResponse errorResponse = AnalysisResponse.newBuilder()
                .setAnalysisType(analysisName)
                .setTemplateName(request.getTemplateName())
                .setMessage(msg)
                .build();
            responseObserver.onNext(errorResponse);
            responseObserver.onCompleted();
            return errorResponse;
        }
    }
}
