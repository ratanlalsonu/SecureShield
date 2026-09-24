package com.example.monitoring

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.local.AppDatabase
import com.example.data.local.SecurityAlertEntity
import com.example.data.local.SecurityLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PackageMonitorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val data = intent.data ?: return
        val packageName = data.schemeSpecificPart ?: return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val timestamp = System.currentTimeMillis()

                when (action) {
                    Intent.ACTION_PACKAGE_ADDED -> {
                        var appLabel = packageName
                        var hasDangerousPerms = false

                        try {
                            val pm = context.packageManager
                            val appInfo = pm.getApplicationInfo(packageName, 0)
                            appLabel = pm.getApplicationLabel(appInfo).toString()

                            val pkgInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                            val perms = pkgInfo.requestedPermissions ?: emptyArray()
                            hasDangerousPerms = perms.any {
                                it.contains("CAMERA") || it.contains("RECORD_AUDIO") ||
                                        it.contains("SMS") || it.contains("LOCATION")
                            }
                        } catch (e: Exception) {
                            // package details may not be available immediately
                        }

                        db.securityLogDao().insertLog(
                            SecurityLogEntity(
                                timestamp = timestamp,
                                eventType = "PACKAGE_ADDED",
                                details = "Application installed: $appLabel ($packageName)"
                            )
                        )

                        // Check if previously scanned in SecureShield
                        val scanRecord = db.scanResultDao().getScanByPackageName(packageName)
                        if (scanRecord != null) {
                            db.scanResultDao().updateScan(scanRecord.copy(isInstalled = true))
                            if (scanRecord.riskScore >= 60) {
                                db.securityAlertDao().insertAlert(
                                    SecurityAlertEntity(
                                        packageName = packageName,
                                        appName = appLabel,
                                        alertType = "HIGH_RISK_APP_INSTALLED",
                                        severity = "HIGH",
                                        timestamp = timestamp,
                                        description = "App $appLabel has been installed with high predicted risk score (${scanRecord.riskScore}/100)."
                                    )
                                )
                            }
                        } else if (hasDangerousPerms) {
                            db.securityAlertDao().insertAlert(
                                SecurityAlertEntity(
                                    packageName = packageName,
                                    appName = appLabel,
                                    alertType = "UNVERIFIED_PACKAGE_WITH_SENSITIVE_PERMISSIONS",
                                    severity = "MEDIUM",
                                    timestamp = timestamp,
                                    description = "Newly installed package was not pre-scanned by SecureShield and requests sensitive hardware permissions."
                                )
                            )
                        }
                    }

                    Intent.ACTION_PACKAGE_REMOVED -> {
                        db.securityLogDao().insertLog(
                            SecurityLogEntity(
                                timestamp = timestamp,
                                eventType = "PACKAGE_REMOVED",
                                details = "Application uninstalled: $packageName"
                            )
                        )
                        val scanRecord = db.scanResultDao().getScanByPackageName(packageName)
                        if (scanRecord != null) {
                            db.scanResultDao().updateScan(scanRecord.copy(isInstalled = false))
                        }
                    }

                    Intent.ACTION_PACKAGE_REPLACED -> {
                        db.securityLogDao().insertLog(
                            SecurityLogEntity(
                                timestamp = timestamp,
                                eventType = "PACKAGE_REPLACED",
                                details = "Application updated: $packageName"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Keep receiver failure resilient
            } finally {
                pendingResult.finish()
            }
        }
    }
}
