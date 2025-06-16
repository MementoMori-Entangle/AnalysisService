package com.entangle.analysis.grpc;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.entangle.analysis.config.GrpcRemoteIpInterceptor;
import com.entangle.analysis.entity.GrpcAccessLog;
import com.entangle.analysis.handler.AnalysisHandler;
import com.entangle.analysis.handler.PatternMatchingHandler;
import com.entangle.analysis.repository.GrpcAccessLogRepository;
import com.entangle.analysis.repository.PatternRepository;
import com.entangle.analysis.service.UserAuthService;
import com.entangle.analysis.util.GrpcUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import analysis.AnalysisServiceGrpc;
import analysis.AnalysisServiceOuterClass;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class AnalysisServiceImpl extends AnalysisServiceGrpc.AnalysisServiceImplBase {
    @Autowired
    private UserAuthService userAuthService;
    @Autowired
    private PatternRepository patternRepository;
    @Autowired
    private com.entangle.analysis.service.ServiceInfoService serviceInfoService;
    @Autowired
    private GrpcAccessLogRepository grpcAccessLogRepository;
    @Autowired
    private com.entangle.analysis.service.AccessKeyService accessKeyService;
    @Autowired
    private MessageSource messageSource;

    private final Map<String, AnalysisHandler> handlerMap = new HashMap<>();
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServiceInfoServiceImpl.class);

    public AnalysisServiceImpl() {
        // JavaCV/Opencv-platform依存でDLL自動ロードされるため、System.loadLibrary等は不要
    }

    @Autowired
    public void initHandlers() {
        // AnalysisHandlerの実装をここに追加
        handlerMap.put("pattern-matching", new PatternMatchingHandler(patternRepository, serviceInfoService, messageSource, Locale.getDefault()));
    }

    // DB認証用メソッド
    public boolean authenticateUser(String username, String password) {
        return userAuthService.authenticate(username, password);
    }

    private void logGrpcAccess(String serviceName, String methodName, Object requestObj, Object responseObj, String accessKey) {
        try {
            String ip = GrpcRemoteIpInterceptor.REMOTE_IP_KEY.get();
            String reqJson = "";
            String resJson = "";
            if (requestObj instanceof AnalysisServiceOuterClass.AnalysisRequest req) {
                AnalysisServiceOuterClass.AnalysisRequest logReq = req.toBuilder().setImageBase64("").build();
                reqJson = JsonFormat.printer().includingDefaultValueFields().print(logReq);
            } else if (requestObj instanceof Message) {
                reqJson = JsonFormat.printer().includingDefaultValueFields().print((Message) requestObj);
            } else if (requestObj != null) {
                reqJson = requestObj.toString();
            }
            if (responseObj instanceof AnalysisServiceOuterClass.AnalysisResponse res) {
                // MatchResultのimageBase64も除外
                AnalysisServiceOuterClass.AnalysisResponse.Builder resBuilder = res.toBuilder();
                List<AnalysisServiceOuterClass.MatchResult> sanitized = new java.util.ArrayList<>();
                for (AnalysisServiceOuterClass.MatchResult m : res.getResultsList()) {
                    sanitized.add(m.toBuilder().setImageBase64("").build());
                }
                resBuilder.clearResults().addAllResults(sanitized);
                resJson = JsonFormat.printer().includingDefaultValueFields().print(resBuilder.build());
            } else if (responseObj instanceof Message) {
                resJson = JsonFormat.printer().includingDefaultValueFields().print((Message) responseObj);
            } else if (responseObj != null) {
                resJson = responseObj.toString();
            }
            GrpcAccessLog log = new GrpcAccessLog();
            log.setServiceName(serviceName);
            log.setMethodName(methodName);
            log.setRequestJson(reqJson);
            log.setResponseJson(resJson);
            log.setAccessKey(accessKey);
            log.setIpAddress(ip != null ? ip : "unknown");
            grpcAccessLogRepository.save(log);
        } catch (Exception e) {
            log.error("GrpcAccessログエラー", e);
        }
    }

    @Override
    public void analyze(AnalysisServiceOuterClass.AnalysisRequest request,
                        StreamObserver<AnalysisServiceOuterClass.AnalysisResponse> responseObserver) {
        String accessKey = request.getAccessKey();
        Locale locale = Locale.getDefault();
        if (!accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid", null, locale);
            responseObserver.onError(new RuntimeException(msg));
            logGrpcAccess("AnalysisService", "analyze", request, null, accessKey);
            return;
        }
        // クライアントから受け取ったanalysisName（例: マッチングA）からServiceInfoを逆引き
        com.entangle.analysis.entity.ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(info -> info.getAnalysisName().equals(request.getAnalysisName()))
            .findFirst().orElse(null);
        String handlerKey = null;
        if (serviceInfo != null) {
            handlerKey = serviceInfo.getAnalysisType(); // 例: pattern-matching
        }
        AnalysisServiceOuterClass.AnalysisResponse response = null;
        String jsonString = serviceInfo.getDataProcessInfoJson();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString);
            boolean enabled = node.get("enabled").asBoolean();
            if (!enabled) {
                String msg = messageSource.getMessage("warning.enabled.false", new Object[]{}, locale);
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription(msg).asRuntimeException());
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
                return;
            }
            boolean isAdmin = GrpcUtil.isAdminAccess();
            boolean release = node.has("release") && node.get("release").asBoolean();
            if (!release && !isAdmin) {
                String msg = messageSource.getMessage("warning.release.false", new Object[]{}, locale);
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription(msg).asRuntimeException());
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
                return;
            }
         } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.json.invalid", null, locale);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(msg).asRuntimeException());
            logGrpcAccess("ServiceInfoService", "getAvailableServices", request, null, accessKey);
            return;
        } finally {
            logGrpcAccess("AnalysisService", "analyze", request, response, accessKey);
        }

        AnalysisHandler handler = handlerKey != null ? handlerMap.get(handlerKey) : null;
        try {
            if (handler != null) {
                response = handler.handle(request, responseObserver);
            } else {
                String msg = messageSource.getMessage("unsupported.analysisType", new Object[]{request.getAnalysisType()}, locale);
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription(msg).asRuntimeException());
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
            }
        } finally {
            // gRPCアクセスログ記録
            if (response == null) {
                String msg = messageSource.getMessage("grpc.response.null", null, locale);
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
            }
            logGrpcAccess("AnalysisService", "analyze", request, response, accessKey);
        }
    }
}
