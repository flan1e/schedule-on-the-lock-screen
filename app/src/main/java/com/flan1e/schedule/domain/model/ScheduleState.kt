package com.flan1e.schedule.domain.model

import java.time.Duration

/**
 * Состояние расписания в конкретный момент времени.
 */
sealed interface ScheduleState {

    /** Пара идёт прямо сейчас. */
    data class InProgress(
        val current: LessonOccurrence,
        val remaining: Duration,
        val following: LessonOccurrence?,
    ) : ScheduleState

    /** Пара ещё не началась; [next] — сегодня или в последующие дни. */
    data class Upcoming(
        val next: LessonOccurrence,
        val untilStart: Duration,
        val following: LessonOccurrence?,
    ) : ScheduleState

    /** Пары на сегодня были, но уже закончились. */
    data class TodayFinished(
        val next: LessonOccurrence?,
    ) : ScheduleState

    /** На сегодня пар нет (по дню недели или по чётности). */
    data class TodayEmpty(
        val next: LessonOccurrence?,
    ) : ScheduleState

    /** В расписании нет ни одной пары. */
    data object NoSchedule : ScheduleState
}
