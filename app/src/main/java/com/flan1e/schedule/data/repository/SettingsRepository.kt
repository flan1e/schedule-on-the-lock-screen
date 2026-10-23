package com.flan1e.schedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.flan1e.schedule.domain.logic.WeekParityCalculator
import com.flan1e.schedule.domain.model.AppLanguage
import com.flan1e.schedule.domain.model.AppSettings
import com.flan1e.schedule.domain.model.AppTheme
import com.flan1e.schedule.domain.model.BellSchedule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDate

/**
 * Настройки в DataStore Preferences.
 *
 * Опорная неделя хранится как дата + её чётность. Ручное переопределение
 * чётности текущей недели не создаёт отдельного флага: мы сдвигаем опорную
 * дату на неделю, поэтому дальнейший расчёт остаётся автоматическим.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { it.toSettings() }

    suspend fun snapshot(): AppSettings = settings.first()

    /** Сетка звонков живёт рядом с настройками: это пользовательский пресет. */
    val bellSchedule: Flow<BellSchedule> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> BellSchedule.decode(prefs[Keys.BELL_SCHEDULE]) }

    suspend fun snapshotBellSchedule(): BellSchedule = bellSchedule.first()

    suspend fun setBellSchedule(schedule: BellSchedule) {
        dataStore.edit { prefs ->
            if (schedule.isEmpty) {
                prefs.remove(Keys.BELL_SCHEDULE)
            } else {
                prefs[Keys.BELL_SCHEDULE] = schedule.encode()
            }
        }
    }

    suspend fun setEnabled(value: Boolean) {
        dataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setLockScreenVisible(value: Boolean) {
        dataStore.edit { it[Keys.LOCK_SCREEN_VISIBLE] = value }
    }

    suspend fun setShowTitle(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_TITLE] = value }
    }

    suspend fun setShowRoom(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_ROOM] = value }
    }

    suspend fun setShowNextLesson(value: Boolean) {
        dataStore.edit { it[Keys.SHOW_NEXT_LESSON] = value }
    }

    suspend fun setCompactMode(value: Boolean) {
        dataStore.edit { it[Keys.COMPACT_MODE] = value }
    }

    suspend fun setTheme(theme: AppTheme) {
        dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { it[Keys.LANGUAGE] = language.name }
    }

    suspend fun setAnchorDate(date: LocalDate) {
        dataStore.edit { it[Keys.ANCHOR_EPOCH_DAY] = date.toEpochDay() }
    }

    suspend fun setAnchorOdd(anchorIsOdd: Boolean) {
        dataStore.edit { it[Keys.ANCHOR_IS_ODD] = anchorIsOdd }
    }

    /**
     * Сдвигает опорную неделю так, чтобы текущая неделя получила нужную чётность.
     * Если она уже совпадает, ничего не меняется.
     */
    suspend fun overrideCurrentWeekParity(currentWeekIsOdd: Boolean) {
        dataStore.edit { prefs ->
            val anchorDate = prefs.anchorDate()
            val anchorIsOdd = prefs[Keys.ANCHOR_IS_ODD] ?: true
            val actualIsOdd = WeekParityCalculator.isOddWeek(LocalDate.now(), anchorDate, anchorIsOdd)
            if (actualIsOdd != currentWeekIsOdd) {
                prefs[Keys.ANCHOR_EPOCH_DAY] = anchorDate.plusWeeks(1).toEpochDay()
            }
        }
    }

    /** Возвращает опорную неделю к 1 сентября текущего года (нечётная). */
    suspend fun resetAnchorWeek() {
        dataStore.edit { prefs ->
            prefs[Keys.ANCHOR_EPOCH_DAY] = AppSettings.defaultAnchorDate().toEpochDay()
            prefs[Keys.ANCHOR_IS_ODD] = true
        }
    }

    suspend fun markNotificationPermissionRequested() {
        dataStore.edit { it[Keys.PERMISSION_REQUESTED] = true }
    }

    private fun Preferences.anchorDate(): LocalDate = this[Keys.ANCHOR_EPOCH_DAY]
        ?.let(LocalDate::ofEpochDay)
        ?: AppSettings.defaultAnchorDate()

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        enabled = this[Keys.ENABLED] ?: true,
        lockScreenVisible = this[Keys.LOCK_SCREEN_VISIBLE] ?: true,
        showTitle = this[Keys.SHOW_TITLE] ?: true,
        showRoom = this[Keys.SHOW_ROOM] ?: true,
        showNextLesson = this[Keys.SHOW_NEXT_LESSON] ?: true,
        compactMode = this[Keys.COMPACT_MODE] ?: false,
        theme = this[Keys.THEME].toEnum(AppTheme.SYSTEM),
        language = this[Keys.LANGUAGE].toEnum(AppLanguage.SYSTEM),
        anchorDate = anchorDate(),
        anchorIsOdd = this[Keys.ANCHOR_IS_ODD] ?: true,
        notificationPermissionRequested = this[Keys.PERMISSION_REQUESTED] ?: false,
    )

    private inline fun <reified T : Enum<T>> String?.toEnum(fallback: T): T =
        this?.let { name -> enumValues<T>().firstOrNull { it.name == name } } ?: fallback

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val LOCK_SCREEN_VISIBLE = booleanPreferencesKey("lock_screen_visible")
        val SHOW_TITLE = booleanPreferencesKey("show_title")
        val SHOW_ROOM = booleanPreferencesKey("show_room")
        val SHOW_NEXT_LESSON = booleanPreferencesKey("show_next_lesson")
        val COMPACT_MODE = booleanPreferencesKey("compact_mode")
        val THEME = stringPreferencesKey("theme")
        val LANGUAGE = stringPreferencesKey("language")
        val ANCHOR_EPOCH_DAY = longPreferencesKey("anchor_epoch_day")
        val ANCHOR_IS_ODD = booleanPreferencesKey("anchor_is_odd")
        val BELL_SCHEDULE = stringPreferencesKey("bell_schedule")
        val PERMISSION_REQUESTED = booleanPreferencesKey("notification_permission_requested")
    }
}
