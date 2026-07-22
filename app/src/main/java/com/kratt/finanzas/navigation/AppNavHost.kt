package com.kratt.finanzas.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.FinanzasApplication
import com.kratt.finanzas.presentation.onboarding.OnboardingRoute
import java.time.YearMonth
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.QuickAction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.presentation.account.AccountDetailRoute
import com.kratt.finanzas.presentation.account.AccountFormRoute
import com.kratt.finanzas.presentation.account.AccountsRoute
import com.kratt.finanzas.presentation.addtransaction.AddTransactionRoute
import com.kratt.finanzas.presentation.about.AboutScreen
import com.kratt.finanzas.presentation.about.LicensesScreen
import com.kratt.finanzas.presentation.about.PrivacyScreen
import com.kratt.finanzas.presentation.appearance.AppearanceRoute
import com.kratt.finanzas.presentation.dashboard.DashboardCustomizeRoute
import com.kratt.finanzas.presentation.dashboard.QuickActionsRoute
import com.kratt.finanzas.presentation.backup.BackupRoute
import com.kratt.finanzas.presentation.budget.BudgetFormRoute
import com.kratt.finanzas.presentation.budget.BudgetsRoute
import com.kratt.finanzas.presentation.category.CategoriesRoute
import com.kratt.finanzas.presentation.category.CategoryFormRoute
import com.kratt.finanzas.presentation.importer.ImportRoute
import com.kratt.finanzas.presentation.installment.InstallmentDetailRoute
import com.kratt.finanzas.presentation.installment.InstallmentFormRoute
import com.kratt.finanzas.presentation.installment.InstallmentScheduleRoute
import com.kratt.finanzas.presentation.installment.InstallmentsRoute
import com.kratt.finanzas.presentation.movement.EditMovementRoute
import com.kratt.finanzas.presentation.recurring.RecurringFormRoute
import com.kratt.finanzas.presentation.recurring.RecurringRoute
import com.kratt.finanzas.presentation.reminder.ReminderSettingsRoute
import com.kratt.finanzas.presentation.movement.MovementDetailRoute
import com.kratt.finanzas.presentation.attachment.MovementAttachmentsRoute
import com.kratt.finanzas.presentation.report.AdaptiveReportsScreen
import com.kratt.finanzas.presentation.report.ReportRoute
import com.kratt.finanzas.presentation.report.ReportType
import com.kratt.finanzas.presentation.report.ReportsHomeScreen
import com.kratt.finanzas.presentation.security.SecuritySettingsRoute
import com.kratt.finanzas.presentation.settings.SettingsRoute
import com.kratt.finanzas.presentation.summary.SummaryRoute
import com.kratt.finanzas.presentation.template.TemplateFormRoute
import com.kratt.finanzas.presentation.template.TemplatesRoute
import com.kratt.finanzas.presentation.transactions.TransactionsRoute
import com.kratt.finanzas.presentation.transfer.TransferFormRoute

// arma la navegacion de la app con la barra inferior de pestañas
@Composable
fun AppNavHost() {
    // decide si arrancar en la configuracion inicial o en el resumen
    val container = (LocalContext.current.applicationContext as FinanzasApplication).container
    val onboardingCompleted by container.onboardingPreferences.completed.collectAsStateWithLifecycle(initialValue = null)
    if (onboardingCompleted == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }
    val startDestination = if (onboardingCompleted == true) Destinations.SUMMARY else Destinations.ONBOARDING

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // navega a la ruta pedida por un acceso directo o widget, solo estando ya desbloqueado
    val pendingRoute by container.pendingNavRoute.collectAsStateWithLifecycle()
    androidx.compose.runtime.LaunchedEffect(pendingRoute) {
        val route = pendingRoute ?: return@LaunchedEffect
        runCatching { navController.navigate(route) }
        container.consumeNavigation()
    }
    val showBottomBar = currentRoute == Destinations.SUMMARY ||
        currentRoute == Destinations.TRANSACTIONS ||
        currentRoute == Destinations.ACCOUNTS

    fun back() { navController.popBackStack() }

    // navegacion entre pestanas conservando el estado de cada una
    val onNavigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    // el ancho de ventana decide la navegacion: barra inferior en compacto, riel en medio y expandido
    val widthClass = LocalWindowWidthSizeClass.current
    val navLayout = AdaptiveNavLayout.navLayout(widthClass == WindowWidthSizeClass.Compact)

    Row(modifier = Modifier.fillMaxSize()) {
        if (showBottomBar && navLayout == NavLayout.NAV_RAIL) {
            AppNavRail(currentRoute = currentRoute, onNavigate = onNavigateTab)
        }
        Scaffold(
            bottomBar = {
                if (showBottomBar && navLayout == NavLayout.BOTTOM_BAR) {
                    AppBottomBar(currentRoute = currentRoute, onNavigate = onNavigateTab)
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.padding(innerPadding),
            ) {
            composable(Destinations.ONBOARDING) {
                OnboardingRoute(
                    onFinish = {
                        navController.navigate(Destinations.SUMMARY) {
                            popUpTo(Destinations.ONBOARDING) { inclusive = true }
                        }
                    },
                    onAddAccount = { navController.navigate(Destinations.ADD_ACCOUNT) },
                    onOpenCategories = { navController.navigate(Destinations.CATEGORIES) },
                    onAddRecurring = { navController.navigate(Destinations.ADD_RECURRING) },
                    onAddBudget = {
                        val month = YearMonth.now()
                        navController.navigate(Destinations.addBudget(month.year, month.monthValue))
                    },
                    onOpenSecurity = { navController.navigate(Destinations.SECURITY) },
                    onOpenBackup = { navController.navigate(Destinations.BACKUP) },
                    onOpenImport = { navController.navigate(Destinations.IMPORT) },
                )
            }
            composable(Destinations.SUMMARY) {
                SummaryRoute(
                    onAddTransactionClick = { navController.navigate(Destinations.ADD_TRANSACTION) },
                    onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
                    onMovementClick = { navController.navigate(Destinations.movementDetail(it)) },
                    onQuickAction = { action -> navController.navigate(quickActionRoute(action)) },
                    onOpenGoals = { navController.navigate(Destinations.SAVINGS_GOALS) },
                    onOpenPurchases = { navController.navigate(Destinations.PLANNED_PURCHASES) },
                )
            }
            composable(Destinations.TRANSACTIONS) {
                TransactionsRoute(onMovementClick = { navController.navigate(Destinations.movementDetail(it)) })
            }
            composable(Destinations.ACCOUNTS) {
                AccountsRoute(
                    onBack = { back() },
                    onAddAccount = { navController.navigate(Destinations.ADD_ACCOUNT) },
                    onAccountClick = { navController.navigate(Destinations.accountDetail(it)) },
                    onAddTransfer = { navController.navigate(Destinations.ADD_TRANSFER) },
                )
            }
            composable(
                Destinations.ADD_TRANSACTION_ROUTE,
                arguments = listOf(
                    navArgument(Destinations.ARG_TYPE) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                    navArgument(Destinations.ARG_TEMPLATE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                // tipo opcional para preseleccionar gasto o ingreso; plantilla opcional para prellenar
                val initialType = entry.arguments?.getString(Destinations.ARG_TYPE)
                    ?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                val templateId = entry.arguments?.getString(Destinations.ARG_TEMPLATE_ID)?.toLongOrNull()
                AddTransactionRoute(onNavigateBack = { back() }, initialType = initialType, templateId = templateId)
            }
            composable(Destinations.ADD_ACCOUNT) {
                AccountFormRoute(accountId = null, onDone = { back() })
            }
            composable(
                Destinations.EDIT_ACCOUNT,
                arguments = listOf(navArgument(Destinations.ARG_ACCOUNT_ID) { type = NavType.LongType }),
            ) { entry ->
                AccountFormRoute(accountId = entry.arguments!!.getLong(Destinations.ARG_ACCOUNT_ID), onDone = { back() })
            }
            composable(
                Destinations.ACCOUNT_DETAIL,
                arguments = listOf(navArgument(Destinations.ARG_ACCOUNT_ID) { type = NavType.LongType }),
            ) { entry ->
                AccountDetailRoute(
                    accountId = entry.arguments!!.getLong(Destinations.ARG_ACCOUNT_ID),
                    onBack = { back() },
                    onEdit = { navController.navigate(Destinations.editAccount(it)) },
                )
            }
            composable(
                Destinations.ADD_TRANSFER_ROUTE,
                arguments = listOf(
                    navArgument(Destinations.ARG_TEMPLATE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                val templateId = entry.arguments?.getString(Destinations.ARG_TEMPLATE_ID)?.toLongOrNull()
                TransferFormRoute(transferId = null, onDone = { back() }, templateId = templateId)
            }
            composable(
                Destinations.EDIT_TRANSFER,
                arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) { type = NavType.LongType }),
            ) { entry ->
                TransferFormRoute(transferId = entry.arguments!!.getLong(Destinations.ARG_TRANSACTION_ID), onDone = { back() })
            }
            composable(Destinations.CATEGORIES) {
                CategoriesRoute(
                    onBack = { back() },
                    onAddCategory = { navController.navigate(Destinations.addCategory(it)) },
                    onEditCategory = { navController.navigate(Destinations.editCategory(it)) },
                )
            }
            composable(
                Destinations.ADD_CATEGORY,
                arguments = listOf(navArgument(Destinations.ARG_TYPE) { type = NavType.StringType }),
            ) { entry ->
                val type = TransactionType.valueOf(entry.arguments!!.getString(Destinations.ARG_TYPE) ?: TransactionType.EXPENSE.name)
                CategoryFormRoute(categoryId = null, initialType = type, onDone = { back() })
            }
            composable(
                Destinations.EDIT_CATEGORY,
                arguments = listOf(navArgument(Destinations.ARG_CATEGORY_ID) { type = NavType.LongType }),
            ) { entry ->
                CategoryFormRoute(
                    categoryId = entry.arguments!!.getLong(Destinations.ARG_CATEGORY_ID),
                    initialType = TransactionType.EXPENSE,
                    onDone = { back() },
                )
            }
            composable(
                Destinations.MOVEMENT_DETAIL,
                arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) { type = NavType.LongType }),
            ) { entry ->
                MovementDetailRoute(
                    transactionId = entry.arguments!!.getLong(Destinations.ARG_TRANSACTION_ID),
                    onBack = { back() },
                    onEditMovement = { navController.navigate(Destinations.editMovement(it)) },
                    onEditTransfer = { navController.navigate(Destinations.editTransfer(it)) },
                    onOpenAttachments = { navController.navigate(Destinations.movementAttachments(it)) },
                )
            }
            composable(
                Destinations.MOVEMENT_ATTACHMENTS,
                arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) { type = NavType.LongType }),
            ) { entry ->
                MovementAttachmentsRoute(
                    transactionId = entry.arguments!!.getLong(Destinations.ARG_TRANSACTION_ID),
                    onBack = { back() },
                )
            }
            composable(
                Destinations.EDIT_MOVEMENT,
                arguments = listOf(navArgument(Destinations.ARG_TRANSACTION_ID) { type = NavType.LongType }),
            ) { entry ->
                EditMovementRoute(transactionId = entry.arguments!!.getLong(Destinations.ARG_TRANSACTION_ID), onDone = { back() })
            }
            composable(Destinations.SECURITY) {
                SecuritySettingsRoute(
                    onBack = { back() },
                    onOpenBackup = { navController.navigate(Destinations.BACKUP) },
                )
            }
            composable(Destinations.BACKUP) {
                BackupRoute(onBack = { back() })
            }
            composable(Destinations.SETTINGS) {
                SettingsRoute(
                    onBack = { back() },
                    onOpenCategories = { navController.navigate(Destinations.CATEGORIES) },
                    onOpenSecurity = { navController.navigate(Destinations.SECURITY) },
                    onOpenBackup = { navController.navigate(Destinations.BACKUP) },
                    onOpenInstallments = { navController.navigate(Destinations.INSTALLMENTS) },
                    onOpenRecurring = { navController.navigate(Destinations.RECURRING) },
                    onOpenReminders = { navController.navigate(Destinations.REMINDERS) },
                    onOpenBudgets = { navController.navigate(Destinations.BUDGETS) },
                    onOpenReports = { navController.navigate(Destinations.REPORTS) },
                    onOpenAbout = { navController.navigate(Destinations.ABOUT) },
                    onOpenImport = { navController.navigate(Destinations.IMPORT) },
                    onOpenAppearance = { navController.navigate(Destinations.APPEARANCE) },
                    onOpenDashboardCustomize = { navController.navigate(Destinations.DASHBOARD_CUSTOMIZE) },
                    onOpenQuickActions = { navController.navigate(Destinations.QUICK_ACTIONS) },
                    onOpenTemplates = { navController.navigate(Destinations.TEMPLATES) },
                    onOpenWidgets = { navController.navigate(Destinations.WIDGET_SETTINGS) },
                    onOpenGoals = { navController.navigate(Destinations.SAVINGS_GOALS) },
                    onOpenPurchases = { navController.navigate(Destinations.PLANNED_PURCHASES) },
                    onOpenPlanningCsv = { navController.navigate(Destinations.PLANNING_CSV) },
                    onOpenAttachments = { navController.navigate(Destinations.ATTACHMENTS_STORAGE) },
                    onOpenAssistant = { navController.navigate(Destinations.ASSISTANT) },
                    onOpenDataProtection = { navController.navigate(Destinations.DATA_PROTECTION) },
                )
            }
            composable(Destinations.ASSISTANT) {
                com.kratt.finanzas.presentation.assistant.AssistantRoute(
                    onBack = { back() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Destinations.DATA_PROTECTION) {
                com.kratt.finanzas.presentation.dataprotection.DataProtectionRoute(
                    onBack = { back() },
                    onNavigate = { route -> navController.navigate(route) },
                )
            }
            composable(Destinations.MIGRATE_PHONE) {
                com.kratt.finanzas.presentation.migrate.MigrateRoute(
                    onBack = { back() },
                    onCreateBackup = { navController.navigate(Destinations.BACKUP) },
                )
            }
            composable(Destinations.ATTACHMENTS_STORAGE) {
                com.kratt.finanzas.presentation.attachment.AttachmentsStorageRoute(onBack = { back() })
            }
            composable(Destinations.APPEARANCE) { AppearanceRoute(onBack = { back() }) }
            composable(Destinations.DASHBOARD_CUSTOMIZE) { DashboardCustomizeRoute(onBack = { back() }) }
            composable(Destinations.QUICK_ACTIONS) { QuickActionsRoute(onBack = { back() }) }
            composable(Destinations.TEMPLATES) {
                TemplatesRoute(
                    onBack = { back() },
                    onAddTemplate = { navController.navigate(Destinations.templateForm(null)) },
                    onEditTemplate = { navController.navigate(Destinations.templateForm(it)) },
                    // usar una plantilla abre el formulario normal ya prellenado
                    onUseTemplate = { template -> navController.navigate(Destinations.useTemplate(template.type, template.id)) },
                )
            }
            composable(
                Destinations.TEMPLATE_FORM,
                arguments = listOf(
                    navArgument(Destinations.ARG_TEMPLATE_ID) {
                        type = NavType.StringType; nullable = true; defaultValue = null
                    },
                ),
            ) { entry ->
                val templateId = entry.arguments?.getString(Destinations.ARG_TEMPLATE_ID)?.toLongOrNull()
                TemplateFormRoute(templateId = templateId, onDone = { back() })
            }
            composable(Destinations.WIDGET_SETTINGS) {
                com.kratt.finanzas.presentation.widgetsettings.WidgetSettingsRoute(onBack = { back() })
            }
            // metas de ahorro (fase 5c)
            composable(Destinations.SAVINGS_GOALS) {
                com.kratt.finanzas.presentation.goal.GoalsRoute(
                    onBack = { back() },
                    onAddGoal = { navController.navigate(Destinations.goalForm(null)) },
                    onGoalClick = { navController.navigate(Destinations.goalDetail(it)) },
                )
            }
            composable(
                Destinations.GOAL_FORM,
                arguments = listOf(navArgument(Destinations.ARG_GOAL_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val goalId = entry.arguments?.getString(Destinations.ARG_GOAL_ID)?.toLongOrNull()
                com.kratt.finanzas.presentation.goal.GoalFormRoute(goalId = goalId, onDone = { back() })
            }
            composable(
                Destinations.GOAL_DETAIL,
                arguments = listOf(navArgument(Destinations.ARG_GOAL_ID) { type = NavType.LongType }),
            ) { entry ->
                com.kratt.finanzas.presentation.goal.GoalDetailRoute(
                    goalId = entry.arguments!!.getLong(Destinations.ARG_GOAL_ID),
                    onBack = { back() },
                    onAddContribution = { navController.navigate(Destinations.contributionForm(it)) },
                    onEdit = { navController.navigate(Destinations.goalForm(it)) },
                )
            }
            composable(
                Destinations.CONTRIBUTION_FORM,
                arguments = listOf(navArgument(Destinations.ARG_GOAL_ID) { type = NavType.LongType }),
            ) { entry ->
                com.kratt.finanzas.presentation.goal.ContributionFormRoute(
                    goalId = entry.arguments!!.getLong(Destinations.ARG_GOAL_ID),
                    onDone = { back() },
                )
            }
            // compras planificadas (fase 5c)
            composable(Destinations.PLANNED_PURCHASES) {
                com.kratt.finanzas.presentation.purchase.PurchasesRoute(
                    onBack = { back() },
                    onAddPurchase = { navController.navigate(Destinations.purchaseForm(null)) },
                    onPurchaseClick = { navController.navigate(Destinations.purchaseDetail(it)) },
                )
            }
            composable(
                Destinations.PURCHASE_FORM,
                arguments = listOf(navArgument(Destinations.ARG_PURCHASE_ID) { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) { entry ->
                val purchaseId = entry.arguments?.getString(Destinations.ARG_PURCHASE_ID)?.toLongOrNull()
                com.kratt.finanzas.presentation.purchase.PurchaseFormRoute(purchaseId = purchaseId, onDone = { back() })
            }
            composable(
                Destinations.PURCHASE_DETAIL,
                arguments = listOf(navArgument(Destinations.ARG_PURCHASE_ID) { type = NavType.LongType }),
            ) { entry ->
                com.kratt.finanzas.presentation.purchase.PurchaseDetailRoute(
                    purchaseId = entry.arguments!!.getLong(Destinations.ARG_PURCHASE_ID),
                    onBack = { back() },
                    onEdit = { navController.navigate(Destinations.purchaseForm(it)) },
                )
            }
            composable(Destinations.PLANNING_CSV) {
                com.kratt.finanzas.presentation.planning.PlanningDataRoute(onBack = { back() })
            }
            composable(Destinations.IMPORT) {
                ImportRoute(
                    onBack = { back() },
                    onOpenBackup = { navController.navigate(Destinations.BACKUP) },
                )
            }
            composable(Destinations.ABOUT) {
                AboutScreen(
                    onBack = { back() },
                    onOpenPrivacy = { navController.navigate(Destinations.PRIVACY) },
                    onOpenLicenses = { navController.navigate(Destinations.LICENSES) },
                )
            }
            composable(Destinations.PRIVACY) { PrivacyScreen(onBack = { back() }) }
            composable(Destinations.LICENSES) { LicensesScreen(onBack = { back() }) }
            composable(Destinations.BUDGETS) {
                BudgetsRoute(
                    onBack = { back() },
                    onAddBudget = { year, month -> navController.navigate(Destinations.addBudget(year, month)) },
                    onEditBudget = { navController.navigate(Destinations.editBudget(it)) },
                )
            }
            composable(
                Destinations.ADD_BUDGET,
                arguments = listOf(
                    navArgument(Destinations.ARG_YEAR) { type = NavType.IntType },
                    navArgument(Destinations.ARG_MONTH) { type = NavType.IntType },
                ),
            ) { entry ->
                BudgetFormRoute(
                    year = entry.arguments!!.getInt(Destinations.ARG_YEAR),
                    month = entry.arguments!!.getInt(Destinations.ARG_MONTH),
                    budgetId = null,
                    onDone = { back() },
                )
            }
            composable(
                Destinations.EDIT_BUDGET,
                arguments = listOf(navArgument(Destinations.ARG_BUDGET_ID) { type = NavType.LongType }),
            ) { entry ->
                BudgetFormRoute(year = 0, month = 0, budgetId = entry.arguments!!.getLong(Destinations.ARG_BUDGET_ID), onDone = { back() })
            }
            composable(Destinations.REPORTS) {
                // en pantallas expandidas los reportes usan lista mas detalle en dos paneles
                AdaptiveReportsScreen(
                    expanded = AdaptiveNavLayout.useReportsTwoPane(LocalWindowWidthSizeClass.current == WindowWidthSizeClass.Expanded),
                    onBack = { back() },
                    onReportClick = { navController.navigate(Destinations.reportDetail(it.name)) },
                    onOpenMovements = { filter ->
                        navController.navigate(Destinations.transactionsFiltered(filter.accountId, filter.categoryId, filter.type))
                    },
                )
            }
            composable(
                Destinations.REPORT_DETAIL,
                arguments = listOf(navArgument(Destinations.ARG_REPORT_TYPE) { type = NavType.StringType }),
            ) { entry ->
                val typeName = entry.arguments!!.getString(Destinations.ARG_REPORT_TYPE) ?: ReportType.INCOME_EXPENSE.name
                ReportRoute(
                    type = ReportType.valueOf(typeName),
                    onBack = { back() },
                    // abre el historial filtrado por la cuenta, categoria o tipo del punto elegido
                    onOpenMovements = { filter ->
                        navController.navigate(Destinations.transactionsFiltered(filter.accountId, filter.categoryId, filter.type))
                    },
                )
            }
            composable(
                Destinations.TRANSACTIONS_FILTERED,
                arguments = listOf(
                    navArgument(Destinations.ARG_ACCOUNT_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_CATEGORY_ID) { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument(Destinations.ARG_TYPE) { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                val a = entry.arguments?.getString(Destinations.ARG_ACCOUNT_ID)?.toLongOrNull()
                val c = entry.arguments?.getString(Destinations.ARG_CATEGORY_ID)?.toLongOrNull()
                val t = entry.arguments?.getString(Destinations.ARG_TYPE)?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }
                TransactionsRoute(
                    onMovementClick = { navController.navigate(Destinations.movementDetail(it)) },
                    initialType = t, initialAccountId = a, initialCategoryId = c,
                )
            }
            composable(Destinations.INSTALLMENTS) {
                InstallmentsRoute(
                    onBack = { back() },
                    onAdd = { navController.navigate(Destinations.ADD_INSTALLMENT) },
                    onPlanClick = { navController.navigate(Destinations.installmentDetail(it)) },
                )
            }
            composable(Destinations.ADD_INSTALLMENT) {
                InstallmentFormRoute(onDone = { back() })
            }
            composable(
                Destinations.INSTALLMENT_DETAIL,
                arguments = listOf(navArgument(Destinations.ARG_PLAN_ID) { type = NavType.LongType }),
            ) { entry ->
                InstallmentDetailRoute(
                    planId = entry.arguments!!.getLong(Destinations.ARG_PLAN_ID),
                    onBack = { back() },
                    onViewSchedule = { navController.navigate(Destinations.installmentSchedule(it)) },
                )
            }
            composable(
                Destinations.INSTALLMENT_SCHEDULE,
                arguments = listOf(navArgument(Destinations.ARG_PLAN_ID) { type = NavType.LongType }),
            ) { entry ->
                InstallmentScheduleRoute(planId = entry.arguments!!.getLong(Destinations.ARG_PLAN_ID), onBack = { back() })
            }
            composable(Destinations.RECURRING) {
                RecurringRoute(onBack = { back() }, onAdd = { navController.navigate(Destinations.ADD_RECURRING) })
            }
            composable(Destinations.ADD_RECURRING) {
                RecurringFormRoute(onDone = { back() })
            }
            composable(Destinations.REMINDERS) {
                ReminderSettingsRoute(onBack = { back() })
            }
            }
        }
    }
}

// convierte una accion rapida en su ruta; reusa los flujos existentes sin duplicar formularios
private fun quickActionRoute(action: QuickAction): String = when (action) {
    QuickAction.ADD_EXPENSE -> Destinations.addTransaction(TransactionType.EXPENSE)
    QuickAction.ADD_INCOME -> Destinations.addTransaction(TransactionType.INCOME)
    QuickAction.TRANSFER -> Destinations.ADD_TRANSFER
    QuickAction.REGISTER_PAYMENT -> Destinations.INSTALLMENTS
    QuickAction.VIEW_MOVEMENTS -> Destinations.TRANSACTIONS
    QuickAction.ADD_ACCOUNT -> Destinations.ADD_ACCOUNT
    QuickAction.CREATE_BUDGET -> YearMonth.now().let { Destinations.addBudget(it.year, it.monthValue) }
    QuickAction.ADD_INSTALLMENT -> Destinations.ADD_INSTALLMENT
    QuickAction.ADD_RECURRING -> Destinations.ADD_RECURRING
    QuickAction.CREATE_BACKUP -> Destinations.BACKUP
}

// riel de navegacion para pantallas de ancho medio o expandido; reusa las mismas rutas y estado
@Composable
private fun AppNavRail(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationRail(modifier = Modifier.testTag(TestTags.NAV_RAIL)) {
        NavigationRailItem(
            selected = currentRoute == Destinations.SUMMARY,
            onClick = { onNavigate(Destinations.SUMMARY) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.summary_title)) },
            modifier = Modifier.testTag(TestTags.NAV_SUMMARY),
        )
        NavigationRailItem(
            selected = currentRoute == Destinations.TRANSACTIONS,
            onClick = { onNavigate(Destinations.TRANSACTIONS) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(stringResource(R.string.transactions_title)) },
            modifier = Modifier.testTag(TestTags.NAV_TRANSACTIONS),
        )
        NavigationRailItem(
            selected = currentRoute == Destinations.ACCOUNTS,
            onClick = { onNavigate(Destinations.ACCOUNTS) },
            icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
            label = { Text(stringResource(R.string.accounts_title)) },
            modifier = Modifier.testTag(TestTags.NAV_ACCOUNTS),
        )
    }
}

@Composable
private fun AppBottomBar(currentRoute: String?, onNavigate: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == Destinations.SUMMARY,
            onClick = { onNavigate(Destinations.SUMMARY) },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text(stringResource(R.string.summary_title)) },
            modifier = Modifier.testTag(TestTags.NAV_SUMMARY),
        )
        NavigationBarItem(
            selected = currentRoute == Destinations.TRANSACTIONS,
            onClick = { onNavigate(Destinations.TRANSACTIONS) },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            label = { Text(stringResource(R.string.transactions_title)) },
            modifier = Modifier.testTag(TestTags.NAV_TRANSACTIONS),
        )
        NavigationBarItem(
            selected = currentRoute == Destinations.ACCOUNTS,
            onClick = { onNavigate(Destinations.ACCOUNTS) },
            icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
            label = { Text(stringResource(R.string.accounts_title)) },
            modifier = Modifier.testTag(TestTags.NAV_ACCOUNTS),
        )
    }
}
