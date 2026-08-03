package com.caffeine.tracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.caffeine.tracker.MainActivity
import com.caffeine.tracker.R

class CaffeineWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
        val currentLevel = prefs.getFloat("widget_current_level", 0f)
        val totalToday = prefs.getFloat("widget_today_total", 0f)

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
