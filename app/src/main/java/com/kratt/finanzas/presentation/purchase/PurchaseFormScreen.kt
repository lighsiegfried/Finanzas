package com.kratt.finanzas.presentation.purchase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.SavingsGoal
import com.kratt.finanzas.domain.usecase.PurchaseValidationError
import com.kratt.finanzas.presentation.common.containerViewModel
import com.kratt.finanzas.presentation.components.DateField
import com.kratt.finanzas.presentation.components.DropdownField
import java.time.LocalDate

@Composable
fun PurchaseFormRoute(
    purchaseId: Long?,
    onDone: () -> Unit,
) {
    val viewModel = containerViewModel(key = "purchase_form_$purchaseId") {
        PurchaseFormViewModel(
            it.plannedPurchaseRepository,
            it.savingsGoalRepository,
            it.categoryRepository,
            purchaseId,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    // cierra el formulario cuando la compra ya quedo guardada
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onDone()
    }

    PurchaseFormScreen(
        state = state,
        categories = categories,
        goals = goals,
        isEditing = viewModel.isEditing,
        onBack = onDone,
        onNameChange = viewModel::onNameChange,
        onCostChange = viewModel::onCostChange,
        onCategorySelected = viewModel::onCategorySelected,
        onGoalSelected = viewModel::onGoalSelected,
        onTargetDateSelected = viewModel::onTargetDateSelected,
        onPriorityChange = viewModel::onPriorityChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onVendorChange = viewModel::onVendorChange,
        onSave = viewModel::onSave,
    )
}

// opcion que representa "sin categoria" o una categoria concreta en el selector
private sealed interface CategoryOption {
    data object None : CategoryOption
    data class Item(val category: Category) : CategoryOption
}

// opcion que representa "sin meta" o una meta concreta en el selector
private sealed interface GoalOption {
    data object None : GoalOption
    data class Item(val goal: SavingsGoal) : GoalOption
}

// formulario para crear o editar una compra planificada
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    state: PurchaseFormUiState,
    categories: List<Category>,
    goals: List<SavingsGoal>,
    isEditing: Boolean,
    onBack: () -> Unit,
    onNameChange: (String) -> Unit,
    onCostChange: (String) -> Unit,
    onCategorySelected: (Long?) -> Unit,
    onGoalSelected: (Long?) -> Unit,
    onTargetDateSelected: (LocalDate?) -> Unit,
    onPriorityChange: (PurchasePriority) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onVendorChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TestTags.PURCHASE_FORM_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(if (isEditing) R.string.purchase_edit else R.string.purchase_new))
                },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.purchase_name)) },
                singleLine = true,
                isError = PurchaseValidationError.NAME_REQUIRED in state.errors,
                supportingText = if (PurchaseValidationError.NAME_REQUIRED in state.errors) {
                    { Text(stringResource(R.string.error_purchase_name)) }
                } else {
                    null
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PURCHASE_NAME_FIELD),
            )

            OutlinedTextField(
                value = state.costText,
                onValueChange = onCostChange,
                label = { Text(stringResource(R.string.purchase_cost)) },
                prefix = { Text(stringResource(R.string.currency_symbol)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = PurchaseValidationError.INVALID_COST in state.errors ||
                    PurchaseValidationError.AMOUNT_TOO_LARGE in state.errors,
                supportingText = costErrorRes(state.errors)?.let { res -> { Text(stringResource(res)) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.PURCHASE_COST_FIELD),
            )

            // categoria opcional; la primera opcion representa "sin categoria"
            val categoryOptions: List<CategoryOption> =
                listOf(CategoryOption.None) + categories.map { CategoryOption.Item(it) }
            DropdownField(
                label = stringResource(R.string.purchase_category),
                options = categoryOptions,
                selected = categoryOptions.firstOrNull {
                    it is CategoryOption.Item && it.category.id == state.categoryId
                } ?: CategoryOption.None,
                optionLabel = { option ->
                    when (option) {
                        CategoryOption.None -> stringResource(R.string.purchase_no_goal)
                        is CategoryOption.Item -> option.category.name
                    }
                },
                onSelected = { option ->
                    onCategorySelected((option as? CategoryOption.Item)?.category?.id)
                },
                tag = TestTags.CATEGORY_FIELD,
            )

            // meta opcional; la primera opcion representa "sin meta"
            val goalOptions: List<GoalOption> =
                listOf(GoalOption.None) + goals.map { GoalOption.Item(it) }
            DropdownField(
                label = stringResource(R.string.purchase_goal),
                options = goalOptions,
                selected = goalOptions.firstOrNull {
                    it is GoalOption.Item && it.goal.id == state.savingsGoalId
                } ?: GoalOption.None,
                optionLabel = { option ->
                    when (option) {
                        GoalOption.None -> stringResource(R.string.purchase_no_goal)
                        is GoalOption.Item -> option.goal.name
                    }
                },
                onSelected = { option ->
                    onGoalSelected((option as? GoalOption.Item)?.goal?.id)
                },
                tag = TestTags.PURCHASE_NAME_FIELD + "_goal",
            )
            // el repositorio avisa si esa meta ya tiene una compra activa
            if (state.goalAlreadyLinked) {
                Text(
                    text = stringResource(R.string.error_purchase_goal_linked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            // fecha estimada opcional; hoy es solo el valor inicial del selector
            DateField(
                date = state.targetDate ?: LocalDate.now(),
                onDateSelected = { onTargetDateSelected(it) },
                label = stringResource(R.string.purchase_target_date),
                tag = TestTags.DATE_FIELD,
            )

            // selector de prioridad con tres opciones, siempre por texto
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.purchase_priority))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = state.priority == PurchasePriority.LOW,
                        onClick = { onPriorityChange(PurchasePriority.LOW) },
                        label = { Text(stringResource(R.string.priority_low)) },
                    )
                    FilterChip(
                        selected = state.priority == PurchasePriority.MEDIUM,
                        onClick = { onPriorityChange(PurchasePriority.MEDIUM) },
                        label = { Text(stringResource(R.string.priority_medium)) },
                    )
                    FilterChip(
                        selected = state.priority == PurchasePriority.HIGH,
                        onClick = { onPriorityChange(PurchasePriority.HIGH) },
                        label = { Text(stringResource(R.string.priority_high)) },
                    )
                }
            }

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.purchase_description)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.vendor,
                onValueChange = onVendorChange,
                label = { Text(stringResource(R.string.purchase_vendor)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TestTags.SAVE_PURCHASE_BUTTON),
            ) {
                Text(stringResource(R.string.purchase_save))
            }
        }
    }
}

// el costo puede fallar por invalido o por ser demasiado grande
private fun costErrorRes(errors: Set<PurchaseValidationError>): Int? = when {
    PurchaseValidationError.INVALID_COST in errors -> R.string.error_purchase_cost
    PurchaseValidationError.AMOUNT_TOO_LARGE in errors -> R.string.error_amount_too_large
    else -> null
}
