package com.example.domain.model

import android.graphics.drawable.Drawable

enum class RiskLevel(val label: String) {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH"),
    CRITICAL("CRITICAL")
}

data class PermissionDetail(
    val name: String,
    val simpleName: String,
    val isDangerous: Boolean,
    val group: String,
    val description: String
)

data class ComponentDetail(
    val name: String,
    val type: String, // Activity, Service, Receiver, Provider
    val isExported: Boolean,
    val permission: String? = null
)

data class CertificateDetail(
    val subject: String,
    val issuer: String,
    val sha256: String,
    val isSelfSigned: Boolean,
    val validFrom: String,
    val validUntil: String
)

data class SecurityFinding(
    val title: String,
    val description: String,
    val severity: RiskLevel,
    val isWarning: Boolean = true
)

data class ApkMetadata(
    val fileName: String,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val targetSdk: Int,
    val fileSize: Long,
    val sha256: String,
    val sourceApp: String,
    val icon: Drawable? = null,
    val permissions: List<PermissionDetail> = emptyList(),
    val activities: List<ComponentDetail> = emptyList(),
    val services: List<ComponentDetail> = emptyList(),
    val receivers: List<ComponentDetail> = emptyList(),
    val providers: List<ComponentDetail> = emptyList(),
    val nativeLibraries: List<String> = emptyList(),
    val dexFilesCount: Int = 1,
    val certificate: CertificateDetail? = null,
    val manifestDetails: Map<String, String> = emptyMap(),
    val localFilePath: String = ""
)

data class RiskEvaluation(
    val score: Int, // 0 - 100
    val level: RiskLevel,
    val confidence: Int, // e.g. 82%
    val probability: Float,
    val findings: List<SecurityFinding>,
    val recommendation: String,
    val vectorDetails: Map<String, Float> = emptyMap()
)

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val targetSdk: Int,
    val isSystemApp: Boolean,
    val isManagedProfile: Boolean,
    val icon: Drawable? = null,
    val permissionsCount: Int,
    val dangerousPermissionsCount: Int,
    val scannedRiskScore: Int? = null,
    val scannedRiskLevel: RiskLevel? = null,
    val installationEnvironment: String? = null,
    val scanTimestamp: Long? = null,
    val certSha256: String? = null,
    val sensitiveFindings: List<String> = emptyList()
)

data class ProtectionStatus(
    val isSecureEnvironmentAvailable: Boolean,
    val isManagedProfileActive: Boolean,
    val isUsageAccessGranted: Boolean,
    val isNotificationGranted: Boolean,
    val isStorageProtectionActive: Boolean,
    val storageProtectionMode: String,
    val devicePolicyActive: Boolean,
    val totalScannedCount: Int = 0,
    val highRiskCount: Int = 0,
    val protectedAppsCount: Int = 0,
    val alertsCount: Int = 0
)
