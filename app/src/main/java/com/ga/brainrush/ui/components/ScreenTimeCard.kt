package com.ga.brainrush.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenTimeCard(screenTime: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("Waktu Layar Hari Ini", fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(screenTime, fontSize = 28.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}
