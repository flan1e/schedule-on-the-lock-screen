package com.flan1e.schedule.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.BellPeriod
import com.flan1e.schedule.domain.model.BellSchedule
import com.flan1e.schedule.ui.components.TimeFieldButton
import com.flan1e.schedule.ui.components.TimePickerDialog
import com.flan1e.schedule.util.DateTimeText
import com.flan1e.schedule.util.DurationText
import java.time.Duration
import java.time.LocalTime

/** Настройка сетки звонков: номера пар и их время. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BellScheduleScreen(
    schedule: BellSchedule,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onUpdate: (Int, BellPeriod) -> Unit,
    onRemove: (Int) -> Unit,
    onUseStandard: () -> Unit,
    onClear: () -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var showClearConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bell_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.action_close),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (schedule.isEmpty) {
                item {
                    Text(
                        text = stringResource(R.string.bell_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            itemsIndexed(schedule.periods) { index, period ->
                PeriodCard(
                    number = index + 1,
                    period = period,
                    onClick = { editingIndex = index },
                )
            }

            item {
                OutlinedButton(
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.bell_add))
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onUseStandard) {
                        Text(stringResource(R.string.bell_standard))
                    }
                    TextButton(onClick = { showClearConfirm = true }) {
                        Text(stringResource(R.string.bell_clear))
                    }
                }
            }
        }
    }

    val index = editingIndex
    if (index != null) {
        val period = schedule.periods.getOrNull(index)
        if (period != null) {
            BellPeriodDialog(
                number = index + 1,
                initial = period,
                onDismiss = { editingIndex = null },
                onSave = { updated ->
                    onUpdate(index, updated)
                    editingIndex = null
                },
                onDelete = {
                    onRemove(index)
                    editingIndex = null
                },
            )
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.bell_clear_title)) },
            text = { Text(stringResource(R.string.bell_clear_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClear()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.bell_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun PeriodCard(
    number: Int,
    period: BellPeriod,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val duration = Duration.ofMinutes(period.durationMinutes.toLong())
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bell_period_title, number),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = stringResource(
                        R.string.bell_period_time,
                        DateTimeText.time(minutesToTime(period.startMinute)),
                        DateTimeText.time(minutesToTime(period.endMinute)),
                        DurationText.format(context, duration),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BellPeriodDialog(
    number: Int,
    initial: BellPeriod,
    onDismiss: () -> Unit,
    onSave: (BellPeriod) -> Unit,
    onDelete: () -> Unit,
) {
    var startMinutes by remember { mutableIntStateOf(initial.startMinute) }
    var endMinutes by remember { mutableIntStateOf(initial.endMinute) }
    var picker by remember { mutableStateOf<PickerField?>(null) }
    val valid = endMinutes > startMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.bell_period_title, number)) },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeFieldButton(
                        label = stringResource(R.string.editor_field_start),
                        value = DateTimeText.time(minutesToTime(startMinutes)),
                        modifier = Modifier.weight(1f),
                        onClick = { picker = PickerField.START },
                    )
                    TimeFieldButton(
                        label = stringResource(R.string.editor_field_end),
                        value = DateTimeText.time(minutesToTime(endMinutes)),
                        modifier = Modifier.weight(1f),
                        onClick = { picker = PickerField.END },
                    )
                }
                if (!valid) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.bell_error_time),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onSave(BellPeriod(startMinutes, endMinutes)) },
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )

    when (picker) {
        PickerField.START -> TimePickerDialog(
            title = stringResource(R.string.editor_time_pick_start),
            initial = minutesToTime(startMinutes),
            onDismiss = { picker = null },
            onConfirm = { picked ->
                startMinutes = picked.hour * 60 + picked.minute
                picker = null
            },
        )

        PickerField.END -> TimePickerDialog(
            title = stringResource(R.string.editor_time_pick_end),
            initial = minutesToTime(endMinutes),
            onDismiss = { picker = null },
            onConfirm = { picked ->
                endMinutes = picked.hour * 60 + picked.minute
                picker = null
            },
        )

        null -> Unit
    }
}

private enum class PickerField {
    START,
    END,
}

private fun minutesToTime(minutes: Int): LocalTime =
    LocalTime.of((minutes % (24 * 60)) / 60, (minutes % (24 * 60)) % 60)
