package com.kratt.finanzas.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.preferences.DisplayPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// expone solo la privacidad de saldos para el interruptor rapido en ajustes
class SettingsViewModel(
    private val displayPreferences: DisplayPreferences,
) : ViewModel() {

    val balancesHidden: StateFlow<Boolean> = displayPreferences.settings
        .map { it.balancesHidden }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onToggleHideBalances() {
        viewModelScope.launch { displayPreferences.setBalancesHidden(!balancesHidden.value) }
    }
}
