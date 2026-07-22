package com.kratt.finanzas.presentation.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.preferences.DisplayPreferences
import com.kratt.finanzas.data.preferences.DisplaySettings
import com.kratt.finanzas.domain.model.Density
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val displayPreferences: DisplayPreferences,
) : ViewModel() {

    val uiState: StateFlow<DisplaySettings> = displayPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettings())

    fun onThemeMode(mode: ThemeMode) = viewModelScope.launch { displayPreferences.setThemeMode(mode) }
    fun onDynamicColor(enabled: Boolean) = viewModelScope.launch { displayPreferences.setDynamicColor(enabled) }
    fun onDensity(density: Density) = viewModelScope.launch { displayPreferences.setDensity(density) }
    fun onReportViewMode(mode: ReportViewMode) = viewModelScope.launch { displayPreferences.setReportViewMode(mode) }
    fun onHapticsEnabled(enabled: Boolean) = viewModelScope.launch { displayPreferences.setHapticsEnabled(enabled) }
}
