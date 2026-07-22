package com.kratt.finanzas.domain.model

// modo de tema elegido por el usuario
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// densidad de las listas y el resumen
enum class Density { COMFORTABLE, COMPACT }

// como se muestra un reporte
enum class ReportViewMode { CHART, LIST, BOTH }

// acciones rapidas del resumen; las cinco primeras son las de por defecto
enum class QuickAction {
    ADD_EXPENSE,
    ADD_INCOME,
    TRANSFER,
    REGISTER_PAYMENT,
    VIEW_MOVEMENTS,
    ADD_ACCOUNT,
    CREATE_BUDGET,
    ADD_INSTALLMENT,
    ADD_RECURRING,
    CREATE_BACKUP,
    ;

    companion object {
        val DEFAULTS = listOf(ADD_EXPENSE, ADD_INCOME, TRANSFER, REGISTER_PAYMENT, VIEW_MOVEMENTS)
        const val MAX_SELECTED = 5
    }
}

// modulos opcionales del resumen que se pueden ocultar y reordenar
// los totales de ingresos, gastos y balance mas el selector de mes son fijos y no entran aqui
enum class DashboardModule {
    QUICK_ACTIONS,
    ACCOUNT_BALANCES,
    UPCOMING,
    BUDGET_PROGRESS,
    RECENT,
    EXPENSE_CATEGORIES,
    MONTHLY_TREND,
    CREDIT_CARD_DEBT,
    SAVINGS_BALANCE,
    SAVINGS_GOALS,
    PLANNED_PURCHASES,
    ;

    companion object {
        // orden original del resumen; el reset vuelve a este orden
        val DEFAULT_ORDER = listOf(
            QUICK_ACTIONS, ACCOUNT_BALANCES, UPCOMING, BUDGET_PROGRESS, RECENT,
            EXPENSE_CATEGORIES, MONTHLY_TREND, CREDIT_CARD_DEBT, SAVINGS_BALANCE,
            SAVINGS_GOALS, PLANNED_PURCHASES,
        )

        // modulos analiticos mas pesados; quedan ocultos de fabrica hasta que el usuario los activa
        val DEFAULT_HIDDEN = setOf(
            EXPENSE_CATEGORIES, MONTHLY_TREND, CREDIT_CARD_DEBT, SAVINGS_BALANCE,
            SAVINGS_GOALS, PLANNED_PURCHASES,
        )
    }
}
