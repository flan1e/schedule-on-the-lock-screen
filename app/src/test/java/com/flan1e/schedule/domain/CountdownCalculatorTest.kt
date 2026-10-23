package com.flan1e.schedule.domain

import com.flan1e.schedule.domain.logic.CountdownCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime

class CountdownCalculatorTest {

    @Test
    fun `остаток округляется вниз до минут`() {
        assertEquals(45L, CountdownCalculator.remainingMinutes(Duration.ofSeconds(45 * 60L + 12)))
    }

    @Test
    fun `меньше минуты даёт ноль`() {
        assertEquals(0L, CountdownCalculator.remainingMinutes(Duration.ofSeconds(59)))
        assertEquals(0L, CountdownCalculator.remainingMinutes(Duration.ofSeconds(1)))
    }

    @Test
    fun `ровные минуты не меняются`() {
        assertEquals(90L, CountdownCalculator.remainingMinutes(Duration.ofMinutes(90)))
    }

    @Test
    fun `нулевой и отрицательный остаток дают ноль`() {
        assertEquals(0L, CountdownCalculator.remainingMinutes(Duration.ZERO))
        assertEquals(0L, CountdownCalculator.remainingMinutes(Duration.ofMinutes(-5)))
    }

    @Test
    fun `прогресс пары считается в процентах`() {
        val start = LocalDateTime.of(2026, 10, 24, 10, 0)
        val end = LocalDateTime.of(2026, 10, 24, 11, 30)
        assertEquals(0, CountdownCalculator.progressPercent(start, end, start))
        assertEquals(50, CountdownCalculator.progressPercent(start, end, start.plusMinutes(45)))
        assertEquals(100, CountdownCalculator.progressPercent(start, end, end))
    }

    @Test
    fun `прогресс не выходит за границы 0 и 100`() {
        val start = LocalDateTime.of(2026, 10, 24, 10, 0)
        val end = LocalDateTime.of(2026, 10, 24, 11, 30)
        assertEquals(0, CountdownCalculator.progressPercent(start, end, start.minusMinutes(30)))
        assertEquals(100, CountdownCalculator.progressPercent(start, end, end.plusMinutes(30)))
    }

    @Test
    fun `пара нулевой длины даёт нулевой прогресс`() {
        val moment = LocalDateTime.of(2026, 10, 24, 10, 0)
        assertEquals(0, CountdownCalculator.progressPercent(moment, moment, moment))
    }
}
