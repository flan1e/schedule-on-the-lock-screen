package com.flan1e.schedule.util

import com.flan1e.schedule.domain.model.LessonKind

/**
 * Цвета меток типа пары. Заданы фиксированными ARGB, а не цветами темы:
 * метка должна выглядеть одинаково в списке расписания и в уведомлении,
 * в том числе на светлой и тёмной системной теме.
 */
object LessonKindColors {

    private const val LECTURE = 0xFF4C8DFF.toInt()
    private const val PRACTICE = 0xFF2E9E5B.toInt()
    private const val SEMINAR = 0xFF9B59D0.toInt()
    private const val LAB = 0xFFE08A1E.toInt()

    /** null — для «Другого»: в уведомлении метка не показывается. */
    fun argb(kind: LessonKind): Int? = when (kind) {
        LessonKind.LECTURE -> LECTURE
        LessonKind.PRACTICE -> PRACTICE
        LessonKind.SEMINAR -> SEMINAR
        LessonKind.LAB -> LAB
        LessonKind.OTHER -> null
    }
}
