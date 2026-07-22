package com.kratt.finanzas.presentation.theme

import android.os.Build

// decide si el color dinamico esta disponible; solo en android 12 (api 31) o superior
object DynamicColorPolicy {
    const val MIN_SDK = Build.VERSION_CODES.S

    fun isAvailable(sdkInt: Int): Boolean = sdkInt >= MIN_SDK
}
