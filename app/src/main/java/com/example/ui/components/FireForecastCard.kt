package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FireForecastResult
import com.example.ui.theme.*

@Composable
fun FireForecastCard(
    forecast: FireForecastResult,
    onEditSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressFraction = (forecast.progressPercentage / 100.0).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "fire_progress")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("card_fire_forecast"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: FIRE badge + Status + Edit Option (Responsive Layout)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFFEDD5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "FIRE FORECAST",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C),
                                maxLines = 1
                            )
                        }
                    }

                    if (forecast.isFireAchieved) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "🎉 FIRE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16A34A),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    } else if (forecast.isCoastFireAchieved) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF)
                        ) {
                            Text(
                                text = "⛵ Coast FIRE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                maxLines = 1
                            )
                        }
                    }
                }

                // Edit Button (Fixed Non-Breaking Layout)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = HighDensityBg,
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onEditSettings() }
                        .testTag("btn_edit_fire_assumptions")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Edit Assumptions",
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Assumptions",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            // Target Corpus & Progress Bar Hero
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Target FIRE Corpus",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(forecast.targetFireCorpus),
                        style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary,
                        maxLines = 1
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Progress",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "${String.format("%.1f", forecast.progressPercentage)}%",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (forecast.progressPercentage >= 100) Color(0xFF16A34A) else Color(0xFFEA580C),
                        maxLines = 1
                    )
                }
            }

            // Custom Progress Track
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFF10B981))
                                )
                            )
                    )
                }

                // Sub-labels for Lean FIRE and Fat FIRE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lean: ${CurrencyFormatter.formatInr(forecast.leanFireCorpus)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "${forecast.settings.swrRate}% SWR",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = HighDensityTextSecondary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "Fat: ${CurrencyFormatter.formatInr(forecast.fatFireCorpus)}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // 3-Column Metrics Grid: Years to Freedom, Projected Age, Passive Income
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Column 1: Time to Freedom
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "TIME TO FIRE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = if (forecast.isFireAchieved) "Achieved! 🎉" else "${String.format("%.1f", forecast.yearsToFire)} Yrs",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = if (forecast.isFireAchieved) "Now" else "Yr ${forecast.projectedFireYear}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                }

                // Column 2: Freedom Age
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "PROJECTED AGE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "Age ${String.format("%.1f", forecast.projectedFireAge)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Target: ${forecast.settings.targetFireAge}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                }

                // Column 3: Monthly Passive Income
                Column(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = "PASSIVE INCOME",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = "${CurrencyFormatter.formatInr(forecast.monthlyPassiveIncomeNow)}/mo",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A),
                        maxLines = 1
                    )
                    Text(
                        text = "${String.format("%.0f", (forecast.monthlyPassiveIncomeNow / maxOf(1.0, forecast.settings.monthlyExpenses)) * 100)}% Lifestyle",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Assumptions Pill Footer
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEditSettings() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Return ${forecast.settings.expectedReturnRate}% • Inf ${forecast.settings.inflationRate}% • SIP ${CurrencyFormatter.formatInr(forecast.settings.monthlyInvestment)}/mo",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = HighDensityTextSecondary,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = HighDensityTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
