package com.kratt.finanzas.common

// enmascara los saldos sin depender de compose, para poder probar la logica en la jvm
object BalanceMask {
    // mascara consistente que se muestra en lugar del monto real
    const val MASK = "Q ••••••"

    // devuelve el monto formateado o la mascara segun la privacidad activa
    fun display(cents: Long, hidden: Boolean): String = if (hidden) MASK else CurrencyFormatter.format(cents)
}
