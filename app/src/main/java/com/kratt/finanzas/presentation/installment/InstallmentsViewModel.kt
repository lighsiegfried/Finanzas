package com.kratt.finanzas.presentation.installment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.domain.model.InstallmentPlan
import com.kratt.finanzas.domain.model.InstallmentStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class InstallmentsUiState(
    val isLoading: Boolean = true,
    val filter: InstallmentStatus? = null,
    val plans: List<InstallmentPlan> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class InstallmentsViewModel(
    installmentRepository: InstallmentRepository,
) : ViewModel() {

    // null representa el filtro "Todas"
    private val filter = MutableStateFlow<InstallmentStatus?>(null)

    val uiState: StateFlow<InstallmentsUiState> = combine(
        installmentRepository.observePlans(),
        filter,
    ) { plans, status ->
        InstallmentsUiState(
            isLoading = false,
            filter = status,
            plans = if (status == null) plans else plans.filter { it.status == status },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentsUiState())

    fun onFilter(status: InstallmentStatus?) { filter.value = status }
}
