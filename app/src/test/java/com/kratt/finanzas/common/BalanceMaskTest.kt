package com.kratt.finanzas.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BalanceMaskTest {

    @Test
    fun display_showsFormattedAmount_whenNotHidden() {
        assertEquals(CurrencyFormatter.format(12_575), BalanceMask.display(12_575, hidden = false))
    }

    @Test
    fun display_showsMask_whenHidden_andRealValueIsAbsent() {
        val masked = BalanceMask.display(12_575, hidden = true)
        assertEquals(BalanceMask.MASK, masked)
        // el valor real no puede aparecer dentro de la mascara
        assertNotEquals(CurrencyFormatter.format(12_575), masked)
    }
}
