package com.sktech.wastetrack.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfExporter {

    fun exportCertificate(context: Context, certificate: CertificateEntity) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = document.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("WasteTrack ESG Certificate", 595f / 2, 100f, paint)

        // Certificate Details
        paint.textSize = 16f
        paint.textAlign = Paint.Align.LEFT
        var yPos = 160f
        val lineSpacing = 30f

        canvas.drawText("Certificate ID: ${certificate.id}", 50f, yPos, paint)
        yPos += lineSpacing
        canvas.drawText("Factory ID: ${certificate.factoryId}", 50f, yPos, paint)
        yPos += lineSpacing
        canvas.drawText("Type: ${certificate.type}", 50f, yPos, paint)
        yPos += lineSpacing
        canvas.drawText("Generated At: ${DateUtils.formatDate(certificate.generatedAt)}", 50f, yPos, paint)
        yPos += lineSpacing
        canvas.drawText("Status: ${certificate.status}", 50f, yPos, paint)

        document.finishPage(page)

        // Save file
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, "WasteTrack_Certificate_${certificate.id}.pdf")

        try {
            document.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Certificate saved to Downloads", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
        }
    }
}
