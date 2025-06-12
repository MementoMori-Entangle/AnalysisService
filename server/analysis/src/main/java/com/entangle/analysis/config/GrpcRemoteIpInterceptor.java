package com.entangle.analysis.config;

import io.grpc.*;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.springframework.context.annotation.Configuration;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

public class GrpcRemoteIpInterceptor implements ServerInterceptor {
    public static final Context.Key<String> REMOTE_IP_KEY = Context.key("remote-ip");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String remoteIp = "unknown";
        SocketAddress remoteAddr = call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        if (remoteAddr instanceof InetSocketAddress) {
            remoteIp = ((InetSocketAddress) remoteAddr).getAddress().getHostAddress();
        }
        Context ctx = Context.current().withValue(REMOTE_IP_KEY, remoteIp);
        return Contexts.interceptCall(ctx, call, headers, next);
    }

    @Configuration
    public static class GrpcInterceptorConfig {
        @GrpcGlobalServerInterceptor
        public ServerInterceptor remoteIpInterceptor() {
            return new GrpcRemoteIpInterceptor();
        }
    }
}
