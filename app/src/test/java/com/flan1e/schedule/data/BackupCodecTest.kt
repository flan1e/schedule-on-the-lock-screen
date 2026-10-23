package com.flan1e.schedule.data

import com.flan1e.schedule.data.backup.BackupCodec
import com.flan1e.schedule.data.backup.BackupFormatException
import com.flan1e.schedule.data.backup.ScheduleBackup
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.WeekParity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class BackupCodecTest {

    private val backup = ScheduleBackup(
        anchorDate = LocalDate.of(2026, 9, 1),
        anchorIsOdd = true,
        lessons = listOf(
            Lesson(
                dayOfWeek = DayOfWeek.MONDAY,
                start = LocalTime.of(8, 30),
                end = LocalTime.of(10, 0),
                title = "Матанализ",
                room = "312",
            ),
            Lesson(
                dayOfWeek = DayOfWeek.SUNDAY,
                start = LocalTime.of(23, 0),
                end = LocalTime.of(0, 30),
                title = "Ночная \"смена\" \\ зачёт",
                room = "",
                parity = WeekParity.ODD,
            ),
        ),
    )

    @Test
    fun `round trip keeps lessons and anchor`() {
        val json = BackupCodec.encode(backup, LocalDateTime.of(2026, 10, 24, 1, 30))
        val decoded = BackupCodec.decode(json)

        assertEquals(backup.anchorDate, decoded.anchorDate)
        assertEquals(backup.anchorIsOdd, decoded.anchorIsOdd)
        assertEquals(backup.lessons.size, decoded.lessons.size)
        assertEquals(backup.lessons[0].title, decoded.lessons[0].title)
        assertEquals(backup.lessons[0].start, decoded.lessons[0].start)
        assertEquals(backup.lessons[0].end, decoded.lessons[0].end)
        assertEquals(backup.lessons[0].room, decoded.lessons[0].room)
        assertEquals(backup.lessons[1].parity, decoded.lessons[1].parity)
        assertEquals(backup.lessons[1].title, decoded.lessons[1].title)
        assertEquals(backup.lessons[1].start, decoded.lessons[1].start)
        assertEquals(backup.lessons[1].end, decoded.lessons[1].end)
    }

    @Test
    fun `exported json is readable and marked as schedule backup`() {
        val json = BackupCodec.encode(backup)

        assertTrue(json.contains("\"format\": \"schedule-backup\""))
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"anchorDate\": \"2026-09-01\""))
        assertTrue(json.contains("\"dayOfWeek\": 1"))
        assertTrue(json.contains("\"start\": \"08:30\""))
    }

    @Test
    fun `empty schedule is allowed`() {
        val decoded = BackupCodec.decode(
            BackupCodec.encode(backup.copy(lessons = emptyList())),
        )

        assertTrue(decoded.lessons.isEmpty())
    }

    @Test
    fun `broken json is rejected`() {
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode("{oops")
        }
    }

    @Test
    fun `file without lessons is rejected`() {
        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(
                """{"anchorDate":"2026-09-01","anchorIsOdd":true}""",
            )
        }
    }

    @Test
    fun `file with unknown day is rejected`() {
        val json = """
            {
              "anchorDate": "2026-09-01",
              "anchorIsOdd": true,
              "lessons": [
                {"dayOfWeek": 9, "start": "08:30", "end": "10:00", "title": "Пара"}
              ]
            }
        """.trimIndent()

        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(json)
        }
    }

    @Test
    fun `file with invalid time is rejected`() {
        val json = """
            {
              "anchorDate": "2026-09-01",
              "anchorIsOdd": true,
              "lessons": [
                {"dayOfWeek": 1, "start": "25:00", "end": "10:00", "title": "Пара"}
              ]
            }
        """.trimIndent()

        assertThrows(BackupFormatException::class.java) {
            BackupCodec.decode(json)
        }
    }

    @Test
    fun `unknown parity falls back to every week`() {
        val json = """
            {
              "anchorDate": "2026-09-01",
              "anchorIsOdd": true,
              "lessons": [
                {
                  "dayOfWeek": 3,
                  "start": "12:10",
                  "end": "13:40",
                  "title": "Физика",
                  "room": "105",
                  "parity": "SOMETIMES"
                }
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decode(json)

        assertEquals(WeekParity.EVERY_WEEK, decoded.lessons.single().parity)
    }

    @Test
    fun `control characters are escaped`() {
        val lesson = Lesson(
            dayOfWeek = DayOfWeek.FRIDAY,
            start = LocalTime.of(9, 0),
            end = LocalTime.of(10, 30),
            title = "Строка\nс переводом\tи табом",
            room = "1",
        )

        val decoded = BackupCodec.decode(
            BackupCodec.encode(backup.copy(lessons = listOf(lesson))),
        )

        assertEquals(lesson.title, decoded.lessons.single().title)
    }
}
