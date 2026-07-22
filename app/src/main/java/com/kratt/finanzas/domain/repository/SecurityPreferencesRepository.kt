package com.kratt.finanzas.domain.repository

import com.kratt.finanzas.domain.model.LockTimeout
import com.kratt.finanzas.domain.model.SecurityPreferences
import kotlinx.coroutines.flow.Flow

interface SecurityPreferencesRepository {
    val preferences: Flow<SecurityPreferences>
    suspend fun setAppLockEnabled(enabled: Boolean)
    suspend fun setLockTimeout(timeout: LockTimeout)
}
