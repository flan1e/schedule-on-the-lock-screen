package com.flan1e.schedule.data.backup

import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.LessonKind
import com.flan1e.schedule.domain.model.WeekParity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

/** Файл не похож на резервную копию расписания. */
class BackupFormatException(message: String) : Exception(message)

/**
 * Содержимое резервной копии: пары и опорная неделя. Без опорной недели
 * нельзя правильно восстановить пары, которые повторяются через неделю.
 */
data class ScheduleBackup(
    val anchorDate: LocalDate,
    val anchorIsOdd: Boolean,
    val lessons: List<Lesson>,
)

/**
 * JSON-кодек резервной копии.
 *
 * Специально без org.json и kotlinx.serialization: во-первых, не хочется
 * тянуть зависимость ради одного файла, во-вторых, кодек — чистый Kotlin,
 * поэтому его легко проверить обычными unit-тестами.
 */
object BackupCodec {

    private const val FORMAT = "schedule-backup"
    private const val VERSION = 1

    fun encode(
        backup: ScheduleBackup,
        exportedAt: LocalDateTime = LocalDateTime.now(),
    ): String = buildString {
        appendLine("{")
        append("  \"format\": ").appendJsonString(FORMAT).appendLine(",")
        append("  \"version\": ").append(VERSION).appendLine(",")
        append("  \"exportedAt\": ").appendJsonString(exportedAt.toString()).appendLine(",")
        append("  \"anchorDate\": ").appendJsonString(backup.anchorDate.toString()).appendLine(",")
        append("  \"anchorIsOdd\": ").append(backup.anchorIsOdd).appendLine(",")
        appendLine("  \"lessons\": [")
        backup.lessons.forEachIndexed { index, lesson ->
            append("    {")
            append("\"dayOfWeek\": ").append(lesson.dayOfWeek.value).append(", ")
            append("\"start\": ").appendJsonString(timeToString(lesson.start)).append(", ")
            append("\"end\": ").appendJsonString(timeToString(lesson.end)).append(", ")
            append("\"title\": ").appendJsonString(lesson.title).append(", ")
            append("\"room\": ").appendJsonString(lesson.room).append(", ")
            append("\"parity\": ").appendJsonString(lesson.parity.name).append(", ")
            append("\"kind\": ").appendJsonString(lesson.kind.name)
            append('}')
            if (index != backup.lessons.lastIndex) append(',')
            appendLine()
        }
        appendLine("  ]")
        appendLine("}")
    }

    @Throws(BackupFormatException::class)
    fun decode(text: String): ScheduleBackup {
        val root = try {
            JsonReader(text).parse()
        } catch (error: BackupFormatException) {
            throw error
        } catch (error: Exception) {
            throw BackupFormatException("Не удалось разобрать JSON: ${error.message}")
        }
        val obj = root as? Map<*, *>
            ?: throw BackupFormatException("Корень файла должен быть объектом")

        val format = obj["format"] as? String
        if (format != null && format != FORMAT) {
            throw BackupFormatException("Неизвестный формат файла: $format")
        }
        val version = (obj["version"] as? Double)?.toInt() ?: VERSION
        if (version > VERSION) {
            throw BackupFormatException("Версия файла новее, чем поддерживает приложение")
        }

        val anchorDate = (obj["anchorDate"] as? String)?.let(::parseDate)
            ?: throw BackupFormatException("В файле нет опорной недели")
        val anchorIsOdd = obj["anchorIsOdd"] as? Boolean
            ?: throw BackupFormatException("В файле нет чётности опорной недели")

        val rawLessons = obj["lessons"] as? List<*>
            ?: throw BackupFormatException("В файле нет списка пар")
        val lessons = rawLessons.mapIndexed { index, item -> parseLesson(item, index) }

        return ScheduleBackup(
            anchorDate = anchorDate,
            anchorIsOdd = anchorIsOdd,
            lessons = lessons,
        )
    }

    private fun parseLesson(item: Any?, index: Int): Lesson {
        val obj = item as? Map<*, *>
            ?: throw BackupFormatException("Пара №${index + 1}: ожидался объект")

        val day = when (val raw = obj["dayOfWeek"]) {
            is Double -> raw.toInt()
            is String -> DAY_NAMES[raw.uppercase()]
            else -> null
        } ?: throw BackupFormatException("Пара №${index + 1}: неверный день недели")
        if (day !in 1..7) {
            throw BackupFormatException("Пара №${index + 1}: день недели вне диапазона")
        }

        val start = parseTime(obj["start"], index, "начало")
        val end = parseTime(obj["end"], index, "окончание")

        val title = (obj["title"] as? String)?.trim().orEmpty()
        if (title.isEmpty()) {
            throw BackupFormatException("Пара №${index + 1}: пустое название")
        }
        val room = (obj["room"] as? String)?.trim().orEmpty()
        val parity = (obj["parity"] as? String)
            ?.let { name -> WeekParity.entries.firstOrNull { it.name == name } }
            ?: WeekParity.EVERY_WEEK
        val kind = (obj["kind"] as? String)
            ?.let(LessonKind::fromName)
            ?: LessonKind.OTHER

        return Lesson(
            dayOfWeek = java.time.DayOfWeek.of(day),
            start = start,
            end = end,
            title = title,
            room = room,
            parity = parity,
            kind = kind,
        )
    }

    private fun parseTime(raw: Any?, index: Int, field: String): LocalTime {
        val value = raw as? String
            ?: throw BackupFormatException("Пара №${index + 1}: нет поля «$field»")
        return try {
            LocalTime.parse(value)
        } catch (error: DateTimeParseException) {
            throw BackupFormatException("Пара №${index + 1}: неверное время «$value»")
        }
    }

    private fun parseDate(raw: String): LocalDate = try {
        LocalDate.parse(raw)
    } catch (error: DateTimeParseException) {
        throw BackupFormatException("Неверная дата опорной недели: $raw")
    }

    private fun timeToString(time: LocalTime): String =
        String.format(java.util.Locale.ROOT, "%02d:%02d", time.hour, time.minute)

    private val DAY_NAMES = mapOf(
        "MONDAY" to 1,
        "TUESDAY" to 2,
        "WEDNESDAY" to 3,
        "THURSDAY" to 4,
        "FRIDAY" to 5,
        "SATURDAY" to 6,
        "SUNDAY" to 7,
    )
}

private fun StringBuilder.appendJsonString(value: String): StringBuilder {
    append('"')
    value.forEach { char ->
        when (char) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '\b' -> append("\\b")
            '\u000C' -> append("\\f")
            else -> if (char < ' ') {
                append(String.format(java.util.Locale.ROOT, "\\u%04x", char.code))
            } else {
                append(char)
            }
        }
    }
    append('"')
    return this
}

/** Минимальный JSON-парсер: объекты, массивы, строки, числа, true/false/null. */
private class JsonReader(private val text: String) {

    private var position = 0

    fun parse(): Any? {
        val value = parseValue()
        skipWhitespace()
        if (position != text.length) {
            throw BackupFormatException("Лишние данные после JSON, позиция $position")
        }
        return value
    }

    private fun parseValue(): Any? {
        skipWhitespace()
        if (position >= text.length) {
            throw BackupFormatException("Неожиданный конец файла")
        }
        return when (val char = text[position]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> parseString()
            't' -> {
                expectWord("true")
                true
            }

            'f' -> {
                expectWord("false")
                false
            }

            'n' -> {
                expectWord("null")
                null
            }

            else -> if (char == '-' || char.isDigit()) parseNumber() else fail("Неожиданный символ '$char'")
        }
    }

    private fun parseObject(): Map<String, Any?> {
        expect('{')
        val result = LinkedHashMap<String, Any?>()
        skipWhitespace()
        if (peek() == '}') {
            position++
            return result
        }
        while (true) {
            skipWhitespace()
            val key = parseString()
            skipWhitespace()
            expect(':')
            result[key] = parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> position++
                '}' -> {
                    position++
                    return result
                }

                else -> fail("Ожидалась ',' или '}'")
            }
        }
    }

    private fun parseArray(): List<Any?> {
        expect('[')
        val result = ArrayList<Any?>()
        skipWhitespace()
        if (peek() == ']') {
            position++
            return result
        }
        while (true) {
            result += parseValue()
            skipWhitespace()
            when (peek()) {
                ',' -> position++
                ']' -> {
                    position++
                    return result
                }

                else -> fail("Ожидалась ',' или ']'")
            }
        }
    }

    private fun parseString(): String {
        expect('"')
        val builder = StringBuilder()
        while (true) {
            if (position >= text.length) fail("Незакрытая строка")
            when (val char = text[position++]) {
                '"' -> return builder.toString()
                '\\' -> {
                    if (position >= text.length) fail("Незакрытая строка")
                    when (val escaped = text[position++]) {
                        '"' -> builder.append('"')
                        '\\' -> builder.append('\\')
                        '/' -> builder.append('/')
                        'b' -> builder.append('\b')
                        'f' -> builder.append('\u000C')
                        'n' -> builder.append('\n')
                        'r' -> builder.append('\r')
                        't' -> builder.append('\t')
                        'u' -> {
                            if (position + 4 > text.length) fail("Обрыв \\u-последовательности")
                            val code = text.substring(position, position + 4).toIntOrNull(16)
                                ?: fail("Неверная \\u-последовательность")
                            builder.append(code.toChar())
                            position += 4
                        }

                        else -> fail("Неизвестная escape-последовательность \\$escaped")
                    }
                }

                else -> builder.append(char)
            }
        }
    }

    private fun parseNumber(): Double {
        val start = position
        while (position < text.length && text[position] in "+-.eE0123456789") {
            position++
        }
        return text.substring(start, position).toDoubleOrNull()
            ?: fail("Неверное число «${text.substring(start, position)}»")
    }

    private fun expect(char: Char) {
        skipWhitespace()
        if (peek() != char) fail("Ожидался символ '$char'")
        position++
    }

    private fun expectWord(word: String) {
        if (!text.startsWith(word, position)) fail("Ожидалось «$word»")
        position += word.length
    }

    private fun peek(): Char {
        if (position >= text.length) fail("Неожиданный конец файла")
        return text[position]
    }

    private fun skipWhitespace() {
        while (position < text.length && text[position].isWhitespace()) position++
    }

    private fun fail(message: String): Nothing =
        throw BackupFormatException("$message (позиция $position)")
}
