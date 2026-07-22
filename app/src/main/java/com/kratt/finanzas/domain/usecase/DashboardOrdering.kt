package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.DashboardModule

// logica pura de reordenar y ocultar modulos del resumen, probable sin android
object DashboardOrdering {

    // sube un modulo una posicion; si ya esta arriba no cambia nada
    fun moveUp(order: List<DashboardModule>, module: DashboardModule): List<DashboardModule> {
        val index = order.indexOf(module)
        if (index <= 0) return order
        return order.toMutableList().apply { this[index] = this[index - 1].also { this[index - 1] = module } }
    }

    // baja un modulo una posicion; si ya esta abajo no cambia nada
    fun moveDown(order: List<DashboardModule>, module: DashboardModule): List<DashboardModule> {
        val index = order.indexOf(module)
        if (index < 0 || index >= order.lastIndex) return order
        return order.toMutableList().apply { this[index] = this[index + 1].also { this[index + 1] = module } }
    }

    // agrega o quita un modulo del conjunto oculto
    fun toggleHidden(hidden: Set<DashboardModule>, module: DashboardModule): Set<DashboardModule> =
        if (module in hidden) hidden - module else hidden + module
}
