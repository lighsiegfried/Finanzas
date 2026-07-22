package com.kratt.finanzas.navigation

import com.kratt.finanzas.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutRoutingTest {

    @Test
    fun addExpenseAndIncome_preselectType() {
        assertEquals(Destinations.addTransaction(TransactionType.EXPENSE), ShortcutRouting.routeForAction(ShortcutRouting.ACTION_ADD_EXPENSE))
        assertEquals(Destinations.addTransaction(TransactionType.INCOME), ShortcutRouting.routeForAction(ShortcutRouting.ACTION_ADD_INCOME))
    }

    @Test
    fun transferUpcomingGoals() {
        assertEquals(Destinations.ADD_TRANSFER, ShortcutRouting.routeForAction(ShortcutRouting.ACTION_TRANSFER))
        assertEquals(Destinations.INSTALLMENTS, ShortcutRouting.routeForAction(ShortcutRouting.ACTION_UPCOMING))
        assertEquals(Destinations.SAVINGS_GOALS, ShortcutRouting.routeForAction(ShortcutRouting.ACTION_GOALS))
    }

    @Test
    fun widgetSummaryActions() {
        assertEquals(Destinations.SUMMARY, ShortcutRouting.routeForAction(ShortcutRouting.ACTION_OPEN_SUMMARY))
        assertEquals(Destinations.BUDGETS, ShortcutRouting.routeForAction(ShortcutRouting.ACTION_OPEN_BUDGETS))
        assertEquals(
            Destinations.transactionsFiltered(null, null, TransactionType.EXPENSE),
            ShortcutRouting.routeForAction(ShortcutRouting.ACTION_OPEN_EXPENSES),
        )
    }

    @Test
    fun unknownAction_isNull() {
        assertNull(ShortcutRouting.routeForAction("com.kratt.finanzas.UNKNOWN"))
        assertNull(ShortcutRouting.routeForAction(null))
    }
}
