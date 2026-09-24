package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ScanResultEntity
import com.example.data.local.SecurityAlertEntity
import com.example.domain.model.ApkMetadata
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.ProtectionStatus
import com.example.domain.model.RiskEvaluation
import com.example.domain.model.RiskLevel
import com.example.installation.InstallationManager
import com.example.ml.MLRiskClassifier
import com.example.monitoring.RuntimeMonitorManager
import com.example.security.ApkAnalyzer
import com.example.security.ApkUriHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

sealed class AnalysisUiState {
    object Idle : AnalysisUiState()
    data class Analyzing(val progress: Float, val stage: String) : AnalysisUiState()
    data class Analyzed(
        val metadata: ApkMetadata,
        val evaluation: RiskEvaluation,
        val previousScanId: Long? = null,
        val isDuplicate: Boolean = false
    ) : AnalysisUiState()
    data class Installing(val progress: Float, val message: String) : AnalysisUiState()
    data class InstallCompleted(
        val appName: String,
        val packageName: String,
        val environment: String,
        val timestamp: Long
    ) : AnalysisUiState()
    data class Error(val message: String) : AnalysisUiState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val classifier = MLRiskClassifier(application)
    private val runtimeMonitor = RuntimeMonitorManager(application)

    private val _uiState = MutableStateFlow<AnalysisUiState>(AnalysisUiState.Idle)
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()

    val allScans = db.scanResultDao().getAllScans().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allAlerts = db.securityAlertDao().getAllAlerts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val scanCount = db.scanResultDao().getScanCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val highRiskCount = db.scanResultDao().getHighRiskCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val protectedCount = db.scanResultDao().getProtectedAppsCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val alertCount = db.securityAlertDao().getAlertCount().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _protectionStatus = MutableStateFlow(
        runtimeMonitor.getProtectionStatus(0, 0, 0, 0)
    )
    val protectionStatus: StateFlow<ProtectionStatus> = _protectionStatus.asStateFlow()

    init {
        refreshProtectionStatus()
        loadInstalledApps()
    }

    fun refreshProtectionStatus() {
        viewModelScope.launch {
            val count = scanCount.value
            val high = highRiskCount.value
            val prot = protectedCount.value
            val alerts = alertCount.value
            _protectionStatus.value = runtimeMonitor.getProtectionStatus(count, high, prot, alerts)
        }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = runtimeMonitor.getInstalledApps()
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun handleIncomingUri(uri: Uri, sourceHint: String? = null) {
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Analyzing(0.1f, "Reading APK file...")
            val resolveResult = withContext(Dispatchers.IO) {
                ApkUriHandler.resolveAndCopyApk(getApplication(), uri, sourceHint)
            }

            resolveResult.onSuccess { resolved ->
                startProgressiveAnalysis(
                    apkFile = resolved.file,
                    fileName = resolved.originalFileName,
                    fileSize = resolved.fileSize,
                    sha256 = resolved.sha256,
                    sourceApp = resolved.sourceApp
                )
            }.onFailure { error ->
                _uiState.value = AnalysisUiState.Error(error.localizedMessage ?: "Failed to read incoming APK URI.")
            }
        }
    }

    private suspend fun startProgressiveAnalysis(
        apkFile: File,
        fileName: String,
        fileSize: Long,
        sha256: String,
        sourceApp: String
    ) {
        // Step 1: Check duplicate in Room
        val existingScan = withContext(Dispatchers.IO) {
            db.scanResultDao().getScanBySha256(sha256)
        }

        _uiState.value = AnalysisUiState.Analyzing(0.3f, "Validating APK structure & signature...")
        delay(300)

        _uiState.value = AnalysisUiState.Analyzing(0.5f, "Extracting manifest, permissions & components...")
        val metadataResult = withContext(Dispatchers.IO) {
            ApkAnalyzer.analyzeApk(
                context = getApplication(),
                apkFile = apkFile,
                fileName = fileName,
                fileSize = fileSize,
                sha256 = sha256,
                sourceApp = sourceApp
            )
        }

        if (metadataResult.isFailure) {
            _uiState.value = AnalysisUiState.Error(
                metadataResult.exceptionOrNull()?.localizedMessage ?: "Failed to parse APK manifest."
            )
            return
        }

        val metadata = metadataResult.getOrThrow()

        _uiState.value = AnalysisUiState.Analyzing(0.75f, "Extracting ML features (30 parameters)...")
        delay(250)

        _uiState.value = AnalysisUiState.Analyzing(0.9f, "Running on-device ML risk inference...")
        val evaluation = withContext(Dispatchers.Default) {
            classifier.evaluate(metadata)
        }

        delay(200)

        // Save scan result to Room database
        withContext(Dispatchers.IO) {
            val findingsArray = JSONArray()
            evaluation.findings.forEach {
                findingsArray.put(it.title)
            }

            val entity = ScanResultEntity(
                apkSha256 = metadata.sha256,
                packageName = metadata.packageName,
                appName = metadata.appName,
                versionName = metadata.versionName,
                versionCode = metadata.versionCode,
                fileSize = metadata.fileSize,
                scanTimestamp = System.currentTimeMillis(),
                riskScore = evaluation.score,
                riskLevel = evaluation.level.label,
                modelConfidence = evaluation.confidence,
                detectedPermissionsCount = metadata.permissions.size,
                dangerousPermissionsCount = metadata.permissions.count { it.isDangerous },
                sensitiveFindingsJson = findingsArray.toString(),
                sourceApp = metadata.sourceApp,
                certSha256 = metadata.certificate?.sha256 ?: "Unknown",
                recommendation = evaluation.recommendation,
                installationEnvironment = "NONE",
                isInstalled = false
            )
            db.scanResultDao().insertScan(entity)
        }

        refreshProtectionStatus()

        _uiState.value = AnalysisUiState.Analyzed(
            metadata = metadata,
            evaluation = evaluation,
            previousScanId = existingScan?.id,
            isDuplicate = existingScan != null
        )
    }

    fun initiateInstallation(environment: String) {
        val current = _uiState.value
        if (current !is AnalysisUiState.Analyzed) return

        val apkFile = File(current.metadata.localFilePath)
        viewModelScope.launch {
            _uiState.value = AnalysisUiState.Installing(0.3f, "Preparing $environment installation...")
            delay(400)

            _uiState.value = AnalysisUiState.Installing(0.7f, "Launching Android PackageInstaller...")
            val result = withContext(Dispatchers.IO) {
                InstallationManager.launchInstaller(
                    context = getApplication(),
                    apkFile = apkFile,
                    targetEnvironment = environment
                )
            }

            when (result) {
                is InstallationManager.InstallResult.Started -> {
                    // Mark as installed in chosen environment
                    withContext(Dispatchers.IO) {
                        val scan = db.scanResultDao().getScanBySha256(current.metadata.sha256)
                        if (scan != null) {
                            db.scanResultDao().updateScan(
                                scan.copy(
                                    installationEnvironment = environment,
                                    isInstalled = true
                                )
                            )
                        }
                    }
                    refreshProtectionStatus()
                    _uiState.value = AnalysisUiState.InstallCompleted(
                        appName = current.metadata.appName,
                        packageName = current.metadata.packageName,
                        environment = if (environment == "MANAGED_PROFILE") "Managed Profile / Secure Environment" else "Normal Android Environment",
                        timestamp = System.currentTimeMillis()
                    )
                }
                is InstallationManager.InstallResult.Failure -> {
                    _uiState.value = AnalysisUiState.Error(result.message)
                }
            }
        }
    }

    fun resetToIdle() {
        _uiState.value = AnalysisUiState.Idle
        refreshProtectionStatus()
    }

    fun dismissAlert(alertId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            db.securityAlertDao().deleteAlert(alertId)
            refreshProtectionStatus()
        }
    }

    fun clearScanHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            db.scanResultDao().clearAllScans()
            db.securityAlertDao().clearAlerts()
            refreshProtectionStatus()
        }
    }
}
