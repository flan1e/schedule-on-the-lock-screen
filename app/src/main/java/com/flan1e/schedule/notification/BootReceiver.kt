package com.flan1e.schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flan1e.schedule.ScheduleApplication
import kotlinx.coroutines.launch

/**
 * Восстановление уведомления после перезагрузки, обновления приложения
 * и смены времени/часового пояса.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as ScheduleApplication).container
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                if (intent.action in SUPPORTED_ACTIONS) {
                    container.serviceController.apply(container.settingsRepository.snapshot())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
        )
    }
}
