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

class MainActivity : ComponentActivity() {

    companion object {
        private const val PREFS_NAME = "vcam_prefs"
        private const val KEY_VIDEO_DIR = "video_dir"
        private const val KEY_TREE_URI = "video_dir_tree_uri"
        private const val DEFAULT_VIDEO_DIR = "/storage/emulated/0/Download/Camera1/"
        private const val CHANNEL_ID = "vcam_status"
        private const val NOTIFICATION_ID = 1001
    }

    private lateinit var pickDirLauncher: ActivityResultLauncher<Uri?>

    private val forceShowState = mutableStateOf(false)
    private val disableState = mutableStateOf(false)
    private val playSoundState = mutableStateOf(false)
    private val forcePrivateDirState = mutableStateOf(false)
    private val disableToastState = mutableStateOf(false)
    private val videoDirState = mutableStateOf(DEFAULT_VIDEO_DIR)
    private val materialCheckState = mutableStateOf("")

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
            Text(
                text = stringResource(id = R.string.description),
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(id = R.string.Warning),
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { openUrl("https://github.com/w2016561536/android_virtual_cam") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.click_to_go_to_repo))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { openUrl("https://gitee.com/w2016561536/android_virtual_cam") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(id = R.string.gitee))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "当前目录: ${videoDirState.value}")
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { pickDirLauncher.launch(null) }) {
                    Text(text = "选择目录")
                }
                Button(onClick = { openVideoDir() }) {
                    Text(text = "打开目录")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { checkMaterial() }, modifier = Modifier.fillMaxWidth()) {
                Text(text = "检测素材")
            }
            if (materialCheckState.value.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = materialCheckState.value)
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

    private fun openUrl(url: String) {
        val uri = Uri.parse(url)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun handlePickedDir(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (e: SecurityException) {
            Log.w(application.packageName, "VCAM failed to persist uri permission", e)
        }

        val path = treeUriToPath(uri)
        if (path == null) {
            Toast.makeText(this, "仅支持主存储目录", Toast.LENGTH_SHORT).show()
            return
        }
        setVideoDir(path, uri)
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
        makePrefsReadable()
        videoDirState.value = normalizeDir(dir)
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
        Log.d(application.packageName, "【VCAM】[sync]同步开关状态")

        if (!hasPermission()) {
            requestPermission()
        } else {
            ensureVideoDirExists()
        }

        makePrefsReadable()
        videoDirState.value = getVideoDir()
        forceShowState.value = File(getVideoDir() + FileMode.FORCE_SHOW.fileName).exists()
        disableState.value = File(getVideoDir() + FileMode.DISABLE.fileName).exists()
        playSoundState.value = File(getVideoDir() + FileMode.PLAY_SOUND.fileName).exists()
        forcePrivateDirState.value = File(getVideoDir() + FileMode.FORCE_PRIVATE_DIR.fileName).exists()
        disableToastState.value = File(getVideoDir() + FileMode.DISABLE_TOAST.fileName).exists()
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
            .setContentTitle("未找到 virtual.mp4")
            .setContentText("当前目录: ${getVideoDir()}")
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
                if (expected == null) {
                    warnings.add("未检测到分辨率提示文件")
                } else if (width != null && height != null &&
                    (width != expected.first || height != expected.second)
                ) {
                    errors.add("分辨率不匹配")
                    warnings.add("期望 ${expected.first}x${expected.second}，当前 ${width}x${height}")
                }
            } catch (e: Exception) {
                errors.add("读取视频失败：${e.message}")
            } finally {
                retriever.release()
            }
        }

        val builder = StringBuilder()
        if (errors.isEmpty()) {
            builder.append("检测通过")
        } else {
            builder.append("检测失败：").append(errors.joinToString("；"))
        }
        if (warnings.isNotEmpty()) {
            builder.append("\n警告：").append(warnings.joinToString("；"))
        }
        materialCheckState.value = builder.toString()
        updateMissingVideoNotification()
    }

    private fun readExpectedResolution(): Pair<Int, Int>? {
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
                if (width == null || height == null) null else width to height
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun ensureVideoDirExists() {
        val cameraDir = File(getVideoDir())
        if (!cameraDir.exists()) {
            cameraDir.mkdirs()
        }
    }

    private enum class FileMode(val fileName: String) {
        FORCE_SHOW("force_show.jpg"),
        DISABLE("disable.jpg"),
        PLAY_SOUND("no-silent.jpg"),
        FORCE_PRIVATE_DIR("private_dir.jpg"),
        DISABLE_TOAST("no_toast.jpg");
    }
}
