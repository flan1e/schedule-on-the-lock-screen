package com.flan1e.schedule.domain

import com.flan1e.schedule.domain.logic.CountdownCalculator
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class CountdownCalculatorTest {

    @Test
    fun `остаток округляется вверх до минут`() {
        assertEquals(46L, CountdownCalculator.remainingMinutes(Duration.ofSeconds(45 * 60L + 12)))
    }

    @Test
    fun `меньше минуты округляется до одной минуты`() {
        assertEquals(1L, CountdownCalculator.remainingMinutes(Duration.ofSeconds(59)))
        assertEquals(1L, CountdownCalculator.remainingMinutes(Duration.ofSeconds(1)))
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
}
