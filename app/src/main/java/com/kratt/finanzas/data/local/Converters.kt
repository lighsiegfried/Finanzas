package com.kratt.finanzas.data.local

import androidx.room.TypeConverter
import com.kratt.finanzas.domain.model.AccountType
import com.kratt.finanzas.domain.model.AttachmentType
import com.kratt.finanzas.domain.model.ContributionType
import com.kratt.finanzas.domain.model.InstallmentFrequency
import com.kratt.finanzas.domain.model.InstallmentOccurrenceStatus
import com.kratt.finanzas.domain.model.InstallmentStatus
import com.kratt.finanzas.domain.model.PostingMode
import com.kratt.finanzas.domain.model.PurchasePriority
import com.kratt.finanzas.domain.model.PurchaseStatus
import com.kratt.finanzas.domain.model.RecurrenceType
import com.kratt.finanzas.domain.model.RecurringOccurrenceStatus
import com.kratt.finanzas.domain.model.SavingsGoalStatus
import com.kratt.finanzas.domain.model.TransactionType

// convierte los enums a texto para poder guardarlos en la base
class Converters {

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @TypeConverter
    fun fromInstallmentStatus(value: InstallmentStatus): String = value.name

    @TypeConverter
    fun toInstallmentStatus(value: String): InstallmentStatus = InstallmentStatus.valueOf(value)

    @TypeConverter
    fun fromInstallmentFrequency(value: InstallmentFrequency): String = value.name

    @TypeConverter
    fun toInstallmentFrequency(value: String): InstallmentFrequency = InstallmentFrequency.valueOf(value)

    @TypeConverter
    fun fromInstallmentOccurrenceStatus(value: InstallmentOccurrenceStatus): String = value.name

    @TypeConverter
    fun toInstallmentOccurrenceStatus(value: String): InstallmentOccurrenceStatus =
        InstallmentOccurrenceStatus.valueOf(value)

    @TypeConverter
    fun fromRecurrenceType(value: RecurrenceType): String = value.name

    @TypeConverter
    fun toRecurrenceType(value: String): RecurrenceType = RecurrenceType.valueOf(value)

    @TypeConverter
    fun fromPostingMode(value: PostingMode): String = value.name

    @TypeConverter
    fun toPostingMode(value: String): PostingMode = PostingMode.valueOf(value)

    @TypeConverter
    fun fromRecurringOccurrenceStatus(value: RecurringOccurrenceStatus): String = value.name

    @TypeConverter
    fun toRecurringOccurrenceStatus(value: String): RecurringOccurrenceStatus =
        RecurringOccurrenceStatus.valueOf(value)

    @TypeConverter
    fun fromSavingsGoalStatus(value: SavingsGoalStatus): String = value.name

    @TypeConverter
    fun toSavingsGoalStatus(value: String): SavingsGoalStatus = SavingsGoalStatus.valueOf(value)

    @TypeConverter
    fun fromContributionType(value: ContributionType): String = value.name

    @TypeConverter
    fun toContributionType(value: String): ContributionType = ContributionType.valueOf(value)

    @TypeConverter
    fun fromPurchasePriority(value: PurchasePriority): String = value.name

    @TypeConverter
    fun toPurchasePriority(value: String): PurchasePriority = PurchasePriority.valueOf(value)

    @TypeConverter
    fun fromPurchaseStatus(value: PurchaseStatus): String = value.name

    @TypeConverter
    fun toPurchaseStatus(value: String): PurchaseStatus = PurchaseStatus.valueOf(value)

    @TypeConverter
    fun fromAttachmentType(value: AttachmentType): String = value.name

    @TypeConverter
    fun toAttachmentType(value: String): AttachmentType = AttachmentType.valueOf(value)
}
