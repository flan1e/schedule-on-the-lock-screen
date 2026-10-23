package com.flan1e.schedule.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.Lesson
import com.flan1e.schedule.domain.model.BellSchedule
import com.flan1e.schedule.domain.model.LessonKind
import com.flan1e.schedule.domain.model.WeekParity
import com.flan1e.schedule.ui.components.TimeFieldButton
import com.flan1e.schedule.ui.components.TimePickerDialog
import com.flan1e.schedule.util.DateTimeText
import com.flan1e.schedule.util.DurationText
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime

private const val MINUTES_PER_DAY = 24 * 60
private const val DEFAULT_DURATION_MINUTES = 90

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonEditorSheet(
    initial: Lesson?,
    initialDay: DayOfWeek,
    defaultStart: LocalTime,
    bellSchedule: BellSchedule,
    onDismiss: () -> Unit,
    onSave: (Lesson) -> Unit,
    onDelete: (Lesson) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val locale = DateTimeText.locale(LocalConfiguration.current)

    val initialStartMinutes = initial?.let { it.start.hour * 60 + it.start.minute }
        ?: (defaultStart.hour * 60 + defaultStart.minute)
    val initialEndMinutes = initial?.let { it.end.hour * 60 + it.end.minute }
        ?: normalizeMinutes(initialStartMinutes + DEFAULT_DURATION_MINUTES)

    var title by rememberSaveable { mutableStateOf(initial?.title.orEmpty()) }
    var room by rememberSaveable { mutableStateOf(initial?.room.orEmpty()) }
    var day by rememberSaveable { mutableStateOf(initial?.dayOfWeek ?: initialDay) }
    var startMinutes by rememberSaveable { mutableIntStateOf(initialStartMinutes) }
    var endMinutes by rememberSaveable { mutableIntStateOf(initialEndMinutes) }
    var endEdited by rememberSaveable { mutableStateOf(initial != null) }
    var parity by rememberSaveable { mutableStateOf(initial?.parity ?: WeekParity.EVERY_WEEK) }
    var kind by rememberSaveable { mutableStateOf(initial?.kind ?: LessonKind.OTHER) }
    var titleError by rememberSaveable { mutableStateOf(false) }
    var timeError by rememberSaveable { mutableStateOf(false) }
    var timePicker by remember { mutableStateOf<TimeField?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val closeSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            onDismiss()
        }
    }

    fun save() {
        when {
            title.isBlank() -> titleError = true
            startMinutes == endMinutes -> timeError = true
            else -> {
                onSave(
                    Lesson(
                        id = initial?.id ?: 0L,
                        dayOfWeek = day,
                        start = minutesToTime(startMinutes),
                        end = minutesToTime(endMinutes),
                        title = title.trim(),
                        room = room.trim(),
                        parity = parity,
                        kind = kind,
                    ),
                )
                closeSheet()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(
                    if (initial == null) R.string.editor_new_title else R.string.editor_edit_title,
                ),
                style = MaterialTheme.typography.headlineSmall,
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    titleError = false
                },
                label = { Text(stringResource(R.string.editor_field_title)) },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text(stringResource(R.string.editor_error_title_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text(stringResource(R.string.editor_field_room)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(
                    text = stringResource(R.string.editor_field_day),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DayOfWeek.values().toList()) { value ->
                        FilterChip(
                            selected = value == day,
                            onClick = { day = value },
                            label = {
                                Text(DateTimeText.dayName(value, locale, java.time.format.TextStyle.SHORT))
                            },
                        )
                    }
                }
            }

            if (!bellSchedule.isEmpty) {
                Column {
                    Text(
                        text = stringResource(R.string.editor_field_bell),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(bellSchedule.periods) { index, period ->
                            val selected = bellSchedule.indexOf(
                                minutesToTime(startMinutes),
                                minutesToTime(endMinutes),
                            ) == index
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    startMinutes = period.startMinute
                                    endMinutes = period.endMinute
                                    endEdited = true
                                    timeError = false
                                },
                                label = {
                                    Text(
                                        stringResource(
                                            R.string.editor_bell_period,
                                            index + 1,
                                            DateTimeText.time(minutesToTime(period.startMinute)),
                                            DateTimeText.time(minutesToTime(period.endMinute)),
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeFieldButton(
                    label = stringResource(R.string.editor_field_start),
                    value = DateTimeText.time(minutesToTime(startMinutes)),
                    modifier = Modifier.weight(1f),
                    onClick = { timePicker = TimeField.START },
                )
                TimeFieldButton(
                    label = stringResource(R.string.editor_field_end),
                    value = DateTimeText.time(minutesToTime(endMinutes)),
                    modifier = Modifier.weight(1f),
                    onClick = { timePicker = TimeField.END },
                )
            }

            val duration = Duration.ofMinutes(
                durationMinutes(startMinutes, endMinutes).toLong(),
            )
            Text(
                text = stringResource(R.string.editor_duration, DurationText.format(context, duration)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (timeError) {
                Text(
                    text = stringResource(R.string.editor_error_time_equal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column {
                Text(
                    text = stringResource(R.string.editor_field_parity),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    val options = listOf(
                        WeekParity.EVERY_WEEK to R.string.schedule_parity_every_short,
                        WeekParity.ODD to R.string.schedule_parity_odd_short,
                        WeekParity.EVEN to R.string.schedule_parity_even_short,
                    )
                    options.forEachIndexed { index, (value, labelRes) ->
                        SegmentedButton(
                            selected = parity == value,
                            onClick = { parity = value },
                            shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            }

            Column {
                Text(
                    text = stringResource(R.string.editor_field_kind),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(LessonKind.entries.toList()) { value ->
                        FilterChip(
                            selected = value == kind,
                            onClick = { kind = value },
                            label = { Text(stringResource(lessonKindLabelRes(value))) },
                            leadingIcon = {
                                Box(
                                    Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(lessonKindColor(value)),
                                )
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_save))
            }

            if (initial != null) {
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
    }

    when (timePicker) {
        TimeField.START -> TimePickerDialog(
            title = stringResource(R.string.editor_time_pick_start),
            initial = minutesToTime(startMinutes),
            onDismiss = { timePicker = null },
            onConfirm = { picked ->
                val pickedMinutes = picked.hour * 60 + picked.minute
                if (!endEdited) {
                    startMinutes = pickedMinutes
                    endMinutes = normalizeMinutes(pickedMinutes + DEFAULT_DURATION_MINUTES)
                } else {
                    val delta = pickedMinutes - startMinutes
                    startMinutes = pickedMinutes
                    endMinutes = normalizeMinutes(endMinutes + delta)
                }
                timeError = false
                timePicker = null
            },
        )

        TimeField.END -> TimePickerDialog(
            title = stringResource(R.string.editor_time_pick_end),
            initial = minutesToTime(endMinutes),
            onDismiss = { timePicker = null },
            onConfirm = { picked ->
                endMinutes = picked.hour * 60 + picked.minute
                endEdited = true
                timeError = false
                timePicker = null
            },
        )

        null -> Unit
    }

    if (showDeleteConfirm && initial != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.editor_delete_title)) },
            text = {
                Text(stringResource(R.string.editor_delete_message, initial.title))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(initial)
                        closeSheet()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}


private enum class TimeField {
    START,
    END,
}

private fun minutesToTime(minutes: Int): LocalTime =
    LocalTime.of(normalizeMinutes(minutes) / 60, normalizeMinutes(minutes) % 60)

private fun normalizeMinutes(minutes: Int): Int =
    ((minutes % MINUTES_PER_DAY) + MINUTES_PER_DAY) % MINUTES_PER_DAY

private fun durationMinutes(startMinutes: Int, endMinutes: Int): Int {
    val diff = endMinutes - startMinutes
    return if (diff > 0) diff else diff + MINUTES_PER_DAY
}
