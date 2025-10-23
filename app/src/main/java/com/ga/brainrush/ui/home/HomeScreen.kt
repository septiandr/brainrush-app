package com.ga.brainrush.ui.home

import android.annotation.SuppressLint
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ga.brainrush.data.model.ScreenTimeEntity
import com.ga.brainrush.domain.repository.ScreenTimeRepository
import com.ga.brainrush.ui.components.ScreenTimeCard
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDataFormat")
@Composable
fun HomeScreen(onNavigateToStats: () -> Unit){
    val context = LocalContext.current
    val repo = remember { ScreenTimeRepository.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()

    var screenTime by remember { mutableStateOf(0) }

    val today = SimpleDateFormat("yyy-MM-dd").format(Date())

    LaunchedEffect(Unit) {
        val existing = repo.getToday(today)
        if(existing == null) {
            repo.insertScreenTime(ScreenTimeEntity(date =today, totalMinutes = 135))
        }
        val data = repo.getToday(today)
        screenTime = data?.totalMinutes ?: 0
    }

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
            ScreenTimeCard(screenTime = "${screenTime / 60} jam ${screenTime % 60} menit" )
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = {
                onNavigateToStats()
            }) {
                Text("Lihat Statistik Dopamin")
            }

        }

    }
}