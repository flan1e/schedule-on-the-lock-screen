package com.flan1e.schedule.notification

import android.app.Notification
import android.app.Service
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.flan1e.schedule.ScheduleApplication
import com.flan1e.schedule.di.AppContainer
import com.flan1e.schedule.domain.model.AppSettings
import com.flan1e.schedule.domain.model.Lesson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Foreground-служба, которая держит постоянное уведомление актуальным.
 *
 * Тип specialUse выбран осознанно: dataSync на Android 15+ ограничен
 * шестью часами в сутки, а здесь нужен постоянный режим. Уведомление
 * обновляется на границе каждой минуты и сразу после изменения настроек
 * или расписания.
 */
class ScheduleForegroundService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var container: AppContainer

    /** Последнее состояние нужно, чтобы перерисовать уведомление при смене темы. */
    @Volatile
    private var lastTick: Tick? = null

    /**
     * Некоторые прошивки (MIUI/HyperOS) замораживают процесс, пока экран выключен
     * или устройство заблокировано, поэтому минутный тик может не сработать.
     * При включении экрана и разблокировке пересчитываем состояние сразу.
     */
    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastTick?.let { post(it.copy(now = LocalDateTime.now())) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        container = (application as ScheduleApplication).container
        NotificationChannels.ensure(this)
        startInForeground(container.notifier.buildLoading())

        val wakeFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            wakeReceiver,
            wakeFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        scope.launch {
            combine(
                container.scheduleRepository.observeLessons(),
                container.settingsRepository.settings,
                minuteTicker(),
            ) { lessons, settings, now -> Tick(lessons, settings, now) }
                .collect { tick ->
                    lastTick = tick
                    post(tick)
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * При смене системной темы цвета текста в уведомлении нужно пересчитать
     * сразу, не дожидаясь следующего тика.
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        lastTick?.let(::post)
    }

    private fun post(tick: Tick) {
        if (!tick.settings.showingOnLockScreen) {
            stopSelf()
            return
        }
        val state = container.resolver.resolve(
            now = tick.now,
            lessons = tick.lessons,
            anchorDate = tick.settings.anchorDate,
            anchorIsOdd = tick.settings.anchorIsOdd,
        )
        // На Android 13+ без разрешения уведомление не показывается,
        // но служба продолжает работать и обновится после выдачи разрешения.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(
            ScheduleNotifier.NOTIFICATION_ID,
            container.notifier.build(state, tick.settings, tick.now),
        )
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { unregisterReceiver(wakeReceiver) }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                ScheduleNotifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(ScheduleNotifier.NOTIFICATION_ID, notification)
        }
    }

    /** Тик ровно на границе минуты — чтобы смена пары происходила без задержки. */
    private fun minuteTicker(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = LocalDateTime.now()
            emit(now)
            val millisIntoMinute = now.second * 1000L + now.nano / 1_000_000L
            delay((60_000L - millisIntoMinute).coerceAtLeast(1_000L))
        }
    }

    private data class Tick(
        val lessons: List<Lesson>,
        val settings: AppSettings,
        val now: LocalDateTime,
    )
}
