package com.caffeine.tracker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.repository.DrinkRepository
import com.caffeine.tracker.data.repository.SettingsRepository
import com.caffeine.tracker.domain.CaffeinePharmacokinetics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class HomeUiState(
    val todayRecords: List<DrinkRecord> = emptyList(),
    val curvePoints: List<CaffeinePharmacokinetics.CurvePoint> = emptyList(),
    val currentLevel: Double = 0.0,
    val totalToday: Double = 0.0,
    val dailyLimit: Double = 400.0,
    val timeToZero: String = "",
    val curveStartTime: String = "",
    val curveMidTime: String = "",
    val curveEndTime: String = "",
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
    private val settingsRepository: SettingsRepository,
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
                val curvePoints = CaffeinePharmacokinetics.generateCurve(
                    records, halfLife, startOfDay, endOfDay
                )
                val currentLevel = CaffeinePharmacokinetics.calculateCurrentLevel(
                    records, halfLife, now
                )
                val totalToday = records.sumOf { it.caffeineMg }
                val ttz = CaffeinePharmacokinetics.estimatedTimeToZero(
                    records, halfLife, now
                )
                val ttzText = if (ttz <= 0) "已代谢完毕"
                else {
                    val hours = ttz / 3_600_000
                    val mins = (ttz % 3_600_000) / 60_000
                    "约 ${hours}h${mins}min 后代谢完毕"
                }

                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                _uiState.value = HomeUiState(
                    todayRecords = records,
                    curvePoints = curvePoints,
                    currentLevel = currentLevel,
                    totalToday = totalToday,
                    dailyLimit = limit,
                    timeToZero = ttzText,
                    curveStartTime = sdf.format(java.util.Date(startOfDay)),
                    curveMidTime = sdf.format(java.util.Date(startOfDay + 43_200_000L)),
                    curveEndTime = "24:00",
                )
            }
        }
    }

    fun deleteRecord(record: DrinkRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            drinkRepository.delete(record)
        }
    }
}
