package com.entangle.analysis.grpc;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;

import com.entangle.analysis.handler.ImageDivHandler;
import com.entangle.analysis.repository.ImageDivisionInfoRepository;
import com.entangle.analysis.service.AccessKeyService;
import com.entangle.analysis.service.ServiceInfoService;

import analysis.AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivEmbedResponse;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest;
import analysis.AnalysisServiceImageDivOuterClass.ImageDivRestoreResponse;
import analysis.ImageDivServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * 画像の分割と埋め込みを行うgRPCサービス
 */
@GrpcService
public class ImageDivServiceImpl extends ImageDivServiceGrpc.ImageDivServiceImplBase {
    private final ImageDivHandler imageDivHandler;

    /**
     * コンストラクタ
     * @param messageSource メッセージソース
     * @param accessKeyService アクセスキーサービス
     * @param serviceInfoService サービス情報サービス
     * @param imageDivisionInfoRepository 画像分割情報リポジトリ
     * @param encryptKey 画像分割の暗号化キー
     * @param defaultMaxUploadSize デフォルトの最大アップロードサイズ
     */
    @Autowired
    public ImageDivServiceImpl(MessageSource messageSource, AccessKeyService accessKeyService,
                            ServiceInfoService serviceInfoService,
                            ImageDivisionInfoRepository imageDivisionInfoRepository,
                            @Value("${image.division.encrypt-key}") String encryptKey,
                            @Value("${image.upload.max-size}") int defaultMaxUploadSize) {
        this.imageDivHandler = new ImageDivHandler(messageSource, Locale.getDefault(),
                                        accessKeyService, serviceInfoService,
                                        imageDivisionInfoRepository, encryptKey,
                                        defaultMaxUploadSize);
    }

    /**
     * 画像の分割と埋め込みを行う
     * @param request 埋め込みリクエスト
     * @param responseObserver レスポンスオブザーバー
     */
    @Override
    public void divideAndEmbed(ImageDivEmbedRequest request,
                              StreamObserver<ImageDivEmbedResponse> responseObserver) {
        imageDivHandler.divideAndEmbed(request, responseObserver);
    }

    /**
     * 埋め込まれたデータを復元する
     * @param request 復元リクエスト
     * @param responseObserver レスポンスオブザーバー
     */
    @Override
    public void restoreEmbedData(ImageDivRestoreRequest request,
                                 StreamObserver<ImageDivRestoreResponse> responseObserver) {
        imageDivHandler.restoreEmbedData(request, responseObserver);
    }
}
