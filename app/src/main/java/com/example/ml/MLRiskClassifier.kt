package com.example.ml

import android.content.Context
import com.example.domain.model.ApkMetadata
import com.example.domain.model.RiskEvaluation
import com.example.domain.model.RiskLevel
import com.example.domain.model.SecurityFinding
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.exp
import kotlin.math.roundToInt

class MLRiskClassifier(private val context: Context) {

    private var modelBias: Float = -0.925844f
    private val weights = FloatArray(30)
    private val means = FloatArray(30)
    private val stds = FloatArray(30)
    private var isModelLoaded = false

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("model_weights.json")
            val reader = BufferedReader(InputStreamReader(inputStream))
            val jsonString = reader.readText()
            reader.close()

            val json = JSONObject(jsonString)
            modelBias = json.optDouble("bias", -0.925844).toFloat()
            val featuresArray = json.getJSONArray("features")

            for (i in 0 until featuresArray.length()) {
                val featObj = featuresArray.getJSONObject(i)
                val order = featObj.getInt("order")
                if (order in 0..29) {
                    weights[order] = featObj.getDouble("weight").toFloat()
                    means[order] = featObj.getDouble("mean").toFloat()
                    stds[order] = featObj.getDouble("std").toFloat()
                }
            }
            isModelLoaded = true
        } catch (e: Exception) {
            // Fallback to schema defaults if asset reading fails
            for (i in 0..29) {
                val name = FeatureSchema.FEATURE_NAMES[i]
                val meta = FeatureSchema.FEATURE_METAS[name] ?: Pair(0f, 1f)
                means[i] = meta.first
                stds[i] = if (meta.second > 0f) meta.second else 1f
                weights[i] = 0.5f
            }
            isModelLoaded = false
        }
    }

    fun evaluate(metadata: ApkMetadata): RiskEvaluation {
        val rawFeatures = FeatureExtractor.extractFeatures(metadata)
        var z = modelBias

        for (i in 0..29) {
            val std = if (stds[i] > 0.0001f) stds[i] else 1.0f
            val normalized = (rawFeatures[i] - means[i]) / std
            z += weights[i] * normalized
        }

        val probability = sigmoid(z)
        val score = (probability * 100f).roundToInt().coerceIn(0, 100)

        val level = when {
            score >= 80 -> RiskLevel.CRITICAL
            score >= 60 -> RiskLevel.HIGH
            score >= 30 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Model confidence based on distance from decision boundary
        val confidence = (50 + (kotlin.math.abs(probability - 0.5f) * 100f)).roundToInt().coerceIn(50, 99)

        val findings = generateFindings(metadata, rawFeatures)

        val recommendation = when (level) {
            RiskLevel.CRITICAL -> "Run in Secure Environment (Isolated Storage). This APK exhibits critical risk indicators and aggressive permission combinations."
            RiskLevel.HIGH -> "Run in Secure Environment (Isolated Storage) recommended. This application has sensitive permissions and background capabilities."
            RiskLevel.MEDIUM -> "Review permissions carefully before normal installation, or run in Secure Environment."
            RiskLevel.LOW -> "Normal Installation is safe. Minimal security risk indicators detected."
        }

        val vectorMap = mutableMapOf<String, Float>()
        for (i in 0..29) {
            vectorMap[FeatureSchema.FEATURE_NAMES[i]] = rawFeatures[i]
        }

        return RiskEvaluation(
            score = score,
            level = level,
            confidence = confidence,
            probability = probability,
            findings = findings,
            recommendation = recommendation,
            vectorDetails = vectorMap
        )
    }

    private fun sigmoid(z: Float): Float {
        if (z < -30f) return 0f
        if (z > 30f) return 1f
        return (1.0f / (1.0f + exp(-z)))
    }

    private fun generateFindings(metadata: ApkMetadata, features: FloatArray): List<SecurityFinding> {
        val list = mutableListOf<SecurityFinding>()

        // Check each feature strictly if present
        if (features[8] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Requests System Overlay Permission",
                    description = "SYSTEM_ALERT_WINDOW allows drawing over other apps, potentially capturing taps or showing phishing screens.",
                    severity = RiskLevel.HIGH
                )
            )
        }

        if (features[9] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Package Installation Capability",
                    description = "REQUEST_INSTALL_PACKAGES allows this app to prompt for installing other secondary APKs.",
                    severity = RiskLevel.HIGH
                )
            )
        }

        if (features[17] == 1f) {
            list.add(
                SecurityFinding(
                    title = "SMS Access with Network Communication",
                    description = "Combination of SMS permissions and Internet access can be leveraged for OTP interception or premium billing.",
                    severity = RiskLevel.CRITICAL
                )
            )
        } else if (features[2] == 1f) {
            list.add(
                SecurityFinding(
                    title = "SMS Permission Requested",
                    description = "App requests read/send SMS capabilities.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[16] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Simultaneous Camera & Microphone Access",
                    description = "App requests both camera and audio recording capabilities.",
                    severity = RiskLevel.HIGH
                )
            )
        }

        if (features[14] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Requests Storage Access with Network Connectivity",
                    description = "App requests external storage write and internet communication.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[15] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Location Tracking with Network Access",
                    description = "App requests fine/coarse location access together with Internet communication.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[18] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Boot Persistence with Background Services",
                    description = "RECEIVE_BOOT_COMPLETED enables the application to start background services on device startup.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[13] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Telephony State Access",
                    description = "READ_PHONE_STATE allows reading phone status, network operators, and device identifiers.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[4] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Contacts Access Requested",
                    description = "App can read or modify personal address book entries.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[24] >= 3f) {
            list.add(
                SecurityFinding(
                    title = "High Exported Components Count (${features[24].toInt()})",
                    description = "Multiple exported activities/receivers expand the external attack surface.",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[26] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Contains Native Binaries (${features[25].toInt()} .so files)",
                    description = "Native libraries bypass Java/Kotlin bytecode analysis.",
                    severity = RiskLevel.LOW,
                    isWarning = false
                )
            )
        }

        if (features[29] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Self-Signed Certificate Detected",
                    description = "Certificate is self-signed or using debug credentials (unknown developer).",
                    severity = RiskLevel.MEDIUM
                )
            )
        }

        if (features[28] == 1f) {
            list.add(
                SecurityFinding(
                    title = "Legacy Target SDK (${metadata.targetSdk})",
                    description = "App targets an older Android SDK version, possibly bypassing modern scoped storage protections.",
                    severity = RiskLevel.HIGH
                )
            )
        }

        if (list.isEmpty()) {
            list.add(
                SecurityFinding(
                    title = "Standard Baseline Permissions",
                    description = "No sensitive or high-risk permission combinations detected.",
                    severity = RiskLevel.LOW,
                    isWarning = false
                )
            )
        }

        return list
    }
}
