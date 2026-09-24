package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.domain.model.ApkMetadata
import com.example.domain.model.RiskEvaluation
import com.example.domain.model.RiskLevel
import com.example.ui.components.RiskScoreCard
import com.example.ui.components.SecurityFindingItem
import com.example.ui.theme.ShieldBluePrimary
import com.example.ui.theme.ShieldRiskCritical
import com.example.ui.theme.ShieldRiskHigh
import com.example.ui.theme.ShieldRiskHighBg
import com.example.ui.theme.ShieldRiskLow
import com.example.ui.theme.ShieldRiskMedium
import com.example.ui.viewmodel.AnalysisUiState

@Composable
fun ApkDetectionScreen(
    uiState: AnalysisUiState,
    onInstall: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    when (uiState) {
        is AnalysisUiState.Analyzing -> {
            AnalyzingProgressView(uiState.progress, uiState.stage)
        }
        is AnalysisUiState.Analyzed -> {
            AnalyzedReportView(
                metadata = uiState.metadata,
                evaluation = uiState.evaluation,
                isDuplicate = uiState.isDuplicate,
                onInstall = onInstall,
                onCancel = onCancel
            )
        }
        is AnalysisUiState.Installing -> {
            InstallingProgressView(uiState.progress, uiState.message)
        }
        is AnalysisUiState.InstallCompleted -> {
            InstallSuccessView(
                appName = uiState.appName,
                packageName = uiState.packageName,
                environment = uiState.environment,
                onOpenApp = {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(uiState.packageName)
                    if (launchIntent != null) {
                        context.startActivity(launchIntent)
                    }
                },
                onDone = onCancel
            )
        }
        is AnalysisUiState.Error -> {
            AnalysisErrorView(uiState.message, onCancel)
        }
        else -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No APK currently loaded.")
            }
        }
    }
}

@Composable
private fun AnalyzingProgressView(progress: Float, stage: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("analyzing_progress_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(ShieldBluePrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Analyzing",
                tint = ShieldBluePrimary,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Analyzing APK Security",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = ShieldBluePrimary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = ShieldBluePrimary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Checklist of steps
        ChecklistStep(title = "Reading APK file...", isDone = progress >= 0.2f)
        ChecklistStep(title = "Validating APK integrity & signatures...", isDone = progress >= 0.4f)
        ChecklistStep(title = "Extracting manifest, components & permissions...", isDone = progress >= 0.6f)
        ChecklistStep(title = "Extracting ML behavioral features (30 parameters)...", isDone = progress >= 0.75f)
        ChecklistStep(title = "Running on-device ML risk inference...", isDone = progress >= 0.9f)
    }
}

@Composable
private fun ChecklistStep(title: String, isDone: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (isDone) ShieldRiskLow else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = if (isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun AnalyzedReportView(
    metadata: ApkMetadata,
    evaluation: RiskEvaluation,
    isDuplicate: Boolean,
    onInstall: (String) -> Unit,
    onCancel: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "App Info", "Permissions", "Components", "Recommendation")
    var selectedEnv by remember {
        mutableStateOf(if (evaluation.level == RiskLevel.LOW) "NORMAL" else "MANAGED_PROFILE")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("analyzed_report_view")
    ) {
        // App Identity Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (metadata.icon != null) {
                    val bitmap = remember(metadata.icon) {
                        try { metadata.icon.toBitmap(128, 128) } catch (e: Exception) { null }
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = metadata.appName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        FallbackAppIcon()
                    }
                } else {
                    FallbackAppIcon()
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = metadata.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = metadata.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Version ${metadata.versionName} • ${(metadata.fileSize / (1024f * 1024f)).formatMb()} MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    if (metadata.sourceApp.isNotBlank()) {
                        Text(
                            text = "Source: ${metadata.sourceApp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ShieldBluePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (isDuplicate) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Duplicate",
                        tint = ShieldBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Previously analyzed APK detected (matching SHA-256).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = title, fontWeight = FontWeight.Medium) },
                    modifier = Modifier.testTag("tab_$index")
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTabIndex) {
                0 -> OverviewTab(metadata, evaluation)
                1 -> AppInfoTab(metadata)
                2 -> PermissionsTab(metadata)
                3 -> ComponentsTab(metadata)
                4 -> RecommendationTab(
                    metadata = metadata,
                    evaluation = evaluation,
                    selectedEnv = selectedEnv,
                    onSelectEnv = { selectedEnv = it },
                    onInstall = { onInstall(selectedEnv) },
                    onCancel = onCancel
                )
            }
        }
    }
}

@Composable
private fun FallbackAppIcon() {
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(ShieldBluePrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = ShieldBluePrimary,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun OverviewTab(metadata: ApkMetadata, evaluation: RiskEvaluation) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            RiskScoreCard(evaluation = evaluation)
        }

        item {
            Text(
                text = "Key Security Findings (${evaluation.findings.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(evaluation.findings) { finding ->
            SecurityFindingItem(finding = finding)
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Static & ML Analysis Notice",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Static analysis and ML inference evaluate security indicators, attack surface, and permission risks. Static analysis cannot guarantee 100% detection of zero-day threats.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AppInfoTab(metadata: ApkMetadata) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Technical Metadata",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            InfoCard(label = "Package Name", value = metadata.packageName)
        }
        item {
            InfoCard(label = "Version", value = "${metadata.versionName} (${metadata.versionCode})")
        }
        item {
            InfoCard(label = "Min SDK", value = "Android ${metadata.minSdk} (API ${metadata.minSdk})")
        }
        item {
            InfoCard(label = "Target SDK", value = "Android ${metadata.targetSdk} (API ${metadata.targetSdk})")
        }
        item {
            InfoCard(label = "File Size", value = "${(metadata.fileSize / (1024f * 1024f)).formatMb()} MB (${metadata.fileSize} bytes)")
        }
        item {
            InfoCard(label = "APK SHA-256 Hash", value = metadata.sha256)
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Signing Certificate",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        if (metadata.certificate != null) {
            item {
                InfoCard(label = "Subject", value = metadata.certificate.subject)
            }
            item {
                InfoCard(label = "Issuer", value = metadata.certificate.issuer)
            }
            item {
                InfoCard(label = "Certificate SHA-256", value = metadata.certificate.sha256)
            }
            item {
                InfoCard(
                    label = "Self-Signed",
                    value = if (metadata.certificate.isSelfSigned) "Yes (Unknown Developer / Debug Key)" else "No (Signed by authority)"
                )
            }
            item {
                InfoCard(label = "Validity", value = "${metadata.certificate.validFrom} to ${metadata.certificate.validUntil}")
            }
        } else {
            item {
                Text(
                    text = "No signing certificate found or signature unreadable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PermissionsTab(metadata: ApkMetadata) {
    var filterType by remember { mutableStateOf("ALL") }
    val filteredPermissions = remember(filterType, metadata.permissions) {
        when (filterType) {
            "DANGEROUS" -> metadata.permissions.filter { it.isDangerous }
            "NORMAL" -> metadata.permissions.filter { !it.isDangerous }
            else -> metadata.permissions
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" },
                label = { Text("All (${metadata.permissions.size})") }
            )
            FilterChip(
                selected = filterType == "DANGEROUS",
                onClick = { filterType = "DANGEROUS" },
                label = { Text("Dangerous (${metadata.permissions.count { it.isDangerous }})") }
            )
            FilterChip(
                selected = filterType == "NORMAL",
                onClick = { filterType = "NORMAL" },
                label = { Text("Normal (${metadata.permissions.count { !it.isDangerous }})") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (filteredPermissions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No permissions found in this category.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredPermissions) { perm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = perm.simpleName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (perm.isDangerous) ShieldRiskHigh.copy(alpha = 0.12f) else ShieldRiskLow.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (perm.isDangerous) "Dangerous" else "Normal",
                                        color = if (perm.isDangerous) ShieldRiskHigh else ShieldRiskLow,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Group: ${perm.group}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ShieldBluePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = perm.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentsTab(metadata: ApkMetadata) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Application Components & Binaries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            ComponentSummaryRow("Activities", metadata.activities.size, metadata.activities.count { it.isExported })
        }
        item {
            ComponentSummaryRow("Services", metadata.services.size, metadata.services.count { it.isExported })
        }
        item {
            ComponentSummaryRow("Broadcast Receivers", metadata.receivers.size, metadata.receivers.count { it.isExported })
        }
        item {
            ComponentSummaryRow("Content Providers", metadata.providers.size, metadata.providers.count { it.isExported })
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "DEX Files & Native Libraries",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "DEX files count: ${metadata.dexFilesCount}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Native .so libraries count: ${metadata.nativeLibraries.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (metadata.nativeLibraries.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Libraries: " + metadata.nativeLibraries.take(5).joinToString(", ") +
                                    if (metadata.nativeLibraries.size > 5) " (+${metadata.nativeLibraries.size - 5} more)" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentSummaryRow(name: String, total: Int, exported: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Total: $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (exported > 0) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ShieldRiskMedium.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "$exported Exported",
                            color = ShieldRiskMedium,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendationTab(
    metadata: ApkMetadata,
    evaluation: RiskEvaluation,
    selectedEnv: String,
    onSelectEnv: (String) -> Unit,
    onInstall: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Recommendation header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (evaluation.level == RiskLevel.LOW) ShieldRiskLow.copy(alpha = 0.08f) else ShieldRiskHighBg
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (evaluation.level == RiskLevel.LOW) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (evaluation.level == RiskLevel.LOW) ShieldRiskLow else ShieldRiskHigh,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (evaluation.level == RiskLevel.LOW) "Low Risk Application" else "High Risk Indicators Detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (evaluation.level == RiskLevel.LOW) ShieldRiskLow else ShieldRiskHigh
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = evaluation.recommendation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Text(
                text = "Choose Installation Environment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Environment Option 1: Secure Environment
            EnvironmentOptionCard(
                title = "Run in Secure Environment (Isolated Storage)",
                subtitle = "Recommended for unknown apps. Targets Android Work Profile isolation with separate Linux UID, dedicated SELinux domain, and isolated profile storage.",
                isSelected = selectedEnv == "MANAGED_PROFILE",
                onClick = { onSelectEnv("MANAGED_PROFILE") }
            )

            // Environment Option 2: Normal Installation
            EnvironmentOptionCard(
                title = "Install Normally",
                subtitle = "Installs in your primary user profile. Uses Android's standard Linux UID sandbox and Scoped Storage protections.",
                isSelected = selectedEnv == "NORMAL",
                onClick = { onSelectEnv("NORMAL") }
            )
        }

        // Action buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onInstall,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("install_app_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ShieldBluePrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedEnv == "MANAGED_PROFILE") "Install in Secure Environment" else "Install Normally"
                )
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("cancel_installation_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Cancel & Discard")
            }
        }
    }
}

@Composable
private fun EnvironmentOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ShieldBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, ShieldBluePrimary) else null,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                modifier = Modifier.padding(top = 2.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InstallingProgressView(progress: Float, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("installing_progress_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = ShieldBluePrimary,
            strokeWidth = 5.dp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Installing Application",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = ShieldBluePrimary
        )
    }
}

@Composable
private fun InstallSuccessView(
    appName: String,
    packageName: String,
    environment: String,
    onOpenApp: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("install_success_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(ShieldRiskLow.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = ShieldRiskLow,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "App Installed Successfully",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$appName ($packageName)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Installation Environment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = environment,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ShieldBluePrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Real-time package and security event monitoring is active.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onOpenApp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("open_installed_app_button"),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ShieldBluePrimary)
        ) {
            Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Open App")
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("done_install_button"),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "Done")
        }
    }
}

@Composable
private fun AnalysisErrorView(error: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("analysis_error_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = ShieldRiskHigh,
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Analysis Failed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(text = "Go Back")
        }
    }
}

private fun Float.formatMb(): String {
    return String.format(java.util.Locale.US, "%.1f", this)
}
