package com.example.vcam.location.xposed.helpers

import de.robv.android.xposed.XposedBridge
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object LocationLogger {
    private const val DEBUG_LOG_FILE = "debug_log.txt"
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(message: String) {
        XposedBridge.log(message)
        if (!ConfigGateway.get().isLocationDebugEnabled()) {
            return
        }
        appendToFile(message)
    }

    private fun appendToFile(message: String) {
        try {
            val dirPath = ConfigGateway.get().getActiveDir()
            if (dirPath.isBlank()) {
                return
            }
            val dir = File(dirPath)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val line = buildLine("D", message)
            File(dir, DEBUG_LOG_FILE).appendText(line + "\n")
        } catch (_: Exception) {
        }
    }

    private fun buildLine(level: String, message: String): String {
        val timestamp = timeFormat.format(System.currentTimeMillis())
        return "$timestamp [$level] $message"
    }
}
