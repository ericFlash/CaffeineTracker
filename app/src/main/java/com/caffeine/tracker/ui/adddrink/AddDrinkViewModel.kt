package com.caffeine.tracker.ui.adddrink

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caffeine.tracker.data.local.DrinkRecord
import com.caffeine.tracker.data.model.DrinkCatalog
import com.caffeine.tracker.data.model.DrinkSize
import com.caffeine.tracker.data.model.DrinkTemplate
import com.caffeine.tracker.data.repository.CustomDrinkRepository
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
    val recentDrinks: List<DrinkTemplate> = emptyList(),
    val selectedDrink: DrinkTemplate? = null,
    val selectedSize: DrinkSize? = null,
    val customVolumeMl: String = "",
    val calculatedCaffeine: Double = 0.0,
    val showCustomVolume: Boolean = false,
    val saving: Boolean = false,
)

@HiltViewModel
class AddDrinkViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
    private val customDrinkRepository: CustomDrinkRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddDrinkUiState())
    val uiState: StateFlow<AddDrinkUiState> = _uiState

    init {
        viewModelScope.launch {
            drinkRepository.getRecentDrinks(5).collect { records ->
                _uiState.value = _uiState.value.copy(
                    recentDrinks = records.mapNotNull { it.toTemplate() }
                )
            }
        }
        viewModelScope.launch {
            customDrinkRepository.getAll().collect { custom ->
                val merged = DrinkCatalog.drinks + custom.map { drink ->
                    DrinkTemplate(
                        name = drink.name,
                        emoji = drink.emoji,
                        defaultCaffeineMg = drink.caffeineMg,
                        standardVolumeMl = drink.standardVolumeMl,
                        sizes = listOf(
                            DrinkSize("默认 (%dml)".format(drink.standardVolumeMl), drink.standardVolumeMl)
                        )
                    )
                }
                _uiState.value = _uiState.value.copy(drinks = merged)
            }
        }
    }

    // 将最近记录匹配到目录模板；未命中则用最近记录字段构造最小模板。
    private fun DrinkRecord.toTemplate(): DrinkTemplate? {
        val catalogMatch = DrinkCatalog.drinks.find { it.name == drinkName && it.emoji == emoji }
        if (catalogMatch != null) return catalogMatch
        if (volumeMl <= 0) return null
        return DrinkTemplate(
            name = drinkName,
            emoji = emoji,
            defaultCaffeineMg = caffeineMg,
            standardVolumeMl = volumeMl,
            sizes = listOf(DrinkSize("杯 (${volumeMl}ml)", volumeMl)),
        )
    }

    fun selectRecent(drink: DrinkTemplate) {
        selectDrink(drink)
    }

    fun selectDrink(drink: DrinkTemplate) {
        // 不预选默认杯量：杯量作为显式步骤由用户选择，避免直接跳过导致误记
        _uiState.value = _uiState.value.copy(
            selectedDrink = drink,
            selectedSize = null,
            showCustomVolume = false,
            customVolumeMl = "",
            calculatedCaffeine = 0.0,
        )
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

    // suspend：调用方在协程中调用，可确保数据落库 + 小组件刷新完成后再执行后续操作（如返回上一页）。
    // 使用 refresh()（可等待）而非 refreshAsync()（fire-and-forget），
    // 确保 onSaved()/popBackStack 发生在 widget 已更新之后，避免首次 Glance 冷启动时刷新未完成。
    // 返回 true 表示保存成功，false 表示失败（调用方据此决定是否 pop）。
    suspend fun saveRecord(): Boolean {
        val drink = _uiState.value.selectedDrink ?: return false
        val caffeine = _uiState.value.calculatedCaffeine
        val volume = if (_uiState.value.showCustomVolume) {
            _uiState.value.customVolumeMl.toIntOrNull() ?: (_uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl)
        } else {
            _uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl
        }
        _uiState.value = _uiState.value.copy(saving = true)
        var success = false
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
            widgetRefresher.refresh()
            success = true
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            return false
        } finally {
            _uiState.value = _uiState.value.copy(saving = false)
        }
        return success
    }
}
