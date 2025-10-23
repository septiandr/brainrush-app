package com.ga.brainrush.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DopaminePieChart(
    data:List<DopamineCategory>,
    modifier:Modifier = Modifier,
    size: Int = 220
){
    val total = data.sumOf { it.value.toDouble()}.toFloat()
    var startAngle = -90f
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ){
        Canvas(modifier = modifier.fillMaxSize()) {
            data.forEach{ category ->
                val sweepAngle = 360 * (category.value / total)
                drawArc(
                    color = category.color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.toFloat(), size.toFloat())
                )
                startAngle += sweepAngle
            }
        }

        Text(
            text = "Balance",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}