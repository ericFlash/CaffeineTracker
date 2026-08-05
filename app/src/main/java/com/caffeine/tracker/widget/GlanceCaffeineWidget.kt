package com.caffeine.tracker.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
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
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GlanceCaffeineWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Exact

    @OptIn(ExperimentalGlanceApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = loadData(context.applicationContext)
        provideContent {
            Column(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(ColorProvider(Color(0xFFFDF6F0)))
                    .clickable(actionStartActivity<MainActivity>()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "体内咖啡因",
                    style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 12.sp)
                )
                Spacer(GlanceModifier.height(8.dp))

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
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = " mg",
                                style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 12.sp)
                            )
                        }
                        Text(
                            text = data.todayText,
                            style = TextStyle(color = ColorProvider(Color(0xFF888888)), fontSize = 11.sp)
                        )
                    }

                    Column(
                        modifier = GlanceModifier.defaultWeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = ColorProvider(data.ringColor)
                        )
                        Spacer(GlanceModifier.height(4.dp))
                        Text(
                            text = data.percentText,
                            style = TextStyle(color = ColorProvider(data.ringColor), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Spacer(GlanceModifier.height(8.dp))
                Text(
                    text = "───────────────",
                    style = TextStyle(color = ColorProvider(Color(0xFFE0D5CC)), fontSize = 10.sp)
                )
                Spacer(GlanceModifier.height(6.dp))

                Row(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    data.hourly.take(5).forEach { hour ->
                        Column(
                            modifier = GlanceModifier.defaultWeight(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = hour.time,
                                style = TextStyle(color = ColorProvider(Color(0xFF999999)), fontSize = 10.sp)
                            )
                            Spacer(GlanceModifier.height(2.dp))
                            Text(
                                text = hour.dot,
                                style = TextStyle(color = ColorProvider(hour.color), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            )
                            Spacer(GlanceModifier.height(2.dp))
                            Text(
                                text = hour.levelText,
                                style = TextStyle(color = ColorProvider(hour.color), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    private data class HourData(val time: String, val dot: String, val levelText: String, val color: Color)

    private data class WidgetData(
        val currentLevel: Double,
        val currentLevelText: String,
        val todayText: String,
        val progressFraction: Float,
        val percentText: String,
        val ringColor: Color,
        val hourly: List<HourData>
    )

    private suspend fun loadData(context: Context): WidgetData {
        var currentLevel = 0.0
        var totalToday = 0.0
        var halfLife = 5.0
        var dailyLimit = 400f
        val hourly = mutableListOf<Pair<String, Double>>()
        val records = mutableListOf<Pair<Double, Long>>()

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

            val sdf = SimpleDateFormat("HH", Locale.getDefault())
            for (h in 0 until 5) {
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

        val hourlyData = hourly.map { (time, level) ->
            HourData(
                time = time,
                dot = levelDot(level),
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
            ringColor = levelColor(currentLevel),
            hourly = hourlyData
        )
    }

    private fun levelDot(level: Double): String = when {
        level > 200 -> "●"
        level > 100 -> "◒"
        level > 50 -> "◑"
        else -> "○"
    }

    private fun levelColor(level: Double): Color = when {
        level > 300 -> Color(0xFFF44336)
        level > 200 -> Color(0xFFFF5722)
        level > 100 -> Color(0xFFFF9800)
        level > 50 -> Color(0xFFFFC107)
        else -> Color(0xFF4CAF50)
    }
}

class GlanceCaffeineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceCaffeineWidget()
}
