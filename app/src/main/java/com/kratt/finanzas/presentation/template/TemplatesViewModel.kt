package com.kratt.finanzas.presentation.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.TransactionTemplateRepository
import com.kratt.finanzas.domain.model.TransactionTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// muestra las plantillas por favoritas, recientes y todas
class TemplatesViewModel(
    private val repo: TransactionTemplateRepository,
) : ViewModel() {

    val favorites: StateFlow<List<TransactionTemplate>> = repo.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recent: StateFlow<List<TransactionTemplate>> = repo.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val all: StateFlow<List<TransactionTemplate>> = repo.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // cambia el estado de favorita de la plantilla
    fun onToggleFavorite(template: TransactionTemplate) = viewModelScope.launch {
        repo.setFavorite(template.id, !template.isFavorite)
    }

    // desactiva la plantilla sin borrarla
    fun onDeactivate(id: Long) = viewModelScope.launch {
        repo.setActive(id, false)
    }
}
