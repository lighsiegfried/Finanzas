package com.kratt.finanzas.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// migracion no destructiva de la version 1 a la 2
// agrega campos de cuenta y categoria, categoria opcional en movimientos y soporte de transferencias
// se reconstruye cada tabla para que el esquema calce exacto con room y sin defaults residuales
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // reconstruye accounts con los campos nuevos y renombra el tipo BANK a BANK_ACCOUNT
        db.execSQL(
            "CREATE TABLE `accounts_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `type` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, " +
                "`initialBalanceCents` INTEGER NOT NULL, `creditLimitCents` INTEGER, " +
                "`lastFourDigits` TEXT, `description` TEXT, `isActive` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `accounts_new` (id, name, type, currencyCode, initialBalanceCents, " +
                "creditLimitCents, lastFourDigits, description, isActive, createdAt, updatedAt) " +
                "SELECT id, name, CASE WHEN type = 'BANK' THEN 'BANK_ACCOUNT' ELSE type END, " +
                "currencyCode, initialBalanceCents, NULL, NULL, NULL, isActive, createdAt, createdAt " +
                "FROM accounts",
        )
        db.execSQL("DROP TABLE accounts")
        db.execSQL("ALTER TABLE accounts_new RENAME TO accounts")

        // reconstruye categories agregando colorKey y updatedAt
        db.execSQL(
            "CREATE TABLE `categories_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `iconKey` TEXT NOT NULL, " +
                "`colorKey` TEXT, `isDefault` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `categories_new` (id, name, transactionType, iconKey, colorKey, " +
                "isDefault, isActive, createdAt, updatedAt) " +
                "SELECT id, name, transactionType, iconKey, NULL, isDefault, isActive, createdAt, createdAt " +
                "FROM categories",
        )
        db.execSQL("DROP TABLE categories")
        db.execSQL("ALTER TABLE categories_new RENAME TO categories")

        // reconstruye transactions: categoryId ahora es opcional y se agrega la cuenta destino
        db.execSQL(
            "CREATE TABLE `transactions_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`accountId` INTEGER NOT NULL, `destinationAccountId` INTEGER, `categoryId` INTEGER, " +
                "`type` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `description` TEXT, " +
                "`transactionDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`destinationAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL(
            "INSERT INTO `transactions_new` (id, accountId, destinationAccountId, categoryId, type, " +
                "amountCents, description, transactionDate, createdAt, updatedAt) " +
                "SELECT id, accountId, NULL, categoryId, type, amountCents, description, transactionDate, " +
                "createdAt, updatedAt FROM transactions",
        )
        db.execSQL("DROP TABLE transactions")
        db.execSQL("ALTER TABLE transactions_new RENAME TO transactions")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_destinationAccountId` ON `transactions` (`destinationAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_transactionDate` ON `transactions` (`transactionDate`)")
    }
}

// migracion no destructiva de la version 2 a la 3
// agrega cuotas, movimientos recurrentes y sus ocurrencias, mas la marca de origen en transactions
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // marca estable del origen del movimiento generado, columna opcional
        db.execSQL("ALTER TABLE `transactions` ADD COLUMN `originKey` TEXT")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `installment_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, " +
                "`totalAmountCents` INTEGER NOT NULL, `installmentCount` INTEGER NOT NULL, " +
                "`installmentAmountCents` INTEGER NOT NULL, `firstDueDate` INTEGER NOT NULL, `frequency` TEXT NOT NULL, " +
                "`paidInstallments` INTEGER NOT NULL, `status` TEXT NOT NULL, `description` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_plans_accountId` ON `installment_plans` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_plans_categoryId` ON `installment_plans` (`categoryId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `installment_occurrences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`installmentPlanId` INTEGER NOT NULL, `sequenceNumber` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, " +
                "`amountCents` INTEGER NOT NULL, `status` TEXT NOT NULL, `paidTransactionId` INTEGER, `paidAt` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`installmentPlanId`) REFERENCES `installment_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`paidTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_installment_occurrences_installmentPlanId_sequenceNumber` ON `installment_occurrences` (`installmentPlanId`, `sequenceNumber`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_occurrences_dueDate` ON `installment_occurrences` (`dueDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_occurrences_status` ON `installment_occurrences` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_installment_occurrences_paidTransactionId` ON `installment_occurrences` (`paidTransactionId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `accountId` INTEGER NOT NULL, " +
                "`categoryId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, " +
                "`interval` INTEGER NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER, " +
                "`nextOccurrenceDate` INTEGER NOT NULL, `postingMode` TEXT NOT NULL, `isActive` INTEGER NOT NULL, " +
                "`description` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_templates_accountId` ON `recurring_templates` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_templates_categoryId` ON `recurring_templates` (`categoryId`)")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `recurring_occurrences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`recurringTemplateId` INTEGER NOT NULL, `scheduledDate` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, " +
                "`status` TEXT NOT NULL, `generatedTransactionId` INTEGER, `createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`recurringTemplateId`) REFERENCES `recurring_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`generatedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringTemplateId_scheduledDate` ON `recurring_occurrences` (`recurringTemplateId`, `scheduledDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_scheduledDate` ON `recurring_occurrences` (`scheduledDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_status` ON `recurring_occurrences` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_generatedTransactionId` ON `recurring_occurrences` (`generatedTransactionId`)")
    }
}

// migracion no destructiva de la version 3 a la 4
// agrega la tabla de presupuestos y los indices compuestos para acelerar los reportes
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `categoryId` INTEGER, " +
                "`limitAmountCents` INTEGER NOT NULL, `warningPercentage` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_year_month_categoryId` ON `budgets` (`year`, `month`, `categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_year_month` ON `budgets` (`year`, `month`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)")

        // indices compuestos de reportes sobre transactions
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_categoryId_transactionDate` ON `transactions` (`categoryId`, `transactionDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transactions_accountId_transactionDate` ON `transactions` (`accountId`, `transactionDate`)")
    }
}

// migracion no destructiva de la version 4 a la 5
// agrega plantillas de movimiento, metas de ahorro, aportes y compras planificadas
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // plantillas reutilizables de movimiento
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `transaction_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `accountId` INTEGER NOT NULL, " +
                "`destinationAccountId` INTEGER, `categoryId` INTEGER, `defaultAmountCents` INTEGER, `description` TEXT, " +
                "`isFavorite` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `lastUsedAt` INTEGER, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`destinationAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_accountId` ON `transaction_templates` (`accountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_destinationAccountId` ON `transaction_templates` (`destinationAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_categoryId` ON `transaction_templates` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_isFavorite` ON `transaction_templates` (`isFavorite`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_templates_isActive` ON `transaction_templates` (`isActive`)")

        // metas de ahorro; el avance se calcula desde los aportes
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `savings_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `targetAmountCents` INTEGER NOT NULL, `linkedAccountId` INTEGER, " +
                "`startDate` INTEGER NOT NULL, `targetDate` INTEGER, `status` TEXT NOT NULL, `description` TEXT, " +
                "`iconKey` TEXT NOT NULL, `colorKey` TEXT NOT NULL, `isArchived` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`linkedAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_linkedAccountId` ON `savings_goals` (`linkedAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_status` ON `savings_goals` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_goals_isArchived` ON `savings_goals` (`isArchived`)")

        // aportes ligados a una meta y, si aplica, al movimiento generado
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `savings_contributions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`savingsGoalId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `contributionDate` INTEGER NOT NULL, " +
                "`sourceAccountId` INTEGER, `linkedTransactionId` INTEGER, `contributionType` TEXT NOT NULL, `note` TEXT, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`savingsGoalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , " +
                "FOREIGN KEY(`sourceAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`linkedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_savingsGoalId` ON `savings_contributions` (`savingsGoalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_sourceAccountId` ON `savings_contributions` (`sourceAccountId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_linkedTransactionId` ON `savings_contributions` (`linkedTransactionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_savings_contributions_contributionDate` ON `savings_contributions` (`contributionDate`)")

        // compras planificadas; no crean gasto hasta registrarse
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `planned_purchases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `estimatedCostCents` INTEGER NOT NULL, `categoryId` INTEGER, `savingsGoalId` INTEGER, " +
                "`targetDate` INTEGER, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `description` TEXT, `vendor` TEXT, " +
                "`purchasedTransactionId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , " +
                "FOREIGN KEY(`savingsGoalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , " +
                "FOREIGN KEY(`purchasedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_purchases_categoryId` ON `planned_purchases` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_purchases_savingsGoalId` ON `planned_purchases` (`savingsGoalId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_purchases_status` ON `planned_purchases` (`status`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_purchases_priority` ON `planned_purchases` (`priority`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_planned_purchases_purchasedTransactionId` ON `planned_purchases` (`purchasedTransactionId`)")
    }
}

// migracion no destructiva de la version 5 a la 6
// agrega la tabla de adjuntos; el contenido real vive cifrado en almacenamiento privado, no en la base
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // metadatos de comprobantes y documentos ligados a un movimiento
        // al borrar el movimiento la fila se elimina en cascada y el caso de uso limpia el archivo
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `attachments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`transactionId` INTEGER NOT NULL, `displayName` TEXT NOT NULL, `mimeType` TEXT NOT NULL, " +
                "`storedFileName` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `checksum` TEXT NOT NULL, " +
                "`attachmentType` TEXT NOT NULL, `previewAvailable` INTEGER NOT NULL, " +
                "`createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, " +
                "FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_transactionId` ON `attachments` (`transactionId`)")
    }
}
