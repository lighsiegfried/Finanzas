package com.kratt.finanzas.domain.model

import java.time.LocalDate

// un movimiento de dinero, el monto siempre va en centavos positivos
// en una transferencia categoryId es nulo y destinationAccountId indica el destino
data class Transaction(
    val id: Long,
    val accountId: Long,
    val type: TransactionType,
    val amountCents: Long,
    val description: String?,
    val date: LocalDate,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
    val categoryId: Long? = null,
    val destinationAccountId: Long? = null,
    // marca de origen para movimientos generados por cuotas o recurrentes
    val originKey: String? = null,
) {
    // un movimiento generado no se borra por el flujo normal, solo por su reverso
    val isGenerated: Boolean get() = originKey != null
}
