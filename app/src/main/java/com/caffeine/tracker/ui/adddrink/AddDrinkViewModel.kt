package com.caffeine.tracker.ui.adddrink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.model.DrinkCatalog
import com.caffeine.tracker.data.model.DrinkSize
import com.caffeine.tracker.data.model.DrinkTemplate
import com.caffeine.tracker.data.repository.DrinkRepository
import com.caffeine.tracker.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.CancellationException
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
    private val widgetRefresher: WidgetRefresher,
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

    // suspend：调用方在协程中调用，可确保数据落库后再执行后续操作（如返回上一页）。
    // 小组件刷新在应用级作用域中触发，避免页面 pop 后 viewModelScope 被取消导致刷新中断。
    suspend fun saveRecord() {
        val drink = _uiState.value.selectedDrink ?: return
        val caffeine = _uiState.value.calculatedCaffeine
        val volume = if (_uiState.value.showCustomVolume) {
            _uiState.value.customVolumeMl.toIntOrNull() ?: (_uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl)
        } else {
            _uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl
        }
        try {
            withContext(Dispatchers.IO) {
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
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            return
        }
        widgetRefresher.refreshAsync()
    }
}
