package com.example.util

import java.util.Locale

/**
 * Evaluates basic arithmetic expressions containing numbers, decimals, and '+' or '-' operators.
 * Enables users to enter multiple amounts (e.g. "5000 + 2500 - 300") in financial input fields.
 */
object MathExpressionEvaluator {

    /**
     * Evaluates an arithmetic expression containing '+' and '-'.
     * Returns the evaluated Double, or null if the expression cannot be parsed or is blank.
     */
    fun evaluate(expression: String?): Double? {
        if (expression.isNullOrBlank()) return null

        // Remove currency symbols, commas, and trim whitespace
        val clean = expression
            .replace("₹", "")
            .replace(",", "")
            .trim()

        if (clean.isEmpty()) return null

        return try {
            var total = 0.0
            var currentOp = '+'
            val currentNumber = StringBuilder()

            for (i in clean.indices) {
                val ch = clean[i]
                when {
                    ch.isDigit() || ch == '.' -> {
                        currentNumber.append(ch)
                    }
                    ch == '+' || ch == '-' -> {
                        if (currentNumber.isNotEmpty()) {
                            val num = currentNumber.toString().toDoubleOrNull() ?: 0.0
                            total = if (currentOp == '+') total + num else total - num
                            currentNumber.clear()
                        } else if (i == 0 && ch == '-') {
                            // Negative sign at start of expression
                            currentOp = '-'
                            continue
                        }
                        currentOp = ch
                    }
                    ch.isWhitespace() -> {
                        // ignore whitespace
                    }
                    else -> {
                        // ignore unexpected characters
                    }
                }
            }

            if (currentNumber.isNotEmpty()) {
                val num = currentNumber.toString().toDoubleOrNull() ?: 0.0
                total = if (currentOp == '+') total + num else total - num
            }

            total
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats evaluated amount cleanly without unnecessary decimal places if it's an integer.
     */
    fun formatEvaluated(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", amount)
        }
    }

    /**
     * Checks if the text contains math operators (+ or -)
     */
    fun hasExpression(text: String): Boolean {
        return text.contains('+') || (text.contains('-') && text.indexOf('-') > 0)
    }

    /**
     * Sanitizes user input to allow digits, decimals, +, -, and spaces.
     */
    fun sanitizeMathInput(input: String): String {
        return input.filter { it.isDigit() || it == '.' || it == '+' || it == '-' || it == ' ' }
    }
}
