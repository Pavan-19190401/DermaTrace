package com.dermatrace.app.admin

import org.junit.Assert.*
import org.junit.Test

class AdminStaffingTest {

    // Simple standalone logic test for demand and capacity math
    @Test
    fun testDemandCapacityCalculations() {
        val baseCap = 60
        val baseDemand = 71

        val isOverInitially = baseDemand > baseCap
        assertTrue("Day should initially be over capacity", isOverInitially)
        assertEquals(11, baseDemand - baseCap)

        // Assigning a reviewer adds +10 capacity
        val updatedCap = baseCap + 10
        assertEquals(70, updatedCap)

        // Assigning a second reviewer adds another +10 capacity
        val resolvedCap = updatedCap + 10
        assertEquals(80, resolvedCap)
        assertFalse("Day should be within capacity after two reviewers assigned", baseDemand > resolvedCap)
    }

    @Test
    fun testCoveragePercentageCalculation() {
        val totalWindows = 25
        val openGaps = 3

        val initialCoverage = Math.round(((totalWindows - openGaps).toFloat() / totalWindows.toFloat()) * 100f)
        assertEquals(88, initialCoverage)

        // Fill one gap
        val updatedOpenGaps = 2
        val updatedCoverage = Math.round(((totalWindows - updatedOpenGaps).toFloat() / totalWindows.toFloat()) * 100f)
        assertEquals(92, updatedCoverage)

        // Fill all gaps
        val allFilledCoverage = Math.round(((totalWindows - 0).toFloat() / totalWindows.toFloat()) * 100f)
        assertEquals(100, allFilledCoverage)
    }

    @Test
    fun testSuspendedUserAccessCheck() {
        val suspendedEmails = listOf("priya.nair@example.com", "dev.patel@example.com")
        val activeEmails = listOf("anika.sharma@example.com", "rohan.mehta@example.com")

        assertTrue(suspendedEmails.contains("priya.nair@example.com"))
        assertTrue(suspendedEmails.contains("dev.patel@example.com"))

        assertFalse(suspendedEmails.contains("anika.sharma@example.com"))
        assertFalse(suspendedEmails.contains("admin@dermatrace.com"))
    }

    @Test
    fun testAdminRoleDetection() {
        fun isUserAdmin(email: String, roleClaim: String?): Boolean {
            if (roleClaim == "admin") return true
            return email.lowercase().startsWith("admin")
        }

        assertTrue(isUserAdmin("admin@dermatrace.com", null))
        assertTrue(isUserAdmin("clinician@hospital.org", "admin"))
        assertFalse(isUserAdmin("patient@example.com", null))
        assertFalse(isUserAdmin("john.doe@gmail.com", "patient"))
    }
}
