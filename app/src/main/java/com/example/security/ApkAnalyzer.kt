package com.example.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.drawable.Drawable
import android.os.Build
import com.example.domain.model.ApkMetadata
import com.example.domain.model.CertificateDetail
import com.example.domain.model.ComponentDetail
import com.example.domain.model.PermissionDetail
import java.io.ByteArrayInputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.zip.ZipFile

object ApkAnalyzer {

    @Suppress("DEPRECATION")
    fun analyzeApk(
        context: Context,
        apkFile: File,
        fileName: String,
        fileSize: Long,
        sha256: String,
        sourceApp: String
    ): Result<ApkMetadata> {
        return try {
            val pm = context.packageManager
            val apkPath = apkFile.absolutePath

            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_SIGNATURES
            }

            val packageInfo: PackageInfo? = pm.getPackageArchiveInfo(apkPath, flags)
                ?: return Result.failure(IllegalArgumentException("Android PackageManager was unable to parse the APK archive. The file may be corrupt or incompatible."))

            val appInfo: ApplicationInfo = packageInfo.applicationInfo ?: ApplicationInfo()
            appInfo.sourceDir = apkPath
            appInfo.publicSourceDir = apkPath

            // 1. App name & icon
            val appLabel = try {
                val label = pm.getApplicationLabel(appInfo).toString()
                if (label.isNotBlank() && !label.startsWith("com.")) label else null
            } catch (e: Exception) {
                null
            } ?: packageInfo.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }

            val appIcon: Drawable? = try {
                pm.getApplicationIcon(appInfo)
            } catch (e: Exception) {
                null
            }

            // 2. Package & SDK details
            val packageName = packageInfo.packageName ?: "unknown.package"
            val versionName = packageInfo.versionName ?: "1.0"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }

            val targetSdk = appInfo.targetSdkVersion
            val minSdk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                appInfo.minSdkVersion
            } else {
                21
            }

            // 3. Permissions analysis
            val permissionsList = mutableListOf<PermissionDetail>()
            val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
            val dangerousPermissionSet = setOf(
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
                "android.permission.CAMERA",
                "android.permission.RECORD_AUDIO",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.GET_ACCOUNTS",
                "android.permission.READ_PHONE_STATE",
                "android.permission.READ_PHONE_NUMBERS",
                "android.permission.CALL_PHONE",
                "android.permission.ANSWER_PHONE_CALLS",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_SMS",
                "android.permission.RECEIVE_WAP_PUSH",
                "android.permission.RECEIVE_MMS",
                "android.permission.BODY_SENSORS",
                "android.permission.POST_NOTIFICATIONS",
                "android.permission.NEARBY_WIFI_DEVICES",
                "android.permission.BLUETOOTH_SCAN",
                "android.permission.BLUETOOTH_CONNECT",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.REQUEST_INSTALL_PACKAGES"
            )

            for (perm in requestedPermissions) {
                val simpleName = perm.substringAfterLast('.')
                val isDang = dangerousPermissionSet.contains(perm)
                val group = resolvePermissionGroup(simpleName)
                val desc = resolvePermissionDescription(simpleName)
                permissionsList.add(
                    PermissionDetail(
                        name = perm,
                        simpleName = simpleName,
                        isDangerous = isDang,
                        group = group,
                        description = desc
                    )
                )
            }

            // 4. Components extraction
            val activities = packageInfo.activities?.map {
                ComponentDetail(
                    name = it.name.substringAfterLast('.'),
                    type = "Activity",
                    isExported = it.exported,
                    permission = it.permission
                )
            } ?: emptyList()

            val services = packageInfo.services?.map {
                ComponentDetail(
                    name = it.name.substringAfterLast('.'),
                    type = "Service",
                    isExported = it.exported,
                    permission = it.permission
                )
            } ?: emptyList()

            val receivers = packageInfo.receivers?.map {
                ComponentDetail(
                    name = it.name.substringAfterLast('.'),
                    type = "Receiver",
                    isExported = it.exported,
                    permission = it.permission
                )
            } ?: emptyList()

            val providers = packageInfo.providers?.map {
                ComponentDetail(
                    name = it.name.substringAfterLast('.'),
                    type = "Provider",
                    isExported = it.exported,
                    permission = it.readPermission ?: it.writePermission
                )
            } ?: emptyList()

            // 5. ZIP inspect: Native libs (.so) and DEX files
            val nativeLibs = mutableListOf<String>()
            var dexCount = 0

            try {
                ZipFile(apkFile).use { zip ->
                    val entries = zip.entries()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val name = entry.name
                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            nativeLibs.add(name.substringAfter("lib/"))
                        }
                        if (name.startsWith("classes") && name.endsWith(".dex")) {
                            dexCount++
                        }
                    }
                }
            } catch (e: Exception) {
                dexCount = 1
            }

            // 6. Certificate extraction
            val certDetail = extractCertificate(packageInfo)

            val manifestDetails = mapOf(
                "Package" to packageName,
                "Version Name" to versionName,
                "Version Code" to versionCode.toString(),
                "Min SDK" to minSdk.toString(),
                "Target SDK" to targetSdk.toString(),
                "Install Location" to "Internal / Auto",
                "Activities Count" to activities.size.toString(),
                "Services Count" to services.size.toString(),
                "Receivers Count" to receivers.size.toString(),
                "Providers Count" to providers.size.toString()
            )

            Result.success(
                ApkMetadata(
                    fileName = fileName,
                    appName = appLabel,
                    packageName = packageName,
                    versionName = versionName,
                    versionCode = versionCode,
                    minSdk = minSdk,
                    targetSdk = targetSdk,
                    fileSize = fileSize,
                    sha256 = sha256,
                    sourceApp = sourceApp,
                    icon = appIcon,
                    permissions = permissionsList,
                    activities = activities,
                    services = services,
                    receivers = receivers,
                    providers = providers,
                    nativeLibraries = nativeLibs,
                    dexFilesCount = if (dexCount > 0) dexCount else 1,
                    certificate = certDetail,
                    manifestDetails = manifestDetails,
                    localFilePath = apkPath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Suppress("DEPRECATION")
    private fun extractCertificate(packageInfo: PackageInfo): CertificateDetail? {
        try {
            val signatures: Array<Signature>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = packageInfo.signingInfo
                if (signingInfo != null) {
                    if (signingInfo.hasMultipleSigners()) {
                        signingInfo.apkContentsSigners
                    } else {
                        signingInfo.signingCertificateHistory
                    }
                } else null
            } else {
                packageInfo.signatures
            }

            if (signatures.isNullOrEmpty()) return null

            val certBytes = signatures[0].toByteArray()
            val cf = CertificateFactory.getInstance("X.509")
            val cert = cf.generateCertificate(ByteArrayInputStream(certBytes)) as? X509Certificate
                ?: return null

            val md = MessageDigest.getInstance("SHA-256")
            val certSha256 = md.digest(certBytes).joinToString(":") { "%02X".format(it) }

            val subject = cert.subjectDN?.name ?: "Unknown"
            val issuer = cert.issuerDN?.name ?: "Unknown"
            val isSelfSigned = subject == issuer || subject.contains("Android Debug", ignoreCase = true)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val validFrom = dateFormat.format(cert.notBefore)
            val validUntil = dateFormat.format(cert.notAfter)

            return CertificateDetail(
                subject = subject,
                issuer = issuer,
                sha256 = certSha256,
                isSelfSigned = isSelfSigned,
                validFrom = validFrom,
                validUntil = validUntil
            )
        } catch (e: Exception) {
            return null
        }
    }

    private fun resolvePermissionGroup(simpleName: String): String {
        return when {
            simpleName.contains("STORAGE") -> "Storage"
            simpleName.contains("CAMERA") -> "Camera"
            simpleName.contains("AUDIO") || simpleName.contains("RECORD") -> "Microphone"
            simpleName.contains("LOCATION") -> "Location"
            simpleName.contains("CONTACTS") || simpleName.contains("ACCOUNTS") -> "Contacts"
            simpleName.contains("PHONE") || simpleName.contains("CALL") -> "Phone"
            simpleName.contains("SMS") -> "SMS"
            simpleName.contains("BLUETOOTH") -> "Bluetooth"
            simpleName.contains("NETWORK") || simpleName.contains("INTERNET") || simpleName.contains("WIFI") -> "Network"
            simpleName.contains("BOOT") -> "Startup"
            simpleName.contains("ALERT") -> "Overlay"
            simpleName.contains("INSTALL") -> "Installation"
            else -> "General"
        }
    }

    private fun resolvePermissionDescription(simpleName: String): String {
        return when (simpleName) {
            "READ_EXTERNAL_STORAGE" -> "Allows the application to read files from external storage."
            "WRITE_EXTERNAL_STORAGE" -> "Allows the application to write and modify files on external storage."
            "CAMERA" -> "Allows taking photos and recording videos at any time."
            "RECORD_AUDIO" -> "Allows audio recording using the device microphone."
            "ACCESS_FINE_LOCATION" -> "Access precise GPS location data."
            "ACCESS_COARSE_LOCATION" -> "Access approximate network-derived location."
            "READ_CONTACTS" -> "Read data from user's address book and contacts."
            "WRITE_CONTACTS" -> "Modify and create entries in contacts database."
            "READ_PHONE_STATE" -> "Access device phone state, cellular network info, and IDs."
            "CALL_PHONE" -> "Initiate phone calls without user dialer intervention."
            "READ_SMS" -> "Read stored text messages and SMS inboxes."
            "SEND_SMS" -> "Send SMS text messages which may incur carrier costs."
            "SYSTEM_ALERT_WINDOW" -> "Draw system overlays over any active foreground app."
            "REQUEST_INSTALL_PACKAGES" -> "Request package installation for other APK files."
            "RECEIVE_BOOT_COMPLETED" -> "Automatically start background services after device reboot."
            "INTERNET" -> "Open network sockets and transmit data over the Internet."
            else -> "Standard Android application capability."
        }
    }
}
