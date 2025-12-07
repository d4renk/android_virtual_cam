package com.example.vcam

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import java.io.File

private const val PREFS_NAME = "vcam_prefs"
private const val KEY_DISABLE_MODULE = "disable_module"
private const val KEY_FORCE_SHOW = "force_show"
private const val KEY_ENABLE_SOUND = "play_sound"
private const val KEY_FORCE_PRIVATE = "force_private_dir"
private const val KEY_SUPPRESS_TOAST = "disable_toast"
private const val KEY_MEDIA_ROOT = "media_root"

private val defaultMediaRoot: File
    get() = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Camera1")

fun loadSettings(context: Context): VcamSettings {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    ensureMediaDir(context)
    return VcamSettings(
        enabled = !prefs.getBoolean(KEY_DISABLE_MODULE, false),
        forceShowTips = prefs.getBoolean(KEY_FORCE_SHOW, false),
        enableShutterSound = prefs.getBoolean(KEY_ENABLE_SOUND, false),
        forcePrivateStorage = prefs.getBoolean(KEY_FORCE_PRIVATE, true),
        suppressToast = prefs.getBoolean(KEY_SUPPRESS_TOAST, false),
        mediaRoot = prefs.getString(KEY_MEDIA_ROOT, defaultMediaRoot.absolutePath) ?: defaultMediaRoot.absolutePath
    )
}

fun saveSettings(context: Context, settings: VcamSettings) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit()
        .putBoolean(KEY_DISABLE_MODULE, !settings.enabled)
        .putBoolean(KEY_FORCE_SHOW, settings.forceShowTips)
        .putBoolean(KEY_ENABLE_SOUND, settings.enableShutterSound)
        .putBoolean(KEY_FORCE_PRIVATE, settings.forcePrivateStorage)
        .putBoolean(KEY_SUPPRESS_TOAST, settings.suppressToast)
        .putString(KEY_MEDIA_ROOT, settings.mediaRoot)
        .apply()
    ensureMediaDir(context)
    makePrefsReadable(prefs)
}

fun ensureMediaDir(context: Context): File {
    val root = File(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_MEDIA_ROOT, defaultMediaRoot.absolutePath) ?: defaultMediaRoot.absolutePath
    )
    if (!root.exists()) {
        root.mkdirs()
    }
    return root
}

private fun makePrefsReadable(prefs: SharedPreferences) {
    try {
        val file = File(prefs.javaClass.getDeclaredField("mFile").apply { isAccessible = true }.get(prefs).toString())
        if (file.exists()) {
            file.setReadable(true, false)
        }
    } catch (_: Exception) {
        // best effort
    }
}

fun mediaRootPath(context: Context): String = ensureMediaDir(context).absolutePath + "/"
