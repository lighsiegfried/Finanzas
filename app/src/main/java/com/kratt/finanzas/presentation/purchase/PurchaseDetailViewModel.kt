package com.kratt.finanzas.presentation.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.data.repository.PlannedPurchaseRepository
import com.kratt.finanzas.data.repository.RegisterPurchaseResult
import com.kratt.finanzas.data.repository.SavingsGoalRepository
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PlannedPurchase
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.usecase.PlannedPurchaseReadinessCalculator
import com.kratt.finanzas.domain.usecase.PurchaseReadiness
import com.kratt.finanzas.domain.usecase.PurchaseReadinessResult
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// estado que ve la pantalla de detalle de una compra
data class PurchaseDetailUiState(
    val purchase: PlannedPurchase? = null,
    val linkedGoal: SavingsGoal? = null,
    val readiness: PurchaseReadinessResult = PurchaseReadinessResult(PurchaseReadiness.NOT_FUNDED, 0L, 0L),
)

// resultado de un intento de registro para avisar a la pantalla
enum class RegisterOutcome { NONE, SUCCESS, ERROR }

// muestra el detalle de la compra y su avance; combina la meta ligada y sus aportes
@OptIn(ExperimentalCoroutinesApi::class)
class PurchaseDetailViewModel(
    private val repo: PlannedPurchaseRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    accountRepository: AccountRepository,
    categoryRepository: CategoryRepository,
    private val reminderPrefs: com.kratt.finanzas.data.preferences.PlanningReminderPreferences,
    private val rescheduleReminders: () -> Unit,
    private val purchaseId: Long,
) : ViewModel() {

    private val purchaseFlow = repo.observeById(purchaseId)

    // recordatorio opcional de esta compra; por defecto apagado
    val reminderEnabled: StateFlow<Boolean> = reminderPrefs.purchaseEnabled(purchaseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onToggleReminder(enabled: Boolean) = viewModelScope.launch {
        reminderPrefs.setPurchaseEnabled(purchaseId, enabled)
        rescheduleReminders()
    }

    // cuando hay meta ligada seguimos su total aportado, si no queda en cero
    val uiState: StateFlow<PurchaseDetailUiState> = purchaseFlow
        .flatMapLatest { purchase ->
            if (purchase == null) {
                flowOf(PurchaseDetailUiState())
            } else {
                val goalId = purchase.savingsGoalId
                if (goalId == null) {
                    flowOf(buildState(purchase, null, null))
                } else {
                    combine(
                        savingsGoalRepository.observeById(goalId),
                        savingsGoalRepository.observeTotalByGoal(goalId),
                    ) { goal, total -> buildState(purchase, goal, total) }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PurchaseDetailUiState())

    val accounts: StateFlow<List<Account>> = accountRepository.observeActiveAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categories: StateFlow<List<Category>> =
        categoryRepository.observeActiveByType(TransactionType.EXPENSE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _registerOutcome = MutableStateFlow(RegisterOutcome.NONE)
    val registerOutcome: StateFlow<RegisterOutcome> = _registerOutcome.asStateFlow()

    // arma el estado con el avance calculado desde la meta ligada
    private fun buildState(purchase: PlannedPurchase, goal: SavingsGoal?, goalTotal: Long?): PurchaseDetailUiState {
        val readiness = PlannedPurchaseReadinessCalculator.calculate(
            status = purchase.status,
            estimatedCostCents = purchase.estimatedCostCents,
            linkedGoalContributedCents = if (purchase.savingsGoalId != null) goalTotal ?: 0L else null,
        )
        return PurchaseDetailUiState(purchase = purchase, linkedGoal = goal, readiness = readiness)
    }

    // registra la compra creando un unico gasto real; necesita una cuenta seleccionada
    fun onRegister(finalAmountCents: Long, accountId: Long, categoryId: Long?, date: LocalDate, description: String?) {
        viewModelScope.launch {
            val result = repo.registerPurchase(purchaseId, finalAmountCents, accountId, categoryId, date, description)
            _registerOutcome.value = if (result is RegisterPurchaseResult.Success) {
                RegisterOutcome.SUCCESS
            } else {
                RegisterOutcome.ERROR
            }
        }
    }

    // revierte la compra: borra el gasto generado y vuelve a pendiente
    fun onReverse() {
        viewModelScope.launch { repo.reversePurchase(purchaseId) }
    }

    // la pantalla llama esto luego de mostrar el aviso de registro
    fun onOutcomeConsumed() {
        _registerOutcome.value = RegisterOutcome.NONE
    }
}
