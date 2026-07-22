package com.kratt.finanzas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kratt.finanzas.data.local.dao.AccountDao
import com.kratt.finanzas.data.local.dao.AttachmentDao
import com.kratt.finanzas.data.local.dao.BudgetDao
import com.kratt.finanzas.data.local.dao.CategoryDao
import com.kratt.finanzas.data.local.dao.InstallmentDao
import com.kratt.finanzas.data.local.dao.PlannedPurchaseDao
import com.kratt.finanzas.data.local.dao.RecurringDao
import com.kratt.finanzas.data.local.dao.SavingsContributionDao
import com.kratt.finanzas.data.local.dao.SavingsGoalDao
import com.kratt.finanzas.data.local.dao.TransactionDao
import com.kratt.finanzas.data.local.dao.TransactionTemplateDao
import com.kratt.finanzas.data.local.entity.AccountEntity
import com.kratt.finanzas.data.local.entity.AttachmentEntity
import com.kratt.finanzas.data.local.entity.BudgetEntity
import com.kratt.finanzas.data.local.entity.CategoryEntity
import com.kratt.finanzas.data.local.entity.InstallmentOccurrenceEntity
import com.kratt.finanzas.data.local.entity.InstallmentPlanEntity
import com.kratt.finanzas.data.local.entity.PlannedPurchaseEntity
import com.kratt.finanzas.data.local.entity.RecurringOccurrenceEntity
import com.kratt.finanzas.data.local.entity.RecurringTemplateEntity
import com.kratt.finanzas.data.local.entity.SavingsContributionEntity
import com.kratt.finanzas.data.local.entity.SavingsGoalEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.data.local.entity.TransactionTemplateEntity
import com.kratt.finanzas.data.security.SqlCipherNative
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        InstallmentPlanEntity::class,
        InstallmentOccurrenceEntity::class,
        RecurringTemplateEntity::class,
        RecurringOccurrenceEntity::class,
        BudgetEntity::class,
        TransactionTemplateEntity::class,
        SavingsGoalEntity::class,
        SavingsContributionEntity::class,
        PlannedPurchaseEntity::class,
        AttachmentEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun installmentDao(): InstallmentDao
    abstract fun recurringDao(): RecurringDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionTemplateDao(): TransactionTemplateDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun savingsContributionDao(): SavingsContributionDao
    abstract fun plannedPurchaseDao(): PlannedPurchaseDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        const val NAME = "finanzas.db"

        // crea la base cifrada con sqlcipher usando la frase indicada
        // sqlcipher deriva la clave de los bytes, la frase nunca se pasa como texto
        fun build(context: Context, passphrase: ByteArray, name: String = NAME): AppDatabase {
            SqlCipherNative.ensureLoaded()
            val factory = SupportOpenHelperFactory(passphrase)
            return Room.databaseBuilder(context, AppDatabase::class.java, name)
                .openHelperFactory(factory)
                .addCallback(DefaultDataCallback())
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .build()
        }
    }
}
