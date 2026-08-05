package com.caffeine.tracker.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.caffeine.tracker.MainActivity
import com.caffeine.tracker.R
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GlanceCaffeineWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    // 柔和暖调色阶，替代刺眼的纯绿/纯红
    private val levelGreen = Color(0xFF8CAF8A)
    private val levelYellow = Color(0xFFC7A34A)
    private val levelOrange = Color(0xFFD98E4A)
    private val levelRed = Color(0xFFC2563C)

    @OptIn(ExperimentalGlanceApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context.applicationContext)
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .background(ColorProvider(Color(0xFFFDF6F0)))
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "体内咖啡因 · ${data.todayText}",
                    style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 11.sp)
                )
                Spacer(GlanceModifier.height(3.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = data.currentLevelText,
                                style = TextStyle(
                                    color = ColorProvider(data.ringColor),
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = " mg",
                                style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 10.sp)
                            )
                        }
                        Text(
                            text = data.metabolismText,
                            style = TextStyle(color = ColorProvider(Color(0xFF795548)), fontSize = 10.sp)
                        )
                    }

                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = GlanceModifier.size(46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(data.ringBitmap),
                                contentDescription = null,
                                modifier = GlanceModifier.fillMaxSize()
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = data.percentText,
                                    style = TextStyle(color = ColorProvider(data.ringColor), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "日限额",
                                    style = TextStyle(color = ColorProvider(Color(0xFF999999)), fontSize = 8.sp)
                                )
                            }
                        }
                    }
                }

                Spacer(GlanceModifier.height(5.dp))
                Text(
                    text = "───────────────",
                    style = TextStyle(color = ColorProvider(Color(0xFFE0D5CC)), fontSize = 9.sp)
                )
                Spacer(GlanceModifier.height(5.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    data.hourly.take(6).forEach { hour ->
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hour.time,
                                style = TextStyle(color = ColorProvider(Color(0xFF999999)), fontSize = 9.sp)
                            )
                        }
                    }
                }
                Spacer(GlanceModifier.height(2.dp))
                // 中部：6 小时浓度迷你柱状图（整行一张位图）
                Image(
                    provider = ImageProvider(data.barsBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxWidth().height(18.dp),
                    contentScale = ContentScale.FillBounds
                )
                Spacer(GlanceModifier.height(2.dp))
                // 底部：6 小时浓度数值
                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    data.hourly.take(6).forEach { hour ->
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hour.levelText,
                                style = TextStyle(color = ColorProvider(hour.color), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    private data class HourData(val time: String, val levelText: String, val color: Color)

    private data class WidgetData(
        val currentLevel: Double,
        val currentLevelText: String,
        val todayText: String,
        val progressFraction: Float,
        val percentText: String,
        val ringColor: Color,
        val ringBitmap: Bitmap,
        val barsBitmap: Bitmap,
        val metabolismText: String,
        val hourly: List<HourData>
    )

    private suspend fun loadData(context: Context): WidgetData {
        var currentLevel = 0.0
        var totalToday = 0.0
        var halfLife = 5.0
        var dailyLimit = 400f
        val hourly = mutableListOf<Pair<String, Double>>()
        val records = mutableListOf<Pair<Double, Long>>()
        var metabolismText = "--"

        try {
            val db = CaffeineDatabase.getInstance(context)
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                timeInMillis = now
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = cal.timeInMillis
            val endOfDay = startOfDay + 86_400_000L
            val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
            halfLife = prefs.getFloat("half_life", 5.0f).toDouble()
            dailyLimit = prefs.getFloat("daily_limit", 400f)
            val drinkRecords = db.drinkDao().getRecordsForDayOnce(startOfDay, endOfDay)
            records.addAll(drinkRecords.map { it.caffeineMg to it.timestamp })
            currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                records.map { it.first }, records.map { it.second }, halfLife, now
            )
            totalToday = drinkRecords.sumOf { it.caffeineMg }

            val sleepSafeMs = CaffeinePharmacokinetics.estimatedTimeToSleepSafe(
                drinkRecords, halfLife, now
            )
            metabolismText = if (sleepSafeMs <= 0) {
                "已低于安全线"
            } else {
                val eta = now + sleepSafeMs
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                "预计 ${timeFmt.format(eta)} 可安心入睡"
            }

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            for (h in 0 until 6) {
                val futureTime = now + h * 3600_000L
                val level = CaffeinePharmacokinetics.calculateCurrentLevel(
                    records.map { it.first }, records.map { it.second }, halfLife, futureTime
                )
                hourly.add(sdf.format(futureTime) to level)
            }
        } catch (_: Exception) {
            val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
            currentLevel = prefs.getFloat("widget_current_level", 0f).toDouble()
            totalToday = prefs.getFloat("widget_today_total", 0f).toDouble()
        }

        val frac = (currentLevel / dailyLimit.toDouble()).toFloat().coerceIn(0f, 1f)
        val pct = (frac * 100).toInt()
        // 按“占日限额比例”动态选色：绿 -> 黄 -> 橙 -> 红
        val ringColor = when {
            frac >= 0.75f -> levelRed
            frac >= 0.5f -> levelOrange
            frac >= 0.25f -> levelYellow
            else -> levelGreen
        }
        val ringBitmap = buildRingBitmap(frac, ringColor, Color(0xFFE8E0D8))
        val barsBitmap = buildBarsBitmap(hourly.map { it.second }, dailyLimit.toDouble())

        val hourlyData = hourly.map { (time, level) ->
            HourData(
                time = time,
                levelText = "%.0f".format(level),
                color = levelColor(level)
            )
        }

        return WidgetData(
            currentLevel = currentLevel,
            currentLevelText = "%.0f".format(currentLevel),
            todayText = "今日 %.0f/%.0f".format(totalToday, dailyLimit),
            progressFraction = frac,
            percentText = "$pct%",
            ringColor = ringColor,
            ringBitmap = ringBitmap,
            barsBitmap = barsBitmap,
            metabolismText = metabolismText,
            hourly = hourlyData
        )
    }

    private fun buildBarsBitmap(levels: List<Double>, dailyLimit: Double): Bitmap {
        val w = 1080
        val h = 108
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val n = levels.size.coerceAtLeast(1)
        val maxVal = maxOf(levels.maxOrNull() ?: 0.0, dailyLimit).coerceAtLeast(1.0)
        val slot = w.toFloat() / n
        val barWidth = slot * 0.42f
        val barMinH = 14f
        val barMaxH = 88f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        levels.forEachIndexed { i, level ->
            val ratio = (level / maxVal).toFloat().coerceIn(0f, 1f)
            val barH = barMinH + (barMaxH - barMinH) * ratio
            val left = i * slot + (slot - barWidth) / 2f
            val top = h - barH
            paint.color = levelColor(level).toArgb()
            canvas.drawRoundRect(left, top, left + barWidth, h.toFloat(), barWidth / 2f, barWidth / 2f, paint)
        }
        return bitmap
    }

    private fun buildRingBitmap(frac: Float, ringColor: Color, trackColor: Color): Bitmap {
        val sizePx = 192
        val strokePx = 16f
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = strokePx
        }
        val inset = strokePx / 2f
        val bounds = RectF(inset, inset, sizePx - inset, sizePx - inset)
        paint.color = trackColor.toArgb()
        canvas.drawOval(bounds, paint)
        if (frac > 0f) {
            paint.color = ringColor.toArgb()
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawArc(bounds, -90f, 360f * frac.coerceIn(0f, 1f), false, paint)
        }
        return bitmap
    }

    private fun levelColor(level: Double): Color = when {
        level > 200 -> levelRed
        level > 100 -> levelOrange
        level > 50 -> levelYellow
        else -> levelGreen
    }
}

class GlanceCaffeineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceCaffeineWidget()
}
