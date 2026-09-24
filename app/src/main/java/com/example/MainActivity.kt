package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.ApkDetectionScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InstalledAppsScreen
import com.example.ui.screens.ProtectionStatusScreen
import com.example.ui.screens.SecurityHistoryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.SecureShieldTheme
import com.example.ui.theme.ShieldBlueDark
import com.example.ui.theme.ShieldBluePrimary
import com.example.ui.viewmodel.AnalysisUiState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

enum class NavigationTab(val title: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home),
    APPS("My Apps", Icons.Default.Apps),
    HISTORY("History", Icons.Default.History),
    SETTINGS("Settings", Icons.Default.Settings)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIncomingIntent(intent)

        setContent {
            val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()
            SecureShieldTheme(themeConfig = themeConfig) {
                SecureShieldApp(
                    viewModel = viewModel,
                    onOpenApkUri = { uri -> viewModel.handleIncomingUri(uri) }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action

        when (action) {
            Intent.ACTION_VIEW -> {
                val uri: Uri? = intent.data
                if (uri != null) {
                    val caller = getCallingPackageName(intent)
                    viewModel.handleIncomingUri(uri, caller)
                }
            }
            Intent.ACTION_SEND -> {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) {
                    val caller = getCallingPackageName(intent)
                    viewModel.handleIncomingUri(uri, caller)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                val targetUri = uris?.firstOrNull()
                if (targetUri != null) {
                    val caller = getCallingPackageName(intent)
                    viewModel.handleIncomingUri(targetUri, caller)
                }
            }
        }
    }

    private fun getCallingPackageName(intent: Intent): String? {
        val callingPkg = callingPackage
        if (!callingPkg.isNullOrBlank()) return callingPkg
        return intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureShieldApp(
    viewModel: MainViewModel,
    onOpenApkUri: (Uri) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val protectionStatus by viewModel.protectionStatus.collectAsStateWithLifecycle()
    val alerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val scans by viewModel.allScans.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val isLoadingApps by viewModel.isLoadingApps.collectAsStateWithLifecycle()
    val themeConfig by viewModel.themeConfig.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(NavigationTab.HOME) }
    var showProtectionStatusDetail by remember { mutableStateOf(false) }

    val isAnalyzingOrShowingReport = uiState !is AnalysisUiState.Idle

    BackHandler(enabled = isAnalyzingOrShowingReport || showProtectionStatusDetail) {
        if (isAnalyzingOrShowingReport) {
            viewModel.resetToIdle()
        } else if (showProtectionStatusDetail) {
            showProtectionStatusDetail = false
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("secureshield_root_scaffold"),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isAnalyzingOrShowingReport) "APK Security Inspection"
                        else if (showProtectionStatusDetail) "Protection Architecture"
                        else when (currentTab) {
                            NavigationTab.HOME -> "SecureShield"
                            NavigationTab.APPS -> "Installed Applications"
                            NavigationTab.HISTORY -> "Security Scan History"
                            NavigationTab.SETTINGS -> "Settings & Themes"
                        },
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    if (isAnalyzingOrShowingReport || showProtectionStatusDetail) {
                        IconButton(
                            onClick = {
                                if (isAnalyzingOrShowingReport) viewModel.resetToIdle()
                                else showProtectionStatusDetail = false
                            },
                            modifier = Modifier.testTag("top_bar_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shield",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .padding(start = 16.dp)
                                .size(24.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            if (!isAnalyzingOrShowingReport) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationTab.values().forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab && !showProtectionStatusDetail,
                            onClick = {
                                currentTab = tab
                                showProtectionStatusDetail = false
                            },
                            icon = {
                                Icon(imageVector = tab.icon, contentDescription = tab.title)
                            },
                            label = { Text(text = tab.title) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                            ),
                            modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isAnalyzingOrShowingReport) {
                ApkDetectionScreen(
                    uiState = uiState,
                    onInstall = { env -> viewModel.initiateInstallation(env) },
                    onCancel = { viewModel.resetToIdle() },
                    onRescan = { viewModel.reAnalyzeCurrentApk() }
                )
            } else if (showProtectionStatusDetail) {
                ProtectionStatusScreen(
                    status = protectionStatus,
                    onRefresh = { viewModel.refreshProtectionStatus() }
                )
            } else {
                when (currentTab) {
                    NavigationTab.HOME -> {
                        DashboardScreen(
                            protectionStatus = protectionStatus,
                            alerts = alerts,
                            onSelectApkUri = onOpenApkUri,
                            onDismissAlert = { alertId -> viewModel.dismissAlert(alertId) },
                            onNavigateToStatus = { showProtectionStatusDetail = true }
                        )
                    }
                    NavigationTab.APPS -> {
                        InstalledAppsScreen(
                            apps = installedApps,
                            isLoading = isLoadingApps,
                            onRefresh = { viewModel.loadInstalledApps() }
                        )
                    }
                    NavigationTab.HISTORY -> {
                        SecurityHistoryScreen(
                            scans = scans,
                            onClearHistory = { viewModel.clearScanHistory() }
                        )
                    }
                    NavigationTab.SETTINGS -> {
                        SettingsScreen(
                            themeConfig = themeConfig,
                            onThemePaletteChange = { viewModel.setThemePalette(it) },
                            onThemeModeChange = { viewModel.setThemeMode(it) },
                            onAmoledToggle = { viewModel.setAmoledPureBlack(it) },
                            onClearDatabase = { viewModel.clearScanHistory() }
                        )
                    }
                }
            }
        }
    }
}
