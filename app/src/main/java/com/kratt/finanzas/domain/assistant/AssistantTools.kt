package com.kratt.finanzas.domain.assistant

import com.kratt.finanzas.domain.model.AccountBalance
import com.kratt.finanzas.domain.model.IncomeExpense
import com.kratt.finanzas.domain.model.LabeledTotal
import com.kratt.finanzas.domain.model.MonthlySummary
import com.kratt.finanzas.domain.model.TransactionListItem
import com.kratt.finanzas.domain.usecase.BudgetProgress
import com.kratt.finanzas.domain.usecase.Commitment
import com.kratt.finanzas.domain.usecase.GoalProgress
import com.kratt.finanzas.domain.usecase.InstallmentProgress
import com.kratt.finanzas.domain.usecase.MetricChange
import com.kratt.finanzas.domain.usecase.PurchaseReadinessResult
import java.time.LocalDate
import java.time.YearMonth

// catalogo tipado de herramientas del asistente; cada una reutiliza el dominio existente
enum class AssistantTool {
    GET_MONTHLY_SUMMARY,
    GET_INCOME_EXPENSE_FOR_PERIOD,
    GET_EXPENSES_BY_CATEGORY,
    GET_EXPENSES_BY_ACCOUNT,
    COMPARE_PERIODS,
    GET_ACCOUNT_BALANCES,
    GET_CREDIT_CARD_DEBT,
    GET_UPCOMING_PAYMENTS,
    GET_INSTALLMENT_PROGRESS,
    GET_RECURRING_COMMITMENTS,
    GET_BUDGET_STATUS,
    GET_AVAILABLE_AFTER_COMMITMENTS,
    GET_SAVINGS_GOAL_PROGRESS,
    GET_PLANNED_PURCHASE_READINESS,
    SEARCH_TRANSACTIONS,
    DETECT_POSSIBLE_DUPLICATES,
    GET_ATTACHMENT_COUNT,
    GET_BACKUP_STATUS,
}

// tipos compartidos por los resultados de herramientas y por las respuestas
data class NamedBalance(val name: String, val balance: AccountBalance)

// linea de presupuesto; categoryName nulo es el presupuesto general del mes
data class BudgetLine(val categoryName: String?, val progress: BudgetProgress)

data class GoalStatus(val goalName: String, val targetCents: Long, val progress: GoalProgress)

data class PurchaseStatusItem(
    val purchaseName: String,
    val estimatedCostCents: Long,
    val readiness: PurchaseReadinessResult,
    val linkedGoalName: String?,
)

// estado de un plan de cuotas; finishDate es la ultima cuota pendiente si el plan sigue activo
data class InstallmentPlanStatus(
    val planName: String,
    val progress: InstallmentProgress,
    val finishDate: LocalDate?,
)

// grupo de movimientos que parecen repetidos; solo es una senal, nunca borra nada
data class DuplicateGroup(
    val amountCents: Long,
    val description: String?,
    val accountName: String,
    val dates: List<LocalDate>,
) {
    val count: Int get() = dates.size
}

// parametros tipados y validados para ejecutar una herramienta
sealed interface ToolRequest {
    val tool: AssistantTool

    data class MonthlySummary(val month: YearMonth) : ToolRequest {
        override val tool = AssistantTool.GET_MONTHLY_SUMMARY
    }

    data class IncomeExpenseForPeriod(val period: ResolvedPeriod) : ToolRequest {
        override val tool = AssistantTool.GET_INCOME_EXPENSE_FOR_PERIOD
    }

    data class ExpensesByCategory(val period: ResolvedPeriod) : ToolRequest {
        override val tool = AssistantTool.GET_EXPENSES_BY_CATEGORY
    }

    data class ExpensesByAccount(val period: ResolvedPeriod) : ToolRequest {
        override val tool = AssistantTool.GET_EXPENSES_BY_ACCOUNT
    }

    data class ComparePeriods(
        val current: ResolvedPeriod,
        val previous: ResolvedPeriod,
    ) : ToolRequest {
        override val tool = AssistantTool.COMPARE_PERIODS
    }

    object AccountBalances : ToolRequest {
        override val tool = AssistantTool.GET_ACCOUNT_BALANCES
    }

    object CreditCardDebt : ToolRequest {
        override val tool = AssistantTool.GET_CREDIT_CARD_DEBT
    }

    // rango de compromisos pendientes; por defecto hasta fin del mes actual
    data class UpcomingPayments(val start: LocalDate, val end: LocalDate, val label: PeriodLabel) : ToolRequest {
        override val tool = AssistantTool.GET_UPCOMING_PAYMENTS
    }

    object InstallmentProgress : ToolRequest {
        override val tool = AssistantTool.GET_INSTALLMENT_PROGRESS
    }

    data class RecurringCommitments(val start: LocalDate, val end: LocalDate, val label: PeriodLabel) : ToolRequest {
        override val tool = AssistantTool.GET_RECURRING_COMMITMENTS
    }

    data class BudgetStatus(val month: YearMonth) : ToolRequest {
        override val tool = AssistantTool.GET_BUDGET_STATUS
    }

    data class AvailableAfterCommitments(val month: YearMonth) : ToolRequest {
        override val tool = AssistantTool.GET_AVAILABLE_AFTER_COMMITMENTS
    }

    // goalId nulo devuelve todas las metas activas
    data class SavingsGoalProgress(val goalId: Long?) : ToolRequest {
        override val tool = AssistantTool.GET_SAVINGS_GOAL_PROGRESS
    }

    data class PlannedPurchaseReadiness(val purchaseId: Long?) : ToolRequest {
        override val tool = AssistantTool.GET_PLANNED_PURCHASE_READINESS
    }

    data class SearchTransactions(
        val queryText: String,
        val accountId: Long?,
        val categoryId: Long?,
        val period: ResolvedPeriod,
    ) : ToolRequest {
        override val tool = AssistantTool.SEARCH_TRANSACTIONS
    }

    data class DetectPossibleDuplicates(val windowDays: Int) : ToolRequest {
        override val tool = AssistantTool.DETECT_POSSIBLE_DUPLICATES
    }

    object BackupStatus : ToolRequest {
        override val tool = AssistantTool.GET_BACKUP_STATUS
    }
}

// resultados tipados; nunca exponen entidades de room, claves ni rutas de archivos
sealed interface ToolResult {
    data class MonthlySummaryResult(val summary: MonthlySummary) : ToolResult
    data class IncomeExpenseResult(val value: IncomeExpense) : ToolResult
    data class CategoryTotalsResult(val items: List<LabeledTotal>) : ToolResult
    data class AccountTotalsResult(val items: List<LabeledTotal>) : ToolResult
    data class ComparisonResult(
        val income: MetricChange,
        val expense: MetricChange,
        val net: MetricChange,
    ) : ToolResult
    data class AccountBalancesResult(val items: List<NamedBalance>) : ToolResult
    data class CreditCardDebtResult(val items: List<NamedBalance>) : ToolResult
    data class UpcomingPaymentsResult(val commitments: List<Commitment>) : ToolResult
    data class InstallmentProgressResult(val plans: List<InstallmentPlanStatus>) : ToolResult
    data class RecurringCommitmentsResult(val commitments: List<Commitment>) : ToolResult
    data class BudgetStatusResult(val overall: BudgetLine?, val categories: List<BudgetLine>) : ToolResult
    data class AvailableAfterCommitmentsResult(
        val incomeCents: Long,
        val expenseCents: Long,
        val committedCents: Long,
        val availableCents: Long,
    ) : ToolResult
    data class SavingsGoalProgressResult(val goals: List<GoalStatus>) : ToolResult
    data class PlannedPurchaseReadinessResult(val purchases: List<PurchaseStatusItem>) : ToolResult
    data class SearchResult(val items: List<TransactionListItem>) : ToolResult
    data class DuplicatesResult(val groups: List<DuplicateGroup>) : ToolResult
    data class BackupStatusResult(
        val hasBackup: Boolean,
        val lastBackupMillis: Long?,
    ) : ToolResult
    // el monto pedido o calculado excede el rango soportado
    object AmountTooLarge : ToolResult
}
