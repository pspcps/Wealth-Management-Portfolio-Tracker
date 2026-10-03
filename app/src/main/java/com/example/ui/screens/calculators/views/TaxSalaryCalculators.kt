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
import com.example.ui.theme.HighDensityPrimary
import com.example.ui.theme.HighDensityTextSecondary
import kotlin.math.pow

// -------------------------------------------------------------
// 1. Income Tax Calculator (New vs Old Regime)
// -------------------------------------------------------------
@Composable
fun IncomeTaxCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var grossSalaryText by remember { mutableStateOf("1200000") }
    var sec80cText by remember { mutableStateOf("150000") }
    var sec80dText by remember { mutableStateOf("25000") }
    var hraDeductionText by remember { mutableStateOf("100000") }
    var homeLoanInterestText by remember { mutableStateOf("0") }

    val grossSalary = grossSalaryText.toDoubleOrNull() ?: 0.0
    val sec80c = (sec80cText.toDoubleOrNull() ?: 0.0).coerceAtMost(150000.0)
    val sec80d = (sec80dText.toDoubleOrNull() ?: 0.0).coerceAtMost(50000.0)
    val hraDed = hraDeductionText.toDoubleOrNull() ?: 0.0
    val homeLoan = (homeLoanInterestText.toDoubleOrNull() ?: 0.0).coerceAtMost(200000.0)

    // NEW REGIME (Standard deduction: ₹75,000 for FY 24-25/25-26, Rebate u/s 87A up to ₹7L/7.75L)
    val newStdDed = 75000.0
    val newTaxable = (grossSalary - newStdDed).coerceAtLeast(0.0)
    var newTax = 0.0
    if (newTaxable > 300000.0) {
        if (newTaxable <= 700000.0) {
            newTax = 0.0 // Rebate u/s 87A makes tax zero up to 7L
        } else {
            // Slabs: 0-3L: 0%, 3-7L: 5% (₹20,000), 7-10L: 10% (₹30,000), 10-12L: 15% (₹30,000), 12-15L: 20% (₹60,000), >15L: 30%
            if (newTaxable > 300000) newTax += ((newTaxable.coerceAtMost(700000.0) - 300000.0) * 0.05)
            if (newTaxable > 700000) newTax += ((newTaxable.coerceAtMost(1000000.0) - 700000.0) * 0.10)
            if (newTaxable > 1000000) newTax += ((newTaxable.coerceAtMost(1200000.0) - 1000000.0) * 0.15)
            if (newTaxable > 1200000) newTax += ((newTaxable.coerceAtMost(1500000.0) - 1200000.0) * 0.20)
            if (newTaxable > 1500000) newTax += ((newTaxable - 1500000.0) * 0.30)
            newTax *= 1.04 // 4% Cess
        }
    }

    // OLD REGIME (Standard deduction: ₹50,000)
    val oldStdDed = 50000.0
    val totalOldDeductions = oldStdDed + sec80c + sec80d + hraDed + homeLoan
    val oldTaxable = (grossSalary - totalOldDeductions).coerceAtLeast(0.0)
    var oldTax = 0.0
    if (oldTaxable > 250000.0) {
        if (oldTaxable <= 500000.0) {
            oldTax = 0.0 // Rebate u/s 87A makes tax zero up to 5L
        } else {
            // Slabs: 0-2.5L: 0%, 2.5-5L: 5% (₹12,500), 5-10L: 20% (₹1,00,000), >10L: 30%
            if (oldTaxable > 250000) oldTax += ((oldTaxable.coerceAtMost(500000.0) - 250000.0) * 0.05)
            if (oldTaxable > 500000) oldTax += ((oldTaxable.coerceAtMost(1000000.0) - 500000.0) * 0.20)
            if (oldTaxable > 1000000) oldTax += ((oldTaxable - 1000000.0) * 0.30)
            oldTax *= 1.04 // 4% Cess
        }
    }

    val savingsWithNew = (oldTax - newTax)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Annual Gross CTC / Total Income", grossSalaryText, { grossSalaryText = it }, prefix = "₹ ")
        }
        item {
            Text("Old Regime Deductions", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("80C (Max 1.5L)", sec80cText, { sec80cText = it }, prefix = "₹ ")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("80D (Health Ins)", sec80dText, { sec80dText = it }, prefix = "₹ ")
                }
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("HRA Exemption", hraDeductionText, { hraDeductionText = it }, prefix = "₹ ")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Home Loan Int (Sec 24)", homeLoanInterestText, { homeLoanInterestText = it }, prefix = "₹ ")
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
                    Text("TAX COMPARISON SUMMARY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("New Tax Regime", style = MaterialTheme.typography.bodySmall, color = HighDensityTextSecondary)
                            Text(CurrencyFormatter.formatInr(newTax), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (newTax <= oldTax) Emerald500 else Color(0xFFDC2626))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Old Tax Regime", style = MaterialTheme.typography.bodySmall, color = HighDensityTextSecondary)
                            Text(CurrencyFormatter.formatInr(oldTax), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (oldTax < newTax) Emerald500 else Color(0xFFDC2626))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    val recommendation = if (savingsWithNew > 0) "💡 You save ${CurrencyFormatter.formatInr(savingsWithNew)} by choosing the New Tax Regime!" else if (savingsWithNew < 0) "💡 You save ${CurrencyFormatter.formatInr(-savingsWithNew)} by choosing the Old Tax Regime!" else "💡 Tax is identical in both regimes."
                    Text(recommendation, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = Color(0xFF0284C7))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 2. Salary / Take-Home Pay Calculator
// -------------------------------------------------------------
@Composable
fun SalaryTakeHomeCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var ctcText by remember { mutableStateOf("1200000") }
    var bonusText by remember { mutableStateOf("100000") }
    var professionalTaxText by remember { mutableStateOf("200") }

    val ctc = ctcText.toDoubleOrNull() ?: 0.0
    val bonus = bonusText.toDoubleOrNull() ?: 0.0
    val pTax = professionalTaxText.toDoubleOrNull() ?: 200.0

    val annualFixed = (ctc - bonus).coerceAtLeast(0.0)
    val monthlyGross = annualFixed / 12.0
    val basicPay = monthlyGross * 0.50 // Standard 50% basic
    val employeePf = (basicPay * 0.12).coerceAtMost(1800.0 * 2) // Monthly PF
    val approxMonthlyTds = (monthlyGross * 0.08) // Approximate TDS estimate
    val monthlyInHand = (monthlyGross - employeePf - pTax - approxMonthlyTds).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Annual Cost to Company (CTC)", ctcText, { ctcText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Annual Bonus / Variable", bonusText, { bonusText = it }, prefix = "₹ ")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Prof. Tax (Monthly)", professionalTaxText, { professionalTaxText = it }, prefix = "₹ ")
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
                    Text("ESTIMATED MONTHLY IN-HAND SALARY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Emerald500)
                    Text(CurrencyFormatter.formatInr(monthlyInHand) + " / mo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Monthly Gross Salary", CurrencyFormatter.formatInr(monthlyGross) + " / mo")
                    MetricDetailRow("Employee PF Contribution", "- ${CurrencyFormatter.formatInr(employeePf)} / mo")
                    MetricDetailRow("Estimated Monthly TDS", "- ${CurrencyFormatter.formatInr(approxMonthlyTds)} / mo")
                    MetricDetailRow("Professional Tax", "- ₹${pTax.toInt()} / mo")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. HRA Exemption Calculator (Section 10(13A))
// -------------------------------------------------------------
@Composable
fun HraCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var basicSalaryText by remember { mutableStateOf("600000") }
    var hraReceivedText by remember { mutableStateOf("240000") }
    var rentPaidText by remember { mutableStateOf("300000") }
    var isMetro by remember { mutableStateOf(true) }

    val basic = basicSalaryText.toDoubleOrNull() ?: 0.0
    val hraReceived = hraReceivedText.toDoubleOrNull() ?: 0.0
    val rentPaid = rentPaidText.toDoubleOrNull() ?: 0.0

    // Rule 2A: Minimum of 3 criteria
    val c1 = hraReceived
    val c2 = (rentPaid - (0.10 * basic)).coerceAtLeast(0.0)
    val c3 = if (isMetro) 0.50 * basic else 0.40 * basic

    val exemptHra = minOf(c1, c2, c3)
    val taxableHra = (hraReceived - exemptHra).coerceAtLeast(0.0)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Annual Basic Salary + DA", basicSalaryText, { basicSalaryText = it }, prefix = "₹ ")
        }
        item {
            CalculatorTextInput("Annual HRA Received from Employer", hraReceivedText, { hraReceivedText = it }, prefix = "₹ ")
        }
        item {
            CalculatorTextInput("Total Annual Rent Paid", rentPaidText, { rentPaidText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isMetro,
                    onClick = { isMetro = true },
                    label = { Text("Metro City (50%)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isMetro,
                    onClick = { isMetro = false },
                    label = { Text("Non-Metro (40%)") },
                    modifier = Modifier.weight(1f)
                )
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
                    Text("HRA TAX EXEMPTION STATUS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF8B5CF6))
                    Text(CurrencyFormatter.formatInr(exemptHra) + " Exempt", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Emerald500)
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Tax-Exempt HRA (Sec 10(13A))", CurrencyFormatter.formatInr(exemptHra), Emerald500, isBold = true)
                    MetricDetailRow("Taxable HRA Balance", CurrencyFormatter.formatInr(taxableHra), Color(0xFFDC2626))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. GST Calculator
// -------------------------------------------------------------
@Composable
fun GstCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var amountText by remember { mutableStateOf("10000") }
    var selectedGstRate by remember { mutableStateOf(18) }
    var isAddGst by remember { mutableStateOf(true) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val rate = selectedGstRate.toDouble()

    val gstAmount: Double
    val totalAmount: Double
    val basePrice: Double

    if (isAddGst) {
        basePrice = amount
        gstAmount = (amount * rate) / 100.0
        totalAmount = basePrice + gstAmount
    } else {
        totalAmount = amount
        basePrice = amount / (1.0 + rate / 100.0)
        gstAmount = totalAmount - basePrice
    }

    val cgst = gstAmount / 2.0
    val sgst = gstAmount / 2.0

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = isAddGst,
                    onClick = { isAddGst = true },
                    label = { Text("Add GST (Exclusive)") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isAddGst,
                    onClick = { isAddGst = false },
                    label = { Text("Remove GST (Inclusive)") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            CalculatorTextInput("Amount (₹)", amountText, { amountText = it }, prefix = "₹ ")
        }
        item {
            Text("GST Slab Rate", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 12, 18, 28).forEach { r ->
                    FilterChip(
                        selected = selectedGstRate == r,
                        onClick = { selectedGstRate = r },
                        label = { Text("$r%") },
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
                    Text("TOTAL FINAL PRICE", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    Text(CurrencyFormatter.formatInr(totalAmount), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0284C7))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Net Base Price", CurrencyFormatter.formatInr(basePrice))
                    MetricDetailRow("Total GST Amount ($selectedGstRate%)", CurrencyFormatter.formatInr(gstAmount), Color(0xFFDC2626), isBold = true)
                    MetricDetailRow("CGST (${selectedGstRate / 2}%)", CurrencyFormatter.formatInr(cgst))
                    MetricDetailRow("SGST (${selectedGstRate / 2}%)", CurrencyFormatter.formatInr(sgst))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 5. TDS Calculator
// -------------------------------------------------------------
@Composable
fun TdsCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var amountText by remember { mutableStateOf("100000") }
    var selectedSection by remember { mutableStateOf("194J") }

    val amount = amountText.toDoubleOrNull() ?: 0.0

    val (sectionName, tdsRate, threshold) = when (selectedSection) {
        "194J" -> Triple("Professional & Technical Fees (194J)", 10.0, 30000.0)
        "194C" -> Triple("Contractor Payment (194C)", 2.0, 30000.0)
        "194I" -> Triple("Rent on Land/Building (194I)", 10.0, 240000.0)
        "194A" -> Triple("Interest on Bank FD (194A)", 10.0, 40000.0)
        else -> Triple("Salary TDS (192)", 10.0, 250000.0)
    }

    val isApplicable = amount > threshold
    val tdsDeducted = if (isApplicable) (amount * tdsRate) / 100.0 else 0.0
    val netPayout = amount - tdsDeducted

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Transaction / Payment Amount", amountText, { amountText = it }, prefix = "₹ ")
        }
        item {
            Text("Payment Type & TDS Section", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "194J" to "Professional Fees (10%)",
                    "194C" to "Contractor / Vendor (2%)",
                    "194I" to "Rent (>₹2.4L/yr - 10%)",
                    "194A" to "Bank FD Interest (10%)"
                ).forEach { (sec, label) ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        label = { Text(label, fontSize = 11.5.sp) },
                        modifier = Modifier.fillMaxWidth()
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
                    Text("NET PAYOUT AFTER TDS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF7C3AED))
                    Text(CurrencyFormatter.formatInr(netPayout), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7C3AED))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("TDS Deducted (${tdsRate.toInt()}%)", CurrencyFormatter.formatInr(tdsDeducted), Color(0xFFDC2626), isBold = true)
                    MetricDetailRow("TDS Exemption Threshold", "₹${threshold.toLong()} / year")
                    MetricDetailRow("TDS Applicable?", if (isApplicable) "Yes (Exceeds threshold)" else "No TDS (Below threshold)", if (isApplicable) Color(0xFFDC2626) else Emerald500)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 6. Brokerage & Charges Calculator
// -------------------------------------------------------------
@Composable
fun BrokerageChargesCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var buyPriceText by remember { mutableStateOf("1000") }
    var sellPriceText by remember { mutableStateOf("1100") }
    var qtyText by remember { mutableStateOf("100") }
    var isDelivery by remember { mutableStateOf(true) }

    val buyPrice = buyPriceText.toDoubleOrNull() ?: 0.0
    val sellPrice = sellPriceText.toDoubleOrNull() ?: 0.0
    val qty = qtyText.toDoubleOrNull() ?: 0.0

    val turnover = (buyPrice + sellPrice) * qty
    val grossProfit = (sellPrice - buyPrice) * qty

    // Charges
    val brokerage = if (isDelivery) 0.0 else 40.0 // ₹20 per executed order intraday, ₹0 delivery (discount broker)
    val stt = if (isDelivery) turnover * 0.001 else (sellPrice * qty) * 0.00025
    val exchFee = turnover * 0.0000345
    val sebiCharges = turnover * 0.000001
    val stampDuty = (buyPrice * qty) * 0.00015
    val gst = (brokerage + exchFee + sebiCharges) * 0.18

    val totalCharges = brokerage + stt + exchFee + sebiCharges + stampDuty + gst
    val netProfit = grossProfit - totalCharges

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = isDelivery, onClick = { isDelivery = true }, label = { Text("Equity Delivery") }, modifier = Modifier.weight(1f))
                FilterChip(selected = !isDelivery, onClick = { isDelivery = false }, label = { Text("Equity Intraday") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Buy Price", buyPriceText, { buyPriceText = it }, prefix = "₹ ")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Sell Price", sellPriceText, { sellPriceText = it }, prefix = "₹ ")
                }
            }
        }
        item {
            CalculatorTextInput("Quantity (Shares)", qtyText, { qtyText = it }, suffix = " Qty")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("NET REALIZED PROFIT / LOSS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = if (netProfit >= 0) Emerald500 else Color(0xFFDC2626))
                    Text(CurrencyFormatter.formatInr(netProfit), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = if (netProfit >= 0) Emerald500 else Color(0xFFDC2626))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Gross Profit", CurrencyFormatter.formatInr(grossProfit))
                    MetricDetailRow("Total Taxes & Charges", "- ${CurrencyFormatter.formatInr(totalCharges)}", Color(0xFFDC2626), isBold = true)
                    MetricDetailRow("STT / CTT", CurrencyFormatter.formatInr(stt))
                    MetricDetailRow("Brokerage + GST", CurrencyFormatter.formatInr(brokerage + gst))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 7. Margin Calculator
// -------------------------------------------------------------
@Composable
fun MarginCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var balanceText by remember { mutableStateOf("50000") }
    var multiplierText by remember { mutableStateOf("5") }

    val balance = balanceText.toDoubleOrNull() ?: 0.0
    val multiplier = multiplierText.toDoubleOrNull() ?: 5.0

    val totalPurchasingPower = balance * multiplier

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Available Account Balance / Cash", balanceText, { balanceText = it }, prefix = "₹ ")
        }
        item {
            CalculatorTextInput("Margin Multiplier (e.g. 5x for MIS)", multiplierText, { multiplierText = it }, suffix = " x")
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("TOTAL TRADING PURCHASING POWER", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    Text(CurrencyFormatter.formatInr(totalPurchasingPower), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("Required Margin (Cash)", CurrencyFormatter.formatInr(balance))
                    MetricDetailRow("Broker Leverage Provided", "${multiplier.toInt()}x Leverage")
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 8. Inflation / Purchasing Power Calculator
// -------------------------------------------------------------
@Composable
fun InflationPurchasingPowerCalculatorView(prefillData: CalculatorAutoPrefillData) {
    var currentPriceText by remember { mutableStateOf("100000") }
    var inflationRateText by remember { mutableStateOf("6") }
    var yearsText by remember { mutableStateOf("10") }

    val currentPrice = currentPriceText.toDoubleOrNull() ?: 0.0
    val inflationRate = inflationRateText.toDoubleOrNull() ?: 6.0
    val years = yearsText.toDoubleOrNull() ?: 10.0

    val futureCost = currentPrice * (1.0 + inflationRate / 100.0).pow(years)
    val purchasingPowerRemaining = currentPrice / (1.0 + inflationRate / 100.0).pow(years)

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            CalculatorTextInput("Current Cost / Price (₹)", currentPriceText, { currentPriceText = it }, prefix = "₹ ")
        }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Inflation Rate (p.a.)", inflationRateText, { inflationRateText = it }, suffix = " %")
                }
                Box(modifier = Modifier.weight(1f)) {
                    CalculatorTextInput("Years Ahead", yearsText, { yearsText = it }, suffix = " Yrs")
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
                    Text("FUTURE INFLATION-ADJUSTED COST", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                    Text(CurrencyFormatter.formatInr(futureCost), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color(0xFFEA580C))
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    MetricDetailRow("What ₹${CurrencyFormatter.formatInr(currentPrice)} buys in ${years.toInt()} yrs", CurrencyFormatter.formatInr(purchasingPowerRemaining), Color(0xFFDC2626), isBold = true)
                    MetricDetailRow("Price Surge Percentage", String.format("+%.1f%%", ((futureCost - currentPrice) / currentPrice) * 100.0))
                }
            }
        }
    }
}
