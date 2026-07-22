package com.kratt.finanzas.presentation.movement

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionDraft
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.AddTransactionResult
import com.kratt.finanzas.domain.usecase.EditTransactionUseCase
import com.kratt.finanzas.domain.usecase.TransactionValidationError
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditMovementUiState(
    val loaded: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    val dirty: Boolean = false,
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val accountErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    @StringRes val saveErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class EditMovementViewModel(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val editTransaction: EditTransactionUseCase,
    private val transactionId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditMovementUiState())
    val uiState: StateFlow<EditMovementUiState> = _uiState.asStateFlow()

    private var existing: Transaction? = null

    init {
        viewModelScope.launch {
            existing = transactionRepository.findById(transactionId)
            existing?.let { t ->
                _uiState.update {
                    it.copy(
                        loaded = true,
                        type = t.type,
                        amountText = AmountParser.formatCents(t.amountCents),
                        selectedAccountId = t.accountId,
                        selectedCategoryId = t.categoryId,
                        description = t.description.orEmpty(),
                        date = t.date,
                    )
                }
            }
        }
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
        viewModelScope.launch {
            _uiState.map { it.type }.distinctUntilChanged()
                .flatMapLatest { type -> categoryRepository.observeActiveByType(type) }
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategoryId = state.selectedCategoryId?.takeIf { id -> categories.any { it.id == id } },
                        )
                    }
                }
        }
    }

    // cambiar el tipo limpia la categoria incompatible
    fun onTypeChange(type: TransactionType) = _uiState.update { it.copy(type = type, dirty = true) }
    fun onAccountSelected(id: Long) = _uiState.update { it.copy(selectedAccountId = id, accountErrorRes = null, dirty = true) }
    fun onCategorySelected(id: Long) = _uiState.update { it.copy(selectedCategoryId = id, categoryErrorRes = null, dirty = true) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value, dirty = true) }
    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(date = date, dirty = true) }

    fun onAmountChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(amountText = value, amountErrorRes = null, dirty = true) }
    }

    fun onSave() {
        val current = existing ?: return
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val draft = TransactionDraft(
            type = state.type,
            amountCents = AmountParser.parseToCents(state.amountText),
            accountId = state.selectedAccountId,
            categoryId = state.selectedCategoryId,
            description = state.description,
            date = state.date,
        )
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (val result = editTransaction(current, draft)) {
                    AddTransactionResult.Success -> _uiState.update { it.copy(isSaved = true) }
                    is AddTransactionResult.Invalid -> {
                        _uiState.update { it.copy(isSaving = false) }
                        showErrors(result.errors)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveErrorRes = R.string.error_save_changes) }
            }
        }
    }

    fun onSaveErrorShown() = _uiState.update { it.copy(saveErrorRes = null) }

    private fun showErrors(errors: Set<TransactionValidationError>) {
        _uiState.update {
            it.copy(
                amountErrorRes = when {
                    TransactionValidationError.INVALID_AMOUNT in errors -> R.string.error_invalid_amount
                    TransactionValidationError.AMOUNT_TOO_LARGE in errors -> R.string.error_amount_too_large
                    else -> null
                },
                accountErrorRes = if (TransactionValidationError.MISSING_ACCOUNT in errors) R.string.error_missing_account else null,
                categoryErrorRes = if (TransactionValidationError.MISSING_CATEGORY in errors) R.string.error_missing_category else null,
            )
        }
    }
}
