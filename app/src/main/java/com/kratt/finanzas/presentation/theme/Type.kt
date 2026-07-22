package com.kratt.finanzas.presentation.theme

import androidx.compose.material3.Typography

// tipografia base de material 3; respeta la escala de fuente del sistema
val AppTypography = Typography()

// roles con nombre para mantener una jerarquia clara; usan la tipografia de material
// asi la escala de fuente del usuario siempre se aplica
object AppTextRoles {
    // saldo destacado del resumen
    val displayBalance @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.headlineMedium

    val screenTitle @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.titleLarge
    val sectionTitle @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.titleMedium
    val cardTitle @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.titleSmall

    // monto financiero en tarjetas y filas
    val amount @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.titleLarge

    val supporting @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.bodyMedium
    val label @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.labelMedium
    val caption @androidx.compose.runtime.Composable @androidx.compose.runtime.ReadOnlyComposable get() = androidx.compose.material3.MaterialTheme.typography.bodySmall
}
