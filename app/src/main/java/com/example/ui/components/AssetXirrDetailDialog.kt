package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.finance.AssetXirrSummary
import com.example.ui.screens.portfolio.AssetWithHolding
import com.example.ui.theme.*

@Composable
fun AssetXirrDetailDialog(
    holding: AssetWithHolding,
    onDismiss: () -> Unit
) {
    val summary = holding.xirrSummary ?: return
    val isPositive = (summary.xirrPercentage ?: 0.0) >= 0
    val xirrColor = if (isPositive) Color(0xFF16A34A) else Color(0xFFDC2626)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Asset Name and Dismiss
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = holding.asset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = holding.category.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                // Hero XIRR Metric Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPositive) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    border = BorderStroke(1.dp, if (isPositive) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "EXTENDED INTERNAL RATE OF RETURN",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isPositive) Color(0xFF15803D) else Color(0xFFB91C1C)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (summary.xirrPercentage != null) {
                                    "${if (isPositive) "+" else ""}${String.format("%.2f", summary.xirrPercentage)}% p.a."
                                } else "N/A",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = xirrColor
                            )
                            Text(
                                text = "Annualized compound return accounting for exact dates",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (isPositive) Color(0xFF22C55E) else Color(0xFFEF4444),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                // Key Performance Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Cost Basis",
                        value = CurrencyFormatter.formatInr(summary.totalInvested),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Current Value",
                        value = CurrencyFormatter.formatInr(summary.currentValue),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricBox(
                        title = "Absolute Return",
                        value = "${CurrencyFormatter.formatInr(summary.absoluteGain, showSign = true)} (${CurrencyFormatter.formatGrowthPercentage(summary.returnPercentage)})",
                        valueColor = if (summary.absoluteGain >= 0) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        title = "Holding Period",
                        value = summary.holdingPeriodFormatted.ifBlank { "${summary.holdingPeriodDays} days" },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Educational Note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HighDensityBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "XIRR takes into account the exact dates of every deposit, SIP contribution, and capital withdrawal to compute your true annualized yield.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                }

                // Cash Flows Timeline Header
                Text(
                    text = "CASH FLOWS TIMELINE (${summary.cashFlows.size})",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )

                // Cash Flows List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(summary.cashFlows) { flow ->
                        val isOutflow = flow.amount < 0
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOutflow) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                                        contentDescription = null,
                                        tint = if (isOutflow) Color(0xFFD97706) else Color(0xFF16A34A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Column {
                                        Text(
                                            text = flow.date,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = if (isOutflow) "Investment / Outflow" else "Terminal Valuation",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                Text(
                                    text = if (isOutflow) {
                                        "-${CurrencyFormatter.formatInr(-flow.amount)}"
                                    } else {
                                        "+${CurrencyFormatter.formatInr(flow.amount)}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isOutflow) Color(0xFFD97706) else Color(0xFF16A34A)
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                ) {
                    Text("Got It")
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    valueColor: Color = HighDensityTextPrimary,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = HighDensityBg,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = HighDensityTextSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}
