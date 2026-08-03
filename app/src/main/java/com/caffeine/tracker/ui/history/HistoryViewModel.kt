package com.caffeine.tracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val records: List<DrinkRecord> = emptyList(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            drinkRepository.getAllRecords().collect { records ->
                _uiState.value = HistoryUiState(records = records)
            }
        }
    }

    fun deleteRecord(record: DrinkRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            drinkRepository.delete(record)
        }
    }
}
