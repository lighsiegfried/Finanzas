package com.kratt.finanzas.security

import android.view.Window
import android.view.WindowManager

object ScreenProtection {

    // en release se bloquean capturas, grabacion y la vista previa de recientes
    // en debug se permiten capturas para la evidencia automatizada
    fun shouldApplySecureFlag(isDebugBuild: Boolean): Boolean = !isDebugBuild

    fun applyTo(window: Window, isDebugBuild: Boolean) {
        if (shouldApplySecureFlag(isDebugBuild)) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE,
            )
        }
    }
}
