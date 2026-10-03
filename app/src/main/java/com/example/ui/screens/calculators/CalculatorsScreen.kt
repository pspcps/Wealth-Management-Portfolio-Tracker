package com.example.ui.screens.calculators

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlobalPrivacyEyeActionIcon
import com.example.ui.screens.calculators.views.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorsScreen(
    viewModel: CalculatorsViewModel = viewModel(),
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val prefillData by viewModel.prefillState.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf(CalculatorCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var activeCalculator by remember { mutableStateOf<CalculatorType?>(null) }

    val allCalculators = remember { CalculatorType.values().toList() }

    val filteredCalculators = remember(selectedCategory, searchQuery) {
        allCalculators.filter { calc ->
            val matchesCategory = (selectedCategory == CalculatorCategory.ALL) || (calc.category == selectedCategory)
            val matchesSearch = searchQuery.isBlank() ||
                    calc.title.contains(searchQuery, ignoreCase = true) ||
                    calc.shortName.contains(searchQuery, ignoreCase = true) ||
                    calc.description.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = if (onNavigateBack != null) {
                    {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = HighDensityTextPrimary
                            )
                        }
                    }
                } else {
                    {}
                },
                title = {
                    Column {
                        Text(
                            text = "Financial Calculators",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "${allCalculators.size} Wealth & Planning Tools",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary
                        )
                    }
                },
                actions = {
                    GlobalPrivacyEyeActionIcon(testTag = "btn_calculators_privacy_eye")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Box
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search any calculator (e.g. SIP, Tax, FIRE, PPF, EMI)...", fontSize = 13.5.sp, color = Color(0xFF94A3B8)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B))
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color(0xFF64748B))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HighDensityPrimary,
                    unfocusedBorderColor = Color(0xFFE2E8F0),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(CalculatorCategory.values()) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = {
                            Text(
                                text = category.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = HighDensityPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                            labelColor = HighDensityTextPrimary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = Color(0xFFE2E8F0),
                            selectedBorderColor = HighDensityPrimary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Featured Hero Card if on ALL and no search query
            if (selectedCategory == CalculatorCategory.ALL && searchQuery.isBlank()) {
                FeaturedCalculatorBanner(
                    onTargetSipClick = { activeCalculator = CalculatorType.TARGET_SIP },
                    onFireClick = { activeCalculator = CalculatorType.FIRE }
                )
            }

            // Calculators Grid
            if (filteredCalculators.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No calculators match \"$searchQuery\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HighDensityTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredCalculators, key = { it.id }) { calc ->
                        GrowwCalculatorCard(
                            calculator = calc,
                            onClick = { activeCalculator = calc }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet Dialog
    activeCalculator?.let { calc ->
        CalculatorDetailBottomSheet(
            calculator = calc,
            prefillData = prefillData,
            onDismiss = { activeCalculator = null }
        )
    }
}

@Composable
private fun FeaturedCalculatorBanner(
    onTargetSipClick: () -> Unit,
    onFireClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Goal Target SIP Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF0284C7),
            modifier = Modifier
                .weight(1f)
                .clickable { onTargetSipClick() }
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "NEW",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "Goal Target SIP",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Find SIP needed for your exact target amount",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // FIRE Planner Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFEA580C),
            modifier = Modifier
                .weight(1f)
                .clickable { onFireClick() }
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "POPULAR",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "FIRE Freedom",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "25x rule, freedom age & portfolio compounding",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, lineHeight = 14.sp),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GrowwCalculatorCard(
    calculator: CalculatorType,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(calculator.accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = calculator.icon,
                    contentDescription = calculator.title,
                    tint = calculator.accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = calculator.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    calculator.badge?.let { badgeText ->
                        Surface(
                            shape = CircleShape,
                            color = calculator.accentColor.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = calculator.accentColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = calculator.description,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 15.sp),
                    color = HighDensityTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalculatorDetailBottomSheet(
    calculator: CalculatorType,
    prefillData: CalculatorAutoPrefillData,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(calculator.accentColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = calculator.icon,
                            contentDescription = null,
                            tint = calculator.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = calculator.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = calculator.category.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Dynamic Calculator Renderer
            when (calculator) {
                // SIP & Growth
                CalculatorType.TARGET_SIP -> TargetGoalSipCalculatorView(prefillData)
                CalculatorType.SIP -> SipCalculatorView(prefillData)
                CalculatorType.STEP_UP_SIP -> StepUpSipCalculatorView(prefillData)
                CalculatorType.LUMPSUM -> LumpsumCalculatorView(prefillData)
                CalculatorType.MF_RETURNS -> MfReturnsCalculatorView(prefillData)
                CalculatorType.CAGR -> CagrCalculatorView(prefillData)
                CalculatorType.XIRR -> XirrCalculatorView(prefillData)
                CalculatorType.STOCK_AVERAGE -> StockAverageCalculatorView(prefillData)
                CalculatorType.ROI -> RoiCalculatorView(prefillData)

                // Retirement & FIRE
                CalculatorType.FIRE -> FireCalculatorView(prefillData)
                CalculatorType.RETIREMENT -> RetirementPlannerCalculatorView(prefillData)
                CalculatorType.SWP -> SwpCalculatorView(prefillData)
                CalculatorType.NPS -> NpsCalculatorView(prefillData)
                CalculatorType.APY -> ApyCalculatorView(prefillData)
                CalculatorType.EPF -> EpfCalculatorView(prefillData)
                CalculatorType.GRATUITY -> GratuityCalculatorView(prefillData)

                // Govt Schemes
                CalculatorType.PPF -> PpfCalculatorView(prefillData)
                CalculatorType.SSY -> SsyCalculatorView(prefillData)
                CalculatorType.NSC -> NscCalculatorView(prefillData)
                CalculatorType.SCSS -> ScssCalculatorView(prefillData)
                CalculatorType.POST_OFFICE_MIS -> PostOfficeMisCalculatorView(prefillData)
                CalculatorType.FD -> FdCalculatorView(prefillData)
                CalculatorType.RD -> RdCalculatorView(prefillData)

                // Loans & Banking
                CalculatorType.EMI -> EmiCalculatorView(prefillData)
                CalculatorType.HOME_LOAN_EMI -> HomeLoanEmiCalculatorView(prefillData)
                CalculatorType.CAR_LOAN_EMI -> CarLoanEmiCalculatorView(prefillData)
                CalculatorType.FLAT_VS_REDUCING -> FlatVsReducingRateCalculatorView(prefillData)
                CalculatorType.SIMPLE_INTEREST -> SimpleInterestCalculatorView(prefillData)
                CalculatorType.COMPOUND_INTEREST -> CompoundInterestCalculatorView(prefillData)

                // Tax & Salary
                CalculatorType.INCOME_TAX -> IncomeTaxCalculatorView(prefillData)
                CalculatorType.SALARY -> SalaryTakeHomeCalculatorView(prefillData)
                CalculatorType.HRA -> HraCalculatorView(prefillData)
                CalculatorType.GST -> GstCalculatorView(prefillData)
                CalculatorType.TDS -> TdsCalculatorView(prefillData)
                CalculatorType.BROKERAGE -> BrokerageChargesCalculatorView(prefillData)
                CalculatorType.MARGIN -> MarginCalculatorView(prefillData)
                CalculatorType.INFLATION -> InflationPurchasingPowerCalculatorView(prefillData)
            }
        }
    }
}
