package com.kratt.finanzas

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.data.preferences.DisplaySettings
import com.kratt.finanzas.di.AppContainer
import com.kratt.finanzas.di.DatabaseBootstrapState
import com.kratt.finanzas.domain.update.UpdateStatus
import com.kratt.finanzas.navigation.AppNavHost
import com.kratt.finanzas.navigation.Destinations
import com.kratt.finanzas.presentation.update.UpdateFailureRoute
import com.kratt.finanzas.presentation.update.UpdateSuccessDialog
import com.kratt.finanzas.navigation.LocalWindowWidthSizeClass
import com.kratt.finanzas.presentation.bootstrap.BootstrapScreen
import com.kratt.finanzas.presentation.bootstrap.RecoveryScreen
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.common.LocalHapticsEnabled
import com.kratt.finanzas.presentation.common.LocalListDensity
import com.kratt.finanzas.presentation.lock.LockRoute
import com.kratt.finanzas.presentation.theme.MisFinanzasTheme
import com.kratt.finanzas.security.LockSessionState
import com.kratt.finanzas.security.ScreenProtection

// fragmentactivity porque biometricprompt lo necesita
class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // instala el splash del sistema antes de dibujar; no retrasa el arranque
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // en release se bloquean capturas y la vista previa de recientes
        ScreenProtection.applyTo(window, BuildConfig.DEBUG)
        val container = (application as FinanzasApplication).container
        // guarda la ruta pedida por un acceso directo o widget; se navega ya desbloqueado
        handleLaunchIntent(intent, container)
        setContent {
            // calcula la clase de ancho de ventana para la navegacion adaptable
            val widthSizeClass = calculateWindowSizeClass(this).widthSizeClass
            MisFinanzasApp(container, widthSizeClass)
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent, (application as FinanzasApplication).container)
    }

    // convierte la accion del intent en una ruta pendiente; no navega si esta bloqueada
    private fun handleLaunchIntent(intent: android.content.Intent?, container: AppContainer) {
        com.kratt.finanzas.navigation.ShortcutRouting.routeForAction(intent?.action)?.let { route ->
            container.requestNavigation(route)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
private fun MisFinanzasApp(container: AppContainer, widthSizeClass: WindowWidthSizeClass) {
    // el tema sigue la preferencia del usuario y se aplica a toda la app sin reiniciar
    val display by container.displayPreferences.settings.collectAsStateWithLifecycle(initialValue = DisplaySettings())
    val session by container.appLockManager.sessionState.collectAsStateWithLifecycle()
    val locked = session == LockSessionState.LOCKED
    MisFinanzasTheme(themeMode = display.themeMode, dynamicColor = display.dynamicColor) {
        Surface(
            // expone los test tags como resource id para poder automatizar pruebas
            modifier = Modifier
                .fillMaxSize()
                .semantics { testTagsAsResourceId = true },
            color = MaterialTheme.colorScheme.background,
        ) {
            // oculta los saldos por preferencia o cuando la app esta bloqueada; ademas propaga densidad y vibracion
            CompositionLocalProvider(
                LocalBalancesHidden provides (display.balancesHidden || locked),
                LocalListDensity provides display.density,
                LocalHapticsEnabled provides display.hapticsEnabled,
                LocalWindowWidthSizeClass provides widthSizeClass,
            ) {
                val dbState by container.databaseState.collectAsStateWithLifecycle()
                // primero se prepara la base cifrada, luego el bloqueo, luego la app
                when (dbState) {
                    DatabaseBootstrapState.PREPARING,
                    DatabaseBootstrapState.MIGRATING,
                    -> BootstrapScreen(state = dbState)

                    DatabaseBootstrapState.RECOVERY_REQUIRED ->
                        RecoveryScreen(onRetry = container::retryDatabaseBootstrap)

                    DatabaseBootstrapState.READY -> {
                        val updateStatus by container.updateStatus.collectAsStateWithLifecycle()
                        // un fallo de actualizacion muestra ayuda sin borrar datos ni ofrecer un reinicio destructivo
                        if (updateStatus == UpdateStatus.FAILED) {
                            UpdateFailureRoute(
                                onRetry = container::retryUpdateHealthCheck,
                                onCreateDiagnostic = {
                                    container.requestNavigation(Destinations.DATA_PROTECTION)
                                    container.proceedPastUpdateFailure()
                                },
                            )
                        } else {
                            when (session) {
                                LockSessionState.UNKNOWN -> Box(modifier = Modifier.fillMaxSize())
                                LockSessionState.LOCKED -> LockRoute()
                                LockSessionState.UNLOCKED -> {
                                    AppNavHost()
                                    // aviso unico de actualizacion exitosa sobre la app
                                    if (updateStatus == UpdateStatus.SUCCESS) {
                                        UpdateSuccessDialog(onDismiss = container::acknowledgeUpdateSuccess)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
