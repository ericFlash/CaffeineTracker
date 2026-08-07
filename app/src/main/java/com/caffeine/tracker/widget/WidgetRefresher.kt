package com.caffeine.tracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.caffeine.tracker.di.AppScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

// 在应用级协程作用域中刷新 Glance 小组件，确保刷新不受界面导航/ViewModel 销毁影响。
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    @AppScope private val appScope: CoroutineScope,
) {
    // 可等待版本：调用方协程会等待刷新完成后再继续。
    suspend fun refresh() {
        try {
            GlanceCaffeineWidget().updateAll(context)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) { }
    }

    // fire-and-forget 版本：在应用级作用域中触发，立即返回，适合 UI 交互后调用。
    fun refreshAsync() {
        appScope.launch { refresh() }
    }
}
