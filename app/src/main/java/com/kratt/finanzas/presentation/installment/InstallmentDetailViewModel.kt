package com.kratt.finanzas.presentation.installment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.domain.model.InstallmentOccurrence
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.InstallmentPlan
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.InstallmentProgress
import com.kratt.finanzas.domain.usecase.InstallmentProgressCalculator
import com.kratt.finanzas.domain.usecase.OccurrenceView
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InstallmentDetailUiState(
    val plan: InstallmentPlan? = null,
    val occurrences: List<InstallmentOccurrence> = emptyList(),
    val progress: InstallmentProgress? = null,
    val nextDue: LocalDate? = null,
    val completion: LocalDate? = null,
    val accountName: String = "",
    val categoryName: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class)
class InstallmentDetailViewModel(
    private val installmentRepository: InstallmentRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val planId: Long,
) : ViewModel() {

    private val names = MutableStateFlow("" to "")

    init {
        viewModelScope.launch {
            val plan = installmentRepository.findPlan(planId) ?: return@launch
            val account = accountRepository.findById(plan.accountId)?.name.orEmpty()
            val category = categoryRepository.findById(plan.categoryId)?.name.orEmpty()
            names.value = account to category
        }
    }

    val uiState: StateFlow<InstallmentDetailUiState> = combine(
        installmentRepository.observePlans().map { list -> list.firstOrNull { it.id == planId } },
        installmentRepository.observeOccurrences(planId),
        names,
    ) { plan, occurrences, (accountName, categoryName) ->
        val progress = InstallmentProgressCalculator.calculate(
            occurrences.map { OccurrenceView(it.amountCents, it.status) },
        )
        InstallmentDetailUiState(
            plan = plan,
            occurrences = occurrences,
            progress = progress,
            nextDue = occurrences
                .filter { it.status == InstallmentOccurrenceStatus.PENDING || it.status == InstallmentOccurrenceStatus.OVERDUE }
                .minByOrNull { it.dueDate }?.dueDate,
            completion = occurrences.maxByOrNull { it.dueDate }?.dueDate,
            accountName = accountName,
            categoryName = categoryName,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InstallmentDetailUiState())

    fun onPauseResume() {
        val plan = uiState.value.plan ?: return
        val target = if (plan.status == InstallmentStatus.PAUSED) InstallmentStatus.ACTIVE else InstallmentStatus.PAUSED
        viewModelScope.launch { installmentRepository.setStatus(plan.id, target) }
    }

    fun onCancelPlan() {
        val plan = uiState.value.plan ?: return
        viewModelScope.launch { installmentRepository.setStatus(plan.id, InstallmentStatus.CANCELLED) }
    }
}
