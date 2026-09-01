package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.security.AntiCheat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchitectureModal(
    userId: String,
    username: String,
    totalLocalClicks: Long,
    unsyncedClicks: Long,
    isConnected: Boolean,
    onlineCount: Int,
    serverUrl: String,
    onUpdateServerUrl: (String) -> Unit,
    onForceSync: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var urlInput by remember { mutableStateOf(serverUrl) }

    val sampleSignature = remember(userId, unsyncedClicks) {
        AntiCheat.signBatch(userId, System.currentTimeMillis(), (if (unsyncedClicks > 0) unsyncedClicks else 5).toInt())
    }

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
                .testTag("architecture_modal")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Mimari",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GERÇEK ZAMANLI SİSTEM MİMARİSİ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kapat",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Profile Identity Card
            ArchitectureCard(
                icon = Icons.Default.Person,
                title = "0. Doğrulanmış Oyuncu Kimliği",
                subtitle = "Her oyuncu benzersiz bir rumuza ve kriptografik cihaz kimliğine sahiptir.",
                statusColor = Color(0xFF10B981),
                statusText = "KAYITLI"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Oyuncu Rumuzu:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                        Text(
                            text = if (username.isNotBlank()) username else "Kayıtsız",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Benzersiz ID:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                        Text(
                            text = userId,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFCBD5E1),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer 1: Local-First Room SQLite
            ArchitectureCard(
                icon = Icons.Default.Storage,
                title = "1. Local-First SQLite (Room DB)",
                subtitle = "Tüm gerçek tıklamalar cihazda anında ve kalıcı olarak yerel veritabanına yazılır. Ağ bağlantısı olmasa bile veri kaybı imkansızdır.",
                statusColor = Color(0xFF10B981),
                statusText = "AKTİF / SENKRON"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Yerel Toplam:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                        Text("$totalLocalClicks Tık", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFFF8FAFC), fontWeight = FontWeight.Bold))
                    }
                    Column {
                        Text("Kuyruktaki Tıklar:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                        Text("$unsyncedClicks Tık", style = MaterialTheme.typography.bodyMedium.copy(color = if (unsyncedClicks > 0) Color(0xFFF59E0B) else Color(0xFF10B981), fontWeight = FontWeight.Bold))
                    }
                    Column {
                        Text("DB Sürümü:", style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8)))
                        Text("Room SQLite", style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer 2: Anti-Cheat Engine & HMAC-SHA256
            ArchitectureCard(
                icon = Icons.Default.Lock,
                title = "2. Anti-Cheat & HMAC-SHA256 İmzalama",
                subtitle = "İstemci tarafında 25 CPS donanımsal tıklama filtresi ve SHA256 mesaj bütünlüğü doğrulaması uygulanır.",
                statusColor = Color(0xFF38BDF8),
                statusText = "HMAC KORUMALI"
            ) {
                Column {
                    Text(
                        text = "Canlı HMAC-SHA256 İmzası Örneği:",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F172A))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = sampleSignature,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            maxLines = 2
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Maksimum Hız Limiti: 25 Tık / Saniye (Minimum 40ms tık aralığı)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B), fontSize = 10.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer 3: Cloud Realtime Sync & Server Connection
            ArchitectureCard(
                icon = Icons.Default.CloudSync,
                title = "3. Bulut Gerçek Zamanlı Eşitleme",
                subtitle = "Her 5 saniyede bir birikmiş gerçek tıklama paketleri sunucuya HMAC imzalı olarak aktarılır.",
                statusColor = if (isConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                statusText = if (isConnected) "ONLINE BULUT AKTİF" else "YEREL ÇEVRİMDIŞI"
            ) {
                Column {
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Bulut Sunucu API Adresi", fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFFF8FAFC),
                            unfocusedTextColor = Color(0xFFF8FAFC),
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onUpdateServerUrl(urlInput) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Adresi Güncelle", color = Color(0xFFF8FAFC), fontSize = 12.sp)
                        }

                        Button(
                            onClick = onForceSync,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Şimdi Eşitle", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Layer 4: Backend Pipeline Spec
            ArchitectureCard(
                icon = Icons.Default.DataObject,
                title = "4. Dağıtık Arka Plan Boru Hattı",
                subtitle = "Yüksek eşzamanlılık için tasarlanan veri akışı.",
                statusColor = Color(0xFF818CF8),
                statusText = "MİMARİ DİYAGRAMI"
            ) {
                Column {
                    Text(
                        text = "Cihaz Dokunuşu -> SQLite Room (0ms) -> Rate Limiter (25 CPS) -> HMAC İmza -> HTTPS/WSS Batch -> Redis HyperLogLog / In-Memory Aggregator -> Global Total & Leaderboard",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            lineHeight = 16.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ArchitectureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusColor: Color,
    statusText: String,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = statusColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFF94A3B8),
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            content()
        }
    }
}
