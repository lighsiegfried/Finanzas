package com.kratt.finanzas.data.assistant

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

// detecta si el dispositivo podria soportar generacion local; nunca descarga ni contacta la red
class DeviceCapabilityDetector(context: Context) {

    private val packageManager = context.applicationContext.packageManager

    // el modo avanzado del sistema requiere android 14 o mayor y el servicio aicore instalado
    // en el emulador api 36 y en el redmi android 11 esto es falso, asi que se usa el modo compatible
    fun isGenerativeSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return isPackageInstalled(AICORE_PACKAGE)
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        packageManager.getPackageInfo(packageName, 0)
        true
    } catch (error: PackageManager.NameNotFoundException) {
        false
    }

    private companion object {
        // servicio del sistema para generacion local; solo presente en pocos dispositivos recientes
        const val AICORE_PACKAGE = "com.google.android.aicore"
    }
}
