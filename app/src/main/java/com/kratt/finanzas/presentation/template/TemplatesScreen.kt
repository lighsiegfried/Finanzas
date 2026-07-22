package com.kratt.finanzas.presentation.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.CurrencyFormatter
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.TransactionTemplate
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.containerViewModel

@Composable
fun TemplatesRoute(
    onBack: () -> Unit,
    onAddTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onUseTemplate: (TransactionTemplate) -> Unit,
) {
    val viewModel = containerViewModel { TemplatesViewModel(it.transactionTemplateRepository) }
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val recent by viewModel.recent.collectAsStateWithLifecycle()
    val all by viewModel.all.collectAsStateWithLifecycle()
    TemplatesScreen(
        favorites = favorites,
        recent = recent,
        all = all,
        onBack = onBack,
        onAddTemplate = onAddTemplate,
        onEditTemplate = onEditTemplate,
        onUseTemplate = onUseTemplate,
        onToggleFavorite = viewModel::onToggleFavorite,
    )
}

private const val TAB_FAVORITES = 0
private const val TAB_RECENT = 1
private const val TAB_ALL = 2

// lista de plantillas separadas por favoritas, recientes y todas
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    favorites: List<TransactionTemplate>,
    recent: List<TransactionTemplate>,
    all: List<TransactionTemplate>,
    onBack: () -> Unit,
    onAddTemplate: () -> Unit,
    onEditTemplate: (Long) -> Unit,
    onUseTemplate: (TransactionTemplate) -> Unit,
    onToggleFavorite: (TransactionTemplate) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_FAVORITES) }

    Scaffold(
        modifier = Modifier.testTag(TestTags.TEMPLATES_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.templates_title)) },
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
                onClick = onAddTemplate,
                modifier = Modifier.testTag(TestTags.ADD_TEMPLATE_BUTTON),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.template_new))
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == TAB_FAVORITES,
                    onClick = { selectedTab = TAB_FAVORITES },
                    text = { Text(stringResource(R.string.template_favorites)) },
                    modifier = Modifier.testTag(TestTags.TEMPLATE_TAB_FAVORITES),
                )
                Tab(
                    selected = selectedTab == TAB_RECENT,
                    onClick = { selectedTab = TAB_RECENT },
                    text = { Text(stringResource(R.string.template_recent)) },
                    modifier = Modifier.testTag(TestTags.TEMPLATE_TAB_RECENT),
                )
                Tab(
                    selected = selectedTab == TAB_ALL,
                    onClick = { selectedTab = TAB_ALL },
                    text = { Text(stringResource(R.string.template_all)) },
                    modifier = Modifier.testTag(TestTags.TEMPLATE_TAB_ALL),
                )
            }

            val templates = when (selectedTab) {
                TAB_FAVORITES -> favorites
                TAB_RECENT -> recent
                else -> all
            }

            if (templates.isEmpty()) {
                // el mensaje vacio cambia segun la pestaña seleccionada
                val emptyRes = when (selectedTab) {
                    TAB_FAVORITES -> R.string.template_no_favorites
                    TAB_RECENT -> R.string.template_empty
                    else -> R.string.template_empty
                }
                val emptyTag = if (selectedTab == TAB_ALL) TestTags.TEMPLATES_EMPTY_STATE else ""
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(emptyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.testTag(emptyTag),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onEdit = { onEditTemplate(template.id) },
                            onUse = { onUseTemplate(template) },
                            onToggleFavorite = { onToggleFavorite(template) },
                        )
                    }
                }
            }
        }
    }
}

// tarjeta con los datos de la plantilla y sus acciones rapidas
@Composable
private fun TemplateCard(
    template: TransactionTemplate,
    onEdit: () -> Unit,
    onUse: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${TestTags.TEMPLATE_ITEM}_${template.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = template.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(typeLabelRes(template.type)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    // solo mostramos el monto cuando la plantilla trae uno
                    template.defaultAmountCents?.let { cents ->
                        Text(
                            text = CurrencyFormatter.format(cents),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.testTag("${TestTags.TEMPLATE_FAVORITE_TOGGLE}_${template.id}"),
                ) {
                    Icon(
                        imageVector = if (template.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = stringResource(R.string.template_favorites),
                    )
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.template_edit))
                }
            }
            Button(
                onClick = onUse,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.USE_TEMPLATE_BUTTON),
            ) {
                Text(stringResource(R.string.template_use))
            }
        }
    }
}

// etiqueta de tipo reutilizando los textos existentes
private fun typeLabelRes(type: TransactionType): Int = when (type) {
    TransactionType.EXPENSE -> R.string.transaction_type_expense
    TransactionType.INCOME -> R.string.transaction_type_income
    TransactionType.TRANSFER -> R.string.filter_transfers
}
