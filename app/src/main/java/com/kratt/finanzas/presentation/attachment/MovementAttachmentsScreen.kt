package com.kratt.finanzas.presentation.attachment

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kratt.finanzas.R
import com.kratt.finanzas.common.TestTags
import com.kratt.finanzas.domain.model.Attachment
import com.kratt.finanzas.domain.model.AttachmentType
import com.kratt.finanzas.domain.usecase.OcrResult
import com.kratt.finanzas.presentation.common.containerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale

@Composable
fun MovementAttachmentsRoute(
    transactionId: Long,
    onBack: () -> Unit,
) {
    val viewModel = containerViewModel(key = "attachments_$transactionId") {
        AttachmentsViewModel(
            transactionId,
            it.observeAttachments,
            it.addAttachment,
            it.deleteAttachment,
            it.readAttachment,
            it.runOcr,
        )
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // archivo temporal de la camara pendiente de importar
    var cameraTemp by remember { mutableStateOf<File?>(null) }

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val file = cameraTemp
        if (success && file != null) {
            viewModel.add(
                displayName = file.name,
                mimeType = "image/jpeg",
                requestedType = AttachmentType.RECEIPT_IMAGE,
                openStream = { FileInputStream(file) },
                onFinished = { file.delete(); cameraTemp = null },
            )
        } else {
            file?.delete()
            cameraTemp = null
        }
    }

    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri) ?: "application/octet-stream"
            val name = queryDisplayName(resolver, uri) ?: "adjunto"
            val requested = if (mime == "application/pdf") AttachmentType.DOCUMENT_PDF else AttachmentType.SUPPORT_IMAGE
            viewModel.add(name, mime, requested, openStream = { resolver.openInputStream(uri) })
        }
    }

    MovementAttachmentsScreen(
        state = state,
        onBack = onBack,
        onTakePhoto = {
            // crea el archivo temporal y abre la camara del sistema
            try {
                val file = createCameraTempFile(context)
                cameraTemp = file
                takePhoto.launch(cameraUri(context, file))
            } catch (e: Exception) {
                cameraTemp?.delete()
                cameraTemp = null
                viewModel.showError(R.string.attachment_camera_unavailable)
            }
        },
        onPickFile = {
            try {
                pickFile.launch(arrayOf("image/*", "application/pdf"))
            } catch (e: Exception) {
                viewModel.showError(R.string.error_attachment_generic)
            }
        },
        onDelete = viewModel::delete,
        onMessageShown = viewModel::onMessageShown,
        decryptBytes = viewModel::decryptBytes,
        decryptToFile = viewModel::decryptToFile,
        ocrAvailable = viewModel.ocrAvailable,
        onRunOcr = viewModel::readReceipt,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementAttachmentsScreen(
    state: AttachmentsUiState,
    onBack: () -> Unit,
    onTakePhoto: () -> Unit,
    onPickFile: () -> Unit,
    onDelete: (Long) -> Unit,
    onMessageShown: () -> Unit,
    decryptBytes: suspend (Attachment) -> ByteArray?,
    decryptToFile: suspend (Attachment) -> File?,
    ocrAvailable: Boolean = false,
    onRunOcr: suspend (Attachment) -> com.kratt.finanzas.domain.usecase.OcrResult =
        { com.kratt.finanzas.domain.usecase.OcrResult.Unavailable },
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<Attachment?>(null) }
    var previewing by remember { mutableStateOf<Attachment?>(null) }

    LaunchedEffect(state.messageRes, state.errorRes) {
        val res = state.messageRes ?: state.errorRes
        if (res != null) {
            snackbarHostState.showSnackbar(context.getString(res))
            onMessageShown()
        }
    }

    Scaffold(
        modifier = Modifier.testTag(TestTags.ATTACHMENTS_SCREEN),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.attachments_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onTakePhoto,
                    enabled = !state.working,
                    modifier = Modifier.weight(1f).testTag(TestTags.ATTACHMENT_TAKE_PHOTO_BUTTON),
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.attachment_take_photo))
                }
                OutlinedButton(
                    onClick = onPickFile,
                    enabled = !state.working,
                    modifier = Modifier.weight(1f).testTag(TestTags.ATTACHMENT_PICK_FILE_BUTTON),
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.attachment_pick_file))
                }
            }

            if (state.working) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(modifier = Modifier.weight(1f))
                    Text(stringResource(R.string.attachment_importing), style = MaterialTheme.typography.bodySmall)
                }
            }

            if (state.attachments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.attachments_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.attachments, key = { it.id }) { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            decryptBytes = decryptBytes,
                            onOpen = { previewing = attachment },
                            onDelete = { pendingDelete = attachment },
                        )
                    }
                }
            }
        }
    }

    pendingDelete?.let { attachment ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.attachment_delete)) },
            text = { Text(stringResource(R.string.attachment_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(attachment.id)
                        pendingDelete = null
                    },
                    modifier = Modifier.testTag(TestTags.CONFIRM_ATTACHMENT_DELETE_BUTTON),
                ) { Text(stringResource(R.string.delete_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel_action)) }
            },
        )
    }

    previewing?.let { attachment ->
        AttachmentPreviewDialog(
            attachment = attachment,
            decryptBytes = decryptBytes,
            decryptToFile = decryptToFile,
            ocrAvailable = ocrAvailable,
            onRunOcr = onRunOcr,
            onClose = { previewing = null },
        )
    }
}

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    decryptBytes: suspend (Attachment) -> ByteArray?,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 8.dp)
            .testTag(TestTags.ATTACHMENT_LIST_ITEM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (attachment.isImage) {
            val thumb by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, attachment.id) {
                value = withContext(Dispatchers.Default) {
                    val bytes = decryptBytes(attachment) ?: return@withContext null
                    AttachmentRendering.decodeSampledImage(bytes, 160, 160)?.asImageBitmap()
                }
            }
            val bitmap = thumb
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.cd_attachment_thumbnail),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(48.dp))
            }
        } else {
            Icon(
                Icons.Filled.Description,
                contentDescription = stringResource(R.string.cd_attachment_document),
                modifier = Modifier.size(48.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val typeLabel = if (attachment.isPdf) {
                stringResource(R.string.attachment_label_document)
            } else {
                stringResource(R.string.attachment_label_receipt)
            }
            Text(
                text = "$typeLabel · ${formatSize(attachment.sizeBytes)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.testTag(TestTags.ATTACHMENT_DELETE_BUTTON)) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.attachment_delete))
        }
    }
}

@Composable
private fun AttachmentPreviewDialog(
    attachment: Attachment,
    decryptBytes: suspend (Attachment) -> ByteArray?,
    decryptToFile: suspend (Attachment) -> File?,
    ocrAvailable: Boolean,
    onRunOcr: suspend (Attachment) -> OcrResult,
    onClose: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize().testTag(TestTags.ATTACHMENT_PREVIEW)) {
            Box(modifier = Modifier.fillMaxSize()) {
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(scroll).padding(top = 56.dp, start = 8.dp, end = 8.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (attachment.isPdf) {
                        PdfPreviewContent(attachment, decryptToFile)
                    } else {
                        ImagePreviewContent(attachment, decryptBytes, ocrAvailable, onRunOcr)
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.attachment_close))
                }
            }
        }
    }
}

@Composable
private fun ImagePreviewContent(
    attachment: Attachment,
    decryptBytes: suspend (Attachment) -> ByteArray?,
    ocrAvailable: Boolean,
    onRunOcr: suspend (Attachment) -> OcrResult,
) {
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, attachment.id) {
        value = withContext(Dispatchers.Default) {
            val bytes = decryptBytes(attachment) ?: return@withContext null
            AttachmentRendering.decodeSampledImage(bytes, 1440, 1440)?.asImageBitmap()
        }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = attachment.displayName, modifier = Modifier.fillMaxWidth())
    } else {
        PreviewLoadingOrError(attachment.id)
    }

    // lectura local del comprobante: solo propone datos para revisar, nunca guarda el movimiento
    val scope = rememberCoroutineScope()
    var ocrRunning by remember(attachment.id) { mutableStateOf(false) }
    var ocrResult by remember(attachment.id) { mutableStateOf<OcrResult?>(null) }
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            if (!ocrRunning) {
                ocrRunning = true
                scope.launch {
                    ocrResult = withContext(Dispatchers.Default) { onRunOcr(attachment) }
                    ocrRunning = false
                }
            }
        },
        enabled = !ocrRunning,
        modifier = Modifier.testTag(TestTags.ATTACHMENT_OCR_BUTTON),
    ) {
        Text(stringResource(R.string.ocr_read_receipt))
    }
    if (ocrRunning) {
        Text(stringResource(R.string.ocr_reading), style = MaterialTheme.typography.bodySmall)
    }
    ocrResult?.let { OcrResultSection(it) }
}

// muestra las sugerencias detectadas para revision; el usuario decide, nada se guarda solo
@Composable
private fun OcrResultSection(result: OcrResult) {
    Spacer(Modifier.height(8.dp))
    when (result) {
        is OcrResult.Detected -> {
            val s = result.suggestions
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(stringResource(R.string.ocr_detected_fields), style = MaterialTheme.typography.titleSmall)
                s.merchant?.let { OcrField(stringResource(R.string.ocr_field_merchant), it) }
                s.dateEpochDay?.let {
                    OcrField(stringResource(R.string.ocr_field_date), com.kratt.finanzas.common.ShortDateFormatter.format(java.time.LocalDate.ofEpochDay(it)))
                }
                s.totalCents?.let {
                    OcrField(stringResource(R.string.ocr_field_total), com.kratt.finanzas.common.CurrencyFormatter.format(it))
                }
                s.invoiceNumber?.let { OcrField(stringResource(R.string.ocr_field_invoice), it) }
                Text(
                    stringResource(R.string.ocr_review_before_saving),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OcrResult.NoData, OcrResult.Unavailable ->
            Text(stringResource(R.string.ocr_no_data), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OcrField(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PdfPreviewContent(
    attachment: Attachment,
    decryptToFile: suspend (Attachment) -> File?,
) {
    // descifra a un archivo temporal y lo borra al cerrar la vista previa
    val file by produceState<File?>(null, attachment.id) {
        value = withContext(Dispatchers.Default) { decryptToFile(attachment) }
        awaitDispose { value?.delete() }
    }
    val pdf = file
    if (pdf == null) {
        PreviewLoadingOrError(attachment.id)
        return
    }
    val pageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, pdf.path) {
        value = withContext(Dispatchers.Default) {
            runCatching { AttachmentRendering.renderPdfPage(pdf, 0, 1440)?.asImageBitmap() }.getOrNull()
        }
    }
    val bitmap = pageBitmap
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = attachment.displayName, modifier = Modifier.fillMaxWidth())
    } else {
        PreviewLoadingOrError(attachment.id)
    }
}

@Composable
private fun PreviewLoadingOrError(key: Long) {
    // muestra un indicador breve; si nunca llega el contenido, avisa sin exponer detalles
    var timedOut by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        kotlinx.coroutines.delay(4000)
        timedOut = true
    }
    Column(
        modifier = Modifier.fillMaxWidth().height(240.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (timedOut) {
            Text(stringResource(R.string.attachment_preview_unavailable))
        } else {
            CircularProgressIndicator()
        }
    }
}

// formatea el tamano del archivo en unidades legibles
private fun formatSize(bytes: Long): String {
    val kb = 1024.0
    val mb = kb * 1024
    val locale = Locale.forLanguageTag("es-GT")
    return when {
        bytes >= mb -> String.format(locale, "%.1f MB", bytes / mb)
        bytes >= kb -> String.format(locale, "%.0f KB", bytes / kb)
        else -> "$bytes B"
    }
}

// nombre visible del documento elegido en el selector del sistema
private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return null
}

private fun createCameraTempFile(context: android.content.Context): File {
    val dir = File(context.cacheDir, "camera").apply { mkdirs() }
    return File.createTempFile("cap_", ".jpg", dir)
}

private fun cameraUri(context: android.content.Context, file: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
