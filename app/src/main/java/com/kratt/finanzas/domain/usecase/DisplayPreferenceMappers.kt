package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.Density
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.ThemeMode

// convierte los valores guardados a enums con respaldo seguro ante datos desconocidos o corruptos
object DisplayPreferenceMappers {

    fun themeMode(name: String?): ThemeMode = enumOrDefault(name, ThemeMode.SYSTEM)

    fun density(name: String?): Density = enumOrDefault(name, Density.COMFORTABLE)

    fun reportViewMode(name: String?): ReportViewMode = enumOrDefault(name, ReportViewMode.BOTH)

    // limita a cinco acciones validas y sin repetir; si no hay ninguna usa las de por defecto
    fun quickActions(names: List<String>?): List<QuickAction> {
        val parsed = names.orEmpty().mapNotNull { parse<QuickAction>(it) }.distinct().take(QuickAction.MAX_SELECTED)
        return parsed.ifEmpty { QuickAction.DEFAULTS }
    }

    // conserva el orden guardado y agrega al final los modulos que falten para no perder ninguno
    fun dashboardOrder(names: List<String>?): List<DashboardModule> {
        val parsed = names.orEmpty().mapNotNull { parse<DashboardModule>(it) }.distinct()
        if (parsed.isEmpty()) return DashboardModule.DEFAULT_ORDER
        val missing = DashboardModule.DEFAULT_ORDER.filter { it !in parsed }
        return parsed + missing
    }

    // sin valor guardado usa el conjunto oculto de fabrica; un conjunto guardado, aun vacio, respeta la eleccion del usuario
    fun hiddenModules(names: Set<String>?): Set<DashboardModule> =
        if (names == null) DashboardModule.DEFAULT_HIDDEN
        else names.mapNotNull { parse<DashboardModule>(it) }.toSet()

    private inline fun <reified T : Enum<T>> parse(name: String): T? =
        runCatching { enumValueOf<T>(name) }.getOrNull()

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        name?.let { parse<T>(it) } ?: default
}
