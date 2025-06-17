package com.entangle.analysis.grpc;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.beans.factory.annotation.Value;

import com.entangle.analysis.handler.ImageDivHandler;
import com.entangle.analysis.repository.ImageDivisionInfoRepository;
import com.entangle.analysis.service.AccessKeyService;
import com.entangle.analysis.service.ServiceInfoService;

import analysis.AnalysisServiceImageDivOuterClass;
import analysis.ImageDivServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ImageDivServiceImpl extends ImageDivServiceGrpc.ImageDivServiceImplBase {
    private final ImageDivHandler imageDivHandler;

    @Autowired
    public ImageDivServiceImpl(MessageSource messageSource, AccessKeyService accessKeyService, ServiceInfoService serviceInfoService, ImageDivisionInfoRepository imageDivisionInfoRepository, @Value("${image.division.encrypt-key}") String encryptKey, @Value("${image.upload.max-size:2097152}") int defaultMaxUploadSize) {
        this.imageDivHandler = new ImageDivHandler(messageSource, Locale.getDefault(), accessKeyService, serviceInfoService, imageDivisionInfoRepository, encryptKey, defaultMaxUploadSize);
    }

    @Override
    public void divideAndEmbed(AnalysisServiceImageDivOuterClass.ImageDivEmbedRequest request,
                              StreamObserver<AnalysisServiceImageDivOuterClass.ImageDivEmbedResponse> responseObserver) {
        imageDivHandler.divideAndEmbed(request, responseObserver);
    }

    @Override
    public void restoreEmbedData(AnalysisServiceImageDivOuterClass.ImageDivRestoreRequest request,
                                 StreamObserver<AnalysisServiceImageDivOuterClass.ImageDivRestoreResponse> responseObserver) {
        imageDivHandler.restoreEmbedData(request, responseObserver);
    }
}
