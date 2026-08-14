package com.caffeine.tracker.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.domain.CaffeineLevels
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

class GlanceCaffeineWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    private data class WC(val bg: Color, val muted: Color, val brown: Color, val track: Color, val green: Color, val yellow: Color, val orange: Color, val red: Color)
    private val light = WC(Color(0xFFFDF6F0), Color(0xFF8A7A6B), Color(0xFF6F4E37), Color(0xFFF0E5DC), Color(0xFF8CAF8A), Color(0xFFC7A34A), Color(0xFFD98E4A), Color(0xFFC2563C))
    private val dark = WC(Color(0xFF1C1815), Color(0xFF9A8E82), Color(0xFFC4A68D), Color(0xFF3A332C), Color(0xFFA8C8A4), Color(0xFFE6C26A), Color(0xFFEFA968), Color(0xFFE0735A))
    private fun colorsFor(context: Context): WC {
        val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (night) dark else light
    }
    @OptIn(ExperimentalGlanceApi::class)
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext
        val c = colorsFor(app)
        val flow = widgetDataFlow(app, c)
        val initial = flow.first()
        provideContent {
            val data by flow.collectAsState(initial)
            Column(modifier = GlanceModifier.fillMaxSize().padding(5.dp).background(ColorProvider(c.bg)).clickable(actionStartActivity<MainActivity>()), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "体内咖啡因 · ${data.todayText}", style = TextStyle(color = ColorProvider(c.muted), fontSize = 11.sp))
                Spacer(GlanceModifier.height(2.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = data.currentLevelText, style = TextStyle(color = ColorProvider(data.ringColor), fontSize = 24.sp, fontWeight = FontWeight.Bold))
                            Text(text = " mg", style = TextStyle(color = ColorProvider(c.muted), fontSize = 10.sp))
                        }
                        Text(text = data.metabolismText, style = TextStyle(color = ColorProvider(c.brown), fontSize = 10.sp))
                    }
                    Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.End) {
                        Text(text = data.percentText, style = TextStyle(color = ColorProvider(data.ringColor), fontSize = 17.sp, fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(GlanceModifier.height(2.dp))
                Image(provider = ImageProvider(data.barBitmap), contentDescription = null, modifier = GlanceModifier.fillMaxWidth().height(13.dp), contentScale = ContentScale.FillBounds)
                Spacer(GlanceModifier.height(3.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    data.hourly.take(6).forEach { hour ->
                        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = hour.time, style = TextStyle(color = ColorProvider(c.muted), fontSize = 10.sp))
                        }
                    }
                }
                Spacer(GlanceModifier.height(3.dp))
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    data.hourly.take(6).forEach { hour ->
                        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = hour.emoji, style = TextStyle(fontSize = 12.sp))
                        }
                    }
                }
                Row(modifier = GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    data.hourly.take(6).forEach { hour ->
                        Column(modifier = GlanceModifier.defaultWeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = hour.levelText, modifier = GlanceModifier.padding(top = 2.dp), style = TextStyle(color = ColorProvider(hour.color), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
    private data class HourData(val time: String, val emoji: String, val levelText: String, val color: Color)
    private data class WidgetData(val currentLevel: Double, val currentLevelText: String, val todayText: String, val progressFraction: Float, val percentText: String, val ringColor: Color, val barBitmap: Bitmap, val metabolismText: String, val hourly: List<HourData>)
    // 组合内订阅的 Flow：今日记录 + 48h 残留记录变化时自动重算并触发重组刷新
    private fun widgetDataFlow(context: Context, c: WC): Flow<WidgetData> {
        val db = CaffeineDatabase.getInstance(context)
        val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
        val halfLife = prefs.getFloat("half_life", 5.0f).toDouble()
        val dailyLimit = prefs.getFloat("daily_limit", 400f)
        val now = System.currentTimeMillis()
        val startOfDay = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        val endOfDay = startOfDay + 86_400_000L
        val residualStart = now - CaffeinePharmacokinetics.RESIDUAL_WINDOW_HOURS * 3_600_000L
        return combine(
            db.drinkDao().getRecordsForDay(startOfDay, endOfDay),
            db.drinkDao().getRecordsSinceFlow(residualStart),
        ) { today, residual ->
            buildWidgetData(today, residual, halfLife, dailyLimit, c, now, startOfDay)
        }.distinctUntilChanged()
    }
    private fun buildWidgetData(today: List<DrinkRecord>, residual: List<DrinkRecord>, halfLife: Double, dailyLimit: Float, c: WC, now: Long, startOfDay: Long): WidgetData {
        val night = (c == dark)
        val pairs = residual.map { it.caffeineMg to it.timestamp }
        val currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(pairs.map { it.first }, pairs.map { it.second }, halfLife, now)
        val totalToday = today.sumOf { it.caffeineMg }
        val sleepSafeMs = CaffeinePharmacokinetics.estimatedTimeToSleepSafe(residual, halfLife, now)
        val metabolismText = if (sleepSafeMs <= 0) "已低于安全线" else { val eta = now + sleepSafeMs; val fmt = SimpleDateFormat("HH:mm", Locale.getDefault()); "预计 ${fmt.format(eta)} 可安心入睡" }
        val carryover = CaffeinePharmacokinetics.calculateCarryoverLevel(pairs.map { it.first }, pairs.map { it.second }, halfLife, startOfDay)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val hourly = (0 until 6).map { h ->
            val f = now + h * 3600_000L
            val lvl = CaffeinePharmacokinetics.calculateCurrentLevel(pairs.map { it.first }, pairs.map { it.second }, halfLife, f)
            sdf.format(f) to lvl
        }
        val frac = (currentLevel / dailyLimit.toDouble()).toFloat().coerceIn(0f, 1f)
        val pct = (frac * 100).toInt()
        val ring = CaffeineLevels.colorForRatio(frac, night)
        val bar = buildBarBitmap(frac, ring, c.track)
        val hourlyData = hourly.map { (t, lvl) -> HourData(t, impactEmoji(lvl), "%.0f".format(lvl), CaffeineLevels.gradient(lvl, dailyLimit.toDouble(), night)) }
        return WidgetData(currentLevel, "%.0f".format(currentLevel), "今日 %.0f/%.0f".format(totalToday, (dailyLimit.toDouble() - carryover).coerceAtLeast(0.0)), frac, "$pct%", ring, bar, metabolismText, hourlyData)
    }
    private fun buildBarBitmap(frac: Float, fillColor: Color, trackColor: Color): Bitmap {
        val w = 1500; val h = 60
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
        val corner = h / 2f
        paint.color = trackColor.toArgb()
        canvas.drawRoundRect(0f, 0f, w.toFloat(), h.toFloat(), corner, corner, paint)
        val fillW = w * frac.coerceIn(0f, 1f)
        if (fillW > 0f) { paint.color = fillColor.toArgb(); canvas.drawRoundRect(0f, 0f, fillW, h.toFloat(), corner, corner, paint) }
        return bitmap
    }
    private fun impactEmoji(level: Double): String = when { level > 200 -> "🤯"; level > 100 -> "😬"; level > 50 -> "🙂"; else -> "😴" }
}
class GlanceCaffeineWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceCaffeineWidget()
}
