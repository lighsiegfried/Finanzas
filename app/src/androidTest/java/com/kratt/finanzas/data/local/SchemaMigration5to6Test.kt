package com.kratt.finanzas.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.security.SqlCipherNative
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// valida la migracion directa 5 a 6 sobre una base cifrada real: crea la tabla de adjuntos y preserva los datos
@RunWith(AndroidJUnit4::class)
class SchemaMigration5to6Test {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "migv5test.db"
    private val passphrase = ByteArray(32) { (it + 6).toByte() }
    private val dbFile: File get() = context.getDatabasePath(dbName)

    // esquema completo de la version 5: tablas de la v4 mas las cuatro tablas nuevas de la v5
    private val v5Schema = listOf(
        "CREATE TABLE IF NOT EXISTS `accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `type` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `initialBalanceCents` INTEGER NOT NULL, `creditLimitCents` INTEGER, `lastFourDigits` TEXT, `description` TEXT, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `iconKey` TEXT NOT NULL, `colorKey` TEXT, `isDefault` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `destinationAccountId` INTEGER, `categoryId` INTEGER, `type` TEXT NOT NULL, `amountCents` INTEGER NOT NULL, `description` TEXT, `transactionDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `originKey` TEXT, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`destinationAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_transactions_accountId` ON `transactions` (`accountId`)",
        "CREATE INDEX IF NOT EXISTS `index_transactions_destinationAccountId` ON `transactions` (`destinationAccountId`)",
        "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId` ON `transactions` (`categoryId`)",
        "CREATE INDEX IF NOT EXISTS `index_transactions_transactionDate` ON `transactions` (`transactionDate`)",
        "CREATE INDEX IF NOT EXISTS `index_transactions_categoryId_transactionDate` ON `transactions` (`categoryId`, `transactionDate`)",
        "CREATE INDEX IF NOT EXISTS `index_transactions_accountId_transactionDate` ON `transactions` (`accountId`, `transactionDate`)",
        "CREATE TABLE IF NOT EXISTS `installment_plans` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, `totalAmountCents` INTEGER NOT NULL, `installmentCount` INTEGER NOT NULL, `installmentAmountCents` INTEGER NOT NULL, `firstDueDate` INTEGER NOT NULL, `frequency` TEXT NOT NULL, `paidInstallments` INTEGER NOT NULL, `status` TEXT NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_installment_plans_accountId` ON `installment_plans` (`accountId`)",
        "CREATE INDEX IF NOT EXISTS `index_installment_plans_categoryId` ON `installment_plans` (`categoryId`)",
        "CREATE TABLE IF NOT EXISTS `installment_occurrences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `installmentPlanId` INTEGER NOT NULL, `sequenceNumber` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `status` TEXT NOT NULL, `paidTransactionId` INTEGER, `paidAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`installmentPlanId`) REFERENCES `installment_plans`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`paidTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_installment_occurrences_installmentPlanId_sequenceNumber` ON `installment_occurrences` (`installmentPlanId`, `sequenceNumber`)",
        "CREATE INDEX IF NOT EXISTS `index_installment_occurrences_dueDate` ON `installment_occurrences` (`dueDate`)",
        "CREATE INDEX IF NOT EXISTS `index_installment_occurrences_status` ON `installment_occurrences` (`status`)",
        "CREATE INDEX IF NOT EXISTS `index_installment_occurrences_paidTransactionId` ON `installment_occurrences` (`paidTransactionId`)",
        "CREATE TABLE IF NOT EXISTS `recurring_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `categoryId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `recurrenceType` TEXT NOT NULL, `interval` INTEGER NOT NULL, `startDate` INTEGER NOT NULL, `endDate` INTEGER, `nextOccurrenceDate` INTEGER NOT NULL, `postingMode` TEXT NOT NULL, `isActive` INTEGER NOT NULL, `description` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_recurring_templates_accountId` ON `recurring_templates` (`accountId`)",
        "CREATE INDEX IF NOT EXISTS `index_recurring_templates_categoryId` ON `recurring_templates` (`categoryId`)",
        "CREATE TABLE IF NOT EXISTS `recurring_occurrences` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `recurringTemplateId` INTEGER NOT NULL, `scheduledDate` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `status` TEXT NOT NULL, `generatedTransactionId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`recurringTemplateId`) REFERENCES `recurring_templates`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`generatedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurring_occurrences_recurringTemplateId_scheduledDate` ON `recurring_occurrences` (`recurringTemplateId`, `scheduledDate`)",
        "CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_scheduledDate` ON `recurring_occurrences` (`scheduledDate`)",
        "CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_status` ON `recurring_occurrences` (`status`)",
        "CREATE INDEX IF NOT EXISTS `index_recurring_occurrences_generatedTransactionId` ON `recurring_occurrences` (`generatedTransactionId`)",
        "CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `year` INTEGER NOT NULL, `month` INTEGER NOT NULL, `categoryId` INTEGER, `limitAmountCents` INTEGER NOT NULL, `warningPercentage` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_budgets_year_month_categoryId` ON `budgets` (`year`, `month`, `categoryId`)",
        "CREATE INDEX IF NOT EXISTS `index_budgets_year_month` ON `budgets` (`year`, `month`)",
        "CREATE INDEX IF NOT EXISTS `index_budgets_categoryId` ON `budgets` (`categoryId`)",
        "CREATE TABLE IF NOT EXISTS `transaction_templates` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `accountId` INTEGER NOT NULL, `destinationAccountId` INTEGER, `categoryId` INTEGER, `defaultAmountCents` INTEGER, `description` TEXT, `isFavorite` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `lastUsedAt` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`destinationAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_transaction_templates_accountId` ON `transaction_templates` (`accountId`)",
        "CREATE INDEX IF NOT EXISTS `index_transaction_templates_destinationAccountId` ON `transaction_templates` (`destinationAccountId`)",
        "CREATE INDEX IF NOT EXISTS `index_transaction_templates_categoryId` ON `transaction_templates` (`categoryId`)",
        "CREATE INDEX IF NOT EXISTS `index_transaction_templates_isFavorite` ON `transaction_templates` (`isFavorite`)",
        "CREATE INDEX IF NOT EXISTS `index_transaction_templates_isActive` ON `transaction_templates` (`isActive`)",
        "CREATE TABLE IF NOT EXISTS `savings_goals` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `targetAmountCents` INTEGER NOT NULL, `linkedAccountId` INTEGER, `startDate` INTEGER NOT NULL, `targetDate` INTEGER, `status` TEXT NOT NULL, `description` TEXT, `iconKey` TEXT NOT NULL, `colorKey` TEXT NOT NULL, `isArchived` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`linkedAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_savings_goals_linkedAccountId` ON `savings_goals` (`linkedAccountId`)",
        "CREATE INDEX IF NOT EXISTS `index_savings_goals_status` ON `savings_goals` (`status`)",
        "CREATE INDEX IF NOT EXISTS `index_savings_goals_isArchived` ON `savings_goals` (`isArchived`)",
        "CREATE TABLE IF NOT EXISTS `savings_contributions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `savingsGoalId` INTEGER NOT NULL, `amountCents` INTEGER NOT NULL, `contributionDate` INTEGER NOT NULL, `sourceAccountId` INTEGER, `linkedTransactionId` INTEGER, `contributionType` TEXT NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`savingsGoalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`sourceAccountId`) REFERENCES `accounts`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`linkedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_savings_contributions_savingsGoalId` ON `savings_contributions` (`savingsGoalId`)",
        "CREATE INDEX IF NOT EXISTS `index_savings_contributions_sourceAccountId` ON `savings_contributions` (`sourceAccountId`)",
        "CREATE INDEX IF NOT EXISTS `index_savings_contributions_linkedTransactionId` ON `savings_contributions` (`linkedTransactionId`)",
        "CREATE INDEX IF NOT EXISTS `index_savings_contributions_contributionDate` ON `savings_contributions` (`contributionDate`)",
        "CREATE TABLE IF NOT EXISTS `planned_purchases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `estimatedCostCents` INTEGER NOT NULL, `categoryId` INTEGER, `savingsGoalId` INTEGER, `targetDate` INTEGER, `priority` TEXT NOT NULL, `status` TEXT NOT NULL, `description` TEXT, `vendor` TEXT, `purchasedTransactionId` INTEGER, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT , FOREIGN KEY(`savingsGoalId`) REFERENCES `savings_goals`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL , FOREIGN KEY(`purchasedTransactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT )",
        "CREATE INDEX IF NOT EXISTS `index_planned_purchases_categoryId` ON `planned_purchases` (`categoryId`)",
        "CREATE INDEX IF NOT EXISTS `index_planned_purchases_savingsGoalId` ON `planned_purchases` (`savingsGoalId`)",
        "CREATE INDEX IF NOT EXISTS `index_planned_purchases_status` ON `planned_purchases` (`status`)",
        "CREATE INDEX IF NOT EXISTS `index_planned_purchases_priority` ON `planned_purchases` (`priority`)",
        "CREATE INDEX IF NOT EXISTS `index_planned_purchases_purchasedTransactionId` ON `planned_purchases` (`purchasedTransactionId`)",
    )

    @Before fun setUp() = cleanAll()
    @After fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"), File(dbFile.path + "-journal")).forEach { it.delete() }
    }

    private fun createV5Fixture() {
        SqlCipherNative.ensureLoaded()
        dbFile.parentFile?.mkdirs()
        val db = CipherDatabase.openOrCreateDatabase(dbFile, passphrase, null, null)
        try {
            db.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }
            v5Schema.forEach { db.execSQL(it) }
            db.execSQL("INSERT INTO accounts VALUES (1,'BAM','BANK_ACCOUNT','GTQ',50000,NULL,NULL,NULL,1,100,100)")
            db.execSQL("INSERT INTO accounts VALUES (2,'Ahorro','SAVINGS','GTQ',0,NULL,NULL,NULL,1,100,100)")
            db.execSQL("INSERT INTO categories VALUES (1,'Alimentación','EXPENSE','food',NULL,1,1,100,100)")
            db.execSQL("INSERT INTO transactions VALUES (1,1,NULL,1,'EXPENSE',85000,'Compra',20000,100,100,NULL)")
            db.execSQL("INSERT INTO savings_goals VALUES (1,'Fondo',500000,2,20000,NULL,'ACTIVE',NULL,'s','g',0,1,1)")
            db.execSQL("INSERT INTO savings_contributions VALUES (1,1,120000,20000,NULL,NULL,'MANUAL_TRACKING',NULL,1,1)")
            db.execSQL("PRAGMA user_version = 5")
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesEncryptedV5ToV6_preservingData_andCreatingAttachmentsTable() {
        createV5Fixture()
        val room = AppDatabase.build(context, passphrase.copyOf(), dbName)
        try {
            runBlocking {
                // datos previos preservados a traves de la migracion 5 a 6
                assertEquals(2, room.accountDao().observeAll().first().size)
                assertEquals(85_000L, room.transactionDao().findById(1)!!.amountCents)
                assertEquals(120_000L, room.savingsContributionDao().totalByGoal(1))
                // la tabla nueva de adjuntos existe y se puede consultar
                assertEquals(0, room.attachmentDao().observeTotalCount().first())
            }
        } finally {
            room.close()
        }
    }
}
