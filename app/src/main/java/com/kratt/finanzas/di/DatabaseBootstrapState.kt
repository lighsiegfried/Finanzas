package com.kratt.finanzas.di

// estado del arranque de la base que observa la ui
enum class DatabaseBootstrapState {
    PREPARING,
    MIGRATING,
    READY,
    RECOVERY_REQUIRED,
}
