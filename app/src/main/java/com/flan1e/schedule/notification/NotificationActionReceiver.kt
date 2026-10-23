package com.flan1e.schedule.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.flan1e.schedule.ScheduleApplication
import kotlinx.coroutines.launch

/**
 * Кнопки в уведомлении. «Скрыть» выключает показ на lock screen,
 * «Отключить» — мастер-переключатель. Приложение при этом не блокируется
 * навсегда: и то, и другое возвращается из приложения или плитки.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ACTION_HIDE && action != ACTION_DISABLE) return

        val container = (context.applicationContext as ScheduleApplication).container
        val pendingResult = goAsync()
        container.applicationScope.launch {
            try {
                when (action) {
                    ACTION_HIDE -> container.settingsRepository.setLockScreenVisible(false)
                    ACTION_DISABLE -> container.settingsRepository.setEnabled(false)
                }
                container.serviceController.apply(container.settingsRepository.snapshot())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_HIDE = "com.flan1e.schedule.action.HIDE"
        const val ACTION_DISABLE = "com.flan1e.schedule.action.DISABLE"
    }
}
