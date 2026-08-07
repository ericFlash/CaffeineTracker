package com.caffeine.tracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.caffeine.tracker.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

// 在应用级协程作用域中刷新 Glance 小组件，确保刷新不受界面导航/ViewModel 销毁影响。
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
) {
    fun refreshAsync() {
        appScope.launch {
            try {
                GlanceCaffeineWidget().updateAll(context)
            } catch (_: Exception) { }
        }
    }
}
