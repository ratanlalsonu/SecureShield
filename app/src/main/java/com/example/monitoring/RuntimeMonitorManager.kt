package com.example.monitoring

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.ProtectionStatus
import com.example.installation.ManagedProfileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RuntimeMonitorManager(private val context: Context) {

    fun isUsageAccessGranted(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun getProtectionStatus(scannedCount: Int, highRiskCount: Int, protectedCount: Int, alertsCount: Int): ProtectionStatus {
        val env = ManagedProfileManager.inspectEnvironment(context)
        val usageGranted = isUsageAccessGranted()
        val notifGranted = isNotificationPermissionGranted()

        // Android 11+ enforces Scoped Storage natively for apps
        val isStorageProtected = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val storageMode = if (isStorageProtected) {
            "Scoped Storage Enforced (Linux UID / SELinux Active)"
        } else {
            "Legacy Storage Isolation (Linux UID Active)"
        }

        return ProtectionStatus(
            isSecureEnvironmentAvailable = env.isHardwareSupported,
            isManagedProfileActive = env.isManagedProfileActive,
            isUsageAccessGranted = usageGranted,
            isNotificationGranted = notifGranted,
            isStorageProtectionActive = isStorageProtected,
            storageProtectionMode = storageMode,
            devicePolicyActive = env.isDeviceOwnerActive,
            totalScannedCount = scannedCount,
            highRiskCount = highRiskCount,
            protectedAppsCount = protectedCount,
            alertsCount = alertsCount
        )
    }

    fun createUsageAccessIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
    }

    suspend fun getInstalledApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val installedPackages = try {
            pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
        } catch (e: Exception) {
            emptyList()
        }

        val result = mutableListOf<InstalledAppInfo>()
        for (pkg in installedPackages) {
            val appInfo = pkg.applicationInfo ?: continue
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appLabel = try {
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                pkg.packageName
            }
            val appIcon = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            }

            val perms = pkg.requestedPermissions ?: emptyArray()
            val dangerousPermsCount = perms.count {
                it.contains("CAMERA") || it.contains("RECORD_AUDIO") ||
                        it.contains("LOCATION") || it.contains("SMS") ||
                        it.contains("CONTACTS") || it.contains("STORAGE")
            }

            result.add(
                InstalledAppInfo(
                    appName = appLabel,
                    packageName = pkg.packageName,
                    versionName = pkg.versionName ?: "1.0",
                    targetSdk = appInfo.targetSdkVersion,
                    isSystemApp = isSystem,
                    isManagedProfile = false,
                    icon = appIcon,
                    permissionsCount = perms.size,
                    dangerousPermissionsCount = dangerousPermsCount
                )
            )
        }

        result.sortedWith(compareBy({ it.isSystemApp }, { it.appName.lowercase() }))
    }
}
