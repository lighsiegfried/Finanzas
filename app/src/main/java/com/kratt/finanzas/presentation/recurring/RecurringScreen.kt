package com.kratt.finanzas.presentation.recurring

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.cardContentPadding
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.listItemSpacing
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun RecurringRoute(onBack: () -> Unit, onAdd: () -> Unit) {
    val viewModel = containerViewModel { RecurringViewModel(it.recurringRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RecurringScreen(state, onBack, onAdd, viewModel::onConfirm, viewModel::onSkip)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    state: RecurringUiState,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onConfirm: (Long) -> Unit,
    onSkip: (Long) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.RECURRING_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.recurring_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_recurring_title)) },
                modifier = Modifier.testTag(TestTags.ADD_RECURRING_BUTTON),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(listItemSpacing()),
        ) {
            items(state.rows, key = { it.template.id }) { row ->
                Card(modifier = Modifier.fillMaxWidth().testTag("${TestTags.RECURRING_ITEM}_${row.template.id}")) {
                    Column(modifier = Modifier.padding(cardContentPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(row.template.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = stringResource(
                                        if (row.template.transactionType == TransactionType.EXPENSE) R.string.recurring_expense else R.string.recurring_income,
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            // oculta el monto cuando la privacidad esta activa
                            Text(maskedAmount(row.template.amountCents), style = MaterialTheme.typography.titleMedium)
                        }
                        val next = row.nextOccurrence
                        if (next != null) {
                            Text(
                                text = stringResource(R.string.due_on_format, ShortDateFormatter.format(next.scheduledDate)),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(
                                    onClick = { onConfirm(next.id) },
                                    modifier = Modifier.testTag("${TestTags.CONFIRM_OCCURRENCE_BUTTON}_${row.template.id}"),
                                ) { Text(stringResource(R.string.confirm_action)) }
                                OutlinedButton(
                                    onClick = { onSkip(next.id) },
                                    modifier = Modifier.testTag("${TestTags.SKIP_OCCURRENCE_BUTTON}_${row.template.id}"),
                                ) { Text(stringResource(R.string.skip_action)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
