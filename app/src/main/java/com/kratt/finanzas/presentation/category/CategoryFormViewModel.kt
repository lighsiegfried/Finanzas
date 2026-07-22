package com.kratt.finanzas.presentation.category

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.CategoryValidationError
import com.kratt.finanzas.domain.usecase.CategoryValidator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CategoryFormUiState(
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    val name: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val iconKey: String = "other",
    val typeLocked: Boolean = false,
    val isActive: Boolean = true,
    @StringRes val nameErrorRes: Int? = null,
    @StringRes val typeNoticeRes: Int? = null,
    val isSaved: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryFormViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryId: Long?,
    initialType: TransactionType,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CategoryFormUiState(isEdit = categoryId != null, type = initialType),
    )
    val uiState: StateFlow<CategoryFormUiState> = _uiState.asStateFlow()

    private var existing: Category? = null
    private var otherActiveNames: Set<String> = emptySet()

    init {
        viewModelScope.launch {
            if (categoryId != null) {
                existing = categoryRepository.findById(categoryId)
                val hasMovements = transactionRepository.countByCategory(categoryId) > 0
                existing?.let { category ->
                    _uiState.update {
                        it.copy(
                            loaded = true,
                            name = category.name,
                            type = category.transactionType,
                            iconKey = category.iconKey,
                            isActive = category.isActive,
                            typeLocked = hasMovements,
                        )
                    }
                }
            } else {
                _uiState.update { it.copy(loaded = true) }
            }
        }
        // nombres activos del tipo elegido para detectar duplicados, sin la que se edita
        viewModelScope.launch {
            _uiState.map { it.type }.distinctUntilChanged()
                .flatMapLatest { type -> categoryRepository.observeActiveByType(type) }
                .collect { list ->
                    otherActiveNames = list.filter { it.id != categoryId }.map { it.name.lowercase() }.toSet()
                }
        }
    }

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameErrorRes = null) }
    fun onIconChange(key: String) = _uiState.update { it.copy(iconKey = key) }

    // no permite cambiar el tipo si la categoria ya tiene movimientos
    fun onTypeChange(type: TransactionType) {
        if (_uiState.value.typeLocked) {
            _uiState.update { it.copy(typeNoticeRes = R.string.error_category_type_locked) }
            return
        }
        _uiState.update { it.copy(type = type, typeNoticeRes = null) }
    }

    fun onSave() {
        val state = _uiState.value
        if (state.isSaved) return
        val errors = CategoryValidator.validate(state.name, otherActiveNames)
        if (errors.isNotEmpty()) {
            _uiState.update {
                it.copy(
                    nameErrorRes = when {
                        CategoryValidationError.EMPTY_NAME in errors -> R.string.error_empty_name
                        CategoryValidationError.DUPLICATE_NAME in errors -> R.string.error_duplicate_category
                        else -> null
                    },
                )
            }
            return
        }
        val category = Category(
            id = existing?.id ?: 0L,
            name = state.name.trim(),
            transactionType = state.type,
            iconKey = state.iconKey,
            isDefault = existing?.isDefault ?: false,
            isActive = existing?.isActive ?: true,
            createdAtMillis = existing?.createdAtMillis ?: 0L,
        )
        viewModelScope.launch {
            if (category.id == 0L) categoryRepository.insert(category) else categoryRepository.update(category)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun onToggleActive() {
        val id = existing?.id ?: return
        viewModelScope.launch {
            categoryRepository.setActive(id, !_uiState.value.isActive)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
