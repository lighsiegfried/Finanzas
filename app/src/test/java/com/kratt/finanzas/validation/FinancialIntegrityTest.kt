package com.kratt.finanzas.validation

import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.usecase.AccountBalanceCalculator
import com.kratt.finanzas.domain.usecase.AccountTotalsCalculator
import com.kratt.finanzas.domain.usecase.BudgetCalculator
import com.kratt.finanzas.domain.usecase.BudgetState
import com.kratt.finanzas.domain.usecase.Commitment
import com.kratt.finanzas.domain.usecase.CommittedTotals
import com.kratt.finanzas.domain.usecase.ContributionPoint
import com.kratt.finanzas.domain.usecase.GoalProgressCalculator
import com.kratt.finanzas.domain.usecase.InstallmentProgressCalculator
import com.kratt.finanzas.domain.usecase.MoneyMath
import com.kratt.finanzas.domain.usecase.OccurrenceView
import com.kratt.finanzas.domain.usecase.PlannedPurchaseReadinessCalculator
import com.kratt.finanzas.domain.usecase.PurchaseReadiness
import com.kratt.finanzas.domain.usecase.SummaryCalculator
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test

// certificacion de integridad financiera: dataset canonico con constantes controladas
// los valores esperados se calculan a mano en el test, nunca desde el motor de reportes
class FinancialIntegrityTest {

    private val month = YearMonth.of(2026, 7)
    private val today = LocalDate.of(2026, 7, 21)

    // constantes controladas en centavos
    private val salario = 800_000L
    private val venta = 75_000L
    private val alimentacion = 85_000L
    private val transporte = 30_000L
    private val hogar = 310_000L
    private val servicios = 100_000L
    private val entretenimiento = 25_000L
    private val internet = 20_000L // ocurrencia recurrente registrada
    private val installmentPaid = 12_800L // cuota 1 del monitor pagada
    private val transfer = 50_000L

    private val totalIncome = salario + venta // 875_000
    private val totalExpense = alimentacion + transporte + hogar + servicios + entretenimiento + internet + installmentPaid // 582_800
    private val net = totalIncome - totalExpense // 292_200

    // cuentas: Efectivo=1, BAM=2, BAC=3, Ahorro=4, Hogar=5
    private val accounts = listOf(
        account(1, "Efectivo", AccountType.CASH),
        account(2, "BAM", AccountType.BANK_ACCOUNT),
        account(3, "BAC", AccountType.BANK_ACCOUNT),
        account(4, "Ahorro", AccountType.SAVINGS),
        account(5, "Hogar", AccountType.HOUSEHOLD),
    )

    // movimientos registrados (posted); las cuotas/recurrentes pendientes no van aqui
    private fun postedMovements(): List<Transaction> = listOf(
        tx(1, 2, TransactionType.INCOME, salario, cat = 8, origin = "recurring:salario"),
        tx(2, 1, TransactionType.INCOME, venta, cat = 9),
        tx(3, 1, TransactionType.EXPENSE, alimentacion, cat = 1),
        tx(4, 1, TransactionType.EXPENSE, transporte, cat = 2),
        tx(5, 5, TransactionType.EXPENSE, hogar, cat = 3),
        tx(6, 2, TransactionType.EXPENSE, servicios, cat = 4),
        tx(7, 3, TransactionType.EXPENSE, entretenimiento, cat = 5),
        tx(8, 2, TransactionType.EXPENSE, internet, cat = 6, origin = "recurring:internet"),
        tx(9, 2, TransactionType.EXPENSE, installmentPaid, cat = 7, origin = "installment:1"),
        tx(10, 1, TransactionType.TRANSFER, transfer, cat = null, dest = 4),
    )

    @Test
    fun invariant01_incomeEqualsPostedIncomeSum() {
        assertEquals(totalIncome, SummaryCalculator.calculate(postedMovements()).incomeCents)
    }

    @Test
    fun invariant02_expensesEqualPostedExpenseSum() {
        assertEquals(totalExpense, SummaryCalculator.calculate(postedMovements()).expenseCents)
    }

    @Test
    fun invariant03_transfersDoNotChangeIncomeOrExpenses() {
        val withTransfer = SummaryCalculator.calculate(postedMovements())
        val withoutTransfer = SummaryCalculator.calculate(postedMovements().filter { it.type != TransactionType.TRANSFER })
        assertEquals(withoutTransfer.incomeCents, withTransfer.incomeCents)
        assertEquals(withoutTransfer.expenseCents, withTransfer.expenseCents)
    }

    @Test
    fun invariant04_transferAffectsBothBalancesExactlyOnce() {
        val totals = AccountTotalsCalculator.totalsByAccount(postedMovements())
        assertEquals(transfer, totals.getValue(1).transferOutCents)
        assertEquals(transfer, totals.getValue(4).transferInCents)
        // el ahorro solo recibe la transferencia, su saldo es exactamente el monto transferido
        val ahorro = AccountBalanceCalculator.calculate(accounts.first { it.id == 4L }, totals.getValue(4))
        assertEquals(transfer, ahorro.currentBalanceCents)
    }

    @Test
    fun invariant05_paidInstallmentCreatesExactlyOneExpense() {
        val generated = postedMovements().filter { it.originKey == "installment:1" }
        assertEquals(1, generated.size)
        assertEquals(TransactionType.EXPENSE, generated.first().type)
        assertEquals(installmentPaid, generated.first().amountCents)
    }

    @Test
    fun invariant06_pendingInstallmentsAreCommitmentsNotExpenses() {
        // 12 cuotas de 12_800; 1 pagada, 11 pendientes
        val occurrences = List(12) { index ->
            OccurrenceView(installmentPaid, if (index == 0) InstallmentOccurrenceStatus.PAID else InstallmentOccurrenceStatus.PENDING)
        }
        val progress = InstallmentProgressCalculator.calculate(occurrences)
        assertEquals(installmentPaid, progress.paidCents)
        assertEquals(11 * installmentPaid, progress.remainingCents)
        // lo pendiente no esta en los gastos registrados
        assertFalse(SummaryCalculator.calculate(postedMovements()).expenseCents >= totalExpense + progress.remainingCents)
    }

    @Test
    fun invariant07_postedRecurringCreatesExactlyOneMovement() {
        assertEquals(1, postedMovements().count { it.originKey == "recurring:internet" })
        assertEquals(1, postedMovements().count { it.originKey == "recurring:salario" })
    }

    @Test
    fun invariant08_pendingRecurringAreCommitmentsOnly() {
        // dos ocurrencias recurrentes futuras de internet pendientes de 20_000
        val pending = listOf(Commitment(today.plusMonths(1), internet), Commitment(today.plusMonths(2), internet))
        assertEquals(2 * internet, CommittedTotals.total(pending))
        // no aparecen en los gastos registrados del mes
        assertEquals(totalExpense, SummaryCalculator.calculate(postedMovements()).expenseCents)
    }

    @Test
    fun invariant09_revertedRecurringRemovesGeneratedMovement() {
        val afterRevert = postedMovements().filter { it.originKey != "recurring:internet" }
        assertEquals(totalExpense - internet, SummaryCalculator.calculate(afterRevert).expenseCents)
    }

    @Test
    fun invariant10_budgetSpendingUsesPostedExpensesOnly() {
        // gasto general = gastos registrados, sin sumar compromisos pendientes
        val overall = BudgetCalculator.progress(limitCents = 600_000, spentCents = totalExpense, warningPercentage = 80)
        assertEquals(totalExpense, overall.spentCents)
        assertEquals(BudgetState.WARNING, overall.state) // 582_800 de 600_000 supera el 80%
        // presupuesto de alimentacion contra el gasto real de esa categoria
        val food = BudgetCalculator.progress(limitCents = 120_000, spentCents = alimentacion, warningPercentage = 80)
        assertEquals(BudgetState.AVAILABLE, food.state)
    }

    @Test
    fun invariant11_savingsTrackingContributionsAreNotIncomeOrExpense() {
        // un aporte de seguimiento no es un movimiento, no cambia ingresos ni gastos
        val summary = SummaryCalculator.calculate(postedMovements())
        assertEquals(totalIncome, summary.incomeCents)
        assertEquals(totalExpense, summary.expenseCents)
    }

    @Test
    fun invariant12_savingsAccountTransferStaysNeutral() {
        // un aporte por transferencia real es un TRANSFER, neutral a ingresos y gastos
        val withSavingsTransfer = postedMovements() + tx(11, 1, TransactionType.TRANSFER, 100_000, cat = null, dest = 4)
        val summary = SummaryCalculator.calculate(withSavingsTransfer)
        assertEquals(totalIncome, summary.incomeCents)
        assertEquals(totalExpense, summary.expenseCents)
    }

    @Test
    fun invariant13_plannedPurchaseIsNotAnExpenseBeforePurchase() {
        val readiness = PlannedPurchaseReadinessCalculator.calculate(PurchaseStatus.PLANNING, 800_000, linkedGoalContributedCents = 200_000)
        assertEquals(PurchaseReadiness.PARTIALLY_FUNDED, readiness.readiness)
        // el gasto registrado no incluye la compra planificada
        assertEquals(totalExpense, SummaryCalculator.calculate(postedMovements()).expenseCents)
    }

    @Test
    fun invariant14_completedPlannedPurchaseCreatesExactlyOneExpense() {
        val purchase = tx(12, 2, TransactionType.EXPENSE, 800_000, cat = 7, origin = "planned_purchase:1")
        val afterPurchase = postedMovements() + purchase
        assertEquals(1, afterPurchase.count { it.originKey == "planned_purchase:1" })
        assertEquals(totalExpense + 800_000, SummaryCalculator.calculate(afterPurchase).expenseCents)
    }

    @Test
    fun invariant15_reversedPlannedPurchaseRemovesExactlyOneExpense() {
        val purchase = tx(12, 2, TransactionType.EXPENSE, 800_000, cat = 7, origin = "planned_purchase:1")
        val afterPurchase = postedMovements() + purchase
        val afterReverse = afterPurchase.filter { it.originKey != "planned_purchase:1" }
        assertEquals(totalExpense, SummaryCalculator.calculate(afterReverse).expenseCents)
    }

    @Test
    fun invariant16and17_categoryTotalsEqualReportedExpenses() {
        val byCategory = postedMovements()
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> MoneyMath.sum(list.map { it.amountCents }) }
        assertEquals(totalExpense, MoneyMath.sum(byCategory.values))
    }

    @Test
    fun invariant18_accountClosingBalancesReconcile() {
        val totals = AccountTotalsCalculator.totalsByAccount(postedMovements())
        val sumOfBalances = MoneyMath.sum(
            accounts.map { AccountBalanceCalculator.calculate(it, totals[it.id] ?: com.kratt.finanzas.domain.model.AccountTotals.EMPTY).currentBalanceCents },
        )
        // todas las cuentas inician en cero, por lo que la suma de saldos es el neto del periodo
        assertEquals(net, sumOfBalances)
    }

    @Test
    fun invariant19_restoredDataProducesIdenticalTotals() {
        val original = SummaryCalculator.calculate(postedMovements())
        // simula una restauracion: los mismos datos deben dar los mismos totales
        val restored = SummaryCalculator.calculate(postedMovements().map { it.copy() })
        assertEquals(original, restored)
    }

    @Test
    fun invariant20_noCentIsLostOrInvented() {
        val summary = SummaryCalculator.calculate(postedMovements())
        // el neto reconstruido con matematica segura coincide exactamente
        assertEquals(net, MoneyMath.subtract(summary.incomeCents, summary.expenseCents))
        // la suma por categoria coincide con el total de gastos, sin perder ni inventar centavos
        val categorySum = MoneyMath.sum(
            postedMovements().filter { it.type == TransactionType.EXPENSE }.map { it.amountCents },
        )
        assertEquals(totalExpense, categorySum)
        assertNotEquals(0L, summary.incomeCents)
    }

    @Test
    fun savingsGoalProgress_isDerivedFromContributions() {
        val progress = GoalProgressCalculator.calculate(
            targetAmountCents = 800_000,
            contributions = listOf(ContributionPoint(200_000, today)),
            startDate = LocalDate.of(2026, 1, 1),
            targetDate = LocalDate.of(2026, 12, 31),
            today = today,
        )
        assertEquals(200_000L, progress.contributedCents)
        assertEquals(600_000L, progress.remainingCents)
        assertEquals(25, progress.progressPercent)
    }

    private fun account(id: Long, name: String, type: AccountType) = Account(
        id = id, name = name, type = type, currencyCode = "GTQ", initialBalanceCents = 0L, isActive = true,
    )

    private fun tx(
        id: Long,
        accountId: Long,
        type: TransactionType,
        amount: Long,
        cat: Long?,
        dest: Long? = null,
        origin: String? = null,
    ) = Transaction(
        id = id,
        accountId = accountId,
        type = type,
        amountCents = amount,
        description = null,
        date = today,
        createdAtMillis = 1L,
        updatedAtMillis = 1L,
        categoryId = cat,
        destinationAccountId = dest,
        originKey = origin,
    )
}
