package com.kratt.finanzas.navigation

// tipo de navegacion segun el ancho de ventana; barra inferior en compacto, riel en lo demas
enum class NavLayout { BOTTOM_BAR, NAV_RAIL }

// decisiones puras de layout adaptable, probables sin android ni compose
object AdaptiveNavLayout {

    fun navLayout(compact: Boolean): NavLayout = if (compact) NavLayout.BOTTOM_BAR else NavLayout.NAV_RAIL

    // el layout de lista mas detalle en reportes solo aplica en pantallas expandidas
    fun useReportsTwoPane(expanded: Boolean): Boolean = expanded
}
