package com.caffeine.tracker.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DaySummary(
    val date: String,
    val totalMg: Double,
    val drinkCount: Int,
)

data class StatsUiState(
    val weekData: List<DaySummary> = emptyList(),
    val monthData: List<DaySummary> = emptyList(),
    val avgDaily: Double = 0.0,
    val favoriteDrink: String = "",
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch(Dispatchers.IO) {
            drinkRepository.getAllRecords().collect { allRecords ->
                val weekData = buildDaySummaries(allRecords, 7)
                val monthData = buildDaySummaries(allRecords, 30)
                val avg = monthData.takeLast(7).let { days ->
                    if (days.isEmpty()) 0.0 else days.sumOf { it.totalMg } / days.size
                }
                val fav = allRecords.groupBy { it.drinkName }
                    .maxByOrNull { it.value.size }
                    ?.key ?: ""
                _uiState.value = StatsUiState(
                    weekData = weekData.takeLast(7),
                    monthData = monthData.takeLast(30),
                    avgDaily = avg,
                    favoriteDrink = fav,
                )
            }
        }
    }

    private fun buildDaySummaries(records: List<DrinkRecord>, days: Int): List<DaySummary> {
        val cal = Calendar.getInstance()
        val result = mutableListOf<DaySummary>()
        for (i in days - 1 downTo 0) {
            cal.timeInMillis = System.currentTimeMillis()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val start = cal.timeInMillis
            val end = start + 86_400_000L
            val dayRecords = records.filter { it.timestamp in start until end }
            val total = dayRecords.sumOf { it.caffeineMg }
            result.add(DaySummary(
                date = "%d/%d".format(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)),
                totalMg = total,
                drinkCount = dayRecords.size,
            ))
        }
        return result
    }
}
