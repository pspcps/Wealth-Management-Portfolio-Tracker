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
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import kotlin.math.pow

// -------------------------------------------------------------
// 1. General EMI Calculator
// -------------------------------------------------------------
@Composable
fun EmiCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var loanAmountText by remember { mutableStateOf("1000000") }
    var interestRateText by remember { mutableStateOf("9.5") }
    var tenureYearsText by remember { mutableStateOf("5") }

    val principal = loanAmountText.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateText.toDoubleOrNull() ?: 0.0
    val years = tenureYearsText.toDoubleOrNull() ?: 0.0

    val months = (years * 12.0).toInt()
    val monthlyRate = (annualRate / 12.0) / 100.0

    val emi = if (monthlyRate > 0 && months > 0 && principal > 0) {
        val factor = (1.0 + monthlyRate).pow(months)
        principal * monthlyRate * factor / (factor - 1.0)
    } else if (months > 0) principal / months else 0.0

    val totalPayment = emi * months
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Loan Principal Amount", loanAmountText, { loanAmountText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Interest Rate", interestRateText, { interestRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Loan Tenure", tenureYearsText, { tenureYearsText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Monthly EMI Payable",
                primaryValue = emi,
                investedAmount = principal,
                returnsAmount = totalInterest,
                investedLabel = "Principal Borrowed",
                returnsLabel = "Total Interest Cost",
                accentColor = Color(0xFFDC2626)
            )
        }
        item {
            InfoNoteCard("💳 Total Amount Repaid: ${CurrencyFormatter.formatInr(totalPayment)} over ${years.toInt()} years (${months} monthly installments).")
        }
    }
}

// -------------------------------------------------------------
// 2. Home Loan EMI Calculator
// -------------------------------------------------------------
@Composable
fun HomeLoanEmiCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var homeLoanText by remember { mutableStateOf("5000000") }
    var interestRateText by remember { mutableStateOf("8.5") }
    var tenureYearsText by remember { mutableStateOf("20") }

    val principal = homeLoanText.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateText.toDoubleOrNull() ?: 0.0
    val years = tenureYearsText.toDoubleOrNull() ?: 0.0

    val months = (years * 12.0).toInt()
    val monthlyRate = (annualRate / 12.0) / 100.0

    val emi = if (monthlyRate > 0 && months > 0 && principal > 0) {
        val factor = (1.0 + monthlyRate).pow(months)
        principal * monthlyRate * factor / (factor - 1.0)
    } else if (months > 0) principal / months else 0.0

    val totalPayment = emi * months
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Home Loan Amount", homeLoanText, { homeLoanText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Interest Rate", interestRateText, { interestRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Tenure", tenureYearsText, { tenureYearsText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Monthly Home Loan EMI",
                primaryValue = emi,
                investedAmount = principal,
                returnsAmount = totalInterest,
                investedLabel = "Loan Principal",
                returnsLabel = "Total Interest Outflow",
                accentColor = Color(0xFF0284C7)
            )
        }
    }
}

// -------------------------------------------------------------
// 3. Car Loan EMI Calculator
// -------------------------------------------------------------
@Composable
fun CarLoanEmiCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var carLoanText by remember { mutableStateOf("800000") }
    var interestRateText by remember { mutableStateOf("9.0") }
    var tenureYearsText by remember { mutableStateOf("5") }

    val principal = carLoanText.toDoubleOrNull() ?: 0.0
    val annualRate = interestRateText.toDoubleOrNull() ?: 0.0
    val years = tenureYearsText.toDoubleOrNull() ?: 0.0

    val months = (years * 12.0).toInt()
    val monthlyRate = (annualRate / 12.0) / 100.0

    val emi = if (monthlyRate > 0 && months > 0 && principal > 0) {
        val factor = (1.0 + monthlyRate).pow(months)
        principal * monthlyRate * factor / (factor - 1.0)
    } else if (months > 0) principal / months else 0.0

    val totalPayment = emi * months
    val totalInterest = (totalPayment - principal).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Car Loan Amount", carLoanText, { carLoanText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Interest Rate", interestRateText, { interestRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Tenure", tenureYearsText, { tenureYearsText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Monthly Car EMI",
                primaryValue = emi,
                investedAmount = principal,
                returnsAmount = totalInterest,
                investedLabel = "Car Loan Amount",
                returnsLabel = "Total Interest",
                accentColor = Color(0xFF7C3AED)
            )
        }
    }
}

// -------------------------------------------------------------
// 4. Flat vs Reducing Rate Calculator
// -------------------------------------------------------------
@Composable
fun FlatVsReducingRateCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var loanAmountText by remember { mutableStateOf("500000") }
    var flatRateText by remember { mutableStateOf("8.5") }
    var tenureYearsText by remember { mutableStateOf("3") }

    val principal = loanAmountText.toDoubleOrNull() ?: 0.0
    val flatRate = flatRateText.toDoubleOrNull() ?: 0.0
    val years = tenureYearsText.toDoubleOrNull() ?: 0.0

    val months = (years * 12.0).toInt()

    // Flat Rate Calculation: Interest = P * R * T
    val flatTotalInterest = principal * (flatRate / 100.0) * years
    val flatTotalPayment = principal + flatTotalInterest
    val flatEmi = if (months > 0) flatTotalPayment / months else 0.0

    // Approximate equivalent reducing balance interest rate
    // Reducing rate is roughly 1.8x to 1.9x the flat rate for medium tenures
    val approxReducingRate = flatRate * 1.85
    val reducingMonthlyRate = (approxReducingRate / 12.0) / 100.0
    val reducingEmi = if (reducingMonthlyRate > 0 && months > 0 && principal > 0) {
        val factor = (1.0 + reducingMonthlyRate).pow(months)
        principal * reducingMonthlyRate * factor / (factor - 1.0)
    } else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Loan Principal", loanAmountText, { loanAmountText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Advertised Flat Rate", flatRateText, { flatRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Tenure", tenureYearsText, { tenureYearsText = it }, suffix = " Yrs")
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
                    Text("TRUE EFFECTIVE REDUCING RATE (EIR)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                    Text(String.format("%.2f%% p.a.", approxReducingRate), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFE11D48))
                    Text("An advertised flat rate of ${flatRate}% actually equals ${String.format("%.2f%%", approxReducingRate)} in standard reducing balance terms!", style = MaterialTheme.typography.bodySmall, color = HighDensityTextSecondary)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Monthly Flat EMI", CurrencyFormatter.formatInr(flatEmi) + " / mo", isBold = true)
                    MetricDetailRow("Total Interest Paid (Flat)", CurrencyFormatter.formatInr(flatTotalInterest))
                    MetricDetailRow("Total Repayment", CurrencyFormatter.formatInr(flatTotalPayment))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. Simple Interest Calculator
// -------------------------------------------------------------
@Composable
fun SimpleInterestCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var principalText by remember { mutableStateOf("100000") }
    var rateText by remember { mutableStateOf("8.0") }
    var timeYearsText by remember { mutableStateOf("3") }

    val p = principalText.toDoubleOrNull() ?: 0.0
    val r = rateText.toDoubleOrNull() ?: 0.0
    val t = timeYearsText.toDoubleOrNull() ?: 0.0

    val simpleInterest = (p * r * t) / 100.0
    val totalAmount = p + simpleInterest

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Principal Amount (P)", principalText, { principalText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Annual Rate (R)", rateText, { rateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Time (T)", timeYearsText, { timeYearsText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Total Amount (P + SI)",
                primaryValue = totalAmount,
                investedAmount = p,
                returnsAmount = simpleInterest,
                investedLabel = "Principal Amount",
                returnsLabel = "Simple Interest (P × R × T / 100)",
                accentColor = Color(0xFF475569)
            )
        }
    }
}

// -------------------------------------------------------------
// 6. Compound Interest Calculator
// -------------------------------------------------------------
@Composable
fun CompoundInterestCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var principalText by remember { mutableStateOf("100000") }
    var rateText by remember { mutableStateOf("8.0") }
    var timeYearsText by remember { mutableStateOf("5") }
    var compoundingFreq by remember { mutableStateOf(4) } // 1 = Annual, 2 = Semi, 4 = Quarterly, 12 = Monthly

    val p = principalText.toDoubleOrNull() ?: 0.0
    val r = rateText.toDoubleOrNull() ?: 0.0
    val t = timeYearsText.toDoubleOrNull() ?: 0.0

    val totalAmount = if (r > 0 && t > 0) {
        p * (1.0 + (r / 100.0) / compoundingFreq).pow(compoundingFreq * t)
    } else p

    val compoundInterest = (totalAmount - p).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Principal Amount", principalText, { principalText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Interest Rate", rateText, { rateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Time Period", timeYearsText, { timeYearsText = it }, suffix = " Yrs")
                }
            }
        }
        item {
            Text("Compounding Frequency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                mapOf(1 to "Yearly", 2 to "Half-Yr", 4 to "Quarterly", 12 to "Monthly").forEach { (freq, label) ->
                    FilterChip(
                        selected = compoundingFreq == freq,
                        onClick = { compoundingFreq = freq },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        item {
            OutcomeDisplayCard(
                primaryLabel = "Total Maturity Amount",
                primaryValue = totalAmount,
                investedAmount = p,
                returnsAmount = compoundInterest,
                investedLabel = "Principal Amount",
                returnsLabel = "Compound Interest Earned",
                accentColor = Emerald500
            )
        }
    }
}

// -------------------------------------------------------------
// 7. ROI (Return on Investment) Calculator
// -------------------------------------------------------------
@Composable
fun RoiCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var investedText by remember { mutableStateOf("200000") }
    var returnedText by remember { mutableStateOf("320000") }

    val invested = investedText.toDoubleOrNull() ?: 0.0
    val returned = returnedText.toDoubleOrNull() ?: 0.0

    val netGain = returned - invested
    val roiPercent = if (invested > 0) (netGain / invested) * 100.0 else 0.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Amount Invested", investedText, { investedText = it }, prefix = "₹ ")
        }
        item {
            CalculatorTextInput("Amount Returned / Current Value", returnedText, { returnedText = it }, prefix = "₹ ")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("RETURN ON INVESTMENT (ROI)", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = if (netGain >= 0) Emerald500 else Color(0xFFDC2626))
                    Text(String.format("%.2f%%", roiPercent), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = if (netGain >= 0) Emerald500 else Color(0xFFDC2626))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Net Gain / Profit", CurrencyFormatter.formatInr(netGain), if (netGain >= 0) Emerald500 else Color(0xFFDC2626), isBold = true)
                    MetricDetailRow("Capital Multiple", String.format("%.2fx", if (invested > 0) returned / invested else 1.0))
                }
            }
        }
    }
}
