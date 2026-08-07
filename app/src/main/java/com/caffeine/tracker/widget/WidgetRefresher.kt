package com.caffeine.tracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.caffeine.tracker.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
) {
    private val widget = GlanceCaffeineWidget()

    suspend fun refresh() {
        try {
            widget.updateAll(context)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w("WidgetRefresher", "widget refresh failed", e)
        }
    }

    fun refreshAsync() {
        appScope.launch { refresh() }
    }
}
