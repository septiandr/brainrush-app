package com.ga.brainrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.ga.brainrush.alerts.NotificationHelper
import com.ga.brainrush.alerts.UsageAlertScheduler
import com.ga.brainrush.ui.detail.DetailScreen
import com.ga.brainrush.ui.home.HomeScreen
import com.ga.brainrush.ui.theme.BrainrushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Pasang SplashScreen seawal mungkin
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Inisialisasi channel notifikasi & scheduler periodic usage check
        NotificationHelper.createChannels(this)
        UsageAlertScheduler.ensurePeriodicWork(this)
        setContent {
            BrainrushTheme {
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
