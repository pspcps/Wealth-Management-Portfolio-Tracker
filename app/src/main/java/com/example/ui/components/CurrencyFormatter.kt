package com.example.ui.components

import com.example.util.FinancialPrivacyManager
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    const val MASKED_FULL = "₹ ••••••"
    const val MASKED_COMPACT = "₹ ••••"

    val isAmountsVisible: Boolean
        get() = FinancialPrivacyManager.isAmountsVisible.value

    val isMasked: Boolean
        get() = !FinancialPrivacyManager.isAmountsVisible.value

    fun maskCurrency(): String = MASKED_FULL
    fun maskCompact(): String = MASKED_COMPACT

    // Indian Lakhs/Crores formatting for full number: e.g. ₹37,20,000
    fun formatInr(amount: Double, showSign: Boolean = false, ignorePrivacy: Boolean = false): String {
        if (!ignorePrivacy && !FinancialPrivacyManager.isAmountsVisible.value) {
            return MASKED_FULL
        }

        val sign = if (showSign && amount > 0.001) "+" else if (amount < -0.001) "-" else ""
        val absVal = Math.abs(amount)
        
        val longVal = Math.round(absVal)
        val formattedNumber = formatIndianNumber(longVal)
        return "$sign₹$formattedNumber"
    }

    // Short format for badges / compact cards: e.g. ₹37.20 L, ₹1.50 Cr, ₹82.5 K
    fun formatInrCompact(amount: Double, showSign: Boolean = false, ignorePrivacy: Boolean = false): String {
        if (!ignorePrivacy && !FinancialPrivacyManager.isAmountsVisible.value) {
            return MASKED_COMPACT
        }

        val sign = if (showSign && amount > 0.001) "+" else if (amount < -0.001) "-" else ""
        val absVal = Math.abs(amount)

        return when {
            absVal >= 10000000 -> { // 1 Crore
                val cr = absVal / 10000000.0
                val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
                "$sign₹${df.format(cr)} Cr"
            }
            absVal >= 100000 -> { // 1 Lakh
                val l = absVal / 100000.0
                val df = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
                "$sign₹${df.format(l)} L"
            }
            absVal >= 1000 -> { // 1 Thousand
                val k = absVal / 1000.0
                val df = DecimalFormat("#.#", DecimalFormatSymbols(Locale.US))
                "$sign₹${df.format(k)} K"
            }
            else -> {
                "$sign₹${Math.round(absVal)}"
            }
        }
    }

    fun formatGrowthPercentage(percentage: Double?): String {
        if (percentage == null) return "N/A"
        val sign = if (percentage > 0.001) "+" else if (percentage < -0.001) "" else ""
        val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale.US))
        return "$sign${df.format(percentage)}%"
    }

    private fun formatIndianNumber(num: Long): String {
        if (num == 0L) return "0"
        val s = num.toString()
        if (s.length <= 3) return s
        
        val lastThree = s.substring(s.length - 3)
        val rest = s.substring(0, s.length - 3)
        
        val sb = StringBuilder()
        var count = 0
        for (i in rest.length - 1 downTo 0) {
            sb.append(rest[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(",")
            }
        }
        val firstPart = sb.reverse().toString()
        return "$firstPart,$lastThree"
    }

    fun parseAmount(input: String): Double {
        val cleaned = input.replace("₹", "").replace(",", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
