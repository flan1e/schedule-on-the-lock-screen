package com.flan1e.schedule.ui.schedule

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.LessonKind

/** Цвет метки типа пары. Одинаков в редакторе и в списке расписания. */
@Composable
fun lessonKindColor(kind: LessonKind): Color = when (kind) {
    LessonKind.LECTURE -> MaterialTheme.colorScheme.primary
    LessonKind.PRACTICE -> Color(0xFF2E9E5B)
    LessonKind.LAB -> Color(0xFFE08A1E)
    LessonKind.OTHER -> MaterialTheme.colorScheme.outline
}

fun lessonKindLabelRes(kind: LessonKind): Int = when (kind) {
    LessonKind.LECTURE -> R.string.kind_lecture
    LessonKind.PRACTICE -> R.string.kind_practice
    LessonKind.LAB -> R.string.kind_lab
    LessonKind.OTHER -> R.string.kind_other
}
