package com.ga.brainrush

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.ga.brainrush.ui.home.HomeScreen
import com.ga.brainrush.ui.theme.BrainrushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BrainrushTheme {
                var showStats by remember { mutableStateOf(false) }
                    HomeScreen(onNavigateToStats = { showStats = true })
            }
        }
    }
}
