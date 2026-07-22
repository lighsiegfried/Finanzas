package com.kratt.finanzas.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// colores semanticos de finanzas; el color siempre acompana a un texto o icono, nunca es la unica senal
data class FinanceColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    val available: Color,
    val warning: Color,
    val exceeded: Color,
    val pending: Color,
    val overdue: Color,
    val completed: Color,
    val neutral: Color,
)

// variante clara con buen contraste sobre fondos claros
val LightFinanceColors = FinanceColors(
    income = Color(0xFF1B7F4B),
    expense = Color(0xFFB3261E),
    transfer = Color(0xFF1E6B70),
    available = Color(0xFF1B7F4B),
    warning = Color(0xFF8A5A00),
    exceeded = Color(0xFFB3261E),
    pending = Color(0xFF6A5300),
    overdue = Color(0xFFB3261E),
    completed = Color(0xFF1B7F4B),
    neutral = Color(0xFF4E6357),
)

// variante oscura con buen contraste sobre fondos oscuros
val DarkFinanceColors = FinanceColors(
    income = Color(0xFF7FD8A6),
    expense = Color(0xFFF2B8B5),
    transfer = Color(0xFF7FD1D6),
    available = Color(0xFF7FD8A6),
    warning = Color(0xFFF2C879),
    exceeded = Color(0xFFF2B8B5),
    pending = Color(0xFFE6CF7A),
    overdue = Color(0xFFF2B8B5),
    completed = Color(0xFF7FD8A6),
    neutral = Color(0xFFB6CCC0),
)

// acceso a los colores semanticos desde cualquier pantalla via el tema
val LocalFinanceColors = staticCompositionLocalOf { LightFinanceColors }
