package com.kratt.finanzas.presentation.appearance

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Density
import com.kratt.finanzas.domain.model.ReportViewMode
import com.kratt.finanzas.domain.model.ThemeMode
import com.kratt.finanzas.presentation.common.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceRoute(onBack: () -> Unit) {
    val viewModel = containerViewModel { AppearanceViewModel(it.displayPreferences) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.testTag(TestTags.APPEARANCE_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.appearance_title)) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.theme_section))
            RadioRow(stringResource(R.string.theme_system), state.themeMode == ThemeMode.SYSTEM, tag = TestTags.THEME_SYSTEM_OPTION) { viewModel.onThemeMode(ThemeMode.SYSTEM) }
            RadioRow(stringResource(R.string.theme_light), state.themeMode == ThemeMode.LIGHT, tag = TestTags.THEME_LIGHT_OPTION) { viewModel.onThemeMode(ThemeMode.LIGHT) }
            RadioRow(stringResource(R.string.theme_dark), state.themeMode == ThemeMode.DARK, tag = TestTags.THEME_DARK_OPTION) { viewModel.onThemeMode(ThemeMode.DARK) }

            HorizontalDivider()
            // el color dinamico solo aplica en android 12 o superior
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.dynamic_color_section))
                    Text(stringResource(R.string.dynamic_color_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.dynamicColor,
                    onCheckedChange = { viewModel.onDynamicColor(it) },
                    enabled = com.kratt.finanzas.presentation.theme.DynamicColorPolicy.isAvailable(Build.VERSION.SDK_INT),
                    modifier = Modifier.testTag(TestTags.DYNAMIC_COLOR_SWITCH),
                )
            }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.density_section))
            RadioRow(stringResource(R.string.density_comfortable), state.density == Density.COMFORTABLE, stringResource(R.string.density_comfortable_note), TestTags.DENSITY_COMFORTABLE_OPTION) { viewModel.onDensity(Density.COMFORTABLE) }
            RadioRow(stringResource(R.string.density_compact), state.density == Density.COMPACT, stringResource(R.string.density_compact_note), TestTags.DENSITY_COMPACT_OPTION) { viewModel.onDensity(Density.COMPACT) }

            HorizontalDivider()
            SectionTitle(stringResource(R.string.report_view_section))
            RadioRow(stringResource(R.string.report_view_chart), state.reportViewMode == ReportViewMode.CHART, tag = TestTags.REPORT_VIEW_CHART_OPTION) { viewModel.onReportViewMode(ReportViewMode.CHART) }
            RadioRow(stringResource(R.string.report_view_list), state.reportViewMode == ReportViewMode.LIST, tag = TestTags.REPORT_VIEW_LIST_OPTION) { viewModel.onReportViewMode(ReportViewMode.LIST) }
            RadioRow(stringResource(R.string.report_view_both), state.reportViewMode == ReportViewMode.BOTH, tag = TestTags.REPORT_VIEW_BOTH_OPTION) { viewModel.onReportViewMode(ReportViewMode.BOTH) }

            HorizontalDivider()
            // vibracion opcional al confirmar; usa la api del sistema y no pide permisos
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.haptics_title))
                    Text(stringResource(R.string.haptics_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = state.hapticsEnabled,
                    onCheckedChange = { viewModel.onHapticsEnabled(it) },
                    modifier = Modifier.testTag(TestTags.HAPTICS_SWITCH),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun RadioRow(label: String, selected: Boolean, note: String? = null, tag: String? = null, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(label)
            note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
}
