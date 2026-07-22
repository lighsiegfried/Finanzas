package com.kratt.finanzas.presentation.goal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.SavingsContributionRepository
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.SavingsContribution
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.usecase.ContributionPoint
import com.kratt.finanzas.domain.usecase.GoalProgress
import com.kratt.finanzas.domain.usecase.GoalProgressCalculator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// estado de la pantalla de detalle: la meta, sus aportes y el avance calculado
data class GoalDetailUiState(
    val goal: SavingsGoal? = null,
    val contributions: List<SavingsContribution> = emptyList(),
    val progress: GoalProgress? = null,
)

// muestra el avance de una meta y su historial de aportes
class GoalDetailViewModel(
    private val goalRepo: SavingsGoalRepository,
    private val contributionRepo: SavingsContributionRepository,
    private val reminderPrefs: com.kratt.finanzas.data.preferences.PlanningReminderPreferences,
    private val rescheduleReminders: () -> Unit,
    private val goalId: Long,
) : ViewModel() {

    // recordatorio opcional de esta meta; por defecto apagado
    val reminderEnabled: StateFlow<Boolean> = reminderPrefs.goalEnabled(goalId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onToggleReminder(enabled: Boolean) = viewModelScope.launch {
        reminderPrefs.setGoalEnabled(goalId, enabled)
        rescheduleReminders()
    }

    val uiState: StateFlow<GoalDetailUiState> = combine(
        goalRepo.observeById(goalId),
        contributionRepo.observeByGoal(goalId),
    ) { goal, contributions ->
        // el avance se calcula siempre desde los aportes, nunca desde la ui
        val progress = goal?.let {
            GoalProgressCalculator.calculate(
                targetAmountCents = it.targetAmountCents,
                contributions = contributions.map { c -> ContributionPoint(c.amountCents, c.contributionDate) },
                startDate = it.startDate,
                targetDate = it.targetDate,
            )
        }
        GoalDetailUiState(goal = goal, contributions = contributions, progress = progress)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalDetailUiState())

    fun onComplete() = viewModelScope.launch {
        goalRepo.setStatus(goalId, SavingsGoalStatus.COMPLETED)
    }

    // alterna entre pausada y activa segun el estado actual
    fun onPauseResume() = viewModelScope.launch {
        val current = uiState.value.goal?.status ?: return@launch
        val next = if (current == SavingsGoalStatus.PAUSED) SavingsGoalStatus.ACTIVE else SavingsGoalStatus.PAUSED
        goalRepo.setStatus(goalId, next)
    }

    fun onArchive() = viewModelScope.launch {
        goalRepo.setArchived(goalId, true)
    }

    // revierte un aporte y su transferencia asociada si la tenia
    fun onRevert(contributionId: Long) = viewModelScope.launch {
        contributionRepo.revert(contributionId)
    }
}
