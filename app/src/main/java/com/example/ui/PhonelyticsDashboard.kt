package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PhonelyticsTheme
import com.example.viewmodel.PhonelyticsViewModel
import com.example.viewmodel.ActiveDeviceMetrics
import com.example.viewmodel.RunningAppInfo
import com.example.viewmodel.SpammedNotification
import com.example.data.OptimizationLog
import com.example.data.SystemStatSnapshot
import com.example.data.PrivacyScanResult
import androidx.compose.ui.draw.scale
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// Enum for Dashboard Screen Sections
enum class DashboardSection(val displayName: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Default.Info),
    PERFORMANCE("Performance", Icons.Default.Build),
    BATTERY_NET("Battery & Net", Icons.Default.LocationOn),
    STORAGE_APPS("Storage & Apps", Icons.Default.Share),
    PRIVACY_SPAM("Privacy & Spam", Icons.Default.Lock),
    LOG_HISTORY("Audit Logs", Icons.Default.List)
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun PhonelyticsDashboard(viewModel: PhonelyticsViewModel) {
    val themePreset by viewModel.themePreset.collectAsState()
    val isDark by viewModel.darkTheme.collectAsState()
    val amoled by viewModel.amoledMode.collectAsState()

    val metrics by viewModel.metrics.collectAsState()
    val runningApps by viewModel.runningApps.collectAsState()
    val notifications by viewModel.spammedNotifications.collectAsState()
    val optimizationLogs by viewModel.optimizationLogs.collectAsState()
    val historicalSnapshots by viewModel.statSnapshotHistory.collectAsState()
    val privacyScans by viewModel.privacyAuditResults.collectAsState()
    val lagExplainer by viewModel.lagExplainer.collectAsState()

    // Interactive state settings
    val isGamingActive by viewModel.gamingModeActive.collectAsState()
    val overlayEnabled by viewModel.floatingCpuOverlayEnabled.collectAsState()
    val pocketActive by viewModel.pocketShieldActive.collectAsState()
    val autoCooling by viewModel.automaticCoolingActive.collectAsState()
    val spamFilter by viewModel.filterSpamActive.collectAsState()

    // Screen navigation
    var currentSection by remember { mutableStateOf(DashboardSection.OVERVIEW) }

    // Floating dynamic island animation state
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    PhonelyticsTheme(
        selectedTheme = themePreset,
        darkTheme = isDark,
        amoledMode = amoled
    ) {
        val colors = MaterialTheme.colorScheme

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = colors.background,
            bottomBar = {
                // Bottom translucent navigation pill bar
                BottomTranslucentNavBar(
                    currentSection = currentSection,
                    onSectionSelected = { currentSection = it },
                    accentColor = colors.primary,
                    bgColor = colors.background
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    // Animated gradient radial backdrops representing ambient energy fields
                    .drawBehind {
                        if (!amoled) {
                            // Immersive UI top blue glowing ambient field
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF1E3A8A).copy(alpha = 0.45f), Color.Transparent),
                                    center = Offset(size.width * 0.5f, -size.height * 0.12f),
                                    radius = size.height * 0.62f
                                )
                            )
                            // Secondary subtle bottom-right glowing ambient field for aesthetic rhythm
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(colors.primary.copy(alpha = 0.08f), Color.Transparent),
                                    center = Offset(size.width * 0.8f, size.height * 0.82f),
                                    radius = size.width * 0.75f
                                )
                            )
                        }
                    }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Header Module with System configuration Controls
                    DashboardHeader(
                        isDark = isDark,
                        useAmoled = amoled,
                        activePreset = themePreset,
                        onThemeChanged = { viewModel.setThemePreset(it) },
                        onToggleDark = { viewModel.toggleDarkTheme() },
                        onToggleAmoled = { viewModel.toggleAmoled() },
                        primaryAccent = colors.primary,
                        pulseFactor = pulseScale
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick-Action Dynamic Health Status Island
                    DynamicStatusIsland(
                        overallHealthScore = metrics.stabilityScore,
                        thermalState = metrics.thermalState,
                        lagStatus = if (isGamingActive) "Gaming Turbo Active" else "Safe Environment",
                        pulseFactor = pulseScale,
                        accentColor = colors.primary,
                        onOneTapOptimized = { viewModel.triggerOneTapSpeedup() }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Main scrolling area showing active screen section with smooth fade slide transition
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        AnimatedContent(
                            targetState = currentSection,
                            transitionSpec = {
                                slideInHorizontally { width -> if (targetState.ordinal > initialState.ordinal) width else -width } + fadeIn() with
                                        slideOutHorizontally { width -> if (targetState.ordinal > initialState.ordinal) -width else width } + fadeOut()
                            },
                            label = "screen_navigation"
                        ) { section ->
                            when (section) {
                                DashboardSection.OVERVIEW -> OverviewScreen(
                                    metrics = metrics,
                                    historicalSnapshots = historicalSnapshots,
                                    isGamingActive = isGamingActive,
                                    onToggleGaming = { viewModel.gamingModeActive.value = !isGamingActive },
                                    onOneClickBoost = { viewModel.triggerOneTapSpeedup() }
                                )
                                DashboardSection.PERFORMANCE -> PerformanceLabScreen(
                                    metrics = metrics,
                                    lagExplainer = lagExplainer,
                                    isGamingActive = isGamingActive,
                                    overlapEnabled = overlayEnabled,
                                    onToggleGaming = { viewModel.gamingModeActive.value = !isGamingActive },
                                    onToggleOverlay = { viewModel.floatingCpuOverlayEnabled.value = !overlayEnabled },
                                    onCoolProcessor = { viewModel.triggerRAMFlashCooler() }
                                )
                                DashboardSection.BATTERY_NET -> BatteryNetworksScreen(
                                    metrics = metrics,
                                    pocketActive = pocketActive,
                                    onTogglePocket = { viewModel.pocketShieldActive.value = !pocketActive },
                                    autoCooling = autoCooling,
                                    onToggleAutoCool = { viewModel.automaticCoolingActive.value = !autoCooling }
                                )
                                DashboardSection.STORAGE_APPS -> StorageBehaviorScreen(
                                    metrics = metrics,
                                    runningApps = runningApps,
                                    onCleanAllJunk = { viewModel.triggerJunkCleanup() },
                                    onToggleFreeze = { viewModel.toggleAppFreezeState(it) }
                                )
                                DashboardSection.PRIVACY_SPAM -> PrivacySpamScreen(
                                    privacyScans = privacyScans,
                                    notifications = notifications,
                                    spamFilter = spamFilter,
                                    onToggleSpamFilter = { viewModel.filterSpamActive.value = !spamFilter },
                                    onMuteApp = { viewModel.blockNotificationApp(it) },
                                    onClearSpam = { viewModel.clearNotificationsSpam() },
                                    onUpdateTrust = { pkg, trust -> viewModel.optimizeAppTrustState(pkg, trust) }
                                )
                                DashboardSection.LOG_HISTORY -> AuditLogsScreen(
                                    optimizationLogs = optimizationLogs,
                                    onClearLogs = { viewModel.clearAllOptimizationLogs() }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(72.dp)) // padding for the translucent nav overlap
                }
            }
        }
    }
}

// ============================================================
// 1. NEON GLASS CARD WRAPPER
// ============================================================
@Composable
fun NeoGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.09f),
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                shape = RoundedCornerShape(24.dp)
            )
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(24.dp)
            )
            .padding(16.dp)
    ) {
        Column { content() }
    }
}

// ============================================================
// 2. HEADER MODULE & PRESETS DROPDOWN
// ============================================================
@Composable
fun DashboardHeader(
    isDark: Boolean,
    useAmoled: Boolean,
    activePreset: PhonelyticsTheme,
    onThemeChanged: (PhonelyticsTheme) -> Unit,
    onToggleDark: () -> Unit,
    onToggleAmoled: () -> Unit,
    primaryAccent: Color,
    pulseFactor: Float
) {
    var expandedThemeDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Elegant glowing circular badge of state matching Immersive UI
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(primaryAccent.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, primaryAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(primaryAccent, CircleShape)
                        .drawBehind {
                            drawCircle(
                                color = primaryAccent.copy(alpha = 0.45f * (1.1f - pulseFactor)),
                                radius = size.width * (1.5f + pulseFactor * 1.5f)
                            )
                        }
                )
            }

            Column {
                Text(
                    text = "PHONELYTICS AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = primaryAccent
                    )
                )
                Text(
                    text = "Intelligence Lab",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Theme palette picker button
            Box {
                IconButton(
                    onClick = { expandedThemeDropdown = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(primaryAccent.copy(alpha = 0.15f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Themes",
                        tint = primaryAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = expandedThemeDropdown,
                    onDismissRequest = { expandedThemeDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    PhonelyticsTheme.values().forEach { preset ->
                        DropdownMenuItem(
                            text = { Text(preset.displayName, style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onThemeChanged(preset)
                                expandedThemeDropdown = false
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            when (preset) {
                                                PhonelyticsTheme.LAVENDER_FROST -> Color(0xFFB19FFB)
                                                PhonelyticsTheme.CYBER_MINT -> Color(0xFF26F5A3)
                                                PhonelyticsTheme.OCEAN_BLUE -> Color(0xFF4DBBFA)
                                                PhonelyticsTheme.SAKURA_PINK -> Color(0xFFF396B2)
                                                PhonelyticsTheme.SUNSET_PEACH -> Color(0xFFFF9472)
                                                PhonelyticsTheme.ARCTIC_WHITE -> Color(0xFF8CE3FF)
                                                PhonelyticsTheme.NEON_PURPLE -> Color(0xFFD54CFF)
                                            },
                                            CircleShape
                                        )
                                )
                            }
                        )
                    }
                }
            }

            // Dark/Light toggle
            IconButton(
                onClick = onToggleDark,
                modifier = Modifier
                    .size(36.dp)
                    .background(primaryAccent.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Default.Warning else Icons.Default.CheckCircle,
                    contentDescription = "Mode Toggle",
                    tint = primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }

            // AMOLED toggle
            IconButton(
                onClick = onToggleAmoled,
                modifier = Modifier
                    .size(36.dp)
                    .background(if (useAmoled) primaryAccent else primaryAccent.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "AMOLED Toggle",
                    tint = if (useAmoled) Color.Black else primaryAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ============================================================
// 3. DYNAMIC STATUS ISLAND WIDGET
// ============================================================
@Composable
fun DynamicStatusIsland(
    overallHealthScore: Int,
    thermalState: String,
    lagStatus: String,
    pulseFactor: Float,
    accentColor: Color,
    onOneTapOptimized: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(pulseFactor)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.25f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(
                    colors = listOf(accentColor, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f))
                ),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable(onClick = onOneTapOptimized)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // High-Contrast Circle Gauge
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(46.dp)) {
                    CircularProgressIndicator(
                        progress = overallHealthScore / 100f,
                        color = accentColor,
                        strokeWidth = 4.dp,
                        trackColor = accentColor.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "$overallHealthScore%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    )
                }

                Column {
                    Text(
                        text = "System Wellness Index",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Thermals: $thermalState • $lagStatus",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    )
                }
            }

            // High-Performance Pill Button
            Button(
                onClick = onOneTapOptimized,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text("OPTIMIZE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
            }
        }
    }
}



// ============================================================
// 4. OVERVIEW DASHBOARD SCREEN
// ============================================================
@Composable
fun OverviewScreen(
    metrics: ActiveDeviceMetrics,
    historicalSnapshots: List<SystemStatSnapshot>,
    isGamingActive: Boolean,
    onToggleGaming: () -> Unit,
    onOneClickBoost: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Circular Speedometer CPU Load
                NeoGlassCard(modifier = Modifier.weight(1f)) {
                    Text(
                        "CPU USAGE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.align(Alignment.CenterHorizontally).size(85.dp)) {
                        CircularProgressIndicator(
                            progress = metrics.cpuAvg / 100f,
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 6.dp,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${metrics.cpuAvg.toInt()}%",
                                style = MaterialTheme.typography.displayLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            )
                            Text(
                                "Live Load",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color.Gray)
                            )
                        }
                    }
                }

                // Dynamic Glassmorphic Core Temperature
                NeoGlassCard(modifier = Modifier.weight(1f)) {
                    Text(
                        "THERMAL CENTER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "${metrics.cpuTemp}°C",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                metrics.cpuTemp >= 40f -> Color.Red
                                metrics.cpuTemp >= 36f -> Color(0xFFFFB300)
                                else -> MaterialTheme.colorScheme.primary
                            }
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Throttling: ${metrics.thermalState}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Live CPU & Net Sparkline Graphs
        item {
            NeoGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "HISTORICAL INTEL SYSTEM SPARKLINE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Live Graph",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                val dataPoints = historicalSnapshots.map { it.cpuAvg }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                ) {
                    MetricsSparkline(
                        points = dataPoints,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "60-sec continuous processing activity logging pipeline",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // RAM & Memory pressure levels
        item {
            NeoGlassCard {
                Text(
                    "RAM INTEGRITY CONTEXT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Memory Load: ${metrics.ramPercentage.toInt()}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${(metrics.totalRamBytes - metrics.availableRamBytes) / 1024 / 1024 / 1024} GB Used of 8 GB",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = Color.Gray)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = metrics.ramPercentage / 100f,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Quick Overview status widgets
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Battery
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Battery", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Battery state", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${metrics.batteryPct}% • ${metrics.batteryHealthStatus}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Cyber Security Index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Security", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("App audit level", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text(metrics.securityLevel, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // Optimization panel triggers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onOneClickBoost,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "RAM boost", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RAM FLASH")
                }

                Button(
                    onClick = onToggleGaming,
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isGamingActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                        contentColor = if (isGamingActive) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Gaming Mode")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isGamingActive) "TURBO ACTIVE" else "GAMING MOD")
                }
            }
        }
    }
}

// ============================================================
// 5. PERFORMANCE LAB & THERMALS SCREEN
// ============================================================
@Composable
fun PerformanceLabScreen(
    metrics: ActiveDeviceMetrics,
    lagExplainer: String,
    isGamingActive: Boolean,
    overlapEnabled: Boolean,
    onToggleGaming: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCoolProcessor: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Core processor allocation load
        item {
            NeoGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CORES INTEL CORE-BY-CORE TRACKER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onCoolProcessor, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Cool Processors", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Core frequencies and cores load
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val halfIndex = metrics.coreCpus.size / 2
                    for (i in 0 until halfIndex) {
                        val c1 = metrics.coreCpus[i]
                        val c2 = metrics.coreCpus[i + halfIndex]
                        val freq1 = metrics.coreFrequencies[i]
                        val freq2 = metrics.coreFrequencies[i + halfIndex]

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Column Core Left
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cpu Core ${i + 1}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp))
                                    Text("$freq1 • ${c1.toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.primary))
                                }
                                LinearProgressIndicator(
                                    progress = c1 / 100f,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                            // Column Core Right
                            Column(modifier = Modifier.weight(1f)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Cpu Core ${i + 5}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp))
                                    Text("$freq2 • ${c2.toInt()}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.primary))
                                }
                                LinearProgressIndicator(
                                    progress = c2 / 100f,
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Lag Predictor & AI Explainers
        item {
            NeoGlassCard(borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "AI DEVICE DIAGNOSTIC ANALYZER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("AI BRAIN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = MaterialTheme.colorScheme.error))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    "Predictive ralentisseur slowdown explanation:",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lagExplainer,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 13.sp, lineHeight = 18.sp)
                )
            }
        }

        // Additional toggles: Floating Overlay, Jetpack Gaming optimization
        item {
            NeoGlassCard {
                Text(
                    "LAB BENCHMARK SYSTEMS MONITOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Floating Overlay Monitor", style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text("Display floating real-time CPU speed above other apps", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                    }
                    Switch(checked = overlapEnabled, onCheckedChange = { onToggleOverlay() })
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Game Turbo Stress Accelerator", style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text("Overclocks processor clocks temporarily for games", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                    }
                    Switch(checked = isGamingActive, onCheckedChange = { onToggleGaming() })
                }
            }
        }
    }
}

// ============================================================
// 6. BATTERY & INTERNET NETWORKS MONITOR SCREEN
// ============================================================
@Composable
fun BatteryNetworksScreen(
    metrics: ActiveDeviceMetrics,
    pocketActive: Boolean,
    onTogglePocket: () -> Unit,
    autoCooling: Boolean,
    onToggleAutoCool: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Battery intelligence systems
        item {
            NeoGlassCard {
                Text(
                    "BATTERY INTELLIGENCE METRIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${metrics.batteryPct}% State of Health: ${metrics.batteryHealthPct}%",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = metrics.screenOnString,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        )
                        Text(
                            text = "Life state: ${metrics.batteryHealthStatus} • Overnight drain rate: ${metrics.overnightDrainPct}%",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Battery Health Indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Linear Battery Health Bar
                LinearProgressIndicator(
                    progress = metrics.batteryPct / 100f,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )
            }
        }

        // Connected Network graph with live speed meter
        item {
            NeoGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "LIVE PACKET SIGNAL ANALYZER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(metrics.currentNetworkType, style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Latency: ${metrics.pingMs} ms", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.tertiary))
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Download speed", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(
                            "${(metrics.downloadKbps / 1024).format(2)} MB/s",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Upload speed", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(
                            "${(metrics.uploadKbps / 1024).format(2)} MB/s",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        )
                    }
                }
            }
        }

        // Pocket Shields & Heat guards
        item {
            NeoGlassCard {
                Text(
                    "SMART PREVENTATIVE HARDWARE SHIELDS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Toggle 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Pocket Heat Sensor Guard", style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text("Warns user if phone detects excessive direct heat when locked in pockets", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                    }
                    Switch(checked = pocketActive, onCheckedChange = { onTogglePocket() })
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))

                // Toggle 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Smart Liquid Cooling Mode", style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text("Automatically sleep processes if thermals approach 42°C", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                    }
                    Switch(checked = autoCooling, onCheckedChange = { onToggleAutoCool() })
                }
            }
        }
    }
}

// Float formatting extension helper
fun Float.format(digits: Int) = String.format("%.${digits}f", this)

// ============================================================
// 7. STORAGE & APP HEALTH BEHAVIOR SCREEN
// ============================================================
@Composable
fun StorageBehaviorScreen(
    metrics: ActiveDeviceMetrics,
    runningApps: List<RunningAppInfo>,
    onCleanAllJunk: () -> Unit,
    onToggleFreeze: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Storage Pie Space Breakdown
        item {
            NeoGlassCard {
                Text(
                    "STORAGE INTEL SUMMARY BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Used space: ${(metrics.totalStorageBytes - metrics.availableStorageBytes).toBytesInGb()} GB",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Free of 128 GB: ${(metrics.availableStorageBytes).toBytesInGb()} GB",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = Color.Gray)
                        )
                    }

                    if (metrics.junkFilesLeft) {
                        Button(
                            onClick = onCleanAllJunk,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("DEEP CLEAN JUNK", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp))
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("CALIBRATED", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.secondary))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                // Junk types details breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val itemsBreakdown = listOf(
                        Triple("System Cache", metrics.cachedJunkSize, MaterialTheme.colorScheme.primary),
                        Triple("Social Media", metrics.WhatsAppClutter, MaterialTheme.colorScheme.secondary),
                        Triple("Screenshots", metrics.screenShotClutter, MaterialTheme.colorScheme.tertiary)
                    )
                    itemsBreakdown.forEach { (type, size, col) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(col.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(type, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                                Text(size, style = MaterialTheme.typography.titleLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = col))
                            }
                        }
                    }
                }
            }
        }

        // Heavy running processes manager
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "HEAVY RUNNING DAEMONS & CHANNELS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Total processes: ${runningApps.size}",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                )
            }
        }

        // Listing dynamic background processes
        items(runningApps) { app ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                    .border(
                        width = 1.dp,
                        color = if (app.isAppFrozen) Color.Gray.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                app.name,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (app.isAppFrozen) Color.Gray else MaterialTheme.colorScheme.onBackground
                                )
                            )
                            if (app.isAppFrozen) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text("FROZEN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, color = Color.Gray))
                                }
                            }
                        }
                        Text(app.packageName, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                        Text(
                            text = "RAM: ${app.ramUsedMb} MB • Battery Drain: ${app.batteryImpact} • WakeLocks: ${app.wakeLocks}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 9.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        )
                    }

                    Button(
                        onClick = { onToggleFreeze(app.name) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (app.isAppFrozen) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.05f),
                            contentColor = if (app.isAppFrozen) Color.Black else MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Text(
                            text = if (app.isAppFrozen) "DEFROST" else "FORCE FREEZE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp)
                        )
                    }
                }
            }
        }
    }
}

// Byte converter helper
fun Long.toBytesInGb(): String {
    val gb = this.toFloat() / 1024f / 1024f / 1024f
    return String.format("%.1f", gb)
}

// ============================================================
// 8. PRIVACY AUDITOR & NOTIFICATION SPAM SCREEN
// ============================================================
@Composable
fun PrivacySpamScreen(
    privacyScans: List<PrivacyScanResult>,
    notifications: List<SpammedNotification>,
    spamFilter: Boolean,
    onToggleSpamFilter: () -> Unit,
    onMuteApp: (String) -> Unit,
    onClearSpam: () -> Unit,
    onUpdateTrust: (String, Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Privacy Safety Score Widget
        item {
            NeoGlassCard {
                Text(
                    "PRIVACY INTEGRITY & SENSOR AUDIT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Risk Score Analysis", style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                        Text("Verifying clipboard access alerts and device location status...", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, color = Color.Gray))
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text("High Risk", style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                // Sensor stats lists
                privacyScans.forEach { scan ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(scan.appName, style = MaterialTheme.typography.titleLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            Text("Sensitive Permissions: ${scan.permissionsGranted}", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                            Text("Score: ${scan.riskScore}/100", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = if (scan.riskScore > 70) Color.Red else Color.Green))
                        }

                        Button(
                            onClick = { onUpdateTrust(scan.packageName, !scan.isSandboxTrusted) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (scan.isSandboxTrusted) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.05f)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text(
                                if (scan.isSandboxTrusted) "TRUSTED" else "UNTRUSTED",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = if (scan.isSandboxTrusted) Color.Black else Color.Gray)
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                }
            }
        }

        // Notification spam list section
        item {
            NeoGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("NOTIFICATION SPAM CHANNELS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        Text("System Spam Interceptor", style = MaterialTheme.typography.titleLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    }

                    Switch(checked = spamFilter, onCheckedChange = { onToggleSpamFilter() })
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (notifications.isEmpty()) {
                    Text(
                        "Pristine environment! No active alert headers detected.",
                        style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spam alert logs", style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray))
                        Text(
                            "Clear all",
                            modifier = Modifier
                                .clickable { onClearSpam() }
                                .padding(4.dp),
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    notifications.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("${item.appName}: ${item.title}", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                                Text("Count: ${item.messageCount} alerts blocked today", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 10.sp, color = Color.Gray))
                            }

                            IconButton(onClick = { onMuteApp(item.appName) }, modifier = Modifier.size(28.dp)) {
                                Icon(imageVector = Icons.Default.Warning, contentDescription = "Mute App", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

// ============================================================
// 9. AUDIT LOGS HISTORY SCREEN (Room Logs)
// ============================================================
@Composable
fun AuditLogsScreen(
    optimizationLogs: List<OptimizationLog>,
    onClearLogs: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "HISTORICAL LOCAL DATABASE TRACKS",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (optimizationLogs.isNotEmpty()) {
                Text(
                    "CLEAR AUDIT",
                    modifier = Modifier
                        .clickable { onClearLogs() }
                        .padding(6.dp),
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.error)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (optimizationLogs.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No database logging entries recorded yet.\nConduct a calibration optimization above.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray))
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(optimizationLogs) { log ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    log.type,
                                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                                Text(log.details, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp))
                                Text(
                                    "Resources calibrated: ${log.amountFreed}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    formatter.format(Date(log.timestamp)),
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, color = Color.Gray)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// 10. TRANSLUCENT NAVIGATION PILL BAR Composable
// ============================================================
@Composable
fun BottomTranslucentNavBar(
    currentSection: DashboardSection,
    onSectionSelected: (DashboardSection) -> Unit,
    accentColor: Color,
    bgColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        // Translucent Glassmorphic Row container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(bgColor.copy(alpha = 0.76f), RoundedCornerShape(26.dp))
                .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(26.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            DashboardSection.values().forEach { sect ->
                val selected = currentSection == sect
                val factor by animateFloatAsState(if (selected) 1.15f else 1.0f, label = "bounceScale")

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .scale(factor)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onSectionSelected(sect) }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = sect.icon,
                        contentDescription = sect.displayName,
                        tint = if (selected) accentColor else accentColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = sect.displayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 8.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) accentColor else accentColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

// ============================================================
// 11. CUSTOM CANVAS DRAWN SPARKLINE ( bezier Splines)
// ============================================================
@Composable
fun MetricsSparkline(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas

        val maxVal = points.maxOrNull() ?: 100f
        val minVal = points.minOrNull() ?: 0f
        val diff = if (maxVal == minVal) 100f else (maxVal - minVal)

        val spacing = size.width / (points.size - 1)
        val path = Path()

        for (i in points.indices) {
            val pointValue = points[i]
            val prevX = (i - 1) * spacing
            val prevY = size.height - (size.height * ((points.getOrNull(i - 1) ?: 0f) - minVal) / diff)
            val currentX = i * spacing
            val currentY = size.height - (size.height * (pointValue - minVal) / diff)

            if (i == 0) {
                path.moveTo(currentX, currentY)
            } else {
                // Draw elegant curved bezier path
                path.cubicTo(
                    (prevX + currentX) / 2f, prevY,
                    (prevX + currentX) / 2f, currentY,
                    currentX, currentY
                )
            }
        }

        // Draw spline strokes
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw filling areas under graphs
        val fillPath = Path().apply {
            addPath(path)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
            )
        )
    }
}
