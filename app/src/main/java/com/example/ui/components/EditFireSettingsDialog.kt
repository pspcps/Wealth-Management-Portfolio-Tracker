package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.FireSettings
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFireSettingsDialog(
    currentSettings: FireSettings,
    onDismiss: () -> Unit,
    onSave: (FireSettings) -> Unit
) {
    var returnRateText by remember { mutableStateOf(currentSettings.expectedReturnRate.toString()) }
    var inflationRateText by remember { mutableStateOf(currentSettings.inflationRate.toString()) }
    var monthlyExpensesText by remember { mutableStateOf(Math.round(currentSettings.monthlyExpenses).toString()) }
    var monthlyInvestmentText by remember { mutableStateOf(Math.round(currentSettings.monthlyInvestment).toString()) }
    var currentAgeText by remember { mutableStateOf(currentSettings.currentAge.toString()) }
    var targetAgeText by remember { mutableStateOf(currentSettings.targetFireAge.toString()) }
    var swrRateText by remember { mutableStateOf(currentSettings.swrRate.toString()) }
    var stepUpText by remember { mutableStateOf(currentSettings.annualStepUpRate.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("dialog_fire_settings"),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF97316).copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFEA580C),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "FIRE & Freedom Assumptions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                    Text(
                        text = "Customize Expected Returns, Inflation & Lifestyle",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Presets Banner
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Standard Defaults:",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextSecondary
                            )
                            Text(
                                text = "12% Return • 6% Inflation • 3.5% SWR",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        FilledTonalButton(
                            onClick = {
                                returnRateText = "12.0"
                                inflationRateText = "6.0"
                                swrRateText = "3.5"
                                stepUpText = "8.0"
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Reset",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Row 1: Expected Return & Inflation Rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = returnRateText,
                        onValueChange = { returnRateText = it },
                        label = { Text("Expected Return (%)", fontSize = 11.sp) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fire_return_rate"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = inflationRateText,
                        onValueChange = { inflationRateText = it },
                        label = { Text("Inflation Rate (%)", fontSize = 11.sp) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fire_inflation_rate"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Row 2: Monthly Expenses & Monthly Investments
                OutlinedTextField(
                    value = monthlyExpensesText,
                    onValueChange = { monthlyExpensesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monthly Lifestyle Expenses (Today's ₹)") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = HighDensityTextPrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fire_monthly_expenses"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = monthlyInvestmentText,
                    onValueChange = { monthlyInvestmentText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Monthly Investments / Total SIPs") },
                    prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_fire_monthly_investment"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                // Row 3: Current Age & Target FIRE Age
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = currentAgeText,
                        onValueChange = { currentAgeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Current Age", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fire_current_age"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = targetAgeText,
                        onValueChange = { targetAgeText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Age", fontSize = 11.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fire_target_age"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // Row 4: Safe Withdrawal Rate (SWR) & Annual Step-Up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = swrRateText,
                        onValueChange = { swrRateText = it },
                        label = { Text("Safe Withdrawal Rate", fontSize = 11.sp) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fire_swr_rate"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = stepUpText,
                        onValueChange = { stepUpText = it },
                        label = { Text("Annual SIP Step-Up", fontSize = 11.sp) },
                        suffix = { Text("%", fontWeight = FontWeight.Bold) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_fire_step_up"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                // SWR Guidance note
                Text(
                    text = "💡 Tip: 3.5% SWR = ~28.5x annual expenses (conservative for India). 4.0% SWR = 25x standard rule.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = HighDensityTextSecondary
                )
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextPrimary)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        val ret = returnRateText.toDoubleOrNull() ?: 12.0
                        val inf = inflationRateText.toDoubleOrNull() ?: 6.0
                        val exp = monthlyExpensesText.toDoubleOrNull() ?: 50000.0
                        val inv = monthlyInvestmentText.toDoubleOrNull() ?: 50000.0
                        val age = currentAgeText.toIntOrNull() ?: 28
                        val tAge = targetAgeText.toIntOrNull() ?: 45
                        val swr = swrRateText.toDoubleOrNull() ?: 3.5
                        val step = stepUpText.toDoubleOrNull() ?: 8.0

                        onSave(
                            FireSettings(
                                expectedReturnRate = ret,
                                inflationRate = inf,
                                monthlyExpenses = exp,
                                monthlyInvestment = inv,
                                currentAge = age,
                                targetFireAge = tAge,
                                swrRate = swr,
                                annualStepUpRate = step
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .testTag("btn_save_fire_settings")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Save Settings",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        },
        dismissButton = null
    )
}
