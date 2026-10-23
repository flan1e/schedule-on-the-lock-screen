package com.flan1e.schedule.domain.model

import java.time.LocalTime

private const val MINUTES_PER_DAY = 24 * 60

/**
 * Пара в сетке звонков. Номер пары не хранится: это позиция в списке,
 * поэтому удаление пары автоматически перенумеровывает остальные.
 */
data class BellPeriod(
    val startMinute: Int,
    val endMinute: Int,
) {
    val crossesMidnight: Boolean
        get() = endMinute <= startMinute

    val durationMinutes: Int
        get() = if (crossesMidnight) {
            endMinute + MINUTES_PER_DAY - startMinute
        } else {
            endMinute - startMinute
        }
}

/**
 * Сетка звонков: упорядоченный список пар. Хранится в настройках строкой
 * `510-600,610-700`, чтобы не заводить отдельную таблицу ради пяти чисел.
 */
data class BellSchedule(
    val periods: List<BellPeriod> = emptyList(),
) {

    val isEmpty: Boolean
        get() = periods.isEmpty()

    /** Номер пары для интервала времени или null, если интервала нет в сетке. */
    fun numberOf(start: LocalTime, end: LocalTime): Int? {
        val index = indexOf(start, end)
        return if (index >= 0) index + 1 else null
    }

    /** Позиция пары в сетке или -1. */
    fun indexOf(start: LocalTime, end: LocalTime): Int {
        val startMinutes = start.hour * 60 + start.minute
        val endMinutes = end.hour * 60 + end.minute
        return periods.indexOfFirst {
            it.startMinute == startMinutes && it.endMinute == endMinutes
        }
    }

    fun encode(): String = periods.joinToString(separator = ",") {
        "${it.startMinute}-${it.endMinute}"
    }

    companion object {

        val Empty = BellSchedule()

        /** Типовая сетка на пять пар — чтобы не вводить всё руками. */
        fun standard(): BellSchedule = BellSchedule(
            listOf(
                BellPeriod(8 * 60 + 30, 10 * 60),
                BellPeriod(10 * 60 + 10, 11 * 60 + 40),
                BellPeriod(12 * 60 + 10, 13 * 60 + 40),
                BellPeriod(13 * 60 + 50, 15 * 60 + 20),
                BellPeriod(15 * 60 + 30, 17 * 60),
            ),
        )

        /** Битые и устаревшие записи молча превращаются в пустую сетку. */
        fun decode(raw: String?): BellSchedule {
            if (raw.isNullOrBlank()) return Empty
            val periods = raw.split(",").mapNotNull { part ->
                val bounds = part.split("-")
                if (bounds.size != 2) return@mapNotNull null
                val start = bounds[0].trim().toIntOrNull() ?: return@mapNotNull null
                val end = bounds[1].trim().toIntOrNull() ?: return@mapNotNull null
                if (start !in 0 until MINUTES_PER_DAY || end !in 0 until MINUTES_PER_DAY) {
                    return@mapNotNull null
                }
                if (start == end) return@mapNotNull null
                BellPeriod(start, end)
            }
            return BellSchedule(periods)
        }
    }
}
