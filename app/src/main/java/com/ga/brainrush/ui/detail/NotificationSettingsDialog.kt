package com.ga.brainrush.ui.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.ga.brainrush.alerts.NotificationModeStore
import com.ga.brainrush.alerts.UsageAlertScheduler
import com.ga.brainrush.alerts.UsageThresholdStore

@Composable
fun NotificationSettingsDialog(pkg: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    var selectedMode = remember { mutableStateOf(NotificationModeStore.getMode(context, pkg) ?: NotificationModeStore.MODE_IMMEDIATE) }
    var limitText = remember {
        val defaultInterval = (NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5).toString()
        val defaultThreshold = (UsageThresholdStore.getThreshold(context, pkg) ?: 60).toString()
        mutableStateOf(if (selectedMode.value == NotificationModeStore.MODE_INTERVAL) defaultInterval else defaultThreshold)
    }
    var inputError = remember { mutableStateOf<String?>(null) }
    var autoOpen = remember { mutableStateOf(NotificationModeStore.isAutoOpen(context, pkg)) }

    fun appLabelFor(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    AlertDialog(
        onDismissRequest = {
            onDismiss()
            inputError.value = null
        },
        title = { Text("Notifikasi penggunaan") },
        text = {
            Column {
                Text("Mode Notifikasi", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMode.value == NotificationModeStore.MODE_IMMEDIATE,
                        onClick = {
                            selectedMode.value = NotificationModeStore.MODE_IMMEDIATE
                            // Sinkronkan label menit dengan threshold default
                            limitText.value = (UsageThresholdStore.getThreshold(context, pkg) ?: 60).toString()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Dibatasi langsung")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedMode.value == NotificationModeStore.MODE_INTERVAL,
                        onClick = {
                            selectedMode.value = NotificationModeStore.MODE_INTERVAL
                            // Sinkronkan label menit dengan interval default
                            limitText.value = (NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5).toString()
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Setiap penggunaan")
                }
                Spacer(Modifier.height(8.dp))
                val label = if (selectedMode.value == NotificationModeStore.MODE_INTERVAL) {
                    "Set batas menit per penggunaan untuk ${appLabelFor(pkg)}"
                } else {
                    "Set batas menit per hari untuk ${appLabelFor(pkg)}"
                }
                Text(label)
                TextField(
                    value = limitText.value,
                    onValueChange = {
                        limitText.value = it.filter { ch -> ch.isDigit() }.take(4)
                    },
                    label = { Text("Menit") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = autoOpen.value,
                        onCheckedChange = { autoOpen.value = it }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Buka Brainrush otomatis setelah notifikasi muncul")
                }
                inputError.value?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = limitText.value.toIntOrNull()
                if (v == null || v <= 0) {
                    inputError.value = "Masukkan menit > 0"
                } else {
                    if (selectedMode.value == NotificationModeStore.MODE_INTERVAL) {
                        NotificationModeStore.setMode(context, pkg, NotificationModeStore.MODE_INTERVAL)
                        NotificationModeStore.setIntervalMinutes(context, pkg, v)
                    } else {
                        UsageThresholdStore.setThreshold(context, pkg, v)
                        NotificationModeStore.setMode(context, pkg, NotificationModeStore.MODE_IMMEDIATE)
                    }
                    // Simpan preferensi auto-open
                    NotificationModeStore.setAutoOpen(context, pkg, autoOpen.value)

                    // Pastikan WorkManager fallback aktif
                    UsageAlertScheduler.ensurePeriodicWork(context)

                    // Mulai Foreground Service segera jika ada paket dimonitor & izin usage tersedia
                    try {
                        val hasMonitored = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL).isNotEmpty() ||
                                UsageThresholdStore.getAllThresholds(context).isNotEmpty()
                        if (hasMonitored && com.ga.brainrush.data.util.UsageStatsHelper.hasUsagePermission(context)) {
                            androidx.core.content.ContextCompat.startForegroundService(
                                context,
                                android.content.Intent(context, com.ga.brainrush.alerts.UsageMonitorService::class.java)
                            )
                        }
                    } catch (_: Exception) {}

                    inputError.value = null
                    onDismiss()
                }
            }) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss(); inputError.value = null }) { Text("Batal") }
        }
    )
}