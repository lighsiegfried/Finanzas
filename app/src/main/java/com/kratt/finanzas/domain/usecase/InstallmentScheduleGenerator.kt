package com.kratt.finanzas.domain.usecase

import java.time.LocalDate

data class ScheduledInstallment(
    val sequenceNumber: Int,
    val dueDate: LocalDate,
    val amountCents: Long,
)

// genera las cuotas de forma determinista, con estrategia de fin de mes anclada al dia inicial
object InstallmentScheduleGenerator {

    // la cuota n vence en firstDueDate.plusMonths(n-1); plusMonths recorta al ultimo dia valido
    fun generate(firstDueDate: LocalDate, count: Int, totalCents: Long): List<ScheduledInstallment> {
        val amounts = InstallmentDistribution.distribute(totalCents, count)
        return (0 until count).map { index ->
            ScheduledInstallment(
                sequenceNumber = index + 1,
                dueDate = firstDueDate.plusMonths(index.toLong()),
                amountCents = amounts[index],
            )
        }
    }
}
