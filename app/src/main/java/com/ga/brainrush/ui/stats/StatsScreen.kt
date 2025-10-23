package com.ga.brainrush.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp



@Composable
fun StatsScreen() {
    val dopamineData = listOf(
        DopamineCategory("High Stim", 50f, Color(0xFFE53935)),
        DopamineCategory("Medium Stim", 30f, Color(0xFFFFB300)), // Chat, musik
        DopamineCategory("Low Stim", 20f, Color(0xFF43A047))
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📊 Statistik Dopamin",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(24.dp)
            )
            DopaminePieChart(data = dopamineData)
            Spacer(modifier = Modifier.height(32.dp))
            dopamineData.forEach {
                Text(
                    text = "${it.label}: ${it.value.toInt()}%",
                    fontSize = 16.sp,
                    color = it.color
                )
            }

        }
    }
}


data class DopamineCategory(
    val label:String,
    val value: Float,
    val color:Color
)