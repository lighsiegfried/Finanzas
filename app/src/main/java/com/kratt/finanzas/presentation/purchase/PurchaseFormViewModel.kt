package com.kratt.finanzas.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.PlannedPurchaseRepository
import com.kratt.finanzas.data.repository.PurchaseSaveResult
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.PurchaseValidationError
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// estado del formulario de compra planificada; el costo queda como texto hasta guardar
data class PurchaseFormUiState(
    val name: String = "",
    val costText: String = "",
    val categoryId: Long? = null,
    val savingsGoalId: Long? = null,
    val targetDate: LocalDate? = null,
    val priority: PurchasePriority = PurchasePriority.MEDIUM,
    val description: String = "",
    val vendor: String = "",
    val errors: Set<PurchaseValidationError> = emptySet(),
    val goalAlreadyLinked: Boolean = false,
    val isSaved: Boolean = false,
)

// crea o edita una compra planificada
class PurchaseFormViewModel(
    private val repo: PlannedPurchaseRepository,
    savingsGoalRepository: SavingsGoalRepository,
    categoryRepository: CategoryRepository,
    private val purchaseId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseFormUiState())
    val uiState: StateFlow<PurchaseFormUiState> = _uiState.asStateFlow()

    // saber si es edicion sirve para el titulo de la pantalla
    val isEditing: Boolean = purchaseId != null

    // el estado existente se conserva al editar; en creacion arranca en planificacion
    private var existingStatus: PurchaseStatus = PurchaseStatus.PLANNING

    // solo se puede ligar a una categoria de gasto
    val categories: StateFlow<List<Category>> =
        categoryRepository.observeActiveByType(TransactionType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val goals: StateFlow<List<SavingsGoal>> = savingsGoalRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // si viene un id cargamos la compra para editarla
        if (purchaseId != null) {
            viewModelScope.launch {
                repo.findById(purchaseId)?.let { purchase ->
                    existingStatus = purchase.status
                    _uiState.update {
                        it.copy(
                            name = purchase.name,
                            costText = AmountParser.formatCents(purchase.estimatedCostCents),
                            categoryId = purchase.categoryId,
                            savingsGoalId = purchase.savingsGoalId,
                            targetDate = purchase.targetDate,
                            priority = purchase.priority,
                            description = purchase.description.orEmpty(),
                            vendor = purchase.vendor.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value, errors = it.errors - PurchaseValidationError.NAME_REQUIRED)
    }

    // solo aceptamos lo que parece un monto valido mientras se escribe
    fun onCostChange(value: String) {
        if (value.isEmpty() || AmountParser.isPartialInput(value)) {
            _uiState.update {
                it.copy(
                    costText = value,
                    errors = it.errors - PurchaseValidationError.INVALID_COST - PurchaseValidationError.AMOUNT_TOO_LARGE,
                )
            }
        }
    }

    fun onCategorySelected(id: Long?) = _uiState.update { it.copy(categoryId = id) }

    // al cambiar la meta limpiamos el error de meta ya ligada
    fun onGoalSelected(id: Long?) = _uiState.update { it.copy(savingsGoalId = id, goalAlreadyLinked = false) }

    fun onTargetDateSelected(date: LocalDate?) = _uiState.update { it.copy(targetDate = date) }

    fun onPriorityChange(priority: PurchasePriority) = _uiState.update { it.copy(priority = priority) }

    fun onDescriptionChange(value: String) = _uiState.update { it.copy(description = value) }

    fun onVendorChange(value: String) = _uiState.update { it.copy(vendor = value) }

    // arma la compra desde el estado y la manda a guardar
    fun onSave() {
        val state = _uiState.value
        val purchase = PlannedPurchase(
            id = purchaseId ?: 0L,
            name = state.name,
            estimatedCostCents = AmountParser.parseToCents(state.costText) ?: 0L,
            categoryId = state.categoryId,
            savingsGoalId = state.savingsGoalId,
            targetDate = state.targetDate,
            priority = state.priority,
            status = existingStatus,
            description = state.description.ifBlank { null },
            vendor = state.vendor.ifBlank { null },
        )
        viewModelScope.launch {
            when (val result = repo.save(purchase)) {
                is PurchaseSaveResult.Success ->
                    _uiState.update { it.copy(isSaved = true, errors = emptySet(), goalAlreadyLinked = false) }
                is PurchaseSaveResult.Invalid ->
                    _uiState.update { it.copy(errors = result.errors, goalAlreadyLinked = false) }
                PurchaseSaveResult.GoalAlreadyLinked ->
                    _uiState.update { it.copy(goalAlreadyLinked = true, errors = emptySet()) }
            }
        }
    }
}
