package com.caffeine.tracker.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)

    var halfLifeHours: Double
        get() = prefs.getFloat("half_life", 5.0f).toDouble()
        set(value) = prefs.edit().putFloat("half_life", value.toFloat()).apply()

    var dailyLimitMg: Double
        get() = prefs.getFloat("daily_limit", 400.0f).toDouble()
        set(value) = prefs.edit().putFloat("daily_limit", value.toFloat()).apply()

    var bodyWeightKg: Float
        get() = prefs.getFloat("body_weight", 70f)
        set(value) = prefs.edit().putFloat("body_weight", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("dark_theme", false)
        set(value) = prefs.edit().putBoolean("dark_theme", value).apply()
}
