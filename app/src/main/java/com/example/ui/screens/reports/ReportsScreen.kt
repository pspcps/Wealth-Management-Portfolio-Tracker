package com.example.ui.screens.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.IconMapper
import com.example.ui.theme.*
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val comp = state.comparison

    var showPeriodADropdown by remember { mutableStateOf(false) }
    var showPeriodBDropdown by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf(0) } // 0: Categories, 1: Assets, 2: Expenses

    Scaffold(
        containerColor = HighDensityBg,
        topBar = {
            Surface(
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_reports")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HighDensityTextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Historical Reports & Audit",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Two-period comparative deep dive",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    GlobalPrivacyEyeButton(
                        size = 36.dp,
                        iconSize = 18.dp,
                        testTag = "btn_reports_privacy_eye"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // Period Selectors Row: [From: Jan 2026] vs [To: Aug 2026]
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Compare Two Time Periods",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Period A Selector
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = HighDensityBg,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showPeriodADropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Baseline (From)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                            Text(comp?.periodALabel ?: state.periodAId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = HighDensityTextSecondary)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showPeriodADropdown,
                                    onDismissRequest = { showPeriodADropdown = false }
                                ) {
                                    state.snapshots.forEach { snap ->
                                        DropdownMenuItem(
                                            text = { Text("${DateFormatSymbols().shortMonths[snap.month - 1]} ${snap.year}") },
                                            onClick = {
                                                viewModel.setPeriodA(snap.id)
                                                showPeriodADropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp))

                            // Period B Selector
                            Box(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = HighDensityBg,
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { showPeriodBDropdown = true }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Target (To)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                            Text(comp?.periodBLabel ?: state.periodBId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                        }
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = HighDensityTextSecondary)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showPeriodBDropdown,
                                    onDismissRequest = { showPeriodBDropdown = false }
                                ) {
                                    state.snapshots.forEach { snap ->
                                        DropdownMenuItem(
                                            text = { Text("${DateFormatSymbols().shortMonths[snap.month - 1]} ${snap.year}") },
                                            onClick = {
                                                viewModel.setPeriodB(snap.id)
                                                showPeriodBDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (comp != null) {
                // High Level Growth Summary Card
                item {
                    val isPos = comp.totalDiff >= 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL PORTFOLIO GROWTH",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSecondary
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isPos) Emerald500.copy(alpha = 0.12f) else Ruby500.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${if (isPos) "+" else ""}${String.format("%.2f", comp.totalGrowthPercentage ?: 0.0)}%",
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPos) Emerald500 else Ruby500,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(comp.periodALabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                    Text(CurrencyFormatter.formatInr(comp.totalA), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Absolute Change", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                    Text(
                                        text = CurrencyFormatter.formatInr(comp.totalDiff, showSign = true),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPos) Emerald500 else Ruby500
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(comp.periodBLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                    Text(CurrencyFormatter.formatInr(comp.totalB), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                }
                            }
                        }
                    }
                }

                // Tab Row for Detailed Diff (Categories vs Assets vs Expenses)
                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(
                                0 to "Categories (${comp.categoryComparisons.size})",
                                1 to "Assets (${comp.assetComparisons.size})",
                                2 to "Expenses"
                            ).forEach { (tabIdx, label) ->
                                val isSel = activeTab == tabIdx
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { activeTab = tabIdx },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) HighDensityPrimaryContainer else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSel) HighDensityPillBorder else Color.Transparent)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSel) HighDensityOnPrimaryContainer else HighDensityTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 0: Categories Diff List
                if (activeTab == 0) {
                    items(comp.categoryComparisons, key = { it.category.id }) { catComp ->
                        val isCatPos = catComp.diffAmount >= 0
                        val catColor = IconMapper.parseColor(catComp.category.colorHex)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = IconMapper.getIcon(catComp.category.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            catComp.category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatInrCompact(catComp.valueA)} → ${CurrencyFormatter.formatInrCompact(catComp.valueB)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatInr(catComp.diffAmount, showSign = true),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCatPos) Emerald500 else Ruby500
                                    )
                                    if (catComp.growthPercentage != null && catComp.valueA > 0) {
                                        Text(
                                            text = "${if (isCatPos) "+" else ""}${String.format("%.1f", catComp.growthPercentage)}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isCatPos) Emerald500 else Ruby500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 1: Asset-level Diff List
                if (activeTab == 1) {
                    items(comp.assetComparisons, key = { it.asset.id }) { assetComp ->
                        val isAssetPos = assetComp.diffAmount >= 0
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        assetComp.asset.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = "${assetComp.categoryName} • ${CurrencyFormatter.formatInrCompact(assetComp.valueA)} → ${CurrencyFormatter.formatInrCompact(assetComp.valueB)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = CurrencyFormatter.formatInr(assetComp.diffAmount, showSign = true),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isAssetPos) Emerald500 else Ruby500
                                    )
                                    if (assetComp.growthPercentage != null && assetComp.valueA > 0) {
                                        Text(
                                            text = "${if (isAssetPos) "+" else ""}${String.format("%.1f", assetComp.growthPercentage)}%",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isAssetPos) Emerald500 else Ruby500
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab 2: Expenses Comparison
                if (activeTab == 2) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Monthly Spending Comparison",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("${comp.periodALabel} Spending", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                        Text(CurrencyFormatter.formatInr(comp.expenseA), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("${comp.periodBLabel} Spending", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                        Text(CurrencyFormatter.formatInr(comp.expenseB), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                    }
                                }

                                val diff = comp.expenseDiff
                                val isIncrease = diff > 0
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isIncrease) Ruby500.copy(alpha = 0.12f) else Emerald500.copy(alpha = 0.12f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "${if (isIncrease) "Spending increased by " else "Spending reduced by "}${CurrencyFormatter.formatInr(Math.abs(diff))}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isIncrease) Ruby500 else Emerald500,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

