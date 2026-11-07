package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DepletingTimerPink
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DepletingPieTimer(
    totalMinutes: Int,
    remainingSeconds: Int,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSec = (totalMinutes * 60).coerceAtLeast(1)
    val fractionRemaining = (remainingSeconds.toFloat() / totalSec).coerceIn(0f, 1f)
    val sweepAngle = fractionRemaining * 360f

    val displayMin = remainingSeconds / 60
    val displaySec = remainingSeconds % 60
    val timeFormatted = String.format("%02d:%02d", displayMin, displaySec)

    Box(
        modifier = modifier
            .size(150.dp)
            .shadow(6.dp, CircleShape)
            .clickable { onReset() }
            .testTag("depleting_pie_timer"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // Background disk (light tint)
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center
            )

            // Depleting colored pie wedge (starts at top, -90 degrees)
            drawArc(
                color = DepletingTimerPink,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )

            // 15, 30, 45, 60 minute marks and labels
            val markPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#9F1239")
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.17f
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val labels = listOf("60" to 0, "15" to 90, "30" to 180, "45" to 270)
            for ((label, deg) in labels) {
                val rad = Math.toRadians((deg - 90).toDouble())
                // tick mark
                val innerR = radius * 0.78f
                val outerR = radius * 0.95f
                val p1 = Offset((center.x + innerR * cos(rad)).toFloat(), (center.y + innerR * sin(rad)).toFloat())
                val p2 = Offset((center.x + outerR * cos(rad)).toFloat(), (center.y + outerR * sin(rad)).toFloat())
                drawLine(
                    color = Color(0xFF881337),
                    start = p1,
                    end = p2,
                    strokeWidth = 2.dp.toPx()
                )

                // text label
                val textR = radius * 0.62f
                val tx = (center.x + textR * cos(rad)).toFloat()
                val ty = (center.y + textR * sin(rad)).toFloat() + (markPaint.textSize / 3f)
                drawContext.canvas.nativeCanvas.drawText(label, tx, ty, markPaint)
            }

            // Center White Circle Hub
            val innerRadius = radius * 0.48f
            drawCircle(
                color = Color.White,
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = innerRadius,
                center = center,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Center Digital Display + Refresh icon
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timeFormatted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset timer",
                modifier = Modifier.size(14.dp),
                tint = Color(0xFF64748B)
            )
        }
    }
}
