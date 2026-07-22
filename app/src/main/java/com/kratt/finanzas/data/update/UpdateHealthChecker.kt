package com.kratt.finanzas.data.update

import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.ObserveMonthlySummaryUseCase
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first

// motivo por el que la verificacion posterior a la actualizacion no paso
enum class UpdateHealthFailure { DATABASE, ROW_COUNTS, SUMMARY }

sealed interface UpdateHealthResult {
    object Healthy : UpdateHealthResult
    data class Failed(val reason: UpdateHealthFailure) : UpdateHealthResult
}

// verificacion no destructiva tras un cambio de version; nunca registra valores financieros
// la base ya esta abierta (bootstrap listo), asi que el acceso a la clave y el esquema ya estan probados
class UpdateHealthChecker(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val observeMonthlySummary: ObserveMonthlySummaryUseCase,
    private val today: () -> LocalDate = LocalDate::now,
) {

    suspend fun check(): UpdateHealthResult {
        // lectura de prueba de la base cifrada; si falla la clave o el esquema, lanza
        try {
            accountRepository.observeActiveAccounts().first()
        } catch (error: Exception) {
            return UpdateHealthResult.Failed(UpdateHealthFailure.DATABASE)
        }
        // confirma que los conteos esenciales se pueden leer sin error
        try {
            transactionRepository.observeAllTransactions().first()
        } catch (error: Exception) {
            return UpdateHealthResult.Failed(UpdateHealthFailure.ROW_COUNTS)
        }
        // confirma que el resumen determinista se calcula sin desbordes
        try {
            observeMonthlySummary(YearMonth.from(today())).first()
        } catch (error: Exception) {
            return UpdateHealthResult.Failed(UpdateHealthFailure.SUMMARY)
        }
        return UpdateHealthResult.Healthy
    }
}
