#include <jni.h>
#include <vector>

#include "libyuv/convert.h"

namespace {
int ConvertAndroid420ToNv21(const uint8_t* src_y,
                            int src_stride_y,
                            const uint8_t* src_u,
                            int src_stride_u,
                            const uint8_t* src_v,
                            int src_stride_v,
                            int src_pixel_stride_uv,
                            int width,
                            int height,
                            uint8_t* dst_nv21) {
    if (!src_y || !src_u || !src_v || !dst_nv21) {
        return -1;
    }
    if (width <= 0 || height <= 0) {
        return -2;
    }

    const int y_size = width * height;
    const int uv_size = y_size / 4;
    const int scratch_size = y_size + uv_size * 2;

    static thread_local std::vector<uint8_t> scratch;
    if (static_cast<int>(scratch.size()) < scratch_size) {
        scratch.resize(static_cast<size_t>(scratch_size));
    }

    uint8_t* i420_y = scratch.data();
    uint8_t* i420_u = i420_y + y_size;
    uint8_t* i420_v = i420_u + uv_size;

    int ret = libyuv::Android420ToI420(
        src_y, src_stride_y,
        src_u, src_stride_u,
        src_v, src_stride_v,
        src_pixel_stride_uv,
        i420_y, width,
        i420_u, width / 2,
        i420_v, width / 2,
        width, height);
    if (ret != 0) {
        return ret;
    }

    uint8_t* dst_y = dst_nv21;
    uint8_t* dst_vu = dst_nv21 + y_size;
    return libyuv::I420ToNV21(
        i420_y, width,
        i420_u, width / 2,
        i420_v, width / 2,
        dst_y, width,
        dst_vu, width,
        width, height);
}
}  // namespace

extern "C" JNIEXPORT jint JNICALL
Java_com_example_vcam_YuvNative_yuv420ToNv21(
    JNIEnv* env,
    jclass,
    jobject y_buf,
    jint y_stride,
    jint y_offset,
    jobject u_buf,
    jint u_stride,
    jint u_pixel_stride,
    jint u_offset,
    jobject v_buf,
    jint v_stride,
    jint v_pixel_stride,
    jint v_offset,
    jint width,
    jint height,
    jbyteArray out_nv21) {
    if (out_nv21 == nullptr) {
        return -3;
    }

    jsize out_len = env->GetArrayLength(out_nv21);
    const int required = width * height * 3 / 2;
    if (out_len < required) {
        return -4;
    }

    auto* y_base = static_cast<uint8_t*>(env->GetDirectBufferAddress(y_buf));
    auto* u_base = static_cast<uint8_t*>(env->GetDirectBufferAddress(u_buf));
    auto* v_base = static_cast<uint8_t*>(env->GetDirectBufferAddress(v_buf));
    if (!y_base || !u_base || !v_base) {
        return -5;
    }

    jbyte* out_ptr = static_cast<jbyte*>(env->GetPrimitiveArrayCritical(out_nv21, nullptr));
    if (!out_ptr) {
        return -6;
    }

    if (u_pixel_stride != v_pixel_stride) {
        env->ReleasePrimitiveArrayCritical(out_nv21, out_ptr, 0);
        return -7;
    }

    const uint8_t* y = y_base + y_offset;
    const uint8_t* u = u_base + u_offset;
    const uint8_t* v = v_base + v_offset;
    int ret = ConvertAndroid420ToNv21(
        y, y_stride,
        u, u_stride,
        v, v_stride,
        u_pixel_stride,
        width, height,
        reinterpret_cast<uint8_t*>(out_ptr));

    env->ReleasePrimitiveArrayCritical(out_nv21, out_ptr, 0);
    return ret;
}
