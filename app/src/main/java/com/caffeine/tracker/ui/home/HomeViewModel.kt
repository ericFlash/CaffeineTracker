package com.caffeine.tracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.repository.DrinkRepository
import com.caffeine.tracker.data.repository.SettingsRepository
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import com.caffeine.tracker.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.CancellationException
import javax.inject.Inject


data class HomeUiState(
    val todayRecords: List<DrinkRecord> = emptyList(),
    val curvePoints: List<CaffeinePharmacokinetics.CurvePoint> = emptyList(),
    val currentLevel: Double = 0.0,
    val totalToday: Double = 0.0,
    val dailyLimit: Double = 400.0,
    val availableDailyLimit: Double = 400.0,
    val timeToSleepSafe: String = "",
    val curveStartTime: Long = 0L,
    val curveEndTime: Long = 0L,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: SettingsRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
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

            drinkRepository.getRecordsForDay(startOfDay, endOfDay).collect { records ->
                val halfLife = settingsRepository.halfLifeHours
                val limit = settingsRepository.dailyLimitMg

                // 体内残留量需回溯历史窗口，避免跨午夜把昨日记录漏算导致清零
                val residualStart = now - CaffeinePharmacokinetics.RESIDUAL_WINDOW_HOURS * 3_600_000L
                val residualRecords = drinkRepository.getRecordsSince(residualStart)

                val curveStart = if (records.isEmpty()) now
                    else records.minOf { it.timestamp }
                val currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                    residualRecords, halfLife, now
                )

                val sleepTtzMs = CaffeinePharmacokinetics.estimatedTimeToSleepSafe(
                    residualRecords, halfLife, now
                )
                val latestRecord = residualRecords.maxOfOrNull { it.timestamp } ?: now
                val curveEnd = maxOf(latestRecord + sleepTtzMs, now + 3600_000L)

                val curvePoints = CaffeinePharmacokinetics.generateCurve(
                    residualRecords, halfLife, curveStart, curveEnd
                )
                val totalToday = records.sumOf { it.caffeineMg }
                val carryoverAtStart = CaffeinePharmacokinetics.calculateCarryoverLevel(
                    residualRecords, halfLife, startOfDay
                )
                val availableLimit = (limit - carryoverAtStart).coerceAtLeast(0.0)

                val timeText = if (sleepTtzMs <= 0) "已低于安全线 ✓"
                else {
                    val hours = sleepTtzMs / 3_600_000
                    val mins = (sleepTtzMs % 3_600_000) / 60_000
                    if (hours > 0) "约 ${hours}h${mins}min 后可安心入睡"
                    else "约 ${mins}min 后可安心入睡"
                }

                _uiState.value = HomeUiState(
                    todayRecords = records,
                    curvePoints = curvePoints,
                    currentLevel = currentLevel,
                    totalToday = totalToday,
                    dailyLimit = limit,
                    availableDailyLimit = availableLimit,
                    timeToSleepSafe = timeText,
                    curveStartTime = curveStart,
                    curveEndTime = curveEnd,
                )
            }
        }
    }

    fun deleteRecord(record: DrinkRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                drinkRepository.delete(record)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                return@launch
            }
            widgetRefresher.refreshAsync()
        }
    }
}
