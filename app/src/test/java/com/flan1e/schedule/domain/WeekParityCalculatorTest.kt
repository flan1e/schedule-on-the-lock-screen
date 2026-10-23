package com.flan1e.schedule.domain

import com.flan1e.schedule.domain.logic.WeekParityCalculator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class WeekParityCalculatorTest {

    // 1 сентября 2025 — понедельник, опорная неделя нечётная.
    private val anchor: LocalDate = LocalDate.of(2025, 9, 1)
    private val anchorIsOdd = true

    private fun isOdd(date: LocalDate, anchorDate: LocalDate = anchor, anchorOdd: Boolean = anchorIsOdd) =
        WeekParityCalculator.isOddWeek(date, anchorDate, anchorOdd)

    @Test
    fun `неделя опорной даты имеет заданную чётность`() {
        assertTrue(isOdd(anchor))
        assertTrue(isOdd(LocalDate.of(2025, 9, 7))) // воскресенье той же недели
    }

    @Test
    fun `чётность чередуется каждую неделю`() {
        assertFalse(isOdd(LocalDate.of(2025, 9, 8)))
        assertTrue(isOdd(LocalDate.of(2025, 9, 15)))
        assertFalse(isOdd(LocalDate.of(2025, 9, 22)))
    }

    @Test
    fun `даты до опорной недели считаются корректно`() {
        assertFalse(isOdd(LocalDate.of(2025, 8, 25)))
        assertTrue(isOdd(LocalDate.of(2025, 8, 18)))
    }

    @Test
    fun `опорная дата в середине недели использует свой понедельник`() {
        val midWeekAnchor = LocalDate.of(2025, 9, 4) // четверг
        assertTrue(isOdd(LocalDate.of(2025, 9, 1), anchorDate = midWeekAnchor))
        assertFalse(isOdd(LocalDate.of(2025, 9, 8), anchorDate = midWeekAnchor))
    }

    @Test
    fun `переход через границу года не ломает чётность`() {
        // 2025-12-29 — понедельник, 17 недель после опорной.
        assertFalse(isOdd(LocalDate.of(2025, 12, 29)))
        assertTrue(isOdd(LocalDate.of(2026, 1, 5)))
    }

    @Test
    fun `опорная неделя может быть задана чётной`() {
        assertFalse(isOdd(anchor, anchorOdd = false))
        assertTrue(isOdd(LocalDate.of(2025, 9, 8), anchorOdd = false))
    }
}
