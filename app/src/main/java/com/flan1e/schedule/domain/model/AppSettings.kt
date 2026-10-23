package com.flan1e.schedule.domain.model

import java.time.LocalDate
import java.time.Month

/**
 * Пользовательские настройки приложения.
 *
 * [anchorDate] — дата внутри опорной недели, [anchorIsOdd] — её чётность.
 * По умолчанию опорная неделя — неделя, содержащая 1 сентября текущего года,
 * и она нечётная (начало учебного года).
 */
data class AppSettings(
    val enabled: Boolean = true,
    val lockScreenVisible: Boolean = true,
    val showTitle: Boolean = true,
    val showRoom: Boolean = true,
    val showNextLesson: Boolean = true,
    val compactMode: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val anchorDate: LocalDate = defaultAnchorDate(),
    val anchorIsOdd: Boolean = true,
    val notificationPermissionRequested: Boolean = false,
) {
    /** Показывать ли что-то на экране блокировки прямо сейчас. */
    val showingOnLockScreen: Boolean
        get() = enabled && lockScreenVisible

    companion object {
        fun defaultAnchorDate(today: LocalDate = LocalDate.now()): LocalDate =
            LocalDate.of(today.year, Month.SEPTEMBER, 1)
    }
}
