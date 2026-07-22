package com.kratt.finanzas.domain.usecase

import com.kratt.finanzas.domain.model.LockTimeout

object AutoLockPolicy {

    // decide si toca bloquear usando solo tiempo monotonico, nunca la hora del reloj
    fun shouldLock(
        backgroundedAtElapsedMillis: Long,
        nowElapsedMillis: Long,
        timeout: LockTimeout,
    ): Boolean {
        // por sesion la sesion sigue viva mientras el proceso viva, nunca vence por tiempo
        if (timeout == LockTimeout.SESSION) return false
        val elapsed = nowElapsedMillis - backgroundedAtElapsedMillis
        // valor negativo seria anomalia de reloj, en modo temporizado se bloquea por seguridad
        if (elapsed < 0) return true
        return elapsed >= timeout.durationMillis
    }
}
