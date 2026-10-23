package com.flan1e.schedule.domain

import com.flan1e.schedule.domain.model.BellPeriod
import com.flan1e.schedule.domain.model.BellSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class BellScheduleTest {

    @Test
    fun `standard grid has five periods`() {
        val schedule = BellSchedule.standard()

        assertEquals(5, schedule.periods.size)
        assertEquals(BellPeriod(8 * 60 + 30, 10 * 60), schedule.periods.first())
        assertEquals(90, schedule.periods.first().durationMinutes)
    }

    @Test
    fun `encode and decode round trip`() {
        val schedule = BellSchedule(
            listOf(
                BellPeriod(510, 600),
                BellPeriod(610, 700),
            ),
        )

        assertEquals("510-600,610-700", schedule.encode())
        assertEquals(schedule, BellSchedule.decode(schedule.encode()))
    }

    @Test
    fun `broken records are ignored`() {
        val schedule = BellSchedule.decode("510-600,oops,610,610-610,2000-2100,620-700")

        assertEquals(
            listOf(BellPeriod(510, 600), BellPeriod(620, 700)),
            schedule.periods,
        )
    }

    @Test
    fun `empty and null decode to empty schedule`() {
        assertTrue(BellSchedule.decode(null).isEmpty)
        assertTrue(BellSchedule.decode("").isEmpty)
        assertTrue(BellSchedule.decode("   ").isEmpty)
    }

    @Test
    fun `number of period is one based`() {
        val schedule = BellSchedule.standard()

        assertEquals(1, schedule.numberOf(LocalTime.of(8, 30), LocalTime.of(10, 0)))
        assertEquals(3, schedule.numberOf(LocalTime.of(12, 10), LocalTime.of(13, 40)))
        assertNull(schedule.numberOf(LocalTime.of(9, 0), LocalTime.of(10, 0)))
    }

    @Test
    fun `period across midnight has correct duration`() {
        val period = BellPeriod(23 * 60 + 30, 50)

        assertTrue(period.crossesMidnight)
        assertEquals(80, period.durationMinutes)
    }

    @Test
    fun `cleared schedule is empty`() {
        val schedule = BellSchedule(emptyList())

        assertTrue(schedule.isEmpty)
        assertFalse(BellSchedule.standard().isEmpty)
    }
}
