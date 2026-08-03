package com.caffeine.tracker.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    viewModel: HomeViewModel,
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加", tint = MaterialTheme.colorScheme.onPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { HeaderCard(state) }
            item { CurveCard(state) }
            item { TodayRecordsList(state, viewModel::deleteRecord) }
        }
    }
}

@Composable
private fun HeaderCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("当前体内咖啡因", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "%.0f mg".format(state.currentLevel),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column {
                    Text("今日摄入", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text("%.0f mg".format(state.totalToday), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("日限额", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text("%.0f mg".format(state.dailyLimit), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
                Column {
                    Text("睡眠安全", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                    Text(state.timeToSleepSafe, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun CurveCard(state: HomeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (state.curvePoints.isEmpty()) {
                Text("暂无数据，点击 + 添加饮品", modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            } else {
                CaffeineCurve(
                    points = state.curvePoints,
                    dailyLimit = state.dailyLimit,
                    curveStartTime = state.curveStartTime,
                    curveEndTime = state.curveEndTime,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CaffeineCurve(
    points: List<CaffeinePharmacokinetics.CurvePoint>,
    dailyLimit: Double,
    curveStartTime: Long = 0L,
    curveEndTime: Long = 0L,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val limitColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val sdfDay = java.text.SimpleDateFormat("MM/dd HH:mm", java.util.Locale.getDefault())

    Canvas(modifier = modifier) {
        val maxVal = points.maxOf { it.level }.coerceAtLeast(dailyLimit) * 1.2
        val minTime = points.first().timestamp
        val maxTime = points.last().timestamp
        val displayStart = minOf(minTime, curveStartTime)
        val displayEnd = maxOf(maxTime, curveEndTime)
        val timeRange = (displayEnd - displayStart).toFloat().coerceAtLeast(1f)

        // grid lines
        for (i in 0..4) {
            val y = size.height * (1f - i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f".format(maxVal * i / 4),
                4f, y - 4f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 20f
                    alpha = 100
                }
            )
        }

        // time labels on x-axis
        val timeLabelPaint = android.graphics.Paint().apply {
            color = textColor.hashCode()
            textSize = 16f
            alpha = 140
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val rangeHours = (displayEnd - displayStart) / 3_600_000f
        val tickCount = when {
            rangeHours <= 4 -> 4
            rangeHours <= 12 -> 6
            else -> 8
        }
        val useDayFormat = rangeHours > 24
        for (i in 0..tickCount) {
            val t = displayStart + ((displayEnd - displayStart) * i / tickCount)
            val x = ((t - displayStart) / timeRange * size.width)
            val label = if (useDayFormat) sdfDay.format(java.util.Date(t)) else sdf.format(java.util.Date(t))
            drawContext.canvas.nativeCanvas.drawText(label, x, size.height - 4f, timeLabelPaint)
        }

        // limit line
        val limitY = size.height * (1f - (dailyLimit / maxVal)).toFloat()
        drawLine(limitColor.copy(alpha = 0.5f), Offset(0f, limitY), Offset(size.width, limitY),
            strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))

        // sleep safe threshold line (50mg)
        val safeColor = Color(0xFF4CAF50)
        val sleepY = size.height * (1f - (CaffeinePharmacokinetics.SLEEP_SAFE_MG / maxVal)).toFloat()
        drawLine(safeColor.copy(alpha = 0.5f), Offset(0f, sleepY), Offset(size.width, sleepY),
            strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        drawContext.canvas.nativeCanvas.drawText(
            "睡眠安全线 50mg", 4f, sleepY - 4f,
            android.graphics.Paint().apply {
                color = safeColor.hashCode()
                textSize = 18f
                alpha = 150
            }
        )

        // curve with cubic bezier smoothing
        if (points.size > 1) {
            val path = Path()
            val pts = points.map { pt ->
                val x = ((pt.timestamp - displayStart) / timeRange * size.width).coerceIn(0f, size.width)
                val y = size.height * (1f - (pt.level / maxVal)).toFloat()
                Offset(x, y)
            }
            path.moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val curr = pts[i]
                val cx1 = prev.x + (curr.x - prev.x) / 3f
                val cx2 = prev.x + (curr.x - prev.x) * 2f / 3f
                path.cubicTo(cx1, prev.y, cx2, curr.y, curr.x, curr.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round))

            // gradient fill under curve
            val fillPath = Path().apply {
                addPath(path)
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }
            drawPath(fillPath, lineColor.copy(alpha = 0.08f))

            // dots for drink records
            val drawnTimes = mutableSetOf<Long>()
            points.forEach { pt ->
                if (pt.level > 0.5 && drawnTimes.add(pt.timestamp / 60_000)) {
                    val x = ((pt.timestamp - displayStart) / timeRange * size.width).coerceIn(0f, size.width)
                    val y = size.height * (1f - (pt.level / maxVal)).toFloat()
                    drawCircle(lineColor, radius = 5f, center = Offset(x, y))
                    drawCircle(surfaceColor, radius = 3f, center = Offset(x, y))
                }
            }
        }
    }
}

@Composable
private fun TodayRecordsList(
    state: HomeUiState,
    onDelete: (com.caffeine.tracker.data.local.DrinkRecord) -> Unit
) {
    if (state.todayRecords.isEmpty()) return
    Text("今日记录", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    state.todayRecords.forEach { record ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(record.emoji, fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    modifier = Modifier.padding(end = 12.dp))
                Column {
                    Text(record.drinkName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text("%.0f mg | %dml".format(record.caffeineMg, record.volumeMl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    IconButton(onClick = { onDelete(record) }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
