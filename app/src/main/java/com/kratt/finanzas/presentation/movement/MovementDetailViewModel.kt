package com.kratt.finanzas.presentation.movement

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.data.repository.RevertResult
import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.DeleteTransactionUseCase
import com.kratt.finanzas.domain.usecase.ObserveAttachmentsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovementDetailUiState(
    val loaded: Boolean = false,
    val transaction: Transaction? = null,
    val accountName: String = "",
    val categoryName: String? = null,
    val destinationName: String? = null,
    val isGenerated: Boolean = false,
    val canRevert: Boolean = false,
    val isDeleted: Boolean = false,
    @StringRes val deleteErrorRes: Int? = null,
)

class MovementDetailViewModel(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val deleteTransaction: DeleteTransactionUseCase,
    private val installmentRepository: InstallmentRepository,
    private val recurringRepository: RecurringRepository,
    observeAttachments: ObserveAttachmentsUseCase,
    transactionId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MovementDetailUiState())
    val uiState: StateFlow<MovementDetailUiState> = _uiState.asStateFlow()

    // adjuntos del movimiento; solo metadatos, no descifra nada en el detalle
    val attachments: StateFlow<List<Attachment>> =
        observeAttachments(transactionId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // carga el movimiento y resuelve los nombres de cuenta y categoria
        viewModelScope.launch {
            val transaction = transactionRepository.findById(transactionId)
            if (transaction == null) {
                _uiState.update { it.copy(loaded = true) }
                return@launch
            }
            val accountName = accountRepository.findById(transaction.accountId)?.name.orEmpty()
            val categoryName = transaction.categoryId?.let { categoryRepository.findById(it)?.name }
            val destinationName = transaction.destinationAccountId?.let { accountRepository.findById(it)?.name }
            _uiState.value = MovementDetailUiState(
                loaded = true,
                transaction = transaction,
                accountName = accountName,
                categoryName = categoryName,
                destinationName = destinationName,
                isGenerated = transaction.isGenerated,
                // solo los movimientos de recurrentes se revierten desde aqui
                canRevert = transaction.originKey?.startsWith("recurring:") == true,
            )
        }
    }

    fun onDelete() {
        val transaction = _uiState.value.transaction ?: return
        viewModelScope.launch {
            try {
                // un movimiento generado no se borra aqui, se revierte desde su origen
                val deleted = deleteTransaction(transaction)
                if (deleted) {
                    _uiState.update { it.copy(isDeleted = true) }
                } else {
                    _uiState.update { it.copy(deleteErrorRes = R.string.error_delete_generated_movement) }
                }
            } catch (e: Exception) {
                // no se registra el detalle para no exponer datos del movimiento
                _uiState.update { it.copy(deleteErrorRes = R.string.error_delete_movement) }
            }
        }
    }

    // revierte el movimiento generado desde su origen: cuota o recurrente
    fun onRevert() {
        val transaction = _uiState.value.transaction ?: return
        val key = transaction.originKey ?: return
        viewModelScope.launch {
            val ok = try {
                when {
                    key.startsWith("installment:") -> {
                        val id = key.removePrefix("installment:").toLongOrNull()
                        if (id != null) { installmentRepository.revertOccurrence(id); true } else false
                    }
                    key.startsWith("recurring:") -> {
                        val id = key.removePrefix("recurring:").toLongOrNull()
                        id != null && recurringRepository.revertOccurrence(id) is RevertResult.Success
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
            if (ok) {
                _uiState.update { it.copy(isDeleted = true) }
            } else {
                _uiState.update { it.copy(deleteErrorRes = R.string.error_revert_movement) }
            }
        }
    }

    fun onDeleteErrorShown() = _uiState.update { it.copy(deleteErrorRes = null) }
}
