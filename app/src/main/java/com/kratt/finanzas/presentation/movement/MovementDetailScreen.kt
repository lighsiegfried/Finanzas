package com.kratt.finanzas.presentation.movement

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.containerViewModel
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.res.pluralStringResource
import com.kratt.finanzas.domain.model.Attachment

@Composable
fun MovementDetailRoute(
    transactionId: Long,
    onBack: () -> Unit,
    onEditMovement: (Long) -> Unit,
    onEditTransfer: (Long) -> Unit,
    onOpenAttachments: (Long) -> Unit,
) {
    val viewModel = containerViewModel(key = "movement_detail_$transactionId") {
        MovementDetailViewModel(
            it.transactionRepository, it.accountRepository, it.categoryRepository, it.deleteTransaction,
            it.installmentRepository, it.recurringRepository, it.observeAttachments, transactionId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    LaunchedEffect(state.isDeleted) { if (state.isDeleted) onBack() }
    MovementDetailScreen(
        state = state,
        attachments = attachments,
        onBack = onBack,
        onEdit = {
            state.transaction?.let { t ->
                if (t.type == TransactionType.TRANSFER) onEditTransfer(t.id) else onEditMovement(t.id)
            }
        },
        onDelete = viewModel::onDelete,
        onRevert = viewModel::onRevert,
        onDeleteErrorShown = viewModel::onDeleteErrorShown,
        onOpenAttachments = { state.transaction?.let { onOpenAttachments(it.id) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementDetailScreen(
    state: MovementDetailUiState,
    attachments: List<com.kratt.finanzas.domain.model.Attachment> = emptyList(),
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRevert: () -> Unit,
    onDeleteErrorShown: () -> Unit,
    onOpenAttachments: () -> Unit = {},
) {
    var showDelete by remember { mutableStateOf(false) }
    var showRevert by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(state.deleteErrorRes) {
        state.deleteErrorRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            onDeleteErrorShown()
        }
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.MOVEMENT_DETAIL_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.movement_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // un movimiento generado no se edita ni se borra aqui, solo desde su origen
                    if (!state.isGenerated) {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag(TestTags.EDIT_MOVEMENT_BUTTON)) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.edit_action))
                        }
                        IconButton(onClick = { showDelete = true }, modifier = Modifier.testTag(TestTags.DELETE_MOVEMENT_BUTTON)) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete_movement))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        val transaction = state.transaction
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (transaction != null) {
                val typeRes = when (transaction.type) {
                    TransactionType.EXPENSE -> R.string.filter_expenses
                    TransactionType.INCOME -> R.string.filter_incomes
                    TransactionType.TRANSFER -> R.string.filter_transfers
                }
                Text(text = stringResource(typeRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(text = CurrencyFormatter.format(transaction.amountCents), style = MaterialTheme.typography.headlineMedium)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow(stringResource(R.string.account_label), state.accountName)
                        if (transaction.type == TransactionType.TRANSFER) {
                            DetailRow(stringResource(R.string.transfer_destination_label), state.destinationName.orEmpty())
                        } else {
                            DetailRow(stringResource(R.string.category_label), state.categoryName.orEmpty())
                        }
                        DetailRow(stringResource(R.string.date_label), ShortDateFormatter.format(transaction.date))
                        transaction.description?.takeIf { it.isNotBlank() }?.let {
                            DetailRow(stringResource(R.string.description_label), it)
                        }
                    }
                }
                AttachmentsSection(
                    attachments = attachments,
                    onOpen = onOpenAttachments,
                )
                if (state.isGenerated) {
                    Text(
                        text = stringResource(R.string.generated_movement_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (state.canRevert) {
                        Button(
                            onClick = { showRevert = true },
                            modifier = Modifier.fillMaxWidth().testTag(TestTags.REVERT_MOVEMENT_BUTTON),
                        ) { Text(stringResource(R.string.revert_movement)) }
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(R.string.delete_movement)) },
            text = { Text(stringResource(R.string.delete_movement_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDelete()
                    },
                    modifier = Modifier.testTag(TestTags.CONFIRM_DELETE_BUTTON),
                ) { Text(stringResource(R.string.delete_action)) }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }

    if (showRevert) {
        AlertDialog(
            onDismissRequest = { showRevert = false },
            title = { Text(stringResource(R.string.revert_recurring_title)) },
            text = { Text(stringResource(R.string.revert_recurring_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRevert = false
                        onRevert()
                    },
                    modifier = Modifier.testTag(TestTags.CONFIRM_REVERT_BUTTON),
                ) { Text(stringResource(R.string.revert_action)) }
            },
            dismissButton = { TextButton(onClick = { showRevert = false }) { Text(stringResource(R.string.cancel_action)) } },
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

// seccion de comprobantes en el detalle: muestra la cantidad y una lista breve, sin descifrar nada
@Composable
private fun AttachmentsSection(
    attachments: List<Attachment>,
    onOpen: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().testTag(TestTags.ATTACHMENTS_SECTION)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.attachments_section_title),
                style = MaterialTheme.typography.titleMedium,
            )
            if (attachments.isEmpty()) {
                Text(
                    text = stringResource(R.string.attachments_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = pluralStringResource(R.plurals.attachment_count, attachments.size, attachments.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                attachments.take(3).forEach { attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null)
                        Text(text = attachment.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            OutlinedButton(
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth().testTag(TestTags.OPEN_ATTACHMENTS_BUTTON),
            ) {
                Text(stringResource(if (attachments.isEmpty()) R.string.attachment_add else R.string.attachment_view))
            }
        }
    }
}
