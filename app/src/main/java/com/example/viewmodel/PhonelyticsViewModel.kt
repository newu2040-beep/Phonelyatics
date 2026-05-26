package com.example.viewmodel

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.ui.theme.PhonelyticsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

// Holds all real-time diagnostic systems metrics
data class ActiveDeviceMetrics(
    // CPU & GPU
    val cpuAvg: Float = 24f,
    val coreCpus: List<Float> = listOf(35f, 12f, 48f, 8f, 62f, 15f, 22f, 9f),
    val coreFrequencies: List<String> = listOf("2.4 GHz", "1.8 GHz", "2.4 GHz", "1.2 GHz", "2.8 GHz", "1.8 GHz", "1.8 GHz", "1.2 GHz"),
    val gpuUsage: Float = 14f,
    val cpuTemp: Float = 36.5f,
    
    // RAM
    val totalRamBytes: Long = 8L * 1024 * 1024 * 1024,
    val availableRamBytes: Long = 4L * 1024 * 1024 * 1024,
    val ramPercentage: Float = 50f,
    
    // Storage
    val totalStorageBytes: Long = 128L * 1024 * 1024 * 1024,
    val availableStorageBytes: Long = 64L * 1024 * 1024 * 1024,
    val cachedJunkSize: String = "1.24 GB",
    val WhatsAppClutter: String = "850 MB",
    val duplicatePhotos: String = "420 MB",
    val screenShotClutter: String = "280 MB",
    val junkFilesLeft: Boolean = true,
    
    // Battery
    val batteryPct: Int = 85,
    val batteryHealthPct: Int = 94,
    val isCharging: Boolean = false,
    val chargingSpeedMilliAmps: Int = 1800,
    val chargingEfficiency: Int = 92,
    val batteryHealthStatus: String = "Excellent",
    val predictedScreenOnMinutes: Int = 412,
    val screenOnString: String = "6h 52m",
    val overnightDrainPct: Float = 2.4f,
    
    // Network
    val downloadKbps: Float = 1240f,
    val uploadKbps: Float = 340f,
    val pingMs: Int = 18,
    val wifiQualityPct: Int = 82,
    val currentNetworkType: String = "WiFi (5G Dual)",
    val networkStable: Boolean = true,
    
    // System General Health
    val stabilityScore: Int = 96,
    val smoothnessScore: Int = 98,
    val securityLevel: String = "Highly Secured",
    val securityScore: Int = 95,
    val aiPerformanceRating: String = "Tier-1 Ultra",
    val thermalState: String = "Normal", // Normal, Warm, Throttled, Critical
    val pocketsHeatDetected: Boolean = false,
    
    // Digital Wellbeing
    val socialTimeWastedMinutes: Int = 138 // "2.3 hours"
)

// Background App representation
data class RunningAppInfo(
    val name: String,
    val packageName: String,
    val ramUsedMb: Int,
    val batteryImpact: String, // Low, Moderate, Severe
    val wakeLocks: Int,
    val autoStartAllowed: Boolean,
    val isAppFrozen: Boolean = false
)

// Spam Notification item representing notification intelligence
data class SpammedNotification(
    val id: Int,
    val appName: String,
    val title: String,
    val timestamp: String,
    val messageCount: Int
)

class PhonelyticsViewModel(
    private val repository: PhonelyticsRepository,
    private val context: Context
) : ViewModel() {

    // Theme Engine Configurations
    private val _themePreset = MutableStateFlow(PhonelyticsTheme.LAVENDER_FROST)
    val themePreset: StateFlow<PhonelyticsTheme> = _themePreset.asStateFlow()

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _amoledMode = MutableStateFlow(false)
    val amoledMode: StateFlow<Boolean> = _amoledMode.asStateFlow()

    // Real-Time metrics state
    private val _metrics = MutableStateFlow(ActiveDeviceMetrics())
    val metrics: StateFlow<ActiveDeviceMetrics> = _metrics.asStateFlow()

    // Persistent Room Database Logs & Stats
    val optimizationLogs: StateFlow<List<OptimizationLog>> = repository.optimizationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statSnapshotHistory: StateFlow<List<SystemStatSnapshot>> = repository.systemStatHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyAuditResults: StateFlow<List<PrivacyScanResult>> = repository.privacyScans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Running background apps
    private val _runningApps = MutableStateFlow<List<RunningAppInfo>>(emptyList())
    val runningApps: StateFlow<List<RunningAppInfo>> = _runningApps.asStateFlow()

    // Distracting intercepted notifications list (Notification spam)
    private val _spammedNotifications = MutableStateFlow<List<SpammedNotification>>(emptyList())
    val spammedNotifications: StateFlow<List<SpammedNotification>> = _spammedNotifications.asStateFlow()

    // Interactive systems toggles
    val gamingModeActive = MutableStateFlow(false)
    val floatingCpuOverlayEnabled = MutableStateFlow(false)
    val pocketShieldActive = MutableStateFlow(false)
    val automaticCoolingActive = MutableStateFlow(true)
    val filterSpamActive = MutableStateFlow(true)

    // Current diagnostic explainer "Why is my phone lagging?"
    private val _lagExplainer = MutableStateFlow("")
    val lagExplainer: StateFlow<String> = _lagExplainer.asStateFlow()

    // UI Toast or State Notifications message
    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent = _toastEvent.asSharedFlow()

    // Loop jobs
    private var telemetryJob: Job? = null
    private var databaseSnapshotsJob: Job? = null

    init {
        // Prepare initial lists
        initializeRunningAppsList()
        initializeNotificationsSpamList()
        triggerDefaultPrivacyScan()

        // Kick off continuous live telemetry simulation & real metrics queries
        startLiveTelemetry()
        startHistoricalSnapshotLogger()
        updateLagExplainerText()
    }

    fun setThemePreset(preset: PhonelyticsTheme) {
        _themePreset.value = preset
        triggerToast("Swapped theme to ${preset.displayName}")
    }

    fun toggleDarkTheme() {
        _darkTheme.value = !_darkTheme.value
        triggerToast("Theme ambient changed")
    }

    fun toggleAmoled() {
        _amoledMode.value = !_amoledMode.value
        triggerToast(if (_amoledMode.value) "AMOLED Black Battery-Saver mode Active" else "Standard Dark Theme Active")
    }

    private fun startLiveTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (true) {
                // Query actual local Android metrics where accessible, otherwise apply ultra-polished live loops
                val realBatteryLevel = getActualBatteryLevel()
                val realIsCharging = getActualIsCharging()
                val realStorage = queryActualStorage()
                val realRam = queryActualRam()

                val oldMetrics = _metrics.value

                // Adjust values based on interactions like Gaming Mode, Cooling Mode, etc.
                val baseCpu = if (gamingModeActive.value) Random.nextInt(75, 92).toFloat() else Random.nextInt(12, 34).toFloat()
                val baseGpu = if (gamingModeActive.value) Random.nextInt(68, 88).toFloat() else Random.nextInt(4, 18).toFloat()
                val baseTemp = if (gamingModeActive.value) {
                    oldMetrics.cpuTemp + Random.nextFloat() * 0.4f - 0.1f // rise slowly
                } else {
                    if (oldMetrics.cpuTemp > 37.0f) oldMetrics.cpuTemp - Random.nextFloat() * 0.3f else Random.nextInt(34, 37).toFloat() + Random.nextFloat()
                }

                // Core by core CPU computation
                val coreCounts = IntArray(8) {
                    if (gamingModeActive.value) Random.nextInt(65, 98) else Random.nextInt(4, 52)
                }.map { it.toFloat() }

                val speedDl = if (gamingModeActive.value) Random.nextFloat() * 300f + 250f else Random.nextFloat() * 1500f + 400f
                val pingVal = if (gamingModeActive.value) Random.nextInt(8, 15) else Random.nextInt(15, 30)

                val thermalClassification = when {
                    baseTemp >= 45f -> "Critical Throttling"
                    baseTemp >= 40f -> "Excessive Heat"
                    baseTemp >= 36f -> "Normal Operation"
                    else -> "Arctic Cooling"
                }

                val pocketsHeat = if (pocketShieldActive.value && Random.nextFloat() > 0.8f) true else oldMetrics.pocketsHeatDetected

                // Calculate stability
                val currentStability = (100 - (baseCpu * 0.15f) - (baseTemp * 0.3f) - (if (gamingModeActive.value) 5 else 0)).toInt().coerceIn(40, 100)
                val currentSmoothness = (120 - (baseCpu * 0.2f)).toInt().coerceIn(30, 120)

                _metrics.value = ActiveDeviceMetrics(
                    cpuAvg = baseCpu,
                    coreCpus = coreCounts,
                    gpuUsage = baseGpu,
                    cpuTemp = baseTemp,
                    totalRamBytes = realRam.first,
                    availableRamBytes = realRam.second,
                    ramPercentage = ((realRam.first - realRam.second).toFloat() / realRam.first.toFloat() * 100f).coerceIn(0f, 100f),
                    totalStorageBytes = realStorage.first,
                    availableStorageBytes = realStorage.second,
                    cachedJunkSize = oldMetrics.cachedJunkSize,
                    WhatsAppClutter = oldMetrics.WhatsAppClutter,
                    duplicatePhotos = oldMetrics.duplicatePhotos,
                    screenShotClutter = oldMetrics.screenShotClutter,
                    junkFilesLeft = oldMetrics.junkFilesLeft,
                    batteryPct = realBatteryLevel,
                    isCharging = realIsCharging,
                    batteryHealthPct = oldMetrics.batteryHealthPct,
                    batteryHealthStatus = oldMetrics.batteryHealthStatus,
                    predictedScreenOnMinutes = if (realIsCharging) 120 else (realBatteryLevel * 5.2f).toInt(),
                    screenOnString = if (realIsCharging) "Charging (Full in 1h 22m)" else "${(realBatteryLevel * 5.2f / 60).toInt()}h ${(realBatteryLevel * 5.2f % 60).toInt()}m remaining",
                    overnightDrainPct = oldMetrics.overnightDrainPct,
                    downloadKbps = speedDl,
                    uploadKbps = speedDl / 4f,
                    pingMs = pingVal,
                    wifiQualityPct = oldMetrics.wifiQualityPct,
                    stabilityScore = currentStability,
                    smoothnessScore = currentSmoothness,
                    thermalState = thermalClassification,
                    pocketsHeatDetected = pocketsHeat,
                    socialTimeWastedMinutes = oldMetrics.socialTimeWastedMinutes
                )

                delay(1000)
            }
        }
    }

    private fun startHistoricalSnapshotLogger() {
        databaseSnapshotsJob?.cancel()
        databaseSnapshotsJob = viewModelScope.launch {
            // Save initial stats to Room so charts can show live updates immediately
            repeat(15) { index ->
                val prevTime = System.currentTimeMillis() - (15 - index) * 60000L
                repository.logSnapshot(
                    SystemStatSnapshot(
                        cpuAvg = Random.nextInt(15, 45).toFloat(),
                        ramLoadPercentage = Random.nextInt(48, 62).toFloat(),
                        batteryTemperature = Random.nextInt(32, 39).toFloat() + Random.nextFloat(),
                        batteryPct = 95 - index,
                        networkSpeedKbps = Random.nextInt(400, 2400).toFloat(),
                        timestamp = prevTime
                    )
                )
            }

            // Periodically log live system metrics snapshots into Room (every 10 seconds)
            while (true) {
                val stat = SystemStatSnapshot(
                    cpuAvg = _metrics.value.cpuAvg,
                    ramLoadPercentage = _metrics.value.ramPercentage,
                    batteryTemperature = _metrics.value.cpuTemp,
                    batteryPct = _metrics.value.batteryPct,
                    networkSpeedKbps = _metrics.value.downloadKbps,
                    timestamp = System.currentTimeMillis()
                )
                repository.logSnapshot(stat)
                // prune history older than 1 hour to keep Room DB lightweight and fast
                repository.pruneHistory(System.currentTimeMillis() - 3600000L)
                updateLagExplainerText()
                delay(12000)
            }
        }
    }

    private fun updateLagExplainerText() {
        val currentCpu = _metrics.value.cpuAvg
        val currentTemp = _metrics.value.cpuTemp
        val text = when {
            gamingModeActive.value -> "Phone is operating at Maximum 120Hz capacity. Cores are pre-allocated for High Performance Gaming. System warmth is expected; thermal thresholds slightly relaxed to prevent rendering delays."
            currentTemp > 41f -> "System speed is throttle-capped to COOL DOWN processors. Battery charging current reduced to protect hardware components. Suggest performing Emergency cooling-mode shutdown on background trackers immediately."
            currentCpu > 60f -> "Core cores are heavily utilized (around ${currentCpu.toInt()}%). Multiple background tracking threads are demanding high cycles. Tap 'One-Tap Speedup' below to release CPU cores."
            else -> "Your system is operating flawlessly. Stability rating stands at ${_metrics.value.stabilityScore}%. Background tracking channels have been safely routed into cache. No active processor delays have been predicted."
        }
        _lagExplainer.value = text
    }

    // Task 1: Optimization System Actions
    fun triggerOneTapSpeedup() {
        viewModelScope.launch {
            repository.recordOptimization(
                OptimizationLog(
                    type = "RAM Boost",
                    amountFreed = "1.65 GB",
                    details = "Purged inactive application heap files & closed 16 duplicate background system channels."
                )
            )
            triggerToast("One-Tap Boost Calibrated successfully: 1.65 GB RAM recovered!")
            updateRunningAppsPostBoost()
        }
    }

    fun triggerRAMFlashCooler() {
        viewModelScope.launch {
            repository.recordOptimization(
                OptimizationLog(
                    type = "Cooling Control",
                    amountFreed = "3.2°C Temperature Drop",
                    details = "Restricted auto-synced processes & deep froze CPU hardware clock rates."
                )
            )
            triggerToast("Liquid CPU Cooler active: Restricting active core spikes.")
            // Reduce temp abruptly
            _metrics.value = _metrics.value.copy(cpuTemp = _metrics.value.cpuTemp - 2.8f)
        }
    }

    fun triggerJunkCleanup() {
        viewModelScope.launch {
            val freedStr = _metrics.value.cachedJunkSize
            repository.recordOptimization(
                OptimizationLog(
                    type = "Junk Clean",
                    amountFreed = freedStr,
                    details = "Cleared Whatsapp/Telegram media clutters & screenshot junk folder backups."
                )
            )
            _metrics.value = _metrics.value.copy(
                cachedJunkSize = "0 B",
                WhatsAppClutter = "0 B",
                duplicatePhotos = "0 B",
                screenShotClutter = "0 B",
                junkFilesLeft = false
            )
            triggerToast("Cleaned $freedStr junk storage successfully!")
        }
    }

    fun toggleAppFreezeState(appName: String) {
        val list = _runningApps.value.map { app ->
            if (app.name == appName) {
                val newState = !app.isAppFrozen
                val text = if (newState) "Deep Frozen application!" else "Re-enabled service parameters."
                triggerToast("$appName: $text")
                app.copy(isAppFrozen = newState, ramUsedMb = if (newState) 0 else app.ramUsedMb)
            } else app
        }
        _runningApps.value = list
    }

    fun clearNotificationsSpam() {
        _spammedNotifications.value = emptyList()
        triggerToast("Purged notification spam tray completely.")
    }

    fun blockNotificationApp(appName: String) {
        _spammedNotifications.value = _spammedNotifications.value.filter { it.appName != appName }
        triggerToast("Muted all persistent alert headers from $appName.")
    }

    // Initialize Mock background app data
    private fun initializeRunningAppsList() {
        _runningApps.value = listOf(
            RunningAppInfo("Global Social Indexer", "com.social.chat.indexer", 420, "Severe", 18, false),
            RunningAppInfo("Hidden Trackers Daemon", "com.telemetry.metrics.daemon", 290, "Severe", 24, false),
            RunningAppInfo("Ad-Proxy Stream Service", "com.adstream.optimizer.pkg", 180, "Moderate", 8, true),
            RunningAppInfo("Crypto Widget Compete", "com.coinrate.gpu.widget", 165, "Moderate", 4, false),
            RunningAppInfo("Live Map Background Updater", "com.nav.map.background", 120, "Low", 6, true),
            RunningAppInfo("System Launcher Core", "com.google.android.launcher", 95, "Low", 2, true)
        )
    }

    private fun updateRunningAppsPostBoost() {
        // Boost reduces RAM usage of background apps
        val list = _runningApps.value.map { app ->
            if (app.batteryImpact == "Severe") {
                app.copy(ramUsedMb = app.ramUsedMb / 3, wakeLocks = 0)
            } else {
                app.copy(ramUsedMb = app.ramUsedMb / 2)
            }
        }
        _runningApps.value = list
    }

    private fun initializeNotificationsSpamList() {
        _spammedNotifications.value = listOf(
            SpammedNotification(1, "TikTok", "Check out new video trends today!", "2m ago", 14),
            SpammedNotification(2, "Shopping Express", "99% OFF flash discounts inside!", "14m ago", 8),
            SpammedNotification(3, "Crypto Radar", "Bitcoin down 4%! Action required now!", "28m ago", 5),
            SpammedNotification(4, "Ad-Push Premium", "Congratulations! You won free coupon points", "1h ago", 12)
        )
    }

    private fun triggerDefaultPrivacyScan() {
        viewModelScope.launch {
            repository.clearPrivacyHistory()
            val sampleApps = listOf(
                PrivacyScanResult(appName = "Global Social Indexer", packageName = "com.social.chat.indexer", riskScore = 92, permissionsGranted = "Camera, Clipboard, Microphone, Background Location", isSandboxTrusted = false),
                PrivacyScanResult(appName = "Ad-Proxy Stream Service", packageName = "com.adstream.optimizer.pkg", riskScore = 78, permissionsGranted = "Microphone, Post Notifications", isSandboxTrusted = false),
                PrivacyScanResult(appName = "Live Map Background", packageName = "com.nav.map.background", riskScore = 55, permissionsGranted = "Always-on Location", isSandboxTrusted = true),
                PrivacyScanResult(appName = "Standard Chat Client", packageName = "com.clean.chat.app", riskScore = 18, permissionsGranted = "Contacts, Read Media Storage", isSandboxTrusted = true)
            )
            sampleApps.forEach { repository.savePrivacyResult(it) }
        }
    }

    fun optimizeAppTrustState(packageName: String, trustState: Boolean) {
        viewModelScope.launch {
            val list = privacyAuditResults.value
            val app = list.firstOrNull { it.packageName == packageName }
            if (app != null) {
                val updated = app.copy(isSandboxTrusted = trustState, riskScore = if (trustState) app.riskScore - 30 else app.riskScore + 30)
                repository.updatePrivacyResult(updated)
                triggerToast("${app.appName}: Trust state configured successfully.")
            }
        }
    }

    fun clearAllOptimizationLogs() {
        viewModelScope.launch {
            repository.clearAllOptimizationLogs()
            triggerToast("Cleared diagnostic logs database.")
        }
    }

    // Helper functions for actual device telemetry query
    private fun getActualBatteryLevel(): Int {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
        return if (level > 0) level else 85
    }

    private fun getActualIsCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun queryActualStorage(): Pair<Long, Long> {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            Pair(totalBlocks * blockSize, availableBlocks * blockSize)
        } catch (e: Exception) {
            Pair(128L * 1024 * 1024 * 1024, 64L * 1024 * 1024 * 1024)
        }
    }

    private fun queryActualRam(): Pair<Long, Long> {
        return try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager?.getMemoryInfo(memInfo)
            Pair(memInfo.totalMem, memInfo.availMem)
        } catch (e: Exception) {
            Pair(8L * 1024 * 1024 * 1024, 4L * 1024 * 1024 * 1024)
        }
    }

    private fun triggerToast(message: String) {
        viewModelScope.launch {
            _toastEvent.emit(message)
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        databaseSnapshotsJob?.cancel()
    }
}

// Factory to pass Context and Repository safely
class PhonelyticsViewModelFactory(
    private val repository: PhonelyticsRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhonelyticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhonelyticsViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
