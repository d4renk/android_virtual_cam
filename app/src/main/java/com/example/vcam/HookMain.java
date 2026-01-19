package com.example.vcam;


import android.Manifest;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.InputConfiguration;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookMain implements IXposedHookLoadPackage {
    public static Surface mSurface;
    public static SurfaceTexture mSurfacetexture;
    public static MediaPlayer mMediaPlayer;
    public static SurfaceTexture fake_SurfaceTexture;
    public static Camera origin_preview_camera;

    public static Camera camera_onPreviewFrame;
    public static Camera start_preview_camera;
    public static volatile byte[] data_buffer = {0};
    public static byte[] input;
    public static int mhight;
    public static int mwidth;
    public static boolean is_someone_playing;
    public static boolean is_hooked;
    public static VideoToFrames hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj;
    public static VideoToFrames c2_hw_decode_obj_1;
    public static SurfaceTexture c1_fake_texture;
    public static Surface c1_fake_surface;
    public static SurfaceHolder ori_holder;
    public static MediaPlayer mplayer1;
    public static Camera mcamera1;
    public int imageReaderFormat = 0;
    public static boolean is_first_hook_build = true;

    public static int onemhight;
    public static int onemwidth;
    public static Class camera_callback_calss;

    private static final String PREFS_NAME = "vcam_prefs";
    private static final String KEY_VIDEO_DIR = "video_dir";
    private static final String DEFAULT_VIDEO_DIR = "/storage/emulated/0/Download/Camera1/";
    private static final String DEBUG_LOG_FILE = "debug_log.txt";
    private static final String NOTIFICATION_CHANNEL_ID = "vcam_host_status";
    private static final int HOST_NOTIFICATION_ID = 2101;
    private static volatile boolean hostNotificationShown = false;

    public static String video_path = DEFAULT_VIDEO_DIR;
    public static boolean use_private_dir = false;

    public static Surface c2_preview_Surfcae;
    public static Surface c2_preview_Surfcae_1;
    public static Surface c2_reader_Surfcae;
    public static Surface c2_reader_Surfcae_1;
    public static MediaPlayer c2_player;
    public static MediaPlayer c2_player_1;
    public static Surface c2_virtual_surface;
    public static SurfaceTexture c2_virtual_surfaceTexture;
    public boolean need_recreate;
    public static CameraDevice.StateCallback c2_state_cb;
    public static CaptureRequest.Builder c2_builder;
    public static SessionConfiguration fake_sessionConfiguration;
    public static SessionConfiguration sessionConfiguration;
    public static OutputConfiguration outputConfiguration;
    public boolean need_to_show_toast = true;
    public boolean has_image_reader_target = false;

    public int c2_ori_width = 1280;
    public int c2_ori_height = 720;

    public static Class c2_state_callback;
    public Context toast_content;

    public static boolean isDebugEnabled() {
        try {
            if (new File(getActiveVideoDir() + "debug_log.jpg").exists()) {
                return true;
            }
            return new File(getConfiguredPublicDir() + "debug_log.jpg").exists();
        } catch (Throwable t) {
            return false;
        }
    }

    public static void debugLog(String message) {
        if (isDebugEnabled()) {
            String line = buildLogLine("D", message);
            XposedBridge.log("【VCAM】【debug】" + message);
            appendDebugLogToFile(line);
        }
    }

    private static String buildLogLine(String level, String message) {
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US);
        String timestamp = format.format(System.currentTimeMillis());
        return timestamp + " [" + level + "] " + message;
    }

    private static void appendDebugLogToFile(String line) {
        try {
            String dirPath = getActiveVideoDir();
            if (dirPath == null || dirPath.isEmpty()) {
                return;
            }
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream fos = new FileOutputStream(new File(dir, DEBUG_LOG_FILE), true);
            fos.write((line + "\n").getBytes());
            fos.flush();
            fos.close();
        } catch (Throwable t) {
            XposedBridge.log("【VCAM】[debug-log-file]" + t);
        }
    }

    private static String getConfiguredPublicDir() {
        XSharedPreferences prefs = new XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME);
        prefs.reload();
        String dir = prefs.getString(KEY_VIDEO_DIR, DEFAULT_VIDEO_DIR);
        if (dir == null || dir.trim().isEmpty()) {
            dir = DEFAULT_VIDEO_DIR;
        }
        if (!dir.endsWith("/")) {
            dir += "/";
        }
        video_path = dir;
        return dir;
    }

    private static String getActiveVideoDir() {
        if (use_private_dir && video_path != null && !video_path.isEmpty()) {
            return video_path;
        }
        return getConfiguredPublicDir();
    }

    private static void writeLastResolution(int width, int height, String source) {
        try {
            FileOutputStream fos = new FileOutputStream(getActiveVideoDir() + "last_resolution.txt", false);
            String content = width + "," + height + "," + source;
            fos.write(content.getBytes());
            fos.flush();
            fos.close();
            debugLog("write last_resolution.txt " + content);
        } catch (Exception e) {
            XposedBridge.log("【VCAM】[resolution]" + e);
        }
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Exception {
        debugLog("handleLoadPackage package=" + lpparam.packageName);
        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewTexture", SurfaceTexture.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                if (file.exists()) {
                    File control_file = new File(getActiveVideoDir() + "disable.jpg");
                    if (control_file.exists()){
                        return;
                    }
                    debugLog("camera1 setPreviewTexture camera=" + param.thisObject);
                    if (is_hooked) {
                        is_hooked = false;
                        return;
                    }
                    if (param.args[0] == null) {
                        return;
                    }
                    if (param.args[0].equals(c1_fake_texture)) {
                        return;
                    }
                    if (origin_preview_camera != null && origin_preview_camera.equals(param.thisObject)) {
                        param.args[0] = fake_SurfaceTexture;
                        XposedBridge.log("【VCAM】发现重复" + origin_preview_camera.toString());
                        return;
                    } else {
                        XposedBridge.log("【VCAM】创建预览");
                    }

                    origin_preview_camera = (Camera) param.thisObject;
                    mSurfacetexture = (SurfaceTexture) param.args[0];
                    if (fake_SurfaceTexture == null) {
                        fake_SurfaceTexture = new SurfaceTexture(10);
                    } else {
                        fake_SurfaceTexture.release();
                        fake_SurfaceTexture = new SurfaceTexture(10);
                    }
                    param.args[0] = fake_SurfaceTexture;
                } else {
                    File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, CameraDevice.StateCallback.class, Handler.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args[1] == null) {
                    return;
                }
                if (param.args[1].equals(c2_state_cb)) {
                    return;
                }
                c2_state_cb = (CameraDevice.StateCallback) param.args[1];
                c2_state_callback = param.args[1].getClass();
                File control_file = new File(getActiveVideoDir() + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                XposedBridge.log("【VCAM】1位参数初始化相机，类：" + c2_state_callback.toString());
                is_first_hook_build = true;
                process_camera2_init(c2_state_callback);
            }
        });


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraManager", lpparam.classLoader, "openCamera", String.class, Executor.class, CameraDevice.StateCallback.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (param.args[2] == null) {
                        return;
                    }
                    if (param.args[2].equals(c2_state_cb)) {
                        return;
                    }
                    c2_state_cb = (CameraDevice.StateCallback) param.args[2];
                    File control_file = new File(getActiveVideoDir() + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    File file = new File(getActiveVideoDir() + "virtual.mp4");
                    File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (!file.exists()) {
                        if (toast_content != null && need_to_show_toast) {
                            try {
                                Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                            } catch (Exception ee) {
                                XposedBridge.log("【VCAM】[toast]" + ee.toString());
                            }
                        }
                        return;
                    }
                    c2_state_callback = param.args[2].getClass();
                    XposedBridge.log("【VCAM】2位参数初始化相机，类：" + c2_state_callback.toString());
                    is_first_hook_build = true;
                    process_camera2_init(c2_state_callback);
                }
            });
        }


        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallbackWithBuffer", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "addCallbackBuffer", byte[].class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    param.args[0] = new byte[((byte[]) param.args[0]).length];
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setOneShotPreviewCallback", Camera.PreviewCallback.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.args[0] != null) {
                    process_callback(param);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "takePicture", Camera.ShutterCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, Camera.PictureCallback.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                XposedBridge.log("【VCAM】4参数拍照");
                if (param.args[1] != null) {
                    process_a_shot_YUV(param);
                }

                if (param.args[3] != null) {
                    process_a_shot_jpeg(param, 3);
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.media.MediaRecorder", lpparam.classLoader, "setCamera", Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                super.beforeHookedMethod(param);
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                XposedBridge.log("【VCAM】[record]" + lpparam.packageName);
                if (toast_content != null && need_to_show_toast) {
                    try {
                        Toast.makeText(toast_content, "应用：" + lpparam.appInfo.name + "(" + lpparam.packageName + ")" + "触发了录像，但目前无法拦截", Toast.LENGTH_SHORT).show();
                    }catch (Exception ee){
                        XposedBridge.log("【VCAM】[toast]" + Arrays.toString(ee.getStackTrace()));
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "callApplicationOnCreate", Application.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);
                if (param.args[0] instanceof Application) {
                    try {
                        toast_content = ((Application) param.args[0]).getApplicationContext();
                        if (!hostNotificationShown && !BuildConfig.APPLICATION_ID.equals(lpparam.packageName)) {
                            hostNotificationShown = true;
                            postHostNotification(toast_content, lpparam.packageName);
                        }
                    } catch (Exception ee) {
                        XposedBridge.log("【VCAM】" + ee.toString());
                    }
                    File force_private = new File(getConfiguredPublicDir() + "private_dir.jpg");
                    if (toast_content != null) {//后半段用于强制私有目录
                        int auth_statue = 0;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                auth_statue += (toast_content.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) + 1);
                            } catch (Exception ee) {
                                XposedBridge.log("【VCAM】[permission-check]" + ee.toString());
                            }
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                    auth_statue += (toast_content.checkSelfPermission(Manifest.permission.MANAGE_EXTERNAL_STORAGE) + 1);
                                }
                            } catch (Exception ee) {
                                XposedBridge.log("【VCAM】[permission-check]" + ee.toString());
                            }
                        }else {
                            if (toast_content.checkCallingPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ){
                                auth_statue = 2;
                            }
                        }
                        //权限判断完毕
                        if (auth_statue < 1 || force_private.exists()) {
                            File shown_file = new File(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/");
                            if ((!shown_file.isDirectory()) && shown_file.exists()) {
                                shown_file.delete();
                            }
                            if (!shown_file.exists()) {
                                shown_file.mkdir();
                            }
                            shown_file = new File(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/" + "has_shown");
                            File toast_force_file = new File(getConfiguredPublicDir() + "force_show.jpg");
                            if ((!lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) && ((!shown_file.exists()) || toast_force_file.exists())) {
                                try {
                                    Toast.makeText(toast_content, lpparam.packageName+"未授予读取本地目录权限，请检查权限\nCamera1目前重定向为 " + toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/", Toast.LENGTH_SHORT).show();
                                    FileOutputStream fos = new FileOutputStream(toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/" + "has_shown");
                                    String info = "shown";
                                    fos.write(info.getBytes());
                                    fos.flush();
                                    fos.close();
                                } catch (Exception e) {
                                    XposedBridge.log("【VCAM】[switch-dir]" + e.toString());
                                }
                            }
                            use_private_dir = true;
                            video_path = toast_content.getExternalFilesDir(null).getAbsolutePath() + "/Camera1/";
                        }else {
                            use_private_dir = false;
                            video_path = getConfiguredPublicDir();
                        }
                    } else {
                        use_private_dir = false;
                        video_path = getConfiguredPublicDir();
                        File uni_DCIM_path = new File(getConfiguredPublicDir());
                        if (uni_DCIM_path.canWrite()) {
                            File uni_Camera1_path = new File(video_path);
                            if (!uni_Camera1_path.exists()) {
                                uni_Camera1_path.mkdir();
                            }
                        }
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "startPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                File control_file = new File(getActiveVideoDir() + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                is_someone_playing = false;
                XposedBridge.log("【VCAM】开始预览");
                start_preview_camera = (Camera) param.thisObject;
                if (ori_holder != null) {

                    if (mplayer1 == null) {
                        mplayer1 = new MediaPlayer();
                    } else {
                        mplayer1.release();
                        mplayer1 = null;
                        mplayer1 = new MediaPlayer();
                    }
                    if (!ori_holder.getSurface().isValid() || ori_holder == null) {
                        return;
                    }
                    mplayer1.setSurface(ori_holder.getSurface());
                    File sfile = new File(getActiveVideoDir() + "no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))) {
                        mplayer1.setVolume(0, 0);
                        is_someone_playing = false;
                    } else {
                        is_someone_playing = true;
                    }
                    mplayer1.setLooping(true);

                    mplayer1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mplayer1.start();
                        }
                    });

                    try {
                        mplayer1.setDataSource(getActiveVideoDir() + "virtual.mp4");
                        mplayer1.prepare();
                    } catch (IOException e) {
                        XposedBridge.log("【VCAM】" + e.toString());
                    }
                }


                if (mSurfacetexture != null) {
                    if (mSurface == null) {
                        mSurface = new Surface(mSurfacetexture);
                    } else {
                        mSurface.release();
                        mSurface = new Surface(mSurfacetexture);
                    }

                    if (mMediaPlayer == null) {
                        mMediaPlayer = new MediaPlayer();
                    } else {
                        mMediaPlayer.release();
                        mMediaPlayer = new MediaPlayer();
                    }

                    mMediaPlayer.setSurface(mSurface);

                    File sfile = new File(getActiveVideoDir() + "no-silent.jpg");
                    if (!(sfile.exists() && (!is_someone_playing))) {
                        mMediaPlayer.setVolume(0, 0);
                        is_someone_playing = false;
                    } else {
                        is_someone_playing = true;
                    }
                    mMediaPlayer.setLooping(true);

                    mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            mMediaPlayer.start();
                        }
                    });

                    try {
                        mMediaPlayer.setDataSource(getActiveVideoDir() + "virtual.mp4");
                        mMediaPlayer.prepare();
                    } catch (IOException e) {
                        XposedBridge.log("【VCAM】" + e.toString());
                    }
                }
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "setPreviewDisplay", SurfaceHolder.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】添加Surfaceview预览");
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                File control_file = new File(getActiveVideoDir() + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                mcamera1 = (Camera) param.thisObject;
                ori_holder = (SurfaceHolder) param.args[0];
                if (c1_fake_texture == null) {
                    c1_fake_texture = new SurfaceTexture(11);
                } else {
                    c1_fake_texture.release();
                    c1_fake_texture = null;
                    c1_fake_texture = new SurfaceTexture(11);
                }

                if (c1_fake_surface == null) {
                    c1_fake_surface = new Surface(c1_fake_texture);
                } else {
                    c1_fake_surface.release();
                    c1_fake_surface = null;
                    c1_fake_surface = new Surface(c1_fake_texture);
                }
                is_hooked = true;
                mcamera1.setPreviewTexture(c1_fake_texture);
                param.setResult(null);
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "addTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {

                if (param.args[0] == null) {
                    return;
                }
                if (param.thisObject == null) {
                    return;
                }
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                if (param.args[0].equals(c2_virtual_surface)) {
                    return;
                }
                File control_file = new File(getActiveVideoDir() + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                String surfaceInfo = param.args[0].toString();
                if (surfaceInfo.contains("Surface(name=null)")) {
                    has_image_reader_target = true;
                    if (c2_reader_Surfcae == null) {
                        c2_reader_Surfcae = (Surface) param.args[0];
                    } else {
                        if ((!c2_reader_Surfcae.equals(param.args[0])) && c2_reader_Surfcae_1 == null) {
                            c2_reader_Surfcae_1 = (Surface) param.args[0];
                        }
                    }
                } else {
                    if (c2_preview_Surfcae == null) {
                        c2_preview_Surfcae = (Surface) param.args[0];
                    } else {
                        if ((!c2_preview_Surfcae.equals(param.args[0])) && c2_preview_Surfcae_1 == null) {
                            c2_preview_Surfcae_1 = (Surface) param.args[0];
                        }
                    }
                }
                XposedBridge.log("【VCAM】添加目标：" + param.args[0].toString());
                if (!has_image_reader_target) {
                    param.args[0] = c2_virtual_surface;
                }

            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "removeTarget", Surface.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {

                if (param.args[0] == null) {
                    return;
                }
                if (param.thisObject == null) {
                    return;
                }
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                File control_file = new File(getActiveVideoDir() + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                Surface rm_surf = (Surface) param.args[0];
                if (rm_surf.equals(c2_preview_Surfcae)) {
                    c2_preview_Surfcae = null;
                }
                if (rm_surf.equals(c2_preview_Surfcae_1)) {
                    c2_preview_Surfcae_1 = null;
                }
                if (rm_surf.equals(c2_reader_Surfcae_1)) {
                    c2_reader_Surfcae_1 = null;
                }
                if (rm_surf.equals(c2_reader_Surfcae)) {
                    c2_reader_Surfcae = null;
                }

                XposedBridge.log("【VCAM】移除目标：" + param.args[0].toString());
            }
        });

        XposedHelpers.findAndHookMethod("android.hardware.camera2.CaptureRequest.Builder", lpparam.classLoader, "build", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.thisObject == null) {
                    return;
                }
                if (param.thisObject.equals(c2_builder)) {
                    return;
                }
                c2_builder = (CaptureRequest.Builder) param.thisObject;
                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists() && need_to_show_toast) {
                    if (toast_content != null) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + lpparam.packageName + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    debugLog("camera2 build: virtual.mp4 missing dir=" + getActiveVideoDir());
                    return;
                }

                File control_file = new File(getActiveVideoDir() + "disable.jpg");
                if (control_file.exists()) {
                    return;
                }
                XposedBridge.log("【VCAM】开始build请求");
                if (!has_image_reader_target) {
                    process_camera2_play();
                }
            }
        });

/*        XposedHelpers.findAndHookMethod("android.hardware.Camera", lpparam.classLoader, "stopPreview", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (param.thisObject.equals(HookMain.origin_preview_camera) || param.thisObject.equals(HookMain.camera_onPreviewFrame) || param.thisObject.equals(HookMain.mcamera1)) {
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    if (mplayer1 != null) {
                        mplayer1.release();
                        mplayer1 = null;
                    }
                    if (mMediaPlayer != null) {
                        mMediaPlayer.release();
                        mMediaPlayer = null;
                    }
                    is_someone_playing = false;

                    XposedBridge.log("停止预览");
                }
            }
        });*/

        XposedHelpers.findAndHookMethod("android.media.ImageReader", lpparam.classLoader, "newInstance", int.class, int.class, int.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                XposedBridge.log("【VCAM】应用创建了渲染器：宽：" + param.args[0] + " 高：" + param.args[1] + "格式" + param.args[2]);
                c2_ori_width = (int) param.args[0];
                c2_ori_height = (int) param.args[1];
                imageReaderFormat = (int) param.args[2];
                debugLog("ImageReader newInstance width=" + c2_ori_width + " height=" + c2_ori_height + " format=" + imageReaderFormat);
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (toast_content != null && need_to_show_toast) {
                    try {
                        Toast.makeText(toast_content, "应用创建了渲染器：\n宽：" + param.args[0] + "\n高：" + param.args[1] + "\n一般只需要宽高比与视频相同", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        XposedBridge.log("【VCAM】[toast]" + e.toString());
                    }
                }
            }
        });


        XposedHelpers.findAndHookMethod("android.hardware.camera2.CameraCaptureSession.CaptureCallback", lpparam.classLoader, "onCaptureFailed", CameraCaptureSession.class, CaptureRequest.class, CaptureFailure.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        XposedBridge.log("【VCAM】onCaptureFailed" + "原因：" + ((CaptureFailure) param.args[2]).getReason());

                    }
                });
    }

    private static void postHostNotification(Context hostContext, String hostPackage) {
        if (hostContext == null) {
            return;
        }
        Context moduleContext;
        try {
            moduleContext = hostContext.createPackageContext(
                BuildConfig.APPLICATION_ID,
                Context.CONTEXT_IGNORE_SECURITY | Context.CONTEXT_INCLUDE_CODE
            );
        } catch (Throwable t) {
            XposedBridge.log("【VCAM】[notify-context]" + t);
            return;
        }
        NotificationManager manager =
            (NotificationManager) hostContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "VCAM Host",
                NotificationManager.IMPORTANCE_LOW
            );
            manager.createNotificationChannel(channel);
        }
        Intent openIntent = new Intent();
        openIntent.setClassName(BuildConfig.APPLICATION_ID, "com.example.vcam.MainActivity");
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
            hostContext,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
            ? new Notification.Builder(hostContext, NOTIFICATION_CHANNEL_ID)
            : new Notification.Builder(hostContext);
        builder.setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("VCAM 模块")
            .setContentText("来自宿主：" + hostPackage)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_view, "打开模块", openPending);
        manager.notify(HOST_NOTIFICATION_ID, builder.build());
    }

    private void process_camera2_play() {

        if (c2_reader_Surfcae != null) {
            if (c2_hw_decode_obj != null) {
                c2_hw_decode_obj.stopDecode();
                c2_hw_decode_obj = null;
            }

            c2_hw_decode_obj = new VideoToFrames();
            try {
                if (imageReaderFormat == 256) {
                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.JPEG);
                } else {
                    c2_hw_decode_obj.setSaveFrames("null", OutputImageFormat.NV21);
                }
                c2_hw_decode_obj.set_surfcae(c2_reader_Surfcae);
                c2_hw_decode_obj.decode(getActiveVideoDir() + "virtual.mp4");
                debugLog("camera2 decode reader surface format=" + imageReaderFormat + " dir=" + getActiveVideoDir());
            } catch (Throwable throwable) {
                XposedBridge.log("【VCAM】" + throwable);
            }
        }

        if (c2_reader_Surfcae_1 != null) {
            if (c2_hw_decode_obj_1 != null) {
                c2_hw_decode_obj_1.stopDecode();
                c2_hw_decode_obj_1 = null;
            }

            c2_hw_decode_obj_1 = new VideoToFrames();
            try {
                if (imageReaderFormat == 256) {
                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.JPEG);
                } else {
                    c2_hw_decode_obj_1.setSaveFrames("null", OutputImageFormat.NV21);
                }
                c2_hw_decode_obj_1.set_surfcae(c2_reader_Surfcae_1);
                c2_hw_decode_obj_1.decode(getActiveVideoDir() + "virtual.mp4");
                debugLog("camera2 decode reader surface(2) format=" + imageReaderFormat + " dir=" + getActiveVideoDir());
            } catch (Throwable throwable) {
                XposedBridge.log("【VCAM】" + throwable);
            }
        }


        if (c2_preview_Surfcae != null) {
            if (c2_player == null) {
                c2_player = new MediaPlayer();
            } else {
                c2_player.release();
                c2_player = new MediaPlayer();
            }
            c2_player.setSurface(c2_preview_Surfcae);
            File sfile = new File(getActiveVideoDir() + "no-silent.jpg");
            if (!sfile.exists()) {
                c2_player.setVolume(0, 0);
            }
            c2_player.setLooping(true);

            try {
                c2_player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        c2_player.start();
                    }
                });
                c2_player.setDataSource(getActiveVideoDir() + "virtual.mp4");
                c2_player.prepare();
                debugLog("camera2 preview player prepared dir=" + getActiveVideoDir());
            } catch (Exception e) {
                XposedBridge.log("【VCAM】[c2player][" + c2_preview_Surfcae.toString() + "]" + e);
            }
        }

        if (c2_preview_Surfcae_1 != null) {
            if (c2_player_1 == null) {
                c2_player_1 = new MediaPlayer();
            } else {
                c2_player_1.release();
                c2_player_1 = new MediaPlayer();
            }
            c2_player_1.setSurface(c2_preview_Surfcae_1);
            File sfile = new File(getActiveVideoDir() + "no-silent.jpg");
            if (!sfile.exists()) {
                c2_player_1.setVolume(0, 0);
            }
            c2_player_1.setLooping(true);

            try {
                c2_player_1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer mp) {
                        c2_player_1.start();
                    }
                });
                c2_player_1.setDataSource(getActiveVideoDir() + "virtual.mp4");
                c2_player_1.prepare();
                debugLog("camera2 preview player(2) prepared dir=" + getActiveVideoDir());
            } catch (Exception e) {
                XposedBridge.log("【VCAM】[c2player1]" + "[ " + c2_preview_Surfcae_1.toString() + "]" + e);
            }
        }
        XposedBridge.log("【VCAM】Camera2处理过程完全执行");
    }

    private Surface create_virtual_surface() {
        if (need_recreate) {
            if (c2_virtual_surfaceTexture != null) {
                c2_virtual_surfaceTexture.release();
                c2_virtual_surfaceTexture = null;
            }
            if (c2_virtual_surface != null) {
                c2_virtual_surface.release();
                c2_virtual_surface = null;
            }
            c2_virtual_surfaceTexture = new SurfaceTexture(15);
            c2_virtual_surface = new Surface(c2_virtual_surfaceTexture);
            need_recreate = false;
        } else {
            if (c2_virtual_surface == null) {
                need_recreate = true;
                c2_virtual_surface = create_virtual_surface();
            }
        }
        XposedBridge.log("【VCAM】【重建垃圾场】" + c2_virtual_surface.toString());
        return c2_virtual_surface;
    }

    private void process_camera2_init(Class hooked_class) {

        XposedHelpers.findAndHookMethod(hooked_class, "onOpened", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                need_recreate = true;
                has_image_reader_target = false;
                create_virtual_surface();
                if (c2_player != null) {
                    c2_player.stop();
                    c2_player.reset();
                    c2_player.release();
                    c2_player = null;
                }
                if (c2_hw_decode_obj_1 != null) {
                    c2_hw_decode_obj_1.stopDecode();
                    c2_hw_decode_obj_1 = null;
                }
                if (c2_hw_decode_obj != null) {
                    c2_hw_decode_obj.stopDecode();
                    c2_hw_decode_obj = null;
                }
                if (c2_player_1 != null) {
                    c2_player_1.stop();
                    c2_player_1.reset();
                    c2_player_1.release();
                    c2_player_1 = null;
                }
                c2_preview_Surfcae_1 = null;
                c2_reader_Surfcae_1 = null;
                c2_reader_Surfcae = null;
                c2_preview_Surfcae = null;
                is_first_hook_build = true;
                XposedBridge.log("【VCAM】打开相机C2");

                File file = new File(getActiveVideoDir() + "virtual.mp4");
                File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                need_to_show_toast = !toast_control.exists();
                if (!file.exists()) {
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "不存在替换视频\n" + toast_content.getPackageName() + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    return;
                }
                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSession", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        if (paramd.args[0] != null) {
                            XposedBridge.log("【VCAM】createCaptureSession创捷捕获，原始:" + paramd.args[0].toString() + "虚拟：" + c2_virtual_surface.toString());
                            if (!has_image_reader_target) {
                                paramd.args[0] = Arrays.asList(c2_virtual_surface);
                                if (paramd.args[1] != null) {
                                    process_camera2Session_callback((CameraCaptureSession.StateCallback) paramd.args[1]);
                                }
                            }
                        }
                    }
                });

/*                XposedHelpers.findAndHookMethod(param.args[0].getClass(), "close", new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                        XposedBridge.log("C2终止预览");
                        if (c2_hw_decode_obj != null) {
                            c2_hw_decode_obj.stopDecode();
                            c2_hw_decode_obj = null;
                        }
                        if (c2_hw_decode_obj_1 != null) {
                            c2_hw_decode_obj_1.stopDecode();
                            c2_hw_decode_obj_1 = null;
                        }
                        if (c2_player != null) {
                            c2_player.release();
                            c2_player = null;
                        }
                        if (c2_player_1 != null){
                            c2_player_1.release();
                            c2_player_1 = null;
                        }
                        c2_preview_Surfcae_1 = null;
                        c2_reader_Surfcae_1 = null;
                        c2_reader_Surfcae = null;
                        c2_preview_Surfcae = null;
                        need_recreate = true;
                        is_first_hook_build= true;
                    }
                });*/

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSessionByOutputConfigurations", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[0] != null) {
                                if (!has_image_reader_target) {
                                    outputConfiguration = new OutputConfiguration(c2_virtual_surface);
                                    param.args[0] = Arrays.asList(outputConfiguration);

                                    XposedBridge.log("【VCAM】执行了createCaptureSessionByOutputConfigurations-144777");
                                    if (param.args[1] != null) {
                                        process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[1]);
                                    }
                                }
                            }
                        }
                    });
                }


                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createConstrainedHighSpeedCaptureSession", List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[0] != null) {
                                if (!has_image_reader_target) {
                                    param.args[0] = Arrays.asList(c2_virtual_surface);
                                    XposedBridge.log("【VCAM】执行了 createConstrainedHighSpeedCaptureSession -5484987");
                                    if (param.args[1] != null) {
                                        process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[1]);
                                    }
                                }
                            }
                        }
                    });


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createReprocessableCaptureSession", InputConfiguration.class, List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[1] != null) {
                                if (!has_image_reader_target) {
                                    param.args[1] = Arrays.asList(c2_virtual_surface);
                                    XposedBridge.log("【VCAM】执行了 createReprocessableCaptureSession ");
                                    if (param.args[2] != null) {
                                        process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[2]);
                                    }
                                }
                            }
                        }
                    });
                }


                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createReprocessableCaptureSessionByConfigurations", InputConfiguration.class, List.class, CameraCaptureSession.StateCallback.class, Handler.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[1] != null) {
                                if (!has_image_reader_target) {
                                    outputConfiguration = new OutputConfiguration(c2_virtual_surface);
                                    param.args[0] = Arrays.asList(outputConfiguration);
                                    XposedBridge.log("【VCAM】执行了 createReprocessableCaptureSessionByConfigurations");
                                    if (param.args[2] != null) {
                                        process_camera2Session_callback((CameraCaptureSession.StateCallback) param.args[2]);
                                    }
                                }
                            }
                        }
                    });
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    XposedHelpers.findAndHookMethod(param.args[0].getClass(), "createCaptureSession", SessionConfiguration.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            if (param.args[0] != null) {
                                XposedBridge.log("【VCAM】执行了 createCaptureSession -5484987");
                                if (!has_image_reader_target) {
                                    sessionConfiguration = (SessionConfiguration) param.args[0];
                                    outputConfiguration = new OutputConfiguration(c2_virtual_surface);
                                    fake_sessionConfiguration = new SessionConfiguration(sessionConfiguration.getSessionType(),
                                            Arrays.asList(outputConfiguration),
                                            sessionConfiguration.getExecutor(),
                                            sessionConfiguration.getStateCallback());
                                    param.args[0] = fake_sessionConfiguration;
                                    process_camera2Session_callback(sessionConfiguration.getStateCallback());
                                }
                            }
                        }
                    });
                }
            }
        });


        XposedHelpers.findAndHookMethod(hooked_class, "onError", CameraDevice.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】相机错误onerror：" + (int) param.args[1]);
            }

        });


        XposedHelpers.findAndHookMethod(hooked_class, "onDisconnected", CameraDevice.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】相机断开onDisconnected ：");
            }

        });


    }

    private void process_a_shot_jpeg(XC_MethodHook.MethodHookParam param, int index) {
        try {
            XposedBridge.log("【VCAM】第二个jpeg:" + param.args[index].toString());
        } catch (Exception eee) {
            XposedBridge.log("【VCAM】" + eee);

        }
        Class callback = param.args[index].getClass();

        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("【VCAM】JPEG拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString());
                    debugLog("photo jpeg init width=" + onemwidth + " height=" + onemhight + " camera=" + loaclcam);
                    writeLastResolution(onemwidth, onemhight, "photo_jpeg");
                    File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：JPEG", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            XposedBridge.log("【VCAM】[toast]" + e.toString());
                        }
                    }
                    File control_file = new File(getActiveVideoDir() + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }

                    Bitmap pict = getBMP(getActiveVideoDir() + "1000.bmp");
                    ByteArrayOutputStream temp_array = new ByteArrayOutputStream();
                    pict.compress(Bitmap.CompressFormat.JPEG, 100, temp_array);
                    byte[] jpeg_data = temp_array.toByteArray();
                    paramd.args[0] = jpeg_data;
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】" + ee.toString());
                }
            }
        });
    }

    private void process_a_shot_YUV(XC_MethodHook.MethodHookParam param) {
        try {
            XposedBridge.log("【VCAM】发现拍照YUV:" + param.args[1].toString());
        } catch (Exception eee) {
            XposedBridge.log("【VCAM】" + eee);
        }
        Class callback = param.args[1].getClass();
        XposedHelpers.findAndHookMethod(callback, "onPictureTaken", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                try {
                    Camera loaclcam = (Camera) paramd.args[1];
                    onemwidth = loaclcam.getParameters().getPreviewSize().width;
                    onemhight = loaclcam.getParameters().getPreviewSize().height;
                    XposedBridge.log("【VCAM】YUV拍照回调初始化：宽：" + onemwidth + "高：" + onemhight + "对应的类：" + loaclcam.toString());
                    debugLog("photo yuv init width=" + onemwidth + " height=" + onemhight + " camera=" + loaclcam);
                    writeLastResolution(onemwidth, onemhight, "photo_yuv");
                    File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "发现拍照\n宽：" + onemwidth + "\n高：" + onemhight + "\n格式：YUV_420_888", Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    File control_file = new File(getActiveVideoDir() + "disable.jpg");
                    if (control_file.exists()) {
                        return;
                    }
                    input = getYUVByBitmap(getBMP(getActiveVideoDir() + "1000.bmp"));
                    paramd.args[0] = input;
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】" + ee.toString());
                }
            }
        });
    }

    private void process_callback(XC_MethodHook.MethodHookParam param) {
        Class preview_cb_class = param.args[0].getClass();
        int need_stop = 0;
        File control_file = new File(getActiveVideoDir() + "disable.jpg");
        if (control_file.exists()) {
            need_stop = 1;
        }
        File file = new File(getActiveVideoDir() + "virtual.mp4");
        File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
        need_to_show_toast = !toast_control.exists();
        if (!file.exists()) {
            if (toast_content != null && need_to_show_toast) {
                try {
                    Toast.makeText(toast_content, "不存在替换视频\n" + toast_content.getPackageName() + "当前路径：" + getActiveVideoDir(), Toast.LENGTH_SHORT).show();
                } catch (Exception ee) {
                    XposedBridge.log("【VCAM】[toast]" + ee);
                }
            }
            debugLog("camera1 preview: virtual.mp4 missing dir=" + getActiveVideoDir());
            need_stop = 1;
        }
        int finalNeed_stop = need_stop;
        XposedHelpers.findAndHookMethod(preview_cb_class, "onPreviewFrame", byte[].class, android.hardware.Camera.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam paramd) throws Throwable {
                Camera localcam = (android.hardware.Camera) paramd.args[1];
                if (localcam.equals(camera_onPreviewFrame)) {
                    while (data_buffer == null) {
                    }
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.length, ((byte[]) paramd.args[0]).length));
                } else {
                    camera_callback_calss = preview_cb_class;
                    camera_onPreviewFrame = (android.hardware.Camera) paramd.args[1];
                    mwidth = camera_onPreviewFrame.getParameters().getPreviewSize().width;
                    mhight = camera_onPreviewFrame.getParameters().getPreviewSize().height;
                    int frame_Rate = camera_onPreviewFrame.getParameters().getPreviewFrameRate();
                    XposedBridge.log("【VCAM】帧预览回调初始化：宽：" + mwidth + " 高：" + mhight + " 帧率：" + frame_Rate);
                    debugLog("camera1 preview init width=" + mwidth + " height=" + mhight + " fps=" + frame_Rate);
                    writeLastResolution(mwidth, mhight, "preview");
                    File toast_control = new File(getActiveVideoDir() + "no_toast.jpg");
                    need_to_show_toast = !toast_control.exists();
                    if (toast_content != null && need_to_show_toast) {
                        try {
                            Toast.makeText(toast_content, "发现预览\n宽：" + mwidth + "\n高：" + mhight + "\n" + "需要视频分辨率与其完全相同", Toast.LENGTH_SHORT).show();
                        } catch (Exception ee) {
                            XposedBridge.log("【VCAM】[toast]" + ee.toString());
                        }
                    }
                    if (finalNeed_stop == 1) {
                        return;
                    }
                    if (hw_decode_obj != null) {
                        hw_decode_obj.stopDecode();
                    }
                    hw_decode_obj = new VideoToFrames();
                    hw_decode_obj.setSaveFrames("", OutputImageFormat.NV21);
                    hw_decode_obj.decode(getActiveVideoDir() + "virtual.mp4");
                    debugLog("camera1 decode start dir=" + getActiveVideoDir());
                    while (data_buffer == null) {
                    }
                    System.arraycopy(data_buffer, 0, paramd.args[0], 0, Math.min(data_buffer.length, ((byte[]) paramd.args[0]).length));
                }

            }
        });

    }

    private void process_camera2Session_callback(CameraCaptureSession.StateCallback callback_calss){
        if (callback_calss == null){
            return;
        }
        XposedHelpers.findAndHookMethod(callback_calss.getClass(), "onConfigureFailed", CameraCaptureSession.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】onConfigureFailed ：" + param.args[0].toString());
            }

        });

        XposedHelpers.findAndHookMethod(callback_calss.getClass(), "onConfigured", CameraCaptureSession.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】onConfigured ：" + param.args[0].toString());
            }
        });

        XposedHelpers.findAndHookMethod( callback_calss.getClass(), "onClosed", CameraCaptureSession.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                XposedBridge.log("【VCAM】onClosed ："+ param.args[0].toString());
            }
        });
    }



    //以下代码来源：https://blog.csdn.net/jacke121/article/details/73888732
    private Bitmap getBMP(String file) throws Throwable {
        return BitmapFactory.decodeFile(file);
    }

    private static byte[] rgb2YCbCr420(int[] pixels, int width, int height) {
        int len = width * height;
        // yuv格式数组大小，y亮度占len长度，u,v各占len/4长度。
        byte[] yuv = new byte[len * 3 / 2];
        int y, u, v;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int rgb = (pixels[i * width + j]) & 0x00FFFFFF;
                int r = rgb & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = (rgb >> 16) & 0xFF;
                // 套用公式
                y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                y = y < 16 ? 16 : (Math.min(y, 255));
                u = u < 0 ? 0 : (Math.min(u, 255));
                v = v < 0 ? 0 : (Math.min(v, 255));
                // 赋值
                yuv[i * width + j] = (byte) y;
                yuv[len + (i >> 1) * width + (j & ~1)] = (byte) u;
                yuv[len + +(i >> 1) * width + (j & ~1) + 1] = (byte) v;
            }
        }
        return yuv;
    }

    private static byte[] getYUVByBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = width * height;
        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        return rgb2YCbCr420(pixels, width, height);
    }
}
