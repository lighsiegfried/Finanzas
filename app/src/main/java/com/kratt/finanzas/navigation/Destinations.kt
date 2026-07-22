package com.kratt.finanzas.navigation

import com.kratt.finanzas.domain.model.TransactionType

// rutas estables de navegacion de la app
object Destinations {
    const val SUMMARY = "summary"
    const val TRANSACTIONS = "transactions"
    const val ACCOUNTS = "accounts"
    const val ADD_TRANSACTION = "add_transaction"
    const val ADD_ACCOUNT = "add_account"
    const val ADD_TRANSFER = "add_transfer"
    const val CATEGORIES = "categories"
    const val SECURITY = "security"
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
    const val INSTALLMENTS = "installments"
    const val ADD_INSTALLMENT = "add_installment"
    const val RECURRING = "recurring"
    const val ADD_RECURRING = "add_recurring"
    const val REMINDERS = "reminders"
    const val BUDGETS = "budgets"
    const val REPORTS = "reports"
    const val ABOUT = "about"
    const val ASSISTANT = "assistant"
    const val DATA_PROTECTION = "data_protection"
    const val MIGRATE_PHONE = "migrate_phone"
    const val PRIVACY = "privacy"
    const val LICENSES = "licenses"
    const val IMPORT = "import"
    const val ONBOARDING = "onboarding"
    const val APPEARANCE = "appearance"
    const val DASHBOARD_CUSTOMIZE = "dashboard_customize"
    const val QUICK_ACTIONS = "quick_actions"
    const val TEMPLATES = "templates"
    const val TEMPLATE_FORM = "template_form?templateId={templateId}"
    const val WIDGET_SETTINGS = "widget_settings"

    // metas de ahorro y compras planificadas (fase 5c)
    const val SAVINGS_GOALS = "savings_goals"
    const val GOAL_FORM = "goal_form?goalId={goalId}"
    const val GOAL_DETAIL = "goal_detail/{goalId}"
    const val CONTRIBUTION_FORM = "contribution_form/{goalId}"
    const val PLANNED_PURCHASES = "planned_purchases"
    const val PLANNING_CSV = "planning_csv"
    const val PURCHASE_FORM = "purchase_form?purchaseId={purchaseId}"
    const val PURCHASE_DETAIL = "purchase_detail/{purchaseId}"
    const val ARG_GOAL_ID = "goalId"
    const val ARG_PURCHASE_ID = "purchaseId"

    fun goalForm(id: Long?) = if (id == null) "goal_form" else "goal_form?goalId=$id"
    fun goalDetail(id: Long) = "goal_detail/$id"
    fun contributionForm(goalId: Long) = "contribution_form/$goalId"
    fun purchaseForm(id: Long?) = if (id == null) "purchase_form" else "purchase_form?purchaseId=$id"
    fun purchaseDetail(id: Long) = "purchase_detail/$id"

    // ruta del formulario de movimiento con tipo y plantilla opcionales
    const val ADD_TRANSACTION_ROUTE = "add_transaction?type={type}&templateId={templateId}"
    const val ADD_TRANSFER_ROUTE = "add_transfer?templateId={templateId}"
    const val ARG_TEMPLATE_ID = "templateId"

    // historial abierto ya filtrado desde una grafica; conserva cuenta, categoria y tipo
    const val TRANSACTIONS_FILTERED = "transactions_filtered?accountId={accountId}&categoryId={categoryId}&type={type}"

    const val ADD_BUDGET = "add_budget/{year}/{month}"
    const val EDIT_BUDGET = "edit_budget/{budgetId}"
    const val REPORT_DETAIL = "report_detail/{reportType}"

    const val ACCOUNT_DETAIL = "account_detail/{accountId}"
    const val EDIT_ACCOUNT = "edit_account/{accountId}"
    const val ADD_CATEGORY = "add_category/{type}"
    const val EDIT_CATEGORY = "edit_category/{categoryId}"
    const val MOVEMENT_DETAIL = "movement_detail/{transactionId}"
    const val MOVEMENT_ATTACHMENTS = "movement_attachments/{transactionId}"
    const val ATTACHMENTS_STORAGE = "attachments_storage"
    const val EDIT_MOVEMENT = "edit_movement/{transactionId}"
    const val EDIT_TRANSFER = "edit_transfer/{transactionId}"
    const val INSTALLMENT_DETAIL = "installment_detail/{planId}"
    const val INSTALLMENT_SCHEDULE = "installment_schedule/{planId}"

    const val ARG_ACCOUNT_ID = "accountId"
    const val ARG_CATEGORY_ID = "categoryId"
    const val ARG_TRANSACTION_ID = "transactionId"
    const val ARG_TYPE = "type"
    const val ARG_PLAN_ID = "planId"
    const val ARG_YEAR = "year"
    const val ARG_MONTH = "month"
    const val ARG_BUDGET_ID = "budgetId"
    const val ARG_REPORT_TYPE = "reportType"

    // arma la ruta del formulario con el tipo preseleccionado
    fun addTransaction(type: TransactionType) = "add_transaction?type=${type.name}"

    fun templateForm(id: Long?) = if (id == null) "template_form" else "template_form?templateId=$id"

    // abre el formulario normal ya prellenado desde una plantilla, segun el tipo
    fun useTemplate(type: TransactionType, id: Long): String = when (type) {
        TransactionType.TRANSFER -> "add_transfer?templateId=$id"
        else -> "add_transaction?type=${type.name}&templateId=$id"
    }

    // arma la ruta del historial filtrado con los parametros presentes
    fun transactionsFiltered(accountId: Long?, categoryId: Long?, type: TransactionType?): String {
        val params = buildList {
            accountId?.let { add("accountId=$it") }
            categoryId?.let { add("categoryId=$it") }
            type?.let { add("type=${it.name}") }
        }
        return if (params.isEmpty()) "transactions_filtered" else "transactions_filtered?${params.joinToString("&")}"
    }

    fun addBudget(year: Int, month: Int) = "add_budget/$year/$month"
    fun editBudget(id: Long) = "edit_budget/$id"
    fun reportDetail(typeName: String) = "report_detail/$typeName"

    fun installmentDetail(id: Long) = "installment_detail/$id"
    fun installmentSchedule(id: Long) = "installment_schedule/$id"

    fun accountDetail(id: Long) = "account_detail/$id"
    fun editAccount(id: Long) = "edit_account/$id"
    fun addCategory(type: TransactionType) = "add_category/${type.name}"
    fun editCategory(id: Long) = "edit_category/$id"
    fun movementDetail(id: Long) = "movement_detail/$id"
    fun movementAttachments(id: Long) = "movement_attachments/$id"
    fun editMovement(id: Long) = "edit_movement/$id"
    fun editTransfer(id: Long) = "edit_transfer/$id"
}
