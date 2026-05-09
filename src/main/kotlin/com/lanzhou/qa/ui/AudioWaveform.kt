package com.lanzhou.qa.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    barCount: Int = 32
) {
    val displayAmplitudes = if (amplitudes.size >= barCount) {
        amplitudes.take(barCount)
    } else {
        amplitudes + List(barCount - amplitudes.size) { 0f }
    }

    val animatedAmplitudes = displayAmplitudes.map { target ->
        val animated by animateFloatAsState(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        animated
    }

    Canvas(modifier = modifier.fillMaxWidth().height(64.dp)) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val points = animatedAmplitudes.size

        // Layer 1: background glow (wider, more transparent)
        drawWaveLayer(
            amplitudes = animatedAmplitudes,
            width = w,
            centerY = centerY,
            heightScale = h * 0.45f,
            color = primaryColor.copy(alpha = 0.12f),
            points = points
        )

        // Layer 2: mid layer
        drawWaveLayer(
            amplitudes = animatedAmplitudes,
            width = w,
            centerY = centerY,
            heightScale = h * 0.32f,
            color = primaryColor.copy(alpha = 0.3f),
            points = points
        )

        // Layer 3: foreground sharp wave
        drawWaveLayer(
            amplitudes = animatedAmplitudes,
            width = w,
            centerY = centerY,
            heightScale = h * 0.22f,
            color = primaryColor.copy(alpha = 0.7f),
            points = points
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveLayer(
    amplitudes: List<Float>,
    width: Float,
    centerY: Float,
    heightScale: Float,
    color: Color,
    points: Int
) {
    if (points < 2) return
    val step = width / (points - 1).toFloat()

    // Upper curve
    val upperPath = Path().apply {
        moveTo(0f, centerY)
        for (i in 0 until points - 1) {
            val x0 = i * step
            val x1 = (i + 1) * step
            val y0 = centerY - amplitudes[i] * heightScale
            val y1 = centerY - amplitudes[i + 1] * heightScale
            val cx = (x0 + x1) / 2f
            cubicTo(cx, y0, cx, y1, x1, y1)
        }
        lineTo(width, centerY)
    }

    // Combine into filled shape (upper curve + mirrored lower curve)
    val filledPath = Path().apply {
        addPath(upperPath)
        moveTo(width, centerY)
        for (i in points - 2 downTo 0) {
            val x0 = (i + 1) * step
            val x1 = i * step
            val y0 = centerY + amplitudes[i + 1] * heightScale
            val y1 = centerY + amplitudes[i] * heightScale
            val cx = (x0 + x1) / 2f
            cubicTo(cx, y0, cx, y1, x1, y1)
        }
        close()
    }

    drawPath(filledPath, color = color, style = Fill)
}
