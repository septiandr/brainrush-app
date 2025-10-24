package com.ga.brainrush

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ga.brainrush.alerts.NotificationHelper
import com.ga.brainrush.alerts.UsageAlertScheduler
import com.ga.brainrush.ui.detail.DetailScreen
import com.ga.brainrush.ui.home.HomeScreen
import com.ga.brainrush.ui.theme.BrainrushTheme
import com.ga.brainrush.alerts.NotificationOpenHelper
import com.ga.brainrush.data.util.UsageStatsHelper
import com.ga.brainrush.alerts.NotificationModeStore
import com.ga.brainrush.alerts.UsageThresholdStore
import android.content.Intent
import com.ga.brainrush.alerts.UsageMonitorService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Pasang SplashScreen seawal mungkin
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Inisialisasi channel notifikasi & scheduler periodic usage check
        NotificationHelper.createChannels(this)
        UsageAlertScheduler.ensurePeriodicWork(this)
        // Trigger notifikasi saat aplikasi dibuka sesuai mode
        if (UsageStatsHelper.hasUsagePermission(this)) {
            NotificationOpenHelper.notifyOnAppOpen(this)
            // Mulai Foreground Service jika ada paket yang dimonitor
            val hasMonitored = NotificationModeStore.getPackagesByMode(this, NotificationModeStore.MODE_INTERVAL).isNotEmpty() ||
                    UsageThresholdStore.getAllThresholds(this).isNotEmpty()
            if (hasMonitored) {
                ContextCompat.startForegroundService(this, Intent(this, UsageMonitorService::class.java))
            }
        }
        setContent {
            BrainrushTheme {
                // Request POST_NOTIFICATIONS on Android 13+
                val context = LocalContext.current
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val launcher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* granted -> no-op */ }
                    LaunchedEffect(Unit) {
                        val granted = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                var detailPkg by remember { mutableStateOf<String?>(null) }
                if (detailPkg == null) {
                    HomeScreen(onNavigateToDetail = { pkg -> detailPkg = pkg })
                } else {
                    DetailScreen(pkg = detailPkg!!, onBack = { detailPkg = null })
                }
            }
        }
    }
}
