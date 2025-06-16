package com.entangle.analysis.grpc;

import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import com.entangle.analysis.config.GrpcRemoteIpInterceptor;
import com.entangle.analysis.entity.GrpcAccessLog;
import com.entangle.analysis.entity.ServiceInfo;
import com.entangle.analysis.repository.GrpcAccessLogRepository;
import com.entangle.analysis.service.AccessKeyService;
import com.entangle.analysis.service.ServiceInfoService;
import com.entangle.analysis.util.GrpcUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import analysis.AnalysisServiceOuterClass;
import analysis.ServiceInfoServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class ServiceInfoServiceImpl extends ServiceInfoServiceGrpc.ServiceInfoServiceImplBase {
    @Autowired
    private AccessKeyService accessKeyService;
    @Autowired
    private ServiceInfoService serviceInfoService;
    @Autowired
    private GrpcAccessLogRepository grpcAccessLogRepository;
    @Autowired
    private MessageSource messageSource;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ServiceInfoServiceImpl.class);

    private void logGrpcAccess(String serviceName, String methodName, Object requestObj, Object responseObj, String accessKey) {
        try {
            String ip = GrpcRemoteIpInterceptor.REMOTE_IP_KEY.get();
            String reqJson = "";
            String resJson = "";
            if (requestObj instanceof Message) {
                reqJson = JsonFormat.printer().includingDefaultValueFields().print((Message) requestObj);
            } else if (requestObj != null) {
                reqJson = requestObj.toString();
            }
            if (responseObj instanceof Message) {
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
    public void getAvailableServices(AnalysisServiceOuterClass.ServiceInfoRequest request,
                                      StreamObserver<AnalysisServiceOuterClass.ServiceInfoResponse> responseObserver) {
        String accessKey = request.getAccessKey();
        Locale locale = Locale.getDefault();
        if (!accessKeyService.isValid(accessKey)) {
            String msg = messageSource.getMessage("error.accesskey.invalid", null, locale);
            responseObserver.onError(io.grpc.Status.PERMISSION_DENIED.withDescription(msg).asRuntimeException());
            logGrpcAccess("ServiceInfoService", "getAvailableServices", request, null, accessKey);
            return;
        }
        List<ServiceInfo> serviceInfoList = serviceInfoService.findAll();
        AnalysisServiceOuterClass.ServiceInfoResponse.Builder responseBuilder = AnalysisServiceOuterClass.ServiceInfoResponse.newBuilder();
        boolean isAdmin = GrpcUtil.isAdminAccess();
        for (ServiceInfo info : serviceInfoList) {
            String jsonString = info.getDataProcessInfoJson();
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode node = mapper.readTree(jsonString);
                boolean enabled = node.get("enabled").asBoolean();
                if (!enabled) continue;
                boolean release = node.has("release") && node.get("release").asBoolean();
                if (!release && !isAdmin) continue;
            } catch (Exception e) {
                String msg = messageSource.getMessage("error.serviceinfo.json.invalid", null, locale);
                responseObserver.onError(io.grpc.Status.INTERNAL.withDescription(msg).asRuntimeException());
                logGrpcAccess("ServiceInfoService", "getAvailableServices", request, null, accessKey);
                continue;
            }
            AnalysisServiceOuterClass.AnalysisType.Builder analysisTypeBuilder = AnalysisServiceOuterClass.AnalysisType.newBuilder()
                .setType(info.getAnalysisType())
                .setDisplayName(info.getAnalysisName());
            responseBuilder.addAnalysisTypes(analysisTypeBuilder.build());
        }
        AnalysisServiceOuterClass.ServiceInfoResponse response = responseBuilder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
        logGrpcAccess("ServiceInfoService", "getAvailableServices", request, response, accessKey);
    }
}
