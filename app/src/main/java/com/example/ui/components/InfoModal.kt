package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoModal(
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F172A),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF475569))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
                .testTag("info_modal")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🪨", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SANAL TAŞ NEDİR?",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TapWorldIllustration()

            Spacer(modifier = Modifier.height(20.dp))

            InfoCard(
                emoji = "🎯",
                title = "Bir Taş. Bir Amacı Yok.",
                accentColor = Color(0xFF38BDF8)
            ) {
                Text(
                    text = "Elindeki bu taş hiçbir işe yaramıyor ve bu tam olarak amaç. Tek görevi, senin ona dokunduğunu hatırlamak ve bunu dünyanın dört bir yanındaki tüm taşseverlerle paylaşmak. Faydası yok, keyfi var.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                emoji = "🗺️",
                title = "Dokunuşun Nereye Gidiyor?",
                accentColor = Color(0xFF10B981)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepRow("👆", "Dokun", "Taş anında tepki verir, senin sayacın bir artar.")
                    StepRow("📱", "Cihazında Kalır", "İnternetin olmasa bile hiçbir dokunuş kaybolmaz, sırasını bekler.")
                    StepRow("☁️", "Sessizce Gönderilir", "Birkaç saniyede bir dokunuşların, taşın küresel hafızasına fısıldanır.")
                    StepRow("🌍", "Herkes Görür", "Senin dokunuşun, dünyanın öbür ucundaki birinin ekranında da sayılır.")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                emoji = "📜",
                title = "Taş Anayasası",
                accentColor = Color(0xFFF59E0B)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RuleRow("Madde 1", "Taş yargılamaz. Ne kadar dokunursan dokun, o hep aynı sakinlikte.")
                    RuleRow("Madde 2", "Saniyede 25 dokunuştan hızlısı insana yakışmaz — taş buna \"hile\" der ve nazikçe geri çevirir.")
                    RuleRow("Madde 3", "Taş uyumaz. Ama dünyanın öbür ucuyla konuşması bazen biraz sürer; sabırlı ol.")
                    RuleRow("Madde 4", "Taşın ismi yok, senin var. Bir rumuz seç, küresel tarihe geç.")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(
                emoji = "🤔",
                title = "Peki Neden?",
                accentColor = Color(0xFF818CF8)
            ) {
                Text(
                    text = "1975'te insanlar gerçek bir taşı \"evcil hayvan\" diye sattı, dünya bayıldı. Biz bir adım öteye gittik: taşı dijitalleştirdik ve herkesin birlikte dokunabileceği bir hale getirdik. Dağları taşıyamadık ama en azından bir taşa birlikte dokunabiliyoruz. Hiçbir anlamı yok — tam da bu yüzden bu kadar güzel.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        lineHeight = 19.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun StepRow(emoji: String, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFF0F172A)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color(0xFFF8FAFC),
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = body,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    lineHeight = 15.sp
                )
            )
        }
    }
}

@Composable
private fun RuleRow(label: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = Color(0xFFF59E0B),
                    fontWeight = FontWeight.Black,
                    fontSize = 9.5.sp
                )
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color(0xFF94A3B8),
                fontSize = 11.5.sp,
                lineHeight = 16.sp
            ),
            modifier = Modifier.padding(top = 1.dp)
        )
    }
}

@Composable
private fun InfoCard(
    emoji: String,
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1E293B))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

/**
 * Small decorative globe with a few pulsing dots, standing in for taps arriving
 * from random places. Purely illustrative - not tied to real click data.
 */
@Composable
private fun TapWorldIllustration() {
    val infiniteTransition = rememberInfiniteTransition(label = "tap_world")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse"
    )

    val tapDots = remember {
        listOf(
            Offset(0.30f, 0.35f) to 0.15f,
            Offset(0.68f, 0.28f) to 0.55f,
            Offset(0.75f, 0.62f) to 0.0f,
            Offset(0.25f, 0.68f) to 0.75f,
            Offset(0.50f, 0.20f) to 0.35f,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(120.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                color = Color(0xFF1E293B),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color(0xFF334155),
                radius = radius,
                center = center,
                style = Stroke(width = 1.5f)
            )

            // Meridian arcs to suggest a globe
            for (i in 1..2) {
                val rx = radius * (i / 3f) * 2f
                drawOval(
                    color = Color(0xFF334155).copy(alpha = 0.7f),
                    topLeft = Offset(center.x - rx / 2f, center.y - radius),
                    size = Size(rx, radius * 2f),
                    style = Stroke(width = 1.2f)
                )
            }
            drawLine(
                color = Color(0xFF334155).copy(alpha = 0.7f),
                start = Offset(center.x - radius, center.y),
                end = Offset(center.x + radius, center.y),
                strokeWidth = 1.2f
            )

            // Pulsing taps at fixed spots around the globe
            tapDots.forEach { (fraction, phaseOffset) ->
                val localPhase = (pulse + phaseOffset) % 1f
                val dotAlpha = (1f - localPhase).coerceIn(0f, 1f)
                val dotScale = 0.4f + localPhase * 1.4f
                val angle = fraction.x * 2f * Math.PI.toFloat()
                val dist = fraction.y * radius * 0.85f
                val dotCenter = Offset(
                    center.x + cos(angle) * dist,
                    center.y + sin(angle) * dist * 0.6f
                )

                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = dotAlpha * 0.5f),
                    radius = 10f * dotScale,
                    center = dotCenter
                )
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = 3.5f,
                    center = dotCenter
                )
            }
        }
    }
}
