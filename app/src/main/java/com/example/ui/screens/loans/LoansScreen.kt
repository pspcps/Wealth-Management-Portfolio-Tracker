package com.example.ui.screens.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.LoanEntity
import com.example.domain.model.LoanPortfolioSummary
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.GlobalPrivacyEyeActionIcon
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    viewModel: LoansViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCalculators: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: My Loans, 1: Prepayment Planner, 2: Amortization

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Loans & Liabilities",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "${state.summary.activeLoansCount} Active · Total Debt ${CurrencyFormatter.formatInrCompact(state.summary.totalOutstanding)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_loans_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HighDensityTextPrimary
                        )
                    }
                },
                actions = {
                    GlobalPrivacyEyeActionIcon(testTag = "btn_loans_privacy_eye")
                    IconButton(
                        onClick = onNavigateToCalculators,
                        modifier = Modifier.testTag("btn_loans_calculators")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "EMI Calculators",
                            tint = HighDensityTextPrimary
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openAddLoan() },
                        modifier = Modifier.testTag("btn_add_loan")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Loan",
                            tint = HighDensityPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddLoan() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Loan") },
                containerColor = HighDensityPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_loan")
            )
        },
        containerColor = HighDensityBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Portfolio Debt Summary Banner
            LoanPortfolioSummaryBanner(summary = state.summary)

            // Segmented Navigation Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = HighDensityPrimary,
                divider = { HorizontalDivider(color = HighDensityBorder) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "My Loans (${state.loans.size})",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "Prepay & Save",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = {
                        Text(
                            "Schedule",
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Loans List
                    if (state.loans.isEmpty()) {
                        EmptyLoansView(onAddClick = { viewModel.openAddLoan() })
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.loans, key = { it.id }) { loan ->
                                LoanCard(
                                    loan = loan,
                                    isSelected = state.selectedLoan?.id == loan.id,
                                    onSelect = { viewModel.selectLoan(loan) },
                                    onEdit = { viewModel.openEditLoan(loan) },
                                    onPrepay = { viewModel.openPrepayDialog(loan) },
                                    onDelete = { viewModel.deleteLoan(loan.id) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(72.dp)) }
                        }
                    }
                }
                1 -> {
                    // Prepayment Simulator
                    PrepaymentPlannerView(
                        state = state,
                        onUpdateSimulation = { extra, lump ->
                            viewModel.updatePrepaymentSimulation(extra, lump)
                        },
                        onSelectLoan = { loan ->
                            viewModel.selectLoan(loan)
                        },
                        onOpenPrepayDialog = { loan ->
                            viewModel.openPrepayDialog(loan)
                        }
                    )
                }
                2 -> {
                    // Amortization Schedule
                    AmortizationScheduleView(
                        state = state,
                        onSelectLoan = { loan -> viewModel.selectLoan(loan) }
                    )
                }
            }
        }
    }

    // Bottom Sheets
    if (state.isAddEditOpen) {
        AddEditLoanSheet(
            loan = state.editingLoan,
            onDismiss = { viewModel.closeAddEdit() },
            onSave = { loan -> viewModel.saveLoan(loan) }
        )
    }

    if (state.isPrepayDialogOpen && state.selectedLoan != null) {
        RecordPrepaymentSheet(
            loan = state.selectedLoan!!,
            onDismiss = { viewModel.closePrepayDialog() },
            onApply = { amount -> viewModel.applyPrepayment(state.selectedLoan!!.id, amount) }
        )
    }
}

@Composable
private fun LoanPortfolioSummaryBanner(summary: LoanPortfolioSummary) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HighDensityBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL OUTSTANDING DEBT",
                    style = MaterialTheme.typography.labelSmall,
                    color = HighDensityTextSecondary,
                    fontWeight = FontWeight.SemiBold
                )
                if (summary.totalOutstanding > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Text(
                            text = "Avg ${String.format("%.2f", summary.weightedAverageInterestRate)}% p.a.",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityExpenseRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = "Debt-Free",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityIncomeGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = CurrencyFormatter.formatInr(summary.totalOutstanding),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = HighDensityExpenseRed
            )

            HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Monthly EMI Obligation",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "${CurrencyFormatter.formatInr(summary.totalMonthlyEmi)}/mo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Original Principal",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = CurrencyFormatter.formatInrCompact(summary.totalOriginalPrincipal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun LoanCard(
    loan: LoanEntity,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onPrepay: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    val paidPercent = if (loan.originalPrincipal > 0) {
        val paid = (loan.originalPrincipal - loan.currentOutstanding).coerceAtLeast(0.0)
        (paid / loan.originalPrincipal).toFloat().coerceIn(0f, 1f)
    } else 0f

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("loan_card_${loan.id}"),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(
            1.dp,
            if (isSelected) HighDensityPrimary else HighDensityBorder
        ),
        shadowElevation = if (isSelected) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(getLoanColorBg(loan.loanType)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getLoanIcon(loan.loanType),
                            contentDescription = null,
                            tint = getLoanColor(loan.loanType),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = loan.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${loan.lenderBank} · ${getLoanTypeDisplayName(loan.loanType)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Prepay Principal") },
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onPrepay()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Details") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Loan", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Outstanding Amount & Interest
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Current Outstanding",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(loan.currentOutstanding),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityExpenseRed
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Monthly EMI",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "${CurrencyFormatter.formatInr(loan.monthlyEmi)}/mo",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }
            }

            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { paidPercent },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = HighDensityIncomeGreen,
                    trackColor = HighDensityBorder
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${(paidPercent * 100).roundToInt()}% Repaid",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityIncomeGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${loan.remainingTenureMonths} mos left · ${loan.annualInterestRate}% p.a.",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                }
            }

            // Bottom action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loan.emiDebitDay > 0) {
                    Text(
                        text = "Due on ${loan.emiDebitDay}${getDaySuffix(loan.emiDebitDay)} of month",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                OutlinedButton(
                    onClick = onPrepay,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp),
                    border = BorderStroke(1.dp, HighDensityPrimary)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = HighDensityPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Prepay",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HighDensityPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun PrepaymentPlannerView(
    state: LoansUiState,
    onUpdateSimulation: (extraMonthly: Double, lumpSum: Double) -> Unit,
    onSelectLoan: (LoanEntity) -> Unit,
    onOpenPrepayDialog: (LoanEntity) -> Unit
) {
    var extraMonthlyText by remember { mutableStateOf("5000") }
    var lumpSumText by remember { mutableStateOf("0") }

    val activeLoans = state.loans.filter { it.isActive }

    if (activeLoans.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No active loans available for simulation", color = HighDensityTextSecondary)
        }
        return
    }

    val selectedLoan = state.selectedLoan ?: activeLoans.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Loan selector chips
        Text(
            text = "SELECT LOAN TO SIMULATE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = HighDensityTextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeLoans.forEach { loan ->
                FilterChip(
                    selected = loan.id == selectedLoan.id,
                    onClick = { onSelectLoan(loan) },
                    label = { Text(loan.name, maxLines = 1) },
                    leadingIcon = if (loan.id == selectedLoan.id) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Savings impact card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF0FDF4),
            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = null,
                        tint = HighDensityIncomeGreen
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estimated Interest Savings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityIncomeGreen
                    )
                }

                Text(
                    text = CurrencyFormatter.formatInr(state.prepaymentScenario.totalInterestSaved),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityIncomeGreen
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tenure Reduced by:",
                        style = MaterialTheme.typography.bodySmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "${state.prepaymentScenario.monthsSaved} Months (${String.format("%.1f", state.prepaymentScenario.monthsSaved / 12.0)} Years)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "New Payoff Tenure:",
                        style = MaterialTheme.typography.bodySmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "${state.prepaymentScenario.revisedTenureMonths} Months (${String.format("%.1f", state.prepaymentScenario.revisedTenureMonths / 12.0)} Years)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }
            }
        }

        // Inputs for Simulation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, HighDensityBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Prepayment Strategy Inputs",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )

                OutlinedTextField(
                    value = extraMonthlyText,
                    onValueChange = {
                        extraMonthlyText = it
                        val extra = it.toDoubleOrNull() ?: 0.0
                        val lump = lumpSumText.toDoubleOrNull() ?: 0.0
                        onUpdateSimulation(extra, lump)
                    },
                    label = { Text("Extra Monthly EMI Contribution (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lumpSumText,
                    onValueChange = {
                        lumpSumText = it
                        val extra = extraMonthlyText.toDoubleOrNull() ?: 0.0
                        val lump = it.toDoubleOrNull() ?: 0.0
                        onUpdateSimulation(extra, lump)
                    },
                    label = { Text("One-time Lump Sum Prepayment (₹)") },
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = { onOpenPrepayDialog(selectedLoan) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Apply Actual Prepayment Now")
                }
            }
        }
        Spacer(modifier = Modifier.height(60.dp))
    }
}

@Composable
private fun AmortizationScheduleView(
    state: LoansUiState,
    onSelectLoan: (LoanEntity) -> Unit
) {
    val activeLoans = state.loans.filter { it.isActive }
    if (activeLoans.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active loans", color = HighDensityTextSecondary)
        }
        return
    }

    val selectedLoan = state.selectedLoan ?: activeLoans.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Loan selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            activeLoans.forEach { loan ->
                FilterChip(
                    selected = loan.id == selectedLoan.id,
                    onClick = { onSelectLoan(loan) },
                    label = { Text(loan.name) }
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            border = BorderStroke(1.dp, HighDensityBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mo", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HighDensityTextSecondary, modifier = Modifier.width(30.dp))
                Text("Principal", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HighDensityIncomeGreen, modifier = Modifier.weight(1f))
                Text("Interest", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HighDensityExpenseRed, modifier = Modifier.weight(1f))
                Text("Ending Bal", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = HighDensityTextPrimary, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(state.amortizationSchedule, key = { it.monthNumber }) { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, HighDensityBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#${item.monthNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HighDensityTextSecondary,
                            modifier = Modifier.width(30.dp)
                        )
                        Text(
                            text = CurrencyFormatter.formatInrCompact(item.principalComponent),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = HighDensityIncomeGreen,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = CurrencyFormatter.formatInrCompact(item.interestComponent),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = HighDensityExpenseRed,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = CurrencyFormatter.formatInrCompact(item.endingBalance),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditLoanSheet(
    loan: LoanEntity?,
    onDismiss: () -> Unit,
    onSave: (LoanEntity) -> Unit
) {
    var name by remember { mutableStateOf(loan?.name ?: "") }
    var loanType by remember { mutableStateOf(loan?.loanType ?: "HOME_LOAN") }
    var lenderBank by remember { mutableStateOf(loan?.lenderBank ?: "") }
    var originalPrincipal by remember { mutableStateOf(if (loan != null && loan.originalPrincipal > 0) loan.originalPrincipal.toString() else "") }
    var currentOutstanding by remember { mutableStateOf(if (loan != null && loan.currentOutstanding > 0) loan.currentOutstanding.toString() else "") }
    var annualInterestRate by remember { mutableStateOf(if (loan != null && loan.annualInterestRate > 0) loan.annualInterestRate.toString() else "8.5") }
    var totalTenureMonths by remember { mutableStateOf(if (loan != null && loan.totalTenureMonths > 0) loan.totalTenureMonths.toString() else "240") }
    var remainingTenureMonths by remember { mutableStateOf(if (loan != null && loan.remainingTenureMonths > 0) loan.remainingTenureMonths.toString() else "180") }
    var monthlyEmi by remember { mutableStateOf(if (loan != null && loan.monthlyEmi > 0) loan.monthlyEmi.toString() else "") }
    var emiDebitDay by remember { mutableStateOf(if (loan != null && loan.emiDebitDay > 0) loan.emiDebitDay.toString() else "5") }
    var notes by remember { mutableStateOf(loan?.notes ?: "") }

    val allLoanTypes = listOf(
        "HOME_LOAN" to "Home Loan",
        "CAR_LOAN" to "Car / Auto Loan",
        "PERSONAL_LOAN" to "Personal Loan",
        "EDUCATION_LOAN" to "Education Loan",
        "CREDIT_CARD_EMI" to "Credit Card EMI",
        "GOLD_LOAN" to "Gold Loan",
        "OTHER" to "Other Debt"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HighDensityPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getLoanIcon(loanType),
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (loan == null) "Add New Loan / Liability" else "Edit Loan Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Track interest, EMIs, and debt-free timeline",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                }
            }

            HorizontalDivider(color = HighDensityBorder)

            // Scrollable Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Basic Info
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Loan Name (e.g. Home Loan - 3BHK)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Selection with LazyRow
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Loan Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HighDensityTextSecondary
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allLoanTypes) { (type, label) ->
                            val isSelected = loanType == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { loanType = type },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getLoanIcon(type),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) HighDensityPrimary else HighDensityTextSecondary
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HighDensityPrimaryContainer,
                                    selectedLabelColor = HighDensityPrimary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = lenderBank,
                    onValueChange = { lenderBank = it },
                    label = { Text("Lender / Bank Name (e.g. HDFC Bank, SBI)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Amounts & Interest
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = originalPrincipal,
                        onValueChange = { originalPrincipal = it },
                        label = { Text("Principal (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = currentOutstanding,
                        onValueChange = { currentOutstanding = it },
                        label = { Text("Outstanding (₹)*") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityExpenseRed) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = annualInterestRate,
                        onValueChange = { annualInterestRate = it },
                        label = { Text("Interest Rate (% p.a.)*") },
                        trailingIcon = { Text("%", fontWeight = FontWeight.Bold, color = HighDensityTextSecondary, modifier = Modifier.padding(end = 8.dp)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = monthlyEmi,
                        onValueChange = { monthlyEmi = it },
                        label = { Text("Monthly EMI (₹)") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Helper auto-calculate EMI button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = {
                            val p = currentOutstanding.toDoubleOrNull() ?: originalPrincipal.toDoubleOrNull() ?: 0.0
                            val r = annualInterestRate.toDoubleOrNull() ?: 0.0
                            val n = remainingTenureMonths.toIntOrNull() ?: totalTenureMonths.toIntOrNull() ?: 0
                            if (p > 0 && r > 0 && n > 0) {
                                val emi = com.example.domain.finance.LoanCalculators.calculateEmi(p, r, n)
                                monthlyEmi = emi.roundToInt().toString()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto-Calculate EMI", fontSize = 12.sp)
                    }
                }

                // 3. Tenure & Day
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = totalTenureMonths,
                        onValueChange = { totalTenureMonths = it },
                        label = { Text("Total Tenure (mos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = remainingTenureMonths,
                        onValueChange = { remainingTenureMonths = it },
                        label = { Text("Remaining (mos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = emiDebitDay,
                    onValueChange = { emiDebitDay = it },
                    label = { Text("EMI Auto-Debit Day (1 - 31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Tax Exemptions (Section 24b, 80EEA, 80E)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            HorizontalDivider(color = HighDensityBorder)

            // Fixed Sticky Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val origP = originalPrincipal.toDoubleOrNull() ?: 0.0
                            val outP = currentOutstanding.toDoubleOrNull() ?: origP
                            val rate = annualInterestRate.toDoubleOrNull() ?: 8.5
                            val emiVal = monthlyEmi.toDoubleOrNull() ?: 0.0
                            val totalT = totalTenureMonths.toIntOrNull() ?: 120
                            val remT = remainingTenureMonths.toIntOrNull() ?: totalT
                            val debitD = emiDebitDay.toIntOrNull() ?: 5

                            val updated = (loan ?: LoanEntity()).copy(
                                name = name.trim(),
                                loanType = loanType,
                                lenderBank = lenderBank.trim(),
                                originalPrincipal = origP,
                                currentOutstanding = outP,
                                annualInterestRate = rate,
                                monthlyEmi = emiVal,
                                totalTenureMonths = totalT,
                                remainingTenureMonths = remT,
                                emiDebitDay = debitD,
                                notes = notes.trim(),
                                isActive = outP > 0
                            )
                            onSave(updated)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    enabled = name.isNotBlank() && (currentOutstanding.isNotBlank() || originalPrincipal.isNotBlank())
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Loan")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordPrepaymentSheet(
    loan: LoanEntity,
    onDismiss: () -> Unit,
    onApply: (amount: Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Record Loan Prepayment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                    Text(
                        text = "Principal lump-sum prepayment for '${loan.name}'",
                        style = MaterialTheme.typography.bodySmall,
                        color = HighDensityTextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = HighDensityPrimaryContainer.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Outstanding", fontSize = 13.sp, color = HighDensityTextSecondary)
                    Text(
                        CurrencyFormatter.formatInr(loan.currentOutstanding),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityPrimary
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Prepayment Amount (₹)*") },
                leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Quick amount chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(25000.0, 50000.0, 100000.0, 200000.0).forEach { quickAmt ->
                    SuggestionChip(
                        onClick = { amountText = quickAmt.toLong().toString() },
                        label = { Text("₹${(quickAmt / 1000).toInt()}k", fontSize = 12.sp) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            onApply(amt)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    enabled = (amountText.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Deduct Balance")
                }
            }
        }
    }
}

@Composable
private fun EmptyLoansView(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFEE2E2)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = HighDensityExpenseRed,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "No Loans or Liabilities Added",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HighDensityTextPrimary
            )
            Text(
                text = "Track your Home Loan, Car Loan, EMIs, simulate prepayments and see how fast you can become debt-free.",
                style = MaterialTheme.typography.bodySmall,
                color = HighDensityTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Your First Loan")
            }
        }
    }
}

private fun getLoanIcon(type: String): ImageVector {
    return when (type) {
        "HOME_LOAN" -> Icons.Default.Home
        "CAR_LOAN" -> Icons.Default.DirectionsCar
        "PERSONAL_LOAN" -> Icons.Default.Person
        "EDUCATION_LOAN" -> Icons.Default.School
        "CREDIT_CARD_EMI" -> Icons.Default.CreditCard
        "GOLD_LOAN" -> Icons.Default.WorkspacePremium
        else -> Icons.Default.AccountBalance
    }
}

private fun getLoanColor(type: String): Color {
    return when (type) {
        "HOME_LOAN" -> Color(0xFF1E40AF)
        "CAR_LOAN" -> Color(0xFF0284C7)
        "PERSONAL_LOAN" -> Color(0xFF9333EA)
        "EDUCATION_LOAN" -> Color(0xFF0D9488)
        "CREDIT_CARD_EMI" -> Color(0xFFE11D48)
        else -> HighDensityPrimary
    }
}

private fun getLoanColorBg(type: String): Color {
    return when (type) {
        "HOME_LOAN" -> Color(0xFFDBEAFE)
        "CAR_LOAN" -> Color(0xFFE0F2FE)
        "PERSONAL_LOAN" -> Color(0xFFF3E8FF)
        "EDUCATION_LOAN" -> Color(0xFFCCFBF1)
        "CREDIT_CARD_EMI" -> Color(0xFFFFE4E6)
        else -> HighDensityPrimaryContainer
    }
}

private fun getLoanTypeDisplayName(type: String): String {
    return when (type) {
        "HOME_LOAN" -> "Home Loan"
        "CAR_LOAN" -> "Car Loan"
        "PERSONAL_LOAN" -> "Personal Loan"
        "EDUCATION_LOAN" -> "Education Loan"
        "CREDIT_CARD_EMI" -> "Card EMI"
        "GOLD_LOAN" -> "Gold Loan"
        else -> "Other Loan"
    }
}

private fun getDaySuffix(day: Int): String {
    return when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}
