package com.caffeine.tracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.CustomDrink
import com.caffeine.tracker.data.repository.CustomDrinkRepository
import com.caffeine.tracker.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class SettingsUiState(
    val customDrinks: List<CustomDrink> = emptyList(),
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val customDrinkRepository: CustomDrinkRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            customDrinkRepository.getAll().collect { drinks ->
                _uiState.value = SettingsUiState(customDrinks = drinks)
            }
        }
    }

    fun addCustomDrink(name: String, emoji: String, caffeineMg: Double, volumeMl: Int) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    customDrinkRepository.insert(
                        CustomDrink(
                            name = name.trim(),
                            emoji = if (emoji.isBlank()) "☕" else emoji,
                            caffeineMg = caffeineMg,
                            standardVolumeMl = volumeMl,
                        )
                    )
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                return@launch
            }
            widgetRefresher.refreshAsync()
        }
    }

    fun deleteCustomDrink(drink: CustomDrink) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    customDrinkRepository.delete(drink)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                return@launch
            }
            widgetRefresher.refreshAsync()
        }
    }
}
