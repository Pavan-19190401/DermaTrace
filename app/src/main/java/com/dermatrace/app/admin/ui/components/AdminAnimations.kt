package com.dermatrace.app.admin.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * High-performance, GPU-accelerated animations using RenderThread graphicsLayer.
 * Prevents recomposition jank and layout measurement stutter during scroll.
 * Easing replicates GSAP 'power3.out' / patient app elastic curves.
 */
val GsapPower3Out = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
val PatientNavEase = CubicBezierEasing(0.34f, 1.45f, 0.64f, 1f)

@Composable
fun AnimatedContentItem(
    modifier: Modifier = Modifier,
    distance: Dp = 28.dp,
    durationMillis: Int = 420,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) {
            kotlinx.coroutines.delay(delayMillis.toLong())
        }
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = GsapPower3Out
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val progress = animProgress.value
            alpha = progress
            scaleX = 0.98f + (0.02f * progress)
            scaleY = 0.98f + (0.02f * progress)
            translationY = (1f - progress) * distance.toPx()
        }
    ) {
        content()
    }
}
