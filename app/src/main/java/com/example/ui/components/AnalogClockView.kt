package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnalogClockView(
    calendar: Calendar,
    modifier: Modifier = Modifier
) {
    val hour = calendar.get(Calendar.HOUR)
    val minute = calendar.get(Calendar.MINUTE)
    val second = calendar.get(Calendar.SECOND)

    Box(
        modifier = modifier
            .size(150.dp)
            .shadow(6.dp, CircleShape)
            .testTag("analog_clock_view"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f

            // White face
            drawCircle(
                color = Color.White,
                radius = radius,
                center = center
            )
            // Outer subtle rim
            drawCircle(
                color = Color(0xFFE2E8F0),
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Hour numbers & minute dots
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#1E293B")
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.22f
                isAntiAlias = true
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            }

            val minuteDotPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#94A3B8")
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = radius * 0.11f
                isAntiAlias = true
            }

            for (i in 1..12) {
                val angle = Math.toRadians((i * 30.0) - 90.0)
                val numRadius = radius * 0.72f
                val x = center.x + (numRadius * cos(angle)).toFloat()
                val y = center.y + (numRadius * sin(angle)).toFloat() + (paint.textSize / 3f)

                drawContext.canvas.nativeCanvas.drawText(i.toString(), x, y, paint)

                // Outer minute number in circle (0, 5, 10, ... 55)
                val minAngle = Math.toRadians((i * 30.0) - 90.0)
                val minRadius = radius * 0.88f
                val mx = center.x + (minRadius * cos(minAngle)).toFloat()
                val my = center.y + (minRadius * sin(minAngle)).toFloat() + (minuteDotPaint.textSize / 3f)
                val minText = (i * 5 % 60).toString()
                drawContext.canvas.nativeCanvas.drawText(minText, mx, my, minuteDotPaint)
            }

            // Hour Hand
            val hourAngle = Math.toRadians(((hour + minute / 60f) * 30f) - 90.0)
            val hourLength = radius * 0.45f
            val hourEnd = Offset(
                (center.x + hourLength * cos(hourAngle)).toFloat(),
                (center.y + hourLength * sin(hourAngle)).toFloat()
            )
            drawLine(
                color = Color(0xFF1E293B),
                start = center,
                end = hourEnd,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Minute Hand
            val minuteAngle = Math.toRadians(((minute + second / 60f) * 6f) - 90.0)
            val minuteLength = radius * 0.65f
            val minuteEnd = Offset(
                (center.x + minuteLength * cos(minuteAngle)).toFloat(),
                (center.y + minuteLength * sin(minuteAngle)).toFloat()
            )
            drawLine(
                color = Color(0xFF38BDF8),
                start = center,
                end = minuteEnd,
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Second Hand
            val secondAngle = Math.toRadians((second * 6f) - 90.0)
            val secondLength = radius * 0.75f
            val secondEnd = Offset(
                (center.x + secondLength * cos(secondAngle)).toFloat(),
                (center.y + secondLength * sin(secondAngle)).toFloat()
            )
            drawLine(
                color = Color(0xFFFB923C),
                start = center,
                end = secondEnd,
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Pin
            drawCircle(
                color = Color(0xFF1E293B),
                radius = 3.5.dp.toPx(),
                center = center
            )
        }
    }
}
