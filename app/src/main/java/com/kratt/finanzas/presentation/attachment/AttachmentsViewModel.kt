package com.kratt.finanzas.presentation.attachment

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kratt.finanzas.R
import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.model.AttachmentType
import com.kratt.finanzas.domain.usecase.AddAttachmentResult
import com.kratt.finanzas.domain.usecase.AddAttachmentUseCase
import com.kratt.finanzas.domain.usecase.AttachmentError
import com.kratt.finanzas.domain.usecase.DeleteAttachmentUseCase
import com.kratt.finanzas.domain.usecase.ObserveAttachmentsUseCase
import com.kratt.finanzas.domain.usecase.OcrResult
import com.kratt.finanzas.domain.usecase.ReadAttachmentUseCase
import com.kratt.finanzas.domain.usecase.RunOcrUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

// estado transitorio de la pantalla de adjuntos, aparte de la lista observada
private data class AttachmentsTransient(
    val working: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @StringRes val messageRes: Int? = null,
)

data class AttachmentsUiState(
    val attachments: List<Attachment> = emptyList(),
    val working: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @StringRes val messageRes: Int? = null,
)

class AttachmentsViewModel(
    private val transactionId: Long,
    observeAttachments: ObserveAttachmentsUseCase,
    private val addAttachment: AddAttachmentUseCase,
    private val deleteAttachment: DeleteAttachmentUseCase,
    private val readAttachment: ReadAttachmentUseCase,
    private val runOcr: RunOcrUseCase,
) : ViewModel() {

    // el ocr solo esta disponible si el motor local esta conectado
    val ocrAvailable: Boolean get() = runOcr.available

    // lee el comprobante de forma local y devuelve solo sugerencias para revisar, nunca guarda
    suspend fun readReceipt(attachment: Attachment): OcrResult {
        val bytes = readAttachment.bytes(attachment) ?: return OcrResult.NoData
        return runOcr.run(bytes)
    }

    private val transient = MutableStateFlow(AttachmentsTransient())

    val uiState: StateFlow<AttachmentsUiState> =
        combine(observeAttachments(transactionId), transient) { list, t ->
            AttachmentsUiState(
                attachments = list,
                working = t.working,
                errorRes = t.errorRes,
                messageRes = t.messageRes,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AttachmentsUiState())

    // adjunta desde una fuente que se abre y cierra dentro de la corrutina
    // evita adjuntos duplicados por toques repetidos con la bandera de trabajo
    fun add(
        displayName: String,
        mimeType: String,
        requestedType: AttachmentType,
        openStream: () -> InputStream?,
        onFinished: (Boolean) -> Unit = {},
    ) {
        if (transient.value.working) return
        transient.value = AttachmentsTransient(working = true)
        viewModelScope.launch {
            val input = try {
                openStream()
            } catch (e: Exception) {
                null
            }
            if (input == null) {
                transient.value = AttachmentsTransient(errorRes = R.string.error_attachment_unreadable)
                onFinished(false)
                return@launch
            }
            val result = input.use { addAttachment.add(transactionId, displayName, mimeType, requestedType, it) }
            transient.value = when (result) {
                is AddAttachmentResult.Success -> AttachmentsTransient(messageRes = R.string.attachment_added)
                is AddAttachmentResult.Failure -> AttachmentsTransient(errorRes = errorMessage(result.error))
            }
            onFinished(result is AddAttachmentResult.Success)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val ok = deleteAttachment.delete(id)
            transient.value = if (ok) {
                AttachmentsTransient(messageRes = R.string.attachment_deleted)
            } else {
                AttachmentsTransient(errorRes = R.string.error_attachment_generic)
            }
        }
    }

    fun onMessageShown() {
        transient.value = transient.value.copy(errorRes = null, messageRes = null)
    }

    // muestra un error puntual, por ejemplo cuando no hay camara disponible
    fun showError(@StringRes res: Int) {
        transient.value = AttachmentsTransient(errorRes = res)
    }

    // descifra solo cuando se necesita mostrar el adjunto
    suspend fun decryptBytes(attachment: Attachment): ByteArray? = readAttachment.bytes(attachment)

    suspend fun decryptToFile(attachment: Attachment): File? = readAttachment.toCacheFile(attachment)

    @StringRes
    private fun errorMessage(error: AttachmentError): Int = when (error) {
        AttachmentError.UNSUPPORTED_TYPE -> R.string.error_attachment_unsupported
        AttachmentError.TOO_LARGE -> R.string.error_attachment_too_large
        AttachmentError.EMPTY -> R.string.error_attachment_unreadable
        AttachmentError.UNREADABLE -> R.string.error_attachment_unreadable
        AttachmentError.NO_SPACE -> R.string.error_attachment_no_space
        AttachmentError.GENERIC -> R.string.error_attachment_generic
    }
}
