package com.kratt.finanzas.presentation.goal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.ShortDateFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.common.maskedAmount

@Composable
fun GoalsRoute(
    onBack: () -> Unit,
    onAddGoal: () -> Unit,
    onGoalClick: (Long) -> Unit,
) {
    val viewModel = containerViewModel { GoalsViewModel(it.savingsGoalRepository) }
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    GoalsScreen(
        goals = goals,
        totals = totals,
        onBack = onBack,
        onAddGoal = onAddGoal,
        onGoalClick = onGoalClick,
    )
}

private const val TAB_ACTIVE = 0
private const val TAB_COMPLETED = 1
private const val TAB_PAUSED = 2
private const val TAB_ARCHIVED = 3
private const val TAB_ALL = 4

// lista de metas de ahorro filtradas por estado con su avance real
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    goals: List<SavingsGoal>,
    totals: Map<Long, Long>,
    onBack: () -> Unit,
    onAddGoal: () -> Unit,
    onGoalClick: (Long) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_ACTIVE) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.GOALS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.goals_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddGoal,
                modifier = Modifier.testTag(TestTags.ADD_GOAL_BUTTON),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.goal_new))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                Tab(
                    selected = selectedTab == TAB_ACTIVE,
                    onClick = { selectedTab = TAB_ACTIVE },
                    text = { Text(stringResource(R.string.goal_tab_active)) },
                )
                Tab(
                    selected = selectedTab == TAB_COMPLETED,
                    onClick = { selectedTab = TAB_COMPLETED },
                    text = { Text(stringResource(R.string.goal_tab_completed)) },
                )
                Tab(
                    selected = selectedTab == TAB_PAUSED,
                    onClick = { selectedTab = TAB_PAUSED },
                    text = { Text(stringResource(R.string.goal_tab_paused)) },
                )
                Tab(
                    selected = selectedTab == TAB_ARCHIVED,
                    onClick = { selectedTab = TAB_ARCHIVED },
                    text = { Text(stringResource(R.string.goal_tab_archived)) },
                )
                Tab(
                    selected = selectedTab == TAB_ALL,
                    onClick = { selectedTab = TAB_ALL },
                    text = { Text(stringResource(R.string.goal_tab_all)) },
                )
            }

            // el filtro por estado sale directo del enum de la meta
            val filtered = when (selectedTab) {
                TAB_ACTIVE -> goals.filter { it.status == SavingsGoalStatus.ACTIVE }
                TAB_COMPLETED -> goals.filter { it.status == SavingsGoalStatus.COMPLETED }
                TAB_PAUSED -> goals.filter { it.status == SavingsGoalStatus.PAUSED }
                TAB_ARCHIVED -> goals.filter { it.status == SavingsGoalStatus.ARCHIVED }
                else -> goals
            }

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.goal_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.testTag(TestTags.GOALS_EMPTY_STATE),
                        )
                        Button(onClick = onAddGoal) {
                            Text(stringResource(R.string.goal_create))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            contributedCents = totals[goal.id] ?: 0L,
                            onClick = { onGoalClick(goal.id) },
                        )
                    }
                }
            }
        }
    }
}

// tarjeta con el avance de una meta; el estado siempre va como texto, nunca solo color
@Composable
private fun GoalCard(
    goal: SavingsGoal,
    contributedCents: Long,
    onClick: () -> Unit,
) {
    // fraccion acotada a 1 para no pasarnos de la barra cuando hay excedente
    val fraction = if (goal.targetAmountCents > 0L) {
        minOf(1f, contributedCents.toFloat() / goal.targetAmountCents.toFloat())
    } else {
        0f
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("${TestTags.GOAL_ITEM}_${goal.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = stringResource(goalStatusLabelRes(goal.status)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${maskedAmount(contributedCents)} / ${maskedAmount(goal.targetAmountCents)}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.GOAL_PROGRESS_BAR),
            )
            // la fecha objetivo solo aparece cuando la meta tiene una
            goal.targetDate?.let { date ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${stringResource(R.string.goal_target_date)}: ${ShortDateFormatter.format(date)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
