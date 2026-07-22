package com.kratt.finanzas.presentation.installment

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.InstallmentDistribution
import com.kratt.finanzas.domain.usecase.InstallmentPlanValidator
import com.kratt.finanzas.domain.usecase.InstallmentValidationError
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InstallmentFormUiState(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val name: String = "",
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val totalText: String = "",
    val countText: String = "",
    val description: String = "",
    val firstDueDate: LocalDate = LocalDate.now(),
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val accountErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    @StringRes val totalErrorRes: Int? = null,
    @StringRes val countErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
) {
    // monto por cuota calculado solo para mostrar
    val installmentAmountCents: Long?
        get() {
            val total = AmountParser.parseToCents(totalText) ?: return null
            val count = countText.toIntOrNull() ?: return null
            if (count < 1) return null
            return InstallmentDistribution.distribute(total, count).first()
        }
}

class InstallmentFormViewModel(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val installmentRepository: InstallmentRepository,
    today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(InstallmentFormUiState(firstDueDate = today))
    val uiState: StateFlow<InstallmentFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts, accountId = it.accountId ?: accounts.firstOrNull()?.id) }
            }
        }
        viewModelScope.launch {
            // las cuotas son gastos, se usan las categorias de gasto
            categoryRepository.observeActiveByType(TransactionType.EXPENSE).collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameErrorRes = null) }
    fun onAccountSelected(id: Long) = _uiState.update { it.copy(accountId = id, accountErrorRes = null) }
    fun onCategorySelected(id: Long) = _uiState.update { it.copy(categoryId = id, categoryErrorRes = null) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }
    fun onDateSelected(date: LocalDate) = _uiState.update { it.copy(firstDueDate = date) }

    fun onTotalChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(totalText = value, totalErrorRes = null) }
    }

    fun onCountChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) _uiState.update { it.copy(countText = value, countErrorRes = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val total = AmountParser.parseToCents(state.totalText)
        val count = state.countText.toIntOrNull()
        val errors = InstallmentPlanValidator.validate(state.name, state.accountId, state.categoryId, total, count, state.firstDueDate)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameErrorRes = if (InstallmentValidationError.EMPTY_NAME in errors) R.string.error_empty_name else null,
                    accountErrorRes = if (InstallmentValidationError.MISSING_ACCOUNT in errors) R.string.error_select_account else null,
                    categoryErrorRes = if (InstallmentValidationError.MISSING_CATEGORY in errors) R.string.error_select_category else null,
                    totalErrorRes = if (InstallmentValidationError.INVALID_TOTAL in errors) R.string.error_invalid_amount else null,
                    countErrorRes = if (InstallmentValidationError.INVALID_COUNT in errors) R.string.error_invalid_count else null,
                )
            }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            installmentRepository.createPlan(
                name = state.name, accountId = state.accountId!!, categoryId = state.categoryId!!,
                totalCents = total!!, installmentCount = count!!, firstDueDate = state.firstDueDate,
                description = state.description,
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
