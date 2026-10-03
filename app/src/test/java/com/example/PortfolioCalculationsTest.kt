package com.example

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.PortfolioViewEntity
import com.example.data.repository.PortfolioRepository
import com.example.domain.model.GrowthMetric
import com.example.ui.components.CurrencyFormatter
import com.example.util.FinancialPrivacyManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PortfolioCalculationsTest {

    @Before
    fun setUp() {
        // By default for standard calculations tests, reveal numbers
        FinancialPrivacyManager.setAmountsVisible(true)
    }

    @Test
    fun testCurrencyFormattingInr() {
        assertEquals("₹0", CurrencyFormatter.formatInr(0.0))
        assertEquals("₹5,00,000", CurrencyFormatter.formatInr(500000.0))
        assertEquals("₹1,25,50,000", CurrencyFormatter.formatInr(12550000.0))
    }

    @Test
    fun testCurrencyFormattingCompact() {
        assertEquals("₹50 K", CurrencyFormatter.formatInrCompact(50000.0))
        assertEquals("₹5 L", CurrencyFormatter.formatInrCompact(500000.0))
        assertEquals("₹1.25 Cr", CurrencyFormatter.formatInrCompact(12500000.0))
    }

    @Test
    fun testPrivacyMaskingAndToggling() {
        // When privacy mode is active (amounts hidden by default)
        FinancialPrivacyManager.setAmountsVisible(false)
        assertEquals(CurrencyFormatter.MASKED_FULL, CurrencyFormatter.formatInr(500000.0))
        assertEquals(CurrencyFormatter.MASKED_COMPACT, CurrencyFormatter.formatInrCompact(500000.0))
        // Ignore privacy flag (e.g. for exports or local overrides)
        assertEquals("₹5,00,000", CurrencyFormatter.formatInr(500000.0, ignorePrivacy = true))
        assertEquals("₹5 L", CurrencyFormatter.formatInrCompact(500000.0, ignorePrivacy = true))

        // When toggled to revealed
        FinancialPrivacyManager.setAmountsVisible(true)
        assertEquals("₹5,00,000", CurrencyFormatter.formatInr(500000.0))
        assertEquals("₹5 L", CurrencyFormatter.formatInrCompact(500000.0))
    }

    @Test
    fun testGrowthCalculations() {
        val metric = GrowthMetric(
            currentValue = 550000.0,
            previousValue = 500000.0
        )
        assertTrue(metric.isPositive)
        assertEquals(50000.0, metric.diffAmount, 0.01)
        assertEquals(10.0, metric.growthPercentage ?: 0.0, 0.01)
        assertEquals("+₹50,000", CurrencyFormatter.formatInr(metric.diffAmount, showSign = true))
        assertEquals("+10.00%", CurrencyFormatter.formatGrowthPercentage(metric.growthPercentage))
    }

    @Test
    fun testCustomCategoryFilterInclusion() {
        val customView = PortfolioViewEntity(
            id = "custom_bank_bonds_fd",
            name = "Bank + Bonds + FD",
            isDefault = false,
            includedCategoryIds = "bank_balance,bonds,fixed_deposits"
        )

        val bankCat = CategoryEntity(id = "bank_balance", name = "Bank Balance", colorHex = "#10B981", iconName = "account_balance", groupType = "CASH", orderIndex = 1)
        val bondsCat = CategoryEntity(id = "bonds", name = "Bonds & SGB", colorHex = "#6366F1", iconName = "receipt_long", groupType = "INVESTMENT", orderIndex = 2)
        val fdCat = CategoryEntity(id = "fixed_deposits", name = "Fixed Deposits", colorHex = "#0EA5E9", iconName = "lock", groupType = "CASH", orderIndex = 3)
        val equityCat = CategoryEntity(id = "indian_stocks", name = "Indian Stocks", colorHex = "#3B82F6", iconName = "trending_up", groupType = "INVESTMENT", orderIndex = 4)
        val epfCat = CategoryEntity(id = "epf", name = "EPF", colorHex = "#F59E0B", iconName = "assured_workload", groupType = "RETIREMENT", orderIndex = 5)

        val includedIds = customView.includedCategoryIds.split(",").map { it.trim() }.toSet()

        assertTrue(includedIds.contains(bankCat.id))
        assertTrue(includedIds.contains(bondsCat.id))
        assertTrue(includedIds.contains(fdCat.id))
        assertFalse(includedIds.contains(equityCat.id))
        assertFalse(includedIds.contains(epfCat.id))
    }
}

