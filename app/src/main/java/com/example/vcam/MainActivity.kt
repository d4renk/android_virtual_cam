package com.example.vcam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initial = loadSettings(this)
        setContent {
            val colors = lightColorScheme()
            var settings by remember { mutableStateOf(initial) }
            Material3Theme(colorScheme = colors) {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text(text = getString(R.string.app_name)) })
                    }
                ) { padding ->
                    SettingsScreen(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        settings = settings,
                        onSettingsChange = {
                            settings = it
                            saveSettings(this, it)
                        },
                        onOpenLink = { openLink(it) }
                    )
                }
            }
        }
    }

    private fun openLink(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    settings: VcamSettings,
    onSettingsChange: (VcamSettings) -> Unit,
    onOpenLink: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatusCard(settings)
        OptionCard(
            title = "配置项",
            items = listOf(
                ToggleItem(
                    title = "启用模块",
                    subtitle = "关闭后虚拟摄像头将不拦截相机",
                    checked = settings.enabled,
                    onToggle = { onSettingsChange(settings.copy(enabled = it)) }
                ),
                ToggleItem(
                    title = "强制私有目录",
                    subtitle = "在私有 Download/Camera1 目录下读取虚拟素材",
                    checked = settings.forcePrivateStorage,
                    onToggle = { onSettingsChange(settings.copy(forcePrivateStorage = it)) }
                ),
                ToggleItem(
                    title = "启用快门音",
                    subtitle = "拍照时播放提示音",
                    checked = settings.enableShutterSound,
                    onToggle = { onSettingsChange(settings.copy(enableShutterSound = it)) }
                ),
                ToggleItem(
                    title = "保留提示",
                    subtitle = "强制提示存储路径与活跃状态",
                    checked = settings.forceShowTips,
                    onToggle = { onSettingsChange(settings.copy(forceShowTips = it)) }
                ),
                ToggleItem(
                    title = "静默模式",
                    subtitle = "抑制 Toast 提示",
                    checked = settings.suppressToast,
                    onToggle = { onSettingsChange(settings.copy(suppressToast = it)) }
                )
            )
        )

        MediaCard(path = mediaRootPath(context))

        OptionCard(
            title = "关于",
            items = listOf(
                TextItem(title = "版本", subtitle = "1.1.1(11)", icon = Icons.Default.Info),
                LinkItem(title = "GitHub Issues", url = "https://github.com/w2016561536/android_virtual_cam/issues"),
                LinkItem(title = "CoolApk", url = "https://www.coolapk.com/u/3304540")
            ),
            onOpenLink = onOpenLink
        )
    }
}

@Composable
private fun StatusCard(settings: VcamSettings) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "设置", style = Material3Theme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(color = MaterialTheme.colors.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = if (settings.enabled) "已激活" else "已停用",
                        color = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Text(text = "1.1.1(11)", style = MaterialTheme.typography.subtitle1)
            }
        }
    }
}

@Composable
private fun MediaCard(path: String) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(imageVector = Icons.Default.PhotoCamera, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "拍摄位置", style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
                Text(text = path, style = MaterialTheme.typography.body2)
            }
        }
    }
}

private data class ToggleItem(
    val title: String,
    val subtitle: String,
    val checked: Boolean,
    val onToggle: (Boolean) -> Unit
)

private data class TextItem(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private data class LinkItem(
    val title: String,
    val url: String
)

@Composable
private fun OptionCard(
    title: String,
    items: List<Any>,
    onOpenLink: ((String) -> Unit)? = null
) {
    ElevatedCard(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = title, style = Material3Theme.typography.titleMedium, fontWeight = FontWeight.Bold)
            items.forEach { item ->
                when (item) {
                    is ToggleItem -> ToggleRow(item)
                    is TextItem -> InfoRow(item)
                    is LinkItem -> LinkRow(item, onOpenLink)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(item: ToggleItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Text(text = item.subtitle, style = MaterialTheme.typography.body2)
        }
        Switch(checked = item.checked, onCheckedChange = item.onToggle)
    }
    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
private fun InfoRow(item: TextItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = item.icon, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Text(text = item.subtitle, style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
private fun LinkRow(item: LinkItem, onOpenLink: ((String) -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenLink?.invoke(item.url) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = Icons.Default.Link, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.title, style = MaterialTheme.typography.subtitle1, fontWeight = FontWeight.Bold)
            Text(text = item.url, style = MaterialTheme.typography.body2)
        }
    }
}
