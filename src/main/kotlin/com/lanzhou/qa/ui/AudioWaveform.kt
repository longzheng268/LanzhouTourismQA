package com.lanzhou.qa.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AudioWaveform(
    amplitudes: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
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
            animationSpec = tween(durationMillis = 80, easing = LinearEasing)
        )
        animated
    }

    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        val totalWidth = size.width
        val totalHeight = size.height
        val barWidth = totalWidth / (barCount * 1.8f)
        val gap = barWidth * 0.8f

        animatedAmplitudes.forEachIndexed { index, amplitude ->
            val barHeight = (amplitude * totalHeight * 0.85f).coerceAtLeast(3f)
            val x = index * (barWidth + gap) + gap / 2
            val y = (totalHeight - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}
