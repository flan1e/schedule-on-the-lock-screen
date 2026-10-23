package com.flan1e.schedule.domain.logic

import java.time.Duration
import java.time.LocalDateTime

object CountdownCalculator {

    /**
     * Округляет остаток вниз до целых минут, чтобы текст совпадал с хронометром
     * (32:29 → «32 мин»). Меньше минуты отдаётся как 0 — вызывающий код пишет
     * «меньше минуты».
     */
    fun remainingMinutes(duration: Duration): Long {
        val seconds = duration.seconds.coerceAtLeast(0L)
        return seconds / 60L
    }

    /**
     * Сколько процентов пары уже прошло — для прогресс-бара в уведомлении.
     * Значение всегда в диапазоне 0..100, поэтому бар не «переполняется»
     * при рассинхроне часов.
     */
    fun progressPercent(start: LocalDateTime, end: LocalDateTime, now: LocalDateTime): Int {
        val total = Duration.between(start, end).seconds
        if (total <= 0L) return 0
        val elapsed = Duration.between(start, now).seconds
        return (elapsed * 100L / total).coerceIn(0L, 100L).toInt()
    }
}
