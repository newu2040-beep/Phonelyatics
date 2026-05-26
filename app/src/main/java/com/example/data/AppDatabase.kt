package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entities (Database Tables)
// ==========================================

@Entity(tableName = "optimization_logs")
data class OptimizationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,               // "RAM Boost", "Junk Clean", "Cooling Control", "Battery Save", "Privacy Shield"
    val amountFreed: String,        // e.g., "820 MB", "1.4 GB", "0 B (Fully Calibrated)"
    val details: String,            // e.g., "Killed 14 background services & cleared system cache"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "system_stat_history")
data class SystemStatSnapshot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cpuAvg: Float,              // avg cpu load %
    val ramLoadPercentage: Float,   // ram usage %
    val batteryTemperature: Float,  // temperature in Celsius
    val batteryPct: Int,            // battery level %
    val networkSpeedKbps: Float,    // local calculated net speed
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "privacy_checks")
data class PrivacyScanResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val packageName: String,
    val riskScore: Int,             // 0 (Safe) to 100 (Critical)
    val permissionsGranted: String, // comma-separated e.g. "Microphone, Camera, Location"
    val isSandboxTrusted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

// ==========================================
// 2. DAOs (Data Access Objects)
// ==========================================

@Dao
interface DiagnosticDao {
    // Optimization Logs queries
    @Query("SELECT * FROM optimization_logs ORDER BY timestamp DESC LIMIT 100")
    fun getOptimizationLogs(): Flow<List<OptimizationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOptimizationLog(log: OptimizationLog)

    @Query("DELETE FROM optimization_logs")
    suspend fun clearOptimizationLogs()

    // System metrics trackers
    @Query("SELECT * FROM system_stat_history ORDER BY timestamp DESC LIMIT 60")
    fun getRecentStatHistory(): Flow<List<SystemStatSnapshot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatSnapshot(snapshot: SystemStatSnapshot)

    @Query("DELETE FROM system_stat_history WHERE timestamp < :cutoffTime")
    suspend fun pruneOldStats(cutoffTime: Long)

    // Privacy checkers
    @Query("SELECT * FROM privacy_checks ORDER BY riskScore DESC")
    fun getScannedApps(): Flow<List<PrivacyScanResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivacyResult(result: PrivacyScanResult)

    @Update
    suspend fun updatePrivacyResult(result: PrivacyScanResult)

    @Query("DELETE FROM privacy_checks")
    suspend fun clearPrivacyScanHistory()
}

// ==========================================
// 3. Database Abstract Class
// ==========================================

@Database(
    entities = [OptimizationLog::class, SystemStatSnapshot::class, PrivacyScanResult::class],
    version = 1,
    exportSchema = false
)
abstract class PhonelyticsDatabase : RoomDatabase() {
    abstract fun diagnosticDao(): DiagnosticDao

    companion object {
        @Volatile
        private var INSTANCE: PhonelyticsDatabase? = null

        fun getDatabase(context: Context): PhonelyticsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PhonelyticsDatabase::class.java,
                    "phonelytics_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// ==========================================
// 4. Repository (Data Access Abstracter)
// ==========================================

class PhonelyticsRepository(private val dao: DiagnosticDao) {
    val optimizationLogs: Flow<List<OptimizationLog>> = dao.getOptimizationLogs()
    val systemStatHistory: Flow<List<SystemStatSnapshot>> = dao.getRecentStatHistory()
    val privacyScans: Flow<List<PrivacyScanResult>> = dao.getScannedApps()

    suspend fun recordOptimization(log: OptimizationLog) {
        dao.insertOptimizationLog(log)
    }

    suspend fun clearAllOptimizationLogs() {
        dao.clearOptimizationLogs()
    }

    suspend fun logSnapshot(snapshot: SystemStatSnapshot) {
        dao.insertStatSnapshot(snapshot)
    }

    suspend fun pruneHistory(cutoff: Long) {
        dao.pruneOldStats(cutoff)
    }

    suspend fun savePrivacyResult(scan: PrivacyScanResult) {
        dao.insertPrivacyResult(scan)
    }

    suspend fun updatePrivacyResult(scan: PrivacyScanResult) {
        dao.updatePrivacyResult(scan)
    }

    suspend fun clearPrivacyHistory() {
        dao.clearPrivacyScanHistory()
    }
}
