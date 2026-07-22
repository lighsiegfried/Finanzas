package com.kratt.finanzas.presentation.installment

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.InstallmentPlan
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.presentation.common.cardContentPadding
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.listItemSpacing
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun InstallmentsRoute(onBack: () -> Unit, onAdd: () -> Unit, onPlanClick: (Long) -> Unit) {
    val viewModel = containerViewModel { InstallmentsViewModel(it.installmentRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    InstallmentsScreen(state, onBack, viewModel::onFilter, onAdd, onPlanClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(
    state: InstallmentsUiState,
    onBack: () -> Unit,
    onFilter: (InstallmentStatus?) -> Unit,
    onAdd: () -> Unit,
    onPlanClick: (Long) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.INSTALLMENTS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.installments_title)) },
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
                text = { Text(stringResource(R.string.new_installment_title)) },
                modifier = Modifier.testTag(TestTags.ADD_INSTALLMENT_BUTTON),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.installment_filter_all), state.filter == null) { onFilter(null) }
                Chip(stringResource(R.string.installment_filter_active), state.filter == InstallmentStatus.ACTIVE) { onFilter(InstallmentStatus.ACTIVE) }
                Chip(stringResource(R.string.installment_filter_completed), state.filter == InstallmentStatus.COMPLETED) { onFilter(InstallmentStatus.COMPLETED) }
                Chip(stringResource(R.string.installment_filter_paused), state.filter == InstallmentStatus.PAUSED) { onFilter(InstallmentStatus.PAUSED) }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(listItemSpacing())) {
                items(state.plans, key = { it.id }) { plan -> PlanCard(plan) { onPlanClick(plan.id) } }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
private fun PlanCard(plan: InstallmentPlan, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("${TestTags.INSTALLMENT_ITEM}_${plan.id}"),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(cardContentPadding()), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = stringResource(R.string.installment_of_format, plan.paidInstallments, plan.installmentCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // oculta el monto cuando la privacidad esta activa
            Text(maskedAmount(plan.totalAmountCents), style = MaterialTheme.typography.titleMedium)
        }
    }
}
