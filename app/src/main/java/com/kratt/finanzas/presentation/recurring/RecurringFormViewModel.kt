package com.kratt.finanzas.presentation.recurring

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.RecurringTemplateValidator
import com.kratt.finanzas.domain.usecase.RecurringValidationError
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

data class RecurringFormUiState(
    val accounts: List<Account> = emptyList(),
    val categories: List<Category> = emptyList(),
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val accountId: Long? = null,
    val categoryId: Long? = null,
    val amountText: String = "",
    val recurrenceType: RecurrenceType = RecurrenceType.MONTHLY,
    val intervalText: String = "1",
    val startDate: LocalDate = LocalDate.now(),
    val hasEndDate: Boolean = false,
    val endDate: LocalDate = LocalDate.now().plusMonths(1),
    val postingMode: PostingMode = PostingMode.REQUIRE_CONFIRMATION,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val accountErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val endDateErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class RecurringFormViewModel(
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val recurringRepository: RecurringRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringFormUiState(startDate = today, endDate = today.plusMonths(1)))
    val uiState: StateFlow<RecurringFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { accounts ->
                _uiState.update { it.copy(accounts = accounts, accountId = it.accountId ?: accounts.firstOrNull()?.id) }
            }
        }
        viewModelScope.launch {
            _uiState.map { it.type }.distinctUntilChanged()
                .flatMapLatest { type -> categoryRepository.observeActiveByType(type) }
                .collect { categories ->
                    _uiState.update { state ->
                        state.copy(categories = categories, categoryId = state.categoryId?.takeIf { id -> categories.any { it.id == id } })
                    }
                }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameErrorRes = null) }
    fun onTypeChange(type: TransactionType) = _uiState.update { it.copy(type = type) }
    fun onAccountSelected(id: Long) = _uiState.update { it.copy(accountId = id, accountErrorRes = null) }
    fun onCategorySelected(id: Long) = _uiState.update { it.copy(categoryId = id, categoryErrorRes = null) }
    fun onRecurrenceChange(type: RecurrenceType) = _uiState.update { it.copy(recurrenceType = type) }
    fun onStartDateSelected(date: LocalDate) = _uiState.update { it.copy(startDate = date) }
    fun onEndDateSelected(date: LocalDate) = _uiState.update { it.copy(endDate = date, endDateErrorRes = null) }
    fun onToggleEndDate(enabled: Boolean) = _uiState.update { it.copy(hasEndDate = enabled, endDateErrorRes = null) }
    fun onPostingModeChange(mode: PostingMode) = _uiState.update { it.copy(postingMode = mode) }

    fun onAmountChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(amountText = value, amountErrorRes = null) }
    }

    fun onIntervalChange(value: String) {
        if (value.isEmpty() || value.all { it.isDigit() }) _uiState.update { it.copy(intervalText = value) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val amount = AmountParser.parseToCents(state.amountText)
        val interval = state.intervalText.toIntOrNull() ?: 1
        val endDate = state.endDate.takeIf { state.hasEndDate }
        val errors = RecurringTemplateValidator.validate(state.name, state.accountId, state.categoryId, amount, interval, state.startDate, endDate)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameErrorRes = if (RecurringValidationError.EMPTY_NAME in errors) R.string.error_empty_name else null,
                    accountErrorRes = if (RecurringValidationError.MISSING_ACCOUNT in errors) R.string.error_select_account else null,
                    categoryErrorRes = if (RecurringValidationError.MISSING_CATEGORY in errors) R.string.error_select_category else null,
                    amountErrorRes = if (RecurringValidationError.INVALID_AMOUNT in errors) R.string.error_invalid_amount else null,
                    endDateErrorRes = if (RecurringValidationError.INVALID_END_DATE in errors) R.string.error_select_date else null,
                )
            }
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            recurringRepository.createTemplate(
                name = state.name, transactionType = state.type, accountId = state.accountId!!, categoryId = state.categoryId!!,
                amountCents = amount!!, recurrenceType = state.recurrenceType, interval = interval.coerceAtLeast(1),
                startDate = state.startDate, endDate = endDate, postingMode = state.postingMode,
                description = null, today = today,
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
