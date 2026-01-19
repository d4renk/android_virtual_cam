package com.example.vcam

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vcam.ui.theme.AppTheme
import java.io.File
import java.io.IOException
import android.provider.DocumentsContract
import android.media.MediaMetadataRetriever
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.FileOutputStream
import android.provider.OpenableColumns

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "VCAM_Main"
        private const val PREFS_NAME = "vcam_prefs"
        private const val KEY_VIDEO_DIR = "video_dir"
        private const val KEY_TREE_URI = "video_dir_tree_uri"
        private const val DEFAULT_VIDEO_DIR = "/storage/emulated/0/Download/Camera1/"
        private const val CHANNEL_ID = "vcam_status"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var pickDirLauncher: ActivityResultLauncher<Uri?>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    private val forceShowState = mutableStateOf(false)
    private val disableState = mutableStateOf(false)
    private val playSoundState = mutableStateOf(false)
    private val forcePrivateDirState = mutableStateOf(false)
    private val disableToastState = mutableStateOf(false)
    private val debugLogState = mutableStateOf(false)
    private val videoDirState = mutableStateOf(DEFAULT_VIDEO_DIR)
    private val materialCheckState = mutableStateOf("")
    private val selectedImageNameState = mutableStateOf("")
    private val selectedImageUriState = mutableStateOf<Uri?>(null)
    private val generateState = mutableStateOf("")
    private val expectedResolutionState = mutableStateOf("")

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show()
            } else {
                ensureVideoDirExists()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncStateWithFiles()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickDirLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
            if (it != null) {
                handlePickedDir(it)
            }
        }
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                handlePickedImage(it)
            }
        }
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize()) { MainScreen() }
            }
        }
        syncStateWithFiles()
    }

    @Composable
    private fun MainScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(text = stringResource(id = R.string.current_dir, videoDirState.value))
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { pickDirLauncher.launch(null) }) {
                    Text(text = stringResource(id = R.string.pick_dir))
                }
                Button(onClick = { openVideoDir() }) {
                    Text(text = stringResource(id = R.string.open_dir))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (expectedResolutionState.value.isNotBlank()) {
                Text(text = expectedResolutionState.value)
                Spacer(modifier = Modifier.height(8.dp))
            }
            Button(onClick = { checkMaterial() }, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(id = R.string.check_material))
            }
            if (materialCheckState.value.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = materialCheckState.value)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { pickImageLauncher.launch("image/*") }) {
                    Text(text = stringResource(id = R.string.pick_image))
                }
                Button(onClick = { generateVideoFromImage() }) {
                    Text(text = stringResource(id = R.string.generate_video))
                }
            }
            if (selectedImageNameState.value.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = stringResource(id = R.string.selected_image, selectedImageNameState.value))
            }
            if (generateState.value.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = generateState.value)
            }
            Spacer(modifier = Modifier.height(16.dp))
            SwitchRow(
                textRes = R.string.switch1,
                checked = forceShowState.value,
                onCheckedChange = { updateToggle(FileMode.FORCE_SHOW, it) }
            )
            SwitchRow(
                textRes = R.string.switch2,
                checked = disableState.value,
                onCheckedChange = { updateToggle(FileMode.DISABLE, it) }
            )
            SwitchRow(
                textRes = R.string.switch3,
                checked = playSoundState.value,
                onCheckedChange = { updateToggle(FileMode.PLAY_SOUND, it) }
            )
            SwitchRow(
                textRes = R.string.switch4,
                checked = forcePrivateDirState.value,
                onCheckedChange = { updateToggle(FileMode.FORCE_PRIVATE_DIR, it) }
            )
            SwitchRow(
                textRes = R.string.switch5,
                checked = disableToastState.value,
                onCheckedChange = { updateToggle(FileMode.DISABLE_TOAST, it) }
            )
            SwitchRow(
                textRes = R.string.switch6,
                checked = debugLogState.value,
                onCheckedChange = { updateToggle(FileMode.DEBUG_LOG, it) }
            )
        }
    }

    @Composable
    private fun SwitchRow(
        textRes: Int,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = textRes),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    private fun handlePickedDir(uri: Uri) {
        logDebug("picked dir uri=$uri")
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            logWarn("failed to persist uri permission", e)
        }

        val path = treeUriToPath(uri)
        if (path == null) {
            Toast.makeText(this, "仅支持主存储目录", Toast.LENGTH_SHORT).show()
            return
        }
        logDebug("picked dir path=$path")
        setVideoDir(path, uri)
    }

    private fun handlePickedImage(uri: Uri) {
        selectedImageUriState.value = uri
        selectedImageNameState.value = getDisplayName(uri) ?: "image"
        generateState.value = ""
        logDebug("picked image uri=$uri name=${selectedImageNameState.value}")
    }

    private fun getDisplayName(uri: Uri): String? {
        return try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(0)
                    } else {
                        null
                    }
                }
        } catch (e: Exception) {
            null
        }
    }

    private fun treeUriToPath(uri: Uri): String? {
        val docId = DocumentsContract.getTreeDocumentId(uri)
        val split = docId.split(":")
        if (split.size < 1) {
            return null
        }
        if (split[0] != "primary") {
            return null
        }
        val relative = if (split.size > 1) split[1] else ""
        val base = Environment.getExternalStorageDirectory().absolutePath
        return if (relative.isEmpty()) {
            "$base/"
        } else {
            "$base/$relative/"
        }
    }

    private fun openVideoDir() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val tree = prefs.getString(KEY_TREE_URI, null)
        if (tree.isNullOrBlank()) {
            Toast.makeText(this, "请先选择目录", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = Uri.parse(tree)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "无法打开目录", Toast.LENGTH_SHORT).show()
        }
    }

    private fun normalizeDir(dir: String): String {
        return if (dir.endsWith("/")) dir else "$dir/"
    }

    private fun getVideoDir(): String {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val dir = prefs.getString(KEY_VIDEO_DIR, DEFAULT_VIDEO_DIR) ?: DEFAULT_VIDEO_DIR
        return normalizeDir(dir)
    }

    private fun setVideoDir(dir: String, treeUri: Uri?) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit().putString(KEY_VIDEO_DIR, normalizeDir(dir))
        if (treeUri != null) {
            editor.putString(KEY_TREE_URI, treeUri.toString())
        }
        editor.apply()
        logDebug("set video dir=${normalizeDir(dir)} treeUri=$treeUri")
        makePrefsReadable()
        videoDirState.value = normalizeDir(dir)
        expectedResolutionState.value = formatExpectedResolution(readExpectedResolution())
        ensureVideoDirExists()
        updateMissingVideoNotification()
    }

    private fun updateToggle(mode: FileMode, enabled: Boolean) {
        if (!hasPermission()) {
            requestPermission()
            return
        }
        val file = File(
            getVideoDir() + mode.fileName
        )
        if (file.exists() != enabled) {
            if (enabled) {
                try {
                    file.createNewFile()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            } else {
                file.delete()
            }
        }
        syncStateWithFiles()
    }

    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED
            ) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.permission_lack_warn)
                builder.setMessage(R.string.permission_description)
                builder.setNegativeButton(R.string.negative) { _, _ ->
                    Toast.makeText(this, R.string.permission_lack_warn, Toast.LENGTH_SHORT).show()
                }
                builder.setPositiveButton(R.string.positive) { _, _ ->
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        1
                    )
                }
                builder.show()
            }
        }
    }

    private fun hasPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_DENIED &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_DENIED
        }
        return true
    }

    private fun syncStateWithFiles() {
        logDebug("sync state with files")

        if (!hasPermission()) {
            requestPermission()
        } else {
            ensureVideoDirExists()
        }

        makePrefsReadable()
        videoDirState.value = getVideoDir()
        expectedResolutionState.value = formatExpectedResolution(readExpectedResolution())
        forceShowState.value = File(getVideoDir() + FileMode.FORCE_SHOW.fileName).exists()
        disableState.value = File(getVideoDir() + FileMode.DISABLE.fileName).exists()
        playSoundState.value = File(getVideoDir() + FileMode.PLAY_SOUND.fileName).exists()
        forcePrivateDirState.value = File(getVideoDir() + FileMode.FORCE_PRIVATE_DIR.fileName).exists()
        disableToastState.value = File(getVideoDir() + FileMode.DISABLE_TOAST.fileName).exists()
        debugLogState.value = File(getVideoDir() + FileMode.DEBUG_LOG.fileName).exists()
        updateMissingVideoNotification()
    }

    private fun makePrefsReadable() {
        val prefFile = File(applicationInfo.dataDir, "shared_prefs/$PREFS_NAME.xml")
        if (prefFile.exists()) {
            prefFile.setReadable(true, false)
        }
    }

    private fun updateMissingVideoNotification() {
        val videoFile = File(getVideoDir() + "virtual.mp4")
        val manager = NotificationManagerCompat.from(this)
        if (videoFile.exists()) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        createNotificationChannel()
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle(getString(R.string.notification_missing_title))
            .setContentText(getString(R.string.notification_missing_text, getVideoDir()))
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "VCAM 状态提示",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun checkMaterial() {
        val videoFile = File(getVideoDir() + "virtual.mp4")
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (!videoFile.exists()) {
            errors.add("未找到 virtual.mp4")
            logWarn("check material: virtual.mp4 missing in ${getVideoDir()}")
        } else {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(videoFile.absolutePath)
                val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toIntOrNull()
                val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull()
                val frame = retriever.getFrameAtTime(0)
                if (width == null || height == null) {
                    errors.add("无法读取视频分辨率")
                }
                if (frame == null) {
                    errors.add("视频解码失败")
                } else {
                    frame.recycle()
                }

                val expected = readExpectedResolution()
                logDebug("check material: video=${width}x${height} expected=$expected")
                if (expected == null) {
                    warnings.add(getString(R.string.expected_resolution_missing))
                } else if (width != null && height != null &&
                    (width != expected.width || height != expected.height)
                ) {
                    errors.add(getString(R.string.resolution_mismatch))
                    warnings.add(
                        getString(
                            R.string.resolution_expected_detail,
                            expected.width,
                            expected.height,
                            width,
                            height
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add(getString(R.string.video_read_failed, e.message ?: ""))
                logWarn("check material: failed to read video", e)
            } finally {
                retriever.release()
            }
        }

        val builder = StringBuilder()
        if (errors.isEmpty()) {
            builder.append(getString(R.string.check_ok))
        } else {
            builder.append(getString(R.string.check_failed, errors.joinToString("；")))
        }
        if (warnings.isNotEmpty()) {
            builder.append("\n").append(getString(R.string.check_warning, warnings.joinToString("；")))
        }
        materialCheckState.value = builder.toString()
        logDebug("check material result=${materialCheckState.value}")
        updateMissingVideoNotification()
    }

    private data class ExpectedResolution(val width: Int, val height: Int, val source: String?)

    private fun readExpectedResolution(): ExpectedResolution? {
        val file = File(getVideoDir() + "last_resolution.txt")
        if (!file.exists()) {
            return null
        }
        return try {
            val parts = file.readText().trim().split(",")
            if (parts.size < 2) {
                null
            } else {
                val width = parts[0].toIntOrNull()
                val height = parts[1].toIntOrNull()
                val source = if (parts.size > 2) parts[2] else null
                if (width == null || height == null) null else ExpectedResolution(width, height, source)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatExpectedResolution(expected: ExpectedResolution?): String {
        return if (expected == null) {
            getString(R.string.expected_resolution_missing)
        } else if (expected.source.isNullOrBlank()) {
            getString(R.string.expected_resolution, expected.width, expected.height)
        } else {
            getString(R.string.expected_resolution_with_source, expected.width, expected.height, expected.source)
        }
    }

    private fun generateVideoFromImage() {
        val uri = selectedImageUriState.value
        if (uri == null) {
            Toast.makeText(this, R.string.pick_image_first, Toast.LENGTH_SHORT).show()
            return
        }
        ensureVideoDirExists()
        generateState.value = getString(R.string.generate_in_progress)
        val inputFile = File(cacheDir, "vcam_source_image")
        if (!copyUriToFile(uri, inputFile)) {
            generateState.value = getString(R.string.generate_failed, "无法读取图片")
            logWarn("generate: failed to copy uri to temp file, uri=$uri")
            return
        }
        val imageSize = getImageResolution(uri)
        if (imageSize == null) {
            generateState.value = getString(R.string.generate_failed, "无法读取图片分辨率")
            logWarn("generate: failed to get image resolution, uri=$uri")
            return
        }
        var width = imageSize.width
        var height = imageSize.height
        val maxDimension = 1920
        if (width > maxDimension || height > maxDimension) {
            val scale = minOf(maxDimension.toDouble() / width, maxDimension.toDouble() / height)
            width = (width * scale).toInt()
            height = (height * scale).toInt()
        }
        if (width % 2 != 0) width += 1
        if (height % 2 != 0) height += 1
        val outputFile = File(getVideoDir() + "virtual.mp4")
        val command = buildFfmpegCommand(inputFile.absolutePath, outputFile.absolutePath, width, height, 1)
        logDebug("generate: target=${width}x${height} source=image cmd=$command")
        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            runOnUiThread {
                if (ReturnCode.isSuccess(returnCode)) {
                    generateState.value = getString(R.string.generate_ok, outputFile.absolutePath)
                    logDebug("generate: success output=${outputFile.absolutePath}")
                } else {
                    generateState.value = getString(R.string.generate_failed, returnCode.toString())
                    logWarn("generate: failed returnCode=$returnCode")
                }
                updateMissingVideoNotification()
            }
        }
    }

    private fun copyUriToFile(uri: Uri, dest: File): Boolean {
        return try {
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(dest).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getImageResolution(uri: Uri): ExpectedResolution? {
        return try {
            val options = android.graphics.BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            contentResolver.openInputStream(uri)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input, null, options)
            }
            if (options.outWidth > 0 && options.outHeight > 0) {
                ExpectedResolution(options.outWidth, options.outHeight, "image")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun buildFfmpegCommand(
        inputPath: String,
        outputPath: String,
        width: Int,
        height: Int,
        durationSeconds: Int
    ): String {
        val filter = "scale=${width}:${height}:flags=lanczos"
        return "-y -loop 1 -i \"$inputPath\" -t $durationSeconds -r 30 -vf \"$filter\" " +
            "-c:v libx264 -crf 18 -preset veryfast -pix_fmt yuv420p " +
            "-colorspace bt709 -color_primaries bt709 -color_trc bt709 -color_range pc " +
            "\"$outputPath\""
    }

    private fun ensureVideoDirExists() {
        val cameraDir = File(getVideoDir())
        if (!cameraDir.exists()) {
            cameraDir.mkdirs()
        }
    }

    private fun isDebugEnabled(): Boolean {
        return File(getVideoDir() + FileMode.DEBUG_LOG.fileName).exists()
    }

    private fun logDebug(message: String) {
        if (isDebugEnabled()) {
            Log.d(TAG, message)
        }
    }

    private fun logWarn(message: String, throwable: Throwable? = null) {
        if (isDebugEnabled()) {
            if (throwable == null) {
                Log.w(TAG, message)
            } else {
                Log.w(TAG, message, throwable)
            }
        }
    }

    private enum class FileMode(val fileName: String) {
        FORCE_SHOW("force_show.jpg"),
        DISABLE("disable.jpg"),
        PLAY_SOUND("no-silent.jpg"),
        FORCE_PRIVATE_DIR("private_dir.jpg"),
        DISABLE_TOAST("no_toast.jpg"),
        DEBUG_LOG("debug_log.jpg");
    }
}
