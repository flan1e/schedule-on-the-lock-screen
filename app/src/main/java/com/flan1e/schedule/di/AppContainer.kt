package com.flan1e.schedule.di

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.flan1e.schedule.data.local.AppDatabase
import com.flan1e.schedule.data.repository.RoomScheduleRepository
import com.flan1e.schedule.data.repository.ScheduleRepository
import com.flan1e.schedule.data.repository.SettingsRepository
import com.flan1e.schedule.domain.logic.ScheduleResolver
import com.flan1e.schedule.notification.ScheduleNotifier
import com.flan1e.schedule.notification.ScheduleServiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Ручной DI-контейнер уровня приложения: без Hilt, весь граф зависимостей
 * виден в одном месте.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** Живёт столько же, сколько процесс: для DataStore и фоновых операций. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "schedule.db",
    ).build()

    val scheduleRepository: ScheduleRepository = RoomScheduleRepository(database.lessonDao())
    val settingsRepository: SettingsRepository
    val resolver = ScheduleResolver()
    val notifier = ScheduleNotifier(appContext)
    val serviceController: ScheduleServiceController

    init {
        val dataStore = PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            scope = applicationScope,
            produceFile = { appContext.preferencesDataStoreFile("settings") },
        )
        settingsRepository = SettingsRepository(dataStore)
        serviceController = ScheduleServiceController(
            context = appContext,
            settingsRepository = settingsRepository,
            scope = applicationScope,
        )
    }
}
