package com.kratt.finanzas.data.repository

import com.kratt.finanzas.data.local.TransactionWithNames
import com.kratt.finanzas.data.local.entity.AccountEntity
import com.kratt.finanzas.data.local.entity.CategoryEntity
import com.kratt.finanzas.data.local.entity.TransactionEntity
import com.kratt.finanzas.domain.model.Account
import com.kratt.finanzas.domain.model.Category
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionListItem
import java.time.LocalDate

// traducciones entre las tablas de room y los modelos de dominio

fun AccountEntity.toDomain(): Account = Account(
    id = id,
    name = name,
    type = type,
    currencyCode = currencyCode,
    initialBalanceCents = initialBalanceCents,
    isActive = isActive,
    creditLimitCents = creditLimitCents,
    lastFourDigits = lastFourDigits,
    description = description,
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt,
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    type = type,
    currencyCode = currencyCode,
    initialBalanceCents = initialBalanceCents,
    creditLimitCents = creditLimitCents,
    lastFourDigits = lastFourDigits,
    description = description,
    isActive = isActive,
    createdAt = createdAtMillis,
    updatedAt = updatedAtMillis,
)

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    transactionType = transactionType,
    iconKey = iconKey,
    isDefault = isDefault,
    isActive = isActive,
    colorKey = colorKey,
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt,
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    transactionType = transactionType,
    iconKey = iconKey,
    colorKey = colorKey,
    isDefault = isDefault,
    isActive = isActive,
    createdAt = createdAtMillis,
    updatedAt = updatedAtMillis,
)

fun TransactionEntity.toDomain(): Transaction = Transaction(
    id = id,
    accountId = accountId,
    type = type,
    amountCents = amountCents,
    description = description,
    date = LocalDate.ofEpochDay(transactionDate),
    createdAtMillis = createdAt,
    updatedAtMillis = updatedAt,
    categoryId = categoryId,
    destinationAccountId = destinationAccountId,
    originKey = originKey,
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    accountId = accountId,
    destinationAccountId = destinationAccountId,
    categoryId = categoryId,
    type = type,
    amountCents = amountCents,
    description = description,
    transactionDate = date.toEpochDay(),
    createdAt = createdAtMillis,
    updatedAt = updatedAtMillis,
    // conserva la marca de origen para no perder el vinculo al actualizar
    originKey = originKey,
)

fun TransactionWithNames.toDomain(): TransactionListItem = TransactionListItem(
    id = id,
    type = type,
    amountCents = amountCents,
    description = description,
    categoryName = categoryName,
    accountName = accountName,
    destinationAccountName = destinationAccountName,
    accountId = accountId,
    categoryId = categoryId,
    destinationAccountId = destinationAccountId,
    date = LocalDate.ofEpochDay(transactionDate),
)
