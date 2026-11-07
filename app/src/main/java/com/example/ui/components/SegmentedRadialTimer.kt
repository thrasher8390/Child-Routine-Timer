package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SegmentedRadialTimer(
    remainingSeconds: Int,
    totalSeconds: Int,
    activeColor: Color,
    modifier: Modifier = Modifier,
    totalSegments: Int = 16
) {
    val totalSec = totalSeconds.coerceAtLeast(1)
    val fractionRemaining = (remainingSeconds.toFloat() / totalSec).coerceIn(0f, 1f)
    val activeSegments = (fractionRemaining * totalSegments).toInt().coerceIn(0, totalSegments)

    val min = remainingSeconds / 60
    val sec = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", min, sec)

    Box(
        modifier = modifier
            .size(175.dp)
            .testTag("segmented_radial_timer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(165.dp)) {
            val strokeWidth = 9.dp.toPx()
            val arcSize = size.width - strokeWidth
            val topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f)
            val segmentAngle = 360f / totalSegments
            val gapAngle = 4.5f
            val sweepAngle = segmentAngle - gapAngle

            for (i in 0 until totalSegments) {
                val startAngle = -90f + (i * segmentAngle) + (gapAngle / 2f)
                val isFilled = i < activeSegments

                // Inactive segment is very faint, active segment is bright
                val segColor = if (isFilled) activeColor else Color(0xFFE2E8F0)

                drawArc(
                    color = segColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = timeFormatted,
            fontSize = 32.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF1E293B)
        )
    }
}
