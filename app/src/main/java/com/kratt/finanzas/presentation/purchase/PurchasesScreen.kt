package com.kratt.finanzas.presentation.purchase

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.FloatingActionButton
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
import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun PurchasesRoute(
    onBack: () -> Unit,
    onAddPurchase: () -> Unit,
    onPurchaseClick: (Long) -> Unit,
) {
    val viewModel = containerViewModel { PurchasesViewModel(it.plannedPurchaseRepository) }
    val purchases by viewModel.purchases.collectAsStateWithLifecycle()
    PurchasesScreen(
        purchases = purchases,
        onBack = onBack,
        onAddPurchase = onAddPurchase,
        onPurchaseClick = onPurchaseClick,
    )
}

// lista de compras planificadas con su costo, prioridad y estado en texto
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    purchases: List<PlannedPurchase>,
    onBack: () -> Unit,
    onAddPurchase: () -> Unit,
    onPurchaseClick: (Long) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.PURCHASES_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.purchases_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPurchase,
                modifier = Modifier.testTag(TestTags.ADD_PURCHASE_BUTTON),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.purchase_new))
            }
        },
    ) { innerPadding ->
        if (purchases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.purchase_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag(TestTags.PURCHASES_EMPTY_STATE),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(purchases, key = { it.id }) { purchase ->
                    PurchaseCard(purchase = purchase, onClick = { onPurchaseClick(purchase.id) })
                }
            }
        }
    }
}

// tarjeta con el nombre, costo, prioridad y estado de la compra
@Composable
private fun PurchaseCard(
    purchase: PlannedPurchase,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.PURCHASE_ITEM}_${purchase.id}")
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = purchase.name, style = MaterialTheme.typography.titleMedium)
            Text(
                text = maskedAmount(purchase.estimatedCostCents),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // la prioridad y el estado siempre van como texto, nunca solo por color
                Text(
                    text = stringResource(priorityLabelRes(purchase.priority)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(purchaseStatusLabelRes(purchase.status)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
