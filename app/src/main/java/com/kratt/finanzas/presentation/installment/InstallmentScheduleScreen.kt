package com.kratt.finanzas.presentation.installment

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.InstallmentOccurrence
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.presentation.common.StatusLabels
import com.kratt.finanzas.presentation.common.containerViewModel

@Composable
fun InstallmentScheduleRoute(planId: Long, onBack: () -> Unit) {
    val viewModel = containerViewModel(key = "installment_schedule_$planId") {
        InstallmentScheduleViewModel(it.installmentRepository, planId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InstallmentScheduleScreen(state, onBack, viewModel::onPay)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentScheduleScreen(
    state: InstallmentScheduleUiState,
    onBack: () -> Unit,
    onPay: (Long) -> Unit,
) {
    var pendingPayment by remember { mutableStateOf<Long?>(null) }
    Scaffold(
        modifier = Modifier.testTag(TestTags.INSTALLMENT_SCHEDULE_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.view_schedule)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            items(state.occurrences, key = { it.id }) { occurrence ->
                OccurrenceRow(occurrence, state.installmentCount) { pendingPayment = occurrence.id }
                HorizontalDivider()
            }
        }
    }

    pendingPayment?.let { occurrenceId ->
        AlertDialog(
            onDismissRequest = { pendingPayment = null },
            title = { Text(stringResource(R.string.register_payment)) },
            text = { Text(stringResource(R.string.register_payment_message)) },
            confirmButton = {
                TextButton(
                    onClick = { onPay(occurrenceId); pendingPayment = null },
                    modifier = Modifier.testTag(TestTags.CONFIRM_PAYMENT_BUTTON),
                ) { Text(stringResource(R.string.register_payment)) }
            },
            dismissButton = { TextButton(onClick = { pendingPayment = null }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
}

@Composable
private fun OccurrenceRow(occurrence: InstallmentOccurrence, count: Int, onPayClick: () -> Unit) {
    val payable = occurrence.status == InstallmentOccurrenceStatus.PENDING || occurrence.status == InstallmentOccurrenceStatus.OVERDUE
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("${TestTags.OCCURRENCE_ITEM}_${occurrence.sequenceNumber}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.installment_of_format, occurrence.sequenceNumber, count), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "${ShortDateFormatter.format(occurrence.dueDate)} · ${stringResource(StatusLabels.installmentOccurrence(occurrence.status))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(CurrencyFormatter.format(occurrence.amountCents), style = MaterialTheme.typography.titleMedium)
        if (payable) {
            TextButton(onClick = onPayClick, modifier = Modifier.testTag("${TestTags.MARK_PAID_BUTTON}_${occurrence.sequenceNumber}")) {
                Text(stringResource(R.string.mark_as_paid))
            }
        }
    }
}
