package com.example.ui.screens.portfolio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.AssetXirrDetailDialog
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.IconMapper
import com.example.ui.theme.*
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioScreen(
    viewModel: PortfolioViewModel,
    onNavigateToMonthlyUpdate: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var showInvestmentImportDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferFromAssetId by remember { mutableStateOf<Long?>(null) }
    var selectedHoldingDetails by remember { mutableStateOf<AssetWithHolding?>(null) }
    var assetToDelete by remember { mutableStateOf<AssetEntity?>(null) }
    var holdingForXirrDetails by remember { mutableStateOf<AssetWithHolding?>(null) }
    var expandedCategoryIds by remember { mutableStateOf(setOf<String>()) }

    val allActiveAssets = remember(state.assetsWithHoldings) {
        state.assetsWithHoldings.map { it.asset }
    }
    val currentValuesMap = remember(state.assetsWithHoldings) {
        state.assetsWithHoldings.associate { it.asset.id to it.currentValue }
    }

    // Filter holdings
    val filteredHoldings = remember(state.assetsWithHoldings, state.selectedGroupFilter, state.searchQuery) {
        state.assetsWithHoldings.filter { holding ->
            val matchesGroup = when (state.selectedGroupFilter) {
                "ALL" -> true
                else -> holding.category.groupType.equals(state.selectedGroupFilter, ignoreCase = true)
            }
            val matchesSearch = state.searchQuery.isEmpty() ||
                    holding.asset.name.contains(state.searchQuery, ignoreCase = true) ||
                    holding.category.name.contains(state.searchQuery, ignoreCase = true)
            matchesGroup && matchesSearch
        }
    }

    val groupedByCategory = remember(filteredHoldings) {
        filteredHoldings.groupBy { it.category }
    }

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
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "Asset Portfolio",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            maxLines = 1
                        )
                        Text(
                            text = "${state.assetsWithHoldings.size} assets tracked",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary,
                            maxLines = 1
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlobalPrivacyEyeButton(
                            size = 36.dp,
                            iconSize = 18.dp,
                            testTag = "btn_portfolio_privacy_eye"
                        )

                        OutlinedButton(
                            onClick = { showInvestmentImportDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF16A34A),
                                containerColor = Color(0xFFF0FDF4)
                            ),
                            modifier = Modifier.height(36.dp).testTag("btn_import_statement_portfolio")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Import", tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Import", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Button(
                            onClick = { 
                                transferFromAssetId = null
                                showTransferDialog = true 
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighDensityPrimaryContainer,
                                contentColor = HighDensityOnPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, HighDensityPillBorder),
                            modifier = Modifier.height(36.dp).testTag("btn_transfer_portfolio")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", tint = HighDensityOnPrimaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        FilledTonalButton(
                            onClick = onNavigateToMonthlyUpdate,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = HighDensityPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp).testTag("btn_monthly_entry_portfolio")
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Entry", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
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
            // Overall Portfolio & XIRR Executive Summary Card
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
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "PORTFOLIO VALUATION",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSecondary
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(state.totalCurrentValue),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = HighDensityTextPrimary
                                )
                            }

                            val portXirr = state.portfolioXirr
                            if (portXirr != null) {
                                val isXirrPos = portXirr >= 0
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isXirrPos) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                    border = BorderStroke(1.dp, if (isXirrPos) Color(0xFF86EFAC) else Color(0xFFFCA5A5))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isXirrPos) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                            contentDescription = null,
                                            tint = if (isXirrPos) Color(0xFF15803D) else Color(0xFFB91C1C),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "XIRR: ${if (isXirrPos) "+" else ""}${String.format("%.2f", portXirr)}% p.a.",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isXirrPos) Color(0xFF15803D) else Color(0xFFB91C1C)
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom row metrics: Invested vs Net Gain
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Invested",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = HighDensityTextSecondary
                                )
                                Text(
                                    text = CurrencyFormatter.formatInr(state.totalInvested),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextPrimary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Total Gain / Loss",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = HighDensityTextSecondary
                                )
                                val isGain = state.totalGainLoss >= 0
                                Text(
                                    text = "${CurrencyFormatter.formatInr(state.totalGainLoss, showSign = true)} (${CurrencyFormatter.formatGrowthPercentage(state.totalReturnPercentage)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626)
                                )
                            }
                        }
                    }
                }
            }

            // Search Input Field
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search assets or categories...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HighDensityTextSecondary) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = HighDensityTextSecondary)
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = HighDensityPrimary
                    )
                )
            }

            // Group Filter Chips Row
            item {
                val filterOptions = listOf(
                    "ALL" to "All Assets",
                    "INVESTMENT" to "Investments",
                    "CASH" to "Cash & Bank",
                    "RETIREMENT" to "Retirement",
                    "BENEFIT" to "Benefits",
                    "CUSTOM" to "Custom"
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterOptions) { (key, label) ->
                        val isSelected = state.selectedGroupFilter == key
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) HighDensityPrimaryContainer else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) HighDensityPillBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.setGroupFilter(key) }
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) HighDensityOnPrimaryContainer else HighDensityTextSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Category Show / Hide Quick Bar
            if (groupedByCategory.isNotEmpty()) {
                item {
                    val allExpanded = groupedByCategory.keys.all { expandedCategoryIds.contains(it.id) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATEGORIES (${groupedByCategory.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        TextButton(
                            onClick = {
                                expandedCategoryIds = if (allExpanded) {
                                    emptySet()
                                } else {
                                    groupedByCategory.keys.map { it.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = null,
                                tint = HighDensityPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allExpanded) "Collapse All" else "Expand All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                        }
                    }
                }
            }

            if (groupedByCategory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No assets found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Add assets to this category to begin tracking your portfolio.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HighDensityTextSecondary
                            )
                            Button(
                                onClick = onNavigateToMonthlyUpdate,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                modifier = Modifier.testTag("btn_empty_goto_monthly")
                            ) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add / Update in Monthly Entry")
                            }
                        }
                    }
                }
            } else {
                // Category Cards with Assets
                groupedByCategory.forEach { (category, holdings) ->
                    val isExpanded = expandedCategoryIds.contains(category.id)
                    val categoryTotal = holdings.sumOf { it.currentValue }
                    val catColor = IconMapper.parseColor(category.colorHex)

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
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Category Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategoryIds = if (expandedCategoryIds.contains(category.id)) {
                                                expandedCategoryIds - category.id
                                            } else {
                                                expandedCategoryIds + category.id
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = IconMapper.getIcon(category.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                     Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                        val totalCatInvested = holdings.sumOf { it.investedAmount }
                                        val totalCatGain = categoryTotal - totalCatInvested
                                        val catReturnPct = if (totalCatInvested > 0) (totalCatGain / totalCatInvested) * 100.0 else null
                                        val catXirr = state.categoryXirrMap[category.id]
                                        val xirrPart = if (catXirr != null) " • XIRR: ${if (catXirr >= 0) "+" else ""}${String.format("%.1f", catXirr)}%" else ""
                                        Text(
                                            text = if (totalCatInvested > 0) "Inv: ${CurrencyFormatter.formatInr(totalCatInvested)} • Gain: ${CurrencyFormatter.formatGrowthPercentage(catReturnPct)}$xirrPart" else "${holdings.size} holdings$xirrPart",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = if (totalCatGain >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyFormatter.formatInr(categoryTotal),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = HighDensityTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Assets in this Category
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        holdings.forEach { holding ->
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .clickable { selectedHoldingDetails = holding },
                                                shape = RoundedCornerShape(10.dp),
                                                color = HighDensityBg,
                                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = holding.asset.name,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = HighDensityTextPrimary
                                                            )

                                                            if (holding.asset.status != "ACTIVE") {
                                                                Surface(
                                                                    shape = RoundedCornerShape(4.dp),
                                                                    color = Color(0xFFE2E8F0)
                                                                ) {
                                                                    Text(
                                                                        text = holding.asset.status,
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                        color = HighDensityTextSecondary,
                                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }
                                                        }

                                                        if (holding.investedAmount > 0) {
                                                            val gainLoss = holding.gainLossAmount
                                                            val retPct = holding.returnPercentage
                                                            val isGain = gainLoss >= 0
                                                            val retColor = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                            Text(
                                                                text = "Cost: ${CurrencyFormatter.formatInr(holding.investedAmount)} • P&L: ${CurrencyFormatter.formatInr(gainLoss, showSign = true)} (${CurrencyFormatter.formatGrowthPercentage(retPct)})",
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                                color = retColor
                                                            )
                                                        } else if (holding.asset.notes.isNotEmpty()) {
                                                            Text(
                                                                text = holding.asset.notes,
                                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                                color = HighDensityTextSecondary,
                                                                maxLines = 1
                                                            )
                                                        }

                                                        // Investment Date & XIRR Pill Row
                                                        Row(
                                                            modifier = Modifier.padding(top = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            if (holding.asset.investmentDate.isNotBlank()) {
                                                                Text(
                                                                    text = "Since: ${holding.asset.investmentDate}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                    color = HighDensityTextSecondary
                                                                )
                                                            }

                                                            val xirrPct = holding.xirrSummary?.xirrPercentage
                                                            if (xirrPct != null) {
                                                                val isXirrPos = xirrPct >= 0
                                                                Surface(
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    color = if (isXirrPos) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                                                                    border = BorderStroke(1.dp, if (isXirrPos) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(12.dp))
                                                                        .clickable {
                                                                            holdingForXirrDetails = holding
                                                                        }
                                                                ) {
                                                                    Row(
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                                    ) {
                                                                        Icon(
                                                                            imageVector = if (isXirrPos) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                                            contentDescription = null,
                                                                            tint = if (isXirrPos) Color(0xFF15803D) else Color(0xFFB91C1C),
                                                                            modifier = Modifier.size(11.dp)
                                                                        )
                                                                        Text(
                                                                            text = "XIRR ${if (isXirrPos) "+" else ""}${String.format("%.1f", xirrPct)}% p.a.",
                                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = if (isXirrPos) Color(0xFF15803D) else Color(0xFFB91C1C)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Column(
                                                            horizontalAlignment = Alignment.End,
                                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                                        ) {
                                                            Text(
                                                                text = CurrencyFormatter.formatInr(holding.currentValue),
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                color = HighDensityTextPrimary
                                                            )

                                                            if (holding.growthPercentage != null && holding.previousValue > 0) {
                                                                val isPos = (holding.growthPercentage ?: 0.0) >= 0
                                                                Text(
                                                                    text = "MoM ${if (isPos) "+" else ""}${String.format("%.1f", holding.growthPercentage)}%",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = if (isPos) Emerald500 else Ruby500
                                                                )
                                                            }
                                                        }

                                                        IconButton(
                                                            onClick = { assetToDelete = holding.asset },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.DeleteOutline,
                                                                contentDescription = "Delete Asset",
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(16.dp)
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
                    }
                }
            }
        }
    }

    // Asset Details View Modal (Holdings is View Mode)
    if (selectedHoldingDetails != null) {
        val holding = selectedHoldingDetails!!
        val asset = holding.asset
        AlertDialog(
            onDismissRequest = { selectedHoldingDetails = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = asset.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = holding.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (asset.status == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = asset.status,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (asset.status == "ACTIVE") Color(0xFF15803D) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Valuation & Invested Summary Card
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Current Valuation", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                    Text(CurrencyFormatter.formatInr(holding.currentValue), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = HighDensityTextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Total Invested", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                    Text(CurrencyFormatter.formatInr(holding.investedAmount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                                }
                            }

                            HorizontalDivider(color = Color(0xFFE2E8F0))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                val gainLoss = holding.gainLossAmount
                                val returnPct = holding.returnPercentage
                                val isPos = gainLoss >= 0
                                Column {
                                    Text("Absolute Return", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                    Text(
                                        text = "${if (isPos) "+" else ""}${CurrencyFormatter.formatInr(gainLoss)} (${if (isPos) "+" else ""}${String.format("%.1f", returnPct)}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPos) Emerald500 else Ruby500
                                    )
                                }

                                val xirr = holding.xirrSummary?.xirrPercentage
                                if (xirr != null) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("XIRR Return", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                        Text(
                                            text = "${if (xirr >= 0) "+" else ""}${String.format("%.2f", xirr)}% p.a.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (xirr >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Asset Metadata & Payout Terms
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (asset.investmentDate.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Investment Date", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                Text(asset.investmentDate, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = HighDensityTextPrimary)
                            }
                        }

                        if (asset.interestRate > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Interest Rate", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                Text("${asset.interestRate}% p.a.", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }
                        }

                        if (asset.payoutFrequency != "NONE") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Interest Payout", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                Text(asset.payoutFrequency, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = HighDensityTextPrimary)
                            }
                        }

                        if (asset.principalPayoutPercent > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Periodic Principal Return", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                Text("${asset.principalPayoutPercent}% per payout", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFFD97706))
                            }
                        }

                        if (asset.installmentAmount > 0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("RD Installment", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                Text("₹${Math.round(asset.installmentAmount)} (${asset.installmentFrequency})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }
                        }

                        if (asset.maturityDate.isNotBlank()) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Maturity Date", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                                Text(asset.maturityDate, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = HighDensityTextPrimary)
                            }
                        }

                        if (asset.notes.isNotBlank()) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))
                            Text("Notes:", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                            Text(asset.notes, style = MaterialTheme.typography.bodySmall, color = HighDensityTextPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedHoldingDetails = null
                        onNavigateToMonthlyUpdate()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit in Monthly Entry")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedHoldingDetails = null }) {
                    Text("Close", color = HighDensityTextSecondary)
                }
            }
        )
    }


    // Delete Asset Confirmation Dialog
    if (assetToDelete != null) {
        val asset = assetToDelete!!
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Delete Asset Holding?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${asset.name}'? This will permanently remove this asset and all its historical valuations from your portfolio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HighDensityTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsset(asset)
                        assetToDelete = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) {
                    Text("Cancel", color = HighDensityTextSecondary)
                }
            }
        )
    }

    // JSON Import Dialog
    if (showImportDialog) {
        com.example.ui.components.SnapshotImportDialog(
            onDismiss = { showImportDialog = false },
            onImportConfirmed = { json ->
                showImportDialog = false
                viewModel.importJsonSnapshot(json)
            }
        )
    }

    // Broker Statement (Excel / CSV) Import Dialog
    if (showInvestmentImportDialog) {
        InvestmentImportDialog(
            onDismiss = { showInvestmentImportDialog = false },
            onImportConfirmed = { statement, targetSnapshotId, updateExisting ->
                viewModel.importInvestmentStatement(statement, targetSnapshotId, updateExisting)
            }
        )
    }

    if (showTransferDialog) {
        com.example.ui.components.TransferAssetDialog(
            snapshotId = state.selectedSnapshotId.ifEmpty { state.snapshots.lastOrNull()?.id ?: "" },
            assets = allActiveAssets,
            categories = state.categories,
            currentValues = currentValuesMap,
            preselectedFromAssetId = transferFromAssetId,
            onDismiss = { showTransferDialog = false },
            onConfirmTransfer = { fromId, toId, amount, closeSource, notes ->
                showTransferDialog = false
                viewModel.transferAsset(fromId, toId, amount, closeSource, notes)
            }
        )
    }

    // Asset XIRR Cash Flows & Breakdown Dialog
    holdingForXirrDetails?.let { holding ->
        AssetXirrDetailDialog(
            holding = holding,
            onDismiss = { holdingForXirrDetails = null }
        )
    }
}

