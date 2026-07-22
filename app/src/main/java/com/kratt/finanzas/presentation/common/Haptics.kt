package com.kratt.finanzas.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

// indica si el usuario dejo activa la vibracion breve al confirmar; se controla en apariencia
val LocalHapticsEnabled = staticCompositionLocalOf { true }

// vibracion breve del sistema; usa la api de compose y no necesita el permiso VIBRATE
class FinanceHaptics(
    private val haptic: HapticFeedback,
    private val enabled: Boolean,
) {
    // confirmacion de una accion exitosa como guardar o registrar un pago
    fun success() {
        if (enabled) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // aviso corto para una accion invalida
    fun reject() {
        if (enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

@Composable
fun rememberFinanceHaptics(): FinanceHaptics {
    val haptic = LocalHapticFeedback.current
    val enabled = LocalHapticsEnabled.current
    return remember(haptic, enabled) { FinanceHaptics(haptic, enabled) }
}
