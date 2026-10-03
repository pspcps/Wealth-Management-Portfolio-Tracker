package com.example.ui.screens.expenses

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.domain.model.ExpenseCategoryBreakdown
import com.example.domain.model.ExpenseComparisonRange
import com.example.ui.components.*
import com.example.util.MathExpressionEvaluator
import com.example.ui.theme.*
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Helper to categorize Needs (Essential) vs Wants (Discretionary)
fun isEssentialCategory(categoryId: String, categoryName: String): Boolean {
    val key = "${categoryId.lowercase()} ${categoryName.lowercase()}"
    val essentialKeywords = listOf(
        "grocer", "supermarket", "ration", "vegetable", "milk", "kirana", "provisions", "daily",
        "rent", "hous", "utilit", "bill", "electr", "water", "wifi", "gas", "cylinder",
        "transport", "fuel", "petrol", "diesel", "cab", "auto", "commute", "bus", "metro",
        "educat", "course", "school", "college", "tuition", "book", "fee",
        "medic", "health", "doctor", "hospital", "pharma", "medicine", "clinic",
        "insur", "emi", "loan", "maintenance", "tax", "essential", "need"
    )
    return essentialKeywords.any { key.contains(it) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: ExpensesViewModel
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showMonthPicker by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showBulkUploadDialog by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var preselectedCatIdForAdd by remember { mutableStateOf<String?>(null) }
    var editingExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    // Search and Filter State
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("ALL") } // ALL, NEEDS, WANTS

    // Expanded Category IDs state: Default is EMPTY (all hidden/collapsed)
    var expandedCategoryIds by remember { mutableStateOf(setOf<String>()) }

    val lastExpenseDateMillis = state.currentMonthExpenses.maxOfOrNull { it.dateMillis } ?: state.lastAddedExpenseDateMillis
    val hasExpenses = state.currentMonthExpenses.isNotEmpty() && lastExpenseDateMillis != null && lastExpenseDateMillis > 0
    val lastDateFormatted = if (hasExpenses) {
        SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(lastExpenseDateMillis!!))
    } else {
        "No expenses recorded yet"
    }
    val nextUploadDateFormatted = if (hasExpenses) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = lastExpenseDateMillis!!
            add(Calendar.DAY_OF_YEAR, 1)
        }
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
    } else null

    // Group expenses by category
    val expensesByCategory = remember(state.currentMonthExpenses) {
        state.currentMonthExpenses.groupBy { it.expenseCategoryId }
    }

    // Analytics Computations
    val totalExpense = state.expenseSummary?.totalExpense ?: 0.0

    // Needs vs Wants split
    val needsBreakdowns = remember(state.categoryBreakdowns) {
        state.categoryBreakdowns.filter { isEssentialCategory(it.category.id, it.category.name) }
    }
    val wantsBreakdowns = remember(state.categoryBreakdowns) {
        state.categoryBreakdowns.filter { !isEssentialCategory(it.category.id, it.category.name) }
    }
    val totalNeedsAmount = remember(needsBreakdowns) { needsBreakdowns.sumOf { it.totalAmount } }
    val totalWantsAmount = remember(wantsBreakdowns) { wantsBreakdowns.sumOf { it.totalAmount } }
    val needsPct = if (totalExpense > 0) (totalNeedsAmount / totalExpense) * 100.0 else 0.0
    val wantsPct = if (totalExpense > 0) (totalWantsAmount / totalExpense) * 100.0 else 0.0

    // Days elapsed in month computation for Daily Burn Rate & Month-End Projection
    val (daysElapsed, totalDaysInMonth, isCurrentSnapshotMonth) = remember(state.selectedSnapshotId) {
        val parts = state.selectedSnapshotId.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 8

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        val targetCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = targetCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        if (y == currentYear && m == currentMonth) {
            Triple(currentDay.coerceIn(1, maxDays), maxDays, true)
        } else if (y < currentYear || (y == currentYear && m < currentMonth)) {
            Triple(maxDays, maxDays, false)
        } else {
            Triple(1, maxDays, false)
        }
    }

    // Identify Fixed Recurring Expenses (Rent, Broadband, Utilities, Subscriptions, Maintenance)
    val recurringFlowTitles = remember(state.activeRecurringExpenses) {
        state.activeRecurringExpenses.map { it.name.trim().lowercase() }.toSet()
    }
    val fixedExpensesList = remember(state.currentMonthExpenses, recurringFlowTitles) {
        state.currentMonthExpenses.filter { exp ->
            val t = exp.title.trim().lowercase()
            val c = exp.expenseCategoryId.lowercase()
            recurringFlowTitles.contains(t) ||
            t.contains("rent") || t.contains("broadband") || t.contains("wifi") ||
            t.contains("maintenance") || t.contains("society") || t.contains("subscription") ||
            c == "utilities" || c == "housing"
        }
    }
    val fixedExpensesTotal = remember(fixedExpensesList) { fixedExpensesList.sumOf { it.amount } }
    val variableExpensesTotal = remember(totalExpense, fixedExpensesTotal) {
        (totalExpense - fixedExpensesTotal).coerceAtLeast(0.0)
    }

    // Variable Daily Burn Rate (excludes lump-sum fixed monthly commitments like Rent & Wifi)
    val dailyBurnRate = if (daysElapsed > 0) variableExpensesTotal / daysElapsed else 0.0
    // Month-End Forecast = Known Fixed Commitments + Projected Variable Lifestyle Burn Rate
    val projectedMonthEndExpense = if (isCurrentSnapshotMonth) {
        fixedExpensesTotal + (dailyBurnRate * totalDaysInMonth)
    } else totalExpense

    // Top merchants / items
    val topMerchants = remember(state.currentMonthExpenses) {
        state.currentMonthExpenses
            .groupBy { it.title.trim().lowercase() }
            .map { entry ->
                val displayTitle = state.currentMonthExpenses.firstOrNull { it.title.trim().equals(entry.key, ignoreCase = true) }?.title ?: entry.key
                val sum = entry.value.sumOf { it.amount }
                val count = entry.value.size
                Triple(displayTitle, sum, count)
            }
            .sortedByDescending { it.second }
            .take(4)
    }
    val topMerchantsTotal = topMerchants.sumOf { it.second }
    val topMerchantsPct = if (totalExpense > 0) (topMerchantsTotal / totalExpense) * 100.0 else 0.0
    val largestSingleExpense = remember(state.currentMonthExpenses) {
        state.currentMonthExpenses.maxByOrNull { it.amount }
    }

    // Filtered Category Breakdowns
    val filteredBreakdowns = remember(state.categoryBreakdowns, searchQuery, selectedFilterType) {
        state.categoryBreakdowns.filter { breakdown ->
            val matchesFilter = when (selectedFilterType) {
                "NEEDS" -> isEssentialCategory(breakdown.category.id, breakdown.category.name)
                "WANTS" -> !isEssentialCategory(breakdown.category.id, breakdown.category.name)
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                val q = searchQuery.trim().lowercase()
                val catMatches = breakdown.category.name.lowercase().contains(q)
                val itemsMatch = (expensesByCategory[breakdown.category.id] ?: emptyList()).any {
                    it.title.lowercase().contains(q) || it.notes.lowercase().contains(q)
                }
                catMatches || itemsMatch
            }
            matchesFilter && matchesSearch
        }
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
                    Column {
                        Text(
                            text = "Expense Analytics",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Tracked independently from portfolio",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlobalPrivacyEyeButton(
                            size = 40.dp,
                            iconSize = 20.dp,
                            testTag = "btn_expenses_privacy_eye"
                        )

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { showBulkUploadDialog = true }
                                .testTag("btn_bulk_upload_top")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileUpload,
                                    contentDescription = "Upload Expenses",
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable { showAddCategoryDialog = true }
                                .testTag("btn_add_expense_category_top")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = "Add Expense Category",
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    preselectedCatIdForAdd = null
                                    editingExpense = null
                                    showAddExpenseDialog = true
                                }
                                .testTag("btn_add_expense_top")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Expense",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // Month Selector Row
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showMonthPicker = true }
                        .testTag("btn_expense_month_selector")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Ruby500.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = Ruby500,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            val snapLabel = state.selectedSnapshotId.let { id ->
                                val parts = id.split("-")
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                val y = parts.getOrNull(0) ?: ""
                                "${DateFormatSymbols().months.getOrNull(m - 1) ?: m} $y"
                            }
                            Column {
                                Text(
                                    text = "SELECTED PERIOD",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSecondary
                                )
                                Text(
                                    text = snapLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                            }
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = HighDensityTextSecondary
                        )
                    }
                }
            }

            // Tracking Status & Last Recorded Date Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (hasExpenses) Color(0xFFF0FDF4) else Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, if (hasExpenses) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (hasExpenses) Icons.Default.EventAvailable else Icons.Default.PendingActions,
                                    contentDescription = null,
                                    tint = if (hasExpenses) Emerald500 else Color(0xFFD97706),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "LAST RECORDED DATE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasExpenses) Emerald500 else Color(0xFFD97706)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (hasExpenses) Emerald500.copy(alpha = 0.15f) else Color(0xFFD97706).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (hasExpenses) "${state.currentMonthExpenses.size} items recorded" else "0 items",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (hasExpenses) Emerald500 else Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Expenses recorded till: $lastDateFormatted",
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            if (nextUploadDateFormatted != null) {
                                Text(
                                    text = "Upload / record expenses starting from $nextUploadDateFormatted onwards",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            } else {
                                Text(
                                    text = "Add your first expense or upload a statement batch",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }
                        }

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBulkUploadDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, HighDensityPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bulk Upload", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                            }

                            Button(
                                onClick = {
                                    preselectedCatIdForAdd = null
                                    editingExpense = null
                                    showAddExpenseDialog = true
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Expense", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                            }
                        }
                    }
                }
            }

            // Total Monthly Expense Card with Benchmarks
            val summary = state.expenseSummary
            item {
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL MONTHLY EXPENSES",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextSecondary
                            )

                            summary?.benchmarkGrowth?.let { growth ->
                                if (growth.hasPreviousData) {
                                    val isReduction = growth.diffAmount <= 0
                                    val diffPct = String.format("%.1f", Math.abs(growth.growthPercentage ?: 0.0))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (isReduction) Emerald500.copy(alpha = 0.12f) else Ruby500.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = "${if (growth.diffAmount > 0) "+" else ""}$diffPct% ${summary.benchmarkLabel}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isReduction) Emerald500 else Ruby500,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = summary?.let { CurrencyFormatter.formatInr(it.totalExpense) } ?: "₹0",
                            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.Black,
                            color = HighDensityTextPrimary
                        )

                        // Comparison Period Selector (1M, 3M, 6M, 12M)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Benchmark Range:",
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
                                            .testTag("btn_exp_page_range_${rangeOption.name}")
                                    ) {
                                        Text(
                                            text = rangeOption.label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else HighDensityTextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (summary?.benchmarkGrowth?.hasPreviousData == true) {
                            val diff = summary.benchmarkGrowth.diffAmount
                            val isReduction = diff <= 0
                            Text(
                                text = "${if (diff > 0) "Increased by " else "Decreased by "}${CurrencyFormatter.formatInr(Math.abs(diff))} compared to ${summary.benchmarkLabel} (${CurrencyFormatter.formatInr(summary.benchmarkExpense)})",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = if (isReduction) Emerald500 else Ruby500
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Rolling Averages Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("3M Average", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                Text(CurrencyFormatter.formatInrCompact(summary?.threeMonthAvg ?: 0.0), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                            }
                            Column {
                                Text("6M Average", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                Text(CurrencyFormatter.formatInrCompact(summary?.sixMonthAvg ?: 0.0), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                            }
                            Column {
                                Text("12M Average", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = HighDensityTextSecondary)
                                Text(CurrencyFormatter.formatInrCompact(summary?.twelveMonthAvg ?: 0.0), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // ADVANCED EXPENSE ANALYTICS & DECISION CARDS
            // ==========================================

            // 1. Needs vs. Wants Split (50/30/20 Rule Analysis & Levers)
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
                                    color = HighDensityPrimary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.PieChart,
                                            contentDescription = null,
                                            tint = HighDensityPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Needs vs. Wants Allocation",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = "50/30/20 Budgeting Rule Health Check",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            // 50/30/20 Status Badge
                            val isHealthyWants = wantsPct <= 35.0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isHealthyWants) Emerald500.copy(alpha = 0.12f) else Ruby500.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isHealthyWants) "Balanced Budget" else "High Lifestyle Spend",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isHealthyWants) Emerald500 else Ruby500,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        // Needs vs Wants Dual Progress Bar
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                if (totalExpense > 0) {
                                    val needsWeight = (needsPct / 100.0).toFloat().coerceIn(0.01f, 0.99f)
                                    val wantsWeight = (wantsPct / 100.0).toFloat().coerceIn(0.01f, 0.99f)

                                    Box(
                                        modifier = Modifier
                                            .weight(needsWeight)
                                            .fillMaxHeight()
                                            .background(Emerald500)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(wantsWeight)
                                            .fillMaxHeight()
                                            .background(Ruby500)
                                    )
                                }
                            }

                            // Legend and Breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Needs info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Emerald500)
                                    )
                                    Column {
                                        Text(
                                            text = "Essential Needs (${String.format("%.1f", needsPct)}%)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatInr(totalNeedsAmount)} • Ideal: ≤50%",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                // Wants info
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Ruby500)
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Lifestyle Wants (${String.format("%.1f", wantsPct)}%)",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = "${CurrencyFormatter.formatInr(totalWantsAmount)} • Ideal: ≤30%",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Actionable Savings Levers & Compound FIRE Multiplier
                        val trimAmount20 = totalWantsAmount * 0.20
                        // Compound growth of monthly saving invested at 12% CAGR over 10 years
                        val compound10Y = trimAmount20 * 232.3 // monthly annuity factor for 120m @ 1% / month
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = BorderStroke(1.dp, Color(0xFFDCFCE7))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.ElectricBolt,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "💡 Decision Lever: Discretionary Spend Optimization",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                    Text(
                                        text = if (totalWantsAmount > 0) {
                                            "Trimming 20% of lifestyle wants saves ${CurrencyFormatter.formatInr(trimAmount20)}/month. Invested at 12% CAGR, this builds ${CurrencyFormatter.formatInrCompact(compound10Y)} extra in 10 years!"
                                        } else {
                                            "Maintain low lifestyle spending to maximize SIP investments and accelerate FIRE."
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                                        color = HighDensityTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Daily Burn Rate & Month-End Projected Outflow
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
                                    color = Color(0xFFD97706).copy(alpha = 0.12f),
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Daily Run Rate & Forecast",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = if (isCurrentSnapshotMonth) "Day $daysElapsed of $totalDaysInMonth elapsed (${String.format("%.0f", (daysElapsed.toDouble() / totalDaysInMonth) * 100)}%)" else "Full Month Summary ($totalDaysInMonth days)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            if (isCurrentSnapshotMonth) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Text(
                                        text = "${totalDaysInMonth - daysElapsed} days left",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextSecondary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // 3-Metric Clean Grid: Variable Daily Burn, Fixed Bills, Month-End Forecast
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Variable Daily Burn Rate
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "VARIABLE BURN",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = "${CurrencyFormatter.formatInr(dailyBurnRate)} / day",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Excl. fixed bills",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = Emerald600,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Fixed Recurring Bills (Rent, Wifi, Subscriptions)
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "FIXED BILLS",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatInr(fixedExpensesTotal),
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "Rent, Wifi, etc.",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            // Projected Month End
                            Surface(
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (isCurrentSnapshotMonth) "PROJECTED TOTAL" else "TOTAL OUTFLOW",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextSecondary
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatInr(projectedMonthEndExpense),
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = Ruby500,
                                        maxLines = 1
                                    )
                                    val threeMAvg = summary?.threeMonthAvg ?: 0.0
                                    val paceDiff = projectedMonthEndExpense - threeMAvg
                                    val compStr = if (threeMAvg > 0) {
                                        "${if (paceDiff > 0) "+" else ""}${CurrencyFormatter.formatInrCompact(paceDiff)} 3M"
                                    } else "Fixed + Variable"
                                    Text(
                                        text = compStr,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                                        color = if (paceDiff > 0) Ruby500 else Emerald500,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Auto-Add Recurring Fixed Expenses Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Autorenew,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Recurring Fixed Bills (${state.activeRecurringExpenses.size} setup)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Rent, Wifi & bills auto-isolated from daily run rate",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        color = HighDensityTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                FilledTonalButton(
                                    onClick = {
                                        viewModel.autoAddRecurringExpenses()
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "⚡ Auto-Add",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Top Spending Merchants & Concentration Outflows
            if (topMerchants.isNotEmpty()) {
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
                                        color = Ruby500.copy(alpha = 0.12f),
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.Storefront,
                                                contentDescription = null,
                                                tint = Ruby500,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Top Outflows & Merchants",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = "Top ${topMerchants.size} account for ${String.format("%.0f", topMerchantsPct)}% of total expenses",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                if (largestSingleExpense != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFFF1F2)
                                    ) {
                                        Text(
                                            text = "Max: ${CurrencyFormatter.formatInrCompact(largestSingleExpense.amount)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = Ruby500,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Top items grid
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                topMerchants.forEachIndexed { index, (merchant, amt, count) ->
                                    val pct = if (totalExpense > 0) (amt / totalExpense) * 100.0 else 0.0
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFFF1F5F9),
                                                modifier = Modifier.size(20.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = "${index + 1}",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityTextSecondary
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = merchant,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = HighDensityTextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "$count transaction${if (count > 1) "s" else ""} • ${String.format("%.1f", pct)}% share",
                                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                    color = HighDensityTextSecondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = CurrencyFormatter.formatInr(amt),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Net Worth Growth vs Monthly Burn Card
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HighDensityPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.CompareArrows,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Wealth Accumulation vs Spending",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Portfolio Growth (MoM)", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatInr(state.portfolioMonthGrowth, showSign = true),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.portfolioMonthGrowth >= 0) Emerald500 else Ruby500
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Monthly Spending", style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = HighDensityTextSecondary)
                                Text(
                                    text = CurrencyFormatter.formatInr(summary?.totalExpense ?: 0.0),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Ruby500
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Expense Spending Trend Chart Card
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
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Ruby500.copy(alpha = 0.12f),
                                modifier = Modifier.size(24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.BarChart,
                                        contentDescription = null,
                                        tint = Ruby500,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Monthly Spending Trend",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        ExpenseTrendChart(
                            dataPoints = state.historicalExpensePoints,
                            selectedSnapshotId = state.selectedSnapshotId,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // ==========================================
            // MERGED & EXPANDABLE CATEGORY BREAKDOWN
            // (Contains individual recorded expenses under each category!)
            // ==========================================

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header Bar with clean Layout and Expand/Collapse All
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Category Breakdown & Expenses",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "${state.categoryBreakdowns.size} Categories • ${state.currentMonthExpenses.size} Recorded Entries",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }

                        // Expand All / Collapse All Toggle Button
                        val allExpanded = state.categoryBreakdowns.isNotEmpty() &&
                                state.categoryBreakdowns.all { expandedCategoryIds.contains(it.category.id) }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showAddCategoryDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Category",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        expandedCategoryIds = if (allExpanded) {
                                            emptySet()
                                        } else {
                                            state.categoryBreakdowns.map { it.category.id }.toSet()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (allExpanded) "Collapse All" else "Expand All",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary
                                    )
                                }
                            }
                        }
                    }

                    // Search & Filter Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // All Filter
                        FilterChip(
                            selected = selectedFilterType == "ALL",
                            onClick = { selectedFilterType = "ALL" },
                            label = { Text("All (${state.categoryBreakdowns.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = HighDensityPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                        // Needs Filter
                        FilterChip(
                            selected = selectedFilterType == "NEEDS",
                            onClick = { selectedFilterType = "NEEDS" },
                            label = { Text("Needs (${needsBreakdowns.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald500,
                                selectedLabelColor = Color.White
                            )
                        )
                        // Wants Filter
                        FilterChip(
                            selected = selectedFilterType == "WANTS",
                            onClick = { selectedFilterType = "WANTS" },
                            label = { Text("Wants (${wantsBreakdowns.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Ruby500,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Category Cards with Collapsible Child Expenses
            if (filteredBreakdowns.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                    ) {
                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (state.categoryBreakdowns.isEmpty()) "No category expenses recorded for this month." else "No matching categories found for filter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = HighDensityTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredBreakdowns, key = { it.category.id }) { catItem ->
                    val catColor = IconMapper.parseColor(catItem.category.colorHex)
                    val isExpanded = expandedCategoryIds.contains(catItem.category.id)
                    val categoryExpenses = expensesByCategory[catItem.category.id] ?: emptyList()
                    val isEssential = isEssentialCategory(catItem.category.id, catItem.category.name)

                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isExpanded) 180f else 0f,
                        label = "expand_rotation"
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, if (isExpanded) catColor.copy(alpha = 0.35f) else Color(0xFFF1F5F9)),
                        shadowElevation = if (isExpanded) 2.dp else 1.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Category Header (Clickable to toggle expand/collapse)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategoryIds = if (isExpanded) {
                                            expandedCategoryIds - catItem.category.id
                                        } else {
                                            expandedCategoryIds + catItem.category.id
                                        }
                                    },
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
                                                imageVector = IconMapper.getIcon(catItem.category.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(19.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f, fill = false),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = catItem.category.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = HighDensityTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            // Needs vs Wants Mini Badge
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isEssential) Emerald500.copy(alpha = 0.1f) else Ruby500.copy(alpha = 0.1f)
                                            ) {
                                                Text(
                                                    text = if (isEssential) "Need" else "Want",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isEssential) Emerald500 else Ruby500,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "${String.format("%.1f", catItem.percentage)}% of spend • ${catItem.itemCount} item${if (catItem.itemCount != 1) "s" else ""}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = HighDensityTextSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
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
                                            text = CurrencyFormatter.formatInr(catItem.totalAmount),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )

                                        if (catItem.comparisonAmount > 0 && catItem.growthPercentage != null) {
                                            val isIncrease = (catItem.growthPercentage ?: 0.0) > 0
                                            val diffPct = String.format("%.1f", Math.abs(catItem.growthPercentage ?: 0.0))
                                            val badgeColor = if (isIncrease) Ruby500 else Emerald500
                                            val arrow = if (isIncrease) "↑" else "↓"
                                            val compCleanLabel = catItem.comparisonLabel.removePrefix("vs ").trim()
                                            Text(
                                                text = "$arrow $diffPct% vs $compCleanLabel",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        } else if (catItem.comparisonAmount == 0.0 && catItem.totalAmount > 0) {
                                            Text(
                                                text = "New spend",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                color = HighDensityPrimary,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    // Expand / Collapse Chevron indicator
                                    IconButton(
                                        onClick = {
                                            expandedCategoryIds = if (isExpanded) {
                                                expandedCategoryIds - catItem.category.id
                                            } else {
                                                expandedCategoryIds + catItem.category.id
                                            }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                                            tint = HighDensityTextSecondary,
                                            modifier = Modifier.rotate(rotationAngle)
                                        )
                                    }
                                }
                            }

                            // Progress Track
                            LinearProgressIndicator(
                                progress = { (catItem.percentage / 100f).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = catColor,
                                trackColor = Color(0xFFF1F5F9)
                            )

                            // Animated Child Expenses Content
                            AnimatedVisibility(
                                visible = isExpanded,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    if (categoryExpenses.isEmpty()) {
                                        Text(
                                            text = "No individual expenses recorded under this category.",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = HighDensityTextSecondary,
                                            modifier = Modifier.padding(vertical = 6.dp)
                                        )
                                    } else {
                                        categoryExpenses.forEach { expense ->
                                            val expenseDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(expense.dateMillis))

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
                                                    Column(
                                                        modifier = Modifier.weight(1f),
                                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                                    ) {
                                                        Text(
                                                            text = expense.title,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = HighDensityTextPrimary
                                                        )
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = expenseDateStr,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                color = HighDensityTextSecondary
                                                            )
                                                            if (expense.notes.isNotBlank()) {
                                                                Text(
                                                                    text = "• ${expense.notes}",
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                    color = HighDensityTextSecondary,
                                                                    maxLines = 1
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Text(
                                                            text = CurrencyFormatter.formatInr(expense.amount),
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Ruby500
                                                        )

                                                        // Edit button
                                                        IconButton(
                                                            onClick = {
                                                                editingExpense = expense
                                                                preselectedCatIdForAdd = expense.expenseCategoryId
                                                                showAddExpenseDialog = true
                                                            },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.Edit,
                                                                contentDescription = "Edit",
                                                                tint = HighDensityTextSecondary,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }

                                                        // Delete button
                                                        IconButton(
                                                            onClick = { viewModel.deleteExpense(expense) },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                Icons.Default.DeleteOutline,
                                                                contentDescription = "Delete",
                                                                tint = Ruby500,
                                                                modifier = Modifier.size(15.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Quick Add Button inside Category
                                    OutlinedButton(
                                        onClick = {
                                            editingExpense = null
                                            preselectedCatIdForAdd = catItem.category.id
                                            showAddExpenseDialog = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = catColor),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add item in ${catItem.category.name}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp), fontWeight = FontWeight.Bold)
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
            onCreateSnapshot = { y, m ->
                val snapId = String.format("%04d-%02d", y, m)
                viewModel.selectSnapshot(snapId)
            },
            onDismiss = { showMonthPicker = false }
        )
    }

    // Add / Edit Expense Dialog
    if (showAddExpenseDialog) {
        var expenseTitle by remember { mutableStateOf(editingExpense?.title ?: "") }
        var selectedCatId by remember {
            mutableStateOf(
                editingExpense?.expenseCategoryId
                    ?: preselectedCatIdForAdd
                    ?: state.expenseCategories.firstOrNull { it.id == "groceries" || it.id == "grocery" || it.name.contains("Grocer", ignoreCase = true) }?.id
                    ?: state.expenseCategories.firstOrNull()?.id
                    ?: "groceries"
            )
        }
        var expenseAmount by remember {
            mutableStateOf(editingExpense?.let { Math.round(it.amount).toString() } ?: "")
        }
        var expenseNotes by remember { mutableStateOf(editingExpense?.notes ?: "") }
        var selectedDateMillis by remember { mutableStateOf(editingExpense?.dateMillis ?: System.currentTimeMillis()) }
        var dateFormattedInput by remember {
            mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedDateMillis)))
        }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            containerColor = Color.White,
            title = {
                Text(
                    text = if (editingExpense != null) "Edit Expense" else "Record Expense",
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = expenseTitle,
                        onValueChange = { expenseTitle = it },
                        label = { Text("Expense Title / Merchant") },
                        placeholder = { Text("e.g. Zepto, Rent, Electricity Bill, Amazon") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Category Dropdown
                    var catDropdownExpanded by remember { mutableStateOf(false) }
                    val currentCatName = state.expenseCategories.firstOrNull { it.id == selectedCatId }?.name ?: selectedCatId

                    Box {
                        OutlinedTextField(
                            value = currentCatName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Category") },
                            trailingIcon = {
                                IconButton(onClick = { catDropdownExpanded = !catDropdownExpanded }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { catDropdownExpanded = true }
                        )

                                DropdownMenu(
                            expanded = catDropdownExpanded,
                            onDismissRequest = { catDropdownExpanded = false }
                        ) {
                            state.expenseCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = IconMapper.getIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = IconMapper.parseColor(cat.colorHex),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    onClick = {
                                        selectedCatId = cat.id
                                        catDropdownExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("+ New Category...", color = HighDensityPrimary, fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    catDropdownExpanded = false
                                    showAddCategoryDialog = true
                                }
                            )
                        }
                    }

                    MathAmountTextField(
                        value = expenseAmount,
                        onValueChange = { expenseAmount = it },
                        label = "Amount",
                        placeholder = "e.g. 350 + 220 + 85",
                        modifier = Modifier.fillMaxWidth()
                    )

                    DateFieldWithPicker(
                        value = dateFormattedInput,
                        onValueChange = {
                            dateFormattedInput = it
                            try {
                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it)
                                if (parsed != null) selectedDateMillis = parsed.time
                            } catch (_: Exception) {}
                        },
                        label = "Expense Date",
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = expenseNotes,
                        onValueChange = { expenseNotes = it },
                        label = { Text("Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                val evaluatedAmount = MathExpressionEvaluator.evaluate(expenseAmount) ?: 0.0
                Button(
                    onClick = {
                        val amt = evaluatedAmount
                        if (expenseTitle.isNotBlank() && amt > 0) {
                            if (editingExpense != null) {
                                viewModel.updateExpense(
                                    editingExpense!!.copy(
                                        title = expenseTitle.trim(),
                                        expenseCategoryId = selectedCatId,
                                        amount = amt,
                                        notes = expenseNotes.trim(),
                                        dateMillis = selectedDateMillis
                                    )
                                )
                            } else {
                                viewModel.addExpense(
                                    title = expenseTitle.trim(),
                                    categoryId = selectedCatId,
                                    amount = amt,
                                    notes = expenseNotes.trim(),
                                    dateMillis = selectedDateMillis
                                )
                            }
                            showAddExpenseDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = expenseTitle.isNotBlank() && evaluatedAmount > 0
                ) {
                    Text(if (editingExpense != null) "Update Expense" else "Save Expense")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel", color = HighDensityTextSecondary)
                }
            }
        )
    }

    // Bulk Upload / Import Dialog
    if (showBulkUploadDialog) {
        var bulkText by remember { mutableStateOf("") }
        var defaultCatId by remember { mutableStateOf(state.expenseCategories.firstOrNull()?.id ?: "food") }
        val parsedExpenses = remember(bulkText, defaultCatId, state.selectedSnapshotId) {
            val lines = bulkText.lines().map { it.trim() }.filter { it.isNotEmpty() }
            val list = mutableListOf<ExpenseEntity>()
            lines.forEach { line ->
                val parts = line.split(",").map { it.trim() }
                if (parts.isNotEmpty()) {
                    var parsedDate = System.currentTimeMillis()
                    var title = ""
                    var catId = defaultCatId
                    var amt = 0.0
                    var notes = ""

                    if (parts.size >= 3) {
                        val dateCandidate = parts[0]
                        val parsedTime = try {
                            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateCandidate)?.time
                        } catch (_: Exception) { null }

                        if (parsedTime != null) {
                            parsedDate = parsedTime
                            title = parts.getOrNull(1) ?: ""
                            val foundCat = state.expenseCategories.firstOrNull { it.id.equals(parts.getOrNull(2), true) || it.name.equals(parts.getOrNull(2), true) }
                            if (foundCat != null) {
                                catId = foundCat.id
                                amt = parts.getOrNull(3)?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
                                notes = parts.getOrNull(4) ?: ""
                            } else {
                                amt = parts.getOrNull(2)?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
                                notes = parts.getOrNull(3) ?: ""
                            }
                        } else {
                            title = parts[0]
                            val foundCat = state.expenseCategories.firstOrNull { it.id.equals(parts[1], true) || it.name.equals(parts[1], true) }
                            if (foundCat != null) {
                                catId = foundCat.id
                                amt = parts.getOrNull(2)?.filter { it.isDigit() || it == '.' }?.toDoubleOrNull() ?: 0.0
                                notes = parts.getOrNull(3) ?: ""
                            } else {
                                amt = parts[1].filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                                notes = parts.getOrNull(2) ?: ""
                            }
                        }
                    } else if (parts.size == 2) {
                        title = parts[0]
                        amt = parts[1].filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
                    }

                    if (title.isNotBlank() && amt > 0) {
                        list.add(
                            ExpenseEntity(
                                snapshotId = state.selectedSnapshotId,
                                expenseCategoryId = catId,
                                amount = amt,
                                title = title,
                                dateMillis = parsedDate,
                                notes = notes
                            )
                        )
                    }
                }
            }
            list
        }

        AlertDialog(
            onDismissRequest = { showBulkUploadDialog = false },
            containerColor = Color.White,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = HighDensityPrimary)
                    Text("Upload Expenses", fontWeight = FontWeight.Bold, color = HighDensityTextPrimary)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "💡 Format: YYYY-MM-DD, Title, Amount, [Category]",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                            Text(
                                text = "Example:\n2026-08-20, Supermarket, 2500, food\n2026-08-21, Electricity bill, 1800, utilities",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        label = { Text("Paste CSV or Statement lines") },
                        placeholder = { Text("Paste rows here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (parsedExpenses.isNotEmpty()) Color(0xFFF0FDF4) else Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parsed Entries: ${parsedExpenses.size} items",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (parsedExpenses.isNotEmpty()) Emerald500 else HighDensityTextSecondary
                            )
                            if (parsedExpenses.isNotEmpty()) {
                                val totalParsed = parsedExpenses.sumOf { it.amount }
                                Text(
                                    text = "Total: ${CurrencyFormatter.formatInr(totalParsed)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsedExpenses.isNotEmpty()) {
                            viewModel.addBulkExpenses(parsedExpenses)
                            showBulkUploadDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp),
                    enabled = parsedExpenses.isNotEmpty()
                ) {
                    Text("Upload ${parsedExpenses.size} Expenses")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkUploadDialog = false }) {
                    Text("Cancel", color = HighDensityTextSecondary)
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var catName by remember { mutableStateOf("") }
        var catIcon by remember { mutableStateOf("shopping_cart") }
        var catColor by remember { mutableStateOf("#16A34A") }

        val expenseIcons = listOf(
            "shopping_cart" to "Groceries",
            "restaurant" to "Dining",
            "local_cafe" to "Cafe",
            "fastfood" to "Snacks",
            "home" to "Rent/House",
            "bolt" to "Utilities",
            "shopping_bag" to "Shopping",
            "directions_car" to "Fuel/Car",
            "directions_bus" to "Transit",
            "school" to "Education",
            "local_hospital" to "Medical",
            "health_and_safety" to "Insurance",
            "fitness_center" to "Fitness",
            "pets" to "Pets",
            "phone_android" to "Mobile/Recharge",
            "subscriptions" to "Software",
            "build" to "Maintenance",
            "flight" to "Travel",
            "movie" to "Entertainment",
            "family_restroom" to "Family",
            "card_giftcard" to "Gifts"
        )

        val colors = listOf(
            "#16A34A", "#F97316", "#3B82F6", "#EAB308", "#EC4899",
            "#EF4444", "#8B5CF6", "#06B6D4", "#6366F1", "#14B8A6", "#64748B"
        )

        val isNeed = remember(catName) {
            isEssentialCategory("custom", catName)
        }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = HighDensityPrimary)
                    Text("New Expense Category", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Groceries, Gym, Pet Care") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Live Preview
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val parsedCol = IconMapper.parseColor(catColor)
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = parsedCol.copy(alpha = 0.15f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = IconMapper.getIcon(catIcon),
                                        contentDescription = null,
                                        tint = parsedCol,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (catName.isNotBlank()) catName else "Category Preview",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isNeed) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (isNeed) "Essential (Needs)" else "Discretionary (Wants)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNeed) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Text("Select Icon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(expenseIcons) { (icKey, icLabel) ->
                            val isSel = catIcon == icKey
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) HighDensityPrimaryContainer else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSel) HighDensityPrimary else Color.Transparent),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { catIcon = icKey }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIcon(icKey),
                                        contentDescription = null,
                                        tint = if (isSel) HighDensityPrimary else HighDensityTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = icLabel,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) HighDensityPrimary else HighDensityTextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Text("Select Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(colors) { c ->
                            val isSel = catColor == c
                            val parsed = IconMapper.parseColor(c)
                            Surface(
                                shape = CircleShape,
                                color = parsed,
                                border = BorderStroke(2.dp, if (isSel) HighDensityTextPrimary else Color.Transparent),
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { catColor = c }
                            ) {}
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addExpenseCategory(catName.trim(), catIcon, catColor) { newCatId ->
                                preselectedCatIdForAdd = newCatId
                            }
                            showAddCategoryDialog = false
                        }
                    },
                    enabled = catName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Create Category")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = HighDensityTextSecondary)
                }
            }
        )
    }
}
