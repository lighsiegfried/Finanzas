package com.kratt.finanzas.presentation.theme

import androidx.compose.ui.unit.dp

// tokens de espaciado y tamano reutilizables para mantener consistencia visual
object Dimens {
    val screenPadding = 16.dp
    val sectionSpacing = 24.dp
    val cardPadding = 16.dp
    val listSpacing = 8.dp

    // alturas de fila segun la densidad; nunca bajan del minimo accesible de toque
    val itemHeightComfortable = 72.dp
    val itemHeightCompact = 56.dp
    val minTouchTarget = 48.dp

    // rango de alto para las graficas locales
    val chartHeightMin = 180.dp
    val chartHeightMax = 240.dp

    // radios de esquina por rol
    val cornerSmall = 8.dp
    val cornerMedium = 12.dp
    val cornerLarge = 20.dp
}
