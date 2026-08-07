package com.caffeine.tracker.di

import android.content.Context
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.data.local.DrinkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AppScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDrinkDao(@ApplicationContext context: Context): DrinkDao {
        return CaffeineDatabase.getInstance(context).drinkDao()
    }

    // 应用级协程作用域：用于跨界面导航后仍需完成的任务（如小组件刷新），
    // 避免依赖 viewModelScope 而在页面销毁时被取消。
    @Provides
    @Singleton
    @AppScope
    fun provideAppScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
