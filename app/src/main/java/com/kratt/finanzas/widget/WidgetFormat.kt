package com.kratt.finanzas.widget

import com.kratt.finanzas.common.BalanceMask

// formato de montos para los widgets; enmascara cuando no se permite mostrarlos
object WidgetFormat {
    fun amount(cents: Long, showAmounts: Boolean): String = BalanceMask.display(cents, hidden = !showAmounts)
}
