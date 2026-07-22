package com.kratt.finanzas.domain.update

// estado que la ui muestra tras un cambio de version de la app
enum class UpdateStatus { NONE, SUCCESS, FAILED }

// situacion al arrancar comparando la version guardada con la actual
enum class UpdateSituation { FIRST_INSTALL, SAME_VERSION, UPDATED }

// logica pura para decidir si hubo una actualizacion; nunca asume instalacion nueva sin datos
object UpdateContinuity {

    // lastSuccessful en 0 significa que aun no se ha registrado ninguna version (primer arranque de esta logica)
    fun situation(lastSuccessfulVersionCode: Int, currentVersionCode: Int): UpdateSituation = when {
        lastSuccessfulVersionCode <= 0 -> UpdateSituation.FIRST_INSTALL
        currentVersionCode > lastSuccessfulVersionCode -> UpdateSituation.UPDATED
        else -> UpdateSituation.SAME_VERSION
    }
}

// calculos puros sobre la antiguedad del respaldo portable
object BackupFreshness {

    const val STALE_AFTER_DAYS = 30
    private const val MILLIS_PER_DAY = 86_400_000L

    // dias transcurridos desde el ultimo respaldo, o null si no hay respaldo
    fun ageDays(lastBackupMillis: Long?, nowMillis: Long): Long? =
        lastBackupMillis?.let { ((nowMillis - it) / MILLIS_PER_DAY).coerceAtLeast(0) }

    // el respaldo esta vencido si supera el periodo seguro; sin respaldo no cuenta como vencido
    fun isStale(lastBackupMillis: Long?, nowMillis: Long, staleAfterDays: Int = STALE_AFTER_DAYS): Boolean {
        val age = ageDays(lastBackupMillis, nowMillis) ?: return false
        return age > staleAfterDays
    }
}
