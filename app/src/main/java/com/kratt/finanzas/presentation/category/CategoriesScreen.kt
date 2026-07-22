package com.kratt.finanzas.presentation.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.common.containerViewModel

@Composable
fun CategoriesRoute(
    onBack: () -> Unit,
    onAddCategory: (TransactionType) -> Unit,
    onEditCategory: (Long) -> Unit,
) {
    val viewModel = containerViewModel { CategoriesViewModel(it.categoryRepository) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesScreen(state, onBack, viewModel::onTypeChange, onAddCategory, onEditCategory, viewModel::onToggleActive)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onAddCategory: (TransactionType) -> Unit,
    onEditCategory: (Long) -> Unit,
    onToggleActive: (Category) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.CATEGORIES_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddCategory(state.type) },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_category_title)) },
                modifier = Modifier.testTag(TestTags.ADD_CATEGORY_BUTTON),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = if (state.type == TransactionType.EXPENSE) 0 else 1) {
                Tab(
                    selected = state.type == TransactionType.EXPENSE,
                    onClick = { onTypeChange(TransactionType.EXPENSE) },
                    text = { Text(stringResource(R.string.categories_expense)) },
                    modifier = Modifier.testTag(TestTags.CATEGORY_TAB_EXPENSE),
                )
                Tab(
                    selected = state.type == TransactionType.INCOME,
                    onClick = { onTypeChange(TransactionType.INCOME) },
                    text = { Text(stringResource(R.string.categories_income)) },
                    modifier = Modifier.testTag(TestTags.CATEGORY_TAB_INCOME),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.categories, key = { it.id }) { category ->
                    CategoryRow(category, onClick = { onEditCategory(category.id) }, onToggle = { onToggleActive(category) })
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: Category, onClick: () -> Unit, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .testTag("${TestTags.CATEGORY_ITEM}_${category.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
            if (!category.isActive) {
                Text(
                    text = stringResource(R.string.category_inactive_tag),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        TextButton(onClick = onToggle) {
            Text(stringResource(if (category.isActive) R.string.deactivate_action else R.string.reactivate_category))
        }
    }
}
