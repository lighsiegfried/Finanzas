package com.kratt.finanzas.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.preferences.DisplayPreferences
import com.kratt.finanzas.data.preferences.DisplaySettings
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.usecase.DashboardOrdering
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardCustomizeViewModel(
    private val displayPreferences: DisplayPreferences,
) : ViewModel() {

    val uiState: StateFlow<DisplaySettings> = displayPreferences.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettings())

    // sube un modulo una posicion en el orden del resumen
    fun onMoveUp(module: DashboardModule) {
        val order = DashboardOrdering.moveUp(uiState.value.dashboardOrder, module)
        viewModelScope.launch { displayPreferences.setDashboardOrder(order) }
    }

    fun onMoveDown(module: DashboardModule) {
        val order = DashboardOrdering.moveDown(uiState.value.dashboardOrder, module)
        viewModelScope.launch { displayPreferences.setDashboardOrder(order) }
    }

    // muestra u oculta un modulo opcional del resumen
    fun onToggleVisible(module: DashboardModule) {
        val hidden = DashboardOrdering.toggleHidden(uiState.value.hiddenModules, module)
        viewModelScope.launch { displayPreferences.setHiddenModules(hidden) }
    }

    fun onReset() {
        viewModelScope.launch { displayPreferences.resetDashboard() }
    }
}
