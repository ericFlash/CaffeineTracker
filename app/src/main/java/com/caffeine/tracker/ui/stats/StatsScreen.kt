package com.caffeine.tracker.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
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
                Text(
                    "近30天趋势 · 累计 %.0f mg".format(state.monthData.sumOf { it.totalMg }),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                MonthHeatmap(state.monthData, state.dailyLimit, modifier = Modifier.fillMaxWidth().height(260.dp))
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
            val isOver = day.totalMg > dailyLimit
            val color = if (isOver) overColor else barColor
            // 零摄入日不画柱体，避免在基线和星期标签之间出现异常圆顶
            if (barHeight > 0f) {
                val y = chartBottom - barHeight
                val radius = (barWidth / 3f).coerceAtMost(10f).coerceAtMost(barHeight / 2f)
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
            }
            drawContext.canvas.nativeCanvas.drawText(day.weekday, x + barWidth / 2, size.height - 2f, xLabelPaint)
        }
    }
}

@Composable
private fun MonthHeatmap(
    data: List<DaySummary>,
    dailyLimit: Double,
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val outlineColor = MaterialTheme.colorScheme.primary
    val textArgb = textColor.toArgb()
    val density = LocalDensity.current.density
    val darkTheme = isSystemInDarkTheme()

    // 色块底色与数值渐变色随主题切换：浅色主题用暖米色系，深色主题用低亮度咖啡色系，
    // 保证 onSurface 文字（浅色主题深字 / 深色主题浅字）在色块上均可读。
    val cellFills = if (darkTheme) listOf(
        Color(0xFF2A2421), Color(0xFF3D3024), Color(0xFF4D3826),
        Color(0xFF5A3C24), Color(0xFF5E3224), Color(0xFF5A2620)
    ) else listOf(
        Color(0xFFF1EBE3), Color(0xFFF9E7CF), Color(0xFFF7DFB9),
        Color(0xFFF3D2A4), Color(0xFFF0C79A), Color(0xFFF0C4BE)
    )
    val gradColors = if (darkTheme) listOf(
        Color(0xFFA8C8A4), Color(0xFFE6C26A), Color(0xFFEFA968), Color(0xFFE0735A)
    ) else listOf(
        Color(0xFF8CAF8A), Color(0xFFC7A34A), Color(0xFFD98E4A), Color(0xFFC2563C)
    )

    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val n = data.size
        val weekdays = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")
        val startIdx = weekdays.indexOf(data.first().weekday).coerceIn(0, 6)
        val rows = ((startIdx + n + 6) / 7).coerceAtLeast(1)

        val headerH = 20f * density
        val legendH = 24f * density
        val bottomPad = 4f * density
        val sidePad = 6f * density
        val gap = 7f * density
        val availW = size.width - sidePad * 2f
        val cellW = (availW - gap * 6f) / 7f
        val availH = size.height - headerH - legendH - bottomPad
        val cellH = (availH - gap * (rows - 1)) / rows.toFloat()
        val corner = 8f * density
        val dayFont = 10f * density
        val valueFont = 8.5f * density
        val headerFont = 11f * density

        // 顶部星期表头
        val headerPaint = android.graphics.Paint().apply {
            color = textArgb; textSize = headerFont; alpha = 150
            textAlign = android.graphics.Paint.Align.CENTER
        }
        weekdays.forEachIndexed { c, label ->
            val x = sidePad + c * (cellW + gap) + cellW / 2f
            drawContext.canvas.nativeCanvas.drawText(label, x, headerH - 6f * density, headerPaint)
        }

        // 30 天色块：底色按比例变浅色，日期 + /数值（渐变配色），今天加描边
        val maxVal = maxOf(data.maxOf { it.totalMg }, dailyLimit).coerceAtLeast(1.0)
        data.forEachIndexed { i, day ->
            val idx = startIdx + i
            val col = idx % 7
            val row = idx / 7
            val ratio = if (dailyLimit > 0) (day.totalMg / dailyLimit).toFloat() else 0f
            val fill = when {
                day.totalMg <= 0 -> cellFills[0]
                ratio > 1f -> cellFills[5]
                ratio > 0.75f -> cellFills[4]
                ratio > 0.5f -> cellFills[3]
                ratio > 0.25f -> cellFills[2]
                else -> cellFills[1]
            }
            val x = sidePad + col * (cellW + gap)
            val y = headerH + row * (cellH + gap)
            val cellSize = Size(cellW, cellH)
            val cellCorner = CornerRadius(corner, corner)
            drawRoundRect(fill, Offset(x, y), cellSize, cellCorner)
            if (i == n - 1) {
                drawRoundRect(outlineColor, Offset(x, y), cellSize, cellCorner, style = Stroke(width = 1.5f * density))
            }
            val centerX = x + cellW / 2f
            // 日期
            val dayPaint = android.graphics.Paint().apply {
                color = textArgb
                textSize = dayFont
                alpha = 150
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                day.date.substringAfterLast("/"),
                centerX,
                y + cellH * 0.38f,
                dayPaint
            )
            // 当日咖啡因（/数值，渐变配色）
            if (day.totalMg <= 0) {
                val emptyPaint = android.graphics.Paint().apply {
                    color = textArgb
                    textSize = valueFont
                    alpha = 110
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText("—", centerX, y + cellH * 0.78f, emptyPaint)
            } else {
                val valuePaint = android.graphics.Paint().apply {
                    color = gradientColor(day.totalMg, maxVal, gradColors).toArgb()
                    textSize = valueFont
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawContext.canvas.nativeCanvas.drawText("%.0f".format(day.totalMg), centerX, y + cellH * 0.78f, valuePaint)
            }
        }

        // 底部图例：少 -> 多 -> 超限
        val legendY = size.height - legendH / 2f + 4f * density
        val step = 22f * density
        val buckets = cellFills
        val centerX = size.width / 2f
        val legendStart = centerX - step * 2.5f
        val labelPaint = android.graphics.Paint().apply {
            color = textArgb; textSize = 10f * density; alpha = 160
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawContext.canvas.nativeCanvas.drawText("少", legendStart - step * 0.8f, legendY, labelPaint)
        buckets.forEachIndexed { k, c ->
            drawCircle(c, radius = 5f * density, center = Offset(legendStart + k * step, legendY))
        }
        drawContext.canvas.nativeCanvas.drawText("多", legendStart + 5f * step + step * 0.8f, legendY, labelPaint)
    }
}

private fun gradientColor(value: Double, maxVal: Double, colors: List<Color>): Color {
    val t = (value / maxVal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
    val green = colors[0]
    val yellow = colors[1]
    val orange = colors[2]
    val red = colors[3]
    return when {
        t < 0.34f -> lerp(green, yellow, t / 0.34f)
        t < 0.67f -> lerp(yellow, orange, (t - 0.34f) / 0.33f)
        else -> lerp(orange, red, (t - 0.67f) / 0.33f)
    }
}
