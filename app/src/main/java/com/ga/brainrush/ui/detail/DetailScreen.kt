package com.ga.brainrush.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ga.brainrush.alerts.UsageAlertScheduler
import com.ga.brainrush.alerts.UsageThresholdStore
import com.ga.brainrush.data.util.UsageStatsHelper
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.Line
import com.ga.brainrush.alerts.NotificationSettingsStore
import com.ga.brainrush.alerts.NotificationHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(pkg: String, onBack: () -> Unit) {
    val context = LocalContext.current

    // Helper label
    fun labelFor(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    var chartRange by remember { mutableStateOf(ChartRange.Day) }
    var zoomFactor by remember { mutableStateOf(1f) }
    var seriesWeek by remember { mutableStateOf<List<Double>>(emptyList()) }
    var seriesHourly by remember { mutableStateOf<List<Double>>(emptyList()) }

    // Load data
    LaunchedEffect(pkg) {
        seriesWeek = UsageStatsHelper.getUsageLastNDays(context, pkg, 7)
        seriesHourly = UsageStatsHelper.getUsageTodayHourly(context, pkg)
        chartRange = ChartRange.Day
    }

    // Notification dialog state
    var thresholdText by remember { mutableStateOf(UsageThresholdStore.getThreshold(context, pkg)?.toString() ?: "") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    // Pengaturan template notifikasi
    var titleTemplate by remember { mutableStateOf(NotificationSettingsStore.getTitleTemplate(context) ?: "Batas penggunaan tercapai") }
    var messageTemplate by remember { mutableStateOf(NotificationSettingsStore.getMessageTemplate(context) ?: "{appLabel} telah digunakan {minutes} menit (batas: {threshold})") }
    var saveInfo by remember { mutableStateOf<String?>(null) }

    fun lastNDaysAbbrev(n: Int): List<String> {
        val cal = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val days = mutableListOf<String>()
        for (i in n - 1 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis() - i * 24L * 60L * 60L * 1000L
            val d = sdf.format(cal.time)
            days += d
        }
        return days
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(labelFor(pkg)) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Kembali") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))

            // Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { showDialog = true }) { Text("Set Notifikasi") }
                Spacer(Modifier.width(12.dp))
                val current = UsageThresholdStore.getThreshold(context, pkg)
                Text(
                    if (current != null) "Batas: ${current}m/hari" else "Belum ada batas",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Pengaturan Notifikasi (judul & isi)
            Text("Pengaturan Notifikasi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            TextField(
                value = titleTemplate,
                onValueChange = { titleTemplate = it.take(80) },
                label = { Text("Judul Notifikasi") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            TextField(
                value = messageTemplate,
                onValueChange = { messageTemplate = it.take(200) },
                label = { Text("Isi Notifikasi") }
            )
            Spacer(Modifier.height(6.dp))
            Text("Placeholder: {appLabel}, {packageName}, {minutes}, {threshold}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            val previewText = messageTemplate
                .replace("{appLabel}", labelFor(pkg))
                .replace("{packageName}", pkg)
                .replace("{minutes}", "42")
                .replace("{threshold}", "60")
            Text("Preview: $previewText", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    NotificationSettingsStore.setTitleTemplate(context, titleTemplate)
                    NotificationSettingsStore.setMessageTemplate(context, messageTemplate)
                    saveInfo = "Tersimpan"
                }) { Text("Simpan") }
                OutlinedButton(onClick = { NotificationHelper.showTestNotification(context, pkg) }) { Text("Kirim Notifikasi Tes") }
            }
            if (saveInfo != null) {
                Spacer(Modifier.height(4.dp))
                Text(saveInfo!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(16.dp))
            // Toggle Hari/Minggu
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = chartRange == ChartRange.Day,
                    onClick = { chartRange = ChartRange.Day },
                    label = { Text("Hari") }
                )
                FilterChip(
                    selected = chartRange == ChartRange.Week,
                    onClick = { chartRange = ChartRange.Week },
                    label = { Text("Minggu") }
                )
            }
            Spacer(Modifier.height(8.dp))

            // Kontrol Zoom
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Zoom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = zoomFactor,
                    onValueChange = { zoomFactor = it.coerceIn(1f, 3f) },
                    valueRange = 1f..3f
                )
                Spacer(Modifier.width(8.dp))
                Text("x${String.format(java.util.Locale.getDefault(), "%.1f", zoomFactor)}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(8.dp))

            // Chart
            val screenWidth = LocalConfiguration.current.screenWidthDp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                LineChart(
                    modifier = Modifier
                        .width((screenWidth * zoomFactor).dp)
                        .height(220.dp),
                    data = listOf(
                        Line(
                            label = labelFor(pkg),
                            values = if (chartRange == ChartRange.Day) seriesHourly else seriesWeek,
                            color = SolidColor(Color(0xFF23af92))
                        )
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
            // Label sumbu-X
            if (chartRange == ChartRange.Day) {
                val ticks = listOf("0","3","6","9","12","15","18","21")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ticks.forEach { t ->
                        Text(t, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                val labels = lastNDaysAbbrev(7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { d ->
                        Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false; inputError = null },
                title = { Text("Notifikasi penggunaan") },
                text = {
                    Column {
                        Text("Set batas menit per hari untuk ${labelFor(pkg)}")
                        Spacer(Modifier.height(12.dp))
                        TextField(
                            value = thresholdText,
                            onValueChange = { thresholdText = it.filter { ch -> ch.isDigit() }.take(4) },
                            label = { Text("Menit per hari") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        if (inputError != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(inputError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val v = thresholdText.toIntOrNull()
                        if (v == null || v <= 0) {
                            inputError = "Masukkan menit > 0"
                        } else {
                            UsageThresholdStore.setThreshold(context, pkg, v)
                            UsageAlertScheduler.ensurePeriodicWork(context)
                            showDialog = false
                            inputError = null
                        }
                    }) { Text("Simpan") }
                },
                dismissButton = { TextButton(onClick = { showDialog = false; inputError = null }) { Text("Batal") } }
            )
        }
    }
}

// Reuse enum
enum class ChartRange { Day, Week }