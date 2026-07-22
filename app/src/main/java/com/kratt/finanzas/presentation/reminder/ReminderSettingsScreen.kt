package com.kratt.finanzas.presentation.reminder

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.reminder.ReminderScheduler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { ReminderSettingsViewModel(it.reminderPreferencesRepository) }
    val settings by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // solo se pide el permiso al activar los recordatorios; si lo niegan, las finanzas siguen igual
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun enableReminders() {
        viewModel.onEnabledChange(true)
        // ni el agendado ni el permiso deben tumbar la app; si fallan, las finanzas siguen igual
        runCatching { ReminderScheduler.schedule(context, settings.hour, settings.minute) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            runCatching { permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS) }
        }
    }

    fun disableReminders() {
        viewModel.onEnabledChange(false)
        ReminderScheduler.cancel(context)
    }

    val notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.REMINDER_SETTINGS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.reminders_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.remind_upcoming), modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.enabled,
                    onCheckedChange = { if (it) enableReminders() else disableReminders() },
                    modifier = Modifier.testTag(TestTags.REMINDER_TOGGLE),
                )
            }
            if (settings.enabled && !notificationsAllowed) {
                Text(
                    text = stringResource(R.string.enable_notifications_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!settings.enabled) {
                Text(
                    text = stringResource(R.string.reminders_disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(stringResource(R.string.days_before_label), style = MaterialTheme.typography.titleSmall)
                DaysOption(R.string.days_same, 0, settings.daysBefore, viewModel::onDaysBeforeChange)
                DaysOption(R.string.days_1, 1, settings.daysBefore, viewModel::onDaysBeforeChange)
                DaysOption(R.string.days_3, 3, settings.daysBefore, viewModel::onDaysBeforeChange)
                DaysOption(R.string.days_7, 7, settings.daysBefore, viewModel::onDaysBeforeChange)

                Text(stringResource(R.string.reminder_time_title), style = MaterialTheme.typography.titleSmall)
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatTime(settings.hour, settings.minute), modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.testTag(TestTags.REMINDER_TIME_BUTTON),
                    ) { Text(stringResource(R.string.select_time)) }
                }
                Text(
                    text = stringResource(R.string.reminder_time_supporting),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialHour = settings.hour,
            initialMinute = settings.minute,
            onConfirm = { hour, minute ->
                showTimePicker = false
                viewModel.onTimeChange(hour, minute)
                // reagenda el trabajo unico con la nueva hora; update evita duplicados
                if (settings.enabled) runCatching { ReminderScheduler.schedule(context, hour, minute) }
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

// muestra la hora como HH:mm de forma determinista
private fun formatTime(hour: Int, minute: Int): String = "%02d:%02d".format(hour, minute)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_time_title)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(state.hour, state.minute) },
                modifier = Modifier.testTag(TestTags.SAVE_TIME_BUTTON),
            ) { Text(stringResource(R.string.save_time)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel_action)) } },
    )
}

@Composable
private fun DaysOption(labelRes: Int, value: Int, selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().selectable(selected = selected == value, onClick = { onSelected(value) }).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected == value, onClick = null)
        Text(stringResource(labelRes), modifier = Modifier.padding(start = 12.dp))
    }
}
