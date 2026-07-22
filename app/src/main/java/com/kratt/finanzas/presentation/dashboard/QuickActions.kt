package com.kratt.finanzas.presentation.dashboard

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.QuickAction

// etiqueta en espanol de cada accion rapida
@StringRes
fun QuickAction.labelRes(): Int = when (this) {
    QuickAction.ADD_EXPENSE -> R.string.qa_add_expense
    QuickAction.ADD_INCOME -> R.string.qa_add_income
    QuickAction.TRANSFER -> R.string.qa_transfer
    QuickAction.REGISTER_PAYMENT -> R.string.qa_register_payment
    QuickAction.VIEW_MOVEMENTS -> R.string.qa_view_movements
    QuickAction.ADD_ACCOUNT -> R.string.qa_add_account
    QuickAction.CREATE_BUDGET -> R.string.qa_create_budget
    QuickAction.ADD_INSTALLMENT -> R.string.qa_add_installment
    QuickAction.ADD_RECURRING -> R.string.qa_add_recurring
    QuickAction.CREATE_BACKUP -> R.string.qa_create_backup
}

// icono claro de cada accion rapida; siempre acompana al texto
fun QuickAction.icon(): ImageVector = when (this) {
    QuickAction.ADD_EXPENSE -> Icons.AutoMirrored.Filled.TrendingDown
    QuickAction.ADD_INCOME -> Icons.AutoMirrored.Filled.TrendingUp
    QuickAction.TRANSFER -> Icons.Filled.SwapHoriz
    QuickAction.REGISTER_PAYMENT -> Icons.Filled.Payments
    QuickAction.VIEW_MOVEMENTS -> Icons.AutoMirrored.Filled.List
    QuickAction.ADD_ACCOUNT -> Icons.Filled.AccountBalanceWallet
    QuickAction.CREATE_BUDGET -> Icons.Filled.PieChart
    QuickAction.ADD_INSTALLMENT -> Icons.Filled.CreditCard
    QuickAction.ADD_RECURRING -> Icons.Filled.Repeat
    QuickAction.CREATE_BACKUP -> Icons.Filled.Backup
}

// fila de acciones rapidas del resumen; reusa los flujos existentes via onAction
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsBar(
    actions: List<QuickAction>,
    onAction: (QuickAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).testTag(TestTags.DASHBOARD_QUICK_ACTIONS),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        actions.forEach { action ->
            val label = stringResource(action.labelRes())
            val cd = stringResource(R.string.cd_quick_action, label)
            AssistChip(
                onClick = { onAction(action) },
                label = { Text(label) },
                leadingIcon = {
                    Icon(action.icon(), contentDescription = null, modifier = Modifier.size(AssistChipDefaults.IconSize))
                },
                modifier = Modifier
                    .testTag("${TestTags.DASHBOARD_QUICK_ACTION}_${action.name}")
                    .semantics { contentDescription = cd },
            )
        }
    }
}
