package com.example.vcam;

import java.nio.ByteBuffer;

final class YuvNative {
    private static final boolean LOADED;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("vcam_jni");
            loaded = true;
        } catch (Throwable t) {
            // Keep native optional; fallback to Java path if unavailable.
        }
        LOADED = loaded;
    }

    private YuvNative() {
    }

    static boolean isLoaded() {
        return LOADED;
    }

    static native int yuv420ToNv21(
            ByteBuffer y, int yRowStride, int yOffset,
            ByteBuffer u, int uRowStride, int uPixelStride, int uOffset,
            ByteBuffer v, int vRowStride, int vPixelStride, int vOffset,
            int width, int height,
            byte[] outNv21);
}
