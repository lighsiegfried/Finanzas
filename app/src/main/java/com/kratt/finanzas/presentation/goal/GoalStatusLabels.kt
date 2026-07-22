package com.kratt.finanzas.presentation.goal

import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.SavingsGoalStatus

// etiqueta de texto del estado de la meta; el estado nunca se muestra solo por color
// reutiliza los textos de las pestañas para no duplicar cadenas
internal fun goalStatusLabelRes(status: SavingsGoalStatus): Int = when (status) {
    SavingsGoalStatus.ACTIVE -> R.string.goal_tab_active
    SavingsGoalStatus.COMPLETED -> R.string.goal_tab_completed
    SavingsGoalStatus.PAUSED -> R.string.goal_tab_paused
    SavingsGoalStatus.ARCHIVED -> R.string.goal_tab_archived
    // no hay pestaña de canceladas, se usa una cadena propia
    SavingsGoalStatus.CANCELLED -> R.string.goal_status_cancelled
}
