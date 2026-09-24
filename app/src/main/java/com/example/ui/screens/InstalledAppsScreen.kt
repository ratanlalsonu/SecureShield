package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.domain.model.InstalledAppInfo
import com.example.domain.model.RiskLevel
import com.example.ui.theme.ShieldBluePrimary
import com.example.ui.theme.ShieldRiskCritical
import com.example.ui.theme.ShieldRiskHigh
import com.example.ui.theme.ShieldRiskLow
import com.example.ui.theme.ShieldRiskMedium
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InstalledAppsScreen(
    apps: List<InstalledAppInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedAppForDialog by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var showFindingsDialog by remember { mutableStateOf<List<String>?>(null) }

    val filterOptions = listOf(
        "ALL" to "All (${apps.size})",
        "SECURE" to "Secure Env",
        "NORMAL" to "Normal Profile",
        "HIGH_RISK" to "High Risk",
        "MEDIUM_RISK" to "Medium Risk",
        "LOW_RISK" to "Low Risk",
        "SYSTEM" to "System Apps"
    )

    val filteredApps = remember(apps, searchQuery, selectedFilter) {
        apps.filter { app ->
            val matchesSearch = app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (selectedFilter) {
                "ALL" -> true
                "SECURE" -> app.isManagedProfile || app.installationEnvironment == "MANAGED_PROFILE"
                "NORMAL" -> !app.isManagedProfile && app.installationEnvironment != "MANAGED_PROFILE" && !app.isSystemApp
                "HIGH_RISK" -> (app.scannedRiskScore ?: 0) >= 60 ||
                        app.scannedRiskLevel == RiskLevel.HIGH ||
                        app.scannedRiskLevel == RiskLevel.CRITICAL
                "MEDIUM_RISK" -> (app.scannedRiskScore ?: 0) in 30..59 ||
                        app.scannedRiskLevel == RiskLevel.MEDIUM
                "LOW_RISK" -> (app.scannedRiskScore != null && (app.scannedRiskScore ?: 0) < 30) ||
                        app.scannedRiskLevel == RiskLevel.LOW
                "SYSTEM" -> app.isSystemApp
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("installed_apps_screen")
    ) {
        // Search bar & refresh
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("search_apps_input"),
                placeholder = { Text("Search installed apps...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("refresh_apps_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Horizontal filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filterOptions.forEach { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No applications matching filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredApps, key = { it.packageName }) { app ->
                    InstalledAppRow(
                        app = app,
                        onClick = { selectedAppForDialog = app }
                    )
                }
            }
        }
    }

    // App Details & Action Dialog (Requirement 23)
    selectedAppForDialog?.let { app ->
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val scanDateText = if (app.scanTimestamp != null && app.scanTimestamp > 0) {
            dateFormat.format(Date(app.scanTimestamp))
        } else {
            "Not scanned prior to installation"
        }

        val envText = if (app.isManagedProfile || app.installationEnvironment == "MANAGED_PROFILE") {
            "Managed Profile (Secure Environment)"
        } else {
            "Normal Android Profile"
        }

        AlertDialog(
            onDismissRequest = { selectedAppForDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (app.icon != null) {
                        val bm = remember(app.icon) {
                            try { app.icon.toBitmap(64, 64) } catch (e: Exception) { null }
                        }
                        if (bm != null) {
                            Image(
                                bitmap = bm.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                    }
                    Text(text = app.appName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "• Version: ${app.versionName}", style = MaterialTheme.typography.bodySmall)
                    Text(text = "• Target SDK: ${app.targetSdk}", style = MaterialTheme.typography.bodySmall)
                    Text(
                        text = "• Permissions: ${app.permissionsCount} total (${app.dangerousPermissionsCount} sensitive)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (app.dangerousPermissionsCount > 0) ShieldRiskMedium else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• Environment: $envText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(text = "• Status: Installed & Active", style = MaterialTheme.typography.bodySmall)

                    if (app.scannedRiskScore != null) {
                        val riskColor = when (app.scannedRiskLevel) {
                            RiskLevel.CRITICAL -> ShieldRiskCritical
                            RiskLevel.HIGH -> ShieldRiskHigh
                            RiskLevel.MEDIUM -> ShieldRiskMedium
                            else -> ShieldRiskLow
                        }
                        Text(
                            text = "• Risk Score: ${app.scannedRiskScore}/100 (${app.scannedRiskLevel?.label ?: "ASSESSED"})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = riskColor
                        )
                    } else {
                        Text(
                            text = "• Risk Score: Not pre-scanned by SecureShield",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    Text(
                        text = "• Last Scan: $scanDateText",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (app.certSha256 != null) {
                        Text(
                            text = "• Cert SHA-256: ${app.certSha256.take(16)}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    if (app.sensitiveFindings.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedButton(
                            onClick = {
                                showFindingsDialog = app.sensitiveFindings
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View Scan Findings (${app.sensitiveFindings.size})", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        }
                        selectedAppForDialog = null
                    }
                ) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Open")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${app.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            selectedAppForDialog = null
                        }
                    ) {
                        Text("App Info")
                    }
                    if (!app.isSystemApp) {
                        OutlinedButton(
                            onClick = {
                                val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.parse("package:${app.packageName}")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(uninstallIntent)
                                selectedAppForDialog = null
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ShieldRiskHigh)
                        ) {
                            Text("Uninstall")
                        }
                    }
                }
            }
        )
    }

    // Findings dialog
    showFindingsDialog?.let { findings ->
        AlertDialog(
            onDismissRequest = { showFindingsDialog = null },
            title = {
                Text("Security Findings", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    findings.forEach { finding ->
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ShieldRiskMedium,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = finding, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showFindingsDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun InstalledAppRow(
    app: InstalledAppInfo,
    onClick: () -> Unit
) {
    val isSecure = app.isManagedProfile || app.installationEnvironment == "MANAGED_PROFILE"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("app_item_${app.packageName}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (app.icon != null) {
                val bitmap = remember(app.icon) {
                    try { app.icon.toBitmap(96, 96) } catch (e: Exception) { null }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.appName,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    FallbackAppRowIcon()
                }
            } else {
                FallbackAppRowIcon()
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (app.scannedRiskScore != null) {
                        val badgeColor = when (app.scannedRiskLevel) {
                            RiskLevel.CRITICAL -> ShieldRiskCritical
                            RiskLevel.HIGH -> ShieldRiskHigh
                            RiskLevel.MEDIUM -> ShieldRiskMedium
                            else -> ShieldRiskLow
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Risk ${app.scannedRiskScore}",
                                color = badgeColor,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    } else if (app.dangerousPermissionsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ShieldRiskMedium.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${app.dangerousPermissionsCount} Perms",
                                color = ShieldRiskMedium,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "v${app.versionName} • Target ${app.targetSdk}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSecure) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (isSecure) "Secure Env" else "Normal",
                            color = if (isSecure) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FallbackAppRowIcon() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}
