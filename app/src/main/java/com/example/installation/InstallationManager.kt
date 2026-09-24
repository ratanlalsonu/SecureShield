package com.example.installation

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import java.io.File

object InstallationManager {

    sealed class InstallResult {
        object Started : InstallResult()
        data class Failure(val message: String) : InstallResult()
    }

    fun launchInstaller(
        context: Context,
        apkFile: File,
        targetEnvironment: String = "NORMAL"
    ): InstallResult {
        return try {
            if (!apkFile.exists() || apkFile.length() <= 0) {
                return InstallResult.Failure("APK file does not exist or is empty.")
            }

            // Check REQUEST_INSTALL_PACKAGES permission on Android 8.0+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canInstall = context.packageManager.canRequestPackageInstalls()
                if (!canInstall) {
                    val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(settingsIntent)
                    return InstallResult.Failure("Permission required: Please enable 'Allow from this source' for SecureShield to install APKs, then retry.")
                }
            }

            val authority = "${context.packageName}.fileprovider"
            val apkUri = FileProvider.getUriForFile(context, authority, apkFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }

            context.startActivity(intent)
            InstallResult.Started
        } catch (e: Exception) {
            InstallResult.Failure(e.localizedMessage ?: "Unknown installation error occurred.")
        }
    }
}
