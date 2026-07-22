package com.kratt.finanzas.presentation.widgetsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.data.preferences.WidgetPreferences
import com.kratt.finanzas.presentation.common.containerViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WidgetSettingsViewModel(
    private val widgetPreferences: WidgetPreferences,
    private val refreshWidgets: () -> Unit,
) : ViewModel() {

    val showAmounts: StateFlow<Boolean> = widgetPreferences.showAmounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    // guarda la preferencia y refresca los widgets para que reflejen el cambio de inmediato
    fun onSetShowAmounts(enabled: Boolean) {
        viewModelScope.launch {
            widgetPreferences.setShowAmounts(enabled)
            refreshWidgets()
        }
    }

    fun onUpdateWidgets() = refreshWidgets()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { WidgetSettingsViewModel(it.widgetPreferences, it::refreshWidgets) }
    val showAmounts by viewModel.showAmounts.collectAsStateWithLifecycle()
    var showConfirm by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.WIDGET_SETTINGS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.widgets_title)) },
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
            // activar la visibilidad de montos pide confirmacion; desactivar es directo
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.widget_show_amounts))
                    Text(stringResource(R.string.widget_show_amounts_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = showAmounts,
                    onCheckedChange = { checked -> if (checked) showConfirm = true else viewModel.onSetShowAmounts(false) },
                    modifier = Modifier.testTag(TestTags.WIDGET_SHOW_AMOUNTS_SWITCH),
                )
            }
            Button(onClick = { viewModel.onUpdateWidgets() }, modifier = Modifier.fillMaxWidth().testTag(TestTags.WIDGET_UPDATE_BUTTON)) {
                Text(stringResource(R.string.widget_update))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.widget_show_amounts_confirm_title)) },
            text = { Text(stringResource(R.string.widget_show_amounts_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; viewModel.onSetShowAmounts(true) }) {
                    Text(stringResource(R.string.widget_show_amounts_confirm_action))
                }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
}
