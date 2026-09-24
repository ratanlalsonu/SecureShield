package com.example.installation

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.UserManager

object ManagedProfileManager {

    data class EnvironmentReport(
        val isHardwareSupported: Boolean,
        val isManagedProfileActive: Boolean,
        val isDeviceOwnerActive: Boolean,
        val statusLabel: String,
        val explanation: String
    )

    fun inspectEnvironment(context: Context): EnvironmentReport {
        val pm = context.packageManager
        val hasFeature = pm.hasSystemFeature(PackageManager.FEATURE_MANAGED_USERS)

        val userManager = context.getSystemService(Context.USER_SERVICE) as? UserManager
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager

        val isManagedProfile = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            userManager?.isManagedProfile ?: false
        } else {
            false
        }

        val isDeviceOwner = dpm?.isDeviceOwnerApp(context.packageName) ?: false

        val (statusLabel, explanation) = when {
            isManagedProfile -> {
                Pair(
                    "ACTIVE",
                    "This application is currently executing inside an Android Managed Profile (Work Profile). App storage and IPC are strictly isolated from personal profile data."
                )
            }
            hasFeature -> {
                Pair(
                    "AVAILABLE",
                    "Device hardware & Android OS support Managed Profiles. Provisioning a Work Profile creates an isolated Linux UID space (User 10) with dedicated SELinux domains and separate /data/user/10/ encrypted storage."
                )
            }
            else -> {
                Pair(
                    "UNAVAILABLE",
                    "Managed Profiles feature is not supported by this Android build. Default Android app sandboxing via Linux UID isolation and Scoped Storage remains fully active."
                )
            }
        }

        return EnvironmentReport(
            isHardwareSupported = hasFeature,
            isManagedProfileActive = isManagedProfile,
            isDeviceOwnerActive = isDeviceOwner,
            statusLabel = statusLabel,
            explanation = explanation
        )
    }
}
