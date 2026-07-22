package com.kratt.finanzas.presentation.installment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.domain.model.InstallmentOccurrence
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstallmentScheduleUiState(
    val installmentCount: Int = 0,
    val occurrences: List<InstallmentOccurrence> = emptyList(),
)

class InstallmentScheduleViewModel(
    private val installmentRepository: InstallmentRepository,
    planId: Long,
) : ViewModel() {

    private val count = MutableStateFlow(0)

    init {
        viewModelScope.launch { count.value = installmentRepository.findPlan(planId)?.installmentCount ?: 0 }
    }

    val uiState: StateFlow<InstallmentScheduleUiState> = combine(
        installmentRepository.observeOccurrences(planId),
        count,
    ) { occurrences, installmentCount ->
        InstallmentScheduleUiState(installmentCount = installmentCount, occurrences = occurrences)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentScheduleUiState())

    // registra el pago de la cuota, la regla evita pagar dos veces
    fun onPay(occurrenceId: Long) {
        viewModelScope.launch { installmentRepository.payOccurrence(occurrenceId) }
    }
}
