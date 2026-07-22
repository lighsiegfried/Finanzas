package com.kratt.finanzas.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.PlannedPurchaseRepository
import com.kratt.finanzas.domain.model.PlannedPurchase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// muestra la lista de compras planificadas
class PurchasesViewModel(
    repo: PlannedPurchaseRepository,
) : ViewModel() {

    val purchases: StateFlow<List<PlannedPurchase>> = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
