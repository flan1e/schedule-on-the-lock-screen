package com.flan1e.schedule.domain.logic

import java.time.Duration

object CountdownCalculator {

    /**
     * Округляет остаток вверх до целых минут: 45 мин 12 с → 46.
     * Так «0 мин» не показывается, пока пара ещё идёт.
     */
    fun remainingMinutes(duration: Duration): Long {
        val seconds = duration.seconds.coerceAtLeast(0L)
        return (seconds + 59L) / 60L
    }
}
