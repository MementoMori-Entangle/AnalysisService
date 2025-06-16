package com.entangle.analysis.util;

import com.entangle.analysis.config.GrpcRemoteIpInterceptor;

public class GrpcUtil {
    /**
     * 管理画面（localhost等）からのアクセスか判定
     */
    public static boolean isAdminAccess() {
        String ip = GrpcRemoteIpInterceptor.REMOTE_IP_KEY.get();
        System.out.println("Remote IP: " + ip);
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "localhost".equals(ip);
    }
}
