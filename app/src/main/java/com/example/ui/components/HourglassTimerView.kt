package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

/**
 * Highly realistic, physically simulated 3D Hourglass Timer View.
 * Features:
 * - Handcrafted turned wood pedestals with brass fittings and drop shadow
 * - 3D turned baluster spindles with cylindrical wood shading
 * - Natural sand dynamics: V-shaped drainage crater in the upper bulb
 * - Physics-based accelerating tapered sand stream with cascading tumbling grains
 * - Micro-impact dispersion splash where falling sand lands on the mound
 * - 3D volumetric sand mound shaped by natural angle of repose
 * - Blown glass volume with double-wall depth and curved specular glares
 */
@Composable
fun HourglassTimerView(
    remainingSeconds: Int,
    totalSeconds: Int,
    sandColor: Color = Color(0xFFF59E0B),
    width: Dp = 230.dp,
    height: Dp = 280.dp,
    modifier: Modifier = Modifier
) {
    val totalSec = totalSeconds.coerceAtLeast(1)
    val fractionRemaining = (remainingSeconds.toFloat() / totalSec).coerceIn(0f, 1f)
    val fractionElapsed = 1f - fractionRemaining
    val isRunning = remainingSeconds > 0

    // Multi-phase continuous physics animations
    val infiniteTransition = rememberInfiniteTransition(label = "HourglassPhysicsTransition")

    // Fast stream grain flow (650ms cycle)
    val streamProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sandStreamPhysics"
    )

    // Landing splash & impact dispersion (380ms cycle)
    val splashProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 380, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "splashDispersion"
    )

    // Gentle sand shimmer wave (2000ms cycle)
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sandShimmer"
    )

    val isOvertime = remainingSeconds < 0
    val absSec = if (isOvertime) -remainingSeconds else remainingSeconds
    val min = absSec / 60
    val sec = absSec % 60
    val timeFormatted = if (isOvertime) String.format("+%02d:%02d", min, sec) else String.format("%02d:%02d", min, sec)

    val scale = (height.value / 280f).coerceIn(0.5f, 2.2f)

    Column(
        modifier = modifier.testTag("hourglass_timer_view"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(width = width, height = height),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(width = width - 8.dp, height = height - 8.dp)) {
                val w = size.width
                val h = size.height

                val centerX = w * 0.50f
                val waistY = h * 0.50f
                val topStandY = h * 0.085f
                val bottomStandY = h * 0.915f

                val outerBulbWidth = w * 0.76f
                val waistWidth = w * 0.11f
                val bulbLeft = centerX - (outerBulbWidth / 2f)
                val bulbRight = centerX + (outerBulbWidth / 2f)

                // -------------------------------------------------------------
                // COLOR PALETTE: Rich Antique Walnut Wood, Brass, and Sand
                // -------------------------------------------------------------
                val woodDeepShadow = Color(0xFF270E04)
                val woodDark = Color(0xFF451A08)
                val woodMid = Color(0xFF78310E)
                val woodLight = Color(0xFFA14917)
                val woodHighlight = Color(0xFFC96E2E)

                val brassDark = Color(0xFF92400E)
                val brassMid = Color(0xFFD97706)
                val brassHighlight = Color(0xFFFDE68A)

                val sandHighlight = Color(
                    red = (sandColor.red * 0.85f + 0.15f).coerceIn(0f, 1f),
                    green = (sandColor.green * 0.85f + 0.15f).coerceIn(0f, 1f),
                    blue = (sandColor.blue * 0.85f + 0.15f).coerceIn(0f, 1f)
                )
                val sandCore = sandColor
                val sandShadow = Color(
                    red = (sandColor.red * 0.72f),
                    green = (sandColor.green * 0.72f),
                    blue = (sandColor.blue * 0.68f)
                )
                val sandDeepShadow = Color(
                    red = (sandColor.red * 0.52f),
                    green = (sandColor.green * 0.52f),
                    blue = (sandColor.blue * 0.48f)
                )

                // -------------------------------------------------------------
                // 0. TABLETOP DROP SHADOW (Grounded in 3D Space)
                // -------------------------------------------------------------
                val shadowWidth = w * 0.76f
                val shadowHeight = h * 0.045f
                val shadowTop = bottomStandY + (h * 0.038f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(centerX, shadowTop + (shadowHeight * 0.45f)),
                        radius = shadowWidth * 0.50f
                    ),
                    topLeft = Offset(centerX - (shadowWidth / 2f), shadowTop),
                    size = Size(shadowWidth, shadowHeight)
                )

                // -------------------------------------------------------------
                // 1. BACK SPINDLE PILLAR (Adds Depth Behind the Glass)
                // -------------------------------------------------------------
                val backPillarX = centerX
                val backPillarRadius = (3.0f * scale).dp.toPx()
                drawTurnedPillar(
                    pillarCenterX = backPillarX,
                    topY = topStandY,
                    bottomY = bottomStandY,
                    pillarRadius = backPillarRadius,
                    woodLight = woodMid.copy(alpha = 0.55f),
                    woodMid = woodDark.copy(alpha = 0.65f),
                    woodShadow = woodDeepShadow.copy(alpha = 0.75f),
                    brassColor = brassDark.copy(alpha = 0.60f),
                    scale = scale
                )

                // -------------------------------------------------------------
                // 2. GLASS BULBS CONTOUR PATH
                // -------------------------------------------------------------
                val neckRadius = waistWidth / 2f
                val glassPath = Path().apply {
                    // Top opening rim
                    moveTo(bulbLeft + (16f * scale), topStandY)
                    // Upper bulb left curve down to waist
                    cubicTo(
                        bulbLeft - (4f * scale), topStandY + (h * 0.18f),
                        centerX - (neckRadius * 1.8f), waistY - (h * 0.08f),
                        centerX - neckRadius, waistY
                    )
                    // Lower bulb left curve down to bottom rim
                    cubicTo(
                        centerX - (neckRadius * 1.8f), waistY + (h * 0.08f),
                        bulbLeft - (4f * scale), bottomStandY - (h * 0.18f),
                        bulbLeft + (16f * scale), bottomStandY
                    )
                    // Bottom opening rim line
                    lineTo(bulbRight - (16f * scale), bottomStandY)
                    // Lower bulb right curve up to waist
                    cubicTo(
                        bulbRight + (4f * scale), bottomStandY - (h * 0.18f),
                        centerX + (neckRadius * 1.8f), waistY + (h * 0.08f),
                        centerX + neckRadius, waistY
                    )
                    // Upper bulb right curve up to top opening rim
                    cubicTo(
                        centerX + (neckRadius * 1.8f), waistY - (h * 0.08f),
                        bulbRight + (4f * scale), topStandY + (h * 0.18f),
                        bulbRight - (16f * scale), topStandY
                    )
                    close()
                }

                // Inner glass chamber (inset by wall thickness for real blown glass depth)
                val wallThick = (3.5f * scale).dp.toPx()
                val innerGlassPath = Path().apply {
                    moveTo(bulbLeft + (16f * scale) + wallThick, topStandY + (2f * scale))
                    cubicTo(
                        bulbLeft + wallThick, topStandY + (h * 0.18f),
                        centerX - (neckRadius * 1.6f), waistY - (h * 0.08f),
                        centerX - (neckRadius - (1.2f * scale)), waistY
                    )
                    cubicTo(
                        centerX - (neckRadius * 1.6f), waistY + (h * 0.08f),
                        bulbLeft + wallThick, bottomStandY - (h * 0.18f),
                        bulbLeft + (16f * scale) + wallThick, bottomStandY - (2f * scale)
                    )
                    lineTo(bulbRight - (16f * scale) - wallThick, bottomStandY - (2f * scale))
                    cubicTo(
                        bulbRight - wallThick, bottomStandY - (h * 0.18f),
                        centerX + (neckRadius * 1.6f), waistY + (h * 0.08f),
                        centerX + (neckRadius - (1.2f * scale)), waistY
                    )
                    cubicTo(
                        centerX + (neckRadius * 1.6f), waistY - (h * 0.08f),
                        bulbRight - wallThick, topStandY + (h * 0.18f),
                        bulbRight - (16f * scale) - wallThick, topStandY + (2f * scale)
                    )
                    close()
                }

                // Glass interior background tint (ambient refraction)
                drawPath(
                    path = glassPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.50f),
                            Color(0xFFE2E8F0).copy(alpha = 0.20f),
                            Color(0xFFF1F5F9).copy(alpha = 0.15f),
                            Color(0xFFE2E8F0).copy(alpha = 0.22f),
                            Color.White.copy(alpha = 0.55f)
                        ),
                        startY = topStandY,
                        endY = bottomStandY
                    )
                )

                // -------------------------------------------------------------
                // 3. PHYSICAL SAND SIMULATION (Clipped Inside Inner Glass)
                // -------------------------------------------------------------
                clipPath(innerGlassPath) {
                    val maxTopSandHeight = waistY - (topStandY + (10f * scale))
                    val streamLandingY: Float

                    // A. UPPER CHAMBER SAND WITH V-SHAPED DRAINAGE CRATER
                    if (remainingSeconds > 0 && fractionRemaining > 0.0001f) {
                        // Upper sand height is strictly proportional to remaining fraction, ensuring sand remains visible until remainingSeconds == 0
                        val topSandHeight = maxTopSandHeight * fractionRemaining
                        val wallSurfaceY = waistY - topSandHeight
                        // Crater depression depth peaks at mid-cycle and smoothly converges as sand funnels into the waist neck
                        val maxCraterDepth = topSandHeight * 0.45f
                        val craterFactor = sin(fractionRemaining * Math.PI.toFloat()).coerceIn(0f, 1f)
                        val craterDepression = maxCraterDepth * craterFactor
                        val craterCenterY = (wallSurfaceY + craterDepression).coerceAtMost(waistY - (2f * scale))

                        val upperSandPath = Path().apply {
                            // Start at left wall contact
                            moveTo(bulbLeft - (10f * scale), wallSurfaceY)
                            // Slope down into central drainage depression (funnel crater)
                            cubicTo(
                                centerX - (outerBulbWidth * 0.26f), wallSurfaceY + (craterDepression * 0.25f),
                                centerX - (waistWidth * 0.8f), craterCenterY,
                                centerX, craterCenterY
                            )
                            // Rise up out of crater to right wall contact
                            cubicTo(
                                centerX + (waistWidth * 0.8f), craterCenterY,
                                centerX + (outerBulbWidth * 0.26f), wallSurfaceY + (craterDepression * 0.25f),
                                bulbRight + (10f * scale), wallSurfaceY
                            )
                            // Follow glass down to waist
                            lineTo(centerX + neckRadius, waistY)
                            lineTo(centerX - neckRadius, waistY)
                            close()
                        }

                        // 3D volumetric light on upper sand: warm sunlit slope from left, deep central funnel shadow
                        drawPath(
                            path = upperSandPath,
                            brush = Brush.linearGradient(
                                colors = listOf(sandHighlight, sandCore, sandShadow, sandDeepShadow),
                                start = Offset(bulbLeft, wallSurfaceY),
                                end = Offset(centerX + (outerBulbWidth * 0.25f), waistY)
                            )
                        )

                        // Subtle rim contour along the upper sand line
                        val craterRim = Path().apply {
                            moveTo(bulbLeft + (10f * scale), wallSurfaceY)
                            cubicTo(
                                centerX - (outerBulbWidth * 0.24f), wallSurfaceY + (craterDepression * 0.25f),
                                centerX - (waistWidth * 0.8f), craterCenterY,
                                centerX, craterCenterY
                            )
                            cubicTo(
                                centerX + (waistWidth * 0.8f), craterCenterY,
                                centerX + (outerBulbWidth * 0.24f), wallSurfaceY + (craterDepression * 0.25f),
                                bulbRight - (10f * scale), wallSurfaceY
                            )
                        }
                        drawPath(
                            path = craterRim,
                            color = sandHighlight.copy(alpha = 0.55f),
                            style = Stroke(width = (2.0f * scale).dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // B. LOWER CHAMBER VOLUMETRIC SAND DUNE (Angle of Repose Conical Mound)
                    val maxBottomSandHeight = (bottomStandY - waistY) * 0.76f
                    val currentBottomSandHeight = maxBottomSandHeight * fractionElapsed
                    val moundPeakY = (bottomStandY - currentBottomSandHeight).coerceAtLeast(waistY + (8f * scale))
                    streamLandingY = if (fractionElapsed > 0.005f) moundPeakY else bottomStandY

                    if (fractionElapsed > 0.005f) {
                        val moundSpreadFactor = (0.25f + 0.75f * fractionElapsed).coerceIn(0.25f, 1f)
                        val moundBaseHalfWidth = (outerBulbWidth * 0.50f) * moundSpreadFactor

                        val lowerSandPath = Path().apply {
                            moveTo(centerX - moundBaseHalfWidth, bottomStandY + (10f * scale))
                            // Natural parabolic angle-of-repose slope up to peak
                            cubicTo(
                                centerX - (moundBaseHalfWidth * 0.52f), bottomStandY - (currentBottomSandHeight * 0.28f),
                                centerX - (neckRadius * 1.5f), moundPeakY + (3f * scale),
                                centerX, moundPeakY
                            )
                            // Natural slope down the right shoulder
                            cubicTo(
                                centerX + (neckRadius * 1.5f), moundPeakY + (3f * scale),
                                centerX + (moundBaseHalfWidth * 0.52f), bottomStandY - (currentBottomSandHeight * 0.28f),
                                centerX + moundBaseHalfWidth, bottomStandY + (10f * scale)
                            )
                            close()
                        }

                        // Conical 3D lighting: sunlit on top-left slope, deep ambient shadow on bottom-right slope
                        drawPath(
                            path = lowerSandPath,
                            brush = Brush.radialGradient(
                                colors = listOf(sandHighlight, sandCore, sandShadow, sandDeepShadow),
                                center = Offset(centerX - (moundBaseHalfWidth * 0.22f), moundPeakY + (currentBottomSandHeight * 0.35f)),
                                radius = moundBaseHalfWidth * 1.15f
                            )
                        )

                        // Top dune crest highlight line
                        val crestPath = Path().apply {
                            moveTo(centerX - (moundBaseHalfWidth * 0.40f), moundPeakY + (currentBottomSandHeight * 0.35f))
                            quadraticTo(
                                centerX, moundPeakY,
                                centerX + (moundBaseHalfWidth * 0.20f), moundPeakY + (currentBottomSandHeight * 0.20f)
                            )
                        }
                        drawPath(
                            path = crestPath,
                            color = sandHighlight.copy(alpha = 0.65f),
                            style = Stroke(width = (2.2f * scale).dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // C. ACCELERATING TAPERED SAND STREAM & CASCADING PARTICLES
                    if (isRunning && remainingSeconds > 0) {
                        val streamStartY = waistY - (2f * scale)
                        val streamEndY = streamLandingY

                        // Fluid physics stream: wide at neck, narrows as it accelerates downward
                        val streamTopWidth = (4.5f * scale).dp.toPx()
                        val streamMidWidth = (2.4f * scale).dp.toPx()
                        val streamBottomWidth = (3.2f * scale).dp.toPx()

                        val streamPath = Path().apply {
                            moveTo(centerX - (streamTopWidth / 2f), streamStartY)
                            cubicTo(
                                centerX - (streamMidWidth / 2f), streamStartY + ((streamEndY - streamStartY) * 0.35f),
                                centerX - (streamMidWidth / 2f), streamStartY + ((streamEndY - streamStartY) * 0.70f),
                                centerX - (streamBottomWidth / 2f), streamEndY
                            )
                            lineTo(centerX + (streamBottomWidth / 2f), streamEndY)
                            cubicTo(
                                centerX + (streamMidWidth / 2f), streamStartY + ((streamEndY - streamStartY) * 0.70f),
                                centerX + (streamMidWidth / 2f), streamStartY + ((streamEndY - streamStartY) * 0.35f),
                                centerX + (streamTopWidth / 2f), streamStartY
                            )
                            close()
                        }

                        // Core stream with soft depth gradient
                        drawPath(
                            path = streamPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(sandShadow, sandCore, sandHighlight),
                                startY = streamStartY,
                                endY = streamEndY
                            )
                        )

                        // Multiple cascading particles tumbling through the neck
                        val streamHeight = (streamEndY - streamStartY).coerceAtLeast(10f)
                        val particleOffsets = floatArrayOf(0.12f, 0.32f, 0.55f, 0.78f, 0.94f)
                        particleOffsets.forEachIndexed { idx, baseOffset ->
                            val p = (streamProgress + baseOffset) % 1f
                            // Gravitational acceleration curve: p^1.4f drops faster near bottom
                            val acceleratedP = Math.pow(p.toDouble(), 1.35).toFloat()
                            val py = streamStartY + (streamHeight * acceleratedP)
                            val pxJitter = sin(p * 20f + idx) * (1.2f * scale)
                            val particleRadius = ((1.4f + (idx % 3) * 0.4f) * scale).dp.toPx()

                            drawCircle(
                                color = if (idx % 2 == 0) Color.White.copy(alpha = 0.90f) else sandHighlight,
                                radius = particleRadius,
                                center = Offset(centerX + pxJitter, py)
                            )
                        }

                        // D. IMPACT DISPERSION & BOUNCING SPLASH SPRAY (At landing site)
                        val splashSpread = (splashProgress * (12f * scale))
                        val splashAlpha = (1f - splashProgress).coerceIn(0f, 1f)

                        // Left bouncing grain
                        drawCircle(
                            color = sandHighlight.copy(alpha = splashAlpha * 0.85f),
                            radius = (1.5f * scale).dp.toPx(),
                            center = Offset(centerX - splashSpread, streamEndY - (splashSpread * 0.45f))
                        )
                        // Right bouncing grain
                        drawCircle(
                            color = Color.White.copy(alpha = splashAlpha * 0.90f),
                            radius = (1.6f * scale).dp.toPx(),
                            center = Offset(centerX + splashSpread, streamEndY - (splashSpread * 0.50f))
                        )
                        // Center upward bounce micro-dot
                        drawCircle(
                            color = sandHighlight.copy(alpha = splashAlpha * 0.75f),
                            radius = (1.2f * scale).dp.toPx(),
                            center = Offset(centerX, streamEndY - (splashSpread * 0.75f))
                        )
                    }
                }

                // -------------------------------------------------------------
                // 4. GLASS SPECULAR GLARES, REFRACTION & RIM HIGHLIGHTS
                // -------------------------------------------------------------
                // A. Outer Glass Silhouette Rim Stroke
                drawPath(
                    path = glassPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color(0xFF94A3B8).copy(alpha = 0.45f),
                            Color(0xFFCBD5E1).copy(alpha = 0.35f),
                            Color(0xFF94A3B8).copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.85f)
                        ),
                        startY = topStandY,
                        endY = bottomStandY
                    ),
                    style = Stroke(width = (2.8f * scale).dp.toPx(), cap = StrokeCap.Round)
                )

                // B. Upper Bulb Primary Curved Specular Glare (Signature Blown-Glass Reflection)
                val upperGlarePath = Path().apply {
                    moveTo(bulbLeft + (18f * scale), topStandY + (14f * scale))
                    cubicTo(
                        bulbLeft + (8f * scale), topStandY + (h * 0.16f),
                        centerX - (neckRadius * 1.55f), waistY - (h * 0.10f),
                        centerX - (neckRadius * 0.9f), waistY - (8f * scale)
                    )
                }
                drawPath(
                    path = upperGlarePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.25f)
                        ),
                        startY = topStandY + (14f * scale),
                        endY = waistY - (8f * scale)
                    ),
                    style = Stroke(width = (4.2f * scale).dp.toPx(), cap = StrokeCap.Round)
                )

                // Secondary soft diffused highlight alongside primary glare
                drawPath(
                    path = upperGlarePath,
                    color = Color.White.copy(alpha = 0.30f),
                    style = Stroke(width = (8.0f * scale).dp.toPx(), cap = StrokeCap.Round)
                )

                // C. Lower Bulb Primary Curved Specular Glare
                val lowerGlarePath = Path().apply {
                    moveTo(centerX - (neckRadius * 0.9f), waistY + (8f * scale))
                    cubicTo(
                        centerX - (neckRadius * 1.55f), waistY + (h * 0.10f),
                        bulbLeft + (8f * scale), bottomStandY - (h * 0.16f),
                        bulbLeft + (18f * scale), bottomStandY - (14f * scale)
                    )
                }
                drawPath(
                    path = lowerGlarePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.90f),
                            Color.White.copy(alpha = 0.70f)
                        ),
                        startY = waistY + (8f * scale),
                        endY = bottomStandY - (14f * scale)
                    ),
                    style = Stroke(width = (4.0f * scale).dp.toPx(), cap = StrokeCap.Round)
                )

                // D. Rim Reflection on Right Bulb Flank
                val rightRimGlare = Path().apply {
                    moveTo(bulbRight - (22f * scale), topStandY + (16f * scale))
                    cubicTo(
                        bulbRight - (12f * scale), topStandY + (h * 0.18f),
                        centerX + (neckRadius * 1.6f), waistY - (h * 0.08f),
                        centerX + (neckRadius * 1.1f), waistY - (6f * scale)
                    )
                }
                drawPath(
                    path = rightRimGlare,
                    color = Color.White.copy(alpha = 0.40f),
                    style = Stroke(width = (2.2f * scale).dp.toPx(), cap = StrokeCap.Round)
                )

                // E. Waist Constriction Specular Ring
                drawOval(
                    color = Color.White.copy(alpha = 0.70f),
                    topLeft = Offset(centerX - (neckRadius * 0.8f), waistY - (1.8f * scale)),
                    size = Size(neckRadius * 1.6f, (3.6f * scale).dp.toPx())
                )

                // -------------------------------------------------------------
                // 5. HANDCRAFTED TURNED BALUSTER SPINDLE PILLARS (Front Left & Right)
                // -------------------------------------------------------------
                val pillarRadius = (4.2f * scale).dp.toPx()
                val leftPillarX = w * 0.125f
                val rightPillarX = w * 0.875f

                // Left Pillar
                drawTurnedPillar(
                    pillarCenterX = leftPillarX,
                    topY = topStandY,
                    bottomY = bottomStandY,
                    pillarRadius = pillarRadius,
                    woodLight = woodLight,
                    woodMid = woodMid,
                    woodShadow = woodDark,
                    brassColor = brassMid,
                    scale = scale
                )

                // Right Pillar
                drawTurnedPillar(
                    pillarCenterX = rightPillarX,
                    topY = topStandY,
                    bottomY = bottomStandY,
                    pillarRadius = pillarRadius,
                    woodLight = woodLight,
                    woodMid = woodMid,
                    woodShadow = woodDark,
                    brassColor = brassMid,
                    scale = scale
                )

                // -------------------------------------------------------------
                // 6. 3-TIER BEVELED WOODEN STANDS WITH BRASS FITTINGS
                // -------------------------------------------------------------
                // TOP STAND (Multi-tier beveled plinth)
                drawBeveledStand(
                    centerX = centerX,
                    baseY = topStandY,
                    isTopStand = true,
                    fullWidth = w * 0.88f,
                    height = h * 0.055f,
                    woodLight = woodHighlight,
                    woodMid = woodMid,
                    woodDark = woodDark,
                    woodDeep = woodDeepShadow,
                    brassColor = brassMid,
                    brassHighlight = brassHighlight,
                    scale = scale
                )

                // BOTTOM STAND (Multi-tier beveled plinth)
                drawBeveledStand(
                    centerX = centerX,
                    baseY = bottomStandY,
                    isTopStand = false,
                    fullWidth = w * 0.88f,
                    height = h * 0.055f,
                    woodLight = woodHighlight,
                    woodMid = woodMid,
                    woodDark = woodDark,
                    woodDeep = woodDeepShadow,
                    brassColor = brassMid,
                    brassHighlight = brassHighlight,
                    scale = scale
                )
            }
        }

        Spacer(modifier = Modifier.height((8f * scale).coerceIn(4f, 14f).dp))

        // Large high-contrast digital readout beneath the hourglass
        val timerFontSize = (44f * scale).coerceIn(28f, 62f).sp
        Text(
            text = timeFormatted,
            fontSize = timerFontSize,
            fontWeight = FontWeight.Black,
            color = if (isOvertime) Color(0xFFDC2626) else Color(0xFF0F172A),
            letterSpacing = 1.sp
        )

        if (isOvertime) {
            Text(
                text = "OVERTIME • USING BONUS TIME",
                fontSize = (11f * scale).coerceIn(10f, 15f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFDC2626),
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Draws a classic lathe-turned baluster spindle pillar with 3D cylindrical lighting.
 */
private fun DrawScope.drawTurnedPillar(
    pillarCenterX: Float,
    topY: Float,
    bottomY: Float,
    pillarRadius: Float,
    woodLight: Color,
    woodMid: Color,
    woodShadow: Color,
    brassColor: Color,
    scale: Float
) {
    val totalHeight = bottomY - topY
    val midY = topY + (totalHeight * 0.50f)
    val capHeight = (6f * scale).dp.toPx()
    val beadRadius = pillarRadius * 1.55f

    // Cylindrical 3D lighting gradient across pillar width
    val cylindricalWoodBrush = Brush.horizontalGradient(
        colors = listOf(woodLight, woodMid, woodShadow),
        startX = pillarCenterX - pillarRadius,
        endX = pillarCenterX + pillarRadius
    )
    val cylindricalBeadBrush = Brush.horizontalGradient(
        colors = listOf(woodLight, woodMid, woodShadow),
        startX = pillarCenterX - beadRadius,
        endX = pillarCenterX + beadRadius
    )

    // Top Brass Ferrule / Fitting
    drawRoundRect(
        color = brassColor,
        topLeft = Offset(pillarCenterX - (pillarRadius * 1.25f), topY),
        size = Size(pillarRadius * 2.5f, capHeight),
        cornerRadius = CornerRadius((2f * scale).dp.toPx(), (2f * scale).dp.toPx())
    )

    // Top Turned Bead (Ornamental Sphere)
    drawOval(
        brush = cylindricalBeadBrush,
        topLeft = Offset(pillarCenterX - beadRadius, topY + capHeight),
        size = Size(beadRadius * 2f, beadRadius * 1.8f)
    )

    // Slender Central Shaft
    val shaftTop = topY + capHeight + (beadRadius * 1.7f)
    val shaftBottom = bottomY - capHeight - (beadRadius * 1.7f)
    drawRoundRect(
        brush = cylindricalWoodBrush,
        topLeft = Offset(pillarCenterX - pillarRadius, shaftTop),
        size = Size(pillarRadius * 2f, (shaftBottom - shaftTop).coerceAtLeast(10f)),
        cornerRadius = CornerRadius((1.5f * scale).dp.toPx(), (1.5f * scale).dp.toPx())
    )

    // Center Turned Ring Collar (lathe detail)
    drawOval(
        brush = cylindricalBeadBrush,
        topLeft = Offset(pillarCenterX - (beadRadius * 1.25f), midY - (beadRadius * 0.6f)),
        size = Size(beadRadius * 2.5f, beadRadius * 1.2f)
    )

    // Bottom Turned Bead (Ornamental Sphere)
    drawOval(
        brush = cylindricalBeadBrush,
        topLeft = Offset(pillarCenterX - beadRadius, bottomY - capHeight - (beadRadius * 1.8f)),
        size = Size(beadRadius * 2f, beadRadius * 1.8f)
    )

    // Bottom Brass Ferrule / Fitting
    drawRoundRect(
        color = brassColor,
        topLeft = Offset(pillarCenterX - (pillarRadius * 1.25f), bottomY - capHeight),
        size = Size(pillarRadius * 2.5f, capHeight),
        cornerRadius = CornerRadius((2f * scale).dp.toPx(), (2f * scale).dp.toPx())
    )
}

/**
 * Draws a 3-tier beveled wooden plinth endcap with brass trim and highlights.
 */
private fun DrawScope.drawBeveledStand(
    centerX: Float,
    baseY: Float,
    isTopStand: Boolean,
    fullWidth: Float,
    height: Float,
    woodLight: Color,
    woodMid: Color,
    woodDark: Color,
    woodDeep: Color,
    brassColor: Color,
    brassHighlight: Color,
    scale: Float
) {
    val tier1W = fullWidth
    val tier1H = height * 0.58f

    val tier2W = fullWidth * 0.82f
    val tier2H = height * 0.28f

    val tier3W = fullWidth * 0.62f
    val tier3H = height * 0.18f

    val t1Y: Float
    val t2Y: Float
    val t3Y: Float

    if (isTopStand) {
        // Top stand stacks downward: tier 1 on top, tier 2 mid, tier 3 facing glass
        t1Y = baseY - height
        t2Y = t1Y + tier1H
        t3Y = t2Y + tier2H
    } else {
        // Bottom stand stacks downward: tier 3 facing glass, tier 2 mid, tier 1 on table
        t3Y = baseY
        t2Y = t3Y + tier3H
        t1Y = t2Y + tier2H
    }

    // TIER 1: Main Heavy Beveled Wooden Base Plate
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(woodLight, woodMid, woodDark, woodDeep),
            startY = t1Y,
            endY = t1Y + tier1H
        ),
        topLeft = Offset(centerX - (tier1W / 2f), t1Y),
        size = Size(tier1W, tier1H),
        cornerRadius = CornerRadius((6f * scale).dp.toPx(), (6f * scale).dp.toPx())
    )

    // Beveled Edge Highlight Line along outer rim
    val rimY = if (isTopStand) t1Y + (1.5f * scale) else t1Y + tier1H - (1.5f * scale)
    drawLine(
        color = woodLight.copy(alpha = 0.85f),
        start = Offset(centerX - (tier1W * 0.46f), rimY),
        end = Offset(centerX + (tier1W * 0.46f), rimY),
        strokeWidth = (1.5f * scale).dp.toPx(),
        cap = StrokeCap.Round
    )

    // TIER 2: Secondary Molded Step with Inlaid Brass Rim
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(brassHighlight, brassColor, woodDark),
            startY = t2Y,
            endY = t2Y + tier2H
        ),
        topLeft = Offset(centerX - (tier2W / 2f), t2Y),
        size = Size(tier2W, tier2H),
        cornerRadius = CornerRadius((4f * scale).dp.toPx(), (4f * scale).dp.toPx())
    )

    // TIER 3: Glass Socket Collar / Cushion Ring
    drawRoundRect(
        brush = Brush.verticalGradient(
            colors = listOf(woodMid, woodDeep),
            startY = t3Y,
            endY = t3Y + tier3H
        ),
        topLeft = Offset(centerX - (tier3W / 2f), t3Y),
        size = Size(tier3W, tier3H),
        cornerRadius = CornerRadius((3f * scale).dp.toPx(), (3f * scale).dp.toPx())
    )
}
