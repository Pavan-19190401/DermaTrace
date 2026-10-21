package com.dermatrace.app

import com.dermatrace.app.data.model.DecisionOutcome
import com.dermatrace.app.data.model.RiskTier
import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionFusionTest {

    private fun fuseDecision(
        changeScore: Float,
        riskTier: RiskTier,
        confidence: Float
    ): DecisionOutcome {
        return if (changeScore > 65.0f || riskTier == RiskTier.MALIGNANT) {
            DecisionOutcome.VISIT_A_DOCTOR
        } else if (changeScore < 35.0f && riskTier == RiskTier.BENIGN && confidence >= 0.55f) {
            DecisionOutcome.NO_VISIT_NEEDED
        } else {
            DecisionOutcome.INCONCLUSIVE
        }
    }

    @Test
    fun testStableBenignLesionResultsInNoVisitNeeded() {
        val outcome = fuseDecision(
            changeScore = 14.5f,
            riskTier = RiskTier.BENIGN,
            confidence = 0.88f
        )
        assertEquals(DecisionOutcome.NO_VISIT_NEEDED, outcome)
    }

    @Test
    fun testMalignantPatternForcesDoctorVisitRegardlessOfChangeScore() {
        val outcome = fuseDecision(
            changeScore = 10.0f, // Low change but malignant class detected
            riskTier = RiskTier.MALIGNANT,
            confidence = 0.95f
        )
        assertEquals(DecisionOutcome.VISIT_A_DOCTOR, outcome)
    }

    @Test
    fun testHighEvolutionChangeScoreForcesDoctorVisit() {
        val outcome = fuseDecision(
            changeScore = 78.4f, // High morphological change
            riskTier = RiskTier.BENIGN,
            confidence = 0.70f
        )
        assertEquals(DecisionOutcome.VISIT_A_DOCTOR, outcome)
    }

    @Test
    fun testLowConfidenceYieldsInconclusive() {
        val outcome = fuseDecision(
            changeScore = 20.0f,
            riskTier = RiskTier.BENIGN,
            confidence = 0.42f // Below 0.55 threshold
        )
        assertEquals(DecisionOutcome.INCONCLUSIVE, outcome)
    }

    @Test
    fun testSuspiciousPrecancerousYieldsDoctorVisitOrInconclusive() {
        val outcome = fuseDecision(
            changeScore = 45.0f,
            riskTier = RiskTier.SUSPICIOUS,
            confidence = 0.65f
        )
        assertEquals(DecisionOutcome.INCONCLUSIVE, outcome)
    }
}
