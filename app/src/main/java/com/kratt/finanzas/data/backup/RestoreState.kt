package com.kratt.finanzas.data.backup

// estados de la restauracion de un respaldo, el reemplazo solo ocurre tras confirmar
enum class RestoreState {
    IDLE,
    READING_HEADER,
    WAITING_FOR_PASSWORD,
    DERIVING_KEY,
    AUTHENTICATING_BACKUP,
    VALIDATING_BACKUP,
    WAITING_FOR_CONFIRMATION,
    PREPARING_ROLLBACK,
    REPLACING_DATABASE,
    VERIFYING_RESTORE,
    COMPLETED,
    FAILED_RECOVERABLE,
    RECOVERY_REQUIRED,
}
