package com.example.vcam;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import com.example.vcam.HookMain;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;

import de.robv.android.xposed.XposedBridge;

//以下代码修改自 https://github.com/zhantong/Android-VideoToImages
public class VideoToFrames implements Runnable {
    private static final String TAG = "VideoToFrames";
    private static final boolean VERBOSE = false;
    private static final long DEFAULT_TIMEOUT_US = 10000;

    private static final int COLOR_FormatI420 = 1;
    private static final int COLOR_FormatNV21 = 2;

    private static byte[] reusableBuffer;
    private static int reusableBufferSize;
    private static byte[] reusableRowData;

    private final int decodeColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;

    private LinkedBlockingQueue<byte[]> mQueue;
    private OutputImageFormat outputImageFormat;
    private boolean stopDecode = false;

    private String videoFilePath;
    private Throwable throwable;
    private Thread childThread;
    private Surface play_surf;

    private Callback callback;

    public interface Callback {
        void onFinishDecode();

        void onDecodeFrame(int index);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void setEnqueue(LinkedBlockingQueue<byte[]> queue) {
        mQueue = queue;
    }

    //设置输出位置，没啥用
    public void setSaveFrames(String dir, OutputImageFormat imageFormat) throws IOException {
        outputImageFormat = imageFormat;

    }

    public void set_surfcae(Surface player_surface) {
        if (player_surface != null) {
            play_surf = player_surface;
        }
    }

    public void stopDecode() {
        stopDecode = true;
    }

    public void decode(String videoFilePath) throws Throwable {
        this.videoFilePath = videoFilePath;
        if (childThread == null) {
            childThread = new Thread(this, "decode");
            childThread.start();
            if (throwable != null) {
                throw throwable;
            }
        }
    }

    public void run() {
        try {
            videoDecode(videoFilePath);
        } catch (Throwable t) {
            throwable = t;
        }
    }

    @SuppressLint("WrongConstant")
    public void videoDecode(String videoFilePath) throws IOException {
        XposedBridge.log("【VCAM】【decoder】开始解码");
        MediaExtractor extractor = null;
        MediaCodec decoder = null;
        try {
            File videoFile = new File(videoFilePath);
            extractor = new MediaExtractor();
            extractor.setDataSource(videoFilePath);
            int trackIndex = selectTrack(extractor);
            if (trackIndex < 0) {
                XposedBridge.log("【VCAM】【decoder】No video track found in " + videoFilePath);
            }
            extractor.selectTrack(trackIndex);
            MediaFormat mediaFormat = extractor.getTrackFormat(trackIndex);
            String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
            decoder = MediaCodec.createDecoderByType(mime);
            showSupportedColorFormat(decoder.getCodecInfo().getCapabilitiesForType(mime));
            if (isColorFormatSupported(decodeColorFormat, decoder.getCodecInfo().getCapabilitiesForType(mime))) {
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, decodeColorFormat);
                XposedBridge.log("【VCAM】【decoder】set decode color format to type " + decodeColorFormat);
            } else {
                Log.i(TAG, "unable to set decode color format, color format type " + decodeColorFormat + " not supported");
                XposedBridge.log("【VCAM】【decoder】unable to set decode color format, color format type " + decodeColorFormat + " not supported");
            }
            decodeFramesToImage(decoder, extractor, mediaFormat);
            decoder.stop();
            while (!stopDecode) {
                extractor.seekTo(0, 0);
                decodeFramesToImage(decoder, extractor, mediaFormat);
                decoder.stop();
            }
        }catch (Exception e){
            XposedBridge.log("【VCAM】[videofile]"+ e.toString());
        } finally {
            if (decoder != null) {
                decoder.stop();
                decoder.release();
                decoder = null;
            }
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
        }
    }

    private void showSupportedColorFormat(MediaCodecInfo.CodecCapabilities caps) {
        System.out.print("supported color format: ");
        for (int c : caps.colorFormats) {
            System.out.print(c + "\t");
        }
        System.out.println();
    }

    private boolean isColorFormatSupported(int colorFormat, MediaCodecInfo.CodecCapabilities caps) {
        for (int c : caps.colorFormats) {
            if (c == colorFormat) {
                return true;
            }
        }
        return false;
    }

    private void decodeFramesToImage(MediaCodec decoder, MediaExtractor extractor, MediaFormat mediaFormat) {
        boolean is_first = false;
        long startWhen = 0;
        long firstFrameMs = 0;
        long lastFrameMs = 0;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        decoder.configure(mediaFormat, play_surf, null, 0);
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        decoder.start();
        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        HookMain.debugLog("decoder configured width=" + width + " height=" + height + " surface=" + (play_surf != null));
        int outputFrameCount = 0;
        while (!sawOutputEOS && !stopDecode) {
            if (!sawInputEOS) {
                int inputBufferId = decoder.dequeueInputBuffer(DEFAULT_TIMEOUT_US);
                if (inputBufferId >= 0) {
                    ByteBuffer inputBuffer = decoder.getInputBuffer(inputBufferId);
                    int sampleSize = extractor.readSampleData(inputBuffer, 0);
                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputBufferId, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        sawInputEOS = true;
                    } else {
                        long presentationTimeUs = extractor.getSampleTime();
                        decoder.queueInputBuffer(inputBufferId, 0, sampleSize, presentationTimeUs, 0);
                        extractor.advance();
                    }
                }
            }
            int outputBufferId = decoder.dequeueOutputBuffer(info, DEFAULT_TIMEOUT_US);
            if (outputBufferId >= 0) {
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    sawOutputEOS = true;
                }
                boolean doRender = (info.size != 0);
                if (doRender) {
                    outputFrameCount++;
                    if (callback != null) {
                        callback.onDecodeFrame(outputFrameCount);
                    }
                    if (!is_first) {
                        startWhen = System.currentTimeMillis();
                        firstFrameMs = startWhen;
                        is_first = true;
                    }
                    if (play_surf == null) {
                        Image image = decoder.getOutputImage(outputBufferId);
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] arr = new byte[buffer.remaining()];
                        buffer.get(arr);
                        if (mQueue != null) {
                            try {
                                mQueue.put(arr);
                            } catch (InterruptedException e) {
                                XposedBridge.log("【VCAM】" + e.toString());
                            }
                        }
                        if (outputImageFormat != null) {
                            HookMain.data_buffer = getDataFromImage(image, COLOR_FormatNV21);
                        }
                        image.close();
                    }
                    long sleepTime = info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen);
                    if (sleepTime > 0) {
                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException e) {
                            XposedBridge.log("【VCAM】" + e.toString());
                            XposedBridge.log("【VCAM】线程延迟出错");
                        }
                    }
                    decoder.releaseOutputBuffer(outputBufferId, true);
                    lastFrameMs = System.currentTimeMillis();
                }
            }
        }
        if (callback != null) {
            callback.onFinishDecode();
        }
        if (outputFrameCount > 0 && firstFrameMs > 0 && lastFrameMs >= firstFrameMs) {
            long durationMs = lastFrameMs - firstFrameMs;
            double fps = durationMs > 0 ? (outputFrameCount * 1000.0 / durationMs) : 0.0;
            HookMain.debugLog("decoder done frames=" + outputFrameCount +
                " durationMs=" + durationMs + " fps=" + String.format(java.util.Locale.US, "%.2f", fps));
        } else {
            HookMain.debugLog("decoder done frames=" + outputFrameCount);
        }
    }

    private static int selectTrack(MediaExtractor extractor) {
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                if (VERBOSE) {
                    Log.d(TAG, "Extractor selected track " + i + " (" + mime + "): " + format);
                }
                return i;
            }
        }
        return -1;
    }

    private static boolean isImageFormatSupported(Image image) {
        int format = image.getFormat();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                return true;
        }
        return false;
    }

    private static byte[] getDataFromImage(Image image, int colorFormat) {
        if (colorFormat != COLOR_FormatI420 && colorFormat != COLOR_FormatNV21) {
            throw new IllegalArgumentException("only support COLOR_FormatI420 " + "and COLOR_FormatNV21");
        }
        if (!isImageFormatSupported(image)) {
            throw new RuntimeException("can't convert Image to byte array, format " + image.getFormat());
        }
        if (colorFormat == COLOR_FormatNV21 && YuvNative.isLoaded()) {
            byte[] nativeData = getDataFromImageNative(image);
            if (nativeData != null) {
                return nativeData;
            }
        }
        return getDataFromImageJava(image, colorFormat);
    }

    private static byte[] getDataFromImageNative(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        if (planes.length < 3) {
            return null;
        }

        int dataSize = width * height * ImageFormat.getBitsPerPixel(format) / 8;
        if (reusableBuffer == null || reusableBufferSize != dataSize) {
            reusableBuffer = new byte[dataSize];
            reusableBufferSize = dataSize;
        }

        int yRowStride = planes[0].getRowStride();
        int yPixelStride = planes[0].getPixelStride();
        int uRowStride = planes[1].getRowStride();
        int uPixelStride = planes[1].getPixelStride();
        int vRowStride = planes[2].getRowStride();
        int vPixelStride = planes[2].getPixelStride();
        if (yPixelStride != 1) {
            return null;
        }

        int yOffset = yRowStride * crop.top + yPixelStride * crop.left;
        int uvShift = 1;
        int uOffset = uRowStride * (crop.top >> uvShift) + uPixelStride * (crop.left >> uvShift);
        int vOffset = vRowStride * (crop.top >> uvShift) + vPixelStride * (crop.left >> uvShift);

        int result = YuvNative.yuv420ToNv21(
                planes[0].getBuffer(), yRowStride, yOffset,
                planes[1].getBuffer(), uRowStride, uPixelStride, uOffset,
                planes[2].getBuffer(), vRowStride, vPixelStride, vOffset,
                width, height,
                reusableBuffer);
        return result == 0 ? reusableBuffer : null;
    }

    private static byte[] getDataFromImageJava(Image image, int colorFormat) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        int rowDataSize = planes[0].getRowStride();
        if (reusableRowData == null || reusableRowData.length < rowDataSize) {
            reusableRowData = new byte[rowDataSize];
        }
        byte[] rowData = reusableRowData;
        if (VERBOSE) Log.v(TAG, "get data from " + planes.length + " planes");
        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = width * height;
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height + 1;
                        outputStride = 2;
                    }
                    break;
                case 2:
                    if (colorFormat == COLOR_FormatI420) {
                        channelOffset = (int) (width * height * 1.25);
                        outputStride = 1;
                    } else if (colorFormat == COLOR_FormatNV21) {
                        channelOffset = width * height;
                        outputStride = 2;
                    }
                    break;
            }
            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();
            if (VERBOSE) {
                Log.v(TAG, "pixelStride " + pixelStride);
                Log.v(TAG, "rowStride " + rowStride);
                Log.v(TAG, "width " + width);
                Log.v(TAG, "height " + height);
                Log.v(TAG, "buffer size " + buffer.remaining());
            }
            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
            if (VERBOSE) Log.v(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }


}

enum OutputImageFormat {
    I420("I420"),
    NV21("NV21"),
    JPEG("JPEG");
    private final String friendlyName;

    OutputImageFormat(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String toString() {
        return friendlyName;
    }
}
