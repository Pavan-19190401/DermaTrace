package com.dermatrace.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

class AbcdeLogicTest {

    @Test
    fun testAsymmetryCalculation() {
        val width = 80
        val height = 40
        val asymmetry = abs(width - height).toFloat() / max(width, height).toFloat()
        assertEquals(0.5f, asymmetry, 0.001f)
    }

    @Test
    fun testPerfectSymmetry() {
        val width = 60
        val height = 60
        val asymmetry = abs(width - height).toFloat() / max(width, height).toFloat()
        assertEquals(0.0f, asymmetry, 0.001f)
    }

    @Test
    fun testDiameterScalingWithReferenceCoin() {
        val coinDiameterMm = 24.26f // US Quarter diameter
        val coinPixelSpan = 120f
        val mmPerPixel = coinDiameterMm / coinPixelSpan

        val lesionPixelSpan = 30
        val calculatedMm = lesionPixelSpan * mmPerPixel

        assertTrue(calculatedMm in 5.9f..6.2f)
    }

    @Test
    fun testChangeScoreNormalization() {
        val dist = 0.55f
        val minD = 0.20f
        val maxD = 0.90f
        val normalizedScore = (((dist - minD) / (maxD - minD)) * 100f).coerceIn(0f, 100f)

        assertEquals(50.0f, normalizedScore, 0.5f)
    }
}
