package com.example.vcam.location.xposed.helpers

import com.example.vcam.location.xposed.helpers.LocationLogger

import android.os.Build
import android.os.Environment
import com.example.vcam.BuildConfig
import com.example.vcam.location.model.FakeLocation
import de.robv.android.xposed.XSharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.reflect.Field

class ConfigGateway private constructor() {
    companion object {
        private const val PREFS_NAME = "vcam_prefs"
        private const val KEY_VIDEO_DIR = "video_dir"
        private const val DEFAULT_VIDEO_DIR = "/storage/emulated/0/Download/Camera1/"

        private const val FLAG_PRIVATE_DIR = "private_dir.jpg"
        private const val FLAG_LOCATION_DISABLE = "location_disable.jpg"
        private const val FLAG_LOCATION_DEBUG = "location_debug.jpg"

        private const val FILE_LOCATION_CONFIG = "location_config.json"
        private const val FILE_LOCATION_WHITELIST = "location_whitelist.json"

        private var instance: ConfigGateway? = null
            get() {
                if (field == null) {
                    field = ConfigGateway()
                }
                return field
            }

        fun get(): ConfigGateway {
            return instance!!
        }
    }

    private fun getPublicDir(): String {
        val prefs = XSharedPreferences(BuildConfig.APPLICATION_ID, PREFS_NAME)
        prefs.reload()
        var dir = prefs.getString(KEY_VIDEO_DIR, DEFAULT_VIDEO_DIR) ?: DEFAULT_VIDEO_DIR
        if (!dir.endsWith("/")) {
            dir += "/"
        }
        return dir
    }

    private fun getPrivateDir(): String {
        val base = Environment.getExternalStorageDirectory().absolutePath
        return "$base/Android/data/${BuildConfig.APPLICATION_ID}/files/Camera1/"
    }

    fun getActiveDir(): String {
        val publicDir = getPublicDir()
        return if (File(publicDir + FLAG_PRIVATE_DIR).exists()) {
            getPrivateDir()
        } else {
            publicDir
        }
    }

    fun isLocationDisabled(): Boolean {
        return File(getActiveDir() + FLAG_LOCATION_DISABLE).exists()
    }

    fun isLocationDebugEnabled(): Boolean {
        return File(getActiveDir() + FLAG_LOCATION_DEBUG).exists()
    }

    fun inWhitelist(packageName: String): Boolean {
        if (isLocationDisabled()) {
            return false
        }
        val list = readWhitelist()
        if (list.isEmpty()) {
            return true
        }
        return list.any { packageName.contains(it) }
    }

    fun readWhitelist(): List<String> {
        val file = File(getActiveDir() + FILE_LOCATION_WHITELIST)
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val json = JSONArray(file.readText())
            List(json.length()) { index -> json.optString(index, "") }.filter { it.isNotBlank() }
        } catch (e: Exception) {
            LocationLogger.log("VCAM: [location] Failed to parse whitelist: $e")
            emptyList()
        }
    }

    fun readFakeLocation(): FakeLocation {
        val file = File(getActiveDir() + FILE_LOCATION_CONFIG)
        val json = if (file.exists()) {
            file.readText()
        } else {
            "{}"
        }
        return try {
            val obj = JSONObject(json)
            FakeLocation(
                obj.optDouble("x", 0.0),
                obj.optDouble("y", 0.0),
                obj.optDouble("offset", 0.0),
                obj.optInt("eci", 0),
                obj.optInt("pci", 0),
                obj.optInt("tac", 0),
                obj.optInt("earfcn", 0),
                obj.optInt("bandwidth", 0)
            )
        } catch (e: Exception) {
            LocationLogger.log("VCAM: [location] Failed to parse config: $e")
            FakeLocation(0.0, 0.0, 0.0, 0, 0, 0, 0, 0)
        }
    }

    fun callerIdentityToPackageName(callerIdentity: Any): String {
        val fields = HiddenApiBypass.getInstanceFields(callerIdentity.javaClass)

        val targetFieldName = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> "private final java.lang.String android.location.util.identity.CallerIdentity.mPackageName"
            Build.VERSION.SDK_INT == Build.VERSION_CODES.R -> "public final java.lang.String com.android.server.location.CallerIdentity.packageName"
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> "public final java.lang.String com.android.server.location.CallerIdentity.mPackageName"
            Build.VERSION.SDK_INT == Build.VERSION_CODES.P -> "final java.lang.String com.android.server.LocationManagerService.Identity.mPackageName"
            else -> ""
        }

        for (field in fields) {
            if (field.toString() == targetFieldName) {
                val targetField = field as Field
                targetField.isAccessible = true
                return targetField.get(callerIdentity) as String
            }
        }

        if (callerIdentity is String) return callerIdentity

        throw IllegalArgumentException("VCAM: Invalid CallerIdentity: $callerIdentity")
    }
}
