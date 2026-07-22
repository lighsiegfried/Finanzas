package com.kratt.finanzas.presentation.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.assistant.AssistantAction
import com.kratt.finanzas.domain.assistant.AssistantFeature
import com.kratt.finanzas.domain.assistant.GenerativeAvailability
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.navigation.Destinations
import com.kratt.finanzas.presentation.common.containerViewModel

@Composable
fun AssistantRoute(onBack: () -> Unit, onNavigate: (String) -> Unit) {
    val viewModel = containerViewModel { AssistantViewModel(it.assistantEngine) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    AssistantScreen(
        state = state,
        onBack = onBack,
        onInputChange = viewModel::onInputChange,
        onSend = { viewModel.onSend() },
        onSuggestion = { viewModel.onSend(it) },
        onCancel = viewModel::onCancel,
        onClear = viewModel::onClear,
        onAction = { action -> routeFor(action)?.let(onNavigate) },
    )
}

// traduce una accion del asistente a una ruta de navegacion existente
private fun routeFor(action: AssistantAction): String? = when (action) {
    is AssistantAction.ViewMovements -> Destinations.transactionsFiltered(action.accountId, action.categoryId, action.type)
    is AssistantAction.OpenFeature -> when (action.feature) {
        AssistantFeature.ACCOUNTS -> Destinations.ACCOUNTS
        AssistantFeature.INSTALLMENTS -> Destinations.INSTALLMENTS
        AssistantFeature.BUDGETS -> Destinations.BUDGETS
        AssistantFeature.SAVINGS_GOALS -> Destinations.SAVINGS_GOALS
        AssistantFeature.PLANNED_PURCHASES -> Destinations.PLANNED_PURCHASES
    }
    is AssistantAction.ReviewDraft ->
        if (action.draft.type == TransactionType.TRANSFER) Destinations.ADD_TRANSFER else Destinations.addTransaction(action.draft.type)
}

private val suggestions = listOf(
    R.string.assistant_suggestion_1,
    R.string.assistant_suggestion_2,
    R.string.assistant_suggestion_3,
    R.string.assistant_suggestion_4,
    R.string.assistant_suggestion_5,
    R.string.assistant_suggestion_6,
    R.string.assistant_suggestion_7,
    R.string.assistant_suggestion_8,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(
    state: AssistantUiState,
    onBack: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onSuggestion: (String) -> Unit,
    onCancel: () -> Unit,
    onClear: () -> Unit,
    onAction: (AssistantAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.ASSISTANT_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.assistant_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.messages.isNotEmpty()) {
                        IconButton(onClick = onClear, modifier = Modifier.testTag(TestTags.ASSISTANT_CLEAR)) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.assistant_clear))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.messages.isEmpty()) {
                AssistantHome(
                    availability = state.status.generativeAvailability,
                    onSuggestion = onSuggestion,
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().testTag(TestTags.ASSISTANT_MESSAGES),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.messages) { message -> MessageItem(message, onAction) }
                }
            }

            if (state.isAnalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp), strokeWidth = 2.dp)
                    Text(stringResource(R.string.assistant_analyzing), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onCancel, modifier = Modifier.testTag(TestTags.ASSISTANT_CANCEL)) {
                        Text(stringResource(R.string.assistant_cancel))
                    }
                }
            }

            InputBar(
                input = state.input,
                enabled = !state.isAnalyzing,
                onInputChange = onInputChange,
                onSend = onSend,
            )
        }
    }
}

@Composable
private fun AssistantHome(
    availability: GenerativeAvailability,
    onSuggestion: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.assistant_home_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.assistant_privacy_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(stringResource(R.string.assistant_suggestions_title), style = MaterialTheme.typography.titleSmall)
        Column(
            modifier = Modifier.testTag(TestTags.ASSISTANT_SUGGESTIONS),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { resId ->
                val text = stringResource(resId)
                SuggestionChip(onClick = { onSuggestion(text) }, label = { Text(text) })
            }
        }

        CapabilityCard(availability)
    }
}

@Composable
private fun CapabilityCard(availability: GenerativeAvailability) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.assistant_capability_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.assistant_mode_compatible), style = MaterialTheme.typography.bodyMedium)
            val advanced = when (availability) {
                GenerativeAvailability.AVAILABLE -> stringResource(R.string.assistant_mode_advanced_available)
                else -> stringResource(R.string.assistant_mode_advanced_unavailable)
            }
            Text(advanced, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.assistant_still_works), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MessageItem(message: AssistantChatMessage, onAction: (AssistantAction) -> Unit) {
    when (message) {
        is AssistantChatMessage.User -> Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    text = message.text,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
        is AssistantChatMessage.Bot -> Card(modifier = Modifier.fillMaxWidth()) {
            AssistantAnswerContent(
                answer = message.answer,
                onAction = onAction,
                modifier = Modifier.padding(12.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputBar(
    input: String,
    enabled: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f).testTag(TestTags.ASSISTANT_INPUT),
            placeholder = { Text(stringResource(R.string.assistant_input_hint)) },
            maxLines = 3,
        )
        IconButton(
            onClick = onSend,
            enabled = enabled && input.isNotBlank(),
            modifier = Modifier.testTag(TestTags.ASSISTANT_SEND),
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.assistant_send))
        }
    }
}
