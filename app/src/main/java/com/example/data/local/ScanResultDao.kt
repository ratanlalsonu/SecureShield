package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanResultDao {
    @Query("SELECT * FROM scan_results ORDER BY scanTimestamp DESC")
    fun getAllScans(): Flow<List<ScanResultEntity>>

    @Query("SELECT * FROM scan_results WHERE apkSha256 = :sha256 LIMIT 1")
    suspend fun getScanBySha256(sha256: String): ScanResultEntity?

    @Query("SELECT * FROM scan_results WHERE packageName = :packageName ORDER BY scanTimestamp DESC LIMIT 1")
    suspend fun getScanByPackageName(packageName: String): ScanResultEntity?

    @Query("SELECT * FROM scan_results WHERE id = :id LIMIT 1")
    suspend fun getScanById(id: Long): ScanResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanResultEntity): Long

    @Update
    suspend fun updateScan(scan: ScanResultEntity)

    @Query("SELECT COUNT(*) FROM scan_results")
    fun getScanCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE riskScore >= 60")
    fun getHighRiskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_results WHERE isInstalled = 1 AND installationEnvironment = 'MANAGED_PROFILE'")
    fun getProtectedAppsCount(): Flow<Int>

    @Query("DELETE FROM scan_results")
    suspend fun clearAllScans()
}
