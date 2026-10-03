package com.example.ui.screens.creditcards

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.CreditCardStatementEntity
import com.example.util.ParsedStatementResult
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsScreen(
    viewModel: CreditCardsViewModel = viewModel(),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Credit Cards",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${uiState.cards.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
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
                    if (uiState.selectedTabIndex == 0) {
                        FilledTonalButton(
                            onClick = { viewModel.openUploadStatement(null) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("upload_statement_tab_btn")
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = "Upload Statement", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload PDF", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { viewModel.openAddCard() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("add_card_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Card", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Card", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tabs Row
            TabRow(
                selectedTabIndex = uiState.selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = uiState.selectedTabIndex == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Statement Analysis", fontWeight = if (uiState.selectedTabIndex == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_statements")
                )
                Tab(
                    selected = uiState.selectedTabIndex == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("My Cards (${uiState.cards.size})", fontWeight = if (uiState.selectedTabIndex == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    modifier = Modifier.testTag("tab_cards")
                )
            }

            // Tab Content
            if (uiState.selectedTabIndex == 0) {
                StatementAnalysisTabContent(
                    uiState = uiState,
                    onSelectCardFilter = { viewModel.setAnalysisCardFilter(it) },
                    onSelectMonthFilter = { viewModel.setAnalysisMonthFilter(it) },
                    onUploadStatement = { viewModel.openUploadStatement(null) },
                    onDeleteStatement = { viewModel.deleteStatement(it) }
                )
            } else {
                CardManagementTabContent(
                    uiState = uiState,
                    onSelectBank = { viewModel.setBankFilter(it) },
                    onAddCard = { viewModel.openAddCard() },
                    onUploadStatement = { viewModel.openUploadStatement(it) },
                    onPayBill = { viewModel.openPayBill(it) },
                    onEditCard = { viewModel.openEditCard(it) },
                    onDeleteCard = { viewModel.deleteCard(it) }
                )
            }
        }
    }

    // Upload Statement Sheet / Dialog (allows uploading for a selected card or standalone)
    if (uiState.isUploadStatementOpen) {
        UploadStatementModal(
            card = uiState.uploadingCard,
            password = uiState.uploadPasswordInput,
            isAutoPicked = uiState.isPasswordAutoPicked,
            sourceLabel = uiState.passwordSourceLabel,
            rememberPassword = uiState.rememberPasswordForBank,
            parsedPreview = uiState.parsedStatementPreview,
            isParsing = uiState.isParsingStatement,
            onPasswordChange = { viewModel.updateUploadPassword(it) },
            onToggleRemember = { viewModel.toggleRememberPassword(it) },
            onFileSelected = { viewModel.handleStatementFileSelected(it) },
            onConfirmSave = { stmtDate, dueDate, totalDue, minDue, cashback, pts, fn ->
                viewModel.confirmSaveStatement(stmtDate, dueDate, totalDue, minDue, cashback, pts, fn)
            },
            onDismiss = { viewModel.closeUploadStatement() }
        )
    }

    // Add / Edit Card Dialog
    if (uiState.isAddEditOpen) {
        AddEditCardDialog(
            editingCard = uiState.editingCard,
            onDismiss = { viewModel.closeAddEdit() },
            onSave = { viewModel.saveCard(it) }
        )
    }

    // Pay Bill Dialog
    if (uiState.isPayBillDialogOpen && uiState.payingCard != null) {
        PayBillDialog(
            card = uiState.payingCard!!,
            onDismiss = { viewModel.closePayBill() },
            onConfirmPay = { amt -> viewModel.confirmPayBill(uiState.payingCard!!, amt) }
        )
    }
}

@Composable
fun CardManagementTabContent(
    uiState: CreditCardsUiState,
    onSelectBank: (String) -> Unit,
    onAddCard: () -> Unit,
    onUploadStatement: (CreditCardEntity) -> Unit,
    onPayBill: (CreditCardEntity) -> Unit,
    onEditCard: (CreditCardEntity) -> Unit,
    onDeleteCard: (CreditCardEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Summary Hero Card
        item {
            CreditCardsSummaryCard(
                summary = uiState.summary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 2. Urgent Next Due Alert (if any card has dues)
        if (uiState.summary.totalDue > 0 && uiState.summary.nextDueDate.isNotBlank()) {
            item {
                NextDueAlertBanner(
                    cardName = uiState.summary.nextDueCardName,
                    dueDate = uiState.summary.nextDueDate,
                    amount = uiState.summary.nextDueAmount,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // 3. Bank Filter Chips
        if (uiState.availableBanks.size > 2) {
            item {
                BankFilterRow(
                    banks = uiState.availableBanks,
                    selectedBank = uiState.selectedBankFilter,
                    onSelectBank = onSelectBank,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
        }

        // 4. Cards Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.selectedBankFilter == "ALL") "All Credit Cards (${uiState.filteredCards.size})" else "${uiState.selectedBankFilter} Cards (${uiState.filteredCards.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Auto-Statement & Billing Dates",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 5. Cards List
        if (uiState.filteredCards.isEmpty()) {
            item {
                EmptyCardsPlaceholder(onAddCard = onAddCard)
            }
        } else {
            items(uiState.filteredCards, key = { it.id }) { card ->
                CreditCardItem(
                    card = card,
                    onUploadStatement = { onUploadStatement(card) },
                    onPayBill = { onPayBill(card) },
                    onEdit = { onEditCard(card) },
                    onDelete = { onDeleteCard(card) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun StatementAnalysisTabContent(
    uiState: CreditCardsUiState,
    onSelectCardFilter: (Long?) -> Unit,
    onSelectMonthFilter: (String) -> Unit,
    onUploadStatement: () -> Unit,
    modifier: Modifier = Modifier,
    onDeleteStatement: ((CreditCardStatementEntity) -> Unit)? = null
) {
    // Helper to format YYYY-MM-DD or YYYY-MM to Month Year (e.g., "2026-09-15" -> "Sep 2026")
    fun getMonthLabel(dateStr: String): String {
        if (dateStr.length >= 7) {
            val parts = dateStr.substring(0, 7).split("-")
            if (parts.size == 2) {
                val year = parts[0]
                val monthName = when (parts[1]) {
                    "01" -> "Jan"
                    "02" -> "Feb"
                    "03" -> "Mar"
                    "04" -> "Apr"
                    "05" -> "May"
                    "06" -> "Jun"
                    "07" -> "Jul"
                    "08" -> "Aug"
                    "09" -> "Sep"
                    "10" -> "Oct"
                    "11" -> "Nov"
                    "12" -> "Dec"
                    else -> parts[1]
                }
                return "$monthName $year"
            }
        }
        return if (dateStr.isNotBlank()) dateStr else "Recent"
    }

    // Filter statements based on selected card filter
    val filteredStatements = remember(uiState.allStatements, uiState.analysisCardFilterId, uiState.analysisMonthFilter) {
        uiState.allStatements.filter { stmt ->
            val matchesCard = uiState.analysisCardFilterId == null || stmt.cardId == uiState.analysisCardFilterId
            val monthLabel = getMonthLabel(stmt.statementDate)
            val matchesMonth = if (uiState.analysisMonthFilter == "ALL") true else {
                stmt.statementDate.contains(uiState.analysisMonthFilter, ignoreCase = true) ||
                monthLabel.contains(uiState.analysisMonthFilter, ignoreCase = true)
            }
            matchesCard && matchesMonth
        }
    }

    // Available months list extracted from statements
    val availableMonths = remember(uiState.allStatements) {
        val months = uiState.allStatements.map { getMonthLabel(it.statementDate) }.distinct()
        listOf("ALL") + months
    }

    // Month-wise grouped analysis
    val monthGroups = remember(filteredStatements) {
        filteredStatements
            .groupBy { getMonthLabel(it.statementDate) }
            .map { (monthYear, stmts) ->
                MonthStatementSummary(
                    monthYear = monthYear,
                    totalSpends = stmts.sumOf { it.spends.coerceAtLeast(0.0) },
                    totalDues = stmts.sumOf { it.totalDue },
                    totalCashback = stmts.sumOf { it.cashbackEarned },
                    statementsCount = stmts.size,
                    statements = stmts.sortedByDescending { it.statementDate }
                )
            }
    }

    val totalSpendsAgg = remember(filteredStatements) {
        filteredStatements.sumOf { it.spends.coerceAtLeast(0.0) }
    }
    val totalDuesAgg = remember(filteredStatements) {
        filteredStatements.sumOf { it.totalDue }
    }
    val totalCashbackAgg = remember(filteredStatements) {
        filteredStatements.sumOf { it.cashbackEarned }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Upload Banner & Callout
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Add & Statement Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Upload bank PDF statements to auto-detect cards, extract dues, cashback & spend breakdown.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = onUploadStatement,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Upload PDF", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // 2. Aggregate / Card-Wise Filter Chips
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                Text(
                    text = "Filter by Card:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = uiState.analysisCardFilterId == null,
                            onClick = { onSelectCardFilter(null) },
                            label = { Text("All Cards (${uiState.cards.size} Aggregated)") },
                            leadingIcon = if (uiState.analysisCardFilterId == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                    items(uiState.cards, key = { it.id }) { card ->
                        FilterChip(
                            selected = uiState.analysisCardFilterId == card.id,
                            onClick = { onSelectCardFilter(card.id) },
                            label = { Text("${card.bankName} ••••${card.last4Digits}") },
                            leadingIcon = if (uiState.analysisCardFilterId == card.id) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            } else null
                        )
                    }
                }
            }
        }

        // 3. Month Filter Chips (if multiple months exist)
        if (availableMonths.size > 2) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Text(
                        text = "Filter by Month:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableMonths) { m ->
                            FilterChip(
                                selected = uiState.analysisMonthFilter == m,
                                onClick = { onSelectMonthFilter(m) },
                                label = { Text(if (m == "ALL") "All Months" else m) }
                            )
                        }
                    }
                }
            }
        }

        // 4. Analytics Summary Card (Aggregate / Selected Card)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
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
                        Text(
                            text = if (uiState.analysisCardFilterId == null) "All Cards Overview" else {
                                val c = uiState.cards.firstOrNull { it.id == uiState.analysisCardFilterId }
                                "${c?.bankName ?: ""} ${c?.cardName ?: "Card"} Overview"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${filteredStatements.size} Statement(s)",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Total Spends",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(totalSpendsAgg),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total Dues",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(totalDuesAgg),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (totalDuesAgg > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Cashback Earned",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(totalCashbackAgg),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }
        }

        // 5. Month-Wise Breakdown Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Month-Wise Reports",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${monthGroups.size} Month(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6. Month Groups and Statement Cards
        if (monthGroups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Statements Uploaded Yet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Upload PDF credit card statements to see month-wise spend reports and dues analysis.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onUploadStatement) {
                            Text("Upload First Statement")
                        }
                    }
                }
            }
        } else {
            monthGroups.forEach { group ->
                item(key = group.monthYear) {
                    MonthGroupCard(
                        group = group,
                        cards = uiState.cards,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        onDeleteStatement = onDeleteStatement
                    )
                }
            }
        }
    }
}

@Composable
fun MonthGroupCard(
    group: MonthStatementSummary,
    cards: List<CreditCardEntity>,
    modifier: Modifier = Modifier,
    onDeleteStatement: ((CreditCardStatementEntity) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(true) }
    var statementToDelete by remember { mutableStateOf<CreditCardStatementEntity?>(null) }

    if (statementToDelete != null) {
        val stmt = statementToDelete!!
        val card = cards.firstOrNull { it.id == stmt.cardId }
        AlertDialog(
            onDismissRequest = { statementToDelete = null },
            title = { Text("Delete Statement") },
            text = {
                Text(
                    "Are you sure you want to delete this statement for ${card?.bankName ?: "Card"} ${card?.cardName ?: ""} (${stmt.statementDate})?\n\nThis will remove dues, cashback, and spend calculations associated with this upload."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteStatement?.invoke(stmt)
                        statementToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { statementToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Month Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = group.monthYear,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${group.statementsCount} Statement${if (group.statementsCount > 1) "s" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(group.totalDues),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (group.totalDues > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                        )
                        Text(
                            text = if (group.totalCashback > 0) "+${formatCurrency(group.totalCashback)} cashback" else "Dues",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (group.totalCashback > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expand"
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                group.statements.forEach { stmt ->
                    val linkedCard = cards.firstOrNull { it.id == stmt.cardId }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${linkedCard?.bankName ?: "Card"} ${linkedCard?.cardName ?: ""} ••••${linkedCard?.last4Digits ?: "----"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (stmt.statementDate.isNotBlank()) {
                                    Text(
                                        text = "Statement: ${stmt.statementDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (stmt.dueDate.isNotBlank()) {
                                    Text(
                                        text = " • Due: ${stmt.dueDate}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (stmt.totalDue > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(stmt.totalDue),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (stmt.totalDue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                                )
                                if (stmt.minDue > 0) {
                                    Text(
                                        text = "Min: ${formatCurrency(stmt.minDue)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (onDeleteStatement != null) {
                                IconButton(
                                    onClick = { statementToDelete = stmt },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Delete Statement",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
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

@Composable
fun CreditCardsSummaryCard(
    summary: com.example.data.repository.CreditCardPortfolioSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Credit Limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(summary.totalCreditLimit),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        summary.utilizationPercent < 15.0 -> Color(0xFF10B981).copy(alpha = 0.15f)
                        summary.utilizationPercent < 30.0 -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.15f)
                    }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Utilization",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f%%", summary.utilizationPercent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                summary.utilizationPercent < 15.0 -> Color(0xFF10B981)
                                summary.utilizationPercent < 30.0 -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Utilization Progress Bar
            LinearProgressIndicator(
                progress = { (summary.utilizationPercent / 100.0).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    summary.utilizationPercent < 15.0 -> Color(0xFF10B981)
                    summary.utilizationPercent < 30.0 -> Color(0xFFF59E0B)
                    else -> Color(0xFFEF4444)
                },
                trackColor = MaterialTheme.colorScheme.surfaceDim
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Outstanding Dues",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(summary.totalDue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.totalDue > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Available Limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(summary.availableLimit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun NextDueAlertBanner(
    cardName: String,
    dueDate: String,
    amount: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFEF3C7) // Amber 100
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.NotificationsActive,
                contentDescription = "Payment Alert",
                tint = Color(0xFFB45309), // Amber 700
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Payment Due Soon: $cardName",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF92400E)
                )
                Text(
                    text = "₹${Math.round(amount)} due on $dueDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB45309)
                )
            }
        }
    }
}

@Composable
fun BankFilterRow(
    banks: List<String>,
    selectedBank: String,
    onSelectBank: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(banks) { bank ->
            val isSelected = bank.equals(selectedBank, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onSelectBank(bank) },
                label = { Text(if (bank == "ALL") "All Banks" else bank) },
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
fun CreditCardItem(
    card: CreditCardEntity,
    onUploadStatement: () -> Unit,
    onPayBill: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Determine card gradient colors based on bank
    val cardGradients = getBankGradient(card.bankName)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("credit_card_${card.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Card Metallic Gradient Face
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(cardGradients))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = card.bankName.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = card.cardName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        // Network Chip
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = card.cardNetwork,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Masked Card Number
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "••••  ••••  ••••  ${card.last4Digits}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 2.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Dues and Limit
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DUE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = if (card.totalDue > 0) formatCurrency(card.totalDue) else "₹0.00 (Cleared)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (card.totalDue > 0) Color(0xFFFCA5A5) else Color(0xFF86EFAC)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "LIMIT",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = formatCurrency(card.creditLimit),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Lower Action Bar & Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(14.dp)
            ) {
                // Dates and Password status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        // Explicit Billing Cycle & Billing Date
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Billing Date: ${card.billingCycleDay}th of month",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (card.dueDate.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Event,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = if (card.totalDue > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Due: ${card.dueDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (card.totalDue > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (card.totalDue > 0) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Password auto-pick tag
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (card.savedPassword.isNotBlank()) "Password Ready" else "Auto-Protected",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Upload Statement PDF
                    Button(
                        onClick = onUploadStatement,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("upload_statement_${card.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "Upload PDF",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Upload Statement", style = MaterialTheme.typography.labelMedium)
                    }

                    // Clear / Pay Bill (if due)
                    if (card.totalDue > 0) {
                        FilledTonalButton(
                            onClick = onPayBill,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Pay Bill",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pay Bill", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    // Edit
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "Edit Card",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Delete
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete Card",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadStatementModal(
    card: CreditCardEntity?,
    password: String,
    isAutoPicked: Boolean,
    sourceLabel: String,
    rememberPassword: Boolean,
    parsedPreview: ParsedStatementResult?,
    isParsing: Boolean,
    onPasswordChange: (String) -> Unit,
    onToggleRemember: (Boolean) -> Unit,
    onFileSelected: (Uri) -> Unit,
    onConfirmSave: (statementDate: String, dueDate: String, totalDue: Double, minDue: Double, cashback: Double, pts: Int, fileName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedFileName by remember { mutableStateOf("") }

    // Editable fields for preview confirmation
    var statementDateInput by remember(parsedPreview) { mutableStateOf(parsedPreview?.statementDate ?: "") }
    var dueDateInput by remember(parsedPreview) { mutableStateOf(parsedPreview?.dueDate ?: "") }
    var totalDueInput by remember(parsedPreview) { mutableStateOf(if (parsedPreview != null) "${parsedPreview.totalDue}" else "") }
    var minDueInput by remember(parsedPreview) { mutableStateOf(if (parsedPreview != null) "${parsedPreview.minDue}" else "") }
    var cashbackInput by remember(parsedPreview) { mutableStateOf(if (parsedPreview != null) "${parsedPreview.cashbackEarned}" else "0") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedFileName = uri.lastPathSegment?.split("/")?.lastOrNull() ?: "Statement.pdf"
            onFileSelected(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Upload Credit Card Statement",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (card != null) {
                            "${card.bankName} • ${card.cardName} (•••• ${card.last4Digits})"
                        } else if (parsedPreview != null) {
                            "Detected: ${parsedPreview.detectedBankName} (•••• ${parsedPreview.last4Digits})"
                        } else {
                            "Auto-detects card & details from PDF"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Password Configuration
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PDF Password Protection",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isAutoPicked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = if (isAutoPicked) "Auto-Picked Saved" else "Auto-Generated",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAutoPicked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("Statement PDF Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("statement_password_input"),
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Rule: ${sourceLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberPassword,
                            onCheckedChange = onToggleRemember,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val bankLabel = card?.bankName ?: (parsedPreview?.detectedBankName ?: "this bank")
                        Text(
                            text = "Save this password for future $bankLabel statements",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 2: File Picker Button
            if (parsedPreview == null && !isParsing) {
                OutlinedButton(
                    onClick = { filePickerLauncher.launch("application/pdf") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("choose_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Credit Card PDF Statement")
                }
            } else if (isParsing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Analyzing PDF & extracting statement date and dues...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Step 3: Extracted Preview Review
            if (parsedPreview != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Statement Parsed Successfully",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46)
                                )
                            }
                            if (parsedPreview.last4Digits.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = "•••• ${parsedPreview.last4Digits}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (parsedPreview.isCreditBalance) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = Color(0xFF047857),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Credit Balance: ₹${parsedPreview.creditBalanceAmount} CR (Excess credit / refunds. ₹0 payment required)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF065F46),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Extracted Date and Due Date fields
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = statementDateInput,
                                onValueChange = { statementDateInput = it },
                                label = { Text("Statement Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = dueDateInput,
                                onValueChange = { dueDateInput = it },
                                label = { Text("Payment Due Date") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Amounts
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = totalDueInput,
                                onValueChange = { totalDueInput = it },
                                label = { Text("Total Due (₹)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = minDueInput,
                                onValueChange = { minDueInput = it },
                                label = { Text("Min Due (₹)") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cashbackInput,
                            onValueChange = { cashbackInput = it },
                            label = { Text("Statement Cashback Earned (₹)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                val totalDueVal = totalDueInput.toDoubleOrNull() ?: parsedPreview.totalDue
                                val minDueVal = minDueInput.toDoubleOrNull() ?: parsedPreview.minDue
                                val cbVal = cashbackInput.toDoubleOrNull() ?: parsedPreview.cashbackEarned
                                onConfirmSave(
                                    statementDateInput.ifBlank { parsedPreview.statementDate },
                                    dueDateInput.ifBlank { parsedPreview.dueDate },
                                    totalDueVal,
                                    minDueVal,
                                    cbVal,
                                    parsedPreview.rewardPoints,
                                    selectedFileName.ifBlank { "Statement.pdf" }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("confirm_save_statement_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Confirm & Save Statement Dues")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddEditCardDialog(
    editingCard: CreditCardEntity?,
    onDismiss: () -> Unit,
    onSave: (CreditCardEntity) -> Unit
) {
    var bankName by remember { mutableStateOf(editingCard?.bankName ?: "ICICI Bank") }
    var cardName by remember { mutableStateOf(editingCard?.cardName ?: "") }
    var cardType by remember { mutableStateOf(editingCard?.cardType ?: "Cashback") }
    var last4Digits by remember { mutableStateOf(editingCard?.last4Digits ?: "") }
    var creditLimit by remember { mutableStateOf(if (editingCard != null) "${editingCard.creditLimit}" else "150000") }
    var totalDue by remember { mutableStateOf(if (editingCard != null) "${editingCard.totalDue}" else "0") }
    var billingCycleDay by remember { mutableStateOf(if (editingCard != null) "${editingCard.billingCycleDay}" else "15") }
    var cardNetwork by remember { mutableStateOf(editingCard?.cardNetwork ?: "VISA") }
    var savedPassword by remember { mutableStateOf(editingCard?.savedPassword ?: "") }

    val commonBanks = listOf("ICICI Bank", "HDFC Bank", "Axis Bank", "Citi Bank", "IndusInd Bank", "Kotak Mahindra Bank", "IDFC FIRST Bank", "AU Small Finance Bank", "SBI Card")
    val cardTypes = listOf("Cashback", "Shopping", "Travel", "Rewards", "UPI RuPay", "Lifestyle", "Fuel")
    val networks = listOf("VISA", "Mastercard", "RuPay", "Diners", "Amex")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingCard == null) "Add Credit Card" else "Edit Credit Card") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Bank Name Dropdown / Selector
                Text("Bank Name", style = MaterialTheme.typography.labelSmall)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(commonBanks) { b ->
                        FilterChip(
                            selected = bankName == b,
                            onClick = { bankName = b },
                            label = { Text(b, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cardName,
                    onValueChange = { cardName = it },
                    label = { Text("Card Name (e.g. Amazon Pay, Swiggy)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = last4Digits,
                        onValueChange = { if (it.length <= 4) last4Digits = it },
                        label = { Text("Last 4 Digits") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = { creditLimit = it },
                        label = { Text("Credit Limit (₹)") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalDue,
                        onValueChange = { totalDue = it },
                        label = { Text("Current Due (₹)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = billingCycleDay,
                        onValueChange = { billingCycleDay = it },
                        label = { Text("Bill Day (1-31)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Network Selector
                Text("Card Network", style = MaterialTheme.typography.labelSmall)
                LazyRow(
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(networks) { net ->
                        FilterChip(
                            selected = cardNetwork == net,
                            onClick = { cardNetwork = net },
                            label = { Text(net, fontSize = 12.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = savedPassword,
                    onValueChange = { savedPassword = it },
                    label = { Text("Saved PDF Password (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limitVal = creditLimit.toDoubleOrNull() ?: 100000.0
                    val dueVal = totalDue.toDoubleOrNull() ?: 0.0
                    val dayVal = billingCycleDay.toIntOrNull() ?: 15

                    val entity = (editingCard ?: CreditCardEntity()).copy(
                        bankName = bankName.trim(),
                        cardName = cardName.trim().ifBlank { "Credit Card" },
                        cardType = cardType,
                        last4Digits = last4Digits.trim().padStart(4, '0'),
                        creditLimit = limitVal,
                        availableLimit = (limitVal - dueVal).coerceAtLeast(0.0),
                        totalDue = dueVal,
                        minDue = if (dueVal > 0) Math.round(dueVal * 0.05).toDouble() else 0.0,
                        billingCycleDay = dayVal,
                        cardNetwork = cardNetwork,
                        savedPassword = savedPassword.trim()
                    )
                    onSave(entity)
                }
            ) {
                Text("Save Card")
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
fun PayBillDialog(
    card: CreditCardEntity,
    onDismiss: () -> Unit,
    onConfirmPay: (Double) -> Unit
) {
    var payAmount by remember { mutableStateOf("${card.totalDue}") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record Payment for ${card.cardName}") },
        text = {
            Column {
                Text(
                    text = "Total Outstanding: ${formatCurrency(card.totalDue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = payAmount,
                    onValueChange = { payAmount = it },
                    label = { Text("Amount Paid (₹)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { payAmount = "${card.totalDue}" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Pay Full")
                    }
                    if (card.minDue > 0) {
                        FilledTonalButton(
                            onClick = { payAmount = "${card.minDue}" },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pay Min")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = payAmount.toDoubleOrNull() ?: card.totalDue
                    onConfirmPay(amt)
                }
            ) {
                Text("Confirm Payment")
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
fun EmptyCardsPlaceholder(onAddCard: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.CreditCard,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Credit Cards Found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Add your credit cards to manage statements, dues & passwords",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddCard) {
                Text("Add Your First Card")
            }
        }
    }
}

// Helpers
fun getBankGradient(bankName: String): List<Color> {
    val b = bankName.uppercase()
    return when {
        b.contains("ICICI") -> listOf(Color(0xFF9A3412), Color(0xFFC2410C), Color(0xFFEA580C))
        b.contains("HDFC") -> listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF2563EB))
        b.contains("AXIS") -> listOf(Color(0xFF831843), Color(0xFF9F1239), Color(0xFFBE123C))
        b.contains("CITI") -> listOf(Color(0xFF0F172A), Color(0xFF0369A1), Color(0xFF0284C7))
        b.contains("INDUS") -> listOf(Color(0xFF713F12), Color(0xFF854D0E), Color(0xFFA16207))
        b.contains("KOTAK") -> listOf(Color(0xFF7F1D1D), Color(0xFF991B1B), Color(0xFFDC2626))
        b.contains("IDFC") -> listOf(Color(0xFF881337), Color(0xFF9F1239), Color(0xFFE11D48))
        b.contains("AU") -> listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF059669))
        else -> listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF475569))
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    format.maximumFractionDigits = 0
    return format.format(amount)
}
