package com.entangle.analysis.handler;

import analysis.AnalysisServiceOuterClass;
import io.grpc.stub.StreamObserver;

public interface AnalysisHandler {
    AnalysisServiceOuterClass.AnalysisResponse handle(AnalysisServiceOuterClass.AnalysisRequest request, StreamObserver<AnalysisServiceOuterClass.AnalysisResponse> responseObserver);
}
