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
import java.util.Calendar

class CaffeineWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        var currentLevel = 0.0
        var totalToday = 0.0

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
                val halfLife = prefs.getFloat("half_life", 5.0f).toDouble()
                val records = db.drinkDao().getRecordsForDayOnce(startOfDay, endOfDay)
                currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(records, halfLife, now)
                totalToday = records.sumOf { it.caffeineMg }
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

            val intent = Intent(context, MainActivity::class.java)
            val pi = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pi)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
