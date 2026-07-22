package com.kratt.finanzas.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.kratt.finanzas.presentation.common.containerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenCategories: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenInstallments: () -> Unit,
    onOpenRecurring: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenBudgets: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenImport: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenDashboardCustomize: () -> Unit,
    onOpenQuickActions: () -> Unit,
    onOpenTemplates: () -> Unit,
    onOpenWidgets: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenPurchases: () -> Unit,
    onOpenPlanningCsv: () -> Unit,
    onOpenAttachments: () -> Unit,
    onOpenAssistant: () -> Unit,
    onOpenDataProtection: () -> Unit,
) {
    val viewModel = containerViewModel { SettingsViewModel(it.displayPreferences) }
    val balancesHidden by viewModel.balancesHidden.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.testTag(TestTags.SETTINGS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState())) {
            SectionHeading(stringResource(R.string.section_appearance))
            SettingsEntry(stringResource(R.string.appearance_title), TestTags.SETTINGS_APPEARANCE, onOpenAppearance)

            SectionHeading(stringResource(R.string.section_personalization))
            SettingsEntry(stringResource(R.string.customize_summary), TestTags.SETTINGS_DASHBOARD, onOpenDashboardCustomize)
            SettingsEntry(stringResource(R.string.customize_quick_actions), TestTags.SETTINGS_QUICK_ACTIONS, onOpenQuickActions)
            SettingsEntry(stringResource(R.string.settings_templates), TestTags.SETTINGS_TEMPLATES, onOpenTemplates)
            SettingsEntry(stringResource(R.string.widgets_title), TestTags.SETTINGS_WIDGETS, onOpenWidgets)
            SettingsEntry(stringResource(R.string.categories_title), TestTags.SETTINGS_CATEGORIES, onOpenCategories)

            SectionHeading(stringResource(R.string.section_privacy_security))
            SettingsEntry(stringResource(R.string.security_title), TestTags.SETTINGS_SECURITY, onOpenSecurity)
            SwitchEntry(stringResource(R.string.hide_balances), balancesHidden, TestTags.SETTINGS_HIDE_BALANCES) { viewModel.onToggleHideBalances() }

            SectionHeading(stringResource(R.string.section_finances))
            SettingsEntry(stringResource(R.string.budgets_title), TestTags.SETTINGS_BUDGETS, onOpenBudgets)
            SettingsEntry(stringResource(R.string.reports_title), TestTags.SETTINGS_REPORTS, onOpenReports)
            SettingsEntry(stringResource(R.string.installments_title), TestTags.SETTINGS_INSTALLMENTS, onOpenInstallments)
            SettingsEntry(stringResource(R.string.recurring_title), TestTags.SETTINGS_RECURRING, onOpenRecurring)
            SettingsEntry(stringResource(R.string.goals_title), TestTags.SETTINGS_GOALS, onOpenGoals)
            SettingsEntry(stringResource(R.string.purchases_title), TestTags.SETTINGS_PURCHASES, onOpenPurchases)

            SectionHeading(stringResource(R.string.section_data))
            SettingsEntry(stringResource(R.string.data_protection_title), TestTags.SETTINGS_DATA_PROTECTION, onOpenDataProtection)
            SettingsEntry(stringResource(R.string.backup_title), TestTags.SETTINGS_BACKUP, onOpenBackup)
            SettingsEntry(stringResource(R.string.import_title), TestTags.SETTINGS_IMPORT, onOpenImport)
            SettingsEntry(stringResource(R.string.settings_planning_csv), TestTags.SETTINGS_PLANNING_CSV, onOpenPlanningCsv)

            SectionHeading(stringResource(R.string.attachments_settings_title))
            SettingsEntry(stringResource(R.string.attachments_storage_title), TestTags.SETTINGS_ATTACHMENTS, onOpenAttachments)

            SectionHeading(stringResource(R.string.section_notifications))
            SettingsEntry(stringResource(R.string.reminders_title), TestTags.SETTINGS_REMINDERS, onOpenReminders)

            SectionHeading(stringResource(R.string.assistant_title))
            SettingsEntry(stringResource(R.string.assistant_title), TestTags.SETTINGS_ASSISTANT, onOpenAssistant)

            SectionHeading(stringResource(R.string.section_information))
            SettingsEntry(stringResource(R.string.about_title), TestTags.SETTINGS_ABOUT, onOpenAbout)
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsEntry(label: String, tag: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
    }
}

@Composable
private fun SwitchEntry(label: String, checked: Boolean, tag: String, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = { onToggle() })
    }
}
