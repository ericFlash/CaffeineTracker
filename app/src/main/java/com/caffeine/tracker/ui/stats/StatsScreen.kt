package com.caffeine.tracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StatsScreen(viewModel: StatsViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("统计", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("近7日均值", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text("%.0f mg".format(state.avgDaily),
                        style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("最爱饮品", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f))
                    Text(if (state.favoriteDrink.isEmpty()) "--" else state.favoriteDrink,
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().height(200.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("近7天趋势", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                BarChart(state.weekData, modifier = Modifier.fillMaxWidth().height(140.dp))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().height(300.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("近30天趋势", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                BarChart(state.monthData, modifier = Modifier.fillMaxWidth().height(240.dp))
            }
        }
    }
}

@Composable
private fun BarChart(
    data: List<DaySummary>,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = data.maxOf { it.totalMg }.coerceAtLeast(1.0)
        val barWidth = size.width / data.size * 0.7f
        val gap = size.width / data.size * 0.3f

        data.forEachIndexed { i, day ->
            val barHeight = (day.totalMg / maxVal * size.height * 0.85f).toFloat()
            val x = i * (barWidth + gap) + gap / 2
            val y = size.height - barHeight
            drawRect(barColor, Offset(x, y), Size(barWidth, barHeight),
                alpha = 0.3f + 0.7f * (day.totalMg / maxVal).toFloat())

            // draw label every 5 items for month view
            if (data.size <= 7 || i % 5 == 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    day.date,
                    x + barWidth / 2 - 10f,
                    size.height - 2f,
                    android.graphics.Paint().apply {
                        color = textColor.hashCode()
                        textSize = 18f
                        alpha = 120
                    }
                )
            }
        }
    }
}
