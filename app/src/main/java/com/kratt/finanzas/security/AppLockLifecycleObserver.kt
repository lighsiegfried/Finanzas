package com.kratt.finanzas.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

// avisa al manager cuando toda la app entra o sale de segundo plano
// processlifecycleowner ya ignora los cambios de configuracion
class AppLockLifecycleObserver(
    private val appLockManager: AppLockManager,
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        appLockManager.onAppForegrounded()
    }

    override fun onStop(owner: LifecycleOwner) {
        appLockManager.onAppBackgrounded()
    }
}
