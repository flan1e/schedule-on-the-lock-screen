package com.flan1e.schedule.domain

import com.flan1e.schedule.domain.logic.ScheduleResolver
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.ScheduleState
import com.flan1e.schedule.domain.model.WeekParity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ScheduleResolverTest {

    private val resolver = ScheduleResolver()

    // Опорная неделя: 1 сентября 2025 (понедельник), нечётная.
    private val anchor: LocalDate = LocalDate.of(2025, 9, 1)
    private val anchorIsOdd = true

    private fun lesson(
        day: DayOfWeek,
        start: String,
        end: String,
        title: String = "Пара",
        room: String = "305",
        parity: WeekParity = WeekParity.EVERY_WEEK,
        id: Long = 1L,
    ) = Lesson(
        id = id,
        dayOfWeek = day,
        start = LocalTime.parse(start),
        end = LocalTime.parse(end),
        title = title,
        room = room,
        parity = parity,
    )

    private fun resolve(
        now: String,
        lessons: List<Lesson>,
        anchorDate: LocalDate = anchor,
        anchorOdd: Boolean = anchorIsOdd,
    ): ScheduleState = resolver.resolve(
        now = LocalDateTime.parse(now),
        lessons = lessons,
        anchorDate = anchorDate,
        anchorIsOdd = anchorOdd,
    )

    @Test
    fun `пара идёт прямо сейчас`() {
        val state = resolve(
            now = "2025-09-01T10:45",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30", title = "Математика")),
        )

        assertTrue(state is ScheduleState.InProgress)
        state as ScheduleState.InProgress
        assertEquals("Математика", state.current.lesson.title)
        assertEquals(45L, state.remaining.toMinutes())
    }

    @Test
    fun `ровно в момент начала пара считается идущей`() {
        val state = resolve(
            now = "2025-09-01T10:00",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30")),
        )

        assertTrue(state is ScheduleState.InProgress)
        assertEquals(90L, (state as ScheduleState.InProgress).remaining.toMinutes())
    }

    @Test
    fun `ровно в момент окончания пара уже не идёт`() {
        val state = resolve(
            now = "2025-09-01T11:30",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30")),
        )

        assertTrue(state is ScheduleState.TodayFinished)
        assertEquals(LocalDate.of(2025, 9, 8), (state as ScheduleState.TodayFinished).next?.date)
    }

    @Test
    fun `до первой пары показывается ближайшая`() {
        val state = resolve(
            now = "2025-09-01T09:00",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30")),
        )

        assertTrue(state is ScheduleState.Upcoming)
        state as ScheduleState.Upcoming
        assertEquals(60L, state.untilStart.toMinutes())
    }

    @Test
    fun `в перерыве показывается следующая пара`() {
        val lessons = listOf(
            lesson(DayOfWeek.MONDAY, "10:00", "11:00", id = 1),
            lesson(DayOfWeek.MONDAY, "12:00", "13:00", id = 2),
        )

        val state = resolve(now = "2025-09-01T11:30", lessons = lessons)

        assertTrue(state is ScheduleState.Upcoming)
        state as ScheduleState.Upcoming
        assertEquals(LocalTime.parse("12:00"), state.next.lesson.start)
        assertEquals(30L, state.untilStart.toMinutes())
    }

    @Test
    fun `после последней пары день считается законченным`() {
        val state = resolve(
            now = "2025-09-01T14:00",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30")),
        )

        assertTrue(state is ScheduleState.TodayFinished)
        assertEquals(LocalDate.of(2025, 9, 8), (state as ScheduleState.TodayFinished).next?.date)
    }

    @Test
    fun `если сегодня пар нет показывается следующая`() {
        val state = resolve(
            now = "2025-09-02T10:00", // вторник
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30")),
        )

        assertTrue(state is ScheduleState.TodayEmpty)
        assertEquals(LocalDate.of(2025, 9, 8), (state as ScheduleState.TodayEmpty).next?.date)
    }

    @Test
    fun `после последней пары следующая может быть завтра`() {
        val lessons = listOf(
            lesson(DayOfWeek.MONDAY, "10:00", "11:00", id = 1),
            lesson(DayOfWeek.TUESDAY, "09:00", "10:00", id = 2),
        )

        val state = resolve(now = "2025-09-01T14:00", lessons = lessons)

        assertTrue(state is ScheduleState.TodayFinished)
        assertEquals(LocalDate.of(2025, 9, 2), (state as ScheduleState.TodayFinished).next?.date)
    }

    @Test
    fun `пара нечётной недели пропускается на чётной неделе`() {
        val state = resolve(
            now = "2025-09-08T09:00", // чётная неделя
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30", parity = WeekParity.ODD)),
        )

        assertTrue(state is ScheduleState.TodayEmpty)
        assertEquals(LocalDate.of(2025, 9, 15), (state as ScheduleState.TodayEmpty).next?.date)
    }

    @Test
    fun `пара чётной недели не показывается на нечётной`() {
        val state = resolve(
            now = "2025-09-01T09:00",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30", parity = WeekParity.EVEN)),
        )

        assertTrue(state is ScheduleState.TodayEmpty)
        assertEquals(LocalDate.of(2025, 9, 8), (state as ScheduleState.TodayEmpty).next?.date)
    }

    @Test
    fun `пара через полночь остаётся идущей после полуночи`() {
        val state = resolve(
            now = "2025-09-02T00:10",
            lessons = listOf(lesson(DayOfWeek.MONDAY, "23:00", "00:30")),
        )

        assertTrue(state is ScheduleState.InProgress)
        state as ScheduleState.InProgress
        assertEquals(20L, state.remaining.toMinutes())
        assertEquals(LocalDate.of(2025, 9, 1), state.current.date)
    }

    @Test
    fun `граница недели воскресенье-понедельник`() {
        val state = resolve(
            now = "2025-09-07T12:00", // воскресенье нечётной недели
            lessons = listOf(lesson(DayOfWeek.MONDAY, "10:00", "11:30", parity = WeekParity.EVEN)),
        )

        assertTrue(state is ScheduleState.TodayEmpty)
        state as ScheduleState.TodayEmpty
        assertEquals(LocalDate.of(2025, 9, 8), state.next?.date)
    }

    @Test
    fun `пересекающиеся пары разрешаются в пользу начавшейся позже`() {
        val lessons = listOf(
            lesson(DayOfWeek.MONDAY, "10:00", "12:00", title = "Первая", id = 1),
            lesson(DayOfWeek.MONDAY, "11:00", "13:00", title = "Вторая", id = 2),
        )

        val state = resolve(now = "2025-09-01T11:30", lessons = lessons)

        assertTrue(state is ScheduleState.InProgress)
        state as ScheduleState.InProgress
        assertEquals("Вторая", state.current.lesson.title)
        assertEquals(90L, state.remaining.toMinutes())
    }

    @Test
    fun `в идущей паре показывается следующая`() {
        val lessons = listOf(
            lesson(DayOfWeek.MONDAY, "10:00", "11:00", id = 1),
            lesson(DayOfWeek.MONDAY, "12:00", "13:00", id = 2),
        )

        val state = resolve(now = "2025-09-01T10:30", lessons = lessons)

        assertTrue(state is ScheduleState.InProgress)
        assertEquals(
            LocalDateTime.parse("2025-09-01T12:00"),
            (state as ScheduleState.InProgress).following?.start,
        )
    }

    @Test
    fun `пустое расписание распознаётся отдельным состоянием`() {
        val state = resolve(now = "2025-09-01T10:00", lessons = emptyList())
        assertTrue(state is ScheduleState.NoSchedule)
    }
}
