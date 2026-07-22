package com.kratt.finanzas.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class InstallmentDistributionTest {

    @Test
    fun exactDivisionAllEqual() {
        // Q1,536.00 en 12 cuotas = 12800 cada una, exacto
        val amounts = InstallmentDistribution.distribute(153_600, 12)
        assertEquals(12, amounts.size)
        assertEquals(List(12) { 12_800L }, amounts)
        assertEquals(153_600L, amounts.sum())
    }

    @Test
    fun remainderGoesToLastInstallment() {
        val amounts = InstallmentDistribution.distribute(1_000, 3)
        assertEquals(listOf(333L, 333L, 334L), amounts)
        assertEquals(1_000L, amounts.sum())
    }

    @Test
    fun sumAlwaysEqualsTotal() {
        for (total in listOf(1L, 99L, 100_001L, 7L)) {
            for (count in 2..9) {
                assertEquals(total, InstallmentDistribution.distribute(total, count).sum())
            }
        }
    }
}
