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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.ga.brainrush.ui.components.AppTopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitoredListScreen(onBack: () -> Unit, onNavigateToDetail: (String) -> Unit) {
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
    var showPicker by remember { mutableStateOf(false) }
    val pm = context.packageManager
    val installedPkgs = remember {
        // Ambil semua aplikasi yang memiliki launcher (ikon di home) agar terlihat oleh aturan package visibility Android 11+
        val launcherIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(launcherIntent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { pkg ->
                // Eksklusikan app sistem dan aplikasi sendiri
                val ai = try { pm.getApplicationInfo(pkg, 0) } catch (_: Exception) { null }
                ai != null && (ai.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 && pkg != context.packageName
            }
    }
    val availablePkgs = remember(packages) { installedPkgs.filter { it !in packages }.sortedBy { labelFor(it) } }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Aplikasi dengan Notifikasi",
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showPicker = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Tambah")
            }
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
                                        try {
                                            val intent = Intent(context, UsageMonitorService::class.java)
                                            context.stopService(intent)
                                        } catch (_: Exception) {}
                                    }
                                }) { Text("Hapus") }
                            }
                        }
                    }
                }
            }
        }
        if (showPicker) {
            AlertDialog(
                onDismissRequest = { showPicker = false },
                title = { Text("Pilih aplikasi untuk ditambahkan") },
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                        items(availablePkgs) { pkg ->
                            TextButton(onClick = {
                                showPicker = false
                                onNavigateToDetail(pkg)
                            }) { Text(labelFor(pkg)) }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPicker = false }) { Text("Tutup") }
                }
            )
        }
    }
}