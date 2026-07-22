package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.DashboardModule
import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardOrderingTest {

    private val order = listOf(
        DashboardModule.QUICK_ACTIONS,
        DashboardModule.ACCOUNT_BALANCES,
        DashboardModule.RECENT,
    )

    @Test
    fun moveUp_swapsWithPrevious() {
        val result = DashboardOrdering.moveUp(order, DashboardModule.ACCOUNT_BALANCES)
        assertEquals(
            listOf(DashboardModule.ACCOUNT_BALANCES, DashboardModule.QUICK_ACTIONS, DashboardModule.RECENT),
            result,
        )
    }

    @Test
    fun moveUp_atTop_isNoOp() {
        assertEquals(order, DashboardOrdering.moveUp(order, DashboardModule.QUICK_ACTIONS))
    }

    @Test
    fun moveDown_swapsWithNext() {
        val result = DashboardOrdering.moveDown(order, DashboardModule.ACCOUNT_BALANCES)
        assertEquals(
            listOf(DashboardModule.QUICK_ACTIONS, DashboardModule.RECENT, DashboardModule.ACCOUNT_BALANCES),
            result,
        )
    }

    @Test
    fun moveDown_atBottom_isNoOp() {
        assertEquals(order, DashboardOrdering.moveDown(order, DashboardModule.RECENT))
    }

    @Test
    fun toggleHidden_addsThenRemoves() {
        val once = DashboardOrdering.toggleHidden(emptySet(), DashboardModule.RECENT)
        assertEquals(setOf(DashboardModule.RECENT), once)
        val twice = DashboardOrdering.toggleHidden(once, DashboardModule.RECENT)
        assertEquals(emptySet<DashboardModule>(), twice)
    }
}
