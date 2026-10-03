package com.example.ui.screens.recurring

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.IconMapper
import com.example.ui.theme.*
import com.example.util.FinancialPrivacyManager
import java.text.DateFormatSymbols
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringCashFlowScreen(
    viewModel: RecurringCashFlowViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedFlowForEdit by remember { mutableStateOf<RecurringCashFlowEntity?>(null) }
    var flowToDelete by remember { mutableStateOf<RecurringCashFlowEntity?>(null) }
    val isAmountsVisible = FinancialPrivacyManager.isAmountsVisible.value
    // Isolated Salary Privacy State - defaults to false (hidden always), unaffected by upper eye
    var isSalaryRevealed by rememberSaveable { mutableStateOf(false) }

    val filteredItems = remember(uiState.items, uiState.selectedFilterType) {
        val filter = uiState.selectedFilterType
        if (filter == null) {
            uiState.items
        } else {
            uiState.items.filter { it.type == filter }
        }
    }

    val categoriesById = remember(uiState.categories) {
        uiState.categories.associateBy { it.id }
    }
    val assetsById = remember(uiState.assets) {
        uiState.assets.associateBy { it.id }
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
                        .padding(start = 12.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("btn_back_recurring")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HighDensityTextPrimary
                            )
                        }
                        Column {
                            Text(
                                text = "Recurring & SIPs",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Manage monthly SIPs, salary & cash inflows",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlobalPrivacyEyeButton(
                            size = 36.dp,
                            iconSize = 18.dp,
                            testTag = "btn_recurring_privacy_eye"
                        )

                        Button(
                            onClick = {
                                selectedFlowForEdit = null
                                showAddEditDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("btn_add_recurring")
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Add SIP / Inflow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // KPI Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryMetricCard(
                        title = "TOTAL OUTFLOWS (SIP+RD)",
                        amount = uiState.summary.totalActiveOutflows,
                        icon = Icons.Default.Repeat,
                        color = Color(0xFF8B5CF6),
                        bgColor = Color(0xFFF5F3FF),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryMetricCard(
                        title = "TOTAL INFLOWS & PAYOUTS",
                        amount = uiState.summary.totalActiveInflows,
                        icon = Icons.Default.Payments,
                        color = Color(0xFF10B981),
                        bgColor = Color(0xFFECFDF5),
                        isSensitive = true,
                        isRevealed = isSalaryRevealed,
                        onToggleReveal = { isSalaryRevealed = !isSalaryRevealed },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Sub-KPI row for Net Surplus & Payouts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("BOND/FD INTEREST INFLOW", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text(CurrencyFormatter.formatInr(uiState.summary.totalActiveInterestPayout) + " / mo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("NET CASH SURPLUS / MO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                            Text(CurrencyFormatter.formatInr(uiState.summary.netRecurringSurplus) + " / mo", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = if (uiState.summary.netRecurringSurplus >= 0) Color(0xFF16A34A) else Color(0xFFDC2626))
                        }
                    }
                }
            }

            // Explanatory Banner for MoM Comparison & SIP Subtraction
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automated Cash Flow & Organic Return Engine",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E40AF)
                            )
                            Text(
                                text = "Tracks recurring SIPs, RDs (monthly/quarterly/annual), and Bond/FD interest payouts. Historical comparisons auto-deduct injected capital for genuine appreciation.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color(0xFF1E3A8A)
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.selectedFilterType == null,
                        onClick = { viewModel.setFilterType(null) },
                        label = { Text("All (${uiState.items.size})", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.selectedFilterType == "SIP",
                        onClick = { viewModel.setFilterType("SIP") },
                        label = { Text("Mutual Fund SIPs", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.selectedFilterType == "RD",
                        onClick = { viewModel.setFilterType("RD") },
                        label = { Text("Recurring Deposits (RD)", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.selectedFilterType == "INTEREST_PAYOUT",
                        onClick = { viewModel.setFilterType("INTEREST_PAYOUT") },
                        label = { Text("Bond / FD Payouts", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.selectedFilterType == "SALARY",
                        onClick = { viewModel.setFilterType("SALARY") },
                        label = { Text("Salary", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.selectedFilterType == "MEAL_COUPON",
                        onClick = { viewModel.setFilterType("MEAL_COUPON") },
                        label = { Text("Meal Card / Sodexo", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = uiState.selectedFilterType == "PPF_EPF_CONTRIBUTION",
                        onClick = { viewModel.setFilterType("PPF_EPF_CONTRIBUTION") },
                        label = { Text("PPF & NPS", fontSize = 12.sp) }
                    )
                }
            }

            // List Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SCHEDULED RECURRING INFLOWS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "${filteredItems.size} items",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EventRepeat,
                                contentDescription = null,
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No recurring inflows in this view",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HighDensityTextSecondary
                            )
                            Button(
                                onClick = {
                                    selectedFlowForEdit = null
                                    showAddEditDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                            ) {
                                Text("Add First SIP / Inflow", fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { flow ->
                    val category = categoriesById[flow.categoryId]
                    val asset = flow.assetId?.let { assetsById[it] }

                    RecurringFlowCard(
                        flow = flow,
                        category = category,
                        asset = asset,
                        isSalaryRevealed = isSalaryRevealed,
                        onToggleSalaryReveal = { isSalaryRevealed = !isSalaryRevealed },
                        onToggleActive = { viewModel.toggleFlowActive(flow) },
                        onEdit = {
                            selectedFlowForEdit = flow
                            showAddEditDialog = true
                        },
                        onDelete = {
                            flowToDelete = flow
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showAddEditDialog) {
        RecurringFlowDialog(
            flow = selectedFlowForEdit,
            categories = uiState.categories,
            assets = uiState.assets,
            onDismiss = { showAddEditDialog = false },
            onSave = { id, name, type, categoryId, assetId, amount, startMonth, endMonth, day, isActive, notes, freq, instAmt, rate, prinPercent ->
                viewModel.saveRecurringCashFlow(
                    id = id,
                    name = name,
                    type = type,
                    categoryId = categoryId,
                    assetId = assetId,
                    monthlyAmount = amount,
                    startMonth = startMonth,
                    endMonth = endMonth,
                    dayOfMonth = day,
                    isActive = isActive,
                    notes = notes,
                    frequency = freq,
                    installmentAmount = instAmt,
                    interestRate = rate,
                    principalPayoutPercent = prinPercent
                )
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (flowToDelete != null) {
        val flow = flowToDelete!!
        AlertDialog(
            onDismissRequest = { flowToDelete = null },
            title = { Text("Delete Recurring Item?") },
            text = { Text("Are you sure you want to delete '${flow.name}'? This will not affect historical snapshots, but future comparisons will no longer include this schedule.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteFlow(flow)
                        flowToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { flowToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricCard(
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    isSensitive: Boolean = false,
    isRevealed: Boolean = true,
    onToggleReveal: (() -> Unit)? = null
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSensitive && onToggleReveal != null) {
                        IconButton(
                            onClick = onToggleReveal,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (isRevealed) "Hide Salary" else "Show Salary",
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = bgColor,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Text(
                text = if (isSensitive && !isRevealed) "₹ ••••••" else CurrencyFormatter.formatInr(amount),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                fontWeight = FontWeight.ExtraBold,
                color = HighDensityTextPrimary
            )
        }
    }
}

@Composable
private fun RecurringFlowCard(
    flow: RecurringCashFlowEntity,
    category: CategoryEntity?,
    asset: AssetEntity?,
    isSalaryRevealed: Boolean,
    onToggleSalaryReveal: () -> Unit,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val typeBadgeColor = when (flow.type) {
        "SIP" -> Color(0xFF8B5CF6)
        "RD" -> Color(0xFF0891B2)
        "INTEREST_PAYOUT" -> Color(0xFF2563EB)
        "PRINCIPAL_PAYOUT" -> Color(0xFF059669)
        "SALARY" -> Color(0xFF10B981)
        "MEAL_COUPON" -> Color(0xFFF97316)
        "PPF_EPF_CONTRIBUTION" -> Color(0xFF0D9488)
        else -> Color(0xFF6366F1)
    }

    val typeLabel = when (flow.type) {
        "SIP" -> "MUTUAL FUND SIP"
        "RD" -> "RECURRING DEPOSIT (RD)"
        "INTEREST_PAYOUT" -> "BOND / FD INTEREST"
        "PRINCIPAL_PAYOUT" -> "PRINCIPAL RETURN"
        "SALARY" -> "SALARY CREDIT"
        "MEAL_COUPON" -> "MEAL CARD"
        "PPF_EPF_CONTRIBUTION" -> "PPF / NPS"
        else -> "RECURRING"
    }

    val isSalary = flow.type == "SALARY"
    val isPeriodicFlow = flow.type in listOf("RD", "INTEREST_PAYOUT", "PRINCIPAL_PAYOUT")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .testTag("card_flow_${flow.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (flow.isActive) Color.White else Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, if (flow.isActive) Color(0xFFE2E8F0) else Color(0xFFCBD5E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (flow.isActive) 1.dp else 0.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = typeBadgeColor.copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = typeBadgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (category != null) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }

                    if (flow.frequency != "MONTHLY") {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = flow.frequency.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Switch(
                    checked = flow.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.size(height = 24.dp, width = 36.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HighDensityPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCBD5E1)
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = flow.name,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (flow.isActive) HighDensityTextPrimary else HighDensityTextSecondary
                    )
                    if (asset != null) {
                        Text(
                            text = "Linked Asset: ${asset.name}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                    if (flow.interestRate > 0) {
                        Text(
                            text = "Rate: ${flow.interestRate}% p.a." + if (flow.principalPayoutPercent > 0) " • ${flow.principalPayoutPercent}% Principal Return" else "",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF0284C7),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isSalary) {
                            IconButton(
                                onClick = onToggleSalaryReveal,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSalaryRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (isSalaryRevealed) "Hide Salary Amount" else "Show Salary Amount",
                                    tint = HighDensityTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        val primaryAmountStr = if (isSalary && !isSalaryRevealed) {
                            "₹ ••••••"
                        } else if (isPeriodicFlow && flow.installmentAmount > 0 && flow.frequency != "MONTHLY") {
                            "${CurrencyFormatter.formatInr(flow.installmentAmount)} / ${flow.frequency.take(3).lowercase()}"
                        } else {
                            "${CurrencyFormatter.formatInr(flow.monthlyAmount)} / mo"
                        }

                        Text(
                            text = primaryAmountStr,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = if (flow.isActive) HighDensityPrimary else HighDensityTextSecondary
                        )
                    }

                    if (isPeriodicFlow && flow.frequency != "MONTHLY" && flow.monthlyAmount > 0) {
                        Text(
                            text = "≈ ${CurrencyFormatter.formatInr(flow.monthlyAmount)} / mo",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        val endStr = flow.endMonth?.let { "to $it" } ?: "Ongoing"
                        Text(
                            text = "${flow.startMonth} $endStr",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        Text(
                            text = "${flow.dayOfMonth}th of month",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = HighDensityTextSecondary, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecurringFlowDialog(
    flow: RecurringCashFlowEntity?,
    categories: List<CategoryEntity>,
    assets: List<AssetEntity>,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        name: String,
        type: String,
        categoryId: String,
        assetId: Long?,
        amount: Double,
        startMonth: String,
        endMonth: String?,
        day: Int,
        isActive: Boolean,
        notes: String,
        frequency: String,
        installmentAmount: Double,
        interestRate: Double,
        principalPayoutPercent: Double
    ) -> Unit
) {
    val isEdit = flow != null
    var name by remember { mutableStateOf(flow?.name ?: "") }
    var type by remember { mutableStateOf(flow?.type ?: "SIP") }
    var categoryId by remember { mutableStateOf(flow?.categoryId ?: categories.firstOrNull()?.id ?: "mutual_funds") }
    var selectedAssetId by remember { mutableStateOf(flow?.assetId) }
    var frequency by remember { mutableStateOf(flow?.frequency ?: "MONTHLY") }
    
    var amountText by remember {
        val initialVal = if (flow != null) {
            if (flow.installmentAmount > 0) flow.installmentAmount.toInt().toString()
            else if (flow.monthlyAmount > 0) flow.monthlyAmount.toInt().toString()
            else ""
        } else ""
        mutableStateOf(initialVal)
    }

    var interestRateText by remember {
        mutableStateOf(if (flow != null && flow.interestRate > 0) flow.interestRate.toString() else "")
    }
    var principalPayoutPercentText by remember {
        mutableStateOf(if (flow != null && flow.principalPayoutPercent > 0) flow.principalPayoutPercent.toString() else "")
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    val defaultStart = "%04d-%02d".format(currentYear, currentMonth)

    var startMonth by remember { mutableStateOf(flow?.startMonth ?: "2025-01") }
    var isOngoing by remember { mutableStateOf(flow?.endMonth == null) }
    var endMonth by remember { mutableStateOf(flow?.endMonth ?: "%04d-12".format(currentYear)) }
    var dayOfMonth by remember { mutableStateOf(flow?.dayOfMonth ?: 5) }
    var isActive by remember { mutableStateOf(flow?.isActive ?: true) }
    var notes by remember { mutableStateOf(flow?.notes ?: "") }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var assetDropdownExpanded by remember { mutableStateOf(false) }

    val flowTypes = listOf(
        "SIP" to "Mutual Fund SIP",
        "RD" to "Recurring Deposit (RD)",
        "INTEREST_PAYOUT" to "Bond / FD Interest Payout",
        "PRINCIPAL_PAYOUT" to "Principal Return / Amortization",
        "SALARY" to "Salary / Income",
        "MEAL_COUPON" to "Meal Card / Sodexo",
        "PPF_EPF_CONTRIBUTION" to "PPF / NPS Contribution",
        "RECURRING_INVESTMENT" to "Other Recurring Investment",
        "OTHER" to "Other Inflow"
    )

    val isRdOrPayout = type in listOf("RD", "INTEREST_PAYOUT", "PRINCIPAL_PAYOUT")

    // Live monthly normalization calculation
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val normalizedMonthly = remember(parsedAmount, frequency) {
        when (frequency) {
            "QUARTERLY" -> parsedAmount / 3.0
            "HALF_YEARLY" -> parsedAmount / 6.0
            "ANNUALLY" -> parsedAmount / 12.0
            else -> parsedAmount
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isEdit) "Edit Recurring Flow" else "Add New Recurring / Inflow",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Schedule / Plan Name") },
                    placeholder = { Text("e.g. HDFC 3-Yr RD or RBI Floating Rate Bond") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                // Type Dropdown
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = !typeDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = flowTypes.find { it.first == type }?.second ?: type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Flow Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        flowTypes.forEach { (typeKey, typeName) ->
                            DropdownMenuItem(
                                text = { Text(typeName) },
                                onClick = {
                                    type = typeKey
                                    // auto suggest category
                                    when (typeKey) {
                                        "SIP" -> categoryId = "mutual_funds"
                                        "RD" -> categoryId = "fixed_deposits"
                                        "INTEREST_PAYOUT" -> categoryId = "bonds"
                                        "PRINCIPAL_PAYOUT" -> categoryId = "bonds"
                                        "SALARY" -> categoryId = "bank_balance"
                                        "MEAL_COUPON" -> categoryId = "meal_card"
                                        "PPF_EPF_CONTRIBUTION" -> categoryId = "nps"
                                    }
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Frequency Selector for RD / Payouts
                if (isRdOrPayout) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Frequency", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B), fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("MONTHLY" to "Monthly", "QUARTERLY" to "Quarterly", "ANNUALLY" to "Annually").forEach { (fKey, fLabel) ->
                                FilterChip(
                                    selected = frequency == fKey,
                                    onClick = { frequency = fKey },
                                    label = { Text(fLabel, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // Amount Field
                val amountLabel = if (isRdOrPayout && frequency != "MONTHLY") {
                    "${frequency.lowercase().replaceFirstChar { it.uppercase() }} Installment / Payout (₹)"
                } else {
                    "Monthly Amount (₹)"
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                    label = { Text(amountLabel) },
                    placeholder = { Text(if (frequency == "QUARTERLY") "e.g. 15000" else "e.g. 5000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) }
                )

                // Live normalization preview badge
                if (isRdOrPayout && frequency != "MONTHLY" && parsedAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Equivalent Flow:", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E40AF))
                            Text("≈ ${CurrencyFormatter.formatInr(normalizedMonthly)} / mo", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1D4ED8))
                        }
                    }
                }

                // Optional Interest Rate & Principal Payout % fields
                if (isRdOrPayout) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = interestRateText,
                            onValueChange = { interestRateText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                            label = { Text("Rate (% p.a.)") },
                            placeholder = { Text("e.g. 7.5") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                        if (type == "PRINCIPAL_PAYOUT" || type == "INTEREST_PAYOUT") {
                            OutlinedTextField(
                                value = principalPayoutPercentText,
                                onValueChange = { principalPayoutPercentText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                                label = { Text("Principal %") },
                                placeholder = { Text("e.g. 10") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                        }
                    }
                }

                // Category Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    val currentCat = categories.find { it.id == categoryId }
                    OutlinedTextField(
                        value = currentCat?.name ?: categoryId,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.name) },
                                onClick = {
                                    categoryId = cat.id
                                    selectedAssetId = null
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date Range Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startMonth,
                        onValueChange = { startMonth = it },
                        label = { Text("Start (YYYY-MM)") },
                        placeholder = { Text("2025-01") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (!isOngoing) {
                        OutlinedTextField(
                            value = endMonth,
                            onValueChange = { endMonth = it },
                            label = { Text("End (YYYY-MM)") },
                            placeholder = { Text("2026-12") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isOngoing = !isOngoing }
                ) {
                    Checkbox(
                        checked = isOngoing,
                        onCheckedChange = { isOngoing = it }
                    )
                    Text("Ongoing (No End Date)", style = MaterialTheme.typography.bodyMedium, color = HighDensityTextPrimary)
                }

                // Actions
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val cleanName = if (name.isBlank()) "Recurring Flow" else name
                            val rateVal = interestRateText.toDoubleOrNull() ?: 0.0
                            val prinVal = principalPayoutPercentText.toDoubleOrNull() ?: 0.0
                            onSave(
                                flow?.id ?: 0L,
                                cleanName,
                                type,
                                categoryId,
                                selectedAssetId,
                                normalizedMonthly,
                                startMonth.trim(),
                                if (isOngoing) null else endMonth.trim(),
                                dayOfMonth,
                                isActive,
                                notes,
                                frequency,
                                parsedAmount,
                                rateVal,
                                prinVal
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isEdit) "Save Changes" else "Add Schedule", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
