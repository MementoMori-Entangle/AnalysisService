package com.entangle.analysis.util;

import org.opencv.core.Core;

public class OpenCVLoaderUtil {
    static {
        // Windows用OpenCV DLLパスを指定（要: opencv_javaXXX.dll配置）
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }
    public static void ensureLoaded() {
        // 何もしない: static初期化子でロード済み
    }
}
