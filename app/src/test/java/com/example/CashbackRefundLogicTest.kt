package com.example

import com.example.data.local.entity.CashbackRefundEntity
import com.example.ui.screens.cashback.CashbackTimeFilter
import com.example.util.MathExpressionEvaluator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class CashbackRefundLogicTest {

    @Test
    fun testMathExpressionEvaluatorForAmount() {
        val expr1 = "500 + 250"
        assertTrue(MathExpressionEvaluator.hasExpression(expr1))
        assertEquals(750.0, MathExpressionEvaluator.evaluate(expr1)!!, 0.001)

        val expr2 = "1200 - 350 + 50"
        assertTrue(MathExpressionEvaluator.hasExpression(expr2))
        assertEquals(900.0, MathExpressionEvaluator.evaluate(expr2)!!, 0.001)

        val plainNumber = "450"
        assertEquals(false, MathExpressionEvaluator.hasExpression(plainNumber))
        assertEquals(450.0, plainNumber.toDoubleOrNull()!!, 0.001)
    }

    @Test
    fun testTimeFilterLogic() {
        val items = listOf(
            CashbackRefundEntity(
                id = 1,
                type = "CASHBACK",
                title = "Amazon",
                source = "Amazon",
                amount = 1450.0,
                status = "RECEIVED",
                expectedDate = "2026-09-10",
                receivedDate = "2026-09-10"
            ),
            CashbackRefundEntity(
                id = 2,
                type = "REFUND",
                title = "Swiggy",
                source = "Swiggy",
                amount = 340.0,
                status = "RECEIVED",
                expectedDate = "2026-09-15",
                receivedDate = "2026-09-15"
            ),
            CashbackRefundEntity(
                id = 3,
                type = "CASHBACK",
                title = "Flipkart",
                source = "Flipkart",
                amount = 680.0,
                status = "RECEIVED",
                expectedDate = "2026-08-20",
                receivedDate = "2026-08-20"
            ),
            CashbackRefundEntity(
                id = 4,
                type = "REFUND",
                title = "MakeMyTrip",
                source = "MakeMyTrip",
                amount = 2500.0,
                status = "RECEIVED",
                expectedDate = "2025-12-05",
                receivedDate = "2025-12-05"
            )
        )

        // 1. Overall Filter
        val overallItems = items
        val overallCashback = overallItems.filter { it.type == "CASHBACK" }.sumOf { it.amount }
        val overallRefunds = overallItems.filter { it.type == "REFUND" }.sumOf { it.amount }
        assertEquals(4, overallItems.size)
        assertEquals(2130.0, overallCashback, 0.001)
        assertEquals(2840.0, overallRefunds, 0.001)
        assertEquals(4970.0, overallCashback + overallRefunds, 0.001)

        // 2. Monthly Filter for Sep 2026
        val sep2026Prefix = String.format(Locale.US, "%04d-%02d", 2026, 9)
        val sepItems = items.filter { item ->
            val dateStr = item.receivedDate ?: item.expectedDate
            dateStr.startsWith(sep2026Prefix)
        }
        val sepCashback = sepItems.filter { it.type == "CASHBACK" }.sumOf { it.amount }
        val sepRefunds = sepItems.filter { it.type == "REFUND" }.sumOf { it.amount }
        assertEquals(2, sepItems.size)
        assertEquals(1450.0, sepCashback, 0.001)
        assertEquals(340.0, sepRefunds, 0.001)
        assertEquals(1790.0, sepCashback + sepRefunds, 0.001)

        // 3. Yearly Filter for 2026
        val year2026Prefix = "2026"
        val y2026Items = items.filter { item ->
            val dateStr = item.receivedDate ?: item.expectedDate
            dateStr.startsWith(year2026Prefix)
        }
        assertEquals(3, y2026Items.size)
        val y2026Total = y2026Items.sumOf { it.amount }
        assertEquals(2470.0, y2026Total, 0.001)
    }

    @Test
    fun testSourceAsTitleReplacement() {
        val source = "Zomato"
        val type = "REFUND"
        val resolvedTitle = source.ifBlank { if (type == "CASHBACK") "Cashback" else "Refund" }
        assertEquals("Zomato", resolvedTitle)

        val emptySource = ""
        val fallbackTitle = emptySource.ifBlank { if (type == "CASHBACK") "Cashback" else "Refund" }
        assertEquals("Refund", fallbackTitle)
    }
}
