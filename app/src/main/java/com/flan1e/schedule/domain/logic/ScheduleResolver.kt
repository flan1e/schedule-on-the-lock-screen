package com.flan1e.schedule.domain.logic

import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.LessonOccurrence
import com.flan1e.schedule.domain.model.ScheduleState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Находит текущую или ближайшую пару с учётом чётности недели.
 *
 * Поиск идёт от вчерашнего дня (чтобы поймать пару, начавшуюся вчера и
 * перешедшую через полночь) до [searchHorizonDays] вперёд. Горизонт в 21 день
 * перекрывает двухнедельный цикл чётности с запасом.
 */
class ScheduleResolver(
    private val searchHorizonDays: Long = 21,
) {

    fun resolve(
        now: LocalDateTime,
        lessons: List<Lesson>,
        anchorDate: LocalDate,
        anchorIsOdd: Boolean,
    ): ScheduleState {
        if (lessons.isEmpty()) return ScheduleState.NoSchedule

        val today = now.toLocalDate()
        val occurrences = buildOccurrences(today, lessons, anchorDate, anchorIsOdd)

        val current = occurrences
            .asSequence()
            .filter { !now.isBefore(it.start) && now.isBefore(it.end) }
            .maxByOrNull { it.start }
        if (current != null) {
            return ScheduleState.InProgress(
                current = current,
                remaining = Duration.between(now, current.end),
                following = nextAfter(occurrences, current),
            )
        }

        val upcoming = occurrences
            .asSequence()
            .filter { it.start.isAfter(now) }
            .minByOrNull { it.start }
        val hasLessonsToday = occurrences.any { it.date == today }

        return when {
            upcoming != null && upcoming.date == today -> ScheduleState.Upcoming(
                next = upcoming,
                untilStart = Duration.between(now, upcoming.start),
                following = nextAfter(occurrences, upcoming),
            )

            hasLessonsToday -> ScheduleState.TodayFinished(upcoming)
            else -> ScheduleState.TodayEmpty(upcoming)
        }
    }

    private fun nextAfter(
        occurrences: List<LessonOccurrence>,
        occurrence: LessonOccurrence,
    ): LessonOccurrence? = occurrences
        .asSequence()
        .filter { it.start.isAfter(occurrence.start) }
        .minByOrNull { it.start }

    private fun buildOccurrences(
        today: LocalDate,
        lessons: List<Lesson>,
        anchorDate: LocalDate,
        anchorIsOdd: Boolean,
    ): List<LessonOccurrence> {
        val occurrences = mutableListOf<LessonOccurrence>()
        val lastDate = today.plusDays(searchHorizonDays)
        var date = today.minusDays(1)

        while (!date.isAfter(lastDate)) {
            val weekIsOdd = WeekParityCalculator.isOddWeek(date, anchorDate, anchorIsOdd)
            for (lesson in lessons) {
                if (lesson.dayOfWeek != date.dayOfWeek || !lesson.parity.matches(weekIsOdd)) continue
                val start = date.atTime(lesson.start)
                val end = if (lesson.crossesMidnight) {
                    date.plusDays(1).atTime(lesson.end)
                } else {
                    date.atTime(lesson.end)
                }
                occurrences += LessonOccurrence(
                    lesson = lesson,
                    date = date,
                    start = start,
                    end = end,
                )
            }
            date = date.plusDays(1)
        }

        return occurrences.sortedBy { it.start }
    }
}
