package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.MonthlySummary
import com.kratt.finanzas.domain.repository.TransactionRepository
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveMonthlySummaryUseCase(
    private val transactionRepository: TransactionRepository,
) {

    // entrega los totales del mes y se actualiza solo cuando cambian los datos
    operator fun invoke(month: YearMonth): Flow<MonthlySummary> =
        transactionRepository.observeMonthly(month).map(SummaryCalculator::calculate)
}
