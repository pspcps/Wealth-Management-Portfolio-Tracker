package com.example.ui.screens.cashback

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CashbackRefundEntity
import com.example.ui.components.DateFieldWithPicker
import com.example.ui.components.MathAmountTextField
import com.example.util.MathExpressionEvaluator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashbackRefundScreen(
    viewModel: CashbackRefundViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(uiState.messageToast) {
        uiState.messageToast?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Text(
                            text = "Cashback & Refunds",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${uiState.filteredItems.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        // Blue Button for Refund
                        Button(
                            onClick = { viewModel.openAdd("REFUND") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("add_refund_top_button")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Refund",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "Refund",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Green Button for Cashback
                        Button(
                            onClick = { viewModel.openAdd("CASHBACK") },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("add_cashback_top_button")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add Cashback",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                "Cashback",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. Time Filter Tabs at the top (Overall, Monthly, Yearly)
            item {
                TimeFilterTabsHeader(
                    currentFilter = uiState.timeFilter,
                    onSelectFilter = { viewModel.setTimeFilter(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 2. Period Navigator (when Monthly or Yearly is active)
            if (uiState.timeFilter == CashbackTimeFilter.MONTHLY) {
                item {
                    MonthNavigator(
                        monthLabel = uiState.selectedMonthLabel,
                        onPrev = { viewModel.prevMonth() },
                        onNext = { viewModel.nextMonth() },
                        onResetCurrent = { viewModel.resetToCurrentDate() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else if (uiState.timeFilter == CashbackTimeFilter.YEARLY) {
                item {
                    YearNavigator(
                        yearLabel = uiState.selectedYearLabel,
                        onPrev = { viewModel.prevYear() },
                        onNext = { viewModel.nextYear() },
                        onResetCurrent = { viewModel.resetToCurrentDate() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // 3. Summary Hero Card (Dynamically reflects the selected period: Overall / Month / Year)
            item {
                CashbackRefundPeriodSummaryCard(
                    timeFilter = uiState.timeFilter,
                    monthLabel = uiState.selectedMonthLabel,
                    yearLabel = uiState.selectedYearLabel,
                    totalRecovered = uiState.periodTotalRecovered,
                    cashbackEarned = uiState.periodCashbackEarned,
                    refundsReceived = uiState.periodRefundsReceived,
                    transactionCount = uiState.periodCount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // 4. Type Sub-Filter Tabs (All, Cashback, Refund)
            item {
                TypeFilterTabsRow(
                    activeType = uiState.activeTypeTab,
                    onSelectType = { viewModel.setTypeTab(it) },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // 5. Search Bar
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search by platform, merchant, source...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
            }

            // 6. Transactions Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions (${uiState.filteredItems.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 7. Items List
            if (uiState.filteredItems.isEmpty()) {
                item {
                    EmptyCashbackPlaceholder(
                        onAddCashback = { viewModel.openAdd("CASHBACK") },
                        onAddRefund = { viewModel.openAdd("REFUND") }
                    )
                }
            } else {
                items(uiState.filteredItems, key = { it.id }) { item ->
                    CashbackRefundListItem(
                        item = item,
                        onEdit = { viewModel.openEdit(item) },
                        onDelete = { viewModel.deleteItem(item) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (uiState.isAddEditOpen && uiState.editingItem != null) {
        AddEditCashbackRefundDialog(
            item = uiState.editingItem!!,
            availablePlatforms = uiState.availablePlatforms,
            onDismiss = { viewModel.closeAddEdit() },
            onSave = { viewModel.saveItem(it) }
        )
    }
}

@Composable
fun TimeFilterTabsHeader(
    currentFilter: CashbackTimeFilter,
    onSelectFilter: (CashbackTimeFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimeFilterTabItem(
                title = "Overall",
                icon = Icons.Default.Analytics,
                isSelected = currentFilter == CashbackTimeFilter.OVERALL,
                onClick = { onSelectFilter(CashbackTimeFilter.OVERALL) },
                modifier = Modifier.weight(1f)
            )
            TimeFilterTabItem(
                title = "Monthly",
                icon = Icons.Default.CalendarMonth,
                isSelected = currentFilter == CashbackTimeFilter.MONTHLY,
                onClick = { onSelectFilter(CashbackTimeFilter.MONTHLY) },
                modifier = Modifier.weight(1f)
            )
            TimeFilterTabItem(
                title = "Yearly",
                icon = Icons.Default.DateRange,
                isSelected = currentFilter == CashbackTimeFilter.YEARLY,
                onClick = { onSelectFilter(CashbackTimeFilter.YEARLY) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TimeFilterTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = isSelected,
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MonthNavigator(
    monthLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onResetCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }
    }
}

@Composable
fun YearNavigator(
    yearLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onResetCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Year $yearLabel",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
            }
        }
    }
}

@Composable
fun CashbackRefundPeriodSummaryCard(
    timeFilter: CashbackTimeFilter,
    monthLabel: String,
    yearLabel: String,
    totalRecovered: Double,
    cashbackEarned: Double,
    refundsReceived: Double,
    transactionCount: Int,
    modifier: Modifier = Modifier
) {
    val periodTitle = when (timeFilter) {
        CashbackTimeFilter.OVERALL -> "All-Time Recovered & Saved"
        CashbackTimeFilter.MONTHLY -> "Recovered in $monthLabel"
        CashbackTimeFilter.YEARLY -> "Recovered in $yearLabel"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = periodTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = formatCashbackCurrency(totalRecovered),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF059669) // Emerald Green
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "$transactionCount Items",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Grid (Cashback vs Refund)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cashback Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cashback",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCashbackCurrency(cashbackEarned),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Refunds Box
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Replay,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Refunds",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCashbackCurrency(refundsReceived),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TypeFilterTabsRow(
    activeType: String,
    onSelectType: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        "ALL" to "All",
        "CASHBACK" to "💰 Cashbacks",
        "REFUND" to "🔄 Refunds"
    )

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (key, label) ->
            val isSelected = activeType == key
            FilterChip(
                selected = isSelected,
                onClick = { onSelectType(key) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun CashbackRefundListItem(
    item: CashbackRefundEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCashback = item.type.equals("CASHBACK", ignoreCase = true)
    val displaySource = item.source.ifBlank { item.title.ifBlank { if (isCashback) "Cashback" else "Refund" } }
    val displayDate = item.receivedDate?.ifBlank { item.expectedDate } ?: item.expectedDate

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cashback_item_${item.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Platform/Source + Date + Category
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Type Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCashback) Color(0xFFECFDF5) else Color(0xFFEFF6FF)
                    ) {
                        Text(
                            text = if (isCashback) "CASHBACK" else "REFUND",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCashback) Color(0xFF047857) else Color(0xFF1D4ED8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (item.category.isNotBlank() && item.category != "General") {
                        Text(
                            text = "• ${item.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Source acts directly as Title
                Text(
                    text = displaySource,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (displayDate.isNotBlank()) {
                    Text(
                        text = "Date: $displayDate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Amount + Action Icons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${if (isCashback) "+" else ""}${formatCashbackCurrency(item.amount)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isCashback) Color(0xFF059669) else Color(0xFF2563EB)
                )

                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditCashbackRefundDialog(
    item: CashbackRefundEntity,
    availablePlatforms: List<String>,
    onDismiss: () -> Unit,
    onSave: (CashbackRefundEntity) -> Unit
) {
    var type by remember { mutableStateOf(item.type) }
    var amount by remember { mutableStateOf(if (item.amount > 0) "${item.amount}" else "") }

    val todayIso = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    var date by remember {
        val d = item.receivedDate?.ifBlank { item.expectedDate } ?: item.expectedDate
        mutableStateOf(if (d.isNotBlank()) d else todayIso)
    }

    // Determine initial selected suggestion chip and custom source
    val initialChip = remember(item.source, availablePlatforms) {
        val s = item.source.trim()
        if (s.isEmpty()) {
            availablePlatforms.firstOrNull { it != "Other" } ?: "Amazon"
        } else if (availablePlatforms.any { it.equals(s, ignoreCase = true) } && !s.equals("Other", ignoreCase = true)) {
            availablePlatforms.first { it.equals(s, ignoreCase = true) }
        } else {
            "Other"
        }
    }
    val initialCustomText = remember(item.source, availablePlatforms) {
        val s = item.source.trim()
        if (!availablePlatforms.any { it.equals(s, ignoreCase = true) } || s.equals("Other", ignoreCase = true)) s else ""
    }

    var selectedChip by remember(initialChip) { mutableStateOf(initialChip) }
    var customSourceText by remember(initialCustomText) { mutableStateOf(initialCustomText) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (item.id == 0L) {
                    "Add ${if (type == "CASHBACK") "Cashback" else "Refund"}"
                } else {
                    "Edit ${if (type == "CASHBACK") "Cashback" else "Refund"}"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // 1. Type Switcher
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = type == "CASHBACK",
                        onClick = { type = "CASHBACK" },
                        label = { Text("💰 Cashback") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = type == "REFUND",
                        onClick = { type = "REFUND" },
                        label = { Text("🔄 Refund") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Amount with calculator / inline '+' arithmetic support
                MathAmountTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    placeholder = "0.00",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Date with Calendar Dialog Picker
                DateFieldWithPicker(
                    value = date,
                    onValueChange = { date = it },
                    label = "Date",
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Source / Platform / Merchant Selection
                Text(
                    text = "Source / Platform / Merchant",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availablePlatforms) { platform ->
                        val isSelected = selectedChip.equals(platform, ignoreCase = true)
                        val isCustom = !DEFAULT_SUGGESTED_PLATFORMS.contains(platform) && platform != "Other"
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedChip = platform
                                if (platform != "Other") {
                                    customSourceText = ""
                                }
                            },
                            label = {
                                Text(
                                    text = platform,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (isCustom) Color(0xFFF3E8FF) else MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = if (isCustom) Color(0xFF7E22CE) else MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Show text box only if "Other" is selected, else hidden
                if (selectedChip == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customSourceText,
                        onValueChange = { customSourceText = it },
                        label = { Text("Custom Source / Merchant") },
                        placeholder = { Text("e.g. Uber, Blinkit, Local Store...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_source_textfield"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val evaluatedAmt = if (MathExpressionEvaluator.hasExpression(amount)) {
                        MathExpressionEvaluator.evaluate(amount) ?: (amount.toDoubleOrNull() ?: 0.0)
                    } else {
                        amount.toDoubleOrNull() ?: 0.0
                    }

                    val finalSource = if (selectedChip == "Other") {
                        customSourceText.trim().ifBlank { "Other" }
                    } else {
                        selectedChip
                    }

                    val finalDate = date.trim().ifBlank { todayIso }

                    val updated = item.copy(
                        type = type,
                        title = finalSource,
                        source = finalSource,
                        amount = evaluatedAmt,
                        cardId = null,
                        status = "RECEIVED",
                        expectedDate = finalDate,
                        receivedDate = finalDate,
                        referenceNumber = "",
                        notes = ""
                    )
                    onSave(updated)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EmptyCashbackPlaceholder(
    onAddCashback: () -> Unit,
    onAddRefund: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Savings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Entries for this Period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Track your shopping cashbacks and returned order refunds",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAddCashback) {
                    Text("+ Add Cashback")
                }
                OutlinedButton(onClick = onAddRefund) {
                    Text("+ Add Refund")
                }
            }
        }
    }
}

fun formatCashbackCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}
