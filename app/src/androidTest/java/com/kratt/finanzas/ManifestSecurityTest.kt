package com.kratt.finanzas

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// verificacion automatica del manifiesto final instalado
@RunWith(AndroidJUnit4::class)
class ManifestSecurityTest {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    private val packageManager: PackageManager
        get() = context.packageManager

    private val packageName: String
        get() = context.packageName

    @Test
    fun noNetworkOrDangerousPermissionsRequested() {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions?.toList().orEmpty()
        // permisos esperados: el interno de androidx, los normales de biometria,
        // el aviso opcional de notificaciones y el wake_lock local de workmanager
        val allowed = setOf(
            "$packageName.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
            "android.permission.USE_BIOMETRIC",
            "android.permission.USE_FINGERPRINT",
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.WAKE_LOCK",
        )
        assertTrue(
            "unexpected permissions: ${requested - allowed}",
            requested.all { it in allowed },
        )
        assertFalse(android.Manifest.permission.INTERNET in requested)
        // se confirmo que estos permisos que mete workmanager fueron quitados a proposito
        assertFalse("android.permission.RECEIVE_BOOT_COMPLETED" in requested)
        assertFalse("android.permission.ACCESS_NETWORK_STATE" in requested)
        assertFalse("android.permission.FOREGROUND_SERVICE" in requested)
    }

    @Test
    fun backupIsDisabled() {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        assertEquals(0, appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    @Test
    fun backupAndExtractionRules_arePackaged() {
        // si el recurso no esta empaquetado esto lanza y la prueba falla
        context.resources.getXml(R.xml.backup_rules).close()
        context.resources.getXml(R.xml.data_extraction_rules).close()
        context.resources.getXml(R.xml.network_security_config).close()
    }

    @Test
    fun onlyExpectedComponentsAreExported() {
        val flags = PackageManager.GET_ACTIVITIES or PackageManager.GET_RECEIVERS or
            PackageManager.GET_SERVICES or PackageManager.GET_PROVIDERS
        val info = packageManager.getPackageInfo(packageName, flags)
        val exported = buildList {
            info.activities?.forEach { if (it.exported) add(it.name) }
            info.receivers?.forEach { if (it.exported) add(it.name) }
            info.services?.forEach { if (it.exported) add(it.name) }
            info.providers?.forEach { if (it.exported) add(it.name) }
        }
        // en debug compose agrega dos activities de herramientas, en release no existen
        // workmanager agrega dos componentes protegidos por permisos del sistema:
        // SystemJobService (BIND_JOB_SERVICE) y DiagnosticsReceiver (DUMP), ambos seguros
        val allowed = setOf(
            "com.kratt.finanzas.MainActivity",
            "androidx.profileinstaller.ProfileInstallReceiver",
            "androidx.activity.ComponentActivity",
            "androidx.compose.ui.tooling.PreviewActivity",
            "androidx.work.impl.background.systemjob.SystemJobService",
            "androidx.work.impl.diagnostics.DiagnosticsReceiver",
            // widgets de glance (fase 5b): el launcher los enlaza via APPWIDGET_UPDATE, sin datos por defecto
            "com.kratt.finanzas.widget.QuickActionsWidgetReceiver",
            "com.kratt.finanzas.widget.SummaryWidgetReceiver",
            "com.kratt.finanzas.widget.UpcomingPaymentsWidgetReceiver",
            // servicio interno de glance para las vistas remotas del widget
            "androidx.glance.appwidget.GlanceRemoteViewsService",
        )
        assertTrue("MainActivity must stay exported", "com.kratt.finanzas.MainActivity" in exported)
        assertTrue(
            "unexpected exported components: ${exported.filter { it !in allowed }}",
            exported.all { it in allowed },
        )
    }

    @Test
    fun debugBuild_isMarkedDebuggable() {
        // esta suite corre sobre el build debug, release se valida con aapt2
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        assertTrue(appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0)
    }
}
