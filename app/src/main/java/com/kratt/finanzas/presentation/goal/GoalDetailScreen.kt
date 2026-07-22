package com.kratt.finanzas.presentation.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.ContributionType
import com.kratt.finanzas.domain.model.SavingsContribution
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.usecase.GoalProgress
import com.kratt.finanzas.presentation.common.LocalBalancesHidden
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun GoalDetailRoute(
    goalId: Long,
    onBack: () -> Unit,
    onAddContribution: (Long) -> Unit,
    onEdit: (Long) -> Unit,
) {
    val viewModel = containerViewModel(key = "goal_detail_$goalId") {
        GoalDetailViewModel(it.savingsGoalRepository, it.savingsContributionRepository, it.planningReminderPreferences, it::rescheduleReminders, goalId)
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val reminderEnabled by viewModel.reminderEnabled.collectAsStateWithLifecycle()
    GoalDetailScreen(
        goal = state.goal,
        contributions = state.contributions,
        progress = state.progress,
        reminderEnabled = reminderEnabled,
        onToggleReminder = viewModel::onToggleReminder,
        onBack = onBack,
        onAddContribution = { onAddContribution(goalId) },
        onEdit = { onEdit(goalId) },
        onComplete = viewModel::onComplete,
        onPauseResume = viewModel::onPauseResume,
        onArchive = viewModel::onArchive,
        onRevert = viewModel::onRevert,
    )
}

// detalle de la meta con su avance, acciones e historial de aportes
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goal: SavingsGoal?,
    contributions: List<SavingsContribution>,
    progress: GoalProgress?,
    reminderEnabled: Boolean = false,
    onToggleReminder: (Boolean) -> Unit = {},
    onBack: () -> Unit,
    onAddContribution: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onPauseResume: () -> Unit,
    onArchive: () -> Unit,
    onRevert: (Long) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.GOAL_DETAIL_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(goal?.name ?: stringResource(R.string.goals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.goal_edit))
                    }
                },
            )
        },
    ) { innerPadding ->
        if (goal == null || progress == null) {
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        ) {
            item { ProgressBlock(goal = goal, progress = progress) }
            item { ActionsBlock(
                goal = goal,
                onAddContribution = onAddContribution,
                onComplete = onComplete,
                onPauseResume = onPauseResume,
                onArchive = onArchive,
            ) }
            item {
                // recordatorio opcional de la meta, apagado por defecto
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.reminder_goal), modifier = Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = reminderEnabled, onCheckedChange = onToggleReminder)
                }
            }
            item {
                Text(
                    text = stringResource(R.string.contribution_history),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (contributions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.contribution_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(contributions, key = { it.id }) { contribution ->
                    ContributionRow(contribution = contribution, onRevert = onRevert)
                }
            }
        }
    }
}

// bloque con el avance real de la meta; el estado va como texto y la barra es accesible
@Composable
private fun ProgressBlock(goal: SavingsGoal, progress: GoalProgress) {
    val hidden = LocalBalancesHidden.current
    val fraction = if (goal.targetAmountCents > 0L) {
        minOf(1f, progress.contributedCents.toFloat() / goal.targetAmountCents.toFloat())
    } else {
        0f
    }
    // si los saldos estan ocultos no exponemos montos en la descripcion accesible
    val progressDescription = if (hidden) {
        stringResource(R.string.goal_progress) + " " + progress.progressPercent + "%"
    } else {
        stringResource(
            R.string.goal_progress_description,
            maskedAmount(progress.contributedCents),
            maskedAmount(goal.targetAmountCents),
            progress.progressPercent,
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(goalStatusLabelRes(goal.status)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LabeledAmount(stringResource(R.string.goal_saved), maskedAmount(progress.contributedCents))
            LabeledAmount(stringResource(R.string.goal_remaining), maskedAmount(progress.remainingCents))
            Text(
                text = "${stringResource(R.string.goal_progress)}: ${progress.progressPercent}%",
                style = MaterialTheme.typography.bodyMedium,
            )
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.GOAL_PROGRESS_BAR)
                    .clearAndSetSemantics { contentDescription = progressDescription },
            )

            if (progress.isComplete) {
                Text(
                    text = stringResource(R.string.goal_completed),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (progress.surplusCents > 0L) {
                    LabeledAmount(stringResource(R.string.goal_surplus), maskedAmount(progress.surplusCents))
                }
            }

            HorizontalDivider()

            // la sugerencia mensual solo tiene sentido con fecha objetivo
            if (progress.hasTargetDate && progress.suggestedMonthlyCents != null) {
                LabeledAmount(
                    stringResource(R.string.goal_suggested_monthly),
                    maskedAmount(progress.suggestedMonthlyCents!!),
                )
            } else {
                Text(
                    text = stringResource(R.string.goal_no_target_date_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            progress.estimatedDate?.let { date ->
                Text(
                    text = "${stringResource(R.string.goal_estimated_date)}: ${ShortDateFormatter.format(date)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

// fila etiqueta + monto reutilizada en el bloque de avance
@Composable
private fun LabeledAmount(label: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(text = amount, style = MaterialTheme.typography.bodyLarge)
    }
}

// botones de accion sobre la meta; el aporte es la accion principal
@Composable
private fun ActionsBlock(
    goal: SavingsGoal,
    onAddContribution: () -> Unit,
    onComplete: () -> Unit,
    onPauseResume: () -> Unit,
    onArchive: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onAddContribution,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TestTags.ADD_CONTRIBUTION_BUTTON),
        ) {
            Text(stringResource(R.string.contribution_add))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onComplete,
                modifier = Modifier.testTag(TestTags.COMPLETE_GOAL_BUTTON),
            ) {
                Text(stringResource(R.string.goal_complete))
            }
            OutlinedButton(
                onClick = onPauseResume,
                modifier = Modifier.testTag(TestTags.PAUSE_GOAL_BUTTON),
            ) {
                // el texto cambia segun este pausada o activa
                val res = if (goal.status == SavingsGoalStatus.PAUSED) R.string.goal_resume else R.string.goal_pause
                Text(stringResource(res))
            }
            OutlinedButton(
                onClick = onArchive,
                modifier = Modifier.testTag(TestTags.ARCHIVE_GOAL_BUTTON),
            ) {
                Text(stringResource(R.string.goal_archive))
            }
        }
    }
}

// fila de un aporte con su monto, fecha, tipo y accion de revertir
@Composable
private fun ContributionRow(
    contribution: SavingsContribution,
    onRevert: (Long) -> Unit,
) {
    var showConfirm by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.CONTRIBUTION_ITEM}_${contribution.id}"),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = maskedAmount(contribution.amountCents),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = ShortDateFormatter.format(contribution.contributionDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(contributionTypeLabelRes(contribution.contributionType)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { showConfirm = true },
                modifier = Modifier.testTag("${TestTags.REVERT_CONTRIBUTION_BUTTON}_${contribution.id}"),
            ) {
                Icon(Icons.Filled.Undo, contentDescription = stringResource(R.string.contribution_revert))
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.contribution_revert)) },
            text = { Text(stringResource(R.string.contribution_revert_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    onRevert(contribution.id)
                }) {
                    Text(stringResource(R.string.revert_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            },
        )
    }
}

// etiqueta corta del tipo de aporte para el historial
private fun contributionTypeLabelRes(type: ContributionType): Int = when (type) {
    ContributionType.MANUAL_TRACKING -> R.string.contribution_type_manual
    ContributionType.ACCOUNT_TRANSFER -> R.string.contribution_type_transfer
    ContributionType.ADJUSTMENT -> R.string.contribution_type_adjustment
}
