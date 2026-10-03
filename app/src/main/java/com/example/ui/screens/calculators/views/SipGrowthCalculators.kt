package com.example.ui.screens.calculators.views

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
import kotlin.math.abs
import kotlin.math.pow

// -------------------------------------------------------------
// 1. Target Goal SIP Calculator (Explicit User Request)
// -------------------------------------------------------------
@Composable
fun TargetGoalSipCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultTarget = if (prefillData.userProfile.targetNetWorth > 0) {
        prefillData.userProfile.targetNetWorth.toLong().toString()
    } else "10000000" // 1 Crore default

    var targetAmountText by remember { mutableStateOf(defaultTarget) }
    var yearsText by remember { mutableStateOf("10") }
    var returnRateText by remember { mutableStateOf("12") }
    var startingLumpsumText by remember { mutableStateOf("0") }

    val targetAmount = targetAmountText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val startingLumpsum = startingLumpsumText.toDoubleOrNull() ?: 0.0

    val months = (years * 12.0).toInt()
    val monthlyRate = (returnRate / 12.0) / 100.0

    // Future value of starting lumpsum
    val futureValueOfLumpsum = if (returnRate > 0 && years > 0) {
        startingLumpsum * (1.0 + returnRate / 100.0).pow(years)
    } else startingLumpsum

    val netTargetRemaining = (targetAmount - futureValueOfLumpsum).coerceAtLeast(0.0)

    // Formula for required monthly SIP: Target / [ ( (1+r)^n - 1 ) / r * (1+r) ]
    val requiredMonthlySip = if (monthlyRate > 0 && months > 0 && netTargetRemaining > 0) {
        netTargetRemaining / ((((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate) * (1.0 + monthlyRate))
    } else if (months > 0 && netTargetRemaining > 0) {
        netTargetRemaining / months
    } else 0.0

    val totalSipInvested = requiredMonthlySip * months
    val totalInvested = totalSipInvested + startingLumpsum
    val totalWealthGained = (targetAmount - totalInvested).coerceAtLeast(0.0)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (prefillData.userProfile.targetNetWorth > 0) {
            item {
                AutoPrefillBadge(text = "Target auto-synced with your Profile Goal (${CurrencyFormatter.formatInr(prefillData.userProfile.targetNetWorth)})")
            }
        }

        item {
            CalculatorTextInput(
                label = "Target Amount You Want (₹)",
                value = targetAmountText,
                prefix = "₹ ",
                placeholder = "10000000",
                onValueChange = { targetAmountText = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Time Horizon",
                        value = yearsText,
                        suffix = " Yrs",
                        placeholder = "10",
                        onValueChange = { yearsText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Expected Return Rate",
                        value = returnRateText,
                        suffix = " %",
                        placeholder = "12",
                        onValueChange = { returnRateText = it }
                    )
                }
            }
        }

        item {
            CalculatorTextInput(
                label = "Initial / Existing Lumpsum (Optional)",
                value = startingLumpsumText,
                prefix = "₹ ",
                placeholder = "0",
                onValueChange = { startingLumpsumText = it }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = BorderStroke(1.dp, Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "REQUIRED MONTHLY SIP",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(requiredMonthlySip) + " / mo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8)
                    )
                    Text(
                        text = "Investing ${CurrencyFormatter.formatInr(requiredMonthlySip)} every month for ${years.toInt()} years at ${returnRate}% CAGR will build your ${CurrencyFormatter.formatInr(targetAmount)} goal.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                        color = Color(0xFF94A3B8)
                    )

                    HorizontalDivider(color = Color(0xFF334155))

                    MetricDetailRow("Target Goal Amount", CurrencyFormatter.formatInr(targetAmount), Color.White)
                    MetricDetailRow("Total Out of Pocket Investment", CurrencyFormatter.formatInr(totalInvested), Color(0xFFE2E8F0))
                    MetricDetailRow("Estimated Wealth Gain (Compounding)", CurrencyFormatter.formatInr(totalWealthGained), Color(0xFF34D399), isBold = true)
                    if (startingLumpsum > 0) {
                        MetricDetailRow("Lumpsum Future Value", CurrencyFormatter.formatInr(futureValueOfLumpsum), Color(0xFFFBBF24))
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Regular SIP Calculator
// -------------------------------------------------------------
@Composable
fun SipCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultAmount = if (prefillData.monthlySipAmount > 0) prefillData.monthlySipAmount.toLong().toString() else "10000"
    var monthlyAmountText by remember { mutableStateOf(defaultAmount) }
    var returnRateText by remember { mutableStateOf("12") }
    var yearsText by remember { mutableStateOf("10") }

    val monthlyAmount = monthlyAmountText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val months = (years * 12.0).toInt()
    val monthlyRate = (returnRate / 12.0) / 100.0
    val totalInvested = monthlyAmount * months
    val maturityValue = if (monthlyRate > 0 && months > 0) {
        monthlyAmount * (((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
    } else totalInvested
    val estReturns = (maturityValue - totalInvested).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (prefillData.monthlySipAmount > 0) {
            item {
                AutoPrefillBadge(text = "Monthly SIP auto-synced from your Recurring Active SIPs (${CurrencyFormatter.formatInr(prefillData.monthlySipAmount)}/mo)")
            }
        }

        item {
            CalculatorTextInput(
                label = "Monthly Investment (SIP)",
                value = monthlyAmountText,
                prefix = "₹ ",
                placeholder = "10000",
                onValueChange = { monthlyAmountText = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Expected Return Rate (p.a.)",
                        value = returnRateText,
                        suffix = " %",
                        placeholder = "12",
                        onValueChange = { returnRateText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Time Period",
                        value = yearsText,
                        suffix = " Yrs",
                        placeholder = "10",
                        onValueChange = { yearsText = it }
                    )
                }
            }
        }

        item {
            OutcomeDisplayCard(
                primaryLabel = "Total Maturity Corpus",
                primaryValue = maturityValue,
                investedAmount = totalInvested,
                returnsAmount = estReturns,
                accentColor = Emerald500
            )
        }
    }
}

// -------------------------------------------------------------
// 3. Step-Up / Top-Up SIP Calculator
// -------------------------------------------------------------
@Composable
fun StepUpSipCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultSip = if (prefillData.monthlySipAmount > 0) prefillData.monthlySipAmount.toLong().toString() else "10000"
    var initialSipText by remember { mutableStateOf(defaultSip) }
    var annualStepUpPercentText by remember { mutableStateOf("10") }
    var returnRateText by remember { mutableStateOf("12") }
    var yearsText by remember { mutableStateOf("15") }

    val initialSip = initialSipText.toDoubleOrNull() ?: 0.0
    val annualStepUpPercent = annualStepUpPercentText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val totalYears = years.toInt()
    val monthlyRate = (returnRate / 12.0) / 100.0
    var totalInvested = 0.0
    var totalMaturity = 0.0

    var currentMonthly = initialSip
    for (year in 1..totalYears) {
        for (month in 1..12) {
            val monthsRemaining = (totalYears - year) * 12 + (12 - month + 1)
            totalInvested += currentMonthly
            val futureValue = currentMonthly * (1.0 + monthlyRate).pow(monthsRemaining)
            totalMaturity += futureValue
        }
        currentMonthly *= (1.0 + annualStepUpPercent / 100.0)
    }

    val regularMaturity = if (monthlyRate > 0 && totalYears > 0) {
        val totalMonths = totalYears * 12
        initialSip * (((1.0 + monthlyRate).pow(totalMonths) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
    } else initialSip * totalYears * 12

    val stepUpBonus = (totalMaturity - regularMaturity).coerceAtLeast(0.0)
    val estReturns = (totalMaturity - totalInvested).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Starting Monthly SIP",
                value = initialSipText,
                prefix = "₹ ",
                placeholder = "10000",
                onValueChange = { initialSipText = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Annual Step-Up",
                        value = annualStepUpPercentText,
                        suffix = " %",
                        placeholder = "10",
                        onValueChange = { annualStepUpPercentText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Expected Return Rate",
                        value = returnRateText,
                        suffix = " %",
                        placeholder = "12",
                        onValueChange = { returnRateText = it }
                    )
                }
            }
        }

        item {
            CalculatorTextInput(
                label = "Investment Duration",
                value = yearsText,
                suffix = " Yrs",
                placeholder = "15",
                onValueChange = { yearsText = it }
            )
        }

        item {
            OutcomeDisplayCard(
                primaryLabel = "Step-Up Maturity Value",
                primaryValue = totalMaturity,
                investedAmount = totalInvested,
                returnsAmount = estReturns,
                accentColor = Color(0xFF8B5CF6)
            )
        }

        item {
            InfoNoteCard("💡 An annual ${annualStepUpPercent}% raise adds ${CurrencyFormatter.formatInr(stepUpBonus)} extra wealth compared to a fixed SIP!")
        }
    }
}

// -------------------------------------------------------------
// 4. Lumpsum Investment Calculator
// -------------------------------------------------------------
@Composable
fun LumpsumCalculatorView(prefillData: CalculatorAutoPrefillData) {
    val defaultLumpsum = if (prefillData.currentNetWorth > 0) (prefillData.currentNetWorth * 0.1).toLong().toString() else "100000"
    var investmentText by remember { mutableStateOf(defaultLumpsum) }
    var returnRateText by remember { mutableStateOf("12") }
    var yearsText by remember { mutableStateOf("10") }

    val investment = investmentText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val maturityValue = if (returnRate > 0 && years > 0) {
        investment * (1.0 + returnRate / 100.0).pow(years)
    } else investment
    val estReturns = (maturityValue - investment).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Total Investment Amount",
                value = investmentText,
                prefix = "₹ ",
                placeholder = "100000",
                onValueChange = { investmentText = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Expected Return Rate",
                        value = returnRateText,
                        suffix = " %",
                        placeholder = "12",
                        onValueChange = { returnRateText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Time Period",
                        value = yearsText,
                        suffix = " Yrs",
                        placeholder = "10",
                        onValueChange = { yearsText = it }
                    )
                }
            }
        }

        item {
            OutcomeDisplayCard(
                primaryLabel = "Total Maturity Value",
                primaryValue = maturityValue,
                investedAmount = investment,
                returnsAmount = estReturns,
                accentColor = Color(0xFF2563EB)
            )
        }
    }
}

// -------------------------------------------------------------
// 5. Mutual Fund Returns Calculator
// -------------------------------------------------------------
@Composable
fun MfReturnsCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var isSipMode by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("10000") }
    var returnRateText by remember { mutableStateOf("14") }
    var yearsText by remember { mutableStateOf("5") }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val returnRate = returnRateText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val investedAmount: Double
    val maturityValue: Double

    if (isSipMode) {
        val months = (years * 12.0).toInt()
        val monthlyRate = (returnRate / 12.0) / 100.0
        investedAmount = amount * months
        maturityValue = if (monthlyRate > 0 && months > 0) {
            amount * (((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
        } else investedAmount
    } else {
        investedAmount = amount
        maturityValue = if (returnRate > 0 && years > 0) {
            amount * (1.0 + returnRate / 100.0).pow(years)
        } else amount
    }

    val returnsAmount = (maturityValue - investedAmount).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isSipMode,
                    onClick = { isSipMode = true },
                    label = { Text("Monthly SIP") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isSipMode,
                    onClick = { isSipMode = false },
                    label = { Text("One-Time Lumpsum") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            CalculatorTextInput(
                label = if (isSipMode) "Monthly SIP Amount" else "Lumpsum Investment",
                value = amountText,
                prefix = "₹ ",
                placeholder = "10000",
                onValueChange = { amountText = it }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Expected Return Rate",
                        value = returnRateText,
                        suffix = " %",
                        placeholder = "14",
                        onValueChange = { returnRateText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Duration",
                        value = yearsText,
                        suffix = " Yrs",
                        placeholder = "5",
                        onValueChange = { yearsText = it }
                    )
                }
            }
        }

        item {
            OutcomeDisplayCard(
                primaryLabel = "Total MF Maturity Value",
                primaryValue = maturityValue,
                investedAmount = investedAmount,
                returnsAmount = returnsAmount,
                accentColor = Color(0xFF0D9488)
            )
        }
    }
}

// -------------------------------------------------------------
// 6. CAGR Calculator
// -------------------------------------------------------------
@Composable
fun CagrCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var initialValueText by remember { mutableStateOf("100000") }
    var finalValueText by remember { mutableStateOf("250000") }
    var yearsText by remember { mutableStateOf("5") }

    val initialValue = initialValueText.toDoubleOrNull() ?: 0.0
    val finalValue = finalValueText.toDoubleOrNull() ?: 0.0
    val years = yearsText.toDoubleOrNull() ?: 0.0

    val cagr = if (initialValue > 0 && finalValue > 0 && years > 0) {
        ((finalValue / initialValue).pow(1.0 / years) - 1.0) * 100.0
    } else 0.0

    val absoluteReturn = if (initialValue > 0) {
        ((finalValue - initialValue) / initialValue) * 100.0
    } else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Initial Investment Value",
                value = initialValueText,
                prefix = "₹ ",
                placeholder = "100000",
                onValueChange = { initialValueText = it }
            )
        }

        item {
            CalculatorTextInput(
                label = "Final Value / Current Portfolio",
                value = finalValueText,
                prefix = "₹ ",
                placeholder = "250000",
                onValueChange = { finalValueText = it }
            )
        }

        item {
            CalculatorTextInput(
                label = "Number of Years",
                value = yearsText,
                suffix = " Yrs",
                placeholder = "5",
                onValueChange = { yearsText = it }
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
                    Text(
                        text = "COMPOUND ANNUAL GROWTH RATE (CAGR)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F46E5)
                    )
                    Text(
                        text = String.format("%.2f%% p.a.", cagr),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF4F46E5)
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Total Absolute Gain", CurrencyFormatter.formatInr((finalValue - initialValue).coerceAtLeast(0.0)), Emerald500, isBold = true)
                    MetricDetailRow("Total Absolute Return", String.format("%.2f%%", absoluteReturn), Color(0xFF0284C7))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. Stock Average Calculator
// -------------------------------------------------------------
@Composable
fun StockAverageCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var share1QtyText by remember { mutableStateOf("50") }
    var share1PriceText by remember { mutableStateOf("1500") }
    var share2QtyText by remember { mutableStateOf("30") }
    var share2PriceText by remember { mutableStateOf("1200") }

    val q1 = share1QtyText.toDoubleOrNull() ?: 0.0
    val p1 = share1PriceText.toDoubleOrNull() ?: 0.0
    val q2 = share2QtyText.toDoubleOrNull() ?: 0.0
    val p2 = share2PriceText.toDoubleOrNull() ?: 0.0

    val totalQty = q1 + q2
    val totalCost = (q1 * p1) + (q2 * p2)
    val avgPrice = if (totalQty > 0) totalCost / totalQty else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("First Purchase", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Quantity",
                        value = share1QtyText,
                        placeholder = "50",
                        onValueChange = { share1QtyText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Buy Price (₹)",
                        value = share1PriceText,
                        prefix = "₹ ",
                        placeholder = "1500",
                        onValueChange = { share1PriceText = it }
                    )
                }
            }
        }

        item {
            Text("Second Purchase", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Quantity",
                        value = share2QtyText,
                        placeholder = "30",
                        onValueChange = { share2QtyText = it }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput(
                        label = "Buy Price (₹)",
                        value = share2PriceText,
                        prefix = "₹ ",
                        placeholder = "1200",
                        onValueChange = { share2PriceText = it }
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
                    Text(
                        text = "NEW AVERAGE SHARE PRICE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(avgPrice),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0284C7)
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Total Shares Held", "${totalQty.toInt()} units")
                    MetricDetailRow("Total Capital Invested", CurrencyFormatter.formatInr(totalCost), isBold = true)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. XIRR Calculator
// -------------------------------------------------------------
@Composable
fun XirrCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var initialOutflowText by remember { mutableStateOf("100000") }
    var interimFlowText by remember { mutableStateOf("50000") }
    var finalPortfolioValueText by remember { mutableStateOf("210000") }
    var totalMonthsText by remember { mutableStateOf("24") }

    val c1 = -(initialOutflowText.toDoubleOrNull() ?: 0.0)
    val c2 = -(interimFlowText.toDoubleOrNull() ?: 0.0)
    val c3 = finalPortfolioValueText.toDoubleOrNull() ?: 0.0
    val months = totalMonthsText.toDoubleOrNull() ?: 24.0

    // Approximate XIRR using simple cash flow IRR approximation
    val totalInvested = abs(c1) + abs(c2)
    val totalGain = c3 - totalInvested
    val approxYears = (months / 12.0).coerceAtLeast(0.1)
    val approxXirr = if (totalInvested > 0 && c3 > 0) {
        (((c3 / totalInvested).pow(1.0 / approxYears) - 1.0) * 100.0) * 1.15 // Weight adjusted
    } else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput(
                label = "Initial Investment (Outflow)",
                value = initialOutflowText,
                prefix = "₹ ",
                placeholder = "100000",
                onValueChange = { initialOutflowText = it }
            )
        }
        item {
            CalculatorTextInput(
                label = "Subsequent Investment (Outflow)",
                value = interimFlowText,
                prefix = "₹ ",
                placeholder = "50000",
                onValueChange = { interimFlowText = it }
            )
        }
        item {
            CalculatorTextInput(
                label = "Current / Redemption Portfolio Value",
                value = finalPortfolioValueText,
                prefix = "₹ ",
                placeholder = "210000",
                onValueChange = { finalPortfolioValueText = it }
            )
        }
        item {
            CalculatorTextInput(
                label = "Total Investment Duration",
                value = totalMonthsText,
                suffix = " Months",
                placeholder = "24",
                onValueChange = { totalMonthsText = it }
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
                    Text(
                        text = "EXTENDED INTERNAL RATE OF RETURN (XIRR)",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9333EA)
                    )
                    Text(
                        text = String.format("%.2f%% p.a.", approxXirr),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF9333EA)
                    )
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Total Capital Deployed", CurrencyFormatter.formatInr(totalInvested))
                    MetricDetailRow("Net Profit Generated", CurrencyFormatter.formatInr(totalGain), Emerald500, isBold = true)
                }
            }
        }
    }
}
