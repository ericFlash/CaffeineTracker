package com.caffeine.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.caffeine.tracker.MainActivity
import com.caffeine.tracker.R
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CaffeineWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        var currentLevel = 0.0
        var totalToday = 0.0
        var halfLife = 5.0
        val hourlyLevels = mutableListOf<Pair<String, Double>>()
        val records = mutableListOf<Pair<Double, Long>>()

        try {
            runBlocking {
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
                val drinkRecords = db.drinkDao().getRecordsForDayOnce(startOfDay, endOfDay)
                records.addAll(drinkRecords.map { it.caffeineMg to it.timestamp })
                currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                    records.map { it.first }, records.map { it.second }, halfLife, now
                )
                totalToday = drinkRecords.sumOf { it.caffeineMg }

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                for (h in 0 until 6) {
                    val futureTime = now + h * 3600_000L
                    val futureLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                        records.map { it.first }, records.map { it.second }, halfLife, futureTime
                    )
                    hourlyLevels.add(sdf.format(futureTime) to futureLevel)
                }
            }
        } catch (_: Exception) {
            val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
            currentLevel = prefs.getFloat("widget_current_level", 0f).toDouble()
            totalToday = prefs.getFloat("widget_today_total", 0f).toDouble()
        }

        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            views.setTextViewText(R.id.widget_caffeine_text, "%.0f mg".format(currentLevel))
            views.setTextViewText(R.id.widget_today_text, "今日: %.0f mg".format(totalToday))

            // progress bar
            val limit = 400.0
            val pct = (currentLevel / limit * 100).toInt().coerceIn(0, 100)
            views.setTextViewText(R.id.widget_progress_bar, "$pct%")
            val progressColor = when {
                pct >= 90 -> R.drawable.card_bg_high
                pct >= 50 -> R.drawable.card_bg_medium
                else -> R.drawable.card_bg_low
            }
            views.setInt(R.id.widget_progress_bar, "setBackgroundResource", progressColor)

            // hourly cards
            hourlyLevels.take(6).forEachIndexed { i, (time, level) ->
                val cardId = getCardId(i)
                val timeId = getTimeId(i)
                val levelTextId = getLevelTextId(i)

                views.setTextViewText(timeId, time)
                val levelLabel = when {
                    level > 350 -> "%.0f".format(level)
                    level > 200 -> "%.0f".format(level)
                    level > 100 -> "%.0f".format(level)
                    level > 50 -> "%.0f".format(level)
                    level > 10 -> "%.0f".format(level)
                    else -> "%.0f".format(level)
                }
                views.setTextViewText(levelTextId, "${levelLabel}mg")

                val cardBg = when {
                    level > 350 -> R.drawable.card_bg_high
                    level > 200 -> R.drawable.card_bg_medium
                    level > 100 -> R.drawable.card_bg_medium
                    level > 50 -> R.drawable.card_bg_low
                    else -> R.drawable.card_bg_low
                }
                views.setInt(cardId, "setBackgroundResource", cardBg)

                // color the level text based on severity
                val textColor = when {
                    level > 200 -> "#FFD32F2F"
                    level > 100 -> "#FFE65100"
                    level > 50 -> "#FF388E3C"
                    else -> "#FF388E3C"
                }
                views.setTextColor(levelTextId, android.graphics.Color.parseColor(textColor))
            }

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun getCardId(i: Int): Int = when (i) {
        0 -> R.id.hour_0; 1 -> R.id.hour_1; 2 -> R.id.hour_2
        3 -> R.id.hour_3; 4 -> R.id.hour_4; 5 -> R.id.hour_5
        else -> R.id.hour_0
    }

    private fun getTimeId(i: Int): Int = when (i) {
        0 -> R.id.hour_0_time; 1 -> R.id.hour_1_time; 2 -> R.id.hour_2_time
        3 -> R.id.hour_3_time; 4 -> R.id.hour_4_time; 5 -> R.id.hour_5_time
        else -> R.id.hour_0_time
    }

    private fun getLevelTextId(i: Int): Int = when (i) {
        0 -> R.id.hour_0_level; 1 -> R.id.hour_1_level; 2 -> R.id.hour_2_level
        3 -> R.id.hour_3_level; 4 -> R.id.hour_4_level; 5 -> R.id.hour_5_level
        else -> R.id.hour_0_level
    }
}
