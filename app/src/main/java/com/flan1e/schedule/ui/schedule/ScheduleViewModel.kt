package com.flan1e.schedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flan1e.schedule.data.repository.ScheduleRepository
import com.flan1e.schedule.data.repository.SettingsRepository
import com.flan1e.schedule.di.AppContainer
import com.flan1e.schedule.domain.logic.ScheduleResolver
import com.flan1e.schedule.domain.logic.WeekParityCalculator
import com.flan1e.schedule.domain.model.AppSettings
import com.flan1e.schedule.domain.model.BellSchedule
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.ScheduleState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    private val resolver: ScheduleResolver,
) : ViewModel() {

    /** Пересчитываем состояние времени, даже если расписание не менялось. */
    private val ticker: Flow<LocalDateTime> = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(TICK_INTERVAL_MILLIS)
        }
    }

    val uiState: StateFlow<ScheduleUiState> = combine(
        scheduleRepository.observeLessons(),
        settingsRepository.settings,
        settingsRepository.bellSchedule,
        ticker,
    ) { lessons, settings, bellSchedule, now ->
        buildState(lessons, settings, bellSchedule, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = ScheduleUiState(),
    )

    fun saveLesson(lesson: Lesson) {
        viewModelScope.launch { scheduleRepository.saveLesson(lesson) }
    }

    fun deleteLesson(id: Long) {
        viewModelScope.launch { scheduleRepository.deleteLesson(id) }
    }

    fun markPermissionRequested() {
        viewModelScope.launch { settingsRepository.markNotificationPermissionRequested() }
    }

    private fun buildState(
        lessons: List<Lesson>,
        settings: AppSettings,
        bellSchedule: BellSchedule,
        now: LocalDateTime,
    ): ScheduleUiState {
        val today = now.toLocalDate()
        return ScheduleUiState(
            loaded = true,
            days = DayOfWeek.values().map { day ->
                DaySchedule(
                    dayOfWeek = day,
                    lessons = lessons.filter { it.dayOfWeek == day }.sortedBy { it.start },
                )
            },
            today = today.dayOfWeek,
            weekIsOdd = WeekParityCalculator.isOddWeek(today, settings.anchorDate, settings.anchorIsOdd),
            scheduleState = resolver.resolve(
                now = now,
                lessons = lessons,
                anchorDate = settings.anchorDate,
                anchorIsOdd = settings.anchorIsOdd,
            ),
            settings = settings,
            bellSchedule = bellSchedule,
        )
    }

    companion object {
        private const val TICK_INTERVAL_MILLIS = 30_000L
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ScheduleViewModel(
                    scheduleRepository = container.scheduleRepository,
                    settingsRepository = container.settingsRepository,
                    resolver = container.resolver,
                )
            }
        }
    }
}

data class DaySchedule(
    val dayOfWeek: DayOfWeek,
    val lessons: List<Lesson>,
)

data class ScheduleUiState(
    val loaded: Boolean = false,
    val days: List<DaySchedule> = emptyList(),
    val today: DayOfWeek = LocalDate.now().dayOfWeek,
    val weekIsOdd: Boolean = true,
    val scheduleState: ScheduleState? = null,
    val settings: AppSettings? = null,
    val bellSchedule: BellSchedule = BellSchedule.Empty,
)
