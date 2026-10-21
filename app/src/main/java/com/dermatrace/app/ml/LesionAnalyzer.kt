package com.dermatrace.app.ml

import android.graphics.Bitmap
import com.dermatrace.app.data.model.AnalysisResult

interface LesionAnalyzer {
    suspend fun analyze(
        currentBitmap: Bitmap,
        baselineBitmap: Bitmap? = null,
        referenceCoinDiameterMm: Float? = null
    ): AnalysisResult
}
