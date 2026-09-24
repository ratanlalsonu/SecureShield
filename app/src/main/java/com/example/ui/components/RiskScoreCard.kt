package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.RiskEvaluation
import com.example.domain.model.RiskLevel
import com.example.ui.theme.ShieldRiskCritical
import com.example.ui.theme.ShieldRiskCriticalBg
import com.example.ui.theme.ShieldRiskHigh
import com.example.ui.theme.ShieldRiskHighBg
import com.example.ui.theme.ShieldRiskLow
import com.example.ui.theme.ShieldRiskLowBg
import com.example.ui.theme.ShieldRiskMedium
import com.example.ui.theme.ShieldRiskMediumBg

@Composable
fun RiskScoreCard(
    evaluation: RiskEvaluation,
    modifier: Modifier = Modifier
) {
    val (badgeBg, badgeColor) = when (evaluation.level) {
        RiskLevel.LOW -> Pair(ShieldRiskLowBg, ShieldRiskLow)
        RiskLevel.MEDIUM -> Pair(ShieldRiskMediumBg, ShieldRiskMedium)
        RiskLevel.HIGH -> Pair(ShieldRiskHighBg, ShieldRiskHigh)
        RiskLevel.CRITICAL -> Pair(ShieldRiskCriticalBg, ShieldRiskCritical)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("risk_score_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = badgeBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ML Predicted Risk Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${evaluation.score}",
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.testTag("risk_score_value")
                )
                Text(
                    text = " / 100",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = "${evaluation.level.label} RISK",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("risk_level_badge")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { (evaluation.score / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = badgeColor,
                trackColor = Color.White.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Model Confidence: ${evaluation.confidence}% • On-Device Inference",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
