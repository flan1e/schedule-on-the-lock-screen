package com.flan1e.schedule.domain.model

/**
 * Правило повторения пары по неделям.
 */
enum class WeekParity {
    EVERY_WEEK,
    ODD,
    EVEN;

    /**
     * Подходит ли пара для недели с указанной чётностью.
     *
     * @param weekIsOdd true, если неделя нечётная.
     */
    fun matches(weekIsOdd: Boolean): Boolean = when (this) {
        EVERY_WEEK -> true
        ODD -> weekIsOdd
        EVEN -> !weekIsOdd
    }
}
