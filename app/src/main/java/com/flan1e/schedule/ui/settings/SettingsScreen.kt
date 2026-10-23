package com.flan1e.schedule.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.flan1e.schedule.R
import com.flan1e.schedule.domain.model.AppLanguage
import com.flan1e.schedule.domain.model.AppSettings
import com.flan1e.schedule.domain.model.AppTheme
import com.flan1e.schedule.util.DateTimeText
import com.flan1e.schedule.util.toUtcMillis
import java.time.LocalDate

/** События экрана настроек: экран не знает о ViewModel и репозиториях. */
sealed interface SettingsEvent {
    data class EnabledChanged(val value: Boolean) : SettingsEvent
    data class LockScreenVisibilityChanged(val value: Boolean) : SettingsEvent
    data class ShowTitleChanged(val value: Boolean) : SettingsEvent
    data class ShowRoomChanged(val value: Boolean) : SettingsEvent
    data class ShowNextLessonChanged(val value: Boolean) : SettingsEvent
    data class CompactModeChanged(val value: Boolean) : SettingsEvent
    data class ThemeChanged(val theme: AppTheme) : SettingsEvent
    data class LanguageChanged(val language: AppLanguage) : SettingsEvent
    data class AnchorDateChanged(val date: LocalDate) : SettingsEvent
    data class AnchorParityChanged(val anchorIsOdd: Boolean) : SettingsEvent
    data class OverrideCurrentWeekParity(val currentWeekIsOdd: Boolean) : SettingsEvent
    data object ResetAnchor : SettingsEvent
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    permissionGranted: Boolean,
    versionName: String,
    onBack: () -> Unit,
    onEvent: (SettingsEvent) -> Unit,
) {
    val context = LocalContext.current
    val locale = DateTimeText.locale(LocalConfiguration.current)
    var showDatePicker by remember { mutableStateOf(false) }
    var showOverrideDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
        val settings = state.settings
        if (!state.loaded || settings == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_section_main))
            SettingsCard {
                SettingSwitchRow(
                    title = stringResource(R.string.settings_enabled),
                    summary = stringResource(R.string.settings_enabled_summary),
                    checked = settings.enabled,
                    onCheckedChange = { onEvent(SettingsEvent.EnabledChanged(it)) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingSwitchRow(
                    title = stringResource(R.string.settings_lock_screen),
                    summary = stringResource(R.string.settings_lock_screen_summary),
                    checked = settings.lockScreenVisible,
                    enabled = settings.enabled,
                    onCheckedChange = { onEvent(SettingsEvent.LockScreenVisibilityChanged(it)) },
                )
            }

            SectionTitle(stringResource(R.string.settings_section_notification))
            SettingsCard {
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_title),
                    checked = settings.showTitle,
                    onCheckedChange = { onEvent(SettingsEvent.ShowTitleChanged(it)) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_room),
                    checked = settings.showRoom,
                    onCheckedChange = { onEvent(SettingsEvent.ShowRoomChanged(it)) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingSwitchRow(
                    title = stringResource(R.string.settings_show_next),
                    checked = settings.showNextLesson,
                    onCheckedChange = { onEvent(SettingsEvent.ShowNextLessonChanged(it)) },
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                SettingSwitchRow(
                    title = stringResource(R.string.settings_compact),
                    summary = stringResource(R.string.settings_compact_summary),
                    checked = settings.compactMode,
                    onCheckedChange = { onEvent(SettingsEvent.CompactModeChanged(it)) },
                )
            }

            SectionTitle(stringResource(R.string.settings_section_appearance))
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        AppTheme.entries.forEachIndexed { index, theme ->
                            SegmentedButton(
                                selected = settings.theme == theme,
                                onClick = { onEvent(SettingsEvent.ThemeChanged(theme)) },
                                shape = SegmentedButtonDefaults.itemShape(index, AppTheme.entries.size),
                            ) {
                                Text(stringResource(themeLabelRes(theme)))
                            }
                        }
                    }
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Column(Modifier.padding(16.dp)) {
                    LanguageDropdown(
                        language = settings.language,
                        onLanguageSelected = { onEvent(SettingsEvent.LanguageChanged(it)) },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_language_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SectionTitle(stringResource(R.string.settings_section_weeks))
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_current_week),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(
                                if (state.currentWeekIsOdd) {
                                    R.string.settings_current_week_odd
                                } else {
                                    R.string.settings_current_week_even
                                },
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    TextButton(onClick = { showOverrideDialog = true }) {
                        Text(stringResource(R.string.settings_override))
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                Surface(
                    onClick = { showDatePicker = true },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_anchor_week),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = stringResource(
                                    R.string.settings_anchor_week_summary,
                                    DateTimeText.longDate(settings.anchorDate, locale),
                                    stringResource(
                                        if (settings.anchorIsOdd) {
                                            R.string.settings_current_week_odd
                                        } else {
                                            R.string.settings_current_week_even
                                        },
                                    ),
                                ),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Icon(
                            imageVector = Icons.Outlined.EditCalendar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))

                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_anchor_parity),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(true, false).forEachIndexed { index, isOdd ->
                            SegmentedButton(
                                selected = settings.anchorIsOdd == isOdd,
                                onClick = { onEvent(SettingsEvent.AnchorParityChanged(isOdd)) },
                                shape = SegmentedButtonDefaults.itemShape(index, 2),
                            ) {
                                Text(
                                    stringResource(
                                        if (isOdd) {
                                            R.string.settings_current_week_odd
                                        } else {
                                            R.string.settings_current_week_even
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                TextButton(
                    onClick = { onEvent(SettingsEvent.ResetAnchor) },
                    modifier = Modifier.padding(horizontal = 8.dp),
                ) {
                    Text(stringResource(R.string.settings_reset_anchor))
                }
            }

            SectionTitle(stringResource(R.string.settings_section_permissions))
            SettingsCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_notifications),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = stringResource(
                                if (permissionGranted) {
                                    R.string.settings_notifications_granted
                                } else {
                                    R.string.settings_notifications_denied
                                },
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (permissionGranted) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                        )
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            context.startActivity(intent)
                        },
                    ) {
                        Text(stringResource(R.string.settings_open_system_settings))
                    }
                }
            }

            SectionTitle(stringResource(R.string.settings_section_about))
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_version, versionName),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_lock_screen_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.settings?.anchorDate?.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onEvent(
                                SettingsEvent.AnchorDateChanged(DateTimeText.utcMillisToDate(millis)),
                            )
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showOverrideDialog) {
        AlertDialog(
            onDismissRequest = { showOverrideDialog = false },
            title = { Text(stringResource(R.string.settings_override_title)) },
            text = { Text(stringResource(R.string.settings_override_message)) },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            onEvent(SettingsEvent.OverrideCurrentWeekParity(true))
                            showOverrideDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.settings_override_odd))
                    }
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            onEvent(SettingsEvent.OverrideCurrentWeekParity(false))
                            showOverrideDialog = false
                        },
                    ) {
                        Text(stringResource(R.string.settings_override_even))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverrideDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(content = content)
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    summary: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
            )
            if (summary != null) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDropdown(
    language: AppLanguage,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = stringResource(languageLabelRes(language)),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.settings_language)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            AppLanguage.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(stringResource(languageLabelRes(item))) },
                    onClick = {
                        expanded = false
                        onLanguageSelected(item)
                    },
                )
            }
        }
    }
}

private fun themeLabelRes(theme: AppTheme): Int = when (theme) {
    AppTheme.LIGHT -> R.string.settings_theme_light
    AppTheme.DARK -> R.string.settings_theme_dark
    AppTheme.SYSTEM -> R.string.settings_theme_system
}

private fun languageLabelRes(language: AppLanguage): Int = when (language) {
    AppLanguage.SYSTEM -> R.string.settings_language_system
    AppLanguage.RUSSIAN -> R.string.settings_language_russian
    AppLanguage.ENGLISH -> R.string.settings_language_english
}
