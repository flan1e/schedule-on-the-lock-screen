package com.flan1e.schedule.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.LessonKind
import com.flan1e.schedule.domain.model.ScheduleState
import com.flan1e.schedule.domain.model.WeekParity
import com.flan1e.schedule.util.DateTimeText
import java.time.DayOfWeek
import java.time.LocalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    permissionGranted: Boolean,
    onOpenSettings: () -> Unit,
    onRequestPermission: () -> Unit,
    onSaveLesson: (Lesson) -> Unit,
    onDeleteLesson: (Long) -> Unit,
) {
    var editorRequest by remember { mutableStateOf<EditorRequest?>(null) }
    val locale = DateTimeText.locale(LocalConfiguration.current)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.schedule_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val today = state.days.firstOrNull { it.dayOfWeek == state.today }
                    editorRequest = EditorRequest(
                        day = state.today,
                        lesson = null,
                        defaultStart = defaultStartFor(today),
                    )
                },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.action_add_lesson)) },
            )
        },
    ) { padding ->
        if (!state.loaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!permissionGranted) {
                item { PermissionCard(onRequestPermission = onRequestPermission) }
            }

            item {
                WeekSummaryCard(state = state)
            }

            items(state.days, key = { it.dayOfWeek.value }) { day ->
                DayCard(
                    day = day,
                    isToday = day.dayOfWeek == state.today,
                    locale = locale,
                    onAdd = {
                        editorRequest = EditorRequest(
                            day = day.dayOfWeek,
                            lesson = null,
                            defaultStart = defaultStartFor(day),
                        )
                    },
                    onEdit = { lesson ->
                        editorRequest = EditorRequest(
                            day = lesson.dayOfWeek,
                            lesson = lesson,
                            defaultStart = lesson.start,
                        )
                    },
                )
            }
        }
    }

    val request = editorRequest
    if (request != null) {
        LessonEditorSheet(
            initial = request.lesson,
            initialDay = request.day,
            defaultStart = request.defaultStart,
            bellSchedule = state.bellSchedule,
            onDismiss = { editorRequest = null },
            onSave = { lesson ->
                onSaveLesson(lesson)
                editorRequest = null
            },
            onDelete = { lesson ->
                onDeleteLesson(lesson.id)
                editorRequest = null
            },
        )
    }
}

@Composable
private fun PermissionCard(onRequestPermission: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.permission_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = stringResource(R.string.permission_text),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            TextButton(onClick = onRequestPermission) {
                Text(stringResource(R.string.permission_grant))
            }
        }
    }
}

@Composable
private fun WeekSummaryCard(state: ScheduleUiState) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(
                        if (state.weekIsOdd) R.string.schedule_week_odd else R.string.schedule_week_even,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            val summary = scheduleSummary(state)
            if (summary != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun scheduleSummary(state: ScheduleUiState): String? = when (val scheduleState = state.scheduleState) {
    is ScheduleState.InProgress -> stringResource(
        R.string.schedule_now_in_progress,
        scheduleState.current.lesson.title,
        DateTimeText.time(scheduleState.current.end.toLocalTime()),
    )

    is ScheduleState.Upcoming -> stringResource(
        R.string.schedule_next_today,
        scheduleState.next.lesson.title,
        DateTimeText.time(scheduleState.next.start.toLocalTime()),
    )

    is ScheduleState.TodayFinished -> stringResource(R.string.schedule_finished_today)
    is ScheduleState.TodayEmpty -> stringResource(R.string.schedule_no_lessons_today)
    ScheduleState.NoSchedule -> stringResource(R.string.schedule_empty)
    null -> null
}

@Composable
private fun DayCard(
    day: DaySchedule,
    isToday: Boolean,
    locale: java.util.Locale,
    onAdd: () -> Unit,
    onEdit: (Lesson) -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = DateTimeText.dayName(day.dayOfWeek, locale),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.weight(1f),
                )
                if (isToday) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_today_badge),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                IconButton(onClick = onAdd) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.schedule_add_to_day),
                    )
                }
            }

            if (day.lessons.isEmpty()) {
                Text(
                    text = stringResource(R.string.schedule_day_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                )
            } else {
                day.lessons.forEachIndexed { index, lesson ->
                    if (index > 0) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    LessonRow(lesson = lesson, onClick = { onEdit(lesson) })
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier
                .padding(top = 2.dp)
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(lessonKindColor(lesson.kind)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.width(64.dp)) {
            Text(
                text = DateTimeText.time(lesson.start),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = if (lesson.crossesMidnight) {
                    stringResource(R.string.schedule_time_next_day, DateTimeText.time(lesson.end))
                } else {
                    DateTimeText.time(lesson.end)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = lesson.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (lesson.kind != LessonKind.OTHER) {
                    Text(
                        text = stringResource(lessonKindLabelRes(lesson.kind)),
                        style = MaterialTheme.typography.bodySmall,
                        color = lessonKindColor(lesson.kind),
                    )
                }
                if (lesson.room.isNotBlank()) {
                    if (lesson.kind != LessonKind.OTHER) {
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        text = stringResource(R.string.schedule_room_prefix, lesson.room),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (lesson.parity != WeekParity.EVERY_WEEK) {
                    Spacer(Modifier.width(8.dp))
                    ParityBadge(parity = lesson.parity)
                }
            }
        }
    }
}

@Composable
private fun ParityBadge(parity: WeekParity) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = stringResource(
                if (parity == WeekParity.ODD) {
                    R.string.schedule_parity_odd_short
                } else {
                    R.string.schedule_parity_even_short
                },
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

private fun defaultStartFor(day: DaySchedule?): LocalTime {
    val last = day?.lessons?.lastOrNull() ?: return LocalTime.of(9, 0)
    return if (last.crossesMidnight) LocalTime.of(9, 0) else last.end
}

private data class EditorRequest(
    val day: DayOfWeek,
    val lesson: Lesson?,
    val defaultStart: LocalTime,
)
