package com.flan1e.schedule.domain.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * Определяет чётность учебной недели относительно опорной даты.
 *
 * Неделя начинается с понедельника (ISO-8601). Опорной считается неделя,
 * в которую попадает [anchorDate]; её чётность задаёт пользователь.
 */
object WeekParityCalculator {

    fun isOddWeek(date: LocalDate, anchorDate: LocalDate, anchorIsOdd: Boolean): Boolean {
        val anchorMonday = anchorDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dateMonday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekOffset = ChronoUnit.WEEKS.between(anchorMonday, dateMonday)
        // floorMod нужен для дат до опорной недели: отрицательный остаток не должен ломать чётность.
        return if (Math.floorMod(weekOffset, 2L) == 0L) anchorIsOdd else !anchorIsOdd
    }
}
