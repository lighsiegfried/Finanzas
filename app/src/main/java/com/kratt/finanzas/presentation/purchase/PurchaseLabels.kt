package com.kratt.finanzas.presentation.purchase

import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.usecase.PurchaseReadiness

// etiqueta de texto de la prioridad; nunca se muestra solo por color
internal fun priorityLabelRes(priority: PurchasePriority): Int = when (priority) {
    PurchasePriority.LOW -> R.string.priority_low
    PurchasePriority.MEDIUM -> R.string.priority_medium
    PurchasePriority.HIGH -> R.string.priority_high
}

// etiqueta de texto del estado de la compra
internal fun purchaseStatusLabelRes(status: PurchaseStatus): Int = when (status) {
    PurchaseStatus.PLANNING -> R.string.purchase_status_planning
    PurchaseStatus.SAVING -> R.string.purchase_status_saving
    PurchaseStatus.READY -> R.string.purchase_status_ready
    PurchaseStatus.PURCHASED -> R.string.purchase_status_purchased
    PurchaseStatus.CANCELLED -> R.string.purchase_status_cancelled
    PurchaseStatus.ARCHIVED -> R.string.purchase_status_archived
}

// etiqueta de texto del avance de la compra
internal fun readinessLabelRes(readiness: PurchaseReadiness): Int = when (readiness) {
    PurchaseReadiness.NOT_FUNDED -> R.string.purchase_not_funded
    PurchaseReadiness.PARTIALLY_FUNDED -> R.string.purchase_partial
    PurchaseReadiness.READY -> R.string.purchase_ready
    PurchaseReadiness.PURCHASED -> R.string.purchase_completed
}
