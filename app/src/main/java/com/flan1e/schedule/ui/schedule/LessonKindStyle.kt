package com.flan1e.schedule.ui.schedule

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.LessonKind
import com.flan1e.schedule.util.LessonKindColors

/** Цвет метки типа пары. Одинаков в редакторе и в списке расписания. */
@Composable
fun lessonKindColor(kind: LessonKind): Color =
    LessonKindColors.argb(kind)?.let(::Color) ?: MaterialTheme.colorScheme.outline

fun lessonKindLabelRes(kind: LessonKind): Int = when (kind) {
    LessonKind.LECTURE -> R.string.kind_lecture
    LessonKind.PRACTICE -> R.string.kind_practice
    LessonKind.SEMINAR -> R.string.kind_seminar
    LessonKind.LAB -> R.string.kind_lab
    LessonKind.OTHER -> R.string.kind_other
}
