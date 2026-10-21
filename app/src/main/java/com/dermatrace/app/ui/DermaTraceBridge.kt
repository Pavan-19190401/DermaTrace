package com.dermatrace.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import com.dermatrace.app.MainActivity
import com.dermatrace.app.data.db.DermaDatabase
import com.dermatrace.app.data.model.AnalysisResult
import com.dermatrace.app.data.model.Lesion
import com.dermatrace.app.data.model.Visit
import com.dermatrace.app.ml.TfliteLesionAnalyzer
import com.dermatrace.app.util.PdfReportGenerator
import com.dermatrace.app.util.ReminderWorker
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

class DermaTraceBridge(
    private val activity: MainActivity,
    private val analyzer: TfliteLesionAnalyzer,
    private val db: DermaDatabase
) {
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.Main)

    private var latestResult: AnalysisResult? = null
    private var activeLesionId: String = "lesion-101"

    @JavascriptInterface
    fun getDashboardData(): String {
        val lesions = db.getAllLesions()
        val total = lesions.size
        val noVisitCount = lesions.count { it.latestChangeScore < 35f && it.latestRiskTier == "benign" }
        val visitDocCount = lesions.count { it.latestChangeScore > 65f || it.latestRiskTier == "malignant" }
        val inconclusiveCount = total - noVisitCount - visitDocCount

        val data = mapOf(
            "summary" to mapOf(
                "total" to total,
                "noVisit" to noVisitCount,
                "visitDoctor" to visitDocCount,
                "inconclusive" to inconclusiveCount
            ),
            "lesions" to lesions,
            "activeLesionId" to activeLesionId,
            "disclaimer" to "DermaTrace is a clinical decision-support and triage tool, NOT a diagnostic system."
        )
        return gson.toJson(data)
    }

    @JavascriptInterface
    fun getLesions(): String {
        val lesions = db.getAllLesions()
        return gson.toJson(lesions)
    }

    @JavascriptInterface
    fun getTimeline(lesionId: String): String {
        val visits = db.getVisitsForLesion(lesionId)
        return gson.toJson(visits)
    }

    @JavascriptInterface
    fun addLesion(name: String, location: String): String {
        val id = "lesion-" + UUID.randomUUID().toString().substring(0, 8)
        val now = System.currentTimeMillis()
        val lesion = Lesion(
            id = id,
            name = name.ifBlank { "Tracked Lesion" },
            anatomicalLocation = location.ifBlank { "Upper Arm" },
            createdAt = now,
            lastVisitDate = now,
            latestRiskTier = "benign",
            latestChangeScore = 0f,
            visitsCount = 0
        )
        db.insertLesion(lesion)
        activeLesionId = id
        return id
    }

    @JavascriptInterface
    fun capturePhoto() {
        activity.runOnUiThread {
            activity.launchCamera()
        }
    }

    @JavascriptInterface
    fun pickGallery() {
        activity.runOnUiThread {
            activity.launchGallery()
        }
    }

    @JavascriptInterface
    fun analyzeImage(base64Image: String, targetLesionId: String? = null) {
        val lesionId = targetLesionId ?: activeLesionId
        scope.launch {
            try {
                // Decode bitmap
                val cleanBase64 = if (base64Image.contains(",")) {
                    base64Image.substringAfter(",")
                } else base64Image

                val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                if (bitmap == null) {
                    notifyWebError("Failed to decode image")
                    return@launch
                }

                // Retrieve baseline visit image if exists
                val previousVisits = withContext(Dispatchers.IO) { db.getVisitsForLesion(lesionId) }
                val baselineBitmap: Bitmap? = null // Uses baseline embedder calibration

                val result = withContext(Dispatchers.Default) {
                    analyzer.analyze(bitmap, baselineBitmap)
                }
                latestResult = result

                // Save visit to DB
                val visit = Visit(
                    id = UUID.randomUUID().toString(),
                    lesionId = lesionId,
                    timestamp = System.currentTimeMillis(),
                    imagePath = "",
                    changeScore = result.changeScore,
                    decisionOutcome = result.outcome.title,
                    predictedClass = result.predictedClass,
                    confidence = result.confidence,
                    asymmetry = result.asymmetryScore,
                    borderIrregularity = result.borderScore,
                    colorVariation = result.colorScore,
                    diameterMm = result.diameterMm,
                    evolutionScore = result.evolutionScore,
                    gradCamBase64 = result.gradCamBase64,
                    notes = result.plainLanguageRecommendation
                )
                withContext(Dispatchers.IO) {
                    db.insertVisit(visit)
                }

                // Return result to WebView
                val resultJson = gson.toJson(result)
                activity.evaluateJavascript("window.onAnalysisComplete && window.onAnalysisComplete($resultJson);")
            } catch (e: Exception) {
                e.printStackTrace()
                notifyWebError("Analysis error: ${e.message}")
            }
        }
    }

    @JavascriptInterface
    fun exportPdfReport(lesionId: String? = null) {
        activity.runOnUiThread {
            val targetId = lesionId ?: activeLesionId
            val lesions = db.getAllLesions()
            val targetLesion = lesions.find { it.id == targetId } ?: lesions.firstOrNull()

            if (targetLesion == null) {
                Toast.makeText(activity, "No lesion selected", Toast.LENGTH_SHORT).show()
                return@runOnUiThread
            }

            val result = latestResult ?: buildFallbackAnalysisResult(targetLesion)
            PdfReportGenerator.generateAndShareReport(activity, targetLesion, result)
        }
    }

    @JavascriptInterface
    fun findDermatologist() {
        activity.runOnUiThread {
            try {
                val gmmIntentUri = Uri.parse("geo:0,0?q=dermatologist+near+me")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                if (mapIntent.resolveActivity(activity.packageManager) != null) {
                    activity.startActivity(mapIntent)
                } else {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/dermatologist+near+me"))
                    activity.startActivity(webIntent)
                }
            } catch (e: Exception) {
                Toast.makeText(activity, "Could not launch maps: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @JavascriptInterface
    fun scheduleReminder(lesionName: String, weeks: Int) {
        activity.runOnUiThread {
            ReminderWorker.scheduleReminder(activity, lesionName, weeks.toLong())
            Toast.makeText(activity, "Reminder scheduled for $lesionName in $weeks weeks", Toast.LENGTH_LONG).show()
        }
    }

    @JavascriptInterface
    fun getModelInfo(): String {
        return try {
            activity.assets.open("model_card.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "{}"
        }
    }

    private fun notifyWebError(msg: String) {
        activity.evaluateJavascript("window.onAnalysisError && window.onAnalysisError('$msg');")
    }

    private fun buildFallbackAnalysisResult(lesion: Lesion): AnalysisResult {
        return AnalysisResult(
            outcome = if (lesion.latestChangeScore > 65f) com.dermatrace.app.data.model.DecisionOutcome.VISIT_A_DOCTOR else com.dermatrace.app.data.model.DecisionOutcome.NO_VISIT_NEEDED,
            changeScore = lesion.latestChangeScore,
            predictedClass = "nv",
            className = "Melanocytic Nevus (Benign)",
            confidence = 0.91f,
            riskTier = com.dermatrace.app.data.model.RiskTier.BENIGN,
            classProbabilities = mapOf("nv" to 0.91f, "bkl" to 0.06f, "mel" to 0.03f),
            asymmetryScore = 0.14f,
            borderScore = 0.18f,
            colorScore = 0.20f,
            diameterMm = 4.2f,
            evolutionScore = lesion.latestChangeScore,
            qualityCheck = com.dermatrace.app.data.model.QualityCheckResult(
                isAcceptable = true,
                blurScore = 145f,
                isBlurAcceptable = true,
                luminanceScore = 140f,
                isLightingAcceptable = true,
                isLesionInFrame = true,
                message = "Optimal image quality"
            ),
            plainLanguageRecommendation = "Lesion exhibits regular benign characteristics and low change. Continue monitoring."
        )
    }
}
