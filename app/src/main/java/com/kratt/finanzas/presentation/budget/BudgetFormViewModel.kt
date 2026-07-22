package com.kratt.finanzas.presentation.budget

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.common.AmountParser
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.BudgetValidationError
import com.kratt.finanzas.domain.usecase.BudgetValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetFormUiState(
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    val isOverall: Boolean = true,
    val categories: List<Category> = emptyList(),
    val categoryId: Long? = null,
    val limitText: String = "",
    val warningText: String = "80",
    @StringRes val amountErrorRes: Int? = null,
    @StringRes val categoryErrorRes: Int? = null,
    val isSaved: Boolean = false,
)

class BudgetFormViewModel(
    private val budgetRepository: BudgetRepository,
    categoryRepository: CategoryRepository,
    private val year: Int,
    private val month: Int,
    private val budgetId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetFormUiState(isEdit = budgetId != null))
    val uiState: StateFlow<BudgetFormUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (budgetId != null) {
                budgetRepository.findById(budgetId)?.let { budget ->
                    _uiState.update {
                        it.copy(
                            loaded = true, isOverall = budget.isOverall, categoryId = budget.categoryId,
                            limitText = AmountParser.formatCents(budget.limitAmountCents), warningText = budget.warningPercentage.toString(),
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(loaded = true) }
            }
        }
        viewModelScope.launch {
            categoryRepository.observeActiveByType(TransactionType.EXPENSE).collect { categories ->
                _uiState.update { it.copy(categories = categories) }
            }
        }
    }

    fun onScopeChange(overall: Boolean) = _uiState.update { it.copy(isOverall = overall, categoryErrorRes = null) }
    fun onCategorySelected(id: Long) = _uiState.update { it.copy(categoryId = id, categoryErrorRes = null) }

    fun onLimitChange(value: String) {
        if (AmountParser.isPartialInput(value)) _uiState.update { it.copy(limitText = value, amountErrorRes = null) }
    }

    fun onWarningChange(value: String) {
        if (value.isEmpty() || (value.all { it.isDigit() } && value.length <= 3)) _uiState.update { it.copy(warningText = value) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaved) return
        val limit = AmountParser.parseToCents(state.limitText)
        // el aviso siempre queda dentro del rango valido para no bloquear el guardado en silencio
        val warning = (state.warningText.toIntOrNull() ?: BudgetValidator.DEFAULT_WARNING)
            .coerceIn(BudgetValidator.MIN_WARNING, BudgetValidator.MAX_WARNING)
        val categoryId = if (state.isOverall) null else state.categoryId
        viewModelScope.launch {
            // en edicion el alcance no cambia, no se revisa duplicado contra si mismo
            val duplicate = !state.isEdit && budgetRepository.exists(year, month, categoryId)
            val errors = BudgetValidator.validate(limit, !state.isOverall, categoryId, warning, duplicate)
            if (errors.isNotEmpty()) {
                _uiState.update {
                    it.copy(
                        amountErrorRes = if (BudgetValidationError.INVALID_AMOUNT in errors) R.string.error_invalid_amount else null,
                        categoryErrorRes = when {
                            BudgetValidationError.MISSING_CATEGORY in errors -> R.string.error_select_category
                            BudgetValidationError.DUPLICATE in errors -> R.string.error_duplicate_budget
                            else -> null
                        },
                    )
                }
                return@launch
            }
            if (state.isEdit && budgetId != null) {
                budgetRepository.findById(budgetId)?.let { existing ->
                    budgetRepository.updateBudget(existing.copy(limitAmountCents = limit!!, warningPercentage = warning))
                }
            } else {
                budgetRepository.createBudget(year, month, categoryId, limit!!, warning)
            }
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
