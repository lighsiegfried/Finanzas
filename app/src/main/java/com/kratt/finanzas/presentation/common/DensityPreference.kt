package com.kratt.finanzas.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.domain.model.Density as DisplayDensity

// densidad de listas y tarjetas elegida por el usuario; llega desde las preferencias visuales
val LocalListDensity = staticCompositionLocalOf { DisplayDensity.COMFORTABLE }

// escala de fuente a partir de la cual se ignora el modo compacto para no apretar el texto
private const val LARGE_FONT_SCALE = 1.3f

// dice si aplicar el modo compacto; la escala de fuente grande siempre gana sobre lo compacto
@Composable
@ReadOnlyComposable
fun isCompactDensity(): Boolean =
    LocalListDensity.current == DisplayDensity.COMPACT && LocalDensity.current.fontScale < LARGE_FONT_SCALE

// separacion vertical de una fila de lista; compacto reduce solo el espacio decorativo
@Composable
@ReadOnlyComposable
fun listItemVerticalPadding(): Dp = if (isCompactDensity()) 4.dp else 10.dp

// separacion entre elementos de una lista o columna
@Composable
@ReadOnlyComposable
fun listItemSpacing(): Dp = if (isCompactDensity()) 4.dp else 8.dp

// relleno interno de una tarjeta
@Composable
@ReadOnlyComposable
fun cardContentPadding(): Dp = if (isCompactDensity()) 12.dp else 16.dp
