package com.caffeine.tracker.ui.adddrink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.model.DrinkCatalog
import com.caffeine.tracker.data.model.DrinkSize
import com.caffeine.tracker.data.model.DrinkTemplate
import com.caffeine.tracker.data.repository.DrinkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddDrinkUiState(
    val drinks: List<DrinkTemplate> = DrinkCatalog.drinks,
    val selectedDrink: DrinkTemplate? = null,
    val selectedSize: DrinkSize? = null,
    val customVolumeMl: String = "",
    val calculatedCaffeine: Double = 0.0,
    val showCustomVolume: Boolean = false,
)

@HiltViewModel
class AddDrinkViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDrinkUiState())
    val uiState: StateFlow<AddDrinkUiState> = _uiState

    fun selectDrink(drink: DrinkTemplate) {
        val defaultSize = drink.sizes.first()
        _uiState.value = _uiState.value.copy(
            selectedDrink = drink,
            selectedSize = defaultSize,
            showCustomVolume = false,
            customVolumeMl = "",
        )
        recalculate(drink, defaultSize)
    }

    fun selectSize(size: DrinkSize) {
        val drink = _uiState.value.selectedDrink ?: return
        _uiState.value = _uiState.value.copy(selectedSize = size, showCustomVolume = false, customVolumeMl = "")
        recalculate(drink, size)
    }

    fun setCustomVolume(ml: String) {
        val drink = _uiState.value.selectedDrink ?: return
        _uiState.value = _uiState.value.copy(customVolumeMl = ml, showCustomVolume = true)
        val vol = ml.toIntOrNull()
        if (vol != null && vol > 0) {
            val ratio = vol.toDouble() / drink.standardVolumeMl
            _uiState.value = _uiState.value.copy(calculatedCaffeine = drink.defaultCaffeineMg * ratio)
        }
    }

    private fun recalculate(drink: DrinkTemplate, size: DrinkSize) {
        val ratio = size.volumeMl.toDouble() / drink.standardVolumeMl
        _uiState.value = _uiState.value.copy(calculatedCaffeine = drink.defaultCaffeineMg * ratio)
    }

    fun saveRecord() {
        val drink = _uiState.value.selectedDrink ?: return
        val caffeine = _uiState.value.calculatedCaffeine
        val volume = if (_uiState.value.showCustomVolume) {
            _uiState.value.customVolumeMl.toIntOrNull() ?: (_uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl)
        } else {
            _uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl
        }
        viewModelScope.launch(Dispatchers.IO) {
            drinkRepository.insert(
                DrinkRecord(
                    drinkName = drink.name,
                    emoji = drink.emoji,
                    caffeineMg = caffeine,
                    volumeMl = volume,
                    timestamp = System.currentTimeMillis(),
                )
            )
        }
    }
}
