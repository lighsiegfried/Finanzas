package com.kratt.finanzas.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.compositionLocalOf

// clase de ancho de ventana actual; decide la navegacion adaptable segun el espacio, no el modelo del equipo
val LocalWindowWidthSizeClass = compositionLocalOf { WindowWidthSizeClass.Compact }
