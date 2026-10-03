package com.example.ui.screens.calculators.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CurrencyFormatter
import com.example.ui.theme.HighDensityPrimary
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary

@Composable
fun AutoPrefillBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF0FDF4),
        border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF15803D)
            )
        }
    }
}

@Composable
fun CalculatorTextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    prefix: String? = null,
    suffix: String? = null,
    placeholder: String = "0",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' || it == '-' }
            val dotCount = filtered.count { it == '.' }
            if (dotCount <= 1) {
                onValueChange(filtered)
            }
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        },
        prefix = if (prefix != null) {
            {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            }
        } else null,
        suffix = if (suffix != null) {
            {
                Text(
                    text = suffix,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = HighDensityTextSecondary
                )
            }
        } else null,
        placeholder = {
            Text(
                text = placeholder,
                color = Color(0xFF94A3B8)
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = HighDensityTextPrimary,
            fontSize = 16.sp
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HighDensityPrimary,
            unfocusedBorderColor = Color(0xFFCBD5E1),
            focusedContainerColor = Color(0xFFF8FAFC),
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedLabelColor = HighDensityPrimary,
            unfocusedLabelColor = HighDensityTextSecondary
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun OutcomeDisplayCard(
    primaryLabel: String,
    primaryValue: Double,
    investedAmount: Double,
    returnsAmount: Double,
    investedLabel: String = "Invested Amount",
    returnsLabel: String = "Estimated Returns",
    accentColor: Color = HighDensityPrimary
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = primaryLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = accentColor
            )

            Text(
                text = CurrencyFormatter.formatInr(primaryValue),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = HighDensityTextPrimary
            )

            // Progress Split Bar
            val total = (investedAmount + returnsAmount).coerceAtLeast(1.0)
            val investedFraction = (investedAmount / total).toFloat().coerceIn(0.01f, 0.99f)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(investedFraction)
                        .background(Color(0xFF94A3B8))
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f - investedFraction)
                        .background(accentColor)
                )
            }

            HorizontalDivider(color = Color(0xFFE2E8F0))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF94A3B8)))
                        Text(investedLabel, style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                    }
                    Text(
                        text = CurrencyFormatter.formatInr(investedAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accentColor))
                        Text(returnsLabel, style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                    }
                    Text(
                        text = CurrencyFormatter.formatInr(returnsAmount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
fun MetricDetailRow(
    label: String,
    value: String,
    valueColor: Color = HighDensityTextPrimary,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = HighDensityTextSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
fun InfoNoteCard(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF1F5F9),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(16.dp).padding(top = 2.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 16.sp),
                color = Color(0xFF475569)
            )
        }
    }
}
