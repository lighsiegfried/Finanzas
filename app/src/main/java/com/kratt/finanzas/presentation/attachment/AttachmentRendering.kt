package com.kratt.finanzas.presentation.attachment

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.File

// utilidades para preparar imagenes y pdf descifrados solo cuando se van a mostrar
object AttachmentRendering {

    // decodifica una imagen con submuestreo para no cargar mas pixeles de los necesarios
    fun decodeSampledImage(bytes: ByteArray, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val opts = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        if (reqWidth <= 0 || reqHeight <= 0) return 1
        var sample = 1
        val halfWidth = width / 2
        val halfHeight = height / 2
        while (halfWidth / sample >= reqWidth && halfHeight / sample >= reqHeight) sample *= 2
        return sample
    }

    // cuenta las paginas de un pdf ya descifrado en un archivo temporal
    fun pdfPageCount(file: File): Int = openPdf(file).use { it.pageCount }

    // renderiza una pagina del pdf a un bitmap del ancho pedido
    fun renderPdfPage(file: File, index: Int, targetWidth: Int): Bitmap? {
        openPdf(file).use { renderer ->
            if (index < 0 || index >= renderer.pageCount) return null
            renderer.openPage(index).use { page ->
                val scale = if (page.width > 0) targetWidth.toFloat() / page.width else 1f
                val w = (page.width * scale).toInt().coerceAtLeast(1)
                val h = (page.height * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                // fondo blanco para paginas con transparencia
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return bitmap
            }
        }
    }

    private fun openPdf(file: File): PdfRenderer {
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return PdfRenderer(pfd)
    }
}
