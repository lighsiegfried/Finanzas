package com.kratt.finanzas.domain.usecase

// reparte el monto total en cuotas usando solo centavos enteros
object InstallmentDistribution {

    // cada cuota lleva la parte entera y la ultima absorbe el remanente
    // asi la suma de todas las cuotas es exactamente el total
    fun distribute(totalCents: Long, count: Int): List<Long> {
        require(count >= 1) { "count debe ser mayor a cero" }
        val base = totalCents / count
        val amounts = MutableList(count) { base }
        // la multiplicacion segura detecta desbordes al repartir montos muy grandes
        val remainder = MoneyMath.subtract(totalCents, MoneyMath.multiply(base, count.toLong()))
        amounts[count - 1] = MoneyMath.add(base, remainder)
        return amounts
    }
}
