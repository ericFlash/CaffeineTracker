package com.caffeine.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.widget.RemoteViews
import com.caffeine.tracker.MainActivity
import com.caffeine.tracker.R
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
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

            val db = SQLiteDatabase.openDatabase(
                context.getDatabasePath("caffeine_tracker.db").absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            val cursor = db.rawQuery(
                "SELECT caffeineMg, timestamp FROM drink_records WHERE timestamp >= ? AND timestamp < ? ORDER BY timestamp ASC",
                arrayOf(startOfDay.toString(), endOfDay.toString())
            )

            val records = mutableListOf<Pair<Double, Long>>()
            var sum = 0.0
            while (cursor.moveToNext()) {
                val mg = cursor.getDouble(0)
                val ts = cursor.getLong(1)
                records.add(mg to ts)
                sum += mg
            }
            cursor.close()
            db.close()

            currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                records.map { it.first }, records.map { it.second }, halfLife, now
            )
            totalToday = sum
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
