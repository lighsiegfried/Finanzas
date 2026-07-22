package com.kratt.finanzas.di

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.data.backup.RestoreOutcome
import com.kratt.finanzas.presentation.addtransaction.AddTransactionViewModel
import com.kratt.finanzas.presentation.backup.BackupViewModel
import com.kratt.finanzas.presentation.lock.LockViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.kratt.finanzas.presentation.security.SecuritySettingsViewModel
import com.kratt.finanzas.security.BiometricAuthenticator

// fabrica los viewmodels usando el contenedor de la aplicacion
object AppViewModelProvider {

    val Factory = viewModelFactory {
        initializer {
            AddTransactionViewModel(
                accountRepository = container().accountRepository,
                categoryRepository = container().categoryRepository,
                addTransaction = container().addTransaction,
                validateTransaction = container().validateTransaction,
            )
        }
        initializer {
            val context = appContext()
            LockViewModel(
                appLockManager = container().appLockManager,
                checkAvailability = { BiometricAuthenticator.availability(context) },
            )
        }
        initializer {
            val context = appContext()
            SecuritySettingsViewModel(
                securityPreferencesRepository = container().securityPreferencesRepository,
                checkAvailability = { BiometricAuthenticator.availability(context) },
            )
        }
        initializer {
            val c = container()
            BackupViewModel(
                metadata = c.backupPreferencesRepository.metadata,
                createBackup = c::createBackup,
                prepareRestore = { uri: Uri, resolver: ContentResolver, password ->
                    // abre el archivo elegido y delega la validacion al contenedor
                    withContext(Dispatchers.IO) {
                        resolver.openInputStream(uri)?.use { input -> c.prepareRestore(input, password) }
                            ?: RestoreOutcome.WrongPasswordOrCorrupt
                    }
                },
                commitRestore = c::commitRestore,
                discardRestore = c::discardRestore,
            )
        }
    }

    private fun CreationExtras.application(): FinanzasApplication {
        val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
        return application as FinanzasApplication
    }

    private fun CreationExtras.container(): AppContainer = application().container

    private fun CreationExtras.appContext(): Context = application().applicationContext
}
