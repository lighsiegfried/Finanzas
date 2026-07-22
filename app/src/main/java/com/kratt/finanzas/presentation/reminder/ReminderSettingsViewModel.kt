package com.kratt.finanzas.presentation.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.reminder.ReminderPreferencesRepository
import com.kratt.finanzas.data.reminder.ReminderSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ReminderSettingsViewModel(
    private val reminderPreferencesRepository: ReminderPreferencesRepository,
) : ViewModel() {

    val uiState: StateFlow<ReminderSettings> = reminderPreferencesRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings.DEFAULT)

    fun onEnabledChange(enabled: Boolean) {
        viewModelScope.launch { reminderPreferencesRepository.setEnabled(enabled) }
    }

    fun onDaysBeforeChange(days: Int) {
        viewModelScope.launch { reminderPreferencesRepository.setDaysBefore(days) }
    }

    // guarda la hora preferida del recordatorio, el reagendado lo hace la pantalla
    fun onTimeChange(hour: Int, minute: Int) {
        viewModelScope.launch { reminderPreferencesRepository.setTime(hour, minute) }
    }
}
