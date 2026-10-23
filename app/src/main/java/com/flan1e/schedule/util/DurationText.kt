package com.flan1e.schedule.util

import android.content.Context
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.logic.CountdownCalculator
import java.time.Duration

/** Человекочитаемая длительность: «45 мин», «1 ч 20 мин», «2 д 3 ч». */
object DurationText {

    fun format(context: Context, duration: Duration): String {
        val minutes = CountdownCalculator.remainingMinutes(duration)
        if (minutes <= 0L) return context.getString(R.string.duration_less_than_minute)
        if (minutes < 60L) return context.getString(R.string.duration_minutes, minutes.toInt())

        val hours = minutes / 60L
        val restMinutes = (minutes % 60L).toInt()
        if (hours < 24L) {
            return context.getString(R.string.duration_hours_minutes, hours.toInt(), restMinutes)
        }

        val days = hours / 24L
        val restHours = (hours % 24L).toInt()
        return context.getString(R.string.duration_days_hours, days.toInt(), restHours)
    }
}
