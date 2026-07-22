package com.kratt.finanzas.domain.usecase

// operaciones de dinero con enteros que detectan desbordes en lugar de girar en silencio
object MoneyMath {

    // monto maximo soportado para un solo valor: Q9,999,999,999.99 en centavos
    const val MAX_SUPPORTED_CENTS = 999_999_999_999L

    // un solo monto guardado debe caber en el rango soportado y no ser negativo
    fun isSupportedAmount(cents: Long): Boolean = cents in 0..MAX_SUPPORTED_CENTS

    // suma segura, lanza si el resultado no cabe en Long
    fun add(a: Long, b: Long): Long = Math.addExact(a, b)

    // resta segura, lanza si el resultado no cabe en Long
    fun subtract(a: Long, b: Long): Long = Math.subtractExact(a, b)

    // multiplicacion segura, util para el reparto de cuotas
    fun multiply(a: Long, b: Long): Long = Math.multiplyExact(a, b)

    // suma una lista detectando cualquier desborde en el camino
    fun sum(values: Iterable<Long>): Long = values.fold(0L) { acc, value -> Math.addExact(acc, value) }
}
