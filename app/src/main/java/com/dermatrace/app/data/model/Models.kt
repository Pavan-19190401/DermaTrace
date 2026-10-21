package com.dermatrace.app.data.model

import java.io.Serializable

data class Lesion(
    val id: String,
    val name: String,
    val anatomicalLocation: String,
    val createdAt: Long,
    val lastVisitDate: Long,
    val latestRiskTier: String, // benign, suspicious, malignant
    val latestChangeScore: Float, // 0 - 100
    val visitsCount: Int = 1,
    val thumbnailBase64: String? = null
) : Serializable

data class Visit(
    val id: String,
    val lesionId: String,
    val timestamp: Long,
    val imagePath: String,
    val changeScore: Float,
    val decisionOutcome: String, // "No Visit Needed", "Visit a Doctor", "Inconclusive"
    val predictedClass: String,
    val confidence: Float,
    val asymmetry: Float,
    val borderIrregularity: Float,
    val colorVariation: Float,
    val diameterMm: Float,
    val evolutionScore: Float,
    val gradCamBase64: String? = null,
    val notes: String = ""
) : Serializable

enum class RiskTier {
    BENIGN,
    SUSPICIOUS,
    MALIGNANT
}

enum class DecisionOutcome(val title: String, val level: String) {
    NO_VISIT_NEEDED("No Visit Needed", "green"),
    VISIT_A_DOCTOR("Visit a Doctor", "red"),
    INCONCLUSIVE("Inconclusive - Retake Recommended", "amber")
}

data class QualityCheckResult(
    val isAcceptable: Boolean,
    val blurScore: Float,
    val isBlurAcceptable: Boolean,
    val luminanceScore: Float,
    val isLightingAcceptable: Boolean,
    val isLesionInFrame: Boolean,
    val message: String
)

data class AnalysisResult(
    val outcome: DecisionOutcome,
    val changeScore: Float, // 0 - 100
    val predictedClass: String, // akiec, bcc, bkl, df, mel, nv, vasc
    val className: String, // Full English Name
    val confidence: Float, // 0 - 1
    val riskTier: RiskTier,
    val classProbabilities: Map<String, Float>,
    val asymmetryScore: Float,
    val borderScore: Float,
    val colorScore: Float,
    val diameterMm: Float,
    val evolutionScore: Float,
    val qualityCheck: QualityCheckResult,
    val plainLanguageRecommendation: String,
    val gradCamBase64: String? = null
)
