package com.example.ml

import com.example.domain.model.ApkMetadata

object FeatureExtractor {

    fun extractFeatures(metadata: ApkMetadata): FloatArray {
        val permNames = metadata.permissions.map { it.name.uppercase() }.toSet()
        val totalPerms = metadata.permissions.size.toFloat()
        val dangPerms = metadata.permissions.count { it.isDangerous }.toFloat()

        // Specific high-risk permissions
        val hasSms = if (permNames.any { it.contains("SMS") }) 1f else 0f
        val hasCall = if (permNames.any { it.contains("CALL_PHONE") || it.contains("CALL_LOG") }) 1f else 0f
        val hasContacts = if (permNames.any { it.contains("CONTACTS") || it.contains("GET_ACCOUNTS") }) 1f else 0f
        val hasLocation = if (permNames.any { it.contains("LOCATION") }) 1f else 0f
        val hasCamera = if (permNames.any { it.contains("CAMERA") }) 1f else 0f
        val hasAudio = if (permNames.any { it.contains("RECORD_AUDIO") }) 1f else 0f
        val hasAlertWindow = if (permNames.any { it.contains("SYSTEM_ALERT_WINDOW") }) 1f else 0f
        val hasInstall = if (permNames.any { it.contains("REQUEST_INSTALL_PACKAGES") || it.contains("INSTALL_PACKAGES") }) 1f else 0f
        val hasBoot = if (permNames.any { it.contains("RECEIVE_BOOT_COMPLETED") }) 1f else 0f
        val hasNet = if (permNames.any { it.contains("INTERNET") }) 1f else 0f
        val hasStorage = if (permNames.any { it.contains("WRITE_EXTERNAL_STORAGE") || it.contains("MANAGE_EXTERNAL_STORAGE") }) 1f else 0f
        val hasPhoneState = if (permNames.any { it.contains("READ_PHONE_STATE") }) 1f else 0f

        // Combinations
        val combStorageNet = if (hasStorage == 1f && hasNet == 1f) 1f else 0f
        val combLocNet = if (hasLocation == 1f && hasNet == 1f) 1f else 0f
        val combCamMic = if (hasCamera == 1f && hasAudio == 1f) 1f else 0f
        val combSmsNet = if (hasSms == 1f && hasNet == 1f) 1f else 0f
        val combBootSvc = if (hasBoot == 1f && metadata.services.isNotEmpty()) 1f else 0f
        val combInstNet = if (hasInstall == 1f && hasNet == 1f) 1f else 0f

        // Components
        val actCount = metadata.activities.size.toFloat()
        val svcCount = metadata.services.size.toFloat()
        val recCount = metadata.receivers.size.toFloat()
        val provCount = metadata.providers.size.toFloat()

        val expCount = (
            metadata.activities.count { it.isExported } +
            metadata.services.count { it.isExported } +
            metadata.receivers.count { it.isExported } +
            metadata.providers.count { it.isExported }
        ).toFloat()

        // Native libs & DEX
        val natCount = metadata.nativeLibraries.size.toFloat()
        val hasNat = if (natCount > 0) 1f else 0f
        val dexCount = metadata.dexFilesCount.coerceAtLeast(1).toFloat()

        // Metadata indicators
        val legacySdk = if (metadata.targetSdk in 1..29) 1f else 0f
        val selfSigned = if (metadata.certificate?.isSelfSigned == true) 1f else 0f

        return floatArrayOf(
            totalPerms,         // 0: permission_count
            dangPerms,          // 1: dangerous_permission_count
            hasSms,             // 2: has_sms_permissions
            hasCall,            // 3: has_call_phone_permissions
            hasContacts,        // 4: has_contacts_permissions
            hasLocation,        // 5: has_location_permissions
            hasCamera,          // 6: has_camera_permission
            hasAudio,           // 7: has_audio_record_permission
            hasAlertWindow,     // 8: has_system_alert_window
            hasInstall,         // 9: has_request_install_packages
            hasBoot,            // 10: has_receive_boot_completed
            hasNet,             // 11: has_internet_permission
            hasStorage,         // 12: has_external_storage_write
            hasPhoneState,      // 13: has_read_phone_state
            combStorageNet,     // 14: comb_storage_and_network
            combLocNet,         // 15: comb_location_and_network
            combCamMic,         // 16: comb_camera_and_mic
            combSmsNet,         // 17: comb_sms_and_network
            combBootSvc,        // 18: comb_boot_and_service
            combInstNet,        // 19: comb_install_and_network
            actCount,           // 20: activity_count
            svcCount,           // 21: service_count
            recCount,           // 22: receiver_count
            provCount,          // 23: provider_count
            expCount,           // 24: exported_component_count
            natCount,           // 25: native_lib_count
            hasNat,             // 26: has_native_libs
            dexCount,           // 27: dex_count
            legacySdk,          // 28: target_sdk_is_legacy
            selfSigned          // 29: cert_is_self_signed
        )
    }
}
