package com.kratt.finanzas.domain.model

// tipo de movimiento que registra el usuario
enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER,
}

// tipo de cuenta donde se guarda el dinero
enum class AccountType {
    CASH,
    BANK_ACCOUNT,
    CREDIT_CARD,
    SAVINGS,
    HOUSEHOLD,
    DIGITAL_WALLET,
    OTHER,
}

// estado de una compra en cuotas
enum class InstallmentStatus {
    ACTIVE,
    COMPLETED,
    PAUSED,
    CANCELLED,
}

// frecuencia de las cuotas, por ahora solo mensual
enum class InstallmentFrequency {
    MONTHLY,
}

// estado de una cuota individual
enum class InstallmentOccurrenceStatus {
    PENDING,
    PAID,
    OVERDUE,
    SKIPPED,
    CANCELLED,
}

// tipo de repeticion de un movimiento recurrente
enum class RecurrenceType {
    WEEKLY,
    MONTHLY,
    YEARLY,
}

// como se registra una ocurrencia recurrente
enum class PostingMode {
    REQUIRE_CONFIRMATION,
    AUTO_POST,
}

// estado de una ocurrencia recurrente
enum class RecurringOccurrenceStatus {
    PENDING,
    POSTED,
    SKIPPED,
    OVERDUE,
    CANCELLED,
}

// estado de una meta de ahorro
enum class SavingsGoalStatus {
    ACTIVE,
    COMPLETED,
    PAUSED,
    CANCELLED,
    ARCHIVED,
}

// como se registra un aporte a la meta
enum class ContributionType {
    // solo suma al avance de la meta, no mueve dinero real
    MANUAL_TRACKING,
    // crea una transferencia real hacia la cuenta de ahorro
    ACCOUNT_TRANSFER,
    // ajuste manual del avance
    ADJUSTMENT,
}

// prioridad de una compra planificada
enum class PurchasePriority {
    LOW,
    MEDIUM,
    HIGH,
}

// estado de una compra planificada
enum class PurchaseStatus {
    PLANNING,
    SAVING,
    READY,
    PURCHASED,
    CANCELLED,
    ARCHIVED,
}
