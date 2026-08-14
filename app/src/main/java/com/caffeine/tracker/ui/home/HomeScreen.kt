package com.caffeine.tracker.ui.home

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.caffeine.tracker.domain.CaffeineLevels
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import com.caffeine.tracker.ui.theme.AppAlpha
import com.caffeine.tracker.ui.theme.AppDimens
import com.caffeine.tracker.ui.theme.ChartText
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
    val container = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val darkTheme = isSystemInDarkTheme()
    // 状态色与曲线/小组件语义一致：绿→黄→橙→红（按当前体内咖啡因占比）
    val ratio = (state.currentLevel / state.dailyLimit.coerceAtLeast(1.0)).toFloat()
    val statusColor = CaffeineLevels.colorForRatio(ratio, darkTheme)
    val levelColor = when {
        ratio >= 0.75f -> Color(0xFF9B3D28)
        ratio >= 0.5f -> Color(0xFFB06F2E)
        else -> contentColor
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                // 顶部柔和高光，增强"咖啡液面/光泽"质感
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.White.copy(alpha = 0.0f)),
                        startY = 0f,
                        endY = 400f,
                    ),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                )
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "当前体内咖啡因",
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor
                    )
                    Spacer(Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
                Text(
                    text = "%.0f mg".format(state.currentLevel),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = levelColor,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatColumn("今日摄入", "%.0f mg".format(state.totalToday), contentColor, modifier = Modifier.weight(1f))
                    StatColumn("今日限额", "%.0f mg".format(state.todayLimit), contentColor, modifier = Modifier.weight(1f))
                    StatColumn("剩余可摄", "%.0f mg".format(state.remainingToday), contentColor, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "睡眠安全 · ${state.timeToSleepSafe}",
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = AppAlpha.Secondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun StatColumn(
    label: String,
    value: String,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(end = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = contentColor.copy(alpha = 0.6f))
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = contentColor,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                    intakeTimestamps = state.todayRecords.map { it.timestamp },
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = "体内咖啡因随时间变化曲线，红线为每日限额，绿线为睡眠安全 50mg" }
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
    intakeTimestamps: List<Long> = emptyList(),
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val limitColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurface
    val surfaceColor = MaterialTheme.colorScheme.surface
    val safeColor = Color(0xFF4CAF50)
    val textArgb = textColor.toArgb()
    val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    val sdfDay = java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault())
    val fontScale = LocalConfiguration.current.fontScale
    val density = LocalDensity.current.density
    val fs = ChartText.clampFontScale(fontScale)

    Canvas(modifier = modifier) {
        val maxVal = points.maxOf { it.level }.coerceAtLeast(dailyLimit) * 1.2
        val minTime = points.first().timestamp
        val maxTime = points.last().timestamp
        val displayStart = minOf(minTime, curveStartTime)
        val displayEnd = maxOf(maxTime, curveEndTime)
        val timeRange = (displayEnd - displayStart).toFloat().coerceAtLeast(1f)

        // limit band (red zone above dailyLimit)
        val limitY = size.height * (1f - (dailyLimit / maxVal)).toFloat()
        drawRect(limitColor.copy(alpha = 0.06f), Offset(0f, 0f), androidx.compose.ui.geometry.Size(size.width, limitY.coerceAtLeast(0f)))

        // sleep-safe band (green zone below 50mg)
        val sleepY = size.height * (1f - (CaffeinePharmacokinetics.SLEEP_SAFE_MG / maxVal)).toFloat()
        drawRect(safeColor.copy(alpha = 0.05f), Offset(0f, sleepY), androidx.compose.ui.geometry.Size(size.width, (size.height - sleepY).coerceAtLeast(0f)))

        // grid lines
        val yLabelPaint = ChartText.paint(
            ChartText.AXIS_SP, textArgb, density, fs,
            Paint.Align.LEFT, alpha = 100
        )
        // X 轴标签预留高度，避免 Y 轴 "0" 与时间标签在左下角重合
        val xAxisReserve = 16f * density
        for (i in 0..4) {
            // 底部刻度（i=0，即 "0"）上移避开 X 轴标签区；其余保持原位
            val y = if (i == 0) size.height - xAxisReserve else size.height * (1f - i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            drawContext.canvas.nativeCanvas.drawText(
                "%.0f".format(maxVal * i / 4), 4f, y - 4f, yLabelPaint
            )
        }

        // time labels on x-axis：整体 4 个刻度；首标签左对齐、尾标签右对齐、中间居中，避免裁切/重叠
        val rangeHours = (displayEnd - displayStart) / 3_600_000f
        val tickCount = when {
            rangeHours <= 4 -> 4
            rangeHours <= 12 -> 4
            else -> 4
        }
        val useDayFormat = rangeHours > 24
        val centerPaint = ChartText.paint(
            ChartText.AXIS_SP, textArgb, density, fs,
            Paint.Align.CENTER, alpha = 140
        )
        val leftPaint = ChartText.paint(
            ChartText.AXIS_SP, textArgb, density, fs,
            Paint.Align.LEFT, alpha = 140
        )
        val rightPaint = ChartText.paint(
            ChartText.AXIS_SP, textArgb, density, fs,
            Paint.Align.RIGHT, alpha = 140
        )
        for (i in 0..tickCount) {
            val t = displayStart + ((displayEnd - displayStart) * i / tickCount)
            val x = ((t - displayStart) / timeRange * size.width)
            val label = if (useDayFormat) sdfDay.format(java.util.Date(t)) else sdf.format(java.util.Date(t))
            val paint = when (i) {
                0 -> leftPaint
                tickCount -> rightPaint
                else -> centerPaint
            }
            drawContext.canvas.nativeCanvas.drawText(label, x, size.height - 4f, paint)
        }

        // limit dashed line
        drawLine(limitColor.copy(alpha = 0.6f), Offset(0f, limitY), Offset(size.width, limitY),
            strokeWidth = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        drawContext.canvas.nativeCanvas.drawText(
            "每日限额 %.0fmg".format(dailyLimit), 4f, limitY - 4f,
            ChartText.paint(ChartText.ANNOTATION_SP, limitColor.toArgb(), density, fs,
                Paint.Align.LEFT, alpha = 160)
        )

        // sleep safe threshold dashed line
        drawLine(safeColor.copy(alpha = 0.6f), Offset(0f, sleepY), Offset(size.width, sleepY),
            strokeWidth = 1.5f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(4f, 6f)))
        drawContext.canvas.nativeCanvas.drawText(
            "睡眠安全 50mg", 4f, sleepY - 4f,
            ChartText.paint(ChartText.ANNOTATION_SP, safeColor.toArgb(), density, fs,
                Paint.Align.LEFT, alpha = 160)
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
                val prevPrev = if (i >= 2) pts[i - 2] else prev
                val next = if (i < pts.size - 1) pts[i + 1] else curr
                val tension = 0.25f
                val cx1 = prev.x + (curr.x - prevPrev.x) * tension
                val cy1 = prev.y + (curr.y - prevPrev.y) * tension
                val cx2 = curr.x - (next.x - prev.x) * tension
                val cy2 = curr.y - (next.y - prev.y) * tension
                path.cubicTo(cx1, cy1, cx2, cy2, curr.x, curr.y)
            }
            drawPath(path, lineColor, style = Stroke(width = 4f, cap = StrokeCap.Round))

            // vertical gradient fill under curve
            val fillPath = Path().apply {
                addPath(path)
                lineTo(pts.last().x, size.height)
                lineTo(pts.first().x, size.height)
                close()
            }
            drawPath(
                fillPath,
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.02f)),
                    startY = 0f, endY = size.height
                )
            )

            // intake markers: triangle at x-axis + dot on curve
            intakeTimestamps.forEach { ts ->
                if (ts in displayStart..displayEnd) {
                    val x = ((ts - displayStart) / timeRange * size.width).coerceIn(0f, size.width)
                    val pt = points.minByOrNull { kotlin.math.abs(it.timestamp - ts) }
                    val level = pt?.level ?: 0.0
                    val y = size.height * (1f - (level / maxVal)).toFloat()
                    drawCircle(lineColor, radius = 5f, center = Offset(x, y))
                    drawCircle(surfaceColor, radius = 3f, center = Offset(x, y))
                    // triangle marker at bottom
                    val tri = Path().apply {
                        moveTo(x, size.height - 12f)
                        lineTo(x - 4f, size.height - 4f)
                        lineTo(x + 4f, size.height - 4f)
                        close()
                    }
                    drawPath(tri, lineColor.copy(alpha = 0.8f))
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
    var pendingDelete by remember { mutableStateOf<com.caffeine.tracker.data.local.DrinkRecord?>(null) }
    Text("今日记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(record.drinkName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("%.0f mg | %dml".format(record.caffeineMg, record.volumeMl),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(record.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    IconButton(onClick = { pendingDelete = record }, modifier = Modifier.size(AppDimens.MinTouchTarget)) {
                        Icon(Icons.Default.Delete, contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(AppDimens.IconTouchInner))
                    }
                }
            }
        }
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除该记录？") },
            text = {
                Text("将删除「%s %.0fmg」这条记录，删除后无法恢复。".format(record.drinkName, record.caffeineMg))
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(record)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}
