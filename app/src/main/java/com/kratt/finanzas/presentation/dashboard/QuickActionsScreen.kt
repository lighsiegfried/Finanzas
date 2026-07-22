package com.kratt.finanzas.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.data.preferences.DisplayPreferences
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.usecase.QuickActionSelection
import com.kratt.finanzas.presentation.common.containerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QuickActionsViewModel(
    private val displayPreferences: DisplayPreferences,
) : ViewModel() {

    val selected: StateFlow<List<QuickAction>> = displayPreferences.settings
        .map { it.quickActions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QuickAction.DEFAULTS)

    // agrega o quita una accion; no pasa del maximo permitido
    fun onToggle(action: QuickAction) {
        val next = QuickActionSelection.toggle(selected.value, action)
        viewModelScope.launch { displayPreferences.setQuickActions(next) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { QuickActionsViewModel(it.displayPreferences) }
    val selected by viewModel.selected.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.testTag(TestTags.QUICK_ACTIONS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.customize_quick_actions)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.quick_actions_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val atLimit = selected.size >= QuickAction.MAX_SELECTED
            QuickAction.entries.forEach { action ->
                val isSelected = action in selected
                // cuando ya hay cinco solo se pueden quitar, no agregar mas
                val enabled = isSelected || !atLimit
                val label = stringResource(action.labelRes())
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = isSelected, enabled = enabled, onValueChange = { viewModel.onToggle(action) })
                        .padding(vertical = 8.dp)
                        .testTag("${TestTags.QUICK_ACTION_OPTION}_${action.name}")
                        .semantics { contentDescription = label },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Checkbox(checked = isSelected, onCheckedChange = null, enabled = enabled)
                    Icon(action.icon(), contentDescription = null)
                    Text(label, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
