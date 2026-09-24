package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.ProtectionStatus
import com.example.ui.theme.ShieldBluePrimary
import com.example.ui.theme.ShieldRiskLow
import com.example.ui.theme.ShieldRiskMedium

@Composable
fun ProtectionStatusScreen(
    status: ProtectionStatus,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("protection_status_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Device Protection Architecture",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real Android security capabilities and active isolation layers.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 1. Secure Environment / Work Profile
        item {
            SecurityLayerCard(
                title = "Android Managed Profile (Work Profile)",
                status = if (status.isManagedProfileActive) "ACTIVE" else if (status.isSecureEnvironmentAvailable) "AVAILABLE" else "UNAVAILABLE",
                isPositive = status.isSecureEnvironmentAvailable || status.isManagedProfileActive,
                description = "Managed Profiles utilize Android's multi-user framework to create an isolated Linux user space (UID 100000+), separate SELinux contexts, and dedicated /data/user/10/ storage that prevents unauthorized cross-profile file access."
            )
        }

        // 2. Storage Protection
        item {
            SecurityLayerCard(
                title = "Android Scoped Storage & Linux UID",
                status = if (status.isStorageProtectionActive) "ACTIVE" else "LIMITED",
                isPositive = status.isStorageProtectionActive,
                description = status.storageProtectionMode + ". Every application runs in a private sandbox directory (/data/data/<package>/) shielded by Linux file permissions and SELinux enforcement."
            )
        }

        // 3. Usage Stats Monitoring
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Usage Access Telemetry",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (status.isUsageAccessGranted) ShieldRiskLow.copy(alpha = 0.12f) else ShieldRiskMedium.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (status.isUsageAccessGranted) "GRANTED" else "NOT GRANTED",
                                color = if (status.isUsageAccessGranted) ShieldRiskLow else ShieldRiskMedium,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Allows SecureShield to observe runtime application foreground launch times and durations via Android UsageStatsManager.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!status.isUsageAccessGranted) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ShieldBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Grant Usage Access")
                        }
                    }
                }
            }
        }

        // 4. Notification Alerts
        item {
            SecurityLayerCard(
                title = "Security Alert Notifications",
                status = if (status.isNotificationGranted) "ACTIVE" else "DISABLED",
                isPositive = status.isNotificationGranted,
                description = "Dispatches immediate notifications when sensitive or unverified packages are installed on device."
            )
        }

        // 5. Educational note on Android limitations
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ShieldBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Genuine Android Security Boundary",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Under standard Android security architecture, applications cannot arbitrarily intercept internal memory or private IPC of other apps. Genuine isolation is provided by Linux UID boundaries, SELinux policy enforcement, and Android Managed Profiles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SecurityLayerCard(
    title: String,
    status: String,
    isPositive: Boolean,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isPositive) ShieldRiskLow.copy(alpha = 0.12f) else ShieldRiskMedium.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = status,
                        color = if (isPositive) ShieldRiskLow else ShieldRiskMedium,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
