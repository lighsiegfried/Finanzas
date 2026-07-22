package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.QuickAction

// logica pura de elegir acciones rapidas respetando el maximo permitido
object QuickActionSelection {

    // agrega o quita una accion; nunca pasa del maximo
    fun toggle(
        current: List<QuickAction>,
        action: QuickAction,
        max: Int = QuickAction.MAX_SELECTED,
    ): List<QuickAction> = when {
        action in current -> current - action
        current.size < max -> current + action
        else -> current
    }
}
