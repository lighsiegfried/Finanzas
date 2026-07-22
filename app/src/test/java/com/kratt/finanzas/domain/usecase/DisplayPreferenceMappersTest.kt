package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.Density
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayPreferenceMappersTest {

    @Test
    fun themeMode_valid_unknown_null() {
        assertEquals(ThemeMode.DARK, DisplayPreferenceMappers.themeMode("DARK"))
        assertEquals(ThemeMode.SYSTEM, DisplayPreferenceMappers.themeMode("PURPLE"))
        assertEquals(ThemeMode.SYSTEM, DisplayPreferenceMappers.themeMode(null))
    }

    @Test
    fun density_valid_and_fallback() {
        assertEquals(Density.COMPACT, DisplayPreferenceMappers.density("COMPACT"))
        assertEquals(Density.COMFORTABLE, DisplayPreferenceMappers.density("weird"))
    }

    @Test
    fun reportViewMode_valid_and_fallback() {
        assertEquals(ReportViewMode.LIST, DisplayPreferenceMappers.reportViewMode("LIST"))
        assertEquals(ReportViewMode.BOTH, DisplayPreferenceMappers.reportViewMode(null))
    }

    @Test
    fun quickActions_limitedToFive_deduped_unknownFiltered() {
        val names = listOf("ADD_EXPENSE", "ADD_INCOME", "TRANSFER", "REGISTER_PAYMENT", "VIEW_MOVEMENTS", "ADD_ACCOUNT", "BOGUS")
        val result = DisplayPreferenceMappers.quickActions(names)
        assertEquals(5, result.size)
        assertEquals(QuickAction.ADD_EXPENSE, result.first())
    }

    @Test
    fun quickActions_emptyOrAllUnknown_fallsBackToDefaults() {
        assertEquals(QuickAction.DEFAULTS, DisplayPreferenceMappers.quickActions(emptyList()))
        assertEquals(QuickAction.DEFAULTS, DisplayPreferenceMappers.quickActions(listOf("X", "Y")))
        assertEquals(QuickAction.DEFAULTS, DisplayPreferenceMappers.quickActions(null))
    }

    @Test
    fun dashboardOrder_keepsOrder_appendsMissing_filtersUnknown() {
        val result = DisplayPreferenceMappers.dashboardOrder(listOf("RECENT", "NOPE"))
        // RECENT primero (respeta lo guardado), luego los que faltan en su orden por defecto
        assertEquals(DashboardModule.RECENT, result.first())
        assertEquals(DashboardModule.DEFAULT_ORDER.size, result.size)
        assertEquals(DashboardModule.DEFAULT_ORDER.toSet(), result.toSet())
    }

    @Test
    fun dashboardOrder_emptyFallsBackToDefault() {
        assertEquals(DashboardModule.DEFAULT_ORDER, DisplayPreferenceMappers.dashboardOrder(null))
        assertEquals(DashboardModule.DEFAULT_ORDER, DisplayPreferenceMappers.dashboardOrder(emptyList()))
    }

    @Test
    fun hiddenModules_parsesKnown_filtersUnknown() {
        assertEquals(setOf(DashboardModule.UPCOMING), DisplayPreferenceMappers.hiddenModules(setOf("UPCOMING", "GHOST")))
    }

    @Test
    fun hiddenModules_nullFallsBackToDefaultHidden_emptySetIsRespected() {
        // sin valor guardado se usa el conjunto oculto de fabrica
        assertEquals(DashboardModule.DEFAULT_HIDDEN, DisplayPreferenceMappers.hiddenModules(null))
        // un conjunto guardado vacio significa que el usuario mostro todo
        assertEquals(emptySet<DashboardModule>(), DisplayPreferenceMappers.hiddenModules(emptySet()))
    }
}
