package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.PortfolioViewEntity
import com.example.domain.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.FinancialPrivacyManager
import java.text.DateFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToMonthlyUpdate: () -> Unit,
    onNavigateToExpenses: () -> Unit,
    onNavigateToPortfolio: () -> Unit,
    onNavigateToRecurring: () -> Unit = {},
    onNavigateToCalculators: () -> Unit = {},
    onNavigateToWealthAi: () -> Unit = {},
    onNavigateToLoans: () -> Unit = {},
    onNavigateToInsurance: () -> Unit = {},
    onNavigateToCashbackRefund: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showMonthPicker by remember { mutableStateOf(false) }
    var showViewPicker by remember { mutableStateOf(false) }
    var showCreateFilterDialog by remember { mutableStateOf(false) }
    var filterToEdit by remember { mutableStateOf<PortfolioViewEntity?>(null) }
    var showManageFiltersDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showDownloadDialog by remember { mutableStateOf(false) }
    var showFireSettingsDialog by remember { mutableStateOf(false) }
    var isCategoriesExpanded by remember { mutableStateOf(false) }
    val isAmountsVisible = FinancialPrivacyManager.isAmountsVisible.value
    // Isolated Salary Privacy State - completely separate from upper eye & global privacy settings; defaults to false (hidden always)
    var isSalaryRevealed by rememberSaveable { mutableStateOf(false) }

    val activeSnapshotLabel = state.selectedSnapshotId?.let { id ->
        val parts = id.split("-")
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val y = parts.getOrNull(0) ?: ""
        "${DateFormatSymbols().shortMonths[m - 1]} $y"
    } ?: "Current Month"

    Scaffold(
        containerColor = HighDensityBg,
        topBar = {
            // High Density Header
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showProfileDialog = true }
                            .testTag("btn_open_profile_dashboard")
                    ) {
                        // User Avatar Chip / Logo with live initials or photo
                        UserAvatarView(
                            userProfile = state.userProfile,
                            size = 42.dp,
                            fontSize = 15.sp,
                            showEditBadge = false,
                            onClick = { showProfileDialog = true }
                        )

                        Column {
                            Text(
                                text = "${state.userProfile.name}'s Portfolio",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary,
                                maxLines = 1
                            )
                            Text(
                                text = "Snapshot: $activeSnapshotLabel",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Global Financial Privacy Eye Toggle (Masks/Reveals numbers)
                        GlobalPrivacyEyeButton(
                            size = 40.dp,
                            iconSize = 20.dp,
                            testTag = "btn_dashboard_privacy_eye"
                        )

                        // Download Snapshot Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { showDownloadDialog = true }
                                .testTag("btn_download_snapshot_topbar")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Download Snapshot",
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Quick Add Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { onNavigateToMonthlyUpdate() }
                                .testTag("btn_quick_update")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Monthly Update",
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (state.snapshots.isEmpty()) {
            // Empty State: No snapshots yet
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = HighDensityPrimaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = "Welcome to Wealth Tracker",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Track your monthly investments, mutual fund SIPs, true organic gains, and expense trends securely offline.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HighDensityTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Button(
                            onClick = onNavigateToMonthlyUpdate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_empty_start_snapshot"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Record First Monthly Snapshot", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { viewModel.restoreDefaultCategories() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ensure Default Categories Exist", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Horizontal High Density View & Month Filter Pills
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Month Pill Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showMonthPicker = true }
                                .testTag("btn_month_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = activeSnapshotLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = HighDensityTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Views Pills
                        state.portfolioViews.forEach { view ->
                            val isSelected = view.id == state.selectedView?.id
                            val pillBg = if (isSelected) HighDensityPrimaryContainer else Color.White
                            val pillTextColor = if (isSelected) HighDensityOnPrimaryContainer else HighDensityTextSecondary
                            val pillBorder = if (isSelected) HighDensityPillBorder else Color(0xFFE2E8F0)

                            Surface(
                                shape = CircleShape,
                                color = pillBg,
                                border = BorderStroke(1.dp, pillBorder),
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.selectPortfolioView(view) }
                                    .testTag("view_chip_${view.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(
                                        start = 14.dp,
                                        end = if (!view.isDefault) 8.dp else 14.dp,
                                        top = 6.dp,
                                        bottom = 6.dp
                                    )
                                ) {
                                    Text(
                                        text = view.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = pillTextColor
                                    )

                                    if (!view.isDefault) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Filter",
                                            tint = if (isSelected) HighDensityPrimary else HighDensityTextSecondary,
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable {
                                                    filterToEdit = view
                                                    showCreateFilterDialog = true
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // + Add Custom Filter Pill Button
                        Surface(
                            shape = CircleShape,
                            color = HighDensityPrimary.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    filterToEdit = null
                                    showCreateFilterDialog = true
                                }
                                .testTag("btn_add_custom_filter")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Filter",
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Custom Filter",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityPrimary
                                )
                            }
                        }

                        // Manage Filters Pill Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { showManageFiltersDialog = true }
                                .testTag("btn_manage_filters")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Manage Filters",
                                    tint = HighDensityTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Manage",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextSecondary
                                )
                            }
                        }
                    }
                }

                // Global Timeframe Selector Bar (1M, 3M, 6M, 12M, All)
                item {
                    val compLabel = state.portfolioSummary?.comparisonSnapshotLabel ?: state.selectedTimeRange.label
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "GROWTH TIMEFRAME",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 0.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = HighDensityPrimaryContainer
                                ) {
                                    Text(
                                        text = "vs $compLabel",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityOnPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Interactive Segmented Buttons: 1M, 3M, 6M, 12M, All
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF1F5F9))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                TimeRangeOption.values().forEach { option ->
                                    val isSelected = option == state.selectedTimeRange
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { viewModel.selectTimeRange(option) }
                                            .testTag("btn_time_range_${option.name}"),
                                        color = if (isSelected) HighDensityPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp),
                                        shadowElevation = if (isSelected) 1.dp else 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else HighDensityTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Primary Net Worth Hero Card (High Density Theme)
                item {
                    val summary = state.portfolioSummary
                    val compLabel = summary?.comparisonSnapshotLabel ?: state.selectedTimeRange.label
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = HighDensityPrimary,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Row: Net Worth Label & Growth Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = (state.selectedView?.name ?: "NET WORTH").uppercase(),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontSize = 12.sp,
                                        letterSpacing = 1.sp
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.85f)
                                )

                                summary?.growth?.let { growth ->
                                    val growthBg = if (growth.isPositive) Color(0xFF22C55E).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f)
                                    val growthTextColor = if (growth.isPositive) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                                    val pct = CurrencyFormatter.formatGrowthPercentage(growth.growthPercentage)

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = growthBg
                                    ) {
                                        Text(
                                            text = "$pct vs ${state.selectedTimeRange.label}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = growthTextColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            // Big Value Display
                            Text(
                                text = summary?.let { CurrencyFormatter.formatInr(it.totalValue) } ?: "₹0",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // Comparison amount vs benchmark with SIP & Capital breakdown
                            summary?.growth?.let { growth ->
                                if (growth.hasPreviousData) {
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = "${CurrencyFormatter.formatInr(growth.diffAmount, showSign = true)} vs $compLabel",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White.copy(alpha = 0.95f)
                                        )

                                        val netCap = summary.newCapitalInvested
                                        val orgGain = summary.marketOrganicGain
                                        val orgPctStr = CurrencyFormatter.formatGrowthPercentage(summary.marketOrganicPercentage)
                                        val orgSign = if (orgGain >= 0) "+" else ""

                                        if (netCap > 0) {
                                            Text(
                                                text = "⚡ Deducted ${CurrencyFormatter.formatInr(netCap)} Capital (Stocks/SIPs) • Market Return: $orgSign${CurrencyFormatter.formatInr(orgGain)} ($orgPctStr)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFFFDE68A),
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else {
                                            Text(
                                                text = "📈 Market Organic Return: $orgSign${CurrencyFormatter.formatInr(orgGain)} ($orgPctStr)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color(0xFFFDE68A),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        if (summary.netMonthlySavings > 0) {
                                            Text(
                                                text = "💼 Monthly Net Savings: +${CurrencyFormatter.formatInr(summary.netMonthlySavings)} (excluded from market returns)",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                                color = Color.White.copy(alpha = 0.85f),
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }

                            // Multi-Period Grid (3M, 6M, 12M)
                            val m3 = state.multiRangeGrowths[TimeRangeOption.THREE_MONTHS]
                            val m6 = state.multiRangeGrowths[TimeRangeOption.SIX_MONTHS]
                            val m12 = state.multiRangeGrowths[TimeRangeOption.TWELVE_MONTHS]

                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.15f),
                                thickness = 1.dp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "3 MONTH",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatGrowthPercentage(m3?.growthPercentage),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column {
                                    Text(
                                        text = "6 MONTH",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatGrowthPercentage(m6?.growthPercentage),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Column {
                                    Text(
                                        text = "12 MONTH",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatGrowthPercentage(m12?.growthPercentage),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // FIRE & Financial Independence Forecast Hero Card (At Top)
                state.fireForecast?.let { forecast ->
                    item {
                        FireForecastCard(
                            forecast = forecast,
                            onEditSettings = { showFireSettingsDialog = true }
                        )
                    }
                }

                // Financial Health Pulse: What's Going Good & What Needs Attention
                item {
                    FinancialPulseCard(
                        summary = state.portfolioSummary,
                        categoryGrowths = state.categoryGrowths,
                        fireForecast = state.fireForecast
                    )
                }

                // Liabilities & Protection Shield Section
                item {
                    DashboardDebtAndProtectionSection(
                        loanSummary = state.loanSummary,
                        insuranceSummary = state.insuranceSummary,
                        onNavigateToLoans = onNavigateToLoans,
                        onNavigateToInsurance = onNavigateToInsurance
                    )
                }

                // Cashback & Refunds Recovered Section
                item {
                    DashboardCashbackRefundSection(
                        cashbackSummary = state.cashbackSummary,
                        thisMonthAmount = state.thisMonthCashbackRefund,
                        onNavigateToCashbackRefund = onNavigateToCashbackRefund
                    )
                }

                // Assets Breakdown Section Header (MOVED UP)
                item {
                    val compTxt = state.portfolioSummary?.comparisonSnapshotLabel ?: state.selectedTimeRange.label
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Assets & Category Breakdown",
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Changes vs $compTxt",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary,
                                    maxLines = 1
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Show / Hide Button (Default: Hide)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCategoriesExpanded) HighDensityPrimaryContainer else Color.White,
                                    border = BorderStroke(1.dp, if (isCategoriesExpanded) HighDensityPillBorder else Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { isCategoriesExpanded = !isCategoriesExpanded }
                                        .testTag("btn_toggle_categories_expand")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isCategoriesExpanded) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (isCategoriesExpanded) HighDensityPrimary else HighDensityTextPrimary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Text(
                                            text = if (isCategoriesExpanded) "Hide" else "Show (${state.categoryGrowths.size})",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCategoriesExpanded) HighDensityPrimary else HighDensityTextPrimary,
                                            maxLines = 1
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = onNavigateToPortfolio,
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "Manage",
                                        color = HighDensityPrimary,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        maxLines = 1
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // If Categories Collapsed (Default), show concise summary card
                if (!isCategoriesExpanded) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { isCategoriesExpanded = true },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shadowElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
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
                                        color = HighDensityPrimaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                tint = HighDensityPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = "${state.categoryGrowths.size} Asset Categories",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Tap to view individual category increase & decrease",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = HighDensityTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Expand",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary,
                                        softWrap = false
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Expanded Category Cards with comprehensive increase/decrease details
                    items(state.categoryGrowths, key = { it.category.id }) { item ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onNavigateToPortfolio() },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 1.dp,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val categoryColor = IconMapper.parseColor(item.category.colorHex)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = categoryColor.copy(alpha = 0.15f),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = IconMapper.getIcon(item.category.iconName),
                                                    contentDescription = null,
                                                    tint = categoryColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = item.category.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = HighDensityTextPrimary
                                            )
                                            Text(
                                                text = "${item.assetCount} active holdings",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                color = HighDensityTextSecondary
                                            )
                                        }
                                    }

                                    // Current Value & Growth Badge
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = CurrencyFormatter.formatInr(item.currentValue),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )

                                        if (item.previousValue > 0 && item.growthPercentage != null) {
                                            val isPos = (item.growthPercentage ?: 0.0) >= 0
                                            val badgeColor = if (isPos) Emerald500 else Ruby500
                                            val arrow = if (isPos) "↑" else "↓"
                                            val diffSign = if (item.diffAmount >= 0) "+" else ""
                                            val pctStr = String.format("%.1f", Math.abs(item.growthPercentage ?: 0.0))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = "$arrow $diffSign${CurrencyFormatter.formatInrCompact(item.diffAmount)} ($pctStr%)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (item.previousValue == 0.0 && item.currentValue > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = HighDensityPrimaryContainer
                                            ) {
                                                Text(
                                                    text = "New Baseline",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = HighDensityOnPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Additional stats row: Benchmark comparison, Invested amount & ROI
                                HorizontalDivider(color = Color(0xFFF8FAFC))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val compLabel = state.portfolioSummary?.comparisonSnapshotLabel ?: state.selectedTimeRange.label
                                    if (item.previousValue > 0) {
                                        Text(
                                            text = "Prev: ${CurrencyFormatter.formatInr(item.previousValue)} ($compLabel)",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    } else {
                                        Text(
                                            text = "First recorded snapshot",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }

                                    val catInvested = item.investedAmount
                                    val catGain = item.currentValue - catInvested
                                    val catRetPct = if (catInvested > 0) (catGain / catInvested) * 100.0 else null
                                    if (catInvested > 0) {
                                        Text(
                                            text = "Inv: ${CurrencyFormatter.formatInrCompact(catInvested)} • Gain: ${CurrencyFormatter.formatGrowthPercentage(catRetPct)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (catGain >= 0) Color(0xFF16A34A) else Color(0xFFDC2626)
                                        )
                                    }
                                }

                                // Organic Gain info if new capital or SIPs are present for this category
                                if (item.sipInflows > 0 && item.previousValue > 0) {
                                    val orgSign = if (item.organicDiff >= 0) "+" else ""
                                    val orgPctStr = CurrencyFormatter.formatGrowthPercentage(item.organicGrowthPercentage)
                                    Text(
                                        text = "⚡ Deducted ${CurrencyFormatter.formatInr(item.sipInflows)} Capital Inflow • Organic: $orgSign${CurrencyFormatter.formatInr(item.organicDiff)} ($orgPctStr)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = Color(0xFF7C3AED),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Investment & Profit/Loss (P&L) Overview Card
                item {
                    val summary = state.portfolioSummary
                    val totalInvested = summary?.totalInvested ?: 0.0
                    val totalValue = summary?.totalValue ?: 0.0
                    val totalProfit = totalValue - totalInvested
                    val returnPct = if (totalInvested > 0) (totalProfit / totalInvested) * 100.0 else null
                    val isProfit = totalProfit >= 0

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isProfit) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(if (isProfit) "📈" else "📉", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "INVESTMENT RETURNS (P&L)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextSecondary
                                        )
                                        Text(
                                            text = "Total Gain / Profit across holdings",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary.copy(alpha = 0.8f)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isProfit) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${CurrencyFormatter.formatGrowthPercentage(returnPct)} ROI",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProfit) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Invested",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatInr(totalInvested),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Profit / Gain",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = "${if (isProfit) "+" else ""}${CurrencyFormatter.formatInr(totalProfit)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProfit) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }

                            // Visual Return ratio bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFE2E8F0))
                                ) {
                                    val ratio = if (totalValue > 0 && totalInvested > 0) {
                                        (totalInvested / totalValue).toFloat().coerceIn(0.1f, 1f)
                                    } else 0.5f

                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .weight(ratio)
                                                .fillMaxHeight()
                                                .background(HighDensityPrimary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight((1f - ratio).coerceAtLeast(0.01f))
                                                .fillMaxHeight()
                                                .background(if (isProfit) Color(0xFF22C55E) else Color(0xFFEF4444))
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "● Invested Capital (${if (totalValue > 0) String.format("%.0f", (totalInvested / totalValue) * 100) else "0"}%)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = HighDensityPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "● Net Profit (${if (totalValue > 0) String.format("%.0f", (totalProfit / totalValue) * 100) else "0"}%)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = if (isProfit) Color(0xFF16A34A) else Color(0xFFDC2626),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Portfolio Growth Line Chart Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ShowChart,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Portfolio Growth Trend",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            PortfolioLineChart(
                                dataPoints = state.historicalPoints,
                                lineColor = HighDensityPrimary,
                                accentColor = Color(0xFF8B5CF6),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Asset Allocation Donut Chart Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PieChart,
                                    contentDescription = null,
                                    tint = HighDensitySecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Asset Allocation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            AllocationDonutChart(
                                slices = state.allocations,
                                totalAmount = state.portfolioSummary?.totalValue ?: 0.0,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Fastest Growing Categories Bar Chart
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Leaderboard,
                                        contentDescription = null,
                                        tint = Amber500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Category Growth Rate",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = HighDensityPrimaryContainer
                                ) {
                                    Text(
                                        text = state.selectedTimeRange.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityOnPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            CategoryGrowthBarChart(
                                growthItems = state.categoryGrowths,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Recurring Cash Flows & SIP Inflows Summary Card
                item {
                    val recSummary = state.recurringSummary
                    val totalMonthlySip = recSummary.totalActiveSipAmount
                    val totalIncome = recSummary.totalActiveSalary + recSummary.totalActiveMealCoupon

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigateToRecurring() },
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFF5F3FF),
                        border = BorderStroke(1.dp, Color(0xFFDDD6FE)),
                        shadowElevation = 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEDE9FE),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("⚡", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "RECURRING SIPS & INFLOWS",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF7C3AED)
                                        )
                                        Text(
                                            text = "Automatically subtracted for true organic growth",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFEDE9FE)
                                ) {
                                    Text(
                                        text = "${recSummary.items.count { it.isActive }} Active Flows",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF6D28D9),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFEDE9FE))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Monthly Mutual Fund SIPs",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatInr(totalMonthlySip),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Salary & Coupons Inflow",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HighDensityTextSecondary
                                        )
                                        IconButton(
                                            onClick = { isSalaryRevealed = !isSalaryRevealed },
                                            modifier = Modifier
                                                .size(22.dp)
                                                .testTag("btn_dashboard_salary_eye")
                                        ) {
                                            Icon(
                                                imageVector = if (isSalaryRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (isSalaryRevealed) "Hide Salary Inflow" else "Show Salary Inflow",
                                                tint = if (isSalaryRevealed) Color(0xFF16A34A) else HighDensityTextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isSalaryRevealed) CurrencyFormatter.formatInr(totalIncome, ignorePrivacy = true) else "₹ ••••••",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Tap to configure SIP start/end dates & amounts",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color(0xFF7C3AED),
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = Color(0xFF7C3AED),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Monthly Expenses & Outflow Overview Section (MOVED TO BOTTOM / LAST)
                item {
                    val expSummary = state.expenseSummary
                    val expTotal = expSummary?.totalExpense ?: 0.0
                    val expGrowth = expSummary?.growth
                    val recSummary = state.recurringSummary
                    val totalInflow = recSummary.totalActiveInflows
                    val netSavings = totalInflow - expTotal
                    val savingsRate = if (totalInflow > 0) ((netSavings / totalInflow) * 100.0).coerceIn(-100.0, 100.0) else null

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onNavigateToExpenses() },
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 1.dp,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Section Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Ruby500.copy(alpha = 0.12f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("💸", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "MONTHLY EXPENSES & BURN",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Ruby500
                                        )
                                        Text(
                                            text = "Tracked separately from net worth",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Full Analytics",
                                        color = HighDensityPrimary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Comparison Period Selector (1M, 3M, 6M, 12M)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Compare against:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextSecondary
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ExpenseComparisonRange.values().forEach { rangeOption ->
                                        val isSelected = state.selectedExpenseRange == rangeOption
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) Ruby500 else Color(0xFFF8FAFC),
                                            border = BorderStroke(1.dp, if (isSelected) Ruby500 else Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { viewModel.setExpenseRange(rangeOption) }
                                                .testTag("btn_expense_range_${rangeOption.name}")
                                        ) {
                                            Text(
                                                text = rangeOption.label,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) Color.White else HighDensityTextSecondary,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Main Total & Growth Badge for Selected Benchmark
                            val benchmarkGrowth = expSummary?.benchmarkGrowth
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Spending",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatInr(expTotal),
                                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
                                        fontWeight = FontWeight.Black,
                                        color = HighDensityTextPrimary
                                    )
                                }

                                if (benchmarkGrowth != null && benchmarkGrowth.hasPreviousData) {
                                    val isReduction = benchmarkGrowth.diffAmount <= 0
                                    val diffPct = String.format("%.1f", Math.abs(benchmarkGrowth.growthPercentage ?: 0.0))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isReduction) Emerald500.copy(alpha = 0.12f) else Ruby500.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = if (isReduction) "↓ $diffPct%" else "↑ +$diffPct%",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isReduction) Emerald500 else Ruby500
                                            )
                                            Text(
                                                text = expSummary.benchmarkLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                color = if (isReduction) Emerald500 else Ruby500
                                            )
                                        }
                                    }
                                }
                            }

                            // Benchmark details helper
                            if (expSummary != null && expSummary.benchmarkExpense > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Benchmark (${state.selectedExpenseRange.label}): ${CurrencyFormatter.formatInr(expSummary.benchmarkExpense)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                        benchmarkGrowth?.diffAmount?.let { diff ->
                                            val isSaved = diff <= 0
                                            Text(
                                                text = "${if (diff > 0) "+" else "-"}${CurrencyFormatter.formatInr(Math.abs(diff))}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSaved) Emerald500 else Ruby500
                                            )
                                        }
                                    }
                                }
                            }

                            // Cash Flow Comparison: Inflow vs Outflow vs Net Savings
                            if (totalInflow > 0) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Monthly Inflows", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                            Text(CurrencyFormatter.formatInr(totalInflow), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                        }
                                        Column {
                                            Text("Net Savings", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                            Text(CurrencyFormatter.formatInr(netSavings, showSign = true), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (netSavings >= 0) Color(0xFF16A34A) else Ruby500)
                                        }
                                        if (savingsRate != null) {
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Savings Rate", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                                Text("${String.format("%.0f", savingsRate)}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (savingsRate >= 20) Color(0xFF16A34A) else Amber500)
                                            }
                                        }
                                    }
                                }
                            }

                            // Top Expense Categories Preview (up to 4 categories)
                            if (state.expenseBreakdowns.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Top Expense Categories",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextSecondary
                                        )
                                        Text(
                                            text = "vs ${state.selectedExpenseRange.label}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = Ruby500,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    state.expenseBreakdowns.take(4).forEach { catBreakdown ->
                                        val cColor = IconMapper.parseColor(catBreakdown.category.colorHex)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = cColor.copy(alpha = 0.15f),
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            imageVector = IconMapper.getIcon(catBreakdown.category.iconName),
                                                            contentDescription = null,
                                                            tint = cColor,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = catBreakdown.category.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = HighDensityTextPrimary,
                                                    maxLines = 1
                                                )
                                            }

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = CurrencyFormatter.formatInr(catBreakdown.totalAmount),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HighDensityTextPrimary
                                                )
                                                Text(
                                                    text = "(${String.format("%.0f", catBreakdown.percentage)}%)",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = HighDensityTextSecondary
                                                )
                                                if (catBreakdown.comparisonAmount > 0 && catBreakdown.growthPercentage != null) {
                                                    val isInc = (catBreakdown.growthPercentage ?: 0.0) > 0
                                                    val pctStr = String.format("%.0f", Math.abs(catBreakdown.growthPercentage ?: 0.0))
                                                    Text(
                                                        text = if (isInc) "↑ +$pctStr%" else "↓ -$pctStr%",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isInc) Ruby500 else Emerald500
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

    // Month Picker Modal Sheet
    if (showMonthPicker) {
        MonthPickerBottomSheet(
            snapshots = state.snapshots,
            selectedSnapshotId = state.selectedSnapshotId,
            onSnapshotSelected = { viewModel.selectSnapshot(it) },
            onCreateSnapshot = { y, m -> viewModel.createNewSnapshot(y, m) },
            onDismiss = { showMonthPicker = false }
        )
    }

    // Create / Edit Custom Filter Dialog
    if (showCreateFilterDialog) {
        CreateEditFilterDialog(
            initialFilter = filterToEdit,
            allCategories = state.categories,
            allAssets = state.assets,
            onDismiss = {
                showCreateFilterDialog = false
                filterToEdit = null
            },
            onSaveFilter = { name, categoryIds ->
                val currentFilter = filterToEdit
                if (currentFilter != null) {
                    viewModel.updateCustomPortfolioView(currentFilter.id, name, categoryIds)
                } else {
                    viewModel.createCustomPortfolioView(name, categoryIds)
                }
                showCreateFilterDialog = false
                filterToEdit = null
            },
            onDeleteFilter = { view ->
                viewModel.deleteCustomPortfolioView(view)
                showCreateFilterDialog = false
                filterToEdit = null
            }
        )
    }

    // Manage Filters Dialog
    if (showManageFiltersDialog) {
        ManageFiltersDialog(
            views = state.portfolioViews,
            selectedView = state.selectedView,
            allCategories = state.categories,
            onDismiss = { showManageFiltersDialog = false },
            onSelectView = { viewModel.selectPortfolioView(it) },
            onOpenCreateFilter = {
                filterToEdit = null
                showCreateFilterDialog = true
            },
            onEditFilter = { view ->
                filterToEdit = view
                showCreateFilterDialog = true
            },
            onDeleteFilter = { view ->
                viewModel.deleteCustomPortfolioView(view)
            }
        )
    }

    // User Profile Dialog (Name, Photo, Initials Logo, Net Worth Goal)
    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = state.userProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, email, tagline, targetNetWorth, colorIndex, dob ->
                viewModel.updateUserProfile(name, email, tagline, targetNetWorth, colorIndex, dob)
            },
            onImageSelected = { uri ->
                viewModel.saveProfileImage(uri)
            },
            onRemoveImage = {
                viewModel.removeProfileImage()
            }
        )
    }

    // Download / Export Snapshot Dialog
    if (showDownloadDialog && state.selectedSnapshotId != null) {
        SnapshotDownloadDialog(
            snapshotId = state.selectedSnapshotId!!,
            snapshotLabel = activeSnapshotLabel,
            totalPortfolioValue = state.portfolioSummary?.totalValue ?: 0.0,
            values = state.currentValues,
            assets = state.assets,
            categories = state.categories,
            sips = state.recurringFlows,
            onDismiss = { showDownloadDialog = false }
        )
    }

    // FIRE Assumptions & Forecast Editor Dialog
    if (showFireSettingsDialog) {
        EditFireSettingsDialog(
            currentSettings = state.fireSettings,
            onDismiss = { showFireSettingsDialog = false },
            onSave = { updatedSettings ->
                viewModel.updateFireSettings(updatedSettings)
                showFireSettingsDialog = false
            }
        )
    }
}

@Composable
private fun DashboardDebtAndProtectionSection(
    loanSummary: LoanPortfolioSummary,
    insuranceSummary: InsuranceShieldSummary,
    onNavigateToLoans: () -> Unit,
    onNavigateToInsurance: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Liabilities & Protection Shield",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                fontWeight = FontWeight.Bold,
                color = HighDensityTextPrimary
            )
            Text(
                text = "Net Worth Health",
                style = MaterialTheme.typography.labelSmall,
                color = HighDensityTextSecondary
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Loans & Liabilities Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToLoans() }
                    .testTag("card_dashboard_loans"),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HighDensityBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (loanSummary.activeLoansCount > 0) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (loanSummary.activeLoansCount > 0) HighDensityExpenseRed else HighDensityIncomeGreen,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (loanSummary.activeLoansCount > 0) "${loanSummary.activeLoansCount} Active" else "Debt-Free",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (loanSummary.activeLoansCount > 0) HighDensityExpenseRed else HighDensityIncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Total Debt",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )

                    Text(
                        text = CurrencyFormatter.formatInrCompact(loanSummary.totalOutstanding),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (loanSummary.totalOutstanding > 0) HighDensityExpenseRed else HighDensityTextPrimary
                    )

                    Text(
                        text = if (loanSummary.totalMonthlyEmi > 0) "EMI: ${CurrencyFormatter.formatInrCompact(loanSummary.totalMonthlyEmi)}/mo" else "Zero EMI",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = HighDensityTextSecondary
                    )
                }
            }

            // Insurance & Protection Shield Card
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToInsurance() }
                    .testTag("card_dashboard_insurance"),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, HighDensityBorder),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (insuranceSummary.activePoliciesCount > 0) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (insuranceSummary.activePoliciesCount > 0) HighDensityIncomeGreen else HighDensityTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = if (insuranceSummary.policiesExpiringSoonCount > 0) {
                                "Due Soon"
                            } else if (insuranceSummary.activePoliciesCount > 0) {
                                "Active"
                            } else {
                                "No Shield"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (insuranceSummary.policiesExpiringSoonCount > 0) {
                                Color(0xFFB45309)
                            } else if (insuranceSummary.activePoliciesCount > 0) {
                                HighDensityIncomeGreen
                            } else {
                                HighDensityTextSecondary
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Life + Health Cover",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )

                    val totalCover = insuranceSummary.totalLifeCover + insuranceSummary.totalHealthCover
                    Text(
                        text = if (totalCover > 0) CurrencyFormatter.formatInrCompact(totalCover) else "Add Cover",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalCover > 0) HighDensityIncomeGreen else HighDensityPrimary
                    )

                    Text(
                        text = if (insuranceSummary.activePoliciesCount > 0) "${insuranceSummary.activePoliciesCount} Policies Tracked" else "Tap to add cover",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = HighDensityTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardCashbackRefundSection(
    cashbackSummary: com.example.data.repository.CashbackRefundSummary,
    thisMonthAmount: Double,
    onNavigateToCashbackRefund: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onNavigateToCashbackRefund() },
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "CASHBACK & REFUNDS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                        Text(
                            text = "Recovered from shopping & returns",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "View All",
                        color = HighDensityPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = HighDensityPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Recovered & Saved",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(cashbackSummary.totalRecoveredAndSaved),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "This Month: ",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF047857)
                        )
                        Text(
                            text = CurrencyFormatter.formatInrCompact(thisMonthAmount),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cashback Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "💰 Cashback",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.formatInrCompact(cashbackSummary.totalCashbackEarned),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }

                // Refunds Pill
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "🔄 Refunds",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                        Text(
                            text = CurrencyFormatter.formatInrCompact(cashbackSummary.totalRefundsReceived),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2563EB)
                        )
                    }
                }
            }
        }
    }
}


