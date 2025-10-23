package com.ga.brainrush.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ga.brainrush.ui.components.ScreenTimeCard


@Composable
fun HomeScreen(onNavigateToStats: () -> Unit){
    var screenTime by remember { mutableStateOf("2 jam 15 menit") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column (
            modifier =Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ){
            Text(
                text = "🧠 Dopamine Dashboard",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(24.dp))
            ScreenTimeCard(screenTime = screenTime)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                onNavigateToStats()
            }) {
                Text("Lihat Statistik Dopamin")
            }

        }

    }
}