package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.sync.LeaderboardEntry
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardModal(
    topCountries: List<LeaderboardEntry>,
    topUsers: List<LeaderboardEntry>,
    userClicks: Long,
    userId: String,
    username: String,
    countryCode: String,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val numberFormat = remember { NumberFormat.getInstance(Locale("tr", "TR")) }

    // Calculate user real rank in the list
    val calculatedUserRank = remember(topUsers, userClicks, userId) {
        val foundIndex = topUsers.indexOfFirst { it.identifier == userId || it.username.equals(username, ignoreCase = true) }
        if (foundIndex >= 0) {
            foundIndex + 1
        } else {
            val countAhead = topUsers.count { it.clicks > userClicks }
            countAhead + 1
        }
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
                .padding(bottom = 24.dp)
                .testTag("leaderboard_modal")
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Liderlik",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GERÇEK ZAMANLI LİDERLİK",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
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

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs
            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Country Wars Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) Color(0xFF38BDF8) else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌍 Ülke Sıralaması",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selectedTab == 0) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // Individual Users Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) Color(0xFF38BDF8) else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "👤 Oyuncular",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = if (selectedTab == 1) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Leaderboard Items List
            val itemsToDisplay = if (selectedTab == 0) topCountries else topUsers

            if (itemsToDisplay.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🚀",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Henüz kayıtlı tıklama yok!",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color(0xFFF8FAFC),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Taşa dokunarak ilk rekoru sen kır!",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF64748B)
                            )
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .padding(horizontal = 20.dp)
                ) {
                    items(itemsToDisplay) { item ->
                        LeaderboardRow(
                            item = item,
                            isCountryTab = selectedTab == 0,
                            numberFormat = numberFormat,
                            isCurrentUser = selectedTab == 1 && (item.identifier == userId || item.username.equals(username, ignoreCase = true))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User's Fixed Position Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#$calculatedUserRank",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = countryCodeToEmoji(countryCode),
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (username.isNotBlank()) username else "Sen",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = Color(0xFFF8FAFC),
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Text(
                                text = "Senin Gerçek Skorun",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B)
                                )
                            )
                        }
                    }

                    Text(
                        text = "${numberFormat.format(userClicks)} Tık",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LeaderboardRow(
    item: LeaderboardEntry,
    isCountryTab: Boolean,
    numberFormat: NumberFormat,
    isCurrentUser: Boolean
) {
    val isTop3 = item.rank <= 3
    val rankColor = when (item.rank) {
        1 -> Color(0xFFF59E0B) // Gold
        2 -> Color(0xFFCBD5E1) // Silver
        3 -> Color(0xFFD97706) // Bronze
        else -> Color(0xFF64748B)
    }

    val rankBadgeText = when (item.rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#${item.rank}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isCurrentUser) Color(0xFF38BDF8).copy(alpha = 0.15f)
                else Color(0xFF162032)
            )
            .border(
                width = 1.dp,
                color = if (isCurrentUser) Color(0xFF38BDF8)
                else if (isTop3) rankColor.copy(alpha = 0.4f)
                else Color(0xFF334155).copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = rankBadgeText,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = rankColor,
                        fontWeight = if (isTop3) FontWeight.Black else FontWeight.Bold,
                        fontSize = if (isTop3) 16.sp else 13.sp
                    )
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Identifier & Flag
            if (isCountryTab) {
                val flag = countryCodeToEmoji(item.countryCode)
                val countryName = countryCodeToName(item.countryCode)
                Text(text = flag, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = countryName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = item.countryCode,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            } else {
                val flag = countryCodeToEmoji(item.countryCode)
                Text(text = flag, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.username,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (isCurrentUser) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                                fontWeight = FontWeight.Bold
                            )
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "(SEN)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Click Count
            Text(
                text = numberFormat.format(item.clicks),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color(0xFF38BDF8),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            )
        }
    }
}

fun countryCodeToEmoji(code: String): String {
    if (code.length != 2) return "🌐"
    val first = Character.codePointAt(code.uppercase(), 0) - 0x41 + 0x1F1E6
    val second = Character.codePointAt(code.uppercase(), 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(first)) + String(Character.toChars(second))
}

fun countryCodeToName(code: String): String {
    return when (code.uppercase()) {
        "TR" -> "Türkiye"
        "AZ" -> "Azerbaycan"
        "DE" -> "Almanya"
        "US" -> "ABD"
        "GB" -> "Birleşik Krallık"
        "FR" -> "Fransa"
        "JP" -> "Japonya"
        "BR" -> "Brezilya"
        "NL" -> "Hollanda"
        "KR" -> "Güney Kore"
        else -> code
    }
}
