package com.example.domain.model

import com.example.data.local.entity.*

enum class TimeRangeOption(val label: String, val monthsBack: Int) {
    MOM("1M", 1),
    THREE_MONTHS("3M", 3),
    SIX_MONTHS("6M", 6),
    TWELVE_MONTHS("12M", 12),
    ALL_TIME("All", -1)
}

data class GrowthMetric(
    val currentValue: Double,
    val previousValue: Double,
    val diffAmount: Double = currentValue - previousValue,
    val growthPercentage: Double? = if (previousValue > 0) ((currentValue - previousValue) / previousValue) * 100.0 else null,
    val hasPreviousData: Boolean = previousValue > 0,
    val sipInflows: Double = 0.0, // Recurring SIP and scheduled deposits added during this comparison period
    val contributions: Double = 0.0, // Manual/Transfer lump-sum contributions
    val withdrawals: Double = 0.0, // Manual/Transfer redemptions or withdrawals
    val netCapitalInflows: Double = sipInflows + contributions - withdrawals, // Total external capital introduced
    val organicDiff: Double = (currentValue - previousValue) - (sipInflows + contributions - withdrawals), // True market gain/loss excluding all capital transfers
    val organicGrowthPercentage: Double? = if (previousValue > 0) (((currentValue - previousValue) - (sipInflows + contributions - withdrawals)) / previousValue) * 100.0 else null
) {
    val isPositive: Boolean get() = diffAmount > 0.001
    val isNegative: Boolean get() = diffAmount < -0.001
    val isZero: Boolean get() = !isPositive && !isNegative

    val isOrganicPositive: Boolean get() = organicDiff > 0.001
    val isOrganicNegative: Boolean get() = organicDiff < -0.001
}

data class InvestmentReturnMetric(
    val totalInvested: Double,
    val currentValue: Double,
    val totalGainLoss: Double = currentValue - totalInvested,
    val returnPercentage: Double? = if (totalInvested > 0) ((currentValue - totalInvested) / totalInvested) * 100.0 else null
) {
    val isPositive: Boolean get() = totalGainLoss > 0.001
    val isNegative: Boolean get() = totalGainLoss < -0.001
    val isZero: Boolean get() = !isPositive && !isNegative
}

data class CategoryGrowthItem(
    val category: CategoryEntity,
    val currentValue: Double,
    val previousValue: Double,
    val diffAmount: Double = currentValue - previousValue,
    val growthPercentage: Double? = if (previousValue > 0) ((currentValue - previousValue) / previousValue) * 100.0 else null,
    val assetCount: Int = 0,
    val investedAmount: Double = 0.0,
    val gainLossAmount: Double = currentValue - investedAmount,
    val returnPercentage: Double? = if (investedAmount > 0) ((currentValue - investedAmount) / investedAmount) * 100.0 else null,
    val sipInflows: Double = 0.0,
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val netCapitalInflows: Double = sipInflows + contributions - withdrawals,
    val organicDiff: Double = (currentValue - previousValue) - (sipInflows + contributions - withdrawals),
    val organicGrowthPercentage: Double? = if (previousValue > 0) (((currentValue - previousValue) - (sipInflows + contributions - withdrawals)) / previousValue) * 100.0 else null,
    val comparisonLabel: String = ""
)

data class AssetGrowthItem(
    val asset: AssetEntity,
    val category: CategoryEntity,
    val currentValue: Double,
    val previousValue: Double,
    val diffAmount: Double = currentValue - previousValue,
    val growthPercentage: Double? = if (previousValue > 0) ((currentValue - previousValue) / previousValue) * 100.0 else null,
    val investedAmount: Double = 0.0,
    val gainLossAmount: Double = currentValue - investedAmount,
    val returnPercentage: Double? = if (investedAmount > 0) ((currentValue - investedAmount) / investedAmount) * 100.0 else null,
    val sipInflows: Double = 0.0,
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val netCapitalInflows: Double = sipInflows + contributions - withdrawals,
    val organicDiff: Double = (currentValue - previousValue) - (sipInflows + contributions - withdrawals),
    val organicGrowthPercentage: Double? = if (previousValue > 0) (((currentValue - previousValue) - (sipInflows + contributions - withdrawals)) / previousValue) * 100.0 else null
)

data class AllocationSlice(
    val id: String,
    val name: String,
    val amount: Double,
    val percentage: Double, // 0 to 100
    val colorHex: String,
    val groupType: String
)

data class HistoricalDataPoint(
    val snapshotId: String,
    val year: Int,
    val month: Int,
    val label: String, // e.g. "Jan '26"
    val portfolioValue: Double,
    val investedValue: Double = 0.0,
    val expenseValue: Double = 0.0,
    val sipInflows: Double = 0.0
)

data class PortfolioSummary(
    val selectedSnapshotId: String,
    val view: PortfolioViewEntity,
    val totalValue: Double,
    val previousValue: Double,
    val growth: GrowthMetric,
    val timeRange: TimeRangeOption,
    val totalInvested: Double = 0.0,
    val investmentReturn: InvestmentReturnMetric = InvestmentReturnMetric(totalInvested, totalValue),
    val activeMonthlySips: Double = 0.0,
    val comparisonSnapshotId: String? = null,
    val comparisonSnapshotLabel: String? = null,
    val marketOrganicGain: Double = 0.0,
    val marketOrganicPercentage: Double? = null,
    val netMonthlySavings: Double = 0.0,
    val newCapitalInvested: Double = 0.0
)

data class RecurringCashFlowSummary(
    val totalActiveSipAmount: Double = 0.0,
    val totalActiveRdAmount: Double = 0.0, // Monthly normalized RD installments
    val totalActiveInterestPayout: Double = 0.0, // Monthly normalized interest from Bonds & FDs
    val totalActivePrincipalPayout: Double = 0.0, // Monthly normalized principal repayment
    val totalActiveSalary: Double = 0.0,
    val totalActiveMealCoupon: Double = 0.0,
    val totalActiveOtherInflows: Double = 0.0,
    val items: List<RecurringCashFlowEntity> = emptyList()
) {
    val totalActiveInflows: Double get() = totalActiveSalary + totalActiveMealCoupon + totalActiveInterestPayout + totalActivePrincipalPayout + totalActiveOtherInflows
    val totalActiveOutflows: Double get() = totalActiveSipAmount + totalActiveRdAmount
    val netRecurringSurplus: Double get() = totalActiveInflows - totalActiveOutflows
}

enum class ExpenseComparisonRange(val label: String, val shortLabel: String, val monthsCount: Int) {
    ONE_MONTH("1 Month", "1M", 1),
    THREE_MONTHS("3 Months", "3M", 3),
    SIX_MONTHS("6 Months", "6M", 6),
    TWELVE_MONTHS("12 Months", "12M", 12)
}

data class ExpenseSummary(
    val selectedSnapshotId: String,
    val totalExpense: Double,
    val previousTotalExpense: Double,
    val growth: GrowthMetric,
    val threeMonthAvg: Double,
    val sixMonthAvg: Double,
    val twelveMonthAvg: Double,
    val highestCategoryName: String?,
    val highestCategoryAmount: Double,
    val lowestCategoryName: String?,
    val lowestCategoryAmount: Double,
    val monthlyInflows: Double = 0.0,
    val savingsAmount: Double = 0.0,
    val savingsRatePercentage: Double? = null,
    val runwayMonths: Double? = null,
    val comparisonRange: ExpenseComparisonRange = ExpenseComparisonRange.ONE_MONTH,
    val benchmarkExpense: Double = previousTotalExpense,
    val benchmarkLabel: String = "Prev Month",
    val benchmarkGrowth: GrowthMetric = GrowthMetric(totalExpense, previousTotalExpense)
)

data class ExpenseCategoryBreakdown(
    val category: ExpenseCategoryEntity,
    val totalAmount: Double,
    val comparisonAmount: Double = 0.0,
    val comparisonLabel: String = "Prev Month",
    val percentage: Double, // 0 to 100
    val itemCount: Int,
    val diffAmount: Double = totalAmount - comparisonAmount,
    val growthPercentage: Double? = if (comparisonAmount > 0) ((totalAmount - comparisonAmount) / comparisonAmount) * 100.0 else null,
    val previousAmount: Double = comparisonAmount,
    val momDiffAmount: Double = diffAmount,
    val momGrowthPercentage: Double? = growthPercentage
)

data class CategoryComparisonItem(
    val category: CategoryEntity,
    val valueA: Double,
    val valueB: Double,
    val diffAmount: Double = valueB - valueA,
    val growthPercentage: Double? = if (valueA > 0) ((valueB - valueA) / valueA) * 100.0 else null,
    val sipInflows: Double = 0.0,
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val netCapitalInflows: Double = sipInflows + contributions - withdrawals,
    val organicDiff: Double = (valueB - valueA) - (sipInflows + contributions - withdrawals),
    val organicGrowthPercentage: Double? = if (valueA > 0) (((valueB - valueA) - (sipInflows + contributions - withdrawals)) / valueA) * 100.0 else null
)

data class AssetComparisonItem(
    val asset: AssetEntity,
    val categoryName: String,
    val valueA: Double,
    val valueB: Double,
    val diffAmount: Double = valueB - valueA,
    val growthPercentage: Double? = if (valueA > 0) ((valueB - valueA) / valueA) * 100.0 else null,
    val sipInflows: Double = 0.0,
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val netCapitalInflows: Double = sipInflows + contributions - withdrawals,
    val organicDiff: Double = (valueB - valueA) - (sipInflows + contributions - withdrawals),
    val organicGrowthPercentage: Double? = if (valueA > 0) (((valueB - valueA) - (sipInflows + contributions - withdrawals)) / valueA) * 100.0 else null
)

data class TwoPeriodComparison(
    val periodAId: String,
    val periodBId: String,
    val periodALabel: String,
    val periodBLabel: String,
    val totalA: Double,
    val totalB: Double,
    val totalDiff: Double = totalB - totalA,
    val totalGrowthPercentage: Double? = if (totalA > 0) ((totalB - totalA) / totalA) * 100.0 else null,
    val totalSipInflows: Double = 0.0,
    val totalContributions: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val totalNetCapitalInflows: Double = totalSipInflows + totalContributions - totalWithdrawals,
    val organicDiff: Double = (totalB - totalA) - (totalSipInflows + totalContributions - totalWithdrawals),
    val organicGrowthPercentage: Double? = if (totalA > 0) (((totalB - totalA) - (totalSipInflows + totalContributions - totalWithdrawals)) / totalA) * 100.0 else null,
    val categoryComparisons: List<CategoryComparisonItem>,
    val assetComparisons: List<AssetComparisonItem>,
    val expenseA: Double,
    val expenseB: Double,
    val expenseDiff: Double = expenseB - expenseA
)
