package com.ga.brainrush.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ga.brainrush.data.util.UsageStatsHelper
import com.ga.brainrush.domain.repository.ScreenTimeRepository
import java.util.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalConfiguration
import ir.ehsannarmani.compose_charts.LineChart
import ir.ehsannarmani.compose_charts.models.Line
import java.text.SimpleDateFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToStats: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { ScreenTimeRepository.getInstance(context) }
    val scope = rememberCoroutineScope()
    var totalToday by remember { mutableStateOf<Int?>(null) }
    var debugText by remember { mutableStateOf<String?>(null) }
    var todayUsage by remember { mutableStateOf<Map<String, Long>>(emptyMap()) }
    val allData by repo.getAllScreenTimes().collectAsState(initial = emptyList())
    var selectedPkg by remember { mutableStateOf<String?>(null) }
    var selectedSeries by remember { mutableStateOf<List<Double>>(emptyList()) }
    val listState = rememberLazyListState()
    // Tambahan: state untuk mode chart, data per jam, dan zoom
    var chartRange by remember { mutableStateOf(ChartRange.Week) }
    var selectedHourly by remember { mutableStateOf<List<Double>>(emptyList()) }
    var zoomFactor by remember { mutableStateOf(1f) }

    // Helper label hari (singkatan) untuk 7 hari terakhir
    fun lastNDaysAbbrev(n: Int): List<String> {
        val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        val calStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            add(java.util.Calendar.DAY_OF_YEAR, -(n - 1))
        }
        val tmp = calStart.clone() as java.util.Calendar
        val labels = mutableListOf<String>()
        repeat(n) {
            labels += sdf.format(java.util.Date(tmp.timeInMillis))
            tmp.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return labels
    }
    fun isTikTok(pkg: String) =
            pkg.contains("tiktok", true) ||
                    pkg.contains("com.zhiliaoapp.musically", true) ||
                    pkg.contains("com.ss.android.ugc.trill", true) ||
                    pkg.contains("com.zhiliaoapp.musically.go", true)
    fun isInstagram(pkg: String) =
            pkg.contains("instagram", true) || pkg.contains("com.instagram.android", true)
    fun isYouTube(pkg: String) =
            pkg.contains("youtube", true) || pkg.contains("com.google.android.youtube", true)
    fun isFacebook(pkg: String) =
            pkg.contains("facebook", true) ||
                    pkg.contains("com.facebook.katana", true) ||
                    pkg.contains("com.facebook.lite", true)
    fun isTwitterX(pkg: String) =
            pkg.contains("twitter", true) ||
                    pkg.contains("com.twitter.android", true) ||
                    pkg.contains("x", true)
    @Composable
    fun colorFor(pkg: String) =
            when {
                isTikTok(pkg) -> Color(0xFF8E44AD)
                isInstagram(pkg) -> Color(0xFFFF7043)
                isYouTube(pkg) -> Color(0xFFEF5350)
                isFacebook(pkg) -> Color(0xFF1E88E5)
                isTwitterX(pkg) -> Color(0xFF333333)
                else -> MaterialTheme.colorScheme.tertiary
            }
    fun labelFor(pkg: String) =
            when {
                isTikTok(pkg) -> "TikTok"
                isInstagram(pkg) -> "Instagram"
                isYouTube(pkg) -> "YouTube"
                isFacebook(pkg) -> "Facebook"
                isTwitterX(pkg) -> "Twitter/X"
                else -> pkg.substringAfterLast('.')
            }

    Scaffold(
            topBar = { TopAppBar(title = { Text("✨ Brainrush", fontWeight = FontWeight.Bold) }) }
    ) { padding ->
        LazyColumn(
                modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                state = listState
        ) {
            // Kartu sapaan
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f)
                                    )
                                )
                            )
                            .padding(24.dp)
                    ) {
                        Column {
                            Text(
                                "Halo 👋",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Kontrol dopamin kamu, dengan gaya.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = totalToday?.let { "$it menit hari ini" } ?: "Izin penggunaan diperlukan",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Tombol refresh
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            if (UsageStatsHelper.hasUsagePermission(context)) {
                                scope.launch {
                                    repo.updateTodayUsage(context)
                                    val appUsageMap = UsageStatsHelper.getTodayUsage(context)
                                    totalToday = appUsageMap.values.sum().toInt()
                                    todayUsage = appUsageMap
                                }
                            } else {
                                UsageStatsHelper.openUsageSettings(context)
                            }
                        }
                    ) { Text("Refresh Data 🔄") }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Kartu statistik ringkas
            item {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -6)
                }
                val dayOrder = mutableListOf<String>()
                val tmp = calStart.clone() as Calendar
                repeat(7) {
                    dayOrder += sdf.format(Date(tmp.timeInMillis))
                    tmp.add(Calendar.DAY_OF_YEAR, 1)
                }
                val totalsMap = allData.associateBy({ it.date }, { it.totalMinutes })
                val last7Totals = dayOrder.map { totalsMap[it] ?: 0 }
                val avg7 = if (last7Totals.isNotEmpty()) last7Totals.sum() / last7Totals.size else 0
                val bestDayIdx = if (last7Totals.isNotEmpty()) last7Totals.indices.minByOrNull { last7Totals[it] } else null
                val latestRecorded = last7Totals.lastOrNull() ?: 0
                val trendWeek = if (avg7 == 0) 0f else (latestRecorded - avg7).toFloat() / avg7 * 100f
                val bestDayMinutes = bestDayIdx?.let { last7Totals[it] } ?: 0
                val bestDayDate = bestDayIdx?.let { dayOrder[it] }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Rata-rata 7 hari", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "$avg7 menit/hari",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (trendWeek >= 0) "+${"%.1f".format(trendWeek)}% vs rata-rata"
                                else "${"%.1f".format(trendWeek)}% vs rata-rata",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (trendWeek >= 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    ElevatedCard(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Hari terbaik", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (bestDayIdx != null) "$bestDayMinutes menit" else "-",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                bestDayDate ?: "Belum ada data",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
            // Chart di bagian atas
            item {
                if (selectedPkg != null && (selectedSeries.isNotEmpty() || selectedHourly.isNotEmpty())) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                if (chartRange == ChartRange.Day)
                                    "Distribusi per jam (hari ini): ${labelFor(selectedPkg!!)}"
                                else
                                    "Riwayat 7 hari: ${labelFor(selectedPkg!!)}",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Spacer(Modifier.height(8.dp))
                            // Toggle Hari/Minggu
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = chartRange == ChartRange.Day,
                                    onClick = { chartRange = ChartRange.Day },
                                    label = { Text("Hari") }
                                )
                                FilterChip(
                                    selected = chartRange == ChartRange.Week,
                                    onClick = { chartRange = ChartRange.Week },
                                    label = { Text("Minggu") }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            // Kontrol Zoom
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Zoom", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Slider(
                                    value = zoomFactor,
                                    onValueChange = { zoomFactor = it.coerceIn(1f, 3f) },
                                    valueRange = 1f..3f
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("x${String.format(java.util.Locale.getDefault(), "%.1f", zoomFactor)}", style = MaterialTheme.typography.labelSmall)
                            }
                            Spacer(Modifier.height(8.dp))
                            // Chart, bisa di-scroll horizontal saat di-zoom
                            val screenWidth = LocalConfiguration.current.screenWidthDp
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                LineChart(
                                    modifier = Modifier
                                        .width((screenWidth * zoomFactor).dp)
                                        .height(220.dp),
                                    data = listOf(
                                        Line(
                                            label = labelFor(selectedPkg!!),
                                            values = if (chartRange == ChartRange.Day) selectedHourly else selectedSeries,
                                            color = SolidColor(Color(0xFF23af92))
                                        )
                                    )
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            // Label sumbu-X
                            if (chartRange == ChartRange.Day) {
                                val ticks = listOf("0","3","6","9","12","15","18","21")
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    ticks.forEach { t ->
                                        Text(t, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            } else {
                                val labels = lastNDaysAbbrev(7)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    labels.forEach { d ->
                                        Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            // Daftar Top Apps hari ini (scrollable)
            if (todayUsage.isNotEmpty()) {
                item {
                    Text("Top Apps Hari Ini", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Ketuk aplikasi untuk melihat grafik 7 hari",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val sorted = todayUsage.entries.sortedByDescending { it.value }
                val total = todayUsage.values.sum().toFloat()
                items(sorted, key = { it.key }) { entry ->
                    val pkg = entry.key
                    val minutes = entry.value
                    val c = colorFor(pkg)
                    val label = labelFor(pkg)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.clickable {
                                selectedPkg = pkg
                                // Ambil data mingguan dan per jam
                                selectedSeries = UsageStatsHelper.getUsageLastNDays(context, pkg, 7)
                                selectedHourly = UsageStatsHelper.getUsageTodayHourly(context, pkg)
                                chartRange = ChartRange.Day // default tampilkan per jam saat dipilih
                                scope.launch { listState.animateScrollToItem(1) }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(12.dp).background(c, CircleShape))
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.weight(1f))
                            Text("${minutes}m", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        val pct = if (total > 0f) minutes.toFloat() / total else 0f
                        LinearProgressIndicator(
                            progress = pct,
                            modifier = Modifier.fillMaxWidth(),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = c
                        )
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }

            // Kartu izin akses penggunaan
            item {
                if (!UsageStatsHelper.hasUsagePermission(context)) {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Izin akses penggunaan belum aktif",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { UsageStatsHelper.openUsageSettings(context) }) {
                                Text("Buka Pengaturan Akses Penggunaan")
                            }
                        }
                    }
                }
            }
        }
    }
}

// Tambahan: enum di level file untuk mode chart
enum class ChartRange { Day, Week }
