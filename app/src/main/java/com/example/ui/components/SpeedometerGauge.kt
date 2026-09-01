package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AntiCheat
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerGauge(
    cps: Int,
    modifier: Modifier = Modifier
) {
    val animatedCps by animateFloatAsState(
        targetValue = cps.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "cps_gauge"
    )

    val progress = (animatedCps / AntiCheat.MAX_HUMAN_CPS).coerceIn(0f, 1f)

    val gaugeColor = when {
        cps >= 20 -> Color(0xFFF43F5E) // Red / near limit
        cps >= 12 -> Color(0xFFF59E0B) // Amber
        cps >= 5 -> Color(0xFF38BDF8) // Cyan
        else -> Color(0xFF64748B) // Slate
    }

    Box(
        modifier = modifier
            .testTag("speedometer_gauge")
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF1E293B).copy(alpha = 0.8f))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speedometer Arc & Value
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp)) {
                    Canvas(modifier = Modifier.size(42.dp)) {
                        val strokeWidth = 5.dp.toPx()
                        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                        // Background Arc (180 degrees)
                        drawArc(
                            color = Color(0xFF334155),
                            startAngle = 160f,
                            sweepAngle = 220f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Active Progress Arc
                        if (progress > 0.01f) {
                            drawArc(
                                color = gaugeColor,
                                startAngle = 160f,
                                sweepAngle = 220f * progress,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Hız Göstergesi",
                        tint = gaugeColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$cps",
                            style = MaterialTheme.typography.titleLarge.copy(
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp
                            )
                        )
                        Text(
                            text = " / 25 CPS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = if (cps >= 20) "⚡ Maksimum İnsan Hızı!" else "Tıklama Hızı Limiti",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (cps >= 20) Color(0xFFF43F5E) else Color(0xFF64748B),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Anti-Cheat Active Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Anti-Cheat Aktif",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "HMAC KORUMALI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.5.sp,
                            letterSpacing = 0.5.sp
                        )
                    )
                }
            }
        }
    }
}
