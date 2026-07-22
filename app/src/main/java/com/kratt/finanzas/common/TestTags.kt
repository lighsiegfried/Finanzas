package com.kratt.finanzas.common

// identificadores estables para las pruebas de ui y para maestro
object TestTags {
    // resumen
    const val SUMMARY_SCREEN = "summary_screen"
    const val SUMMARY_TITLE = "summary_title"
    const val SUMMARY_MONTH = "summary_month"
    const val SUMMARY_INCOME_CARD = "summary_income_card"
    const val SUMMARY_EXPENSE_CARD = "summary_expense_card"
    const val SUMMARY_BALANCE_CARD = "summary_balance_card"
    const val SUMMARY_EMPTY_STATE = "summary_empty_state"
    const val RECENT_LIST = "recent_list"
    const val ADD_TRANSACTION_BUTTON = "add_transaction_button"
    const val SECURITY_ACTION = "security_action"

    // navegacion
    const val NAV_SUMMARY = "nav_summary"
    const val NAV_TRANSACTIONS = "nav_transactions"

    // formulario de movimiento
    const val ADD_TRANSACTION_SCREEN = "add_transaction_screen"
    const val TYPE_EXPENSE_OPTION = "type_expense_option"
    const val TYPE_INCOME_OPTION = "type_income_option"
    const val AMOUNT_FIELD = "amount_field"
    const val ACCOUNT_FIELD = "account_field"
    const val CATEGORY_FIELD = "category_field"
    const val DESCRIPTION_FIELD = "description_field"
    const val DATE_FIELD = "date_field"
    const val DATE_PICKER_BUTTON = "date_picker_button"
    const val SAVE_BUTTON = "save_button"
    const val CANCEL_BUTTON = "cancel_button"

    // historial de movimientos
    const val TRANSACTIONS_SCREEN = "transactions_screen"
    const val TRANSACTIONS_LIST = "transactions_list"
    const val TRANSACTIONS_EMPTY_STATE = "transactions_empty_state"
    const val TRANSACTION_ITEM = "transaction_item"

    // pantalla de bloqueo
    const val LOCK_SCREEN = "lock_screen"
    const val UNLOCK_BUTTON = "unlock_button"
    const val CONTINUE_BUTTON = "continue_button"
    const val LOCK_MESSAGE = "lock_message"

    // arranque de la base y recuperacion
    const val BOOTSTRAP_SCREEN = "bootstrap_screen"
    const val RECOVERY_SCREEN = "recovery_screen"
    const val RETRY_BUTTON = "retry_button"

    // ajustes de seguridad
    const val SECURITY_SCREEN = "security_screen"
    const val LOCK_SWITCH = "lock_switch"
    const val LOCK_UNAVAILABLE_MESSAGE = "lock_unavailable_message"
    const val TIMEOUT_SESSION = "timeout_session"
    const val TIMEOUT_TEN_MINUTES = "timeout_ten_minutes"

    // respaldo de datos
    const val BACKUP_SETTINGS_ENTRY = "backup_settings_entry"
    const val BACKUP_SCREEN = "backup_screen"
    const val CREATE_BACKUP_BUTTON = "create_backup_button"
    const val RESTORE_BACKUP_BUTTON = "restore_backup_button"
    const val LAST_BACKUP_LABEL = "last_backup_label"
    const val PASSWORD_FIELD = "password_field"
    const val CONFIRM_PASSWORD_FIELD = "confirm_password_field"
    const val SHOW_PASSWORD_TOGGLE = "show_password_toggle"
    const val PASSWORD_CONTINUE_BUTTON = "password_continue_button"
    const val EXPORT_CONFIRM_BUTTON = "export_confirm_button"
    const val RESTORE_PASSWORD_SUBMIT = "restore_password_submit"
    const val RESTORE_CONFIRM_BUTTON = "restore_confirm_button"

    // navegacion y ajustes (fase 3a)
    const val NAV_ACCOUNTS = "nav_accounts"
    const val SETTINGS_ACTION = "settings_action"
    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_CATEGORIES = "settings_categories"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_BACKUP = "settings_backup"

    // cuentas
    const val ACCOUNTS_SCREEN = "accounts_screen"
    const val ADD_ACCOUNT_BUTTON = "add_account_button"
    const val ACCOUNT_ITEM = "account_item"
    const val ACCOUNT_FORM_SCREEN = "account_form_screen"
    const val ACCOUNT_NAME_FIELD = "account_name_field"
    const val ACCOUNT_TYPE_FIELD = "account_type_field"
    const val ACCOUNT_INITIAL_BALANCE_FIELD = "account_initial_balance_field"
    const val ACCOUNT_CREDIT_LIMIT_FIELD = "account_credit_limit_field"
    const val ACCOUNT_LAST_FOUR_FIELD = "account_last_four_field"
    const val ACCOUNT_DESCRIPTION_FIELD = "account_description_field"
    const val SAVE_ACCOUNT_BUTTON = "save_account_button"
    const val DEACTIVATE_ACCOUNT_BUTTON = "deactivate_account_button"
    const val ACCOUNT_DETAIL_SCREEN = "account_detail_screen"
    const val EDIT_ACCOUNT_BUTTON = "edit_account_button"

    // categorias
    const val CATEGORIES_SCREEN = "categories_screen"
    const val ADD_CATEGORY_BUTTON = "add_category_button"
    const val CATEGORY_ITEM = "category_item"
    const val CATEGORY_FORM_SCREEN = "category_form_screen"
    const val CATEGORY_NAME_FIELD = "category_name_field"
    const val SAVE_CATEGORY_BUTTON = "save_category_button"
    const val DEACTIVATE_CATEGORY_BUTTON = "deactivate_category_button"
    const val CATEGORY_TAB_EXPENSE = "category_tab_expense"
    const val CATEGORY_TAB_INCOME = "category_tab_income"

    // transferencia
    const val TRANSFER_SCREEN = "transfer_screen"
    const val ADD_TRANSFER_BUTTON = "add_transfer_button"
    const val TRANSFER_SOURCE_FIELD = "transfer_source_field"
    const val TRANSFER_DESTINATION_FIELD = "transfer_destination_field"
    const val TRANSFER_AMOUNT_FIELD = "transfer_amount_field"
    const val SAVE_TRANSFER_BUTTON = "save_transfer_button"

    // detalle y edicion de movimiento
    const val MOVEMENT_DETAIL_SCREEN = "movement_detail_screen"
    const val EDIT_MOVEMENT_BUTTON = "edit_movement_button"
    const val DELETE_MOVEMENT_BUTTON = "delete_movement_button"
    const val CONFIRM_DELETE_BUTTON = "confirm_delete_button"
    const val EDIT_MOVEMENT_SCREEN = "edit_movement_screen"
    const val SAVE_CHANGES_BUTTON = "save_changes_button"
    const val REVERT_MOVEMENT_BUTTON = "revert_movement_button"
    const val CONFIRM_REVERT_BUTTON = "confirm_revert_button"

    // adjuntos de un movimiento
    const val ATTACHMENTS_SECTION = "attachments_section"
    const val OPEN_ATTACHMENTS_BUTTON = "open_attachments_button"
    const val ATTACHMENTS_SCREEN = "attachments_screen"
    const val ATTACHMENT_TAKE_PHOTO_BUTTON = "attachment_take_photo_button"
    const val ATTACHMENT_PICK_FILE_BUTTON = "attachment_pick_file_button"
    const val ATTACHMENT_LIST_ITEM = "attachment_list_item"
    const val ATTACHMENT_DELETE_BUTTON = "attachment_delete_button"
    const val CONFIRM_ATTACHMENT_DELETE_BUTTON = "confirm_attachment_delete_button"
    const val ATTACHMENT_PREVIEW = "attachment_preview"
    const val ATTACHMENT_OCR_BUTTON = "attachment_ocr_button"
    const val BACKUP_INCLUDE_ATTACHMENTS_SWITCH = "backup_include_attachments_switch"
    const val SETTINGS_ATTACHMENTS = "settings_attachments"
    const val SETTINGS_ASSISTANT = "settings_assistant"

    // asistente financiero local (fase 6a)
    const val ASSISTANT_SCREEN = "assistant_screen"
    const val ASSISTANT_INPUT = "assistant_input"
    const val ASSISTANT_SEND = "assistant_send"
    const val ASSISTANT_MESSAGES = "assistant_messages"
    const val ASSISTANT_CLEAR = "assistant_clear"
    const val ASSISTANT_SUGGESTIONS = "assistant_suggestions"
    const val ASSISTANT_HOW_CALCULATED = "assistant_how_calculated"
    const val ASSISTANT_ACTION = "assistant_action"
    const val ASSISTANT_CANCEL = "assistant_cancel"

    // continuidad de datos y actualizaciones (fase 6b)
    const val UPDATE_SUCCESS_DIALOG = "update_success_dialog"
    const val UPDATE_FAILURE_SCREEN = "update_failure_screen"
    const val UPDATE_RETRY = "update_retry"
    const val UPDATE_DIAGNOSTIC = "update_diagnostic"
    const val UPDATE_HELP = "update_help"
    const val SETTINGS_DATA_PROTECTION = "settings_data_protection"
    const val DATA_PROTECTION_SCREEN = "data_protection_screen"
    const val DATA_PROTECTION_BACKUP = "data_protection_backup"
    const val DATA_PROTECTION_MIGRATE = "data_protection_migrate"
    const val DATA_PROTECTION_RESTORE = "data_protection_restore"
    const val DATA_PROTECTION_VERIFY = "data_protection_verify"
    const val MIGRATE_SCREEN = "migrate_screen"
    const val MIGRATE_CREATE = "migrate_create"

    // navegacion de mes y filtros
    const val MONTH_PREVIOUS = "month_previous"
    const val MONTH_NEXT = "month_next"
    const val MONTH_CURRENT = "month_current"
    const val SEARCH_FIELD = "search_field"
    const val FILTER_TYPE_ALL = "filter_type_all"
    const val FILTER_TYPE_EXPENSE = "filter_type_expense"
    const val FILTER_TYPE_INCOME = "filter_type_income"
    const val FILTER_TYPE_TRANSFER = "filter_type_transfer"
    const val CLEAR_FILTERS_BUTTON = "clear_filters_button"
    const val NO_RESULTS = "no_results"

    // cuotas (fase 3b)
    const val INSTALLMENTS_SCREEN = "installments_screen"
    const val ADD_INSTALLMENT_BUTTON = "add_installment_button"
    const val INSTALLMENT_ITEM = "installment_item"
    const val INSTALLMENT_FORM_SCREEN = "installment_form_screen"
    const val INSTALLMENT_NAME_FIELD = "installment_name_field"
    const val INSTALLMENT_TOTAL_FIELD = "installment_total_field"
    const val INSTALLMENT_COUNT_FIELD = "installment_count_field"
    const val INSTALLMENT_ACCOUNT_FIELD = "installment_account_field"
    const val INSTALLMENT_CATEGORY_FIELD = "installment_category_field"
    const val SAVE_INSTALLMENT_BUTTON = "save_installment_button"
    const val INSTALLMENT_DETAIL_SCREEN = "installment_detail_screen"
    const val VIEW_SCHEDULE_BUTTON = "view_schedule_button"
    const val INSTALLMENT_SCHEDULE_SCREEN = "installment_schedule_screen"
    const val OCCURRENCE_ITEM = "occurrence_item"
    const val MARK_PAID_BUTTON = "mark_paid_button"
    const val CONFIRM_PAYMENT_BUTTON = "confirm_payment_button"
    const val PAUSE_INSTALLMENT_BUTTON = "pause_installment_button"

    // recurrentes (fase 3b)
    const val RECURRING_SCREEN = "recurring_screen"
    const val ADD_RECURRING_BUTTON = "add_recurring_button"
    const val RECURRING_ITEM = "recurring_item"
    const val RECURRING_FORM_SCREEN = "recurring_form_screen"
    const val RECURRING_NAME_FIELD = "recurring_name_field"
    const val RECURRING_AMOUNT_FIELD = "recurring_amount_field"
    const val SAVE_RECURRING_BUTTON = "save_recurring_button"
    const val RECURRING_OCCURRENCE_ITEM = "recurring_occurrence_item"
    const val CONFIRM_OCCURRENCE_BUTTON = "confirm_occurrence_button"
    const val SKIP_OCCURRENCE_BUTTON = "skip_occurrence_button"

    // proximos pagos y recordatorios (fase 3b)
    const val UPCOMING_PAYMENTS_SECTION = "upcoming_payments_section"
    const val REMINDER_SETTINGS_SCREEN = "reminder_settings_screen"
    const val REMINDER_TOGGLE = "reminder_toggle"
    const val REMINDER_TIME_BUTTON = "reminder_time_button"
    const val SAVE_TIME_BUTTON = "save_time_button"

    // entradas del menu de ajustes (fase 3b)
    const val SETTINGS_INSTALLMENTS = "settings_installments"
    const val SETTINGS_RECURRING = "settings_recurring"
    const val SETTINGS_REMINDERS = "settings_reminders"

    // presupuestos y reportes (fase 3c)
    const val BUDGETS_SCREEN = "budgets_screen"
    const val ADD_BUDGET_BUTTON = "add_budget_button"
    const val BUDGET_ITEM = "budget_item"
    const val BUDGET_OVERALL_CARD = "budget_overall_card"
    const val BUDGET_WARNING_BANNER = "budget_warning_banner"
    const val BUDGET_FORM_SCREEN = "budget_form_screen"
    const val BUDGET_LIMIT_FIELD = "budget_limit_field"
    const val BUDGET_CATEGORY_FIELD = "budget_category_field"
    const val BUDGET_WARNING_FIELD = "budget_warning_field"
    const val SAVE_BUDGET_BUTTON = "save_budget_button"
    const val REPORTS_SCREEN = "reports_screen"
    const val REPORT_ITEM = "report_item"
    const val REPORT_DETAIL_SCREEN = "report_detail_screen"
    const val REPORT_CHART = "report_chart"
    const val REPORT_SUMMARY = "report_summary"
    const val REPORT_TABLE = "report_table"
    const val EXPORT_REPORT_BUTTON = "export_report_button"
    const val PERIOD_FIELD = "period_field"
    const val SETTINGS_BUDGETS = "settings_budgets"
    const val SETTINGS_REPORTS = "settings_reports"

    // importar datos (fase 4d)
    const val SETTINGS_IMPORT = "settings_import"
    const val IMPORT_SCREEN = "import_screen"
    const val SELECT_CSV_BUTTON = "select_csv_button"
    const val IMPORT_PREVIEW = "import_preview"
    const val IMPORT_BUTTON = "import_button"
    const val CONFIRM_IMPORT_BUTTON = "confirm_import_button"
    const val IMPORT_SUMMARY = "import_summary"

    // onboarding (fase 4d)
    const val ONBOARDING_SCREEN = "onboarding_screen"
    const val ONBOARDING_START_BUTTON = "onboarding_start_button"
    const val ONBOARDING_SKIP_BUTTON = "onboarding_skip_button"
    const val ONBOARDING_CONTINUE_BUTTON = "onboarding_continue_button"
    const val ONBOARDING_FINISH_BUTTON = "onboarding_finish_button"

    // apariencia y privacidad de saldos (fase 5a)
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val APPEARANCE_SCREEN = "appearance_screen"
    const val DYNAMIC_COLOR_SWITCH = "dynamic_color_switch"
    const val SUMMARY_PRIVACY_TOGGLE = "summary_privacy_toggle"
    const val SETTINGS_HIDE_BALANCES = "settings_hide_balances"
    const val SETTINGS_DASHBOARD = "settings_dashboard"
    const val DASHBOARD_CUSTOMIZE_SCREEN = "dashboard_customize_screen"

    // acerca de, privacidad y licencias (fase 4c)
    const val SETTINGS_ABOUT = "settings_about"
    const val ABOUT_SCREEN = "about_screen"
    const val ABOUT_PRIVACY = "about_privacy"
    const val ABOUT_LICENSES = "about_licenses"
    const val PRIVACY_SCREEN = "privacy_screen"
    const val LICENSES_SCREEN = "licenses_screen"

    // experiencia visual personalizada (fase 5a)
    // opciones de apariencia
    const val THEME_SYSTEM_OPTION = "theme_system_option"
    const val THEME_LIGHT_OPTION = "theme_light_option"
    const val THEME_DARK_OPTION = "theme_dark_option"
    const val DENSITY_COMFORTABLE_OPTION = "density_comfortable_option"
    const val DENSITY_COMPACT_OPTION = "density_compact_option"
    const val REPORT_VIEW_CHART_OPTION = "report_view_chart_option"
    const val REPORT_VIEW_LIST_OPTION = "report_view_list_option"
    const val REPORT_VIEW_BOTH_OPTION = "report_view_both_option"
    const val HAPTICS_SWITCH = "haptics_switch"

    // personalizacion del resumen y acciones rapidas
    const val SETTINGS_QUICK_ACTIONS = "settings_quick_actions"
    const val QUICK_ACTIONS_SCREEN = "quick_actions_screen"
    const val QUICK_ACTION_OPTION = "quick_action_option"
    const val DASHBOARD_QUICK_ACTIONS = "dashboard_quick_actions"
    const val DASHBOARD_QUICK_ACTION = "dashboard_quick_action"
    const val DASHBOARD_MODULE_ROW = "dashboard_module_row"
    const val DASHBOARD_MODULE_VISIBILITY = "dashboard_module_visibility"
    const val MODULE_QUICK_ACTIONS = "module_quick_actions_section"

    // modulos del resumen
    const val MODULE_ACCOUNT_BALANCES = "module_account_balances"
    const val MODULE_BUDGET_PROGRESS = "module_budget_progress"
    const val MODULE_EXPENSE_CATEGORIES = "module_expense_categories"
    const val MODULE_MONTHLY_TREND = "module_monthly_trend"
    const val MODULE_CREDIT_CARD_DEBT = "module_credit_card_debt"
    const val MODULE_SAVINGS_BALANCE = "module_savings_balance"
    const val MODULE_SAVINGS_GOALS = "module_savings_goals_dash"
    const val MODULE_PLANNED_PURCHASES = "module_planned_purchases_dash"

    // reportes interactivos y periodo
    const val REPORT_VIEW_TOGGLE = "report_view_toggle"
    const val REPORT_LIST_MODE = "report_list_mode"
    const val REPORT_CHART_MODE = "report_chart_mode"
    const val REPORT_BOTH_MODE = "report_both_mode"
    const val CHART_INTERACTIVE = "chart_interactive"
    const val CHART_POINT = "chart_point"
    const val CHART_DETAIL_CARD = "chart_detail_card"
    const val VIEW_MOVEMENTS_BUTTON = "view_movements_button"
    const val PERIOD_SELECTOR = "period_selector"
    const val PERIOD_CHIP = "period_chip"

    // estados reutilizables
    const val STATE_LOADING = "state_loading"
    const val STATE_EMPTY = "state_empty"
    const val STATE_ERROR = "state_error"
    const val STATE_RETRY_BUTTON = "state_retry_button"

    // navegacion adaptable
    const val NAV_RAIL = "nav_rail"

    // plantillas de movimiento (fase 5b)
    const val SETTINGS_TEMPLATES = "settings_templates"
    const val TEMPLATES_SCREEN = "templates_screen"
    const val ADD_TEMPLATE_BUTTON = "add_template_button"
    const val TEMPLATE_ITEM = "template_item"
    const val TEMPLATE_FORM_SCREEN = "template_form_screen"
    const val TEMPLATE_NAME_FIELD = "template_name_field"
    const val TEMPLATE_AMOUNT_FIELD = "template_amount_field"
    const val SAVE_TEMPLATE_BUTTON = "save_template_button"
    const val USE_TEMPLATE_BUTTON = "use_template_button"
    const val TEMPLATE_FAVORITE_TOGGLE = "template_favorite_toggle"
    const val TEMPLATE_TAB_FAVORITES = "template_tab_favorites"
    const val TEMPLATE_TAB_RECENT = "template_tab_recent"
    const val TEMPLATE_TAB_ALL = "template_tab_all"
    const val TEMPLATES_EMPTY_STATE = "templates_empty_state"

    // widgets (fase 5b)
    const val SETTINGS_WIDGETS = "settings_widgets"
    const val WIDGET_SETTINGS_SCREEN = "widget_settings_screen"
    const val WIDGET_SHOW_AMOUNTS_SWITCH = "widget_show_amounts_switch"
    const val WIDGET_UPDATE_BUTTON = "widget_update_button"

    // metas de ahorro (fase 5c)
    const val SETTINGS_GOALS = "settings_goals"
    const val GOALS_SCREEN = "goals_screen"
    const val ADD_GOAL_BUTTON = "add_goal_button"
    const val GOAL_ITEM = "goal_item"
    const val GOALS_EMPTY_STATE = "goals_empty_state"
    const val GOAL_FORM_SCREEN = "goal_form_screen"
    const val GOAL_NAME_FIELD = "goal_name_field"
    const val GOAL_TARGET_FIELD = "goal_target_field"
    const val SAVE_GOAL_BUTTON = "save_goal_button"
    const val GOAL_DETAIL_SCREEN = "goal_detail_screen"
    const val ADD_CONTRIBUTION_BUTTON = "add_contribution_button"
    const val CONTRIBUTION_FORM_SCREEN = "contribution_form_screen"
    const val CONTRIBUTION_AMOUNT_FIELD = "contribution_amount_field"
    const val SAVE_CONTRIBUTION_BUTTON = "save_contribution_button"
    const val CONTRIBUTION_ITEM = "contribution_item"
    const val REVERT_CONTRIBUTION_BUTTON = "revert_contribution_button"
    const val GOAL_PROGRESS_BAR = "goal_progress_bar"
    const val COMPLETE_GOAL_BUTTON = "complete_goal_button"
    const val PAUSE_GOAL_BUTTON = "pause_goal_button"
    const val ARCHIVE_GOAL_BUTTON = "archive_goal_button"

    // compras planificadas (fase 5c)
    const val SETTINGS_PURCHASES = "settings_purchases"
    const val PURCHASES_SCREEN = "purchases_screen"
    const val ADD_PURCHASE_BUTTON = "add_purchase_button"
    const val PURCHASE_ITEM = "purchase_item"
    const val PURCHASES_EMPTY_STATE = "purchases_empty_state"
    const val PURCHASE_FORM_SCREEN = "purchase_form_screen"
    const val PURCHASE_NAME_FIELD = "purchase_name_field"
    const val PURCHASE_COST_FIELD = "purchase_cost_field"
    const val SAVE_PURCHASE_BUTTON = "save_purchase_button"
    const val PURCHASE_DETAIL_SCREEN = "purchase_detail_screen"
    const val REGISTER_PURCHASE_BUTTON = "register_purchase_button"
    const val CONFIRM_REGISTER_PURCHASE_BUTTON = "confirm_register_purchase_button"
    const val REVERSE_PURCHASE_BUTTON = "reverse_purchase_button"
    const val CONFIRM_REVERSE_PURCHASE_BUTTON = "confirm_reverse_purchase_button"

    // csv de planificacion (fase 5c)
    const val SETTINGS_PLANNING_CSV = "settings_planning_csv"
    const val PLANNING_CSV_SCREEN = "planning_csv_screen"
    const val EXPORT_GOALS_BUTTON = "export_goals_button"
    const val EXPORT_PURCHASES_BUTTON = "export_purchases_button"
    const val IMPORT_PLANNING_BUTTON = "import_planning_button"
    const val CONFIRM_IMPORT_PLANNING_BUTTON = "confirm_import_planning_button"
}
