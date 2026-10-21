package com.dermatrace.app.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val email: String,
    val role: String = "client"
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    val role: String,
    val email: String,
    @SerializedName("user_id") val userId: Int
)

data class CompareResponse(
    @SerializedName("change_score") val changeScore: Double,
    val decision: String,
    @SerializedName("visit_needed") val visitNeeded: Boolean,
    val confidence: Double,
    @SerializedName("model_used") val modelUsed: String,
    @SerializedName("baseline_dx") val baselineDx: String? = null,
    @SerializedName("followup_dx") val followupDx: String? = null,
    @SerializedName("baseline_conf") val baselineConf: Double = 0.0,
    @SerializedName("followup_conf") val followupConf: Double = 0.0
)

data class ConfusionMatrix(
    @SerializedName("TP") val tp: Int,
    @SerializedName("FP") val fp: Int,
    @SerializedName("FN") val fn: Int,
    @SerializedName("TN") val tn: Int
)

data class MetricsResponse(
    @SerializedName("change_auc") val changeAuc: Double,
    @SerializedName("change_acc") val changeAcc: Double,
    val precision: Double,
    val recall: Double,
    @SerializedName("f1_score") val f1Score: Double,
    @SerializedName("confusion_matrix") val confusionMatrix: ConfusionMatrix,
    @SerializedName("dx_balanced_acc") val dxBalancedAcc: Double,
    @SerializedName("malignant_sensitivity") val malignantSensitivity: Double,
    @SerializedName("malignant_specificity") val malignantSpecificity: Double
)

data class PatientRecord(
    val id: Int,
    val email: String,
    val role: String,
    @SerializedName("lesion_count") val lesionCount: Int,
    @SerializedName("latest_scan_date") val latestScanDate: String,
    @SerializedName("status") val status: String,
    @SerializedName("urgent") val urgent: Boolean
)
