package com.kratt.finanzas.presentation.common

import androidx.annotation.StringRes
import com.kratt.finanzas.R

// catalogo simple de iconos para las categorias, guarda una clave estable
object IconCatalog {

    data class Option(val key: String, @StringRes val label: Int)

    val options = listOf(
        Option("other", R.string.icon_general),
        Option("food", R.string.icon_food),
        Option("transport", R.string.icon_transport),
        Option("home", R.string.icon_home),
        Option("health", R.string.icon_health),
        Option("shopping", R.string.icon_shopping),
        Option("salary", R.string.icon_work),
    )

    @StringRes
    fun labelFor(key: String): Int = options.firstOrNull { it.key == key }?.label ?: R.string.icon_general
}
