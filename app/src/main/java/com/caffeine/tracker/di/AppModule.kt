package com.caffeine.tracker.di

import android.content.Context
import com.caffeine.tracker.data.local.CaffeineDatabase
import com.caffeine.tracker.data.local.DrinkDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDrinkDao(@ApplicationContext context: Context): DrinkDao {
        return CaffeineDatabase.getInstance(context).drinkDao()
    }
}
