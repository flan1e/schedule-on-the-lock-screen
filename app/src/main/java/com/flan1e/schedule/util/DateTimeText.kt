package com.flan1e.schedule.util

import android.content.Context
import android.content.res.Configuration
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.Lesson
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Единые правила форматирования времени и дат для UI и уведомления. */
object DateTimeText {

    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

    fun time(time: LocalTime): String = time.format(TIME_FORMATTER)

    fun timeRange(lesson: Lesson, context: Context): String {
        val start = time(lesson.start)
        val end = time(lesson.end)
        return if (lesson.crossesMidnight) {
            context.getString(R.string.time_range_next_day, start, end)
        } else {
            context.getString(R.string.time_range, start, end)
        }
    }

    fun locale(context: Context): Locale = context.resources.configuration.locales[0]

    fun locale(configuration: Configuration): Locale = configuration.locales[0]

    /** «сегодня» / «завтра» / «послезавтра» / «4 нояб.» */
    fun relativeDateLabel(date: LocalDate, today: LocalDate, context: Context): String {
        val days = ChronoUnit.DAYS.between(today, date)
        return when (days) {
            0L -> context.getString(R.string.relative_today)
            1L -> context.getString(R.string.relative_tomorrow)
            2L -> context.getString(R.string.relative_day_after_tomorrow)
            else -> date.format(SHORT_DATE_FORMATTER.withLocale(locale(context)))
        }
    }

    fun longDate(date: LocalDate, locale: Locale): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale))

    fun dayName(dayOfWeek: DayOfWeek, locale: Locale, style: TextStyle = TextStyle.FULL): String {
        val name = dayOfWeek.getDisplayName(style, locale)
        return name.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase(locale) else first.toString()
        }
    }

    fun utcMillisToDate(millis: Long): LocalDate =
        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
}

/** M3 DatePicker работает с UTC-полуночью. */
fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
