package com.youtubebridge.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.youtubebridge.app.model.LogEntry
import com.youtubebridge.app.model.ServerUiState
import com.youtubebridge.app.ui.theme.GreenOnline
import com.youtubebridge.app.ui.theme.RedOffline
import com.youtubebridge.app.util.QrCodeGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: ServerUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onPortChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    onUpdateExtractor: () -> Unit
) {
    val context = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    val serverUrl = "http://${state.ipAddress}:${state.port}"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("YouTube Bridge", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "الإعدادات")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            StatusCard(state, serverUrl)

            Spacer(Modifier.height(16.dp))

            ControlButtons(
                isRunning = state.isRunning,
                onStart = onStart,
                onStop = onStop,
                onRestart = onRestart,
                onCopy = { copyToClipboard(context, serverUrl) },
                onShare = { shareUrl(context, serverUrl) },
                onQr = { showQr = true }
            )

            Spacer(Modifier.height(16.dp))

            Text("السجل (Logs)", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LogsList(state.logs, modifier = Modifier.weight(1f))
        }
    }

    if (showSettings) {
        SettingsSheet(
            state = state,
            onDismiss = { showSettings = false },
            onPortChange = onPortChange,
            onAutoStartChange = onAutoStartChange,
            onDarkModeChange = onDarkModeChange,
            onClearLogs = onClearLogs,
            onUpdateExtractor = onUpdateExtractor
        )
    }

    if (showQr) {
        QrDialog(url = serverUrl, onDismiss = { showQr = false })
    }
}

@Composable
private fun StatusCard(state: ServerUiState, serverUrl: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (state.isRunning) GreenOnline else RedOffline)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (state.isRunning) "Running" else "Stopped",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(16.dp))
            InfoRow("عنوان IP", state.ipAddress)
            InfoRow("المنفذ", state.port.toString())
            InfoRow("عدد الطلبات", state.requestCount.toString())
            InfoRow("الأجهزة المتصلة", state.connectedClients.toString())
            InfoRow("الرابط الكامل", serverUrl)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ControlButtons(
    isRunning: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRestart: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onQr: () -> Unit
) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = onStart, enabled = !isRunning, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("تشغيل")
            }
            Button(
                onClick = onStop,
                enabled = isRunning,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("إيقاف")
            }
            OutlinedButton(onClick = onRestart, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Restart")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("نسخ IP")
            }
            OutlinedButton(onClick = onShare, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("مشاركة")
            }
            OutlinedButton(onClick = onQr, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.QrCode, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("QR")
            }
        }
    }
}

@Composable
private fun LogsList(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    val fmt = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text("لا توجد سجلات بعد", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(Modifier.padding(8.dp)) {
                items(logs.reversed()) { log ->
                    Text(
                        "[${fmt.format(Date(log.timestamp))}] ${log.message}",
                        color = if (log.isError) RedOffline else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    state: ServerUiState,
    onDismiss: () -> Unit,
    onPortChange: (Int) -> Unit,
    onAutoStartChange: (Boolean) -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
    onUpdateExtractor: () -> Unit
) {
    var portText by remember { mutableStateOf(state.port.toString()) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(20.dp).padding(bottom = 32.dp)) {
            Text("الإعدادات", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            SettingSwitchRow("تشغيل تلقائي", state.autoStart, onAutoStartChange)
            SettingSwitchRow("الوضع الليلي", state.darkMode, onDarkModeChange)

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { c -> c.isDigit() }.take(5) },
                label = { Text("المنفذ (Port)") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = { portText.toIntOrNull()?.let(onPortChange) }) {
                        Text("حفظ")
                    }
                }
            )

            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onClearLogs, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("حذف السجل")
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onUpdateExtractor, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Update, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("تحديث أداة استخراج روابط YouTube")
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "ملاحظة: تحديث الأداة يتم عبر رفع نسخة تطبيق جديدة تحتوي على أحدث إصدار من مكتبة " +
                    "NewPipeExtractor، لأن الاستخراج مكتبة مُجمّعة داخل التطبيق وليست عملية خارجية.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QrDialog(url: String, onDismiss: () -> Unit) {
    val bitmap = remember(url) { QrCodeGenerator.generate(url) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("إغلاق") } },
        title = { Text("امسح الرمز للاتصال") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR",
                    modifier = Modifier.size(240.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(url, textAlign = TextAlign.Center)
            }
        }
    )
}

private fun copyToClipboard(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("YouTube Bridge URL", text))
}

private fun shareUrl(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "مشاركة رابط السيرفر"))
}
