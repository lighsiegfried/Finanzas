package com.kratt.finanzas.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.listItemVerticalPadding
import com.kratt.finanzas.presentation.common.maskedAmount
import com.kratt.finanzas.presentation.theme.LocalFinanceColors

// fila compartida para mostrar un movimiento en el resumen y en el historial
@Composable
fun TransactionListRow(
    item: TransactionListItem,
    modifier: Modifier = Modifier,
) {
    val isTransfer = item.type == TransactionType.TRANSFER
    val title = item.description?.takeIf { it.isNotBlank() }
        ?: if (isTransfer) stringResource(R.string.transfer_label) else item.categoryName ?: item.accountName
    // oculta el monto real cuando la privacidad de saldos esta activa
    val formattedAmount = maskedAmount(item.amountCents)
    val finance = LocalFinanceColors.current
    // el signo indica el tipo para no depender solo del color
    val amountText = when (item.type) {
        TransactionType.EXPENSE -> stringResource(R.string.expense_amount_format, formattedAmount)
        TransactionType.INCOME -> stringResource(R.string.income_amount_format, formattedAmount)
        TransactionType.TRANSFER -> formattedAmount
    }
    val amountColor = when (item.type) {
        TransactionType.EXPENSE -> finance.expense
        TransactionType.INCOME -> finance.income
        TransactionType.TRANSFER -> finance.transfer
    }
    // en una transferencia se muestra origen y destino en vez de cuenta y fecha
    val subtitle = if (isTransfer && item.destinationAccountName != null) {
        stringResource(R.string.transfer_subtitle_format, item.accountName, item.destinationAccountName)
    } else {
        stringResource(R.string.transaction_subtitle, item.accountName, ShortDateFormatter.format(item.date))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = listItemVerticalPadding())
            .testTag("${TestTags.TRANSACTION_ITEM}_${item.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = amountText,
            style = MaterialTheme.typography.titleMedium,
            color = amountColor,
        )
    }
}
