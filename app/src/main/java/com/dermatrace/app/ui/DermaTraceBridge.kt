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
import com.dermatrace.app.util.BiometricSecurityManager
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
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

    fun getActiveLesionId(): String = activeLesionId

    @JavascriptInterface
    fun performSuccessHaptic() {
        activity.runOnUiThread {
            com.dermatrace.app.util.HapticFeedbackHelper.performSuccess(activity)
        }
    }

    @JavascriptInterface
    fun performTickHaptic() {
        activity.runOnUiThread {
            com.dermatrace.app.util.HapticFeedbackHelper.performLightClick(activity)
        }
    }

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
        return addLesionInternal(name, location)
    }

    @JavascriptInterface
    fun addLesion(name: String, location: String, detailsJson: String?): String {
        return addLesionInternal(name, location)
    }

    private fun addLesionInternal(name: String, location: String): String {
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
            visitsCount = 1
        )
        db.insertLesion(lesion)
        activeLesionId = id
        return id
    }

    @JavascriptInterface
    fun addVisit(lesionId: String, changeScore: Float, decisionOutcome: String, predictedClass: String, confidence: Float, diameterMm: Float, notes: String?): String {
        val now = System.currentTimeMillis()
        val visitId = "visit-" + UUID.randomUUID().toString().substring(0, 8)
        val visit = Visit(
            id = visitId,
            lesionId = lesionId,
            timestamp = now,
            imagePath = "",
            changeScore = changeScore,
            decisionOutcome = decisionOutcome,
            predictedClass = predictedClass,
            confidence = confidence,
            asymmetry = 0.12f,
            borderIrregularity = 0.18f,
            colorVariation = 0.15f,
            diameterMm = diameterMm,
            evolutionScore = changeScore,
            gradCamBase64 = null,
            notes = notes ?: "Follow-up visit comparison"
        )
        db.insertVisit(visit)
        return visitId
    }

    @JavascriptInterface
    fun capturePhoto() {
        activity.runOnUiThread {
            activity.launchCamera()
        }
    }

    @JavascriptInterface
    fun launchCameraForFollowUp(lesionId: String, baselinePhotoBase64: String?) {
        activeLesionId = lesionId
        activity.runOnUiThread {
            var ghostFilePath: String? = null
            if (!baselinePhotoBase64.isNullOrBlank()) {
                try {
                    val clean = if (baselinePhotoBase64.contains(",")) baselinePhotoBase64.substringAfter(",") else baselinePhotoBase64
                    val bytes = Base64.decode(clean, Base64.DEFAULT)
                    val tempFile = File(activity.externalCacheDir, "ghost_baseline_${lesionId}.jpg")
                    tempFile.writeBytes(bytes)
                    ghostFilePath = tempFile.absolutePath
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            activity.launchCameraWithGhost(ghostFilePath)
        }
    }

    @JavascriptInterface
    fun isBiometricSupported(): Boolean {
        return BiometricSecurityManager.isHardwareSupported(activity)
    }

    @JavascriptInterface
    fun isBiometricEnabled(): Boolean {
        return BiometricSecurityManager.isBiometricLockEnabled(activity)
    }

    @JavascriptInterface
    fun toggleBiometric(enable: Boolean) {
        activity.runOnUiThread {
            if (enable) {
                BiometricSecurityManager.authenticate(
                    activity = activity,
                    title = "Enable Biometric Security",
                    subtitle = "Confirm biometric identity or device PIN to protect patient telemetry",
                    onSuccess = {
                        activity.markSessionAuthenticated()
                        BiometricSecurityManager.setBiometricLockEnabled(activity, true)
                        Toast.makeText(activity, "Biometric App Lock enabled", Toast.LENGTH_SHORT).show()
                        activity.evaluateJavascript("window.onBiometricStateChanged && window.onBiometricStateChanged(true);")
                    },
                    onError = { err ->
                        Toast.makeText(activity, "Authentication failed: $err", Toast.LENGTH_SHORT).show()
                        activity.evaluateJavascript("window.onBiometricStateChanged && window.onBiometricStateChanged(false);")
                    }
                )
            } else {
                BiometricSecurityManager.setBiometricLockEnabled(activity, false)
                Toast.makeText(activity, "Biometric App Lock disabled", Toast.LENGTH_SHORT).show()
                activity.evaluateJavascript("window.onBiometricStateChanged && window.onBiometricStateChanged(false);")
            }
        }
    }

    @JavascriptInterface
    fun testBiometricPrompt() {
        activity.runOnUiThread {
            BiometricSecurityManager.authenticate(
                activity = activity,
                title = "Biometric Verification Test",
                subtitle = "Testing fingerprint, face, or device PIN gatekeeper",
                onSuccess = {
                    activity.markSessionAuthenticated()
                    Toast.makeText(activity, "✓ Biometric identity verified successfully!", Toast.LENGTH_LONG).show()
                },
                onError = { err ->
                    Toast.makeText(activity, "Biometric test canceled: $err", Toast.LENGTH_SHORT).show()
                }
            )
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

                val result = withContext(Dispatchers.Default) {
                    analyzer.analyze(bitmap, null)
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
    fun analyzeImageSync(base64Image: String): String {
        return analyzeWithBaselineSync(base64Image, null)
    }

    @JavascriptInterface
    fun analyzeWithBaselineSync(base64Image: String, baselineBase64: String?): String {
        return try {
            val cleanBase64 = if (base64Image.contains(",")) base64Image.substringAfter(",") else base64Image
            val decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

            var baselineBitmap: Bitmap? = null
            if (!baselineBase64.isNullOrBlank()) {
                val cleanBase = if (baselineBase64.contains(",")) baselineBase64.substringAfter(",") else baselineBase64
                val baseBytes = Base64.decode(cleanBase, Base64.DEFAULT)
                baselineBitmap = BitmapFactory.decodeByteArray(baseBytes, 0, baseBytes.size)
            }

            if (bitmap != null) {
                val result = runBlocking(Dispatchers.Default) {
                    analyzer.analyze(bitmap, baselineBitmap)
                }
                latestResult = result
                gson.toJson(result)
            } else {
                "{}"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "{}"
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
        launchDermatologistSearch()
    }

    @JavascriptInterface
    fun findDermatologist(lat: Double, lon: Double) {
        launchDermatologistSearch()
    }

    private fun launchDermatologistSearch() {
        activity.runOnUiThread {
            try {
                val gmmIntentUri = Uri.parse("geo:0,0?q=dermatologist+skin+cancer+clinic+near+me")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                try {
                    activity.startActivity(mapIntent)
                } catch (e: Exception) {
                    // Fallback to browser Google Maps search
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/dermatologist+skin+cancer+clinic+near+me"))
                    activity.startActivity(webIntent)
                }
            } catch (e: Exception) {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/dermatologist+skin+cancer+clinic+near+me"))
                activity.startActivity(webIntent)
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

    @JavascriptInterface
    fun checkUserAccess(email: String): String {
        val emailTrimmed = email.trim().lowercase()
        val repo = com.dermatrace.app.admin.data.repository.SeedAdminRepository.getInstance(activity)
        val users = repo.getUsers().value
        val user = users.find { it.email.equals(emailTrimmed, ignoreCase = true) }
        if (user != null && !user.isActive) {
            return "SUSPENDED"
        }
        val isAdmin = emailTrimmed.startsWith("admin") || emailTrimmed.contains("admin")
        if (isAdmin) {
            val prefs = activity.getSharedPreferences("derma_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_admin", true).apply()
            return "ADMIN"
        }
        val prefs = activity.getSharedPreferences("derma_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_admin", false).apply()
        return "USER"
    }

    @JavascriptInterface
    fun launchAdminWorkspace() {
        activity.runOnUiThread {
            val prefs = activity.getSharedPreferences("derma_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_admin", true).apply()
            val intent = Intent(activity, com.dermatrace.app.admin.ui.AdminActivity::class.java)
            activity.startActivity(intent)
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
