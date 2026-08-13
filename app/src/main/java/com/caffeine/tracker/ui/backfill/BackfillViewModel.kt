package com.caffeine.tracker.ui.backfill

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
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.CancellationException
import javax.inject.Inject

data class BackfillUiState(
    val drinks: List<DrinkTemplate> = DrinkCatalog.drinks,
    val selectedDrink: DrinkTemplate? = null,
    val selectedSize: DrinkSize? = null,
    val customVolumeMl: String = "",
    val showCustomVolume: Boolean = false,
    val calculatedCaffeine: Double = 0.0,
    val selectedDateUtcMillis: Long = todayUtcMillis(),
    val selectedHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
    val selectedMinute: Int = Calendar.getInstance().get(Calendar.MINUTE),
    val saving: Boolean = false,
) {
    companion object {
        fun todayUtcMillis(): Long {
            val c = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            c.clear()
            c.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR))
            c.set(Calendar.MONTH, Calendar.getInstance().get(Calendar.MONTH))
            c.set(Calendar.DAY_OF_MONTH, Calendar.getInstance().get(Calendar.DAY_OF_MONTH))
            return c.timeInMillis
        }
    }
}

@HiltViewModel
class BackfillViewModel @Inject constructor(
    private val drinkRepository: DrinkRepository,
    private val customDrinkRepository: CustomDrinkRepository,
    private val widgetRefresher: WidgetRefresher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackfillUiState())
    val uiState: StateFlow<BackfillUiState> = _uiState

    init {
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

    fun selectDrink(drink: DrinkTemplate) {
        val defaultSize = drink.sizes.first()
        val ratio = defaultSize.volumeMl.toDouble() / drink.standardVolumeMl
        _uiState.value = _uiState.value.copy(
            selectedDrink = drink,
            selectedSize = defaultSize,
            showCustomVolume = false,
            customVolumeMl = "",
            calculatedCaffeine = drink.defaultCaffeineMg * ratio,
        )
    }

    fun selectSize(size: DrinkSize) {
        val drink = _uiState.value.selectedDrink ?: return
        val ratio = size.volumeMl.toDouble() / drink.standardVolumeMl
        _uiState.value = _uiState.value.copy(
            selectedSize = size,
            showCustomVolume = false,
            customVolumeMl = "",
            calculatedCaffeine = drink.defaultCaffeineMg * ratio,
        )
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

    fun setDate(utcMillis: Long) {
        _uiState.value = _uiState.value.copy(selectedDateUtcMillis = utcMillis)
    }

    fun setTime(hour: Int, minute: Int) {
        _uiState.value = _uiState.value.copy(selectedHour = hour, selectedMinute = minute)
    }

    // 选定日期时间 -> 本地时间戳，禁止未来（clamp 到 now）
    fun selectedTimestamp(): Long {
        val s = _uiState.value
        val c = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            timeInMillis = s.selectedDateUtcMillis
        }
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, c.get(Calendar.YEAR))
            set(Calendar.MONTH, c.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, c.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, s.selectedHour)
            set(Calendar.MINUTE, s.selectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = System.currentTimeMillis()
        return if (cal.timeInMillis > now) now else cal.timeInMillis
    }

    suspend fun saveRecord(): Boolean {
        val drink = _uiState.value.selectedDrink ?: return false
        val caffeine = _uiState.value.calculatedCaffeine
        val volume = if (_uiState.value.showCustomVolume) {
            _uiState.value.customVolumeMl.toIntOrNull()
                ?: (_uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl)
        } else {
            _uiState.value.selectedSize?.volumeMl ?: drink.standardVolumeMl
        }
        val timestamp = selectedTimestamp()
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
                        timestamp = timestamp,
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
