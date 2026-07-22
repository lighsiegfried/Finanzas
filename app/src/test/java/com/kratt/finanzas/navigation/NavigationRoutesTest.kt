package com.kratt.finanzas.navigation

import com.kratt.finanzas.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRoutesTest {

    @Test
    fun navLayout_isBottomBar_whenCompact() {
        assertEquals(NavLayout.BOTTOM_BAR, AdaptiveNavLayout.navLayout(compact = true))
    }

    @Test
    fun navLayout_isRail_whenNotCompact() {
        assertEquals(NavLayout.NAV_RAIL, AdaptiveNavLayout.navLayout(compact = false))
    }

    @Test
    fun reportsTwoPane_onlyWhenExpanded() {
        assertTrue(AdaptiveNavLayout.useReportsTwoPane(expanded = true))
        assertFalse(AdaptiveNavLayout.useReportsTwoPane(expanded = false))
    }

    @Test
    fun addTransaction_includesPreselectedType() {
        assertEquals("add_transaction?type=EXPENSE", Destinations.addTransaction(TransactionType.EXPENSE))
        assertEquals("add_transaction?type=INCOME", Destinations.addTransaction(TransactionType.INCOME))
    }

    @Test
    fun transactionsFiltered_buildsQueryFromPresentFilters() {
        val route = Destinations.transactionsFiltered(accountId = 7L, categoryId = null, type = TransactionType.EXPENSE)
        assertEquals("transactions_filtered?accountId=7&type=EXPENSE", route)
    }

    @Test
    fun transactionsFiltered_categoryFilter() {
        val route = Destinations.transactionsFiltered(accountId = null, categoryId = 3L, type = TransactionType.EXPENSE)
        assertEquals("transactions_filtered?categoryId=3&type=EXPENSE", route)
    }

    @Test
    fun transactionsFiltered_isBareRoute_whenNoFilters() {
        assertEquals("transactions_filtered", Destinations.transactionsFiltered(null, null, null))
    }
}
