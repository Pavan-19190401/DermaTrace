package com.dermatrace.app.ml

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

enum class QualityLevel {
    OPTIMAL,
    BLURRY,
    TOO_DARK,
    GLARE
}

data class QualityStatus(
    val isReady: Boolean,
    val level: QualityLevel,
    val sharpnessScore: Float,
    val brightnessScore: Float,
    val guidanceMessage: String
)

/**
 * CameraX ImageAnalysis Analyzer that evaluates raw camera frames at 30 FPS.
 * Computes Laplacian variance (sharpness/motion blur) and luminance distribution
 * on the central Region of Interest (ROI) before TFLite inference.
 */
class ImageQualityAnalyzer(
    private val onQualityEvaluated: (QualityStatus) -> Unit
) : ImageAnalysis.Analyzer {

    // Thresholds calibrated for skin lesion dermatological imaging
    private val minSharpnessThreshold = 100.0f
    private val minBrightnessThreshold = 45.0f
    private val maxBrightnessThreshold = 220.0f

    // Exponential moving average for temporal stability across frames
    private var smoothedSharpness = 120.0f
    private var smoothedBrightness = 120.0f
    private val alpha = 0.35f

    override fun analyze(image: ImageProxy) {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val width = image.width
        val height = image.height
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride

        try {
            // Extract central 50% ROI corresponding to the circular reticle ring
            val roiStartX = width / 4
            val roiEndX = (width * 3) / 4
            val roiStartY = height / 4
            val roiEndY = (height * 3) / 4

            val roiW = roiEndX - roiStartX
            val roiH = roiEndY - roiStartY

            // Downsample ROI by factor of 2 for ultra-fast, zero-jank frame processing (< 5ms)
            val step = 2
            val sampledW = roiW / step
            val sampledH = roiH / step

            val luminanceData = IntArray(sampledW * sampledH)
            var sumBrightness = 0L

            for (y in 0 until sampledH) {
                val origY = roiStartY + (y * step)
                val rowOffset = origY * rowStride
                for (x in 0 until sampledW) {
                    val origX = roiStartX + (x * step)
                    val bufferPos = rowOffset + (origX * pixelStride)
                    val pixel = buffer.get(bufferPos).toInt() and 0xFF
                    luminanceData[y * sampledW + x] = pixel
                    sumBrightness += pixel
                }
            }

            val totalPixels = sampledW * sampledH
            val avgBrightness = if (totalPixels > 0) sumBrightness.toFloat() / totalPixels else 128f

            // Compute discrete 3x3 Laplacian operator variance on the downsampled grayscale grid
            // Kernel:
            //  0  1  0
            //  1 -4  1
            //  0  1  0
            var laplacianSum = 0.0
            var laplacianSumSq = 0.0
            var laplacianCount = 0

            for (y in 1 until sampledH - 1) {
                val rowIdx = y * sampledW
                val rowAbove = (y - 1) * sampledW
                val rowBelow = (y + 1) * sampledW

                for (x in 1 until sampledW - 1) {
                    val center = luminanceData[rowIdx + x]
                    val up = luminanceData[rowAbove + x]
                    val down = luminanceData[rowBelow + x]
                    val left = luminanceData[rowIdx + (x - 1)]
                    val right = luminanceData[rowIdx + (x + 1)]

                    val lapVal = (up + down + left + right - (4 * center)).toDouble()
                    laplacianSum += lapVal
                    laplacianSumSq += lapVal * lapVal
                    laplacianCount++
                }
            }

            val sharpness = if (laplacianCount > 0) {
                val mean = laplacianSum / laplacianCount
                val variance = (laplacianSumSq / laplacianCount) - (mean * mean)
                max(0.0, variance).toFloat()
            } else {
                0.0f
            }

            // Apply exponential moving average to prevent UI jitter
            smoothedSharpness = (alpha * sharpness) + ((1f - alpha) * smoothedSharpness)
            smoothedBrightness = (alpha * avgBrightness) + ((1f - alpha) * smoothedBrightness)

            // Determine Quality Status
            val status = when {
                smoothedBrightness < minBrightnessThreshold -> {
                    QualityStatus(
                        isReady = false,
                        level = QualityLevel.TOO_DARK,
                        sharpnessScore = smoothedSharpness,
                        brightnessScore = smoothedBrightness,
                        guidanceMessage = "💡 Lighting too dim • Turn on torch"
                    )
                }
                smoothedBrightness > maxBrightnessThreshold -> {
                    QualityStatus(
                        isReady = false,
                        level = QualityLevel.GLARE,
                        sharpnessScore = smoothedSharpness,
                        brightnessScore = smoothedBrightness,
                        guidanceMessage = "☀️ Glare detected • Adjust angle slightly"
                    )
                }
                smoothedSharpness < minSharpnessThreshold -> {
                    QualityStatus(
                        isReady = false,
                        level = QualityLevel.BLURRY,
                        sharpnessScore = smoothedSharpness,
                        brightnessScore = smoothedBrightness,
                        guidanceMessage = "🔍 Blurry / In Motion • Hold steady to focus"
                    )
                }
                else -> {
                    QualityStatus(
                        isReady = true,
                        level = QualityLevel.OPTIMAL,
                        sharpnessScore = smoothedSharpness,
                        brightnessScore = smoothedBrightness,
                        guidanceMessage = "✓ Optimal Focus • Ready to scan"
                    )
                }
            }

            onQualityEvaluated(status)
        } catch (e: Exception) {
            // Safe fallback if buffer invalid
        } finally {
            image.close()
        }
    }
}
