package com.dermatrace.app.admin.data.model

data class ReviewerCandidate(
    val id: String,
    val name: String,
    val role: String,
    val isAvailable: Boolean = true
)

data class ShiftGap(
    val id: String,
    val dayIndex: Int, // 0 = Mon .. 6 = Sun
    val whenText: String, // e.g. "Thu • PM 14–20"
    val roleNeeded: String, // e.g. "Dermatologist"
    val assignedReviewer: String? = null
)

data class DayDemandCapacity(
    val dayIndex: Int,
    val shortLabel: String, // "M", "T", "W", ...
    val fullName: String, // "Monday", "Tuesday", ...
    val demand: Int,
    val capacity: Int,
    val isOverCapacity: Boolean = demand > capacity
)

data class OnShiftMember(
    val name: String,
    val initials: String,
    val role: String,
    val status: String, // "On shift", "Break"
    val casesText: String, // "18 / 20"
    val loadPercent: Int // e.g. 90
)

data class AdminKpi(
    val label: String,
    val value: String,
    val note: String
)

data class AdminUser(
    val id: String,
    val name: String,
    val email: String,
    val isActive: Boolean,
    val role: String = "Patient",
    val joinDate: String = "2026-01-15",
    val lesionCount: Int = 3,
    val lastActive: String = "Today"
) {
    val initials: String
        get() = name.split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
}

data class ModelHealthService(
    val id: String,
    val name: String,
    val isOperational: Boolean,
    val lastChecked: String = "Just now",
    val version: String = "2.4.1"
)

data class AuditLogEntry(
    val id: String,
    val timestamp: String,
    val adminEmail: String,
    val action: String, // "ASSIGN_REVIEWER", "SUSPEND_USER", "RESTORE_USER", "UPDATE_THRESHOLD", "RESTART_SERVICE"
    val target: String,
    val details: String
)

data class DecisionThresholds(
    val changeScoreLow: Float = 35.0f,
    val changeScoreHigh: Float = 65.0f,
    val minConfidence: Float = 0.70f,
    val version: Int = 1,
    val updatedAt: String = "2026-10-22"
)

data class TriageCase(
    val caseId: String, // e.g. "CASE-8492"
    val lesionId: String, // e.g. "LES-104"
    val outcome: String, // "Visit a Doctor", "Inconclusive", "No Visit Needed"
    val changeScore: Float, // 0.0 - 100.0 or normalized
    val confidence: Float, // 0.0 - 1.0
    val bodySite: String, // "Back", "Forearm", "Neck"
    val abcdeSummary: String, // "Asymmetry: High, Border: Irregular, Color: Variegated"
    val createdAt: String,
    val status: String = "Pending", // "Pending", "Assigned", "Reviewed"
    val assignedTo: String? = null,
    val reviewerNote: String? = null
)
