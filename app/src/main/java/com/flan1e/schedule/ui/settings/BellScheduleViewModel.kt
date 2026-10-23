package com.flan1e.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flan1e.schedule.data.repository.SettingsRepository
import com.flan1e.schedule.di.AppContainer
import com.flan1e.schedule.domain.model.BellPeriod
import com.flan1e.schedule.domain.model.BellSchedule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val MINUTES_PER_DAY = 24 * 60
private const val DEFAULT_PERIOD_MINUTES = 90
private const val DEFAULT_GAP_MINUTES = 10

/** Редактор сетки звонков: те же данные, что видит редактор пары. */
class BellScheduleViewModel(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val schedule: StateFlow<BellSchedule> = settingsRepository.bellSchedule
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = BellSchedule.Empty,
        )

    fun addPeriod() {
        val periods = schedule.value.periods
        val lastEnd = periods.lastOrNull()?.endMinute
        val start = lastEnd?.plus(DEFAULT_GAP_MINUTES) ?: (8 * 60 + 30)
        // Если до конца суток не хватает места, начинаем день заново.
        val period = if (start + DEFAULT_PERIOD_MINUTES >= MINUTES_PER_DAY) {
            BellPeriod(8 * 60 + 30, 10 * 60)
        } else {
            BellPeriod(start, start + DEFAULT_PERIOD_MINUTES)
        }
        save(periods + period)
    }

    fun updatePeriod(index: Int, period: BellPeriod) {
        val periods = schedule.value.periods.toMutableList()
        if (index !in periods.indices) return
        periods[index] = period
        save(periods)
    }

    fun removePeriod(index: Int) {
        val periods = schedule.value.periods
        if (index !in periods.indices) return
        save(periods.filterIndexed { i, _ -> i != index })
    }

    fun useStandard() = save(BellSchedule.standard().periods)

    fun clear() = save(emptyList())

    private fun save(periods: List<BellPeriod>) {
        viewModelScope.launch {
            settingsRepository.setBellSchedule(BellSchedule(periods))
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                BellScheduleViewModel(settingsRepository = container.settingsRepository)
            }
        }
    }
}
