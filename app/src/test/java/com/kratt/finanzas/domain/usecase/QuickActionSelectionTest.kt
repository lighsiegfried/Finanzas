package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.QuickAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickActionSelectionTest {

    @Test
    fun toggle_adds_whenUnderLimit() {
        val result = QuickActionSelection.toggle(listOf(QuickAction.ADD_EXPENSE), QuickAction.TRANSFER)
        assertEquals(listOf(QuickAction.ADD_EXPENSE, QuickAction.TRANSFER), result)
    }

    @Test
    fun toggle_removes_whenPresent() {
        val result = QuickActionSelection.toggle(listOf(QuickAction.ADD_EXPENSE, QuickAction.TRANSFER), QuickAction.ADD_EXPENSE)
        assertEquals(listOf(QuickAction.TRANSFER), result)
    }

    @Test
    fun toggle_ignoresAdd_whenAtLimit() {
        val full = listOf(
            QuickAction.ADD_EXPENSE, QuickAction.ADD_INCOME, QuickAction.TRANSFER,
            QuickAction.REGISTER_PAYMENT, QuickAction.VIEW_MOVEMENTS,
        )
        val result = QuickActionSelection.toggle(full, QuickAction.CREATE_BACKUP)
        assertEquals(full, result)
        assertEquals(QuickAction.MAX_SELECTED, result.size)
    }

    @Test
    fun toggle_removesEvenAtLimit() {
        val full = listOf(
            QuickAction.ADD_EXPENSE, QuickAction.ADD_INCOME, QuickAction.TRANSFER,
            QuickAction.REGISTER_PAYMENT, QuickAction.VIEW_MOVEMENTS,
        )
        val result = QuickActionSelection.toggle(full, QuickAction.TRANSFER)
        assertTrue(QuickAction.TRANSFER !in result)
        assertEquals(4, result.size)
    }
}
