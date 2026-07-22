package com.kratt.finanzas.presentation.category

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.CategoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoriesUiState(
    val type: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val selectedType = MutableStateFlow(TransactionType.EXPENSE)

    // muestra todas las categorias del tipo elegido, activas e inactivas
    val uiState: StateFlow<CategoriesUiState> = selectedType
        .flatMapLatest { type ->
            categoryRepository.observeAllByType(type).map { CategoriesUiState(type, it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onTypeChange(type: TransactionType) {
        selectedType.value = type
    }

    fun onToggleActive(category: Category) {
        viewModelScope.launch {
            categoryRepository.setActive(category.id, !category.isActive)
        }
    }
}
