package com.entangle.analysis.grpc;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;

import com.entangle.analysis.config.GrpcRemoteIpInterceptor;
import com.entangle.analysis.entity.GrpcAccessLog;
import com.entangle.analysis.entity.ServiceInfo;
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

import analysis.AnalysisServiceGrpc.AnalysisServiceImplBase;
import analysis.AnalysisServiceOuterClass;
import analysis.AnalysisServiceOuterClass.AnalysisRequest;
import analysis.AnalysisServiceOuterClass.AnalysisResponse;
import analysis.AnalysisServiceOuterClass.MatchResult;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * 分析を行うgRPCサービス
 */
@GrpcService
public class AnalysisServiceImpl extends AnalysisServiceImplBase {
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

    @Value("${image.upload.max-size}")
    private int defaultMaxUploadSize;

    private final Map<String, AnalysisHandler> handlerMap = new HashMap<>();
    private static final Logger log = LoggerFactory.getLogger(ServiceInfoServiceImpl.class);

    /**
     * コンストラクタ
     */
    public AnalysisServiceImpl() {
        // JavaCV/Opencv-platform依存でDLL自動ロードされるため、System.loadLibrary等は不要
    }

    /**
     * gRPCサービスのハンドラーを初期化
     * ここでは、AnalysisHandlerの実装を登録します。
     * 例えば、PatternMatchingHandlerなどの具体的なハンドラーを追加します。
     */
    @Autowired
    public void initHandlers() {
        // AnalysisHandlerの実装をここに追加
        handlerMap.put("pattern-matching", new PatternMatchingHandler(patternRepository, serviceInfoService, messageSource, Locale.getDefault(), defaultMaxUploadSize));
    }

    /**
     * ユーザー認証を行う
     * @param username ユーザー名
     * @param password パスワード
     * @return 認証成功ならtrue、失敗ならfalse
     */
    public boolean authenticateUser(String username, String password) {
        return userAuthService.authenticate(username, password);
    }

    /**
     * gRPCアクセスログを保存する
     * @param serviceName サービス名
     * @param methodName メソッド名
     * @param requestObj リクエストオブジェクト
     * @param responseObj レスポンスオブジェクト
     * @param accessKey アクセスキー
     */
    private void logGrpcAccess(String serviceName, String methodName,
                        Object requestObj, Object responseObj, String accessKey) {
        try {
            String ip = GrpcRemoteIpInterceptor.REMOTE_IP_KEY.get();
            String reqJson = "";
            String resJson = "";
            if (requestObj instanceof AnalysisRequest req) {
                AnalysisRequest logReq = req.toBuilder().setImageBase64("").build();
                reqJson = JsonFormat.printer().includingDefaultValueFields().print(logReq);
            } else if (requestObj instanceof Message) {
                reqJson = JsonFormat.printer().includingDefaultValueFields()
                                    .print((Message) requestObj);
            } else if (requestObj != null) {
                reqJson = requestObj.toString();
            }
            if (responseObj instanceof AnalysisResponse res) {
                // MatchResultのimageBase64も除外
                AnalysisResponse.Builder resBuilder = res.toBuilder();
                List<MatchResult> sanitized = new java.util.ArrayList<>();
                for (MatchResult m : res.getResultsList()) {
                    sanitized.add(m.toBuilder().setImageBase64("").build());
                }
                resBuilder.clearResults().addAllResults(sanitized);
                resJson = JsonFormat.printer().includingDefaultValueFields()
                                    .print(resBuilder.build());
            } else if (responseObj instanceof Message) {
                resJson = JsonFormat.printer().includingDefaultValueFields()
                                    .print((Message) responseObj);
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

    /**
     * gRPCの分析リクエストを処理する
     * @param request リクエストオブジェクト
     * @param responseObserver レスポンスオブジェクトのストリームオブザーバー
     */
    @Override
    public void analyze(AnalysisRequest request,
                        StreamObserver<AnalysisResponse> responseObserver) {
        String accessKey = request.getAccessKey();
        Locale locale = Locale.getDefault();
        if (!accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid",
                                    null, locale);
            responseObserver.onError(new RuntimeException(msg));
            logGrpcAccess("AnalysisService",
                            "analyze", request, null, accessKey);
            return;
        }
        // クライアントから受け取ったanalysisName（例: マッチングA）からServiceInfoを逆引き
        ServiceInfo serviceInfo = serviceInfoService.findAll().stream()
            .filter(info -> info.getAnalysisName().equals(request.getAnalysisName()))
            .findFirst().orElse(null);
        if (serviceInfo == null) {
            String msg = messageSource.getMessage("error.serviceinfo.notfound",
                                        new Object[]{request.getAnalysisName()}, locale);
            responseObserver.onError(io.grpc.Status.NOT_FOUND.withDescription(msg)
                                                            .asRuntimeException());
            logGrpcAccess("AnalysisService",
                            "analyze", request, null, accessKey);
            return;
        }
        String handlerKey = null;
        if (serviceInfo != null) {
            handlerKey = serviceInfo.getAnalysisType();
        }
        AnalysisServiceOuterClass.AnalysisResponse response = null;
        String jsonString = serviceInfo.getDataProcessInfoJson();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(jsonString);
            boolean enabled = node.get("enabled").asBoolean();
            if (!enabled) {
                String msg = messageSource.getMessage("warning.enabled.false",
                                            new Object[]{}, locale);
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription(msg)
                                                                        .asRuntimeException());
                response = AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
                return;
            }
            boolean isAdmin = GrpcUtil.isAdminAccess();
            boolean release = node.has("release") &&
                                node.get("release").asBoolean();
            if (!release && !isAdmin) {
                String msg = messageSource.getMessage("warning.release.false",
                                            new Object[]{}, locale);
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription(msg)
                                                                        .asRuntimeException());
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
                return;
            }
         } catch (Exception e) {
            String msg = messageSource.getMessage("error.serviceinfo.json.invalid",
                                    null, locale);
            responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(msg)
                                                            .asRuntimeException());
            logGrpcAccess("ServiceInfoService",
                            "getAvailableServices", request,
                            null, accessKey);
            return;
        } finally {
            logGrpcAccess("AnalysisService",
                        "analyze", request, response, accessKey);
        }
        AnalysisHandler handler = handlerKey != null ? handlerMap.get(handlerKey) : null;
        try {
            if (handler != null) {
                response = handler.handle(request, responseObserver);
            } else {
                String msg = messageSource.getMessage("unsupported.analysisType",
                                            new Object[]{request.getAnalysisType()}, locale);
                responseObserver.onError(io.grpc.Status.FAILED_PRECONDITION.withDescription(msg)
                                                                        .asRuntimeException());
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
            }
        } finally {
            // gRPCアクセスログ記録
            if (response == null) {
                String msg = messageSource.getMessage("grpc.response.null",
                                        null, locale);
                response = AnalysisServiceOuterClass.AnalysisResponse.newBuilder()
                    .setAnalysisType(request.getAnalysisType())
                    .setTemplateName(request.getTemplateName())
                    .setMessage(msg)
                    .build();
            }
            logGrpcAccess("AnalysisService",
                            "analyze", request, response, accessKey);
        }
    }
}
