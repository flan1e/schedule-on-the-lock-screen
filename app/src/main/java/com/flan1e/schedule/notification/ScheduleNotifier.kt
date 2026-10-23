package com.flan1e.schedule.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.flan1e.schedule.MainActivity
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.AppSettings
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.LessonOccurrence
import com.flan1e.schedule.domain.model.ScheduleState
import com.flan1e.schedule.util.DateTimeText
import com.flan1e.schedule.util.DurationText
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Собирает постоянное уведомление. Обратный отсчёт отдан системному
 * chronometer: секунды тикает сама система, приложению не нужно просыпаться
 * каждую секунду.
 */
class ScheduleNotifier(private val context: Context) {

    fun buildLoading(): Notification = baseBuilder()
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(context.getString(R.string.notification_preparing))
        .setShowWhen(false)
        .build()

    fun build(
        state: ScheduleState,
        settings: AppSettings,
        now: LocalDateTime = LocalDateTime.now(),
    ): Notification {
        val content = contentFor(state, settings, now)
        val builder = baseBuilder()
            .setContentTitle(content.title)
            .setContentText(content.text)

        if (content.countdownTargetMillis != null) {
            builder
                .setWhen(content.countdownTargetMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
        } else {
            builder.setShowWhen(false)
        }

        val bigText = content.bigText
        if (!settings.compactMode && bigText != null) {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
        }

        return builder.build()
    }

    private fun baseBuilder(): NotificationCompat.Builder = NotificationCompat.Builder(
        context,
        NotificationChannels.CHANNEL_ID,
    )
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setSilent(true)
        .setCategory(NotificationCompat.CATEGORY_STATUS)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        .setContentIntent(openAppIntent())
        .addAction(
            0,
            context.getString(R.string.notification_action_hide),
            actionIntent(NotificationActionReceiver.ACTION_HIDE),
        )
        .addAction(
            0,
            context.getString(R.string.notification_action_disable),
            actionIntent(NotificationActionReceiver.ACTION_DISABLE),
        )

    private fun contentFor(
        state: ScheduleState,
        settings: AppSettings,
        now: LocalDateTime,
    ): NotificationContent = when (state) {
        is ScheduleState.InProgress -> {
            val lesson = state.current.lesson
            val details = detailsLine(lesson, settings)
            val countdown = context.getString(
                R.string.notification_until_end,
                DurationText.format(context, state.remaining),
            )
            val following = followingLine(state.following, settings, now)
            NotificationContent(
                title = ongoingTitle(lesson, settings),
                text = headlineText(countdown, lesson, settings),
                bigText = listOfNotNull(details, countdown, following).joinToString("\n"),
                countdownTargetMillis = state.current.end.toEpochMillis(),
            )
        }

        is ScheduleState.Upcoming -> {
            val lesson = state.next.lesson
            val details = detailsLine(lesson, settings)
            // Для пары не на сегодня добавляем день, иначе «До начала 14 ч» непонятно когда.
            val dayLine = if (state.next.date == now.toLocalDate()) {
                details
            } else {
                DateTimeText.relativeDateLabel(state.next.date, now.toLocalDate(), context) + ", " + details
            }
            val countdown = context.getString(
                R.string.notification_until_start,
                DurationText.format(context, state.untilStart),
            )
            val following = followingLine(state.following, settings, now)
            NotificationContent(
                title = upcomingTitle(lesson, settings),
                text = headlineText(countdown, lesson, settings),
                bigText = listOfNotNull(dayLine, countdown, following).joinToString("\n"),
                countdownTargetMillis = state.next.start.toEpochMillis(),
            )
        }

        is ScheduleState.TodayFinished -> {
            val text = nextText(state.next, settings, now)
            NotificationContent(
                title = context.getString(R.string.notification_today_finished),
                text = text,
                bigText = text.takeIf { state.next != null },
                countdownTargetMillis = null,
            )
        }

        is ScheduleState.TodayEmpty -> {
            val text = nextText(state.next, settings, now)
            NotificationContent(
                title = context.getString(R.string.notification_today_empty),
                text = text,
                bigText = text.takeIf { state.next != null },
                countdownTargetMillis = null,
            )
        }

        ScheduleState.NoSchedule -> NotificationContent(
            title = context.getString(R.string.notification_no_schedule),
            text = context.getString(R.string.notification_no_schedule_text),
            bigText = null,
            countdownTargetMillis = null,
        )
    }

    /** Заголовок идущей пары: «Идёт: Матанализ». */
    private fun ongoingTitle(lesson: Lesson, settings: AppSettings): String =
        if (settings.showTitle) {
            context.getString(R.string.notification_title_ongoing, lesson.title)
        } else {
            context.getString(R.string.notification_lesson_ongoing)
        }

    /** Заголовок следующей пары: «Следующая: Физика». */
    private fun upcomingTitle(lesson: Lesson, settings: AppSettings): String =
        if (settings.showTitle) {
            context.getString(R.string.notification_next, lesson.title)
        } else {
            context.getString(R.string.notification_lesson_next)
        }

    /**
     * Строка, видимая в свёрнутом уведомлении: сам отсчёт и, если включён,
     * кабинет. Хронометр справа дополняет её секундами.
     */
    private fun headlineText(countdown: String, lesson: Lesson, settings: AppSettings): String {
        if (!settings.showRoom || lesson.room.isBlank()) return countdown
        val room = context.getString(R.string.room_prefix, lesson.room)
        return context.getString(R.string.details_with_room, countdown, room)
    }

    private fun detailsLine(lesson: Lesson, settings: AppSettings): String {
        val range = DateTimeText.timeRange(lesson, context)
        if (!settings.showRoom || lesson.room.isBlank()) return range
        val room = context.getString(R.string.room_prefix, lesson.room)
        return context.getString(R.string.details_with_room, range, room)
    }

    private fun followingLine(
        following: LessonOccurrence?,
        settings: AppSettings,
        now: LocalDateTime,
    ): String? {
        if (!settings.showNextLesson || following == null) return null
        return context.getString(R.string.notification_next_after, occurrenceSummary(following, settings, now))
    }

    private fun nextText(
        next: LessonOccurrence?,
        settings: AppSettings,
        now: LocalDateTime,
    ): String = if (next == null) {
        context.getString(R.string.notification_no_schedule_text)
    } else {
        context.getString(R.string.notification_next, occurrenceSummary(next, settings, now))
    }

    private fun occurrenceSummary(
        occurrence: LessonOccurrence,
        settings: AppSettings,
        now: LocalDateTime,
    ): String {
        val parts = mutableListOf(
            DateTimeText.relativeDateLabel(occurrence.date, now.toLocalDate(), context),
            DateTimeText.timeRange(occurrence.lesson, context),
        )
        if (settings.showRoom && occurrence.lesson.room.isNotBlank()) {
            parts += context.getString(R.string.room_prefix, occurrence.lesson.room)
        }
        if (settings.showTitle) {
            parts += occurrence.lesson.title
        }
        return parts.joinToString(", ")
    }

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private data class NotificationContent(
        val title: String,
        val text: String,
        val bigText: String?,
        val countdownTargetMillis: Long?,
    )

    companion object {
        const val NOTIFICATION_ID = 1001
        private const val REQUEST_OPEN_APP = 2001
    }
}
