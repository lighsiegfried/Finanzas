package com.kratt.finanzas.presentation.summary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.domain.usecase.DueWhen
import com.kratt.finanzas.presentation.charts.ChartPoint
import com.kratt.finanzas.presentation.charts.InteractiveChartCard
import com.kratt.finanzas.presentation.common.EmptyState
import com.kratt.finanzas.presentation.common.LoadingState
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskAmount
import com.kratt.finanzas.presentation.common.maskedAmount
import com.kratt.finanzas.presentation.common.rememberFinanceHaptics
import com.kratt.finanzas.presentation.components.TransactionListRow
import com.kratt.finanzas.presentation.dashboard.QuickActionsBar
import com.kratt.finanzas.presentation.theme.LocalFinanceColors
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme

@Composable
fun SummaryRoute(
    onAddTransactionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onMovementClick: (Long) -> Unit,
    onQuickAction: (QuickAction) -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onOpenPurchases: () -> Unit = {},
) {
    val viewModel = containerViewModel {
        SummaryViewModel(
            it.observeMonthlySummary, it.transactionRepository, it.accountRepository,
            it.reportRepository, it.budgetRepository, it.savingsGoalRepository, it.plannedPurchaseRepository,
            it.commitmentService, it.displayPreferences,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val balancesHidden by viewModel.balancesHidden.collectAsStateWithLifecycle()
    val modules by viewModel.dashboardModules.collectAsStateWithLifecycle()
    SummaryScreen(
        state = state,
        modules = modules,
        balancesHidden = balancesHidden,
        onToggleBalances = viewModel::onToggleBalances,
        onAddTransactionClick = onAddTransactionClick,
        onSettingsClick = onSettingsClick,
        onMovementClick = onMovementClick,
        onQuickAction = onQuickAction,
        onOpenGoals = onOpenGoals,
        onOpenPurchases = onOpenPurchases,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCurrentMonth = viewModel::onCurrentMonth,
    )
}

// pantalla de resumen con los totales del mes y los modulos elegidos por el usuario
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(
    state: SummaryUiState,
    onAddTransactionClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modules: List<DashboardModule> = DashboardModule.DEFAULT_ORDER.filter { it !in DashboardModule.DEFAULT_HIDDEN },
    balancesHidden: Boolean = false,
    onToggleBalances: () -> Unit = {},
    onMovementClick: (Long) -> Unit = {},
    onQuickAction: (QuickAction) -> Unit = {},
    onOpenGoals: () -> Unit = {},
    onOpenPurchases: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onCurrentMonth: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // vibracion breve opcional al cambiar la privacidad de saldos
    val haptics = rememberFinanceHaptics()
    Scaffold(
        modifier = modifier.testTag(TestTags.SUMMARY_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.summary_title), modifier = Modifier.testTag(TestTags.SUMMARY_TITLE)) },
                actions = {
                    // interruptor rapido para ocultar o mostrar los saldos
                    IconButton(onClick = { haptics.success(); onToggleBalances() }, modifier = Modifier.testTag(TestTags.SUMMARY_PRIVACY_TOGGLE)) {
                        Icon(
                            imageVector = if (balancesHidden) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = stringResource(if (balancesHidden) R.string.show_balances else R.string.hide_balances),
                        )
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.testTag(TestTags.SETTINGS_ACTION)) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.open_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddTransactionClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_transaction)) },
                modifier = Modifier.testTag(TestTags.ADD_TRANSACTION_BUTTON),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            MonthSelector(
                monthLabel = state.monthLabel,
                isCurrentMonth = state.isCurrentMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onCurrentMonth = onCurrentMonth,
            )
            Spacer(modifier = Modifier.height(12.dp))
            // totales fijos del mes: ingresos, gastos y balance
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    label = stringResource(R.string.income_label),
                    value = maskedAmount(state.incomeCents),
                    valueColor = LocalFinanceColors.current.income,
                    modifier = Modifier.weight(1f).testTag(TestTags.SUMMARY_INCOME_CARD),
                )
                SummaryCard(
                    label = stringResource(R.string.expense_label),
                    value = maskedAmount(state.expenseCents),
                    valueColor = LocalFinanceColors.current.expense,
                    modifier = Modifier.weight(1f).testTag(TestTags.SUMMARY_EXPENSE_CARD),
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            SummaryCard(
                label = stringResource(R.string.monthly_balance_label),
                value = maskedAmount(state.balanceCents),
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SUMMARY_BALANCE_CARD),
            )
            // los modulos opcionales se muestran en el orden y visibilidad elegidos por el usuario
            modules.forEach { module ->
                DashboardModuleContent(module, state, onMovementClick, onQuickAction, onOpenGoals, onOpenPurchases)
            }
            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

// dibuja el contenido de un modulo del resumen segun su tipo
@Composable
private fun DashboardModuleContent(
    module: DashboardModule,
    state: SummaryUiState,
    onMovementClick: (Long) -> Unit,
    onQuickAction: (QuickAction) -> Unit,
    onOpenGoals: () -> Unit,
    onOpenPurchases: () -> Unit,
) {
    when (module) {
        DashboardModule.QUICK_ACTIONS -> ModuleSection(R.string.quick_actions_title, TestTags.MODULE_QUICK_ACTIONS) {
            QuickActionsBar(actions = state.quickActions, onAction = onQuickAction)
        }
        DashboardModule.ACCOUNT_BALANCES -> ModuleSection(R.string.account_balances_title, TestTags.MODULE_ACCOUNT_BALANCES) {
            AmountList(state.accountBalances.map { it.name to it.amountCents })
        }
        DashboardModule.UPCOMING -> if (state.upcoming.isNotEmpty()) UpcomingPaymentsSection(state)
        DashboardModule.BUDGET_PROGRESS -> ModuleSection(R.string.budget_progress_title, TestTags.MODULE_BUDGET_PROGRESS) {
            BudgetProgressContent(state)
        }
        DashboardModule.RECENT -> RecentSection(state, onMovementClick)
        DashboardModule.EXPENSE_CATEGORIES -> ModuleSection(R.string.expense_categories_title, TestTags.MODULE_EXPENSE_CATEGORIES) {
            AmountList(state.expenseCategories.map { it.name to it.totalCents })
        }
        DashboardModule.MONTHLY_TREND -> ModuleSection(R.string.monthly_trend_title, TestTags.MODULE_MONTHLY_TREND) {
            TrendContent(state)
        }
        DashboardModule.CREDIT_CARD_DEBT -> ModuleSection(R.string.credit_card_debt_title, TestTags.MODULE_CREDIT_CARD_DEBT) {
            AmountList(state.creditCards.map { it.name to it.amountCents })
        }
        DashboardModule.SAVINGS_BALANCE -> ModuleSection(R.string.savings_title, TestTags.MODULE_SAVINGS_BALANCE) {
            SavingsContent(state)
        }
        DashboardModule.SAVINGS_GOALS -> ModuleSection(R.string.module_savings_goals, TestTags.MODULE_SAVINGS_GOALS, onSeeAll = onOpenGoals) {
            GoalsModuleContent(state)
        }
        DashboardModule.PLANNED_PURCHASES -> ModuleSection(R.string.module_planned_purchases, TestTags.MODULE_PLANNED_PURCHASES, onSeeAll = onOpenPurchases) {
            PurchasesModuleContent(state)
        }
    }
}

@Composable
private fun GoalsModuleContent(state: SummaryUiState) {
    if (state.savingsGoals.isEmpty()) {
        EmptyState(message = stringResource(R.string.module_empty))
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.closestGoal?.let { goal ->
                Text("${stringResource(R.string.closest_goal)}: ${goal.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            state.savingsGoals.forEach { goal ->
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(goal.name, style = MaterialTheme.typography.bodyMedium)
                        // texto con progreso, no depende solo de la barra
                        Text("${maskedAmount(goal.contributedCents)} / ${maskedAmount(goal.targetCents)} (${goal.progressPercent}%)", style = MaterialTheme.typography.bodySmall)
                    }
                    val fraction = if (goal.targetCents > 0) (goal.contributedCents.toFloat() / goal.targetCents).coerceIn(0f, 1f) else 0f
                    LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun PurchasesModuleContent(state: SummaryUiState) {
    if (state.plannedPurchases.isEmpty()) {
        EmptyState(message = stringResource(R.string.module_empty))
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.nextPurchase?.let { purchase ->
                Text("${stringResource(R.string.next_purchase)}: ${purchase.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            state.plannedPurchases.forEach { purchase ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(purchase.name, style = MaterialTheme.typography.bodyMedium)
                    // muestra lo que falta ahorrar, no depende solo del color
                    Text("${stringResource(R.string.goal_remaining)} ${maskedAmount(purchase.remainingCents)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

// envoltura comun de un modulo: titulo de seccion mas contenido, con accion opcional de ver todas
@Composable
private fun ModuleSection(titleRes: Int, tag: String, onSeeAll: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Column(modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium)
            if (onSeeAll != null) {
                Text(
                    text = stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onSeeAll).padding(4.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

// lista de nombre y monto; oculta los montos cuando la privacidad esta activa
@Composable
private fun AmountList(rows: List<Pair<String, Long>>) {
    if (rows.isEmpty()) {
        EmptyState(message = stringResource(R.string.module_empty))
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (name, cents) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, style = MaterialTheme.typography.bodyMedium)
                    Text(maskedAmount(cents), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressContent(state: SummaryUiState) {
    val budget = state.overallBudget
    if (budget == null) {
        EmptyState(message = stringResource(R.string.module_empty))
        return
    }
    val stateRes = when (budget.state) {
        BudgetState.AVAILABLE -> R.string.balance_available
        BudgetState.WARNING -> R.string.budget_state_warning
        BudgetState.EXCEEDED -> R.string.budget_state_exceeded
    }
    val fraction = if (budget.limitCents > 0) (budget.spentCents.toFloat() / budget.limitCents).coerceIn(0f, 1f) else 0f
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.spent_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(maskedAmount(budget.spentCents), style = MaterialTheme.typography.titleMedium)
            }
            // la barra es solo de presentacion; el estado tambien se dice con texto, no solo con color
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.remaining_label) + ": " + maskedAmount(budget.remainingCents), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(stateRes), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TrendContent(state: SummaryUiState) {
    if (state.trend.isEmpty()) {
        EmptyState(message = stringResource(R.string.module_empty))
        return
    }
    val points = state.trend.map { ChartPoint(it.label, it.balanceCents) }
    // lee la privacidad una vez y arma el resumen textual sin llamar composables dentro del lambda
    val hidden = LocalBalancesHidden.current
    val summary = stringResource(R.string.monthly_trend_title) + ": " +
        state.trend.joinToString(", ") { "${it.label} ${maskAmount(it.balanceCents, hidden)}" }
    InteractiveChartCard(points = points, line = true, summary = summary)
}

@Composable
private fun SavingsContent(state: SummaryUiState) {
    if (state.savingsAccounts.isEmpty()) {
        EmptyState(message = stringResource(R.string.module_empty))
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.savings_total), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(maskedAmount(state.savingsTotalCents), style = MaterialTheme.typography.titleMedium)
            }
            state.savingsAccounts.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                    Text(maskedAmount(item.amountCents), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun RecentSection(state: SummaryUiState, onMovementClick: (Long) -> Unit) {
    Spacer(modifier = Modifier.height(24.dp))
    Text(stringResource(R.string.recent_transactions_title), style = MaterialTheme.typography.titleMedium)
    when {
        state.isLoading -> LoadingState()
        state.recentTransactions.isEmpty() -> {
            Text(
                text = stringResource(R.string.summary_empty_state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag(TestTags.SUMMARY_EMPTY_STATE),
            )
        }
        else -> {
            Column(modifier = Modifier.testTag(TestTags.RECENT_LIST)) {
                state.recentTransactions.forEach { item ->
                    TransactionListRow(item = item, modifier = Modifier.clickable { onMovementClick(item.id) })
                }
            }
        }
    }
}

// fila de navegacion entre meses con boton para volver al mes actual
@Composable
private fun MonthSelector(
    monthLabel: String,
    isCurrentMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.testTag(TestTags.MONTH_PREVIOUS)) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.month_previous))
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).testTag(TestTags.SUMMARY_MONTH),
        )
        IconButton(onClick = onNextMonth, modifier = Modifier.testTag(TestTags.MONTH_NEXT)) {
            Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.month_next))
        }
        if (!isCurrentMonth) {
            IconButton(onClick = onCurrentMonth, modifier = Modifier.testTag(TestTags.MONTH_CURRENT)) {
                Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.month_current))
            }
        }
    }
}

// seccion de proximos pagos, muestra los compromisos del mes sin mezclarlos con los gastos
@Composable
private fun UpcomingPaymentsSection(state: SummaryUiState) {
    Spacer(modifier = Modifier.height(24.dp))
    Column(modifier = Modifier.fillMaxWidth().testTag(TestTags.UPCOMING_PAYMENTS_SECTION)) {
        Text(stringResource(R.string.upcoming_payments_title), style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.month_commitments), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(maskedAmount(state.committedCents), style = MaterialTheme.typography.titleMedium)
                }
                state.upcoming.forEach { item ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(dueLabel(item.dueWhen, item.dueDate), style = MaterialTheme.typography.bodyMedium)
                        Text(maskedAmount(item.amountCents), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun dueLabel(dueWhen: DueWhen, date: java.time.LocalDate): String = when (dueWhen) {
    DueWhen.OVERDUE -> stringResource(R.string.overdue_label)
    DueWhen.TODAY -> stringResource(R.string.due_today)
    DueWhen.TOMORROW -> stringResource(R.string.due_tomorrow)
    DueWhen.LATER -> stringResource(R.string.due_on_format, ShortDateFormatter.format(date))
}

@Composable
private fun SummaryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, color = valueColor)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryScreenPreview() {
    MisFinanzasTheme {
        SummaryScreen(
            state = SummaryUiState(isLoading = false, monthLabel = "Julio de 2026"),
            onAddTransactionClick = {},
            onSettingsClick = {},
        )
    }
}
