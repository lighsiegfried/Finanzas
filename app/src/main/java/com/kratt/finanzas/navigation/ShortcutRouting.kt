package com.kratt.finanzas.navigation

import com.kratt.finanzas.domain.model.TransactionType

// acciones de los accesos directos; el destino se resuelve dentro de la app ya desbloqueada
object ShortcutRouting {
    const val ACTION_ADD_EXPENSE = "com.kratt.finanzas.shortcut.ADD_EXPENSE"
    const val ACTION_ADD_INCOME = "com.kratt.finanzas.shortcut.ADD_INCOME"
    const val ACTION_TRANSFER = "com.kratt.finanzas.shortcut.TRANSFER"
    const val ACTION_UPCOMING = "com.kratt.finanzas.shortcut.UPCOMING"
    const val ACTION_GOALS = "com.kratt.finanzas.shortcut.GOALS"

    // acciones de los widgets que abren pantallas concretas
    const val ACTION_OPEN_SUMMARY = "com.kratt.finanzas.widget.OPEN_SUMMARY"
    const val ACTION_OPEN_EXPENSES = "com.kratt.finanzas.widget.OPEN_EXPENSES"
    const val ACTION_OPEN_BUDGETS = "com.kratt.finanzas.widget.OPEN_BUDGETS"

    // convierte la accion del acceso directo o widget en una ruta de navegacion; null si no aplica
    fun routeForAction(action: String?): String? = when (action) {
        ACTION_ADD_EXPENSE -> Destinations.addTransaction(TransactionType.EXPENSE)
        ACTION_ADD_INCOME -> Destinations.addTransaction(TransactionType.INCOME)
        ACTION_TRANSFER -> Destinations.ADD_TRANSFER
        ACTION_UPCOMING -> Destinations.INSTALLMENTS
        ACTION_GOALS -> Destinations.SAVINGS_GOALS
        ACTION_OPEN_SUMMARY -> Destinations.SUMMARY
        ACTION_OPEN_EXPENSES -> Destinations.transactionsFiltered(null, null, TransactionType.EXPENSE)
        ACTION_OPEN_BUDGETS -> Destinations.BUDGETS
        else -> null
    }
}
