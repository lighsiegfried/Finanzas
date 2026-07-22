package com.kratt.finanzas.presentation.importer

import android.content.ContentResolver
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.data.csv.CsvImporter
import com.kratt.finanzas.domain.usecase.Csv
import com.kratt.finanzas.domain.usecase.csv.ImportPreview
import com.kratt.finanzas.domain.usecase.csv.ImportSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportUiState(
    val loading: Boolean = false,
    val preview: ImportPreview? = null,
    val summary: ImportSummary? = null,
    @StringRes val messageRes: Int? = null,
)

class ImportViewModel(
    private val csvImporter: CsvImporter,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    // lee y valida el archivo elegido; no cambia nada en la base
    fun onFileSelected(uri: Uri, resolver: ContentResolver) {
        _uiState.update { ImportUiState(loading = true) }
        viewModelScope.launch {
            val preview = withContext(Dispatchers.IO) {
                runCatching { resolver.openInputStream(uri)?.use { csvImporter.preview(it) } }.getOrNull()
            }
            if (preview == null) {
                _uiState.update { ImportUiState(messageRes = R.string.import_failed) }
            } else {
                _uiState.update { ImportUiState(preview = preview) }
            }
        }
    }

    // confirma la importacion; omite duplicados si el usuario lo pidio, todo en una transaccion
    fun onConfirmImport(skipDuplicates: Boolean) {
        val preview = _uiState.value.preview ?: return
        val duplicateRows = preview.duplicates.map { it.rowNumber }.toSet()
        val toImport = if (skipDuplicates) preview.valid.filterNot { it.rowNumber in duplicateRows } else preview.valid
        _uiState.update { it.copy(loading = true) }
        viewModelScope.launch {
            val imported = withContext(Dispatchers.IO) { runCatching { csvImporter.commit(toImport) }.getOrNull() }
            if (imported == null) {
                _uiState.update { ImportUiState(messageRes = R.string.import_failed) }
            } else {
                _uiState.update {
                    ImportUiState(
                        summary = ImportSummary(
                            imported = imported,
                            skippedDuplicates = preview.validCount - toImport.size,
                            errors = preview.errorCount,
                            duplicatesDetected = preview.duplicateCount,
                        ),
                    )
                }
            }
        }
    }

    // escribe una plantilla de ejemplo en el destino elegido
    fun onWriteTemplate(uri: Uri, resolver: ContentResolver) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    resolver.openOutputStream(uri)?.use { output ->
                        output.bufferedWriter(Charsets.UTF_8).use { writer ->
                            writer.write(Csv.BOM)
                            writer.write(TEMPLATE)
                        }
                    } != null
                }.getOrDefault(false)
            }
            if (!ok) _uiState.update { it.copy(messageRes = R.string.import_failed) }
        }
    }

    fun onCancel() { _uiState.update { ImportUiState(messageRes = R.string.import_cancelled) } }
    fun onMessageShown() { _uiState.update { it.copy(messageRes = null) } }
    fun reset() { _uiState.update { ImportUiState() } }

    private companion object {
        val TEMPLATE = buildString {
            append("tipo,fecha,descripcion,monto,cuenta,categoria,cuenta_destino\r\n")
            append("ingreso,2026-07-01,Salario,8000.00,Efectivo,Salario,\r\n")
            append("gasto,2026-07-05,Almuerzo,85.00,Efectivo,Alimentacion,\r\n")
            append("transferencia,2026-07-07,Ahorro,500.00,Efectivo,,Ahorro\r\n")
        }
    }
}
