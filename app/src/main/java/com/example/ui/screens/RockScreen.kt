package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.ArchitectureModal
import com.example.ui.components.LeaderboardModal
import com.example.ui.components.NameRegistrationDialog
import com.example.ui.components.PetRockView
import com.example.ui.components.SpeedometerGauge
import com.example.ui.components.countryCodeToEmoji
import com.example.ui.viewmodel.RockViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RockScreen(
    viewModel: RockViewModel = viewModel()
) {
    val localStats by viewModel.localStats.collectAsStateWithLifecycle()
    val serverState by viewModel.serverState.collectAsStateWithLifecycle()
    val currentCps by viewModel.currentCps.collectAsStateWithLifecycle()
    val isLeaderboardOpen by viewModel.isLeaderboardOpen.collectAsStateWithLifecycle()
    val isArchitectureOpen by viewModel.isArchitectureOpen.collectAsStateWithLifecycle()
    val rateLimitWarning by viewModel.rateLimitWarning.collectAsStateWithLifecycle()

    val isUserRegistered by viewModel.isUserRegistered.collectAsStateWithLifecycle()
    val isCheckingName by viewModel.isCheckingName.collectAsStateWithLifecycle()
    val nameErrorMessage by viewModel.nameErrorMessage.collectAsStateWithLifecycle()
    val currentUsername by viewModel.username.collectAsStateWithLifecycle()
    val currentCountryCode by viewModel.countryCode.collectAsStateWithLifecycle()

    val numberFormat = remember { NumberFormat.getInstance(Locale("tr", "TR")) }
    val totalLocalClicks = localStats?.totalClicks ?: 0L
    val unsyncedClicks = localStats?.unsyncedClicks ?: 0L

    // Real global count = server global clicks + any pending unsynced clicks from this device
    val displayedGlobalClicks = serverState.globalClicks + unsyncedClicks

    Scaffold(
        containerColor = Color(0xFF0F172A),
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // User Identity Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = countryCodeToEmoji(currentCountryCode), fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (currentUsername.isNotBlank()) currentUsername else "Oyuncu",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFF8FAFC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }

                    // Online Server Status & Architecture Button
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (serverState.isConnected) Color(0xFF10B981) else Color(0xFFF59E0B))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (serverState.isConnected) "${serverState.onlineCount} Çevrimiçi" else "Local-First",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (serverState.isConnected) Color(0xFF10B981) else Color(0xFFF59E0B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Architecture & System Inspector Button
                        IconButton(
                            onClick = { viewModel.openArchitecture() },
                            modifier = Modifier
                                .testTag("architecture_button")
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = "Mimari Bilgisi",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Global Stats Header
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "GERÇEK KÜRESEL DOKUNMA SAYISI",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF64748B),
                            letterSpacing = 1.8.sp,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = numberFormat.format(displayedGlobalClicks),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color(0xFFF8FAFC),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 34.sp,
                            letterSpacing = (-0.5).sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // World Leaderboard Pill Button
                    Box(
                        modifier = Modifier
                            .testTag("leaderboard_pill_button")
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E293B))
                            .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                            .clickable { viewModel.openLeaderboard() }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌍 Canlı Liderlik Tablosu: #1 ${countryCodeToEmoji(serverState.topCountry)} ${serverState.topCountry}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Detay",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Center Pet Rock Interactive Canvas
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        PetRockView(
                            cps = currentCps,
                            onTap = { viewModel.onTapRock() }
                        )

                        // Rate limit warning banner
                        AnimatedVisibility(
                            visible = rateLimitWarning,
                            enter = fadeIn(tween(150)),
                            exit = fadeOut(tween(300))
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF43F5E).copy(alpha = 0.18f))
                                    .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFF43F5E),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Hile Koruması: Maksimum 25 tık/saniye!",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = Color(0xFFF43F5E),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Panel: Speedometer + Personal Clicks
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    SpeedometerGauge(
                        cps = currentCps,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Personal Clicks & Sync Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SENİN DOKUNUŞLARIN",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B),
                                    letterSpacing = 1.2.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            )
                            Text(
                                text = numberFormat.format(totalLocalClicks),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp
                                )
                            )
                        }

                        // Sync Status Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF162032))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .clickable { viewModel.forceSync() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (unsyncedClicks > 0) Icons.Default.Sync else Icons.Default.CloudDone,
                                    contentDescription = "Senkronizasyon",
                                    tint = if (unsyncedClicks > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (unsyncedClicks > 0) "$unsyncedClicks Senkron Bekliyor" else "Eşitlendi",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (unsyncedClicks > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Onboarding: Name Registration Dialog on first launch
            if (!isUserRegistered) {
                NameRegistrationDialog(
                    initialCountry = currentCountryCode,
                    isChecking = isCheckingName,
                    errorMessage = nameErrorMessage,
                    onSubmit = { name, country ->
                        viewModel.registerUser(name, country)
                    }
                )
            }

            // Leaderboard Modal
            if (isLeaderboardOpen) {
                LeaderboardModal(
                    topCountries = serverState.topCountries,
                    topUsers = serverState.topUsers,
                    userClicks = totalLocalClicks,
                    userId = viewModel.userId,
                    username = currentUsername,
                    countryCode = currentCountryCode,
                    onDismiss = { viewModel.closeLeaderboard() }
                )
            }

            // Architecture Modal
            if (isArchitectureOpen) {
                ArchitectureModal(
                    userId = viewModel.userId,
                    username = currentUsername,
                    totalLocalClicks = totalLocalClicks,
                    unsyncedClicks = unsyncedClicks,
                    isConnected = serverState.isConnected,
                    onlineCount = serverState.onlineCount,
                    serverUrl = serverState.serverUrl,
                    onUpdateServerUrl = { viewModel.updateServerUrl(it) },
                    onForceSync = { viewModel.forceSync() },
                    onDismiss = { viewModel.closeArchitecture() }
                )
            }
        }
    }
}
