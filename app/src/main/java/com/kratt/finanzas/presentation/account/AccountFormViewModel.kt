package com.kratt.finanzas.presentation.account

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.usecase.AccountValidationError
import com.kratt.finanzas.domain.usecase.AccountValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountFormUiState(
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    val name: String = "",
    val type: AccountType = AccountType.CASH,
    val initialBalanceText: String = "",
    val creditLimitText: String = "",
    val lastFour: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val lastFourErrorRes: Int? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

class AccountFormViewModel(
    private val accountRepository: AccountRepository,
    private val accountId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountFormUiState(isEdit = accountId != null))
    val uiState: StateFlow<AccountFormUiState> = _uiState.asStateFlow()

    private var existing: Account? = null
    private var otherActiveNames: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            if (accountId != null) {
                existing = accountRepository.findById(accountId)
                existing?.let { account ->
                    _uiState.update {
                        it.copy(
                            loaded = true,
                            name = account.name,
                            type = account.type,
                            initialBalanceText = AmountParser.formatCents(account.initialBalanceCents),
                            creditLimitText = account.creditLimitCents?.let(AmountParser::formatCents).orEmpty(),
                            lastFour = account.lastFourDigits.orEmpty(),
                            description = account.description.orEmpty(),
                            isActive = account.isActive,
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(loaded = true) }
            }
        }
        // nombres activos en minusculas para detectar duplicados, sin la cuenta que se edita
        viewModelScope.launch {
            accountRepository.observeActiveAccounts().collect { list ->
                otherActiveNames = list.filter { it.id != accountId }.map { it.name.lowercase() }.toSet()
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameErrorRes = null) }
    fun onTypeChange(value: AccountType) = _uiState.update { it.copy(type = value) }
    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onInitialBalanceChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(initialBalanceText = value, amountErrorRes = null) }
    }

    fun onCreditLimitChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(creditLimitText = value, amountErrorRes = null) }
    }

    fun onLastFourChange(value: String) {
        if (value.length <= 4 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(lastFour = value, lastFourErrorRes = null) }
        }
    }

    // valida y guarda la cuenta, evita guardar dos veces
    fun onSave() {
        val state = _uiState.value
        if (state.isSaving || state.isSaved) return
        val initial = parsedInitial(state)
        val creditInvalid = state.type == AccountType.CREDIT_CARD &&
            state.creditLimitText.isNotBlank() && AmountParser.parseToCents(state.creditLimitText) == null
        val errors = AccountValidator.validate(state.name, initial, state.lastFour, otherActiveNames)
        if (errors.isNotEmpty() || creditInvalid) {
            showErrors(errors, creditInvalid)
            return
        }
        _uiState.update { it.copy(isSaving = true) }
        val account = Account(
            id = existing?.id ?: 0L,
            name = state.name.trim(),
            type = state.type,
            currencyCode = "GTQ",
            initialBalanceCents = initial ?: 0L,
            isActive = existing?.isActive ?: true,
            creditLimitCents = parsedCreditLimit(state),
            lastFourDigits = state.lastFour.trim().takeIf { it.isNotEmpty() },
            description = state.description.trim().takeIf { it.isNotEmpty() },
            createdAtMillis = existing?.createdAtMillis ?: 0L,
        )
        viewModelScope.launch {
            if (account.id == 0L) accountRepository.insert(account) else accountRepository.update(account)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    // activa o desactiva la cuenta conservando su historial
    fun onToggleActive() {
        val id = existing?.id ?: return
        viewModelScope.launch {
            accountRepository.setActive(id, !_uiState.value.isActive)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    private fun parsedInitial(state: AccountFormUiState): Long? =
        AmountParser.parseAmountAllowingZero(state.initialBalanceText)

    private fun parsedCreditLimit(state: AccountFormUiState): Long? {
        if (state.type != AccountType.CREDIT_CARD) return null
        val text = state.creditLimitText.trim()
        return if (text.isEmpty()) null else AmountParser.parseToCents(text)
    }

    private fun showErrors(errors: Set<AccountValidationError>, creditInvalid: Boolean) {
        _uiState.update {
            it.copy(
                nameErrorRes = when {
                    AccountValidationError.EMPTY_NAME in errors -> R.string.error_empty_name
                    AccountValidationError.DUPLICATE_NAME in errors -> R.string.error_duplicate_account
                    else -> null
                },
                amountErrorRes = if (AccountValidationError.INVALID_AMOUNT in errors || creditInvalid) R.string.error_invalid_amount else null,
                lastFourErrorRes = if (AccountValidationError.INVALID_LAST_FOUR in errors) R.string.error_invalid_last_four else null,
            )
        }
    }
}
