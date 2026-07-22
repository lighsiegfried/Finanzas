package com.kratt.finanzas.presentation.installment

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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.presentation.common.cardContentPadding
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun InstallmentDetailRoute(planId: Long, onBack: () -> Unit, onViewSchedule: (Long) -> Unit) {
    val viewModel = containerViewModel(key = "installment_detail_$planId") {
        InstallmentDetailViewModel(it.installmentRepository, it.accountRepository, it.categoryRepository, planId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InstallmentDetailScreen(state, onBack, { onViewSchedule(planId) }, viewModel::onPauseResume, viewModel::onCancelPlan)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentDetailScreen(
    state: InstallmentDetailUiState,
    onBack: () -> Unit,
    onViewSchedule: () -> Unit,
    onPauseResume: () -> Unit,
    onCancelPlan: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.INSTALLMENT_DETAIL_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(state.plan?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        val plan = state.plan
        val progress = state.progress
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (plan != null && progress != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    // oculta los montos cuando la privacidad esta activa
                    Column(modifier = Modifier.padding(cardContentPadding()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(stringResource(R.string.total_amount_label), maskedAmount(progress.totalCents))
                        DetailRow(stringResource(R.string.remaining_balance), maskedAmount(progress.remainingCents))
                        DetailRow(stringResource(R.string.paid_installments), "${progress.paidCount}")
                        DetailRow(stringResource(R.string.pending_installments), "${progress.pendingCount}")
                        DetailRow(
                            stringResource(R.string.installment_of_format, progress.paidCount, plan.installmentCount),
                            "",
                        )
                        state.nextDue?.let { DetailRow(stringResource(R.string.next_payment), ShortDateFormatter.format(it)) }
                        state.completion?.let { DetailRow(stringResource(R.string.ends_in), ShortDateFormatter.format(it)) }
                        DetailRow(stringResource(R.string.account_label), state.accountName)
                        DetailRow(stringResource(R.string.category_label), state.categoryName)
                    }
                }
                Button(onClick = onViewSchedule, modifier = Modifier.fillMaxWidth().testTag(TestTags.VIEW_SCHEDULE_BUTTON)) {
                    Text(stringResource(R.string.view_schedule))
                }
                OutlinedButton(onClick = onPauseResume, modifier = Modifier.fillMaxWidth().testTag(TestTags.PAUSE_INSTALLMENT_BUTTON)) {
                    Text(stringResource(if (plan.status == InstallmentStatus.PAUSED) R.string.resume else R.string.pause))
                }
                OutlinedButton(onClick = onCancelPlan, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel_installment))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
