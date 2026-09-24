package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_results")
data class ScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val apkSha256: String,
    val packageName: String,
    val appName: String,
    val versionName: String,
    val versionCode: Long,
    val fileSize: Long,
    val scanTimestamp: Long,
    val riskScore: Int,
    val riskLevel: String,
    val modelConfidence: Int,
    val detectedPermissionsCount: Int,
    val dangerousPermissionsCount: Int,
    val sensitiveFindingsJson: String,
    val sourceApp: String,
    val certSha256: String,
    val recommendation: String,
    val installationEnvironment: String = "NONE",
    val isInstalled: Boolean = false
)
