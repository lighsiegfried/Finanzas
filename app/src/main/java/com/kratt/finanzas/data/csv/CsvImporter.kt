package com.kratt.finanzas.data.csv

import androidx.room.withTransaction
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.domain.model.Transaction
import com.kratt.finanzas.domain.model.TransactionType
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.TextNormalizer
import com.kratt.finanzas.domain.usecase.csv.CsvImportFormat
import com.kratt.finanzas.domain.usecase.csv.CsvImportParser
import com.kratt.finanzas.domain.usecase.csv.CsvImportValidator
import com.kratt.finanzas.domain.usecase.csv.ImportContext
import com.kratt.finanzas.domain.usecase.csv.ImportFileError
import com.kratt.finanzas.domain.usecase.csv.ImportPreview
import com.kratt.finanzas.domain.usecase.csv.ImportedMovement
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlinx.coroutines.flow.first

// importa movimientos desde un csv de forma local y segura, sin tocar nada hasta confirmar
class CsvImporter(
    private val database: AppDatabase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {

    // lee y valida el archivo sin modificar la base; controla el tamano antes de cargarlo
    suspend fun preview(input: InputStream): ImportPreview {
        val bytes = readBounded(input, CsvImportFormat.MAX_FILE_BYTES)
            ?: return ImportPreview(fileError = ImportFileError.TOO_LARGE)
        val text = bytes.toString(Charsets.UTF_8)
        return CsvImportValidator.buildPreview(CsvImportParser.parse(text), buildContext())
    }

    // inserta los movimientos elegidos en una sola transaccion; si algo falla se revierte todo
    suspend fun commit(movements: List<ImportedMovement>): Int = database.withTransaction {
        val now = nowMillis()
        movements.forEach { movement ->
            transactionRepository.insert(
                Transaction(
                    id = 0L,
                    accountId = movement.accountId,
                    type = movement.type,
                    amountCents = movement.amountCents,
                    description = movement.description,
                    date = movement.date,
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    categoryId = movement.categoryId,
                    destinationAccountId = movement.destinationAccountId,
                ),
            )
        }
        movements.size
    }

    // arma la referencia de cuentas y categorias activas y las llaves de lo ya guardado
    private suspend fun buildContext(): ImportContext {
        val accounts = accountRepository.observeActiveAccounts().first()
            .associate { TextNormalizer.normalize(it.name) to it.id }
        val expense = categoryRepository.observeActiveByType(TransactionType.EXPENSE).first()
            .associate { TextNormalizer.normalize(it.name) to it.id }
        val income = categoryRepository.observeActiveByType(TransactionType.INCOME).first()
            .associate { TextNormalizer.normalize(it.name) to it.id }
        val existingKeys = transactionRepository.observeAllTransactions().first()
            .map { CsvImportValidator.keyOf(it.type, it.date, it.amountCents, it.accountId, it.categoryId, it.destinationAccountId, it.description) }
            .toSet()
        return ImportContext(accounts, expense, income, existingKeys)
    }

    // lee hasta el maximo permitido; si se pasa devuelve null para no cargar algo enorme
    private fun readBounded(input: InputStream, maxBytes: Long): ByteArray? {
        val buffer = ByteArrayOutputStream()
        val chunk = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = input.read(chunk)
            if (read == -1) break
            total += read
            if (total > maxBytes) return null
            buffer.write(chunk, 0, read)
        }
        return buffer.toByteArray()
    }
}
