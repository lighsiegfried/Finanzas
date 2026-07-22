package com.kratt.finanzas.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kratt.finanzas.data.local.entity.PlannedPurchaseEntity
import com.kratt.finanzas.data.local.entity.SavingsContributionEntity
import com.kratt.finanzas.data.local.entity.SavingsGoalEntity
import com.kratt.finanzas.data.local.entity.TransactionTemplateEntity
import com.kratt.finanzas.data.security.SqlCipherNative
import com.kratt.finanzas.domain.model.ContributionType
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.model.TransactionType
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SQLiteDatabase as CipherDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// valida la migracion 4 a 5 sobre una base cifrada real, preservando datos y creando las tablas nuevas
@RunWith(AndroidJUnit4::class)
class SchemaMigration4to5Test {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val dbName = "migv4test.db"
    private val passphrase = ByteArray(32) { (it + 5).toByte() }
    private val dbFile: File get() = context.getDatabasePath(dbName)

    private val v4Schema = listOf(
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
    )

    @Before fun setUp() = cleanAll()
    @After fun tearDown() = cleanAll()

    private fun cleanAll() {
        listOf(dbFile, File(dbFile.path + "-wal"), File(dbFile.path + "-shm"), File(dbFile.path + "-journal")).forEach { it.delete() }
    }

    private fun createV4Fixture() {
        SqlCipherNative.ensureLoaded()
        dbFile.parentFile?.mkdirs()
        val db = CipherDatabase.openOrCreateDatabase(dbFile, passphrase, null, null)
        try {
            db.rawQuery("PRAGMA journal_mode=DELETE", null).use { it.moveToFirst() }
            v4Schema.forEach { db.execSQL(it) }
            db.execSQL("INSERT INTO accounts VALUES (1,'BAM','BANK_ACCOUNT','GTQ',50000,NULL,NULL,NULL,1,100,100)")
            db.execSQL("INSERT INTO accounts VALUES (2,'Ahorro','SAVINGS','GTQ',0,NULL,NULL,NULL,1,100,100)")
            db.execSQL("INSERT INTO categories VALUES (1,'Alimentación','EXPENSE','food',NULL,1,1,100,100)")
            db.execSQL("INSERT INTO transactions VALUES (1,1,NULL,1,'EXPENSE',85000,'Compra',20000,100,100,NULL)")
            db.execSQL("PRAGMA user_version = 4")
        } finally {
            db.close()
        }
    }

    @Test
    fun migratesEncryptedV4ToV5_preservingData_andCreatingNewTables() {
        createV4Fixture()
        val room = AppDatabase.build(context, passphrase.copyOf(), dbName)
        try {
            runBlocking {
                // datos previos preservados
                assertEquals(2, room.accountDao().observeAll().first().size)
                assertEquals(85_000L, room.transactionDao().findById(1)!!.amountCents)

                // plantilla nueva
                val templateId = room.transactionTemplateDao().insert(
                    TransactionTemplateEntity(name = "Gasolina", transactionType = TransactionType.EXPENSE, accountId = 1, categoryId = 1, isFavorite = true, isActive = true, createdAt = 1, updatedAt = 1),
                )
                assertEquals(1, room.transactionTemplateDao().observeFavorites().first().size)

                // meta de ahorro nueva y aporte
                val goalId = room.savingsGoalDao().insert(
                    SavingsGoalEntity(name = "Fondo", targetAmountCents = 500_000, linkedAccountId = 2, startDate = 20000, status = SavingsGoalStatus.ACTIVE, iconKey = "s", colorKey = "g", isArchived = false, createdAt = 1, updatedAt = 1),
                )
                room.savingsContributionDao().insert(
                    SavingsContributionEntity(savingsGoalId = goalId, amountCents = 120_000, contributionDate = 20000, contributionType = ContributionType.MANUAL_TRACKING, createdAt = 1, updatedAt = 1),
                )
                assertEquals(120_000L, room.savingsContributionDao().totalByGoal(goalId))

                // compra planificada nueva ligada a la meta
                room.plannedPurchaseDao().insert(
                    PlannedPurchaseEntity(name = "Laptop", estimatedCostCents = 800_000, savingsGoalId = goalId, priority = PurchasePriority.HIGH, status = PurchaseStatus.PLANNING, createdAt = 1, updatedAt = 1),
                )
                assertEquals(1, room.plannedPurchaseDao().activeByGoal(goalId).size)
                assertEquals(templateId, room.transactionTemplateDao().findById(templateId)!!.id)
            }
        } finally {
            room.close()
        }
    }
}
