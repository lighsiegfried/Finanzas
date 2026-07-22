package com.kratt.finanzas.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kratt.finanzas.domain.model.DashboardModule
import com.kratt.finanzas.domain.model.Density
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.ThemeMode
import com.kratt.finanzas.domain.usecase.DisplayPreferenceMappers
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// preferencias visuales no sensibles: tema, densidad, privacidad de saldos, resumen, acciones rapidas
data class DisplaySettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val density: Density = Density.COMFORTABLE,
    val balancesHidden: Boolean = false,
    val reportViewMode: ReportViewMode = ReportViewMode.BOTH,
    val hapticsEnabled: Boolean = true,
    val quickActions: List<QuickAction> = QuickAction.DEFAULTS,
    val dashboardOrder: List<DashboardModule> = DashboardModule.DEFAULT_ORDER,
    val hiddenModules: Set<DashboardModule> = DashboardModule.DEFAULT_HIDDEN,
)

private val Context.displayDataStore: DataStore<Preferences> by preferencesDataStore(name = "display_preferences")

class DisplayPreferences(context: Context) {

    private val dataStore = context.applicationContext.displayDataStore

    val settings: Flow<DisplaySettings> = dataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { prefs ->
            DisplaySettings(
                themeMode = DisplayPreferenceMappers.themeMode(prefs[THEME_MODE]),
                dynamicColor = prefs[DYNAMIC_COLOR] ?: false,
                density = DisplayPreferenceMappers.density(prefs[DENSITY]),
                balancesHidden = prefs[BALANCES_HIDDEN] ?: false,
                reportViewMode = DisplayPreferenceMappers.reportViewMode(prefs[REPORT_VIEW]),
                hapticsEnabled = prefs[HAPTICS] ?: true,
                quickActions = DisplayPreferenceMappers.quickActions(prefs[QUICK_ACTIONS]?.split(SEP)),
                dashboardOrder = DisplayPreferenceMappers.dashboardOrder(prefs[DASHBOARD_ORDER]?.split(SEP)),
                hiddenModules = DisplayPreferenceMappers.hiddenModules(prefs[HIDDEN_MODULES]),
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) = dataStore.edit { it[THEME_MODE] = mode.name }
    suspend fun setDynamicColor(enabled: Boolean) = dataStore.edit { it[DYNAMIC_COLOR] = enabled }
    suspend fun setDensity(density: Density) = dataStore.edit { it[DENSITY] = density.name }
    suspend fun setBalancesHidden(hidden: Boolean) = dataStore.edit { it[BALANCES_HIDDEN] = hidden }
    suspend fun setReportViewMode(mode: ReportViewMode) = dataStore.edit { it[REPORT_VIEW] = mode.name }
    suspend fun setHapticsEnabled(enabled: Boolean) = dataStore.edit { it[HAPTICS] = enabled }

    // guarda maximo cinco acciones, ya validadas por el mapeador al leer
    suspend fun setQuickActions(actions: List<QuickAction>) = dataStore.edit {
        it[QUICK_ACTIONS] = actions.distinct().take(QuickAction.MAX_SELECTED).joinToString(SEP) { a -> a.name }
    }

    suspend fun setDashboardOrder(order: List<DashboardModule>) = dataStore.edit {
        it[DASHBOARD_ORDER] = order.distinct().joinToString(SEP) { m -> m.name }
    }

    suspend fun setHiddenModules(hidden: Set<DashboardModule>) = dataStore.edit {
        it[HIDDEN_MODULES] = hidden.map { m -> m.name }.toSet()
    }

    // restablece el diseno del resumen al orden original y muestra todos los modulos
    suspend fun resetDashboard() = dataStore.edit {
        it.remove(DASHBOARD_ORDER)
        it.remove(HIDDEN_MODULES)
    }

    private companion object {
        const val SEP = ","
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val DENSITY = stringPreferencesKey("density")
        val BALANCES_HIDDEN = booleanPreferencesKey("balances_hidden")
        val REPORT_VIEW = stringPreferencesKey("report_view")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val QUICK_ACTIONS = stringPreferencesKey("quick_actions")
        val DASHBOARD_ORDER = stringPreferencesKey("dashboard_order")
        val HIDDEN_MODULES = stringSetPreferencesKey("hidden_modules")
    }
}
