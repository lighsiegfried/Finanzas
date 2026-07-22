package com.kratt.finanzas

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.kratt.finanzas.di.AppContainer
import com.kratt.finanzas.security.AppLockLifecycleObserver

class FinanzasApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // vigila cuando la app completa entra o sale de segundo plano
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            AppLockLifecycleObserver(container.appLockManager),
        )
        // arranca la base cifrada fuera del hilo principal
        container.startDatabaseBootstrap()
        // reagenda el recordatorio con la zona horaria actual del dispositivo
        container.rescheduleRemindersIfEnabled(this)
        // publica los accesos directos de la app
        com.kratt.finanzas.navigation.ShortcutRegistrar.register(this)
    }
}
