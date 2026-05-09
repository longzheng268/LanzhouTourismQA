package com.lanzhou.qa.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val categoryColors = mapOf(
    "美食" to Color(0xFFFF9800),
    "景点" to Color(0xFF2196F3),
    "文化" to Color(0xFFE91E63),
    "交通" to Color(0xFF9C27B0),
    "历史" to Color(0xFF795548),
    "体验" to Color(0xFF4CAF50),
    "实用信息" to Color(0xFF607D8B),
    "地理" to Color(0xFF009688),
    "购物" to Color(0xFFFFC107),
    "住宿" to Color(0xFF3F51B5)
)

fun getCategoryColor(category: String): Color {
    return categoryColors[category] ?: Color(0xFF607D8B)
}

@Composable
fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DonutChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val total = data.sumOf { it.second }.toFloat()
    if (total == 0f) return

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(180.dp)
        ) {
            Canvas(modifier = Modifier.size(180.dp)) {
                val strokeWidth = 32f
                val radius = (size.minDimension - strokeWidth) / 2
                val topLeft = Offset(
                    (size.width - radius * 2) / 2,
                    (size.height - radius * 2) / 2
                )
                val arcSize = Size(radius * 2, radius * 2)

                var startAngle = -90f
                data.forEach { (category, count) ->
                    val sweep = (count / total) * 360f * animationProgress
                    drawArc(
                        color = getCategoryColor(category),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = total.toInt().toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "总条目",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legend
        val rows = data.chunked(4)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                row.forEach { (category, count) ->
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(getCategoryColor(category))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$category($count)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalBarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.second }?.toFloat() ?: 1f

    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        data.forEach { (category, count) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    modifier = Modifier.width(50.dp),
                    fontSize = 12.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val fraction = (count / maxValue) * animationProgress
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(10.dp))
                            .background(getCategoryColor(category))
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    modifier = Modifier.width(30.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
