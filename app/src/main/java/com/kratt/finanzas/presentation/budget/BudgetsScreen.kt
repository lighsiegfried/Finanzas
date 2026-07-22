package com.kratt.finanzas.presentation.budget

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.presentation.common.cardContentPadding
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun BudgetsRoute(onBack: () -> Unit, onAddBudget: (Int, Int) -> Unit, onEditBudget: (Long) -> Unit) {
    val viewModel = containerViewModel {
        BudgetsViewModel(it.budgetRepository, it.transactionRepository, it.categoryRepository, it.commitmentService)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BudgetsScreen(
        state = state,
        onBack = onBack,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCurrentMonth = viewModel::onCurrentMonth,
        onAddBudget = { val m = viewModel.selectedMonth(); onAddBudget(m.year, m.monthValue) },
        onEditBudget = onEditBudget,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    state: BudgetsUiState,
    onBack: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    onAddBudget: () -> Unit,
    onEditBudget: (Long) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.BUDGETS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.budgets_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddBudget,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_budget)) },
                modifier = Modifier.testTag(TestTags.ADD_BUDGET_BUTTON),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth) { Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.month_previous)) }
                Text(state.monthLabel, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = onNextMonth) { Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.month_next)) }
                if (!state.isCurrentMonth) {
                    IconButton(onClick = onCurrentMonth) { Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.month_current)) }
                }
            }

            state.banner?.let { WarningBanner(it) }

            Card(modifier = Modifier.fillMaxWidth().testTag(TestTags.BUDGET_OVERALL_CARD)) {
                // oculta los montos cuando la privacidad esta activa
                Column(modifier = Modifier.padding(cardContentPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.budget_summary), style = MaterialTheme.typography.titleMedium)
                    LabelValue(stringResource(R.string.actual_expenses), maskedAmount(state.actualExpensesCents))
                    LabelValue(stringResource(R.string.pending_commitments), maskedAmount(state.committedCents))
                    state.availableCents?.let { LabelValue(stringResource(R.string.budget_available), maskedAmount(it)) }
                    val overall = state.overall
                    if (overall != null) {
                        BudgetProgressBlock(stringResource(R.string.budget_overall), overall)
                    } else {
                        Text(stringResource(R.string.no_budget), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            state.categoryBudgets.forEach { row ->
                Card(modifier = Modifier.fillMaxWidth().clickable { onEditBudget(row.budget.id) }.testTag("${TestTags.BUDGET_ITEM}_${row.budget.id}")) {
                    Column(modifier = Modifier.padding(cardContentPadding())) {
                        BudgetProgressBlock(row.categoryName ?: stringResource(R.string.budget_category), row)
                    }
                }
            }

            if (state.categoriesWithoutBudget.isNotEmpty()) {
                Text(stringResource(R.string.categories_without_budget), style = MaterialTheme.typography.titleSmall)
                state.categoriesWithoutBudget.forEach { category ->
                    Text("• ${category.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WarningBanner(banner: BudgetBanner) {
    val messageRes = when (banner) {
        BudgetBanner.NEAR_LIMIT -> R.string.warning_near_limit
        BudgetBanner.CATEGORY_EXCEEDED -> R.string.warning_category_exceeded
        BudgetBanner.OVERALL_EXCEEDED -> R.string.warning_overall_exceeded
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth().testTag(TestTags.BUDGET_WARNING_BANNER),
    ) {
        Text(
            text = stringResource(messageRes),
            modifier = Modifier.padding(16.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun BudgetProgressBlock(title: String, row: BudgetRowUi) {
    val p = row.progress
    val stateRes = when (p.state) {
        BudgetState.AVAILABLE -> R.string.balance_available
        BudgetState.WARNING -> R.string.budget_state_warning
        BudgetState.EXCEEDED -> R.string.budget_state_exceeded
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        LinearProgressIndicator(
            progress = { (p.percentage.coerceIn(0, 100)) / 100f },
            modifier = Modifier.fillMaxWidth(),
        )
        LabelValue(stringResource(R.string.spent_label), maskedAmount(p.spentCents))
        LabelValue(stringResource(R.string.limit_label), maskedAmount(p.limitCents))
        LabelValue(stringResource(R.string.remaining_label), maskedAmount(p.remainingCents))
        // el estado va en texto para no depender solo del color
        Text("${stringResource(stateRes)} · ${p.percentage}%", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun LabelValue(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
