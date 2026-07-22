package com.kratt.finanzas.presentation.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.rememberFinanceHaptics

@StringRes
fun DashboardModule.labelRes(): Int = when (this) {
    DashboardModule.QUICK_ACTIONS -> R.string.module_quick_actions
    DashboardModule.ACCOUNT_BALANCES -> R.string.module_account_balances
    DashboardModule.UPCOMING -> R.string.module_upcoming
    DashboardModule.BUDGET_PROGRESS -> R.string.module_budget_progress
    DashboardModule.RECENT -> R.string.module_recent
    DashboardModule.EXPENSE_CATEGORIES -> R.string.module_expense_categories
    DashboardModule.MONTHLY_TREND -> R.string.module_monthly_trend
    DashboardModule.CREDIT_CARD_DEBT -> R.string.module_credit_card_debt
    DashboardModule.SAVINGS_BALANCE -> R.string.module_savings_balance
    DashboardModule.SAVINGS_GOALS -> R.string.module_savings_goals
    DashboardModule.PLANNED_PURCHASES -> R.string.module_planned_purchases
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardCustomizeRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { DashboardCustomizeViewModel(it.displayPreferences) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showReset by remember { mutableStateOf(false) }
    // vibracion breve opcional al reordenar los modulos del resumen
    val haptics = rememberFinanceHaptics()

    Scaffold(
        modifier = Modifier.testTag(TestTags.DASHBOARD_CUSTOMIZE_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.customize_summary)) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.dashboardOrder.forEachIndexed { index, module ->
                val visible = module !in state.hiddenModules
                val moduleLabel = stringResource(module.labelRes())
                val stateLabel = stringResource(if (visible) R.string.visible_state else R.string.hidden_state)
                Card(modifier = Modifier.fillMaxWidth().testTag("${TestTags.DASHBOARD_MODULE_ROW}_${module.name}")) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(moduleLabel, modifier = Modifier.weight(1f))
                        IconButton(onClick = { haptics.success(); viewModel.onMoveUp(module) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = stringResource(R.string.move_up))
                        }
                        IconButton(onClick = { haptics.success(); viewModel.onMoveDown(module) }, enabled = index < state.dashboardOrder.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = stringResource(R.string.move_down))
                        }
                        // el switch anuncia el estado actual del modulo para lectores de pantalla
                        Switch(
                            checked = visible,
                            onCheckedChange = { viewModel.onToggleVisible(module) },
                            modifier = Modifier
                                .testTag("${TestTags.DASHBOARD_MODULE_VISIBILITY}_${module.name}")
                                .semantics { contentDescription = "$moduleLabel, $stateLabel" },
                        )
                    }
                }
            }
            OutlinedButton(onClick = { showReset = true }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.reset_layout))
            }
        }
    }

    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            title = { Text(stringResource(R.string.reset_summary_title)) },
            text = { Text(stringResource(R.string.reset_summary_message)) },
            confirmButton = {
                TextButton(onClick = { showReset = false; viewModel.onReset() }) { Text(stringResource(R.string.reset_action)) }
            },
            dismissButton = { TextButton(onClick = { showReset = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
}
