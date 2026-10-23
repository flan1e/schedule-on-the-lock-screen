package com.flan1e.schedule.notification

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.flan1e.schedule.data.repository.SettingsRepository
import com.flan1e.schedule.domain.model.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Единая точка запуска и остановки foreground-службы.
 * Вызывать только из пользовательского действия или при старте приложения:
 * так не нарушаются ограничения Android на фоновый запуск служб.
 */
class ScheduleServiceController(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope,
) {

    fun sync() {
        scope.launch { apply(settingsRepository.snapshot()) }
    }

    fun apply(settings: AppSettings) {
        if (settings.showingOnLockScreen) start() else stop()
    }

    fun stop() {
        context.stopService(Intent(context, ScheduleForegroundService::class.java))
        NotificationManagerCompat.from(context).cancel(ScheduleNotifier.NOTIFICATION_ID)
    }

    private fun start() {
        ContextCompat.startForegroundService(
            context,
            Intent(context, ScheduleForegroundService::class.java),
        )
    }
}
