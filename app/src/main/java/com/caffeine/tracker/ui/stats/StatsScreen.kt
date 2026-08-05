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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
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
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("近7天趋势", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                BarChart(state.weekData, state.dailyLimit, modifier = Modifier.fillMaxWidth().height(160.dp))
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().height(320.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("近30天趋势", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                LineChart(state.monthData, state.dailyLimit, modifier = Modifier.fillMaxWidth().height(260.dp))
            }
        }
    }
}

@Composable
private fun BarChart(
    data: List<DaySummary>,
    dailyLimit: Double,
    modifier: Modifier = Modifier
) {
    val barColor = MaterialTheme.colorScheme.primary
    val overColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textArgb = textColor.toArgb()

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = maxOf(data.maxOf { it.totalMg }, dailyLimit) * 1.15
        val chartLeft = 38f
        val chartTop = 6f
        val chartBottom = size.height - 22f
        val chartHeight = chartBottom - chartTop
        val totalBarArea = size.width - chartLeft
        val slot = totalBarArea / data.size
        val barWidth = slot * 0.6f

        val yLabelPaint = android.graphics.Paint().apply {
            color = textArgb; textSize = 18f; alpha = 110
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        for (i in 0..4) {
            val yVal = maxVal * (4 - i) / 4
            val y = chartTop + chartHeight * i / 4f
            drawLine(gridColor, Offset(chartLeft, y), Offset(size.width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(yVal), chartLeft - 4f, y + 5f, yLabelPaint)
        }

        // limit dashed line
        val limitY = chartBottom - (dailyLimit / maxVal * chartHeight).toFloat()
        drawLine(
            overColor.copy(alpha = 0.6f), Offset(chartLeft, limitY), Offset(size.width, limitY),
            strokeWidth = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )

        val xLabelPaint = android.graphics.Paint().apply {
            color = textArgb; textSize = 18f; alpha = 140
            textAlign = android.graphics.Paint.Align.CENTER
        }
        data.forEachIndexed { i, day ->
            val ratio = (day.totalMg / maxVal).toFloat().coerceIn(0f, 1f)
            val barHeight = ratio * chartHeight
            val x = chartLeft + i * slot + (slot - barWidth) / 2
            val y = chartBottom - barHeight
            val radius = (barWidth / 3f).coerceAtMost(10f)
            val isOver = day.totalMg > dailyLimit
            val color = if (isOver) overColor else barColor
            // top-rounded bar via Path
            val barPath = Path().apply {
                moveTo(x, chartBottom)
                lineTo(x, y + radius)
                quadraticBezierTo(x, y, x + radius, y)
                lineTo(x + barWidth - radius, y)
                quadraticBezierTo(x + barWidth, y, x + barWidth, y + radius)
                lineTo(x + barWidth, chartBottom)
                close()
            }
            drawPath(barPath, color.copy(alpha = 0.35f + 0.6f * ratio))
            if (isOver) {
                drawPath(barPath, overColor.copy(alpha = 0.9f), style = Stroke(width = 2f))
            }
            drawContext.canvas.nativeCanvas.drawText(day.weekday, x + barWidth / 2, size.height - 2f, xLabelPaint)
        }
    }
}

@Composable
private fun LineChart(
    data: List<DaySummary>,
    dailyLimit: Double,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val avgColor = MaterialTheme.colorScheme.secondary
    val limitColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textArgb = textColor.toArgb()

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = maxOf(data.maxOf { it.totalMg }, dailyLimit) * 1.15
        val chartLeft = 38f
        val chartTop = 6f
        val chartBottom = size.height - 22f
        val chartHeight = chartBottom - chartTop
        val chartWidth = size.width - chartLeft

        val yLabelPaint = android.graphics.Paint().apply {
            color = textArgb; textSize = 18f; alpha = 110
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        for (i in 0..4) {
            val yVal = maxVal * (4 - i) / 4
            val y = chartTop + chartHeight * i / 4f
            drawLine(gridColor, Offset(chartLeft, y), Offset(size.width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText("%.0f".format(yVal), chartLeft - 4f, y + 5f, yLabelPaint)
        }

        // limit band
        val limitY = chartBottom - (dailyLimit / maxVal * chartHeight).toFloat()
        drawRect(limitColor.copy(alpha = 0.06f), Offset(chartLeft, chartTop), Size(chartWidth, (limitY - chartTop).coerceAtLeast(0f)))
        drawLine(
            limitColor.copy(alpha = 0.6f), Offset(chartLeft, limitY), Offset(size.width, limitY),
            strokeWidth = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
        )

        val n = data.size
        val denom = (n - 1).coerceAtLeast(1)
        fun pointFor(i: Int, value: Double): Offset {
            val x = chartLeft + i * chartWidth / denom
            val y = chartBottom - (value / maxVal * chartHeight).toFloat()
            return Offset(x, y)
        }
        val pts = data.mapIndexed { i, day -> pointFor(i, day.totalMg) }

        if (pts.size > 1) {
            // smoothed curve
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val curr = pts[i]
                    val prevPrev = if (i >= 2) pts[i - 2] else prev
                    val next = if (i < pts.size - 1) pts[i + 1] else curr
                    val tension = 0.22f
                    val cx1 = prev.x + (curr.x - prevPrev.x) * tension
                    val cy1 = prev.y + (curr.y - prevPrev.y) * tension
                    val cx2 = curr.x - (next.x - prev.x) * tension
                    val cy2 = curr.y - (next.y - prev.y) * tension
                    cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
                }
            }
            // gradient fill
            val fillPath = Path().apply {
                addPath(path)
                lineTo(pts.last().x, chartBottom)
                lineTo(pts.first().x, chartBottom)
                close()
            }
            drawPath(
                fillPath,
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
                    startY = chartTop,
                    endY = chartBottom
                )
            )
            drawPath(path, lineColor, style = Stroke(width = 2.8f, cap = StrokeCap.Round))

            // 7-day moving average
            if (n >= 7) {
                val avgPts = (0..n - 7).map { i ->
                    val avg = data.subList(i, i + 7).map { it.totalMg }.sum() / 7.0
                    pointFor(i + 3, avg)
                }
                val avgPath = Path().apply {
                    moveTo(avgPts[0].x, avgPts[0].y)
                    for (i in 1 until avgPts.size) lineTo(avgPts[i].x, avgPts[i].y)
                }
                drawPath(
                    avgPath, avgColor.copy(alpha = 0.7f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 6f)))
                )
            }

            // data dots
            pts.forEach { pt ->
                drawCircle(lineColor, radius = 4f, center = pt)
                drawCircle(surfaceColor, radius = 2f, center = pt)
            }
        }

        // smart x labels: first, last, and every ~7
        val xLabelPaint = android.graphics.Paint().apply {
            color = textArgb; textSize = 17f; alpha = 140
            textAlign = android.graphics.Paint.Align.CENTER
        }
        val step = if (n <= 7) 1 else 7
        data.forEachIndexed { i, day ->
            if (i == 0 || i == n - 1 || i % step == 0) {
                val x = chartLeft + i * chartWidth / denom
                drawContext.canvas.nativeCanvas.drawText(day.date, x, size.height - 2f, xLabelPaint)
            }
        }
    }
}
