package com.ga.brainrush.ui.monitor

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ga.brainrush.alerts.NotificationModeStore
import com.ga.brainrush.alerts.UsageThresholdStore
import com.ga.brainrush.alerts.UsageMonitorService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoredListScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    fun labelFor(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    fun buildDisplayInfo(pkg: String): String {
        val mode = NotificationModeStore.getMode(context, pkg)
        val interval = NotificationModeStore.getIntervalMinutes(context, pkg)
        val threshold = UsageThresholdStore.getThreshold(context, pkg)
        val parts = mutableListOf<String>()
        if (mode == NotificationModeStore.MODE_INTERVAL && interval != null) {
            parts += "Interval: ${interval}m"
        }
        if (threshold != null) {
            parts += "Batas harian: ${threshold}m"
        }
        return if (parts.isEmpty()) "Tidak disetel" else parts.joinToString(" • ")
    }

    var packages by remember {
        mutableStateOf(run {
            val intervalPkgs = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL)
            val thresholdPkgs = UsageThresholdStore.getAllThresholds(context).keys
            (intervalPkgs + thresholdPkgs).toSet().toList().sorted()
        })
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aplikasi dengan Notifikasi", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { TextButton(onClick = onBack) { Text("Kembali") } }
            )
        }
    ) { padding ->
        if (packages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Belum ada aplikasi dengan notifikasi", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Set notifikasi dari halaman detail aplikasi",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(20.dp)
            ) {
                items(packages, key = { it }) { pkg ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(labelFor(pkg), style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(4.dp))
                            Text(pkg, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(buildDisplayInfo(pkg), style = MaterialTheme.typography.labelSmall)
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedButton(onClick = {
                                    UsageThresholdStore.removeThreshold(context, pkg)
                                    NotificationModeStore.clearPackage(context, pkg)
                                    packages = packages.filter { it != pkg }
                                    val hasMonitored = NotificationModeStore.getPackagesByMode(context, NotificationModeStore.MODE_INTERVAL).isNotEmpty() ||
                                        UsageThresholdStore.getAllThresholds(context).isNotEmpty()
                                    if (!hasMonitored) {
                                        context.stopService(Intent(context, UsageMonitorService::class.java))
                                    }
                                }) { Text("Hapus") }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}