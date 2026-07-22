package com.kratt.finanzas.presentation.addtransaction

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionDraft
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.AddTransactionResult
import com.kratt.finanzas.domain.usecase.AddTransactionUseCase
import com.kratt.finanzas.domain.usecase.TransactionValidationError
import com.kratt.finanzas.domain.usecase.ValidateTransactionUseCase
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

data class AddTransactionUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val amountText: String = "",
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedAccountId: Long? = null,
    val selectedCategoryId: Long? = null,
    val description: String = "",
    val date: LocalDate = LocalDate.now(),
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val accountErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    @StringRes val dateErrorRes: Int? = null,
    @StringRes val saveErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionViewModel(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val addTransaction: AddTransactionUseCase,
    private val validateTransaction: ValidateTransactionUseCase,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState(date = today))
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        // observa las cuentas activas y deja la primera elegida por defecto
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update { state ->
                    state.copy(
                        accounts = accounts,
                        selectedAccountId = state.selectedAccountId ?: accounts.firstOrNull()?.id,
                    )
                }
            }
        }
        // carga las categorias del tipo elegido y quita la seleccion incompatible
        viewModelScope.launch {
            _uiState.map { it.type }
                .distinctUntilChanged()
                .flatMapLatest { type -> categoryRepository.observeActiveByType(type) }
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(
                            categories = categories,
                            selectedCategoryId = state.selectedCategoryId
                                ?.takeIf { id -> categories.any { it.id == id } },
                        )
                    }
                }
        }
    }

    fun onTypeChange(type: TransactionType) {
        _uiState.update { it.copy(type = type) }
    }

    fun onAmountChange(text: String) {
        // deja pasar solo texto que puede formar un monto valido
        if (AmountParser.isPartialInput(text)) {
            _uiState.update { it.copy(amountText = text, amountErrorRes = null) }
        }
    }

    fun onAccountSelected(id: Long) {
        _uiState.update { it.copy(selectedAccountId = id, accountErrorRes = null) }
    }

    fun onCategorySelected(id: Long) {
        _uiState.update { it.copy(selectedCategoryId = id, categoryErrorRes = null) }
    }

    fun onDescriptionChange(text: String) {
        _uiState.update { it.copy(description = text) }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date, dateErrorRes = null) }
    }

    // valida el formulario y guarda una sola vez aunque toquen rapido el boton
    fun onSaveClick() {
        val current = _uiState.value
        if (current.isSaving || current.isSaved) return

        val draft = TransactionDraft(
            type = current.type,
            amountCents = AmountParser.parseToCents(current.amountText),
            accountId = current.selectedAccountId,
            categoryId = current.selectedCategoryId,
            description = current.description,
            date = current.date,
        )
        val errors = validateTransaction(draft)
        if (errors.isNotEmpty()) {
            showValidationErrors(errors)
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                when (val result = addTransaction(draft)) {
                    is AddTransactionResult.Success -> {
                        _uiState.update { it.copy(isSaved = true) }
                    }
                    is AddTransactionResult.Invalid -> {
                        _uiState.update { it.copy(isSaving = false) }
                        showValidationErrors(result.errors)
                    }
                }
            } catch (e: Exception) {
                // no se registra el detalle del error para no exponer datos financieros
                _uiState.update {
                    it.copy(isSaving = false, saveErrorRes = R.string.error_save_failed)
                }
            }
        }
    }

    fun onSaveErrorShown() {
        _uiState.update { it.copy(saveErrorRes = null) }
    }

    private fun showValidationErrors(errors: Set<TransactionValidationError>) {
        _uiState.update {
            it.copy(
                amountErrorRes = when {
                    TransactionValidationError.INVALID_AMOUNT in errors -> R.string.error_invalid_amount
                    TransactionValidationError.AMOUNT_TOO_LARGE in errors -> R.string.error_amount_too_large
                    else -> null
                },
                accountErrorRes = if (TransactionValidationError.MISSING_ACCOUNT in errors) {
                    R.string.error_missing_account
                } else {
                    null
                },
                categoryErrorRes = if (TransactionValidationError.MISSING_CATEGORY in errors) {
                    R.string.error_missing_category
                } else {
                    null
                },
                dateErrorRes = if (TransactionValidationError.INVALID_DATE in errors) {
                    R.string.error_invalid_date
                } else {
                    null
                },
            )
        }
    }
}
