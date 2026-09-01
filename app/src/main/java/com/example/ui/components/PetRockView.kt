package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class TapParticle(
    val id: Long,
    val x: Float,
    val y: Float,
    val animProgress: Animatable<Float, *> = Animatable(0f),
    val text: String = "+1",
    val color: Color = Color(0xFF38BDF8)
)

enum class RockMood {
    CHILL,
    HAPPY,
    ENERGETIC,
    HYPERDRIVE
}

@Composable
fun PetRockView(
    cps: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1.0f) }
    val particles = remember { mutableStateListOf<TapParticle>() }

    val infiniteTransition = rememberInfiniteTransition(label = "rock_idle")
    val idleBreathe by infiniteTransition.animateFloat(
        initialValue = 0.985f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    // Eye blinking
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(3000, 6000))
            isBlinking = true
            delay(150)
            isBlinking = false
        }
    }

    val mood = when {
        cps >= 15 -> RockMood.HYPERDRIVE
        cps >= 8 -> RockMood.ENERGETIC
        cps >= 2 -> RockMood.HAPPY
        else -> RockMood.CHILL
    }

    Box(
        modifier = modifier
            .testTag("pet_rock_interactive")
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        onTap()
                        // Spring squeeze animation
                        coroutineScope.launch {
                            scaleAnim.snapTo(0.91f)
                            scaleAnim.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(dampingRatio = 0.45f, stiffness = 600f)
                            )
                        }

                        // Spawn float particle
                        val particleId = System.currentTimeMillis() + Random.nextInt(1000)
                        val particleColor = when (mood) {
                            RockMood.HYPERDRIVE -> Color(0xFFF43F5E)
                            RockMood.ENERGETIC -> Color(0xFFF59E0B)
                            RockMood.HAPPY -> Color(0xFF38BDF8)
                            RockMood.CHILL -> Color(0xFF38BDF8)
                        }
                        val particleText = when (mood) {
                            RockMood.HYPERDRIVE -> "🔥 +1"
                            RockMood.ENERGETIC -> "⚡ +1"
                            else -> "+1"
                        }
                        val particle = TapParticle(
                            id = particleId,
                            x = offset.x - 40f + Random.nextInt(-30, 30),
                            y = offset.y - 40f + Random.nextInt(-20, 20),
                            text = particleText,
                            color = particleColor
                        )
                        particles.add(particle)

                        coroutineScope.launch {
                            particle.animProgress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                            )
                            particles.remove(particle)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Tap Particles
        particles.forEach { particle ->
            val progress = particle.animProgress.value
            val offsetY = -progress * 90f
            val alpha = (1f - progress).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Text(
                    text = particle.text,
                    color = particle.color,
                    fontSize = (16 + (progress * 6)).sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = particle.x.roundToInt(),
                                y = (particle.y + offsetY).roundToInt()
                            )
                        }
                        .alpha(alpha)
                )
            }
        }

        // The Rock Canvas
        Canvas(
            modifier = Modifier
                .size(260.dp)
                .scale(scaleAnim.value * idleBreathe)
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2f, height / 2f)

            // Dynamic Aura for energetic moods
            if (mood == RockMood.HYPERDRIVE || mood == RockMood.ENERGETIC) {
                val auraRadius = (width / 2f) + if (mood == RockMood.HYPERDRIVE) 26f else 14f
                val auraColor = if (mood == RockMood.HYPERDRIVE) Color(0xFFF43F5E).copy(alpha = 0.35f)
                else Color(0xFF38BDF8).copy(alpha = 0.25f)

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(auraColor, Color.Transparent),
                        center = center,
                        radius = auraRadius
                    ),
                    radius = auraRadius,
                    center = center
                )
            }

            // Floor drop shadow
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF020617).copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(center.x, height * 0.88f),
                    radius = width * 0.44f
                ),
                topLeft = Offset(width * 0.1f, height * 0.80f),
                size = Size(width * 0.8f, height * 0.18f)
            )

            // Organic Rock Shape
            val rockPath = Path().apply {
                moveTo(width * 0.50f, height * 0.16f)
                // Top-right curve
                cubicTo(
                    width * 0.76f, height * 0.16f,
                    width * 0.90f, height * 0.30f,
                    width * 0.92f, height * 0.52f
                )
                // Bottom-right curve
                cubicTo(
                    width * 0.94f, height * 0.74f,
                    width * 0.82f, height * 0.84f,
                    width * 0.52f, height * 0.85f
                )
                // Bottom-left curve
                cubicTo(
                    width * 0.22f, height * 0.86f,
                    width * 0.08f, height * 0.74f,
                    width * 0.09f, height * 0.52f
                )
                // Top-left curve
                cubicTo(
                    width * 0.10f, height * 0.30f,
                    width * 0.24f, height * 0.16f,
                    width * 0.50f, height * 0.16f
                )
                close()
            }

            // Rock Base Body Gradient
            val rockGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF475569),
                    Color(0xFF334155),
                    Color(0xFF1E293B)
                ),
                start = Offset(width * 0.2f, height * 0.2f),
                end = Offset(width * 0.8f, height * 0.85f)
            )
            drawPath(path = rockPath, brush = rockGradient, style = Fill)

            // Top specular highlight
            val highlightPath = Path().apply {
                moveTo(width * 0.30f, height * 0.22f)
                cubicTo(
                    width * 0.45f, height * 0.18f,
                    width * 0.60f, height * 0.18f,
                    width * 0.72f, height * 0.24f
                )
                cubicTo(
                    width * 0.60f, height * 0.29f,
                    width * 0.42f, height * 0.28f,
                    width * 0.30f, height * 0.22f
                )
                close()
            }
            drawPath(
                path = highlightPath,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF94A3B8).copy(alpha = 0.5f), Color.Transparent)
                )
            )

            // Rock Rim Glow Outline
            val rimColor = when (mood) {
                RockMood.HYPERDRIVE -> Color(0xFFF43F5E)
                RockMood.ENERGETIC -> Color(0xFFF59E0B)
                else -> Color(0xFF38BDF8)
            }
            drawPath(
                path = rockPath,
                color = rimColor.copy(alpha = if (mood == RockMood.CHILL) 0.35f else 0.75f),
                style = Stroke(width = if (mood == RockMood.HYPERDRIVE) 4.5f else 2.5f)
            )

            // Stylized Stone Texture / Creases
            val creasePath = Path().apply {
                moveTo(width * 0.26f, height * 0.38f)
                lineTo(width * 0.34f, height * 0.44f)
                lineTo(width * 0.30f, height * 0.50f)
            }
            drawPath(
                path = creasePath,
                color = Color(0xFF1E293B).copy(alpha = 0.6f),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )

            // Eyes Configuration
            val eyeCenterY = height * 0.48f
            val leftEyeX = width * 0.38f
            val rightEyeX = width * 0.62f
            val eyeRadius = width * 0.058f

            if (isBlinking) {
                // Closed Eyes (Blink lines)
                drawLine(
                    color = Color(0xFFF8FAFC),
                    start = Offset(leftEyeX - eyeRadius, eyeCenterY),
                    end = Offset(leftEyeX + eyeRadius, eyeCenterY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFF8FAFC),
                    start = Offset(rightEyeX - eyeRadius, eyeCenterY),
                    end = Offset(rightEyeX + eyeRadius, eyeCenterY),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round
                )
            } else {
                // Open Expressive Eyes
                // Sclera (White)
                drawCircle(color = Color(0xFFF8FAFC), radius = eyeRadius, center = Offset(leftEyeX, eyeCenterY))
                drawCircle(color = Color(0xFFF8FAFC), radius = eyeRadius, center = Offset(rightEyeX, eyeCenterY))

                // Iris/Pupil (Dark Slate + Cyan Tint)
                val pupilRadius = eyeRadius * 0.58f
                drawCircle(color = Color(0xFF0F172A), radius = pupilRadius, center = Offset(leftEyeX + 1.5f, eyeCenterY))
                drawCircle(color = Color(0xFF0F172A), radius = pupilRadius, center = Offset(rightEyeX + 1.5f, eyeCenterY))

                // Light Catch (Glint)
                val glintRadius = eyeRadius * 0.26f
                drawCircle(color = Color(0xFF38BDF8), radius = glintRadius, center = Offset(leftEyeX + 3f, eyeCenterY - 3f))
                drawCircle(color = Color(0xFF38BDF8), radius = glintRadius, center = Offset(rightEyeX + 3f, eyeCenterY - 3f))
            }

            // Cheeks / Blush
            val blushRadiusX = width * 0.045f
            val blushRadiusY = width * 0.025f
            drawOval(
                color = Color(0xFFF43F5E).copy(alpha = if (mood == RockMood.CHILL) 0.35f else 0.65f),
                topLeft = Offset(leftEyeX - (eyeRadius * 1.5f), eyeCenterY + (eyeRadius * 0.7f)),
                size = Size(blushRadiusX * 2, blushRadiusY * 2)
            )
            drawOval(
                color = Color(0xFFF43F5E).copy(alpha = if (mood == RockMood.CHILL) 0.35f else 0.65f),
                topLeft = Offset(rightEyeX + (eyeRadius * 0.4f), eyeCenterY + (eyeRadius * 0.7f)),
                size = Size(blushRadiusX * 2, blushRadiusY * 2)
            )

            // Mouth Shape based on Mood
            val mouthY = height * 0.62f
            when (mood) {
                RockMood.HYPERDRIVE -> {
                    // Wide Excited Open Mouth
                    val openMouthPath = Path().apply {
                        moveTo(width * 0.43f, mouthY)
                        cubicTo(
                            width * 0.45f, mouthY + 22f,
                            width * 0.55f, mouthY + 22f,
                            width * 0.57f, mouthY
                        )
                        close()
                    }
                    drawPath(path = openMouthPath, color = Color(0xFF0F172A), style = Fill)
                    drawPath(
                        path = openMouthPath,
                        color = Color(0xFFF43F5E),
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                    )
                }
                RockMood.ENERGETIC, RockMood.HAPPY -> {
                    // Big Happy Smile
                    val smilePath = Path().apply {
                        moveTo(width * 0.42f, mouthY)
                        cubicTo(
                            width * 0.47f, mouthY + 16f,
                            width * 0.53f, mouthY + 16f,
                            width * 0.58f, mouthY
                        )
                    }
                    drawPath(
                        path = smilePath,
                        color = Color(0xFFF8FAFC),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }
                RockMood.CHILL -> {
                    // Gentle Chill Smile
                    val chillPath = Path().apply {
                        moveTo(width * 0.45f, mouthY)
                        cubicTo(
                            width * 0.48f, mouthY + 8f,
                            width * 0.52f, mouthY + 8f,
                            width * 0.55f, mouthY
                        )
                    }
                    drawPath(
                        path = chillPath,
                        color = Color(0xFFF8FAFC),
                        style = Stroke(width = 3f, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}
