package com.flan1e.schedule.domain.model

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

/**
 * Пара из расписания. Время хранится локально, без привязки к часовому поясу:
 * расписание пользователя живёт в его местном времени.
 */
data class Lesson(
    val id: Long = 0L,
    val dayOfWeek: DayOfWeek,
    val start: LocalTime,
    val end: LocalTime,
    val title: String,
    val room: String,
    val parity: WeekParity = WeekParity.EVERY_WEEK,
    val kind: LessonKind = LessonKind.OTHER,
) {
    /**
     * Пара переходит через полночь, если окончание не позже начала
     * (например, 23:00–00:30). Чётность такой пары определяется по дню начала.
     */
    val crossesMidnight: Boolean
        get() = !end.isAfter(start)

    val duration: Duration
        get() = if (crossesMidnight) {
            Duration.between(start, end).plusDays(1)
        } else {
            Duration.between(start, end)
        }
}
