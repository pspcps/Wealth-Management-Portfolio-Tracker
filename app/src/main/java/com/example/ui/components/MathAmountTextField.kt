package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HighDensityPrimary
import com.example.ui.theme.HighDensityTextPrimary
import com.example.ui.theme.HighDensityTextSecondary
import com.example.util.MathExpressionEvaluator

/**
 * An intelligent amount text field that supports inline arithmetic (+ and -).
 * Allows users to aggregate multiple figures directly (e.g. "5000 + 2000 - 350"),
 * with quick inline + / - tap targets, live preview calculation, and auto-evaluation on submit.
 */
@Composable
fun MathAmountTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    prefixText: String = "₹ ",
    enabled: Boolean = true,
    testTag: String? = null,
    keyboardType: KeyboardType = KeyboardType.Number,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp)
) {
    val hasExpression = remember(value) { MathExpressionEvaluator.hasExpression(value) }
    val evaluatedAmount = remember(value) {
        if (hasExpression) MathExpressionEvaluator.evaluate(value) else null
    }

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = { tfv ->
                val sanitized = MathExpressionEvaluator.sanitizeMathInput(tfv.text)
                val diff = tfv.text.length - sanitized.length
                val newSelection = if (diff != 0) {
                    val newCursor = (tfv.selection.end - diff).coerceIn(0, sanitized.length)
                    TextRange(newCursor)
                } else {
                    tfv.selection
                }
                textFieldValue = tfv.copy(text = sanitized, selection = newSelection)
                onValueChange(sanitized)
            },
            label = {
                Text(
                    text = label,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            placeholder = {
                Text(
                    text = placeholder.ifEmpty { "e.g. 5000 + 2500" },
                    color = HighDensityTextSecondary.copy(alpha = 0.5f),
                    fontSize = 13.sp
                )
            },
            prefix = {
                if (prefixText.isNotEmpty()) {
                    Text(
                        text = prefixText,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }
            },
            trailingIcon = {
                // Quick + operator button for easily adding multiple amounts even on numeric keypad
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val currentText = textFieldValue.text
                            val trimmed = currentText.trimEnd()
                            if (trimmed.isNotEmpty() && !trimmed.endsWith("+") && !trimmed.endsWith("-")) {
                                val newText = "$trimmed + "
                                textFieldValue = TextFieldValue(
                                    text = newText,
                                    selection = TextRange(newText.length)
                                )
                                onValueChange(newText)
                                focusRequester.requestFocus()
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = HighDensityPrimary
                        )
                    }
                }
            },
            singleLine = true,
            enabled = enabled,
            shape = shape,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (evaluatedAmount != null) {
                        val formatted = MathExpressionEvaluator.formatEvaluated(evaluatedAmount)
                        textFieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                        onValueChange(formatted)
                    }
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFF8FAFC),
                focusedContainerColor = Color.White,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedBorderColor = HighDensityPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        )

        // Live calculation preview badge if an expression is being evaluated
        AnimatedVisibility(
            visible = evaluatedAmount != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (evaluatedAmount != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFECFDF5),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            val formatted = MathExpressionEvaluator.formatEvaluated(evaluatedAmount)
                            textFieldValue = TextFieldValue(text = formatted, selection = TextRange(formatted.length))
                            onValueChange(formatted)
                            focusRequester.requestFocus()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Total: ₹${CurrencyFormatter.formatInr(evaluatedAmount, ignorePrivacy = true).removePrefix("₹")}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF10B981)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "Tap to apply",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
