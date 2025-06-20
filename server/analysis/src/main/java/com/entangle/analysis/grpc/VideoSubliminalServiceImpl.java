package com.entangle.analysis.grpc;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;

import com.entangle.analysis.handler.VideoSubliminalHandler;
import com.entangle.analysis.service.AccessKeyService;
import com.entangle.analysis.service.ServiceInfoService;

import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalCheckRequest;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalCheckResponse;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalGenerateRequest;
import analysis.AnalysisServiceVideoSubliminalOuterClass.VideoSubliminalGenerateResponse;
import analysis.VideoSubliminalServiceGrpc.VideoSubliminalServiceImplBase;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * 動画のサブリミナル生成とチェックを行うgRPCサービス
 */
@GrpcService
public class VideoSubliminalServiceImpl extends VideoSubliminalServiceImplBase {
    private final VideoSubliminalHandler videoSubliminalHandler;

    /**
     * コンストラクタ
     * @param messageSource メッセージソース
     * @param accessKeyService アクセスキーサービス
     * @param serviceInfoService サービス情報サービス
     * @param defaultVideoMaxUploadSize 動画の最大アップロードサイズ
     * @param defaultImageMaxUploadSize 画像の最大アップロードサイズ
     * @param defaultSubliminalThreshold サブリミナル検出の閾値
     * @param defaultSubliminalMaxDetected サブリミナル検出の最大数
     */
    @Autowired
    public VideoSubliminalServiceImpl(
                MessageSource messageSource, AccessKeyService accessKeyService,
                ServiceInfoService serviceInfoService,
                @Value("${subliminal.video.upload.max-size}") int defaultVideoMaxUploadSize,
                @Value("${subliminal.image.upload.max-size}") int defaultImageMaxUploadSize,
                @Value("${subliminal_check_threshold}") double defaultSubliminalThreshold,
                @Value("${subliminal_check_max_detected}") int defaultSubliminalMaxDetected) {
        this.videoSubliminalHandler = new VideoSubliminalHandler(messageSource,
                                            Locale.getDefault(), accessKeyService,
                                            serviceInfoService, defaultVideoMaxUploadSize,
                                            defaultImageMaxUploadSize, defaultSubliminalThreshold,
                                            defaultSubliminalMaxDetected);
    }

    /**
     * 動画のサブリミナル生成を行う
     * @param request 生成リクエスト
     * @param responseObserver レスポンスオブザーバー
     * @return 生成結果を含むレスポンス
     */
    @Override
    public void generateSubliminal(VideoSubliminalGenerateRequest request,
                                StreamObserver<VideoSubliminalGenerateResponse> responseObserver) {
        VideoSubliminalGenerateResponse resp = videoSubliminalHandler.generateSubliminal(request);
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }

    /**
     * 動画のサブリミナルチェックを行う
     * @param request チェックリクエスト
     * @param responseObserver レスポンスオブザーバー
     * @return チェック結果を含むレスポンス
     */
    @Override
    public void checkSubliminal(VideoSubliminalCheckRequest request,
                            StreamObserver<VideoSubliminalCheckResponse> responseObserver) {
        VideoSubliminalCheckResponse resp = videoSubliminalHandler.checkSubliminal(request);
        responseObserver.onNext(resp);
        responseObserver.onCompleted();
    }
}
