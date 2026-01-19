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
import androidx.activity.ComponentActivity
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
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val forceShowState = mutableStateOf(false)
    private val disableState = mutableStateOf(false)
    private val playSoundState = mutableStateOf(false)
    private val forcePrivateDirState = mutableStateOf(false)
    private val disableToastState = mutableStateOf(false)

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
                val cameraDir = File(
                    Environment.getExternalStorageDirectory().absolutePath + "/DCIM/Camera1/"
                )
                if (!cameraDir.exists()) {
                    cameraDir.mkdir()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        syncStateWithFiles()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
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

    private fun updateToggle(mode: FileMode, enabled: Boolean) {
        if (!hasPermission()) {
            requestPermission()
            return
        }
        val file = File(
            Environment.getExternalStorageDirectory().absolutePath + "/DCIM/Camera1/" + mode.fileName
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
            val cameraDir = File(
                Environment.getExternalStorageDirectory().absolutePath + "/DCIM/Camera1"
            )
            if (!cameraDir.exists()) {
                cameraDir.mkdir()
            }
        }

        forceShowState.value = FileMode.FORCE_SHOW.file.exists()
        disableState.value = FileMode.DISABLE.file.exists()
        playSoundState.value = FileMode.PLAY_SOUND.file.exists()
        forcePrivateDirState.value = FileMode.FORCE_PRIVATE_DIR.file.exists()
        disableToastState.value = FileMode.DISABLE_TOAST.file.exists()
    }

    private enum class FileMode(val fileName: String) {
        FORCE_SHOW("force_show.jpg"),
        DISABLE("disable.jpg"),
        PLAY_SOUND("no-silent.jpg"),
        FORCE_PRIVATE_DIR("private_dir.jpg"),
        DISABLE_TOAST("no_toast.jpg");

        val file: File
            get() = File(
                Environment.getExternalStorageDirectory().absolutePath + "/DCIM/Camera1/" + fileName
            )
    }
}
