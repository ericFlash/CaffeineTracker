package com.caffeine.tracker.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
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
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
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
                    .padding(5.dp)
                    .background(ColorProvider(Color(0xFFFDF6F0)))
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "体内咖啡因 · ${data.todayText}",
                    style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 11.sp)
                )
                Spacer(GlanceModifier.height(2.dp))

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
                                    fontSize = 24.sp,
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
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = data.percentText,
                            style = TextStyle(color = ColorProvider(data.ringColor), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(GlanceModifier.height(2.dp))
                // 横向进度条：体内浓度占日限额的比例
                Image(
                    provider = ImageProvider(data.barBitmap),
                    contentDescription = null,
                    modifier = GlanceModifier.fillMaxWidth().height(13.dp),
                    contentScale = ContentScale.FillBounds
                )
                Spacer(GlanceModifier.height(3.dp))

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
                Spacer(GlanceModifier.height(3.dp))
                // 未来 6 小时：影响程度 emoji
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
                                text = hour.emoji,
                                style = TextStyle(fontSize = 12.sp)
                            )
                        }
                    }
                }
                // 未来 6 小时：预测浓度（渐变配色）
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
                                modifier = GlanceModifier.padding(top = 2.dp),
                                style = TextStyle(color = ColorProvider(hour.color), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    private data class HourData(val time: String, val emoji: String, val levelText: String, val color: Color)

    private data class WidgetData(
        val currentLevel: Double,
        val currentLevelText: String,
        val todayText: String,
        val progressFraction: Float,
        val percentText: String,
        val ringColor: Color,
        val barBitmap: Bitmap,
        val metabolismText: String,
        val hourly: List<HourData>
    )

    private suspend fun loadData(context: Context): WidgetData {
        var currentLevel = 0.0
        var totalToday = 0.0
        var halfLife = 5.0
        var dailyLimit = 400f
        var carryoverAtStart = 0.0
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
            val residualStart = now - CaffeinePharmacokinetics.RESIDUAL_WINDOW_HOURS * 3_600_000L
            val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
            halfLife = prefs.getFloat("half_life", 5.0f).toDouble()
            dailyLimit = prefs.getFloat("daily_limit", 400f)
            val drinkRecords = db.drinkDao().getRecordsForDayOnce(startOfDay, endOfDay)
            val residualRecords = db.drinkDao().getRecordsSince(residualStart)
            records.addAll(residualRecords.map { it.caffeineMg to it.timestamp })
            currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                records.map { it.first }, records.map { it.second }, halfLife, now
            )
            totalToday = drinkRecords.sumOf { it.caffeineMg }

            val sleepSafeMs = CaffeinePharmacokinetics.estimatedTimeToSleepSafe(
                residualRecords, halfLife, now
            )
            metabolismText = if (sleepSafeMs <= 0) {
                "已低于安全线"
            } else {
                val eta = now + sleepSafeMs
                val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
                "预计 ${timeFmt.format(eta)} 可安心入睡"
            }

            // 今日零点结转残留：用于扣减今日可用限额
            carryoverAtStart = CaffeinePharmacokinetics.calculateCarryoverLevel(
                records.map { it.first }, records.map { it.second }, halfLife, startOfDay
            )

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
            carryoverAtStart = prefs.getFloat("widget_carryover", 0f).toDouble()
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
        val barBitmap = buildBarBitmap(frac, ringColor, Color(0xFFE8E0D8))

        val hourlyData = hourly.map { (time, level) ->
            HourData(
                time = time,
                emoji = impactEmoji(level),
                levelText = "%.0f".format(level),
                // 与左上角数值、进度条统一：颜色 = 占日限额的比例
                color = gradientColor(level, dailyLimit.toDouble())
            )
        }

        return WidgetData(
            currentLevel = currentLevel,
            currentLevelText = "%.0f".format(currentLevel),
            todayText = "今日 %.0f/%.0f".format(
                totalToday,
                (dailyLimit.toDouble() - carryoverAtStart).coerceAtLeast(0.0)
            ),
            progressFraction = frac,
            percentText = "$pct%",
            ringColor = ringColor,
            barBitmap = barBitmap,
            metabolismText = metabolismText,
            hourly = hourlyData
        )
    }

    private fun buildBarBitmap(frac: Float, fillColor: Color, trackColor: Color): Bitmap {
        // 25:1 位图，接近实际显示比例，两端保持正圆
        val w = 1500
        val h = 60
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val corner = h / 2f
        paint.color = trackColor.toArgb()
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), corner, corner, paint)
        val fillW = w * frac.coerceIn(0f, 1f)
        if (fillW > 0f) {
            paint.color = fillColor.toArgb()
            canvas.drawRoundRect(0f, 0f, fillW, h.toFloat(), corner, corner, paint)
        }
        return bitmap
    }

    private fun impactEmoji(level: Double): String = when {
        level > 200 -> "🤯"
        level > 100 -> "😬"
        level > 50 -> "🙂"
        else -> "😴"
    }

    private fun gradientColor(level: Double, maxVal: Double): Color {
        val t = (level / maxVal.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f)
        return when {
            t < 0.34f -> lerp(levelGreen, levelYellow, t / 0.34f)
            t < 0.67f -> lerp(levelYellow, levelOrange, (t - 0.34f) / 0.33f)
            else -> lerp(levelOrange, levelRed, (t - 0.67f) / 0.33f)
        }
    }
}

class GlanceCaffeineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceCaffeineWidget()
}
