package com.flan1e.schedule.domain.logic

import java.time.Duration

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
}
