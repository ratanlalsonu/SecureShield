package com.example.ml

data class FeatureDefinition(
    val order: Int,
    val name: String,
    val type: String,
    val mean: Float,
    val std: Float,
    val weight: Float
)

object FeatureSchema {
    // 30 exact features matching the trained model pipeline
    val FEATURE_NAMES = listOf(
        "permission_count",
        "dangerous_permission_count",
        "has_sms_permissions",
        "has_call_phone_permissions",
        "has_contacts_permissions",
        "has_location_permissions",
        "has_camera_permission",
        "has_audio_record_permission",
        "has_system_alert_window",
        "has_request_install_packages",
        "has_receive_boot_completed",
        "has_internet_permission",
        "has_external_storage_write",
        "has_read_phone_state",
        "comb_storage_and_network",
        "comb_location_and_network",
        "comb_camera_and_mic",
        "comb_sms_and_network",
        "comb_boot_and_service",
        "comb_install_and_network",
        "activity_count",
        "service_count",
        "receiver_count",
        "provider_count",
        "exported_component_count",
        "native_lib_count",
        "has_native_libs",
        "dex_count",
        "target_sdk_is_legacy",
        "cert_is_self_signed"
    )

    // Baseline statistical normalization parameters from dataset
    val FEATURE_METAS = mapOf(
        "permission_count" to Pair(12.4f, 10.2f),
        "dangerous_permission_count" to Pair(3.8f, 4.1f),
        "has_sms_permissions" to Pair(0.12f, 0.32f),
        "has_call_phone_permissions" to Pair(0.15f, 0.35f),
        "has_contacts_permissions" to Pair(0.18f, 0.38f),
        "has_location_permissions" to Pair(0.35f, 0.47f),
        "has_camera_permission" to Pair(0.28f, 0.44f),
        "has_audio_record_permission" to Pair(0.16f, 0.36f),
        "has_system_alert_window" to Pair(0.14f, 0.34f),
        "has_request_install_packages" to Pair(0.09f, 0.28f),
        "has_receive_boot_completed" to Pair(0.42f, 0.49f),
        "has_internet_permission" to Pair(0.88f, 0.32f),
        "has_external_storage_write" to Pair(0.45f, 0.49f),
        "has_read_phone_state" to Pair(0.38f, 0.48f),
        "comb_storage_and_network" to Pair(0.42f, 0.49f),
        "comb_location_and_network" to Pair(0.33f, 0.47f),
        "comb_camera_and_mic" to Pair(0.14f, 0.34f),
        "comb_sms_and_network" to Pair(0.11f, 0.31f),
        "comb_boot_and_service" to Pair(0.36f, 0.48f),
        "comb_install_and_network" to Pair(0.08f, 0.27f),
        "activity_count" to Pair(14.2f, 18.5f),
        "service_count" to Pair(4.1f, 6.8f),
        "receiver_count" to Pair(3.6f, 5.2f),
        "provider_count" to Pair(1.2f, 2.1f),
        "exported_component_count" to Pair(2.8f, 4.5f),
        "native_lib_count" to Pair(2.4f, 4.8f),
        "has_native_libs" to Pair(0.32f, 0.46f),
        "dex_count" to Pair(1.6f, 1.2f),
        "target_sdk_is_legacy" to Pair(0.22f, 0.41f),
        "cert_is_self_signed" to Pair(0.29f, 0.45f)
    )
}
