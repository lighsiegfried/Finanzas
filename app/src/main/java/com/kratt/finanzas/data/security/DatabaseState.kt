package com.kratt.finanzas.data.security

// estados posibles de la base al arrancar la aplicacion
enum class DatabaseState {
    NEW_INSTALL,
    PLAINTEXT_READY_FOR_MIGRATION,
    MIGRATION_IN_PROGRESS,
    ENCRYPTED_READY,
    RECOVERY_REQUIRED,
    CORRUPT_OR_UNSUPPORTED,
}
