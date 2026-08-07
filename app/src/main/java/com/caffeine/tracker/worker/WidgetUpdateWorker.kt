package com.caffeine.tracker.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import com.caffeine.tracker.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class WidgetUpdateWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val widgetRefresher: WidgetRefresher,
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = CaffeineDatabase.getInstance(applicationContext)
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

        val prefs = applicationContext.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
        val halfLife = prefs.getFloat("half_life", 5.0f).toDouble()

        val todayRecords = db.drinkDao().getRecordsForDayOnce(startOfDay, endOfDay)
        val residualRecords = db.drinkDao().getRecordsSince(residualStart)
        val currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(residualRecords, halfLife, now)
        val totalToday = todayRecords.sumOf { it.caffeineMg }
        val carryoverAtStart = CaffeinePharmacokinetics.calculateCarryoverLevel(
            residualRecords, halfLife, startOfDay
        )

        prefs.edit()
            .putFloat("widget_current_level", currentLevel.toFloat())
            .putFloat("widget_today_total", totalToday.toFloat())
            .putFloat("widget_carryover", carryoverAtStart.toFloat())
            .apply()

        // 真正触发小组件视觉刷新：周期 Worker 是刷新的可靠兜底，
        // 即便即时刷新失败，最多 15 分钟后也会重新渲染。
        widgetRefresher.refresh()

        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                15, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "widget_update",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
