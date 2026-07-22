package com.kratt.finanzas.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R

// tarjeta de educacion sobre desinstalar; tono claro y no alarmante, no bloquea el uso
// se reutiliza en proteccion de datos, respaldo y acerca de
@Composable
fun UninstallEducationCard(onCreateBackup: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.education_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.education_uninstall), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.education_backup_first), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(onClick = onCreateBackup, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.education_create_backup))
            }
        }
    }
}

// explica la diferencia entre actualizar, restaurar, desinstalar, borrar datos y borrar cache
@Composable
fun DataActionsExplainer(modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.education_diff_title), style = MaterialTheme.typography.titleSmall)
            listOf(
                R.string.education_diff_update,
                R.string.education_diff_restore,
                R.string.education_diff_uninstall,
                R.string.education_diff_clear_data,
                R.string.education_diff_clear_cache,
            ).forEach { res ->
                Text("• ${stringResource(res)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
