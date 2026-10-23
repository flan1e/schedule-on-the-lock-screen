package com.flan1e.schedule.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Конкретное вхождение пары в календаре: пара + дата, к которой она относится.
 * [date] — дата начала (для пар через полночь окончание будет на следующий день).
 */
data class LessonOccurrence(
    val lesson: Lesson,
    val date: LocalDate,
    val start: LocalDateTime,
    val end: LocalDateTime,
)
