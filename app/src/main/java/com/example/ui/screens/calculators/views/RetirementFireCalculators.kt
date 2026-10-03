package com.example.ui.screens.calculators.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurrencyFormatter
import com.example.ui.screens.calculators.CalculatorAutoPrefillData
import com.example.ui.screens.calculators.components.*
import com.example.ui.theme.*
import kotlin.math.pow

// -------------------------------------------------------------
// 1. FIRE (Financial Independence Retire Early) Planner
// -------------------------------------------------------------
@Composable
fun FireCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultAge = prefillData.currentAge.toString()
    val defaultExpense = if (prefillData.avgMonthlyExpense > 0) prefillData.avgMonthlyExpense.toLong().toString() else "50000"
    val defaultSavings = if (prefillData.currentNetWorth > 0) prefillData.currentNetWorth.toLong().toString() else "1000000"

    var currentAgeText by remember { mutableStateOf(defaultAge) }
    var targetRetireAgeText by remember { mutableStateOf("45") }
    var monthlyExpensesText by remember { mutableStateOf(defaultExpense) }
    var existingSavingsText by remember { mutableStateOf(defaultSavings) }
    var inflationRateText by remember { mutableStateOf("6") }
    var preRetireReturnText by remember { mutableStateOf("12") }
    var includeExistingSavings by remember { mutableStateOf(true) }

    val currentAge = currentAgeText.toDoubleOrNull() ?: 28.0
    val targetRetireAge = targetRetireAgeText.toDoubleOrNull() ?: 45.0
    val monthlyExpenses = monthlyExpensesText.toDoubleOrNull() ?: 0.0
    val existingSavings = if (includeExistingSavings) (existingSavingsText.toDoubleOrNull() ?: 0.0) else 0.0
    val inflationRate = inflationRateText.toDoubleOrNull() ?: 6.0
    val preRetireReturn = preRetireReturnText.toDoubleOrNull() ?: 12.0

    val yearsToRetire = (targetRetireAge - currentAge).coerceAtLeast(0.0)
    val futureMonthlyExpense = monthlyExpenses * (1.0 + inflationRate / 100.0).pow(yearsToRetire)
    val futureAnnualExpense = futureMonthlyExpense * 12.0
    val swr = 0.04 // 4% safe withdrawal rule (25x annual expenses)
    val targetFireCorpus = if (futureAnnualExpense > 0) futureAnnualExpense / swr else 0.0
    val leanFireCorpus = targetFireCorpus * 0.75
    val fatFireCorpus = targetFireCorpus * 1.5

    // Future value of existing savings at retirement age
    val futureValueOfSavings = existingSavings * (1.0 + preRetireReturn / 100.0).pow(yearsToRetire)
    val gapCorpus = (targetFireCorpus - futureValueOfSavings).coerceAtLeast(0.0)
    val surplusCorpus = (futureValueOfSavings - targetFireCorpus).coerceAtLeast(0.0)

    // Required Monthly SIP to bridge any corpus gap
    val months = (yearsToRetire * 12.0).toInt()
    val r = (preRetireReturn / 12.0) / 100.0
    val requiredMonthlySip = if (r > 0 && gapCorpus > 0 && months > 0) {
        gapCorpus / ((((1.0 + r).pow(months) - 1.0) / r) * (1.0 + r))
    } else 0.0

    // Fresh SIP required if existing portfolio was 0
    val rawFreshSip = if (r > 0 && targetFireCorpus > 0 && months > 0) {
        targetFireCorpus / ((((1.0 + r).pow(months) - 1.0) / r) * (1.0 + r))
    } else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            AutoPrefillBadge(text = "Auto-filled: Age ($defaultAge yrs from DOB), Monthly Expense & Net Worth")
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Current Age",
                        value = currentAgeText,
                        suffix = " Yrs",
                        placeholder = "28",
                        onValueChange = { currentAgeText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Target FIRE Age",
                        value = targetRetireAgeText,
                        suffix = " Yrs",
                        placeholder = "45",
                        onValueChange = { targetRetireAgeText = it }
                    )
                }
            }
        }

        item {
            CalculatorTextInput(
                label = "Current Monthly Expenses",
                value = monthlyExpensesText,
                prefix = "₹ ",
                placeholder = "50000",
                onValueChange = { monthlyExpensesText = it }
            )
        }

        item {
            CalculatorTextInput(
                label = "Current Savings / Net Worth",
                value = existingSavingsText,
                prefix = "₹ ",
                placeholder = "1000000",
                onValueChange = { existingSavingsText = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Inflation Rate",
                        value = inflationRateText,
                        suffix = " %",
                        placeholder = "6",
                        onValueChange = { inflationRateText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Return Rate (CAGR)",
                        value = preRetireReturnText,
                        suffix = " %",
                        placeholder = "12",
                        onValueChange = { preRetireReturnText = it }
                    )
                }
            }
        }

        // Toggle to show SIP with or without existing portfolio
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Compound Existing Portfolio",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = if (includeExistingSavings) "Factoring in current ₹${CurrencyFormatter.formatInr(existingSavings)}" else "Calculating fresh SIP required from ₹0",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                    Switch(
                        checked = includeExistingSavings,
                        onCheckedChange = { includeExistingSavings = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFEA580C))
                    )
                }
            }
        }

        // Target Corpus Result Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "FIRE TARGET CORPUS (25X RULE)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFB923C)
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(targetFireCorpus),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "Future Monthly Expense at age ${targetRetireAge.toInt()}: ${CurrencyFormatter.formatInr(futureMonthlyExpense)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF94A3B8)
                    )

                    HorizontalDivider(color = Color(0xFF334155))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Lean FIRE (75%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text(CurrencyFormatter.formatInr(leanFireCorpus), fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Fat FIRE (150%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8))
                            Text(CurrencyFormatter.formatInr(fatFireCorpus), fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                        }
                    }

                    if (includeExistingSavings && existingSavings > 0) {
                        HorizontalDivider(color = Color(0xFF334155))
                        MetricDetailRow("Existing Savings Growth in ${yearsToRetire.toInt()} yrs", CurrencyFormatter.formatInr(futureValueOfSavings), Color(0xFF38BDF8))
                    }

                    // Result Box
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (requiredMonthlySip == 0.0 && includeExistingSavings) Color(0xFF064E3B) else Color(0xFF1E293B)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = if (requiredMonthlySip == 0.0 && includeExistingSavings) "Monthly Additional SIP" else "Monthly SIP to Reach Target",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (requiredMonthlySip == 0.0 && includeExistingSavings) Color(0xFFA7F3D0) else Color(0xFF94A3B8)
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatInr(requiredMonthlySip) + " / mo",
                                        fontWeight = FontWeight.Bold,
                                        color = if (requiredMonthlySip == 0.0 && includeExistingSavings) Color(0xFF34D399) else Color(0xFFFBBF24)
                                    )
                                }
                                Text(
                                    text = "in ${yearsToRetire.toInt()} yrs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            if (requiredMonthlySip == 0.0 && includeExistingSavings && surplusCorpus > 0) {
                                Text(
                                    text = "🎉 Your existing portfolio alone compounds to ${CurrencyFormatter.formatInr(futureValueOfSavings)}, exceeding your FIRE corpus by ${CurrencyFormatter.formatInr(surplusCorpus)} surplus! No extra monthly SIP is mandatory.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                                    color = Color(0xFFE2E8F0)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (includeExistingSavings && requiredMonthlySip == 0.0) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp).padding(top = 2.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Why is Monthly SIP ₹0?",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D)
                            )
                            Text(
                                text = "Your current Net Worth of ${CurrencyFormatter.formatInr(existingSavings)} growing at ${preRetireReturn}% p.a. for ${yearsToRetire.toInt()} years will grow to ${CurrencyFormatter.formatInr(futureValueOfSavings)}. Because this exceeds the required target of ${CurrencyFormatter.formatInr(targetFireCorpus)}, your FIRE target is already 100% funded!",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                                color = Color(0xFF166534)
                            )
                            Text(
                                text = "If you want to start from ₹0 fresh savings, toggle off 'Compound Existing Portfolio' above (Fresh SIP would be ${CurrencyFormatter.formatInr(rawFreshSip)}/mo).",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF0F766E)
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Comprehensive Retirement Planning Calculator
// -------------------------------------------------------------
@Composable
fun RetirementPlannerCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultAge = prefillData.currentAge.toString()
    val defaultExpense = if (prefillData.avgMonthlyExpense > 0) prefillData.avgMonthlyExpense.toLong().toString() else "50000"

    var currentAgeText by remember { mutableStateOf(defaultAge) }
    var retireAgeText by remember { mutableStateOf("60") }
    var lifeExpectancyText by remember { mutableStateOf("85") }
    var monthlyExpenseText by remember { mutableStateOf(defaultExpense) }
    var inflationText by remember { mutableStateOf("6") }
    var preRetireReturnText by remember { mutableStateOf("12") }
    var postRetireReturnText by remember { mutableStateOf("8") }

    val currentAge = currentAgeText.toDoubleOrNull() ?: 30.0
    val retireAge = retireAgeText.toDoubleOrNull() ?: 60.0
    val lifeExpectancy = lifeExpectancyText.toDoubleOrNull() ?: 85.0
    val monthlyExpense = monthlyExpenseText.toDoubleOrNull() ?: 50000.0
    val inflation = inflationText.toDoubleOrNull() ?: 6.0
    val preReturn = preRetireReturnText.toDoubleOrNull() ?: 12.0
    val postReturn = postRetireReturnText.toDoubleOrNull() ?: 8.0

    val yearsToRetire = (retireAge - currentAge).coerceAtLeast(1.0)
    val yearsInRetirement = (lifeExpectancy - retireAge).coerceAtLeast(1.0)

    val inflatedMonthlyExpense = monthlyExpense * (1.0 + inflation / 100.0).pow(yearsToRetire)
    val inflatedAnnualExpense = inflatedMonthlyExpense * 12.0

    // Real rate of return in retirement
    val realRate = ((1.0 + postReturn / 100.0) / (1.0 + inflation / 100.0)) - 1.0
    val requiredCorpus = if (realRate != 0.0) {
        inflatedAnnualExpense * ((1.0 - (1.0 + realRate).pow(-yearsInRetirement)) / realRate)
    } else inflatedAnnualExpense * yearsInRetirement

    // SIP to accumulate corpus
    val months = (yearsToRetire * 12.0).toInt()
    val monthlyPreRate = (preReturn / 12.0) / 100.0
    val requiredMonthlySip = if (monthlyPreRate > 0 && months > 0) {
        requiredCorpus / ((((1.0 + monthlyPreRate).pow(months) - 1.0) / monthlyPreRate) * (1.0 + monthlyPreRate))
    } else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Current Age", currentAgeText, { currentAgeText = it }, suffix = " Yrs")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Retirement Age", retireAgeText, { retireAgeText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Current Monthly Expense", monthlyExpenseText, { monthlyExpenseText = it }, prefix = "₹ ")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Life Expectancy", lifeExpectancyText, { lifeExpectancyText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Pre-Retire Return", preRetireReturnText, { preRetireReturnText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Post-Retire Return", postRetireReturnText, { postRetireReturnText = it }, suffix = " %")
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TOTAL RETIREMENT CORPUS NEEDED", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                    Text(CurrencyFormatter.formatInr(requiredCorpus), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFD97706))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Monthly SIP Required", CurrencyFormatter.formatInr(requiredMonthlySip) + " / mo", Color(0xFF0284C7), isBold = true)
                    MetricDetailRow("Expense at Retirement (${retireAge.toInt()} yrs)", CurrencyFormatter.formatInr(inflatedMonthlyExpense) + " / mo")
                    MetricDetailRow("Years in Retirement", "${yearsInRetirement.toInt()} years")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. SWP (Systematic Withdrawal Plan) Calculator
// -------------------------------------------------------------
@Composable
fun SwpCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var totalInvestmentText by remember { mutableStateOf("5000000") }
    var withdrawalPerMonthText by remember { mutableStateOf("30000") }
    var returnRateText by remember { mutableStateOf("9") }
    var yearsText by remember { mutableStateOf("10") }

    val totalInvestment = totalInvestmentText.toDoubleOrNull() ?: 0.0
    val withdrawal = withdrawalPerMonthText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val totalMonths = (years * 12.0).toInt()
    val monthlyRate = (returnRate / 12.0) / 100.0
    var balance = totalInvestment
    var totalWithdrawn = 0.0

    for (m in 1..totalMonths) {
        val interest = balance * monthlyRate
        balance = (balance + interest - withdrawal).coerceAtLeast(0.0)
        totalWithdrawn += withdrawal
        if (balance <= 0.0) break
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Total Investment Corpus", totalInvestmentText, { totalInvestmentText = it }, prefix = "₹ ")
        }
        item {
            CalculatorTextInput("Monthly Withdrawal (SWP)", withdrawalPerMonthText, { withdrawalPerMonthText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Expected Return Rate", returnRateText, { returnRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Duration", yearsText, { yearsText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Final Remaining Corpus",
                primaryValue = balance,
                investedAmount = totalInvestment,
                returnsAmount = totalWithdrawn,
                investedLabel = "Initial Corpus",
                returnsLabel = "Total Withdrawn",
                accentColor = Color(0xFF059669)
            )
        }
    }
}

// -------------------------------------------------------------
// 4. NPS (National Pension Scheme) Calculator
// -------------------------------------------------------------
@Composable
fun NpsCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultAge = prefillData.currentAge.toString()
    var monthlyDepositText by remember { mutableStateOf("10000") }
    var currentAgeText by remember { mutableStateOf(defaultAge) }
    var retireAgeText by remember { mutableStateOf("60") }
    var returnRateText by remember { mutableStateOf("10") }
    var annuityPercentText by remember { mutableStateOf("40") }
    var annuityReturnText by remember { mutableStateOf("6.5") }

    val monthly = monthlyDepositText.toDoubleOrNull() ?: 0.0
    val curAge = currentAgeText.toDoubleOrNull() ?: 30.0
    val retAge = retireAgeText.toDoubleOrNull() ?: 60.0
    val retRate = returnRateText.toDoubleOrNull() ?: 10.0
    val annuityPct = annuityPercentText.toDoubleOrNull() ?: 40.0
    val annuityRate = annuityReturnText.toDoubleOrNull() ?: 6.5

    val years = (retAge - curAge).coerceAtLeast(1.0)
    val months = (years * 12.0).toInt()
    val monthlyRate = (retRate / 12.0) / 100.0
    val totalInvested = monthly * months
    val totalCorpus = if (monthlyRate > 0 && months > 0) {
        monthly * (((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
    } else totalInvested

    val annuityCorpus = totalCorpus * (annuityPct / 100.0)
    val lumpSumCorpus = totalCorpus - annuityCorpus
    val monthlyPension = (annuityCorpus * (annuityRate / 100.0)) / 12.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Monthly NPS Contribution", monthlyDepositText, { monthlyDepositText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Current Age", currentAgeText, { currentAgeText = it }, suffix = " Yrs")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Retirement Age", retireAgeText, { retireAgeText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Expected Return (CAGR)", returnRateText, { returnRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Annuity Reinvestment", annuityPercentText, { annuityPercentText = it }, suffix = " %")
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NPS TOTAL RETIREMENT CORPUS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                    Text(CurrencyFormatter.formatInr(totalCorpus), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF9333EA))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Tax-Free Lump Sum (60%)", CurrencyFormatter.formatInr(lumpSumCorpus), Color(0xFF0284C7), isBold = true)
                    MetricDetailRow("Annuity Corpus (40%)", CurrencyFormatter.formatInr(annuityCorpus))
                    MetricDetailRow("Estimated Monthly Pension", CurrencyFormatter.formatInr(monthlyPension) + " / mo", Emerald500, isBold = true)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. APY (Atal Pension Yojana) Calculator
// -------------------------------------------------------------
@Composable
fun ApyCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultAge = prefillData.currentAge.coerceIn(18, 40).toString()
    var ageText by remember { mutableStateOf(defaultAge) }
    var selectedPension by remember { mutableStateOf(5000) }

    val age = ageText.toIntOrNull() ?: 25
    val yearsToContribute = (60 - age).coerceIn(20, 42)

    // APY official monthly contribution lookup per age for ₹5000 pension
    val baseContribFor5000 = when (age) {
        18 -> 210
        19 -> 231
        20 -> 248
        21 -> 269
        22 -> 292
        23 -> 318
        24 -> 346
        25 -> 376
        26 -> 409
        27 -> 446
        28 -> 485
        29 -> 529
        30 -> 577
        31 -> 630
        32 -> 689
        33 -> 752
        34 -> 824
        35 -> 902
        36 -> 990
        37 -> 1087
        38 -> 1194
        39 -> 1318
        40 -> 1454
        else -> 577
    }

    val monthlyContrib = (baseContribFor5000 * (selectedPension.toDouble() / 5000.0)).toInt()
    val totalInvested = monthlyContrib.toDouble() * yearsToContribute * 12.0
    val guaranteedCorpus = selectedPension * 1700.0 // Indicative nominee corpus

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Your Current Age (18 to 40 yrs)", ageText, { ageText = it }, suffix = " Yrs")
        }
        item {
            Text("Desired Guaranteed Monthly Pension at Age 60", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(1000, 2000, 3000, 5000).forEach { pension ->
                    FilterChip(
                        selected = selectedPension == pension,
                        onClick = { selectedPension = pension },
                        label = { Text("₹$pension") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("REQUIRED MONTHLY CONTRIBUTION", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                    Text("₹$monthlyContrib / month", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF6366F1))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Guaranteed Monthly Pension", "₹$selectedPension / mo", Emerald500, isBold = true)
                    MetricDetailRow("Total Contribution Duration", "$yearsToContribute years")
                    MetricDetailRow("Total Out of Pocket Deposit", CurrencyFormatter.formatInr(totalInvested))
                    MetricDetailRow("Nominee Return Corpus", CurrencyFormatter.formatInr(guaranteedCorpus))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. EPF (Employee Provident Fund) Calculator
// -------------------------------------------------------------
@Composable
fun EpfCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var basicSalaryText by remember { mutableStateOf("50000") }
    var currentAgeText by remember { mutableStateOf("28") }
    var retireAgeText by remember { mutableStateOf("58") }
    var epfBalanceText by remember { mutableStateOf("200000") }
    var annualHikeText by remember { mutableStateOf("8") }

    val basic = basicSalaryText.toDoubleOrNull() ?: 0.0
    val curAge = currentAgeText.toDoubleOrNull() ?: 28.0
    val retAge = retireAgeText.toDoubleOrNull() ?: 58.0
    var balance = epfBalanceText.toDoubleOrNull() ?: 0.0
    val hike = annualHikeText.toDoubleOrNull() ?: 8.0

    val years = (retAge - curAge).coerceAtLeast(1.0).toInt()
    val epfRate = 0.0825 // 8.25% EPFO rate

    var currentBasic = basic
    var totalEmployeeContrib = 0.0
    var totalEmployerContrib = 0.0

    for (y in 1..years) {
        val employeeYearly = currentBasic * 0.12 * 12.0
        val employerEpfYearly = currentBasic * 0.0367 * 12.0 // 3.67% to EPF (8.33% goes to EPS)
        val yearlyTotalContrib = employeeYearly + employerEpfYearly
        totalEmployeeContrib += employeeYearly
        totalEmployerContrib += employerEpfYearly

        val interest = (balance + (yearlyTotalContrib / 2.0)) * epfRate
        balance += yearlyTotalContrib + interest
        currentBasic *= (1.0 + hike / 100.0)
    }

    val totalInvested = totalEmployeeContrib + totalEmployerContrib + (epfBalanceText.toDoubleOrNull() ?: 0.0)
    val totalInterest = (balance - totalInvested).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Monthly Basic Salary + DA", basicSalaryText, { basicSalaryText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Current Age", currentAgeText, { currentAgeText = it }, suffix = " Yrs")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Current EPF Balance", epfBalanceText, { epfBalanceText = it }, prefix = "₹ ")
                }
            }
        }
        item {
            CalculatorTextInput("Expected Annual Salary Hike", annualHikeText, { annualHikeText = it }, suffix = " %")
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Total EPF Corpus at Retirement",
                primaryValue = balance,
                investedAmount = totalInvested,
                returnsAmount = totalInterest,
                investedLabel = "Total EPF Contribution",
                returnsLabel = "Total EPF Interest (8.25%)",
                accentColor = Color(0xFF0284C7)
            )
        }
    }
}

// -------------------------------------------------------------
// 7. Gratuity Calculator
// -------------------------------------------------------------
@Composable
fun GratuityCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var lastSalaryText by remember { mutableStateOf("60000") }
    var serviceYearsText by remember { mutableStateOf("7") }

    val salary = lastSalaryText.toDoubleOrNull() ?: 0.0
    val years = serviceYearsText.toDoubleOrNull() ?: 0.0

    // Statutory Gratuity Formula: (15 * Last Drawn Basic+DA * Tenure) / 26
    val gratuityAmount = if (years >= 5.0) {
        ((15.0 * salary * years) / 26.0).coerceAtMost(2000000.0) // 20 Lakh statutory cap
    } else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Last Drawn Basic Salary + DA", lastSalaryText, { lastSalaryText = it }, prefix = "₹ ")
        }
        item {
            CalculatorTextInput("Years of Continuous Service", serviceYearsText, { serviceYearsText = it }, suffix = " Yrs")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("STATUTORY GRATUITY PAYOUT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF4F46E5))
                    Text(CurrencyFormatter.formatInr(gratuityAmount), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF4F46E5))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Eligibility Status", if (years >= 5.0) "✅ Eligible (>5 years)" else "⚠️ Minimum 5 years required for eligibility", if (years >= 5.0) Emerald500 else Color(0xFFDC2626))
                    MetricDetailRow("Tax-Exempt Ceiling", "Up to ₹20,00,000 under Section 10(10)")
                }
            }
        }
    }
}
