package com.kratt.finanzas.presentation.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.SavingsGoal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// lista todas las metas de ahorro con su total aportado para mostrar el avance
class GoalsViewModel(
    private val repo: SavingsGoalRepository,
) : ViewModel() {

    val goals: StateFlow<List<SavingsGoal>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // mapa de meta a total aportado para pintar la barra de progreso sin recalcular
    val totals: StateFlow<Map<Long, Long>> = repo.observeTotalsByGoal()
        .map { list -> list.associate { it.savingsGoalId to it.totalCents } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
