package com.flan1e.schedule.domain.model

/**
 * Тип пары. На логику выбора текущей/следующей пары не влияет:
 * используется только для цветовой метки и подписи в расписании.
 */
enum class LessonKind {
    LECTURE,
    PRACTICE,
    SEMINAR,
    LAB,
    OTHER,
    ;

    companion object {
        fun fromName(raw: String?): LessonKind =
            entries.firstOrNull { it.name == raw } ?: OTHER
    }
}
