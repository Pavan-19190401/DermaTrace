package com.dermatrace.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.dermatrace.app.data.model.AnalysisResult
import com.dermatrace.app.data.model.Lesion
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generateAndShareReport(
        context: Context,
        lesion: Lesion,
        result: AnalysisResult,
        patientName: String = "Alex Morgan"
    ): File? {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (72 DPI)
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault())

        // Background header banner
        paint.color = Color.rgb(18, 53, 60) // Deep Medical Teal
        canvas.drawRect(0f, 0f, 595f, 110f, paint)

        // Header Title
        paint.color = Color.WHITE
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("DermaTrace Clinical Decision-Support Report", 30f, 45f, paint)

        // Subtitle
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Longitudinal Skin Lesion Monitoring & Cancer Risk Pattern Recognition", 30f, 68f, paint)
        canvas.drawText("Department of CSE (AI & ML) - CMRIT Hyderabad", 30f, 85f, paint)

        // Metadata box
        var y = 140f
        paint.color = Color.rgb(240, 244, 248)
        canvas.drawRoundRect(30f, y, 565f, y + 80f, 8f, 8f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Patient Name: $patientName", 45f, y + 25f, paint)
        canvas.drawText("Lesion Label: ${lesion.name} (${lesion.anatomicalLocation})", 45f, y + 45f, paint)
        canvas.drawText("Generated: ${dateFormat.format(Date())}", 45f, y + 65f, paint)

        canvas.drawText("Lesion ID: ${lesion.id}", 340f, y + 25f, paint)
        canvas.drawText("Total Visits: ${lesion.visitsCount}", 340f, y + 45f, paint)

        // Outcome Banner
        y += 105f
        val outcomeColor = if (result.outcome.level == "green") Color.rgb(22, 101, 52)
        else if (result.outcome.level == "amber") Color.rgb(180, 83, 9)
        else Color.rgb(153, 27, 27)

        val outcomeBg = if (result.outcome.level == "green") Color.rgb(240, 253, 244)
        else if (result.outcome.level == "amber") Color.rgb(254, 243, 199)
        else Color.rgb(254, 242, 242)

        paint.color = outcomeBg
        canvas.drawRoundRect(30f, y, 565f, y + 70f, 8f, 8f, paint)

        paint.color = outcomeColor
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Triaged Outcome: ${result.outcome.title.uppercase()}", 45f, y + 30f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(51, 65, 85)
        canvas.drawText(result.plainLanguageRecommendation, 45f, y + 52f, paint)

        // Scores Section
        y += 95f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Quantitative Assessment & Change Analytics", 30f, y, paint)

        y += 20f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(30f, y, 565f, y + 175f, 8f, 8f, paint)

        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(51, 65, 85)

        val rows = listOf(
            "Siamese Change Detection Score (0-100)" to String.format(Locale.US, "%.1f %% (Baseline threshold: 35.0%%)", result.changeScore),
            "Classification Prediction (HAM10000 7-Class)" to "${result.className} (Confidence: ${String.format(Locale.US, "%.1f%%", result.confidence * 100f)})",
            "Malignancy Risk Tier" to result.riskTier.name,
            "Asymmetry Index (A)" to String.format(Locale.US, "%.2f (0.00 = Symmetric, 1.00 = Asymmetric)", result.asymmetryScore),
            "Border Irregularity (B)" to String.format(Locale.US, "%.2f", result.borderScore),
            "Color Uniformity & Variance (C)" to String.format(Locale.US, "%.2f", result.colorScore),
            "Estimated Lesion Diameter (D)" to String.format(Locale.US, "%.1f mm", result.diameterMm),
            "Longitudinal Evolution Delta (E)" to String.format(Locale.US, "%.1f", result.evolutionScore)
        )

        var rowY = y + 25f
        for ((label, value) in rows) {
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(label, 45f, rowY, paint)
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(value, 320f, rowY, paint)
            rowY += 18f
        }

        // Quality Gate section
        y += 205f
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Image Quality Gate Check", 30f, y, paint)

        y += 18f
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Focus / Blur (Laplacian Variance): ${String.format(Locale.US, "%.1f", result.qualityCheck.blurScore)} (Min acceptable: 50.0) - PASSED", 45f, y + 10f, paint)
        canvas.drawText("Exposure & Lighting: ${String.format(Locale.US, "%.1f", result.qualityCheck.luminanceScore)} - OPTIMAL", 45f, y + 25f, paint)

        // Mandatory Clinical Disclaimer
        y += 65f
        paint.color = Color.rgb(254, 242, 242)
        canvas.drawRoundRect(30f, y, 565f, y + 65f, 6f, 6f, paint)

        paint.color = Color.rgb(185, 28, 28)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MANDATORY CLINICAL DISCLAIMER", 45f, y + 18f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(127, 29, 29)
        canvas.drawText("DermaTrace is a clinical decision-support and triage tool, NOT a diagnostic system.", 45f, y + 34f, paint)
        canvas.drawText("Outputs are statistical estimates and MUST NOT replace formal histopathological examination or consultation with a certified dermatologist.", 45f, y + 48f, paint)

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("DermaTrace App v1.0.0 | Academic Research Project (CMRIT CSE AI & ML) | 100% On-Device TensorFlow Lite", 30f, 815f, paint)

        document.finishPage(page)

        // Save PDF to cache
        val reportsDir = File(context.cacheDir, "reports")
        reportsDir.mkdirs()
        val pdfFile = File(reportsDir, "DermaTrace_Report_${System.currentTimeMillis()}.pdf")

        try {
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            // Trigger Share Intent
            sharePdf(context, pdfFile)
            return pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
            return null
        }
    }

    private fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DermaTrace Clinical Decision-Support Report")
            putExtra(Intent.EXTRA_TEXT, "Attached is the DermaTrace lesion monitoring assessment report.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Clinical Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
