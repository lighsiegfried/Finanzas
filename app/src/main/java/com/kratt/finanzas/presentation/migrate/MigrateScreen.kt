package com.kratt.finanzas.presentation.migrate

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags

@Composable
fun MigrateRoute(onBack: () -> Unit, onCreateBackup: () -> Unit) {
    MigrateScreen(onBack = onBack, onCreateBackup = onCreateBackup)
}

// guia para pasar los datos a otro telefono; reutiliza el respaldo portable, sin servidor
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MigrateScreen(onBack: () -> Unit, onCreateBackup: () -> Unit) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.MIGRATE_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.migrate_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.migrate_intro), style = MaterialTheme.typography.bodyLarge)

            listOf(
                R.string.migrate_step_create,
                R.string.migrate_step_password,
                R.string.migrate_step_move,
                R.string.migrate_step_install,
                R.string.migrate_step_restore,
                R.string.migrate_step_verify,
            ).forEach { step ->
                Text(stringResource(step), style = MaterialTheme.typography.bodyMedium)
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.migrate_password_warning), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.migrate_no_server), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(onClick = onCreateBackup, modifier = Modifier.fillMaxWidth().testTag(TestTags.MIGRATE_CREATE)) {
                Text(stringResource(R.string.migrate_create_action))
            }
        }
    }
}
