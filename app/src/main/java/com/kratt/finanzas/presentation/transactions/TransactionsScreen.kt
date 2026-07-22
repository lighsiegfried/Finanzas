package com.kratt.finanzas.presentation.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DropdownField
import com.kratt.finanzas.presentation.components.TransactionListRow

@Composable
fun TransactionsRoute(
    onMovementClick: (Long) -> Unit,
    initialType: TransactionType? = null,
    initialAccountId: Long? = null,
    initialCategoryId: Long? = null,
) {
    // clave distinta cuando llega filtrado para no reusar el viewmodel de la pestana
    val key = if (initialType == null && initialAccountId == null && initialCategoryId == null) null
    else "transactions_${initialType}_${initialAccountId}_${initialCategoryId}"
    val viewModel = containerViewModel(key = key) {
        TransactionsViewModel(
            it.transactionRepository, it.accountRepository, it.categoryRepository,
            initialType = initialType, initialAccountId = initialAccountId, initialCategoryId = initialCategoryId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TransactionsScreen(
        state = state,
        onMovementClick = onMovementClick,
        onQueryChange = viewModel::onQueryChange,
        onTypeFilter = viewModel::onTypeFilter,
        onAccountFilter = viewModel::onAccountFilter,
        onCategoryFilter = viewModel::onCategoryFilter,
        onClearFilters = viewModel::onClearFilters,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onCurrentMonth = viewModel::onCurrentMonth,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onMovementClick: (Long) -> Unit,
    onQueryChange: (String) -> Unit,
    onTypeFilter: (TransactionType?) -> Unit,
    onAccountFilter: (Long?) -> Unit,
    onCategoryFilter: (Long?) -> Unit,
    onClearFilters: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCurrentMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.testTag(TestTags.TRANSACTIONS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { CenterAlignedTopAppBar(title = { Text(stringResource(R.string.transactions_title)) }) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            // navegacion de mes
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreviousMonth, modifier = Modifier.testTag(TestTags.MONTH_PREVIOUS)) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.month_previous))
                }
                Text(state.monthLabel, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                IconButton(onClick = onNextMonth, modifier = Modifier.testTag(TestTags.MONTH_NEXT)) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.month_next))
                }
                if (!state.isCurrentMonth) {
                    IconButton(onClick = onCurrentMonth, modifier = Modifier.testTag(TestTags.MONTH_CURRENT)) {
                        Icon(Icons.Filled.Today, contentDescription = stringResource(R.string.month_current))
                    }
                }
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.search_movements)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.SEARCH_FIELD),
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TypeChip(stringResource(R.string.filter_all), state.filter.type == null, TestTags.FILTER_TYPE_ALL) { onTypeFilter(null) }
                TypeChip(stringResource(R.string.filter_expenses), state.filter.type == TransactionType.EXPENSE, TestTags.FILTER_TYPE_EXPENSE) { onTypeFilter(TransactionType.EXPENSE) }
                TypeChip(stringResource(R.string.filter_incomes), state.filter.type == TransactionType.INCOME, TestTags.FILTER_TYPE_INCOME) { onTypeFilter(TransactionType.INCOME) }
                TypeChip(stringResource(R.string.filter_transfers), state.filter.type == TransactionType.TRANSFER, TestTags.FILTER_TYPE_TRANSFER) { onTypeFilter(TransactionType.TRANSFER) }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DropdownField(
                    label = stringResource(R.string.filter_account),
                    options = listOf<Account?>(null) + state.accounts,
                    selected = state.accounts.firstOrNull { it.id == state.filter.accountId },
                    optionLabel = { it?.name ?: stringResource(R.string.filter_all) },
                    onSelected = { onAccountFilter(it?.id) },
                    tag = "filter_account_field",
                    modifier = Modifier.weight(1f),
                )
                DropdownField(
                    label = stringResource(R.string.filter_category),
                    options = listOf<Category?>(null) + state.categories,
                    selected = state.categories.firstOrNull { it.id == state.filter.categoryId },
                    optionLabel = { it?.name ?: stringResource(R.string.filter_all) },
                    onSelected = { onCategoryFilter(it?.id) },
                    tag = "filter_category_field",
                    modifier = Modifier.weight(1f),
                )
            }
            if (!state.filter.isEmpty) {
                TextButton(onClick = onClearFilters, modifier = Modifier.testTag(TestTags.CLEAR_FILTERS_BUTTON)) {
                    Text(stringResource(R.string.clear_filters))
                }
            }
            when {
                state.results.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().testTag(TestTags.TRANSACTIONS_LIST),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        items(state.results, key = { it.id }) { item ->
                            TransactionListRow(item = item, modifier = Modifier.clickable { onMovementClick(item.id) })
                        }
                    }
                }
                state.hasMovementsThisMonth -> EmptyMessage(R.string.no_results, TestTags.NO_RESULTS)
                else -> EmptyMessage(R.string.summary_empty_state, TestTags.TRANSACTIONS_EMPTY_STATE)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypeChip(label: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.testTag(tag))
}

@Composable
private fun EmptyMessage(res: Int, tag: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(res),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(tag),
        )
    }
}
