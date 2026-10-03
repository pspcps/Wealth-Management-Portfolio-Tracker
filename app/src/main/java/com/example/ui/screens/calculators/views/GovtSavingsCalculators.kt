package com.example.ui.screens.calculators.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.ui.theme.Emerald500
import kotlin.math.pow

// -------------------------------------------------------------
// 1. PPF (Public Provident Fund) Calculator
// -------------------------------------------------------------
@Composable
fun PpfCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var yearlyDepositText by remember { mutableStateOf("150000") }
    var yearsText by remember { mutableStateOf("15") }
    val interestRate = 7.1 // Current Sovereign PPF Rate

    val yearlyDeposit = yearlyDepositText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toIntOrNull() ?: 15

    var balance = 0.0
    var totalInvested = 0.0

    for (y in 1..years) {
        totalInvested += yearlyDeposit
        balance = (balance + yearlyDeposit) * (1.0 + interestRate / 100.0)
    }

    val totalInterest = (balance - totalInvested).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Yearly Deposit (Max ₹1.5L/yr)",
                value = yearlyDepositText,
                prefix = "₹ ",
                placeholder = "150000",
                onValueChange = { yearlyDepositText = it }
            )
        }
        item {
            CalculatorTextInput(
                label = "Tenure (15 yrs or extended in 5-yr blocks)",
                value = yearsText,
                suffix = " Yrs",
                placeholder = "15",
                onValueChange = { yearsText = it }
            )
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Total PPF Maturity Amount (Tax-Free)",
                primaryValue = balance,
                investedAmount = totalInvested,
                returnsAmount = totalInterest,
                investedLabel = "Total Investment",
                returnsLabel = "Interest Earned (7.1% EEE)",
                accentColor = Emerald500
            )
        }
        item {
            InfoNoteCard("🛡️ PPF features complete EEE tax-exemption under Section 80C. Principal, interest and maturity are 100% tax-free.")
        }
    }
}

// -------------------------------------------------------------
// 2. SSY (Sukanya Samriddhi Yojana) Calculator
// -------------------------------------------------------------
@Composable
fun SsyCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var yearlyDepositText by remember { mutableStateOf("100000") }
    var girlAgeText by remember { mutableStateOf("2") }
    val ssyRate = 8.2 // Current SSY rate

    val yearlyDeposit = yearlyDepositText.toDoubleOrNull() ?: 0.0
    val girlAge = girlAgeText.toIntOrNull() ?: 2

    // Deposits for 15 years, maturity at 21 years from account opening (or girl age 21)
    val depositYears = 15
    val maturityYears = (21 - girlAge).coerceIn(15, 21)

    var balance = 0.0
    var totalInvested = 0.0

    for (y in 1..maturityYears) {
        if (y <= depositYears) {
            totalInvested += yearlyDeposit
            balance = (balance + yearlyDeposit) * (1.0 + ssyRate / 100.0)
        } else {
            balance *= (1.0 + ssyRate / 100.0)
        }
    }

    val totalInterest = (balance - totalInvested).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Yearly Deposit (Max ₹1.5L/yr)",
                value = yearlyDepositText,
                prefix = "₹ ",
                placeholder = "100000",
                onValueChange = { yearlyDepositText = it }
            )
        }
        item {
            CalculatorTextInput(
                label = "Girl Child's Age",
                value = girlAgeText,
                suffix = " Yrs",
                placeholder = "2",
                onValueChange = { girlAgeText = it }
            )
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Total SSY Maturity Amount",
                primaryValue = balance,
                investedAmount = totalInvested,
                returnsAmount = totalInterest,
                investedLabel = "Total Deposited (15 Yrs)",
                returnsLabel = "Total Interest (8.2%)",
                accentColor = Color(0xFFEC4899)
            )
        }
        item {
            InfoNoteCard("🌸 Sukanya Samriddhi Yojana offers the highest sovereign rate (8.2%) with 100% tax-free EEE status for the daughter's higher education/marriage at age 21.")
        }
    }
}

// -------------------------------------------------------------
// 3. NSC (National Savings Certificate) Calculator
// -------------------------------------------------------------
@Composable
fun NscCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var investmentText by remember { mutableStateOf("100000") }
    val nscRate = 7.7 // Current 5-year NSC rate

    val investment = investmentText.toDoubleOrNull() ?: 0.0
    val maturityValue = investment * (1.0 + nscRate / 100.0).pow(5)
    val totalInterest = (maturityValue - investment).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Total Investment in NSC",
                value = investmentText,
                prefix = "₹ ",
                placeholder = "100000",
                onValueChange = { investmentText = it }
            )
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Maturity Amount (5-Year Lock-In)",
                primaryValue = maturityValue,
                investedAmount = investment,
                returnsAmount = totalInterest,
                investedLabel = "Principal Amount",
                returnsLabel = "Interest Earned (7.7% p.a.)",
                accentColor = Color(0xFFF59E0B)
            )
        }
    }
}

// -------------------------------------------------------------
// 4. SCSS (Senior Citizen Savings Scheme) Calculator
// -------------------------------------------------------------
@Composable
fun ScssCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var depositText by remember { mutableStateOf("1500000") }
    val scssRate = 8.2 // Current SCSS rate

    val deposit = (depositText.toDoubleOrNull() ?: 0.0).coerceAtMost(3000000.0) // Max 30 Lakhs
    val annualInterest = deposit * (scssRate / 100.0)
    val quarterlyPayout = annualInterest / 4.0
    val totalInterest5Yrs = annualInterest * 5.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Deposit Amount (Max ₹30 Lakh)",
                value = depositText,
                prefix = "₹ ",
                placeholder = "1500000",
                onValueChange = { depositText = it }
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("QUARTERLY GUARANTEED INTEREST PAYOUT", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF0D9488))
                    Text(CurrencyFormatter.formatInr(quarterlyPayout) + " / quarter", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0D9488))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Annual Interest Income", CurrencyFormatter.formatInr(annualInterest) + " / yr", isBold = true)
                    MetricDetailRow("Total 5-Year Interest Payout", CurrencyFormatter.formatInr(totalInterest5Yrs), Emerald500)
                    MetricDetailRow("Principal Returned on Maturity", CurrencyFormatter.formatInr(deposit))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. Post Office MIS (Monthly Income Scheme) Calculator
// -------------------------------------------------------------
@Composable
fun PostOfficeMisCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var depositText by remember { mutableStateOf("900000") }
    val misRate = 7.4 // Current PO MIS rate

    val deposit = (depositText.toDoubleOrNull() ?: 0.0).coerceAtMost(1500000.0) // Max 9L single / 15L joint
    val annualInterest = deposit * (misRate / 100.0)
    val monthlyPayout = annualInterest / 12.0
    val total5YrInterest = annualInterest * 5.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Deposit Amount (Max ₹9L Single / ₹15L Joint)",
                value = depositText,
                prefix = "₹ ",
                placeholder = "900000",
                onValueChange = { depositText = it }
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("MONTHLY GUARANTEED CASH FLOW", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Text(CurrencyFormatter.formatInr(monthlyPayout) + " / month", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFDC2626))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Annual Guaranteed Payout", CurrencyFormatter.formatInr(annualInterest) + " / yr")
                    MetricDetailRow("Total 5-Year Interest Payout", CurrencyFormatter.formatInr(total5YrInterest), Emerald500, isBold = true)
                    MetricDetailRow("Principal Returned on 5-Yr Maturity", CurrencyFormatter.formatInr(deposit))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. FD & Fixed Income / Bond Calculator (Payouts & Amortization)
// -------------------------------------------------------------
@Composable
fun FdCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var depositText by remember { mutableStateOf("100000") }
    var interestRateText by remember { mutableStateOf("7.5") }
    var tenureMonthsText by remember { mutableStateOf("36") }
    var selectedPayoutMode by remember { mutableStateOf(com.example.domain.finance.PayoutMode.INTEREST_ONLY) }
    var selectedInterestFrequency by remember { mutableStateOf(com.example.domain.finance.PayoutFrequency.MONTHLY) }
    var principalPayoutPercentText by remember { mutableStateOf("5.0") }
    var selectedPrincipalFrequency by remember { mutableStateOf(com.example.domain.finance.PayoutFrequency.QUARTERLY) }
    var showSchedule by remember { mutableStateOf(false) }

    val deposit = depositText.toDoubleOrNull() ?: 0.0
    val rate = interestRateText.toDoubleOrNull() ?: 7.5
    val months = tenureMonthsText.toIntOrNull() ?: 36
    val principalPayoutPercent = principalPayoutPercentText.toDoubleOrNull() ?: 5.0

    val calcResult = remember(deposit, rate, months, selectedPayoutMode, selectedInterestFrequency, principalPayoutPercent, selectedPrincipalFrequency) {
        com.example.domain.finance.FinanceCalculators.calculateFixedIncomePayout(
            principal = deposit,
            annualRatePercentage = rate,
            tenureMonths = months,
            payoutFrequency = selectedInterestFrequency,
            payoutMode = selectedPayoutMode,
            principalPayoutPercent = if (selectedPayoutMode == com.example.domain.finance.PayoutMode.INTEREST_AND_PRINCIPAL) principalPayoutPercent else 0.0,
            principalPayoutFrequency = selectedPrincipalFrequency
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            // Payout Mode Selector Tabs
            Text(
                text = "Payout Mode",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedPayoutMode == com.example.domain.finance.PayoutMode.CUMULATIVE,
                    onClick = { selectedPayoutMode = com.example.domain.finance.PayoutMode.CUMULATIVE },
                    label = { Text("Cumulative (Maturity)", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPayoutMode == com.example.domain.finance.PayoutMode.INTEREST_ONLY,
                    onClick = { selectedPayoutMode = com.example.domain.finance.PayoutMode.INTEREST_ONLY },
                    label = { Text("Interest Payout", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedPayoutMode == com.example.domain.finance.PayoutMode.INTEREST_AND_PRINCIPAL,
                    onClick = { selectedPayoutMode = com.example.domain.finance.PayoutMode.INTEREST_AND_PRINCIPAL },
                    label = { Text("Interest + % Principal", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            CalculatorTextInput("Investment Principal (₹)", depositText, { depositText = it }, prefix = "₹ ")
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Interest Rate (p.a.)", interestRateText, { interestRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Tenure (Months)", tenureMonthsText, { tenureMonthsText = it }, suffix = " Mo")
                }
            }
        }

        if (selectedPayoutMode != com.example.domain.finance.PayoutMode.CUMULATIVE) {
            item {
                Text(
                    text = "Interest Payout Frequency",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        com.example.domain.finance.PayoutFrequency.MONTHLY,
                        com.example.domain.finance.PayoutFrequency.QUARTERLY,
                        com.example.domain.finance.PayoutFrequency.HALF_YEARLY,
                        com.example.domain.finance.PayoutFrequency.ANNUALLY
                    ).forEach { freq ->
                        FilterChip(
                            selected = selectedInterestFrequency == freq,
                            onClick = { selectedInterestFrequency = freq },
                            label = { Text(freq.label.substringBefore(" "), fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        if (selectedPayoutMode == com.example.domain.finance.PayoutMode.INTEREST_AND_PRINCIPAL) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Periodic Principal Amortization / Payout",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                CalculatorTextInput("Principal Return %", principalPayoutPercentText, { principalPayoutPercentText = it }, suffix = " %")
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                Column {
                                    Text("Frequency", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        FilterChip(
                                            selected = selectedPrincipalFrequency == com.example.domain.finance.PayoutFrequency.QUARTERLY,
                                            onClick = { selectedPrincipalFrequency = com.example.domain.finance.PayoutFrequency.QUARTERLY },
                                            label = { Text("Qtr", fontSize = 10.sp) }
                                        )
                                        FilterChip(
                                            selected = selectedPrincipalFrequency == com.example.domain.finance.PayoutFrequency.ANNUALLY,
                                            onClick = { selectedPrincipalFrequency = com.example.domain.finance.PayoutFrequency.ANNUALLY },
                                            label = { Text("Yr", fontSize = 10.sp) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Outcome Display
        item {
            if (selectedPayoutMode == com.example.domain.finance.PayoutMode.CUMULATIVE) {
                OutcomeDisplayCard(
                    primaryLabel = "Total FD Maturity Payout",
                    primaryValue = calcResult.finalMaturityPayout,
                    investedAmount = calcResult.initialPrincipal,
                    returnsAmount = calcResult.totalInterestEarned,
                    investedLabel = "Principal Deposited",
                    returnsLabel = "Compounded Interest Earned",
                    accentColor = Color(0xFF2563EB)
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "REGULAR CASH FLOW STREAM",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDBEAFE)
                            ) {
                                Text(
                                    text = "${selectedInterestFrequency.label.substringBefore(" ")} Payout",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "${CurrencyFormatter.formatInr(calcResult.periodicInterestPayout)} / ${selectedInterestFrequency.label.substringBefore(" ").lowercase()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B)
                        )

                        if (calcResult.periodicPrincipalPayout > 0) {
                            Text(
                                text = "+ ${CurrencyFormatter.formatInr(calcResult.periodicPrincipalPayout)} principal returned every ${selectedPrincipalFrequency.label.substringBefore(" ").lowercase()}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF059669)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFE2E8F0))

                        MetricDetailRow("Total Interest Received", CurrencyFormatter.formatInr(calcResult.totalInterestEarned), Emerald500, isBold = true)
                        MetricDetailRow("Total Principal Returned", CurrencyFormatter.formatInr(calcResult.totalPrincipalReturned))
                        MetricDetailRow("Total Cash Inflow", CurrencyFormatter.formatInr(calcResult.totalCashReceived), Color(0xFF2563EB), isBold = true)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(onClick = { showSchedule = !showSchedule }) {
                                Text(if (showSchedule) "Hide Cash Flow Schedule" else "View Full Cash Flow Schedule (${calcResult.schedule.size} Periods)")
                            }
                        }
                    }
                }
            }
        }

        if (showSchedule && calcResult.schedule.isNotEmpty()) {
            item {
                Text(
                    text = "Cash Flow Payout Breakdown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            calcResult.schedule.take(16).forEach { item ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.periodLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Principal Bal: ${CurrencyFormatter.formatInr(item.openingPrincipal)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("+ ${CurrencyFormatter.formatInr(item.totalPayout)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                Text(
                                    text = if (item.principalPayout > 0) "Int: ${CurrencyFormatter.formatInr(item.interestPayout)} • Prin: ${CurrencyFormatter.formatInr(item.principalPayout)}" else "Interest: ${CurrencyFormatter.formatInr(item.interestPayout)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. RD (Recurring Deposit) Calculator (Monthly, Quarterly, Annually)
// -------------------------------------------------------------
@Composable
fun RdCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var depositText by remember { mutableStateOf("5000") }
    var selectedFrequency by remember { mutableStateOf(com.example.domain.finance.RdFrequency.MONTHLY) }
    var interestRateText by remember { mutableStateOf("7.1") }
    var tenureMonthsText by remember { mutableStateOf("24") }
    var showSchedule by remember { mutableStateOf(false) }

    val installment = depositText.toDoubleOrNull() ?: 0.0
    val rate = interestRateText.toDoubleOrNull() ?: 7.1
    val months = tenureMonthsText.toIntOrNull() ?: 24

    val rdResult = remember(installment, selectedFrequency, rate, months) {
        com.example.domain.finance.FinanceCalculators.calculateRecurringDeposit(
            installmentAmount = installment,
            frequency = selectedFrequency,
            annualRatePercentage = rate,
            tenureMonths = months
        )
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                text = "Deposit Frequency",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                com.example.domain.finance.RdFrequency.values().forEach { freq ->
                    FilterChip(
                        selected = selectedFrequency == freq,
                        onClick = { selectedFrequency = freq },
                        label = { Text("${freq.label} RD", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            CalculatorTextInput(
                label = "${selectedFrequency.label} Installment (₹)",
                value = depositText,
                onValueChange = { depositText = it },
                prefix = "₹ "
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Interest Rate (p.a.)", interestRateText, { interestRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Tenure (Months)", tenureMonthsText, { tenureMonthsText = it }, suffix = " Mo")
                }
            }
        }

        // Quick tenure preset chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(12 to "1 Yr", 24 to "2 Yrs", 36 to "3 Yrs", 60 to "5 Yrs").forEach { (m, label) ->
                    AssistChip(
                        onClick = { tenureMonthsText = m.toString() },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        item {
            OutcomeDisplayCard(
                primaryLabel = "Total RD Maturity Corpus",
                primaryValue = rdResult.maturityAmount,
                investedAmount = rdResult.totalDeposited,
                returnsAmount = rdResult.totalInterestEarned,
                investedLabel = "Total Capital Deposited (${rdResult.totalInstallments} installments)",
                returnsLabel = "Quarterly Compounded Interest",
                accentColor = Color(0xFF0891B2)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = BorderStroke(1.dp, Color(0xFFBBF7D0))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "RD SCHEDULE DETAILS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    MetricDetailRow("Deposit per period", "${CurrencyFormatter.formatInr(rdResult.installmentAmount)} / ${selectedFrequency.label.lowercase()}")
                    MetricDetailRow("Total Installments", "${rdResult.totalInstallments} deposits")
                    MetricDetailRow("Accrued Return Rate", "${String.format("%.2f", if (rdResult.totalDeposited > 0) (rdResult.totalInterestEarned / rdResult.totalDeposited) * 100.0 else 0.0)}% absolute return", Emerald500, isBold = true)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { showSchedule = !showSchedule }) {
                            Text(if (showSchedule) "Hide Schedule" else "View Installment Compounding Schedule")
                        }
                    }
                }
            }
        }

        if (showSchedule && rdResult.schedule.isNotEmpty()) {
            item {
                Text(
                    text = "Deposit & Balance Progression",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
                )
            }
            rdResult.schedule.take(16).forEach { item ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.periodLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF1E293B))
                                Text("Deposit: ${CurrencyFormatter.formatInr(item.cumulativeDeposited)}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF64748B))
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Corpus: ${CurrencyFormatter.formatInr(item.accruedBalance)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF0891B2))
                                Text("+ ${CurrencyFormatter.formatInr(item.interestEarnedPeriod)} interest", style = MaterialTheme.typography.labelSmall, color = Color(0xFF16A34A))
                            }
                        }
                    }
                }
            }
        }
    }
}
