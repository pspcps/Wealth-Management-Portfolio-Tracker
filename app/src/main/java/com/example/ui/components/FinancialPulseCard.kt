package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CategoryGrowthItem
import com.example.domain.model.FireForecastResult
import com.example.domain.model.PortfolioSummary
import com.example.ui.theme.*

/**
 * High-clarity Financial Pulse card displaying:
 * - What's going good (Green highlights)
 * - What needs attention (Amber / Rose cautions)
 * Directly addresses user request for clear operational visibility on portfolio, returns, and expenses.
 */
@Composable
fun FinancialPulseCard(
    summary: PortfolioSummary?,
    categoryGrowths: List<CategoryGrowthItem>,
    fireForecast: FireForecastResult?,
    modifier: Modifier = Modifier
) {
    if (summary == null) return

    var isExpanded by remember { mutableStateOf(true) }

    // Analyze what's going good
    val goods = remember(summary, categoryGrowths, fireForecast) {
        val list = mutableListOf<PulseItem>()

        // 1. Organic market gain
        if (summary.marketOrganicGain > 0) {
            val pct = CurrencyFormatter.formatGrowthPercentage(summary.marketOrganicPercentage)
            list.add(
                PulseItem(
                    title = "Organic Market Performance",
                    description = "Pure market appreciation added +${CurrencyFormatter.formatInr(summary.marketOrganicGain)} ($pct), strictly excluding new capital/SIPs.",
                    icon = Icons.Default.TrendingUp,
                    color = Emerald500
                )
            )
        }

        // 2. Net Monthly Savings from Salary
        if (summary.netMonthlySavings > 0) {
            list.add(
                PulseItem(
                    title = "Positive Monthly Cash Flow",
                    description = "Retained +${CurrencyFormatter.formatInr(summary.netMonthlySavings)} from monthly salary after expenses.",
                    icon = Icons.Default.Savings,
                    color = Emerald500
                )
            )
        }

        // 3. Top asset category performer
        val topCategory = categoryGrowths
            .filter { (it.growthPercentage ?: 0.0) > 0 }
            .maxByOrNull { it.growthPercentage ?: 0.0 }
        if (topCategory != null) {
            val pctStr = CurrencyFormatter.formatGrowthPercentage(topCategory.growthPercentage)
            list.add(
                PulseItem(
                    title = "Top Asset Driver: ${topCategory.category.name}",
                    description = "Grew by $pctStr (+${CurrencyFormatter.formatInr(topCategory.diffAmount)}) vs benchmark period.",
                    icon = Icons.Default.Star,
                    color = Emerald500
                )
            )
        }

        // 4. FIRE progress milestone
        if (fireForecast != null && fireForecast.targetFireCorpus > 0) {
            val progressPct = fireForecast.progressPercentage
            if (progressPct >= 5.0) {
                list.add(
                    PulseItem(
                        title = "FIRE Corpus Pacing",
                        description = "Achieved ${String.format("%.1f", progressPct)}% of your target FIRE corpus (${CurrencyFormatter.formatInrCompact(fireForecast.targetFireCorpus)}).",
                        icon = Icons.Default.CheckCircle,
                        color = Emerald500
                    )
                )
            }
        }

        list
    }

    // Analyze what needs attention
    val attentions = remember(summary, categoryGrowths, fireForecast) {
        val list = mutableListOf<PulseItem>()

        // 1. Injected capital clarity
        if (summary.newCapitalInvested > 0) {
            list.add(
                PulseItem(
                    title = "Capital Allocation Adjustment",
                    description = "${CurrencyFormatter.formatInr(summary.newCapitalInvested)} new funds (Stocks/SIPs) moved into investments from your bank; isolated to keep ROI realistic.",
                    icon = Icons.Default.AccountBalance,
                    color = Color(0xFFD97706)
                )
            )
        }

        // 2. Lagging / Underperforming categories
        val laggingCategory = categoryGrowths
            .filter { (it.growthPercentage ?: 0.0) < -0.5 }
            .minByOrNull { it.growthPercentage ?: 0.0 }
        if (laggingCategory != null) {
            val pctStr = CurrencyFormatter.formatGrowthPercentage(laggingCategory.growthPercentage)
            list.add(
                PulseItem(
                    title = "Lagging Segment: ${laggingCategory.category.name}",
                    description = "Down $pctStr (${CurrencyFormatter.formatInr(laggingCategory.diffAmount)}) vs benchmark.",
                    icon = Icons.Default.Warning,
                    color = Ruby500
                )
            )
        }

        // 3. Negative organic return or market dip
        if (summary.marketOrganicGain < 0) {
            val pct = CurrencyFormatter.formatGrowthPercentage(summary.marketOrganicPercentage)
            list.add(
                PulseItem(
                    title = "Market Correction / Dip",
                    description = "Organic market decline of ${CurrencyFormatter.formatInr(summary.marketOrganicGain)} ($pct). Good opportunity for disciplined SIP accumulation.",
                    icon = Icons.Default.TrendingDown,
                    color = Ruby500
                )
            )
        }

        // 4. Cash Drag check
        val cashCategory = categoryGrowths.firstOrNull { it.category.name.contains("Cash", ignoreCase = true) || it.category.name.contains("Bank", ignoreCase = true) }
        if (cashCategory != null && summary.totalValue > 0) {
            val cashRatio = (cashCategory.currentValue / summary.totalValue) * 100.0
            if (cashRatio > 35.0) {
                list.add(
                    PulseItem(
                        title = "High Cash Allocation (${String.format("%.0f", cashRatio)}%)",
                        description = "Over 35% in bank cash can cause inflation drag. Consider routing surplus into diversified index funds or liquid funds.",
                        icon = Icons.Default.Info,
                        color = Color(0xFFD97706)
                    )
                )
            }
        }

        list
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0284C7).copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Insights,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Financial Health Pulse",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (attentions.isEmpty()) Emerald500.copy(alpha = 0.12f) else Color(0xFFD97706).copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (attentions.isEmpty()) "Optimal" else "${attentions.size} Alerts",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (attentions.isEmpty()) Emerald500 else Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                        Text(
                            text = "What's going good & what requires attention",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = HighDensityTextSecondary
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HorizontalDivider(color = Color(0xFFF1F5F9))

                    // SECTION 1: What's Going Good
                    if (goods.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Emerald500)
                                )
                                Text(
                                    text = "WHAT'S GOING GOOD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                            }

                            goods.forEach { item ->
                                PulseItemRow(item = item)
                            }
                        }
                    }

                    // SECTION 2: What Needs Attention
                    if (attentions.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD97706))
                                )
                                Text(
                                    text = "WHAT NEEDS ATTENTION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706)
                                )
                            }

                            attentions.forEach { item ->
                                PulseItemRow(item = item)
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class PulseItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun PulseItemRow(item: PulseItem) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = item.color.copy(alpha = 0.12f),
                modifier = Modifier.size(24.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = item.color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                    color = HighDensityTextSecondary,
                    lineHeight = 14.sp
                )
            }
        }
    }
}
