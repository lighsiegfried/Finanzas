package com.kratt.finanzas.presentation.common

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

// duracion corta para transiciones sutiles; nunca retrasa una accion financiera
private const val SHORT_MOTION_MS = 180

// lee la preferencia del sistema de reducir o desactivar animaciones
// si el usuario desactivo las animaciones se respeta y no se anima
@Composable
fun reduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
}

// spec de animacion breve que cae en un cambio instantaneo cuando se piden menos animaciones
@Composable
fun <T> financeMotionSpec(): AnimationSpec<T> =
    if (reduceMotion()) snap() else tween(durationMillis = SHORT_MOTION_MS)
