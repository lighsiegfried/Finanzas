package com.kratt.finanzas.presentation.transfer

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.SaveTransferUseCase
import com.kratt.finanzas.domain.usecase.TransferResult
import com.kratt.finanzas.domain.usecase.TransferValidationError
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TransferFormUiState(
    val accounts: List<Account> = emptyList(),
    val sourceId: Long? = null,
    val destinationId: Long? = null,
    val amountText: String = "",
    val date: LocalDate = LocalDate.now(),
    val description: String = "",
    @StringRes val sourceErrorRes: Int? = null,
    @StringRes val destinationErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

class TransferFormViewModel(
    accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val saveTransfer: SaveTransferUseCase,
    private val transferId: Long?,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferFormUiState(date = today))
    val uiState: StateFlow<TransferFormUiState> = _uiState.asStateFlow()

    private var createdAtMillis: Long? = null

    init {
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        if (transferId != null) {
            viewModelScope.launch {
                transactionRepository.findById(transferId)?.let { transfer ->
                    createdAtMillis = transfer.createdAtMillis
                    _uiState.update {
                        it.copy(
                            sourceId = transfer.accountId,
                            destinationId = transfer.destinationAccountId,
                            amountText = AmountParser.formatCents(transfer.amountCents),
                            date = transfer.date,
                            description = transfer.description.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onSourceSelected(id: Long) = _uiState.update { it.copy(sourceId = id, sourceErrorRes = null) }
    fun onDestinationSelected(id: Long) = _uiState.update { it.copy(destinationId = id, destinationErrorRes = null) }
    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(date = date) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onAmountChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(amountText = value, amountErrorRes = null) }
    }

    // valida y guarda la transferencia como una sola fila
    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val result = saveTransfer(
                id = transferId ?: 0L,
                sourceAccountId = state.sourceId,
                destinationAccountId = state.destinationId,
                amountCents = AmountParser.parseToCents(state.amountText),
                date = state.date,
                description = state.description,
                createdAtMillis = createdAtMillis,
            )
            when (result) {
                TransferResult.Success -> _uiState.update { it.copy(isSaved = true) }
                is TransferResult.Invalid -> {
                    _uiState.update { it.copy(isSaving = false) }
                    showErrors(result.errors)
                }
            }
        }
    }

    private fun showErrors(errors: Set<TransferValidationError>) {
        _uiState.update {
            it.copy(
                sourceErrorRes = if (TransferValidationError.MISSING_SOURCE in errors) R.string.error_missing_source else null,
                destinationErrorRes = when {
                    TransferValidationError.MISSING_DESTINATION in errors -> R.string.error_missing_destination
                    TransferValidationError.SAME_ACCOUNT in errors -> R.string.error_same_account
                    else -> null
                },
                amountErrorRes = if (TransferValidationError.INVALID_AMOUNT in errors) R.string.error_invalid_amount else null,
            )
        }
    }
}
