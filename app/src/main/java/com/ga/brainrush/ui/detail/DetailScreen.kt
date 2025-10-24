package com.ga.brainrush.ui.detail

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ga.brainrush.alerts.NotificationHelper
import com.ga.brainrush.alerts.NotificationModeStore
import com.ga.brainrush.alerts.UsageThresholdStore
import com.ga.brainrush.data.util.UsageStatsHelper
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.Line
import android.content.Intent
import androidx.core.content.ContextCompat
import com.ga.brainrush.alerts.UsageMonitorService

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

    // Tampilkan mode notifikasi terpilih sebagai teks ringkas
    fun modeText(): String {
        val mode = NotificationModeStore.getMode(context, pkg)
        return if (mode == null) {
            "Mode: Tidak disetel"
        } else if (mode == NotificationModeStore.MODE_INTERVAL) {
            val minutes = NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5
            "Mode: Setiap penggunaan (${minutes}m)"
        } else {
            "Mode: Dibatasi langsung"
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
    var thresholdText by remember {
        mutableStateOf(UsageThresholdStore.getThreshold(context, pkg)?.toString() ?: "")
    }
    var inputError by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var saveInfo by remember { mutableStateOf<String?>(null) }

    // Mode notifikasi & interval
    var selectedMode by remember {
        mutableStateOf(
            NotificationModeStore.getMode(context, pkg) ?: NotificationModeStore.MODE_IMMEDIATE
        )
    }
    var intervalText by remember {
        mutableStateOf((NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5).toString())
    }

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
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        Column(
            modifier =
                Modifier.padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
        ) {
            Text(
                pkg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // Toggle Hari/Minggu (di atas grafik)
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
                Text(
                    "Zoom",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = zoomFactor,
                    onValueChange = { zoomFactor = it.coerceIn(1f, 3f) },
                    valueRange = 1f..3f
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "x${String.format(java.util.Locale.getDefault(), "%.1f", zoomFactor)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Spacer(Modifier.height(8.dp))

            // Grafik di atas
            val screenWidth = LocalConfiguration.current.screenWidthDp
            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                LineChart(
                    modifier = Modifier.width((screenWidth * zoomFactor).dp).height(220.dp),
                    data =
                        listOf(
                            Line(
                                label = labelFor(pkg),
                                values =
                                    if (chartRange == ChartRange.Day)
                                        seriesHourly
                                    else seriesWeek,
                                color = SolidColor(Color(0xFF23af92))
                            )
                        )
                )
            }
            Spacer(Modifier.height(10.dp))
            // Label sumbu-X
            if (chartRange == ChartRange.Day) {
                val ticks = listOf("0", "3", "6", "9", "12", "15", "18", "21")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ticks.forEach { t ->
                        Text(
                            t,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val labels = lastNDaysAbbrev(7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    labels.forEach { d ->
                        Text(
                            d,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // Actions: Set Notifikasi (dipindah ke bawah)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showDialog = true }) { Text("Set Notifikasi") }
                    Spacer(Modifier.width(12.dp))
                    val current = UsageThresholdStore.getThreshold(context, pkg)
                    Text(
                        if (current != null) "Batas: ${current}menit" else "Belum ada batas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    modeText(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { saveInfo = "Tersimpan" }) { Text("Simpan") }
                OutlinedButton(
                    onClick = { NotificationHelper.showTestNotification(context, pkg) }
                ) { Text("Test") }
                OutlinedButton(
                    onClick = {
                        // Hapus pengaturan untuk paket ini
                        UsageThresholdStore.removeThreshold(context, pkg)
                        NotificationModeStore.clearPackage(context, pkg)
                        // Update UI state
                        thresholdText = ""
                        selectedMode = NotificationModeStore.getMode(context, pkg)
                            ?: NotificationModeStore.MODE_IMMEDIATE
                        intervalText = (NotificationModeStore.getIntervalMinutes(context, pkg) ?: 5).toString()
                        saveInfo = "Pengaturan notifikasi dihapus"
                        // Hentikan service jika tidak ada paket dimonitor
                        val hasMonitored = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL).isNotEmpty() ||
                                UsageThresholdStore.getAllThresholds(context).isNotEmpty()
                        if (!hasMonitored) {
                            context.stopService(Intent(context, UsageMonitorService::class.java))
                        }
                    }
                ) { Text("Hapus") }
            }
            if (saveInfo != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    saveInfo!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (showDialog) {
            NotificationSettingsDialog(
                pkg = pkg,
                onDismiss = {
                    showDialog = false
                    inputError = null
                }
            )
        }
    }
}

enum class ChartRange {
    Day,
    Week
}
