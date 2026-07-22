package com.kratt.finanzas.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.kratt.finanzas.common.BalanceMask

// indica si los saldos deben ocultarse en toda la app; se activa por preferencia o al bloquear
val LocalBalancesHidden = staticCompositionLocalOf { false }

// mascara consistente que se muestra en lugar del monto real
const val AMOUNT_MASK = BalanceMask.MASK

// devuelve el monto formateado o la mascara segun la privacidad activa
// al mostrar la mascara el monto real nunca entra al arbol ni a la semantica accesible
@Composable
@ReadOnlyComposable
fun maskedAmount(cents: Long): String = BalanceMask.display(cents, LocalBalancesHidden.current)

// version sin composable para usar dentro de lambdas normales (map, joinToString)
// el llamador lee LocalBalancesHidden en contexto composable y pasa el estado aqui
fun maskAmount(cents: Long, hidden: Boolean): String = BalanceMask.display(cents, hidden)
