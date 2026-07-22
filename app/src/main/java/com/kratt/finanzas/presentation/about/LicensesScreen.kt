package com.kratt.finanzas.presentation.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

// nombre tecnico del componente y su licencia, datos no traducibles
private data class LicenseItem(val name: String, val license: String)

private val licenses = listOf(
    LicenseItem("AndroidX Core, Lifecycle, Activity, Navigation", "Apache-2.0"),
    LicenseItem("Jetpack Compose + Material 3", "Apache-2.0"),
    LicenseItem("AndroidX Room", "Apache-2.0"),
    LicenseItem("SQLCipher for Android (net.zetetic)", "SQLCipher/Zetetic BSD-style"),
    LicenseItem("Bouncy Castle (bcprov-jdk18on)", "MIT (Bouncy Castle License)"),
    LicenseItem("AndroidX Biometric", "Apache-2.0"),
    LicenseItem("AndroidX DataStore", "Apache-2.0"),
    LicenseItem("AndroidX WorkManager", "Apache-2.0"),
    LicenseItem("AndroidX Fragment", "Apache-2.0"),
    LicenseItem("Vico (com.patrykandpatrick.vico)", "Apache-2.0"),
    LicenseItem("Kotlin standard library + coroutines", "Apache-2.0"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.LICENSES_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.licenses_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(stringResource(R.string.licenses_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(licenses) { item ->
                Column {
                    Text(item.name, style = MaterialTheme.typography.bodyLarge)
                    Text(item.license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
