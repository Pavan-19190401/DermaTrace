package com.dermatrace.app.admin.data.repository

import android.content.Context
import com.dermatrace.app.admin.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*

interface AdminRepository {
    fun isSeedMode(): Boolean
    
    // Staffing & Capacity
    fun getWeekIndex(): StateFlow<Int>
    fun setWeekIndex(index: Int)
    fun getSelectedDay(): StateFlow<Int>
    fun setSelectedDay(index: Int)
    fun getDemandCapacityList(): List<DayDemandCapacity>
    fun getGaps(): List<ShiftGap>
    fun getKpis(): List<AdminKpi>
    fun getCandidates(): List<ReviewerCandidate>
    fun getOnShiftTeam(dayIndex: Int): List<OnShiftMember>
    fun assignReviewer(gapId: String, candidateName: String)
    fun unassignGap(gapId: String)
    
    // Review Queue
    fun getCases(): StateFlow<List<TriageCase>>
    fun assignCase(caseId: String, reviewerName: String)
    fun markCaseReviewed(caseId: String, note: String)

    // Models & Thresholds
    fun getHealthServices(): StateFlow<List<ModelHealthService>>
    fun restartService(serviceId: String): Boolean
    fun getThresholds(): StateFlow<DecisionThresholds>
    fun updateThresholds(low: Float, high: Float, conf: Float)
    fun getModelCardJson(): String

    // Users
    fun getUsers(): StateFlow<List<AdminUser>>
    fun toggleUserStatus(userId: String): AdminUser?

    // Audit
    fun getAuditLogs(): StateFlow<List<AuditLogEntry>>
    fun logAction(action: String, target: String, details: String)
}

class SeedAdminRepository private constructor(private val context: Context) : AdminRepository {

    private val _weekIndex = MutableStateFlow(0) // 0 = This week, 1 = Next week
    private val _selectedDay = MutableStateFlow(3) // Default Thursday (index 3)

    // Gap assignments map: gapId -> reviewerName
    private val _assignments = MutableStateFlow<Map<String, String>>(emptyMap())

    private val candidatesList = listOf(
        ReviewerCandidate("c1", "Dr. L. Fernandes", "Dermatologist"),
        ReviewerCandidate("c2", "M. Khan", "Senior reviewer"),
        ReviewerCandidate("c3", "P. Nair", "Triage nurse"),
        ReviewerCandidate("c4", "Dr. A. Rao", "Dermatologist")
    )

    // Week 0: Thursday selected, demand: [42, 58, 64, 71, 66, 38, 29], cap: [60, 60, 60, 70, 70, 40, 40]
    private val week0Gaps = listOf(
        ShiftGap("a1", 3, "Thu • PM 14–20", "Dermatologist"),
        ShiftGap("a2", 5, "Sat • PM 14–20", "Triage nurse"),
        ShiftGap("a3", 6, "Sun • Full 08–20", "Senior reviewer")
    )

    // Week 1: Monday selected, demand: [48, 55, 61, 66, 70, 41, 33], cap: [60, 60, 60, 70, 70, 40, 40]
    private val week1Gaps = listOf(
        ShiftGap("b1", 1, "Tue • AM 08–14", "Dermatologist"),
        ShiftGap("b2", 2, "Wed • PM 14–20", "Senior reviewer")
    )

    private val baseDemandWeek0 = listOf(42, 58, 64, 71, 66, 38, 29)
    private val baseCapWeek0 = listOf(60, 60, 60, 70, 70, 40, 40)

    private val baseDemandWeek1 = listOf(48, 55, 61, 66, 70, 41, 33)
    private val baseCapWeek1 = listOf(60, 60, 60, 70, 70, 40, 40)

    private val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val dayShort = listOf("M", "T", "W", "T", "F", "S", "S")

    // Users
    private val _users = MutableStateFlow(
        listOf(
            AdminUser("u1", "Anika Sharma", "anika.sharma@example.com", isActive = true, lesionCount = 4),
            AdminUser("u2", "Rohan Mehta", "rohan.mehta@example.com", isActive = true, lesionCount = 2),
            AdminUser("u3", "Priya Nair", "priya.nair@example.com", isActive = false, lesionCount = 1),
            AdminUser("u4", "Karan Verma", "karan.verma@example.com", isActive = true, lesionCount = 5),
            AdminUser("u5", "Sara Thomas", "sara.thomas@example.com", isActive = true, lesionCount = 3),
            AdminUser("u6", "Dev Patel", "dev.patel@example.com", isActive = false, lesionCount = 2)
        )
    )

    // Model Services
    private val _services = MutableStateFlow(
        listOf(
            ModelHealthService("m1", "Lesion classifier (MobileNetV2)", isOperational = true),
            ModelHealthService("m2", "Siamese change-score embedder", isOperational = true),
            ModelHealthService("m3", "Image quality gate (Laplacian)", isOperational = true),
            ModelHealthService("m4", "Report generator & Triage sync", isOperational = false)
        )
    )

    // Thresholds
    private val _thresholds = MutableStateFlow(
        DecisionThresholds(changeScoreLow = 35.0f, changeScoreHigh = 65.0f, minConfidence = 0.70f, version = 1)
    )

    // Triage Cases (Pseudonymous, clinical privacy preserved)
    private val _cases = MutableStateFlow(
        listOf(
            TriageCase(
                caseId = "CASE-8492",
                lesionId = "LES-104",
                outcome = "Visit a Doctor",
                changeScore = 78.4f,
                confidence = 0.89f,
                bodySite = "Upper Back",
                abcdeSummary = "Border irregular, asymmetry marked, color variegation",
                createdAt = "Today, 10:14 AM",
                status = "Pending"
            ),
            TriageCase(
                caseId = "CASE-7319",
                lesionId = "LES-088",
                outcome = "Inconclusive",
                changeScore = 52.1f,
                confidence = 0.58f,
                bodySite = "Left Forearm",
                abcdeSummary = "Border slightly notched, evolution borderline",
                createdAt = "Today, 09:30 AM",
                status = "Pending"
            ),
            TriageCase(
                caseId = "CASE-6104",
                lesionId = "LES-045",
                outcome = "Visit a Doctor",
                changeScore = 69.8f,
                confidence = 0.84f,
                bodySite = "Right Shoulder",
                abcdeSummary = "Diameter >6mm, rapid elevation reported",
                createdAt = "Yesterday",
                status = "Assigned",
                assignedTo = "Dr. A. Rao"
            ),
            TriageCase(
                caseId = "CASE-5520",
                lesionId = "LES-012",
                outcome = "No Visit Needed",
                changeScore = 14.2f,
                confidence = 0.94f,
                bodySite = "Abdomen",
                abcdeSummary = "Stable nevus pattern, uniform pigment",
                createdAt = "Yesterday",
                status = "Reviewed",
                assignedTo = "M. Khan",
                reviewerNote = "Consistent with stable benign dysplastic nevus."
            )
        )
    )

    // Audit logs
    private val _auditLogs = MutableStateFlow(
        listOf(
            AuditLogEntry("al-1", "2026-10-22 09:15", "admin@dermatrace.com", "SYS_STARTUP", "System", "Admin workspace initialized with seed dataset"),
            AuditLogEntry("al-2", "2026-10-22 09:45", "admin@dermatrace.com", "ASSIGN_REVIEWER", "Shift a1 (Thu PM)", "Assigned Dr. A. Rao"),
            AuditLogEntry("al-3", "2026-10-22 10:30", "admin@dermatrace.com", "UPDATE_THRESHOLD", "DecisionThresholds", "Updated version to v1.0.0")
        )
    )

    override fun isSeedMode(): Boolean = true

    override fun getWeekIndex(): StateFlow<Int> = _weekIndex.asStateFlow()
    override fun setWeekIndex(index: Int) {
        _weekIndex.value = index
        _selectedDay.value = if (index == 0) 3 else 1 // Thu vs Tue
    }

    override fun getSelectedDay(): StateFlow<Int> = _selectedDay.asStateFlow()
    override fun setSelectedDay(index: Int) {
        _selectedDay.value = index
    }

    override fun getDemandCapacityList(): List<DayDemandCapacity> {
        val w = _weekIndex.value
        val dem = if (w == 0) baseDemandWeek0 else baseDemandWeek1
        val baseCap = if (w == 0) baseCapWeek0 else baseCapWeek1
        val currentGaps = if (w == 0) week0Gaps else week1Gaps
        val asg = _assignments.value

        return (0..6).map { i ->
            val extraCap = currentGaps.count { it.dayIndex == i && asg.containsKey(it.id) } * 10
            val totalCap = baseCap[i] + extraCap
            DayDemandCapacity(
                dayIndex = i,
                shortLabel = dayShort[i],
                fullName = dayNames[i],
                demand = dem[i],
                capacity = totalCap,
                isOverCapacity = dem[i] > totalCap
            )
        }
    }

    override fun getGaps(): List<ShiftGap> {
        val w = _weekIndex.value
        val rawGaps = if (w == 0) week0Gaps else week1Gaps
        val asg = _assignments.value
        return rawGaps.map { g ->
            g.copy(assignedReviewer = asg[g.id])
        }
    }

    override fun getKpis(): List<AdminKpi> {
        val dayList = getDemandCapacityList()
        val totalDemand = dayList.sumOf { it.demand }
        val overDays = dayList.count { it.isOverCapacity }
        val gapsList = getGaps()
        val openGaps = gapsList.count { it.assignedReviewer == null }
        val totalWindows = 25
        val coveragePct = Math.round(((totalWindows - openGaps).toFloat() / totalWindows.toFloat()) * 100f)

        return listOf(
            AdminKpi(
                label = "Forecast analyses",
                value = totalDemand.toString(),
                note = if (_weekIndex.value == 0) "This week" else "Next week"
            ),
            AdminKpi(
                label = "Over-capacity days",
                value = overDays.toString(),
                note = if (overDays > 0) "Needs cover" else "All clear"
            ),
            AdminKpi(
                label = "Reviewer coverage",
                value = "$coveragePct%",
                note = "Of shift windows"
            ),
            AdminKpi(
                label = "Open gaps",
                value = openGaps.toString(),
                note = if (openGaps > 0) "Assign below" else "Fully staffed"
            )
        )
    }

    override fun getCandidates(): List<ReviewerCandidate> = candidatesList

    override fun getOnShiftTeam(dayIndex: Int): List<OnShiftMember> {
        return listOf(
            OnShiftMember("Dr. A. Rao", "AR", "Dermatologist", "On shift", "18 / 20", 90),
            OnShiftMember("Dr. S. Iyer", "SI", "Dermatologist", "On shift", "21 / 20", 100),
            OnShiftMember("M. Khan", "MK", "Senior reviewer", "On shift", "12 / 24", 50),
            OnShiftMember("P. Nair", "PN", "Triage nurse", "Break", "9 / 30", 30)
        )
    }

    override fun assignReviewer(gapId: String, candidateName: String) {
        val map = _assignments.value.toMutableMap()
        map[gapId] = candidateName
        _assignments.value = map
        logAction("ASSIGN_REVIEWER", gapId, "Assigned $candidateName to gap $gapId")
    }

    override fun unassignGap(gapId: String) {
        val map = _assignments.value.toMutableMap()
        val previous = map.remove(gapId)
        _assignments.value = map
        logAction("UNASSIGN_REVIEWER", gapId, "Removed assignment $previous from gap $gapId")
    }

    override fun getCases(): StateFlow<List<TriageCase>> = _cases.asStateFlow()

    override fun assignCase(caseId: String, reviewerName: String) {
        _cases.value = _cases.value.map {
            if (it.caseId == caseId) it.copy(status = "Assigned", assignedTo = reviewerName) else it
        }
        logAction("ASSIGN_CASE", caseId, "Assigned to $reviewerName")
    }

    override fun markCaseReviewed(caseId: String, note: String) {
        _cases.value = _cases.value.map {
            if (it.caseId == caseId) it.copy(status = "Reviewed", reviewerNote = note) else it
        }
        logAction("REVIEW_CASE", caseId, "Reviewed with note: $note")
    }

    override fun getHealthServices(): StateFlow<List<ModelHealthService>> = _services.asStateFlow()

    override fun restartService(serviceId: String): Boolean {
        _services.value = _services.value.map {
            if (it.id == serviceId) it.copy(isOperational = true, lastChecked = "Just now") else it
        }
        logAction("RESTART_SERVICE", serviceId, "Service restarted and verified operational")
        return true
    }

    override fun getThresholds(): StateFlow<DecisionThresholds> = _thresholds.asStateFlow()

    override fun updateThresholds(low: Float, high: Float, conf: Float) {
        val curr = _thresholds.value
        val updated = curr.copy(
            changeScoreLow = low,
            changeScoreHigh = high,
            minConfidence = conf,
            version = curr.version + 1,
            updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        )
        _thresholds.value = updated
        logAction("UPDATE_THRESHOLDS", "v${updated.version}", "Set low=$low, high=$high, conf=$conf")
    }

    override fun getModelCardJson(): String {
        return try {
            context.assets.open("model_card.json").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            "{ \"error\": \"Could not load model_card.json\" }"
        }
    }

    override fun getUsers(): StateFlow<List<AdminUser>> = _users.asStateFlow()

    override fun toggleUserStatus(userId: String): AdminUser? {
        var modified: AdminUser? = null
        _users.value = _users.value.map {
            if (it.id == userId) {
                val next = it.copy(isActive = !it.isActive)
                modified = next
                next
            } else it
        }
        modified?.let {
            val act = if (it.isActive) "RESTORE_USER" else "SUSPEND_USER"
            logAction(act, it.email, "User ${it.name} status changed to ${if (it.isActive) "Active" else "Suspended"}")
        }
        return modified
    }

    override fun getAuditLogs(): StateFlow<List<AuditLogEntry>> = _auditLogs.asStateFlow()

    override fun logAction(action: String, target: String, details: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val entry = AuditLogEntry(
            id = "al-${System.currentTimeMillis()}",
            timestamp = sdf.format(Date()),
            adminEmail = "admin@dermatrace.com",
            action = action,
            target = target,
            details = details
        )
        _auditLogs.value = listOf(entry) + _auditLogs.value
    }

    companion object {
        @Volatile
        private var INSTANCE: SeedAdminRepository? = null

        fun getInstance(context: Context): SeedAdminRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SeedAdminRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
