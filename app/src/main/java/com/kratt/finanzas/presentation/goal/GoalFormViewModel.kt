package com.kratt.finanzas.presentation.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.GoalSaveResult
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.usecase.SavingsGoalValidationError
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// estado del formulario de meta; el monto queda como texto hasta guardar
data class GoalFormUiState(
    val name: String = "",
    val targetText: String = "",
    val targetDate: LocalDate? = null,
    val linkedAccountId: Long? = null,
    val description: String = "",
    val errors: Set<SavingsGoalValidationError> = emptySet(),
    val isSaved: Boolean = false,
)

// crea o edita una meta de ahorro; conserva fecha inicial y estado al editar
class GoalFormViewModel(
    private val repo: SavingsGoalRepository,
    accountRepository: AccountRepository,
    private val goalId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalFormUiState())
    val uiState: StateFlow<GoalFormUiState> = _uiState.asStateFlow()

    // saber si es edicion sirve para el titulo de la pantalla
    val isEditing: Boolean = goalId != null

    // guardamos la fecha inicial y el estado originales para no perderlos al editar
    private var existingStartDate: LocalDate? = null
    private var existingStatus: SavingsGoalStatus = SavingsGoalStatus.ACTIVE

    val accounts: StateFlow<List<Account>> = accountRepository.observeActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // si viene un id cargamos la meta para editarla y prellenar los campos
        if (goalId != null) {
            viewModelScope.launch {
                repo.findById(goalId)?.let { goal ->
                    existingStartDate = goal.startDate
                    existingStatus = goal.status
                    _uiState.update {
                        it.copy(
                            name = goal.name,
                            targetText = AmountParser.formatCents(goal.targetAmountCents),
                            targetDate = goal.targetDate,
                            linkedAccountId = goal.linkedAccountId,
                            description = goal.description.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value, errors = it.errors - SavingsGoalValidationError.NAME_REQUIRED)
    }

    // solo aceptamos lo que parece un monto valido mientras se escribe
    fun onTargetChange(value: String) {
        if (value.isEmpty() || AmountParser.isPartialInput(value)) {
            _uiState.update {
                it.copy(
                    targetText = value,
                    errors = it.errors - SavingsGoalValidationError.INVALID_TARGET - SavingsGoalValidationError.AMOUNT_TOO_LARGE,
                )
            }
        }
    }

    fun onTargetDateChange(date: LocalDate?) = _uiState.update {
        it.copy(targetDate = date, errors = it.errors - SavingsGoalValidationError.TARGET_DATE_BEFORE_START)
    }

    fun onLinkedAccountChange(id: Long?) = _uiState.update { it.copy(linkedAccountId = id) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    // arma la meta desde el estado y la manda a guardar
    fun onSave() {
        val state = _uiState.value
        val targetCents = AmountParser.parseToCents(state.targetText)
        // si el texto no se pudo leer como monto marcamos el objetivo invalido de una vez
        if (targetCents == null) {
            _uiState.update { it.copy(errors = it.errors + SavingsGoalValidationError.INVALID_TARGET) }
            return
        }
        val goal = SavingsGoal(
            id = goalId ?: 0L,
            name = state.name,
            targetAmountCents = targetCents,
            linkedAccountId = state.linkedAccountId,
            startDate = existingStartDate ?: LocalDate.now(),
            targetDate = state.targetDate,
            status = existingStatus,
            description = state.description.ifBlank { null },
        )
        viewModelScope.launch {
            when (val result = repo.save(goal)) {
                is GoalSaveResult.Success -> _uiState.update { it.copy(isSaved = true, errors = emptySet()) }
                is GoalSaveResult.Invalid -> _uiState.update { it.copy(errors = result.errors) }
            }
        }
    }
}
