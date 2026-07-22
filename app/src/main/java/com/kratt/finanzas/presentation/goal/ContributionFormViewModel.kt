package com.kratt.finanzas.presentation.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.ContributionResult
import com.kratt.finanzas.data.repository.SavingsContributionRepository
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.ContributionType
import com.kratt.finanzas.domain.repository.AccountRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// campos que puede fallar la validacion del aporte
enum class ContributionFormError {
    INVALID_AMOUNT,
    ACCOUNT_REQUIRED,
    NO_LINKED_ACCOUNT,
}

// estado del formulario de aporte; el monto queda como texto hasta guardar
data class ContributionFormUiState(
    val type: ContributionType = ContributionType.MANUAL_TRACKING,
    val amountText: String = "",
    val date: LocalDate = LocalDate.now(),
    val sourceAccountId: Long? = null,
    val note: String = "",
    // la transferencia solo se habilita si la meta tiene cuenta asociada
    val transferEnabled: Boolean = false,
    val errors: Set<ContributionFormError> = emptySet(),
    val isSaved: Boolean = false,
)

// registra un aporte manual o por transferencia hacia la cuenta asociada
class ContributionFormViewModel(
    private val goalRepo: SavingsGoalRepository,
    private val contributionRepo: SavingsContributionRepository,
    accountRepository: AccountRepository,
    private val goalId: Long,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContributionFormUiState())
    val uiState: StateFlow<ContributionFormUiState> = _uiState.asStateFlow()

    val accounts: StateFlow<List<Account>> = accountRepository.observeActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // cargamos la meta para saber si permite transferencia
        viewModelScope.launch {
            val goal = goalRepo.findById(goalId)
            val canTransfer = goal?.linkedAccountId != null
            _uiState.update { it.copy(transferEnabled = canTransfer) }
        }
    }

    // sin cuenta asociada el tipo transferencia queda bloqueado
    fun onTypeChange(type: ContributionType) = _uiState.update {
        if (type == ContributionType.ACCOUNT_TRANSFER && !it.transferEnabled) it
        else it.copy(type = type, errors = it.errors - ContributionFormError.ACCOUNT_REQUIRED)
    }

    fun onAmountChange(value: String) {
        if (value.isEmpty() || AmountParser.isPartialInput(value)) {
            _uiState.update { it.copy(amountText = value, errors = it.errors - ContributionFormError.INVALID_AMOUNT) }
        }
    }

    fun onDateChange(date: LocalDate) = _uiState.update { it.copy(date = date) }

    fun onSourceAccountChange(id: Long) = _uiState.update {
        it.copy(sourceAccountId = id, errors = it.errors - ContributionFormError.ACCOUNT_REQUIRED)
    }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    // arma el aporte segun el tipo y lo manda al repositorio
    fun onSave() {
        val state = _uiState.value
        val cents = AmountParser.parseToCents(state.amountText)
        if (cents == null) {
            _uiState.update { it.copy(errors = it.errors + ContributionFormError.INVALID_AMOUNT) }
            return
        }
        val note = state.note.ifBlank { null }
        viewModelScope.launch {
            val result = if (state.type == ContributionType.ACCOUNT_TRANSFER) {
                val source = state.sourceAccountId
                if (source == null) {
                    _uiState.update { it.copy(errors = it.errors + ContributionFormError.ACCOUNT_REQUIRED) }
                    return@launch
                }
                contributionRepo.addTransfer(goalId, cents, state.date, source, note)
            } else {
                contributionRepo.addManual(goalId, cents, state.date, note)
            }
            handleResult(result)
        }
    }

    // mapea el resultado del repositorio a los errores por campo
    private fun handleResult(result: ContributionResult) {
        when (result) {
            is ContributionResult.Success -> _uiState.update { it.copy(isSaved = true, errors = emptySet()) }
            ContributionResult.InvalidAmount -> _uiState.update { it.copy(errors = it.errors + ContributionFormError.INVALID_AMOUNT) }
            ContributionResult.NoLinkedAccount -> _uiState.update { it.copy(errors = it.errors + ContributionFormError.NO_LINKED_ACCOUNT) }
            ContributionResult.SameAccount -> _uiState.update { it.copy(errors = it.errors + ContributionFormError.ACCOUNT_REQUIRED) }
            ContributionResult.NotFound -> _uiState.update { it.copy(errors = it.errors + ContributionFormError.NO_LINKED_ACCOUNT) }
        }
    }
}
