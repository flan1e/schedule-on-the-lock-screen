package com.flan1e.schedule.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.flan1e.schedule.data.backup.BackupCodec
import com.flan1e.schedule.data.backup.ScheduleBackup
import com.flan1e.schedule.data.repository.ScheduleRepository
import com.flan1e.schedule.data.repository.SettingsRepository
import com.flan1e.schedule.di.AppContainer
import com.flan1e.schedule.domain.logic.WeekParityCalculator
import com.flan1e.schedule.domain.model.AppLanguage
import com.flan1e.schedule.domain.model.AppSettings
import com.flan1e.schedule.domain.model.AppTheme
import com.flan1e.schedule.notification.ScheduleServiceController
import com.flan1e.schedule.util.LocaleController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val scheduleRepository: ScheduleRepository,
    private val serviceController: ScheduleServiceController,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = settingsRepository.settings
        .map { settings ->
            SettingsUiState(
                loaded = true,
                settings = settings,
                currentWeekIsOdd = WeekParityCalculator.isOddWeek(
                    date = LocalDate.now(),
                    anchorDate = settings.anchorDate,
                    anchorIsOdd = settings.anchorIsOdd,
                ),
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SettingsUiState(),
        )

    fun onEnabledChanged(value: Boolean) = update { setEnabled(value) }

    fun onLockScreenVisibilityChanged(value: Boolean) = update { setLockScreenVisible(value) }

    fun onShowTitleChanged(value: Boolean) = update { setShowTitle(value) }

    fun onShowRoomChanged(value: Boolean) = update { setShowRoom(value) }

    fun onShowNextLessonChanged(value: Boolean) = update { setShowNextLesson(value) }

    fun onCompactModeChanged(value: Boolean) = update { setCompactMode(value) }

    fun onThemeChanged(theme: AppTheme) = update { setTheme(theme) }

    fun onLanguageChanged(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            LocaleController.apply(language)
        }
    }

    fun onAnchorDateChanged(date: LocalDate) = update { setAnchorDate(date) }

    fun onAnchorParityChanged(anchorIsOdd: Boolean) = update { setAnchorOdd(anchorIsOdd) }

    fun onOverrideCurrentWeekParity(currentWeekIsOdd: Boolean) =
        update { overrideCurrentWeekParity(currentWeekIsOdd) }

    fun onResetAnchor() = update { resetAnchorWeek() }

    /** JSON для файла резервной копии: пары и опорная неделя. */
    suspend fun exportJson(): String {
        val settings = settingsRepository.snapshot()
        return BackupCodec.encode(
            ScheduleBackup(
                anchorDate = settings.anchorDate,
                anchorIsOdd = settings.anchorIsOdd,
                lessons = scheduleRepository.getLessons(),
            ),
        )
    }

    /**
     * Применяет уже разобранную копию: пары одной транзакцией Room,
     * затем опорная неделя. После этого сразу обновляем службу уведомления.
     */
    fun applyBackup(backup: ScheduleBackup, onApplied: (Int) -> Unit) {
        viewModelScope.launch {
            scheduleRepository.replaceAll(backup.lessons)
            settingsRepository.setAnchorDate(backup.anchorDate)
            settingsRepository.setAnchorOdd(backup.anchorIsOdd)
            serviceController.apply(settingsRepository.snapshot())
            onApplied(backup.lessons.size)
        }
    }

    /** Единая точка входа для событий экрана настроек. */
    fun handle(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.EnabledChanged -> onEnabledChanged(event.value)
            is SettingsEvent.LockScreenVisibilityChanged -> onLockScreenVisibilityChanged(event.value)
            is SettingsEvent.ShowTitleChanged -> onShowTitleChanged(event.value)
            is SettingsEvent.ShowRoomChanged -> onShowRoomChanged(event.value)
            is SettingsEvent.ShowNextLessonChanged -> onShowNextLessonChanged(event.value)
            is SettingsEvent.CompactModeChanged -> onCompactModeChanged(event.value)
            is SettingsEvent.ThemeChanged -> onThemeChanged(event.theme)
            is SettingsEvent.LanguageChanged -> onLanguageChanged(event.language)
            is SettingsEvent.AnchorDateChanged -> onAnchorDateChanged(event.date)
            is SettingsEvent.AnchorParityChanged -> onAnchorParityChanged(event.anchorIsOdd)
            is SettingsEvent.OverrideCurrentWeekParity ->
                onOverrideCurrentWeekParity(event.currentWeekIsOdd)
            SettingsEvent.ResetAnchor -> onResetAnchor()
        }
    }

    /** Меняет настройку, затем синхронизирует foreground-службу. */
    private fun update(block: suspend SettingsRepository.() -> Unit) {
        viewModelScope.launch {
            settingsRepository.block()
            serviceController.apply(settingsRepository.snapshot())
        }
    }

    companion object {
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = container.settingsRepository,
                    scheduleRepository = container.scheduleRepository,
                    serviceController = container.serviceController,
                )
            }
        }
    }
}

data class SettingsUiState(
    val loaded: Boolean = false,
    val settings: AppSettings? = null,
    val currentWeekIsOdd: Boolean = true,
)
