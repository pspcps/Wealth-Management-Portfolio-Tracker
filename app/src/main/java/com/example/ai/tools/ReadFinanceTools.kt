package com.example.ai.tools

import com.example.ai.Source
import com.example.ai.SourceCategory
import com.example.data.local.entity.PortfolioViewEntity
import com.example.data.repository.FireSettingsRepository
import com.example.data.repository.InsuranceRepository
import com.example.data.repository.LoanRepository
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfileRepository
import com.example.domain.model.ExpenseComparisonRange
import com.example.domain.model.FireCalculator
import com.example.domain.model.TimeRangeOption
import com.example.domain.model.*
import kotlinx.coroutines.flow.first
import java.text.DecimalFormat

private val inrFormatter = DecimalFormat("#,##,###.##")

private fun formatInr(amount: Double): String {
    return "₹" + inrFormatter.format(amount)
}

/**
 * 1. get_net_worth
 */
class GetNetWorthTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_net_worth"
    override val description: String = "Retrieves the current or date-specific user net worth, total invested capital, and category breakdown from the local database."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = mapOf(
        "date" to "Optional snapshot ID in YYYY-MM format (e.g. '2026-08'). Defaults to latest available month."
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        if (snapshots.isEmpty()) {
            return ToolResult.unavailable("I don't have any net worth snapshots recorded yet in your local database.")
        }

        val requestedDate = arguments["date"]?.toString()?.trim()
        val targetSnapshot = if (!requestedDate.isNullOrBlank()) {
            snapshots.find { it.id == requestedDate } ?: snapshots.last()
        } else {
            snapshots.last()
        }

        val defaultView = PortfolioViewEntity("all", "All Assets", true, "*")
        val summary = portfolioRepository.calculatePortfolioSummary(
            snapshotId = targetSnapshot.id,
            view = defaultView,
            timeRange = TimeRangeOption.MOM
        )

        val categories = portfolioRepository.calculateCategoryGrowth(
            snapshotId = targetSnapshot.id,
            view = defaultView,
            timeRange = TimeRangeOption.MOM
        )

        val breakdown = categories.filter { it.currentValue > 0 }.map {
            mapOf(
                "category" to it.category.name,
                "value" to it.currentValue,
                "formattedValue" to formatInr(it.currentValue),
                "invested" to it.investedAmount,
                "gains" to it.gainLossAmount,
                "percentage" to if (summary.totalValue > 0) ((it.currentValue / summary.totalValue) * 100.0) else 0.0
            )
        }

        val data = mapOf(
            "netWorth" to summary.totalValue,
            "formattedNetWorth" to formatInr(summary.totalValue),
            "currency" to "INR",
            "date" to targetSnapshot.id,
            "dateLabel" to portfolioRepository.formatSnapshotLabel(targetSnapshot.id),
            "totalInvested" to summary.totalInvested,
            "formattedTotalInvested" to formatInr(summary.totalInvested),
            "unrealizedGain" to summary.investmentReturn.totalGainLoss,
            "returnPercentage" to summary.investmentReturn.returnPercentage,
            "momDiff" to summary.growth.diffAmount,
            "organicDiff" to summary.growth.organicDiff,
            "categoryBreakdown" to breakdown
        )

        val summaryText = "Your current Net Worth for ${portfolioRepository.formatSnapshotLabel(targetSnapshot.id)} is ${formatInr(summary.totalValue)} (Invested: ${formatInr(summary.totalInvested)}, Overall Gain: ${formatInr(summary.investmentReturn.totalGainLoss)})."

        return ToolResult.success(
            data = data,
            summaryText = summaryText,
            source = Source(
                title = "Local Portfolio Snapshot (${targetSnapshot.id})",
                snippet = "Total Net Worth: ${formatInr(summary.totalValue)}, Invested: ${formatInr(summary.totalInvested)}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 2. get_net_worth_history
 */
class GetNetWorthHistoryTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_net_worth_history"
    override val description: String = "Retrieves time-series net worth history across multiple months."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = mapOf(
        "from" to "Optional start month in YYYY-MM format",
        "to" to "Optional end month in YYYY-MM format"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val defaultView = PortfolioViewEntity("all", "All Assets", true, "*")
        val history: List<HistoricalDataPoint> = portfolioRepository.getHistoricalPoints(defaultView)
        if (history.isEmpty()) {
            return ToolResult.unavailable("I don't have historical net worth data recorded in your database.")
        }

        val from = arguments["from"]?.toString()?.trim()
        val to = arguments["to"]?.toString()?.trim()

        val filtered: List<HistoricalDataPoint> = history.filter { pt ->
            val matchesFrom = from.isNullOrBlank() || pt.snapshotId >= from
            val matchesTo = to.isNullOrBlank() || pt.snapshotId <= to
            matchesFrom && matchesTo
        }

        val timeSeries = filtered.map {
            mapOf(
                "snapshotId" to it.snapshotId,
                "label" to it.label,
                "netWorth" to it.portfolioValue,
                "formattedNetWorth" to formatInr(it.portfolioValue),
                "invested" to it.investedValue,
                "expenses" to it.expenseValue
            )
        }

        val firstVal = filtered.firstOrNull()?.portfolioValue ?: 0.0
        val lastVal = filtered.lastOrNull()?.portfolioValue ?: 0.0
        val overallGrowth = lastVal - firstVal

        val data = mapOf(
            "timeSeries" to timeSeries,
            "totalPoints" to filtered.size,
            "startMonth" to (filtered.firstOrNull()?.label ?: ""),
            "endMonth" to (filtered.lastOrNull()?.label ?: ""),
            "startValue" to firstVal,
            "endValue" to lastVal,
            "overallGrowth" to overallGrowth
        )

        return ToolResult.success(
            data = data,
            summaryText = "Retrieved ${filtered.size} months of net worth history. Started at ${formatInr(firstVal)} and currently at ${formatInr(lastVal)} (Growth: ${formatInr(overallGrowth)}).",
            source = Source(
                title = "Historical Snapshots (${filtered.size} entries)",
                snippet = "Period: ${filtered.firstOrNull()?.label ?: ""} to ${filtered.lastOrNull()?.label ?: ""}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 3. get_portfolio
 */
class GetPortfolioTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_portfolio"
    override val description: String = "Retrieves active portfolio assets, holdings, current valuations, and invested amounts grouped by category."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = mapOf(
        "category" to "Optional category name or ID to filter by (e.g. 'Equity', 'Mutual Funds', 'Gold')",
        "status" to "Asset status, defaults to 'ACTIVE'"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val latestSnapshot = snapshots.lastOrNull()
            ?: return ToolResult.unavailable("I don't have any portfolio snapshot in your database.")

        val allAssets = portfolioRepository.getAllAssetsList()
        val allCategories = portfolioRepository.getAllCategoriesList().associateBy { it.id }
        val currentValues = portfolioRepository.getValuesBySnapshotList(latestSnapshot.id).associateBy { it.assetId }

        val filterCategory = arguments["category"]?.toString()?.trim()?.lowercase()
        val statusFilter = arguments["status"]?.toString()?.trim() ?: "ACTIVE"

        val activeAssets = allAssets.filter { asset ->
            val matchesStatus = statusFilter.equals("ALL", ignoreCase = true) || asset.status.equals(statusFilter, ignoreCase = true)
            val catName = allCategories[asset.categoryId]?.name?.lowercase() ?: ""
            val matchesCategory = filterCategory.isNullOrBlank() ||
                    catName.contains(filterCategory) ||
                    asset.categoryId.lowercase().contains(filterCategory)
            matchesStatus && matchesCategory
        }

        if (activeAssets.isEmpty()) {
            return ToolResult.unavailable("No assets found matching the criteria ${filterCategory?.let { "for category '$it'" } ?: ""}.")
        }

        val holdings = activeAssets.map { asset ->
            val valueObj = currentValues[asset.id]
            val value = valueObj?.value ?: 0.0
            val invested = if ((valueObj?.investedAmount ?: 0.0) > 0) valueObj!!.investedAmount else asset.initialInvestedAmount
            val gainLoss = value - invested
            val catName = allCategories[asset.categoryId]?.name ?: asset.categoryId

            mapOf(
                "id" to asset.id,
                "name" to asset.name,
                "category" to catName,
                "currentValue" to value,
                "formattedValue" to formatInr(value),
                "investedAmount" to invested,
                "formattedInvested" to formatInr(invested),
                "gainLoss" to gainLoss,
                "notes" to asset.notes
            )
        }

        val totalVal = holdings.sumOf { (it["currentValue"] as Double) }
        val totalInv = holdings.sumOf { (it["investedAmount"] as Double) }

        val data = mapOf(
            "holdingsCount" to holdings.size,
            "totalValuation" to totalVal,
            "formattedTotalValuation" to formatInr(totalVal),
            "totalInvested" to totalInv,
            "formattedTotalInvested" to formatInr(totalInv),
            "holdings" to holdings
        )

        return ToolResult.success(
            data = data,
            summaryText = "Found ${holdings.size} active assets with total valuation of ${formatInr(totalVal)} (Invested: ${formatInr(totalInv)}).",
            source = Source(
                title = "Local Asset Holdings",
                snippet = "${holdings.size} assets tracked in snapshot ${latestSnapshot.id}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 4. get_asset_allocation
 */
class GetAssetAllocationTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_asset_allocation"
    override val description: String = "Returns the asset allocation breakdown (Equity, Debt, Gold, Cash, Real Estate, etc.) with values and percentages."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val latestSnapshot = snapshots.lastOrNull()
            ?: return ToolResult.unavailable("I don't have any portfolio snapshot in your database.")

        val defaultView = PortfolioViewEntity("all", "All Assets", true, "*")
        val slices = portfolioRepository.calculateAssetAllocation(latestSnapshot.id, defaultView)

        if (slices.isEmpty()) {
            return ToolResult.unavailable("No asset allocation data is currently available in your database.")
        }

        val allocationMap = mutableMapOf<String, Double>()
        val details = slices.map { slice ->
            allocationMap[slice.name] = slice.percentage
            mapOf(
                "assetClass" to slice.name,
                "amount" to slice.amount,
                "formattedAmount" to formatInr(slice.amount),
                "percentage" to slice.percentage,
                "formattedPercentage" to "${String.format("%.1f", slice.percentage)}%"
            )
        }

        val totalAmount = slices.sumOf { it.amount }

        val data = mapOf(
            "totalPortfolioValue" to totalAmount,
            "formattedTotalValue" to formatInr(totalAmount),
            "allocationSummary" to allocationMap,
            "breakdown" to details
        )

        val summaryText = details.joinToString(", ") { "${it["assetClass"]}: ${it["formattedPercentage"]} (${it["formattedAmount"]})" }

        return ToolResult.success(
            data = data,
            summaryText = "Asset Allocation: $summaryText",
            source = Source(
                title = "Local Asset Allocation",
                snippet = "Total Portfolio: ${formatInr(totalAmount)} across ${slices.size} asset classes",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 5. get_expenses
 */
class GetExpensesTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_expenses"
    override val description: String = "Retrieves expenses for a given month or range, categorized with totals, category breakdown, and Needs vs Wants classification."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = mapOf(
        "from" to "Optional month YYYY-MM or date",
        "to" to "Optional month YYYY-MM or date",
        "category" to "Optional category filter (e.g. 'Groceries', 'Rent')"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val latestSnapshot = snapshots.lastOrNull()
            ?: return ToolResult.unavailable("I don't have any expense records in your database.")

        val targetSnapshotId = arguments["from"]?.toString()?.take(7) ?: latestSnapshot.id
        val expenseSummary = portfolioRepository.calculateExpenseSummary(targetSnapshotId, ExpenseComparisonRange.ONE_MONTH)
        val categoryBreakdowns = portfolioRepository.getExpenseCategoryBreakdown(targetSnapshotId, ExpenseComparisonRange.ONE_MONTH)

        if (categoryBreakdowns.isEmpty() && expenseSummary.totalExpense == 0.0) {
            return ToolResult.unavailable("I don't have any expense records for period '$targetSnapshotId'.")
        }

        val filterCat = arguments["category"]?.toString()?.trim()?.lowercase()

        val filteredCats: List<ExpenseCategoryBreakdown> = if (!filterCat.isNullOrBlank()) {
            categoryBreakdowns.filter { it.category.name.lowercase().contains(filterCat) || it.category.id.lowercase().contains(filterCat) }
        } else {
            categoryBreakdowns
        }

        val categoriesData = filteredCats.map { b ->
            mapOf(
                "category" to b.category.name,
                "amount" to b.totalAmount,
                "formattedAmount" to formatInr(b.totalAmount),
                "percentage" to b.percentage,
                "formattedPercentage" to "${String.format("%.1f", b.percentage)}%",
                "momDiff" to b.diffAmount
            )
        }

        val totalExpense = if (filterCat.isNullOrBlank()) expenseSummary.totalExpense else filteredCats.sumOf { it.totalAmount }

        val data = mapOf(
            "period" to targetSnapshotId,
            "periodLabel" to portfolioRepository.formatSnapshotLabel(targetSnapshotId),
            "totalExpense" to totalExpense,
            "formattedTotalExpense" to formatInr(totalExpense),
            "threeMonthAverage" to expenseSummary.threeMonthAvg,
            "sixMonthAverage" to expenseSummary.sixMonthAvg,
            "highestCategory" to (expenseSummary.highestCategoryName ?: "None"),
            "highestCategoryAmount" to expenseSummary.highestCategoryAmount,
            "categories" to categoriesData
        )

        return ToolResult.success(
            data = data,
            summaryText = "Total expenses for ${portfolioRepository.formatSnapshotLabel(targetSnapshotId)}: ${formatInr(totalExpense)} across ${categoriesData.size} categories. Top spending: ${expenseSummary.highestCategoryName ?: "N/A"} (${formatInr(expenseSummary.highestCategoryAmount)}).",
            source = Source(
                title = "Local Expense Database ($targetSnapshotId)",
                snippet = "Total Spend: ${formatInr(totalExpense)}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 6. get_income
 */
class GetIncomeTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_income"
    override val description: String = "Retrieves active recurring income, salary, meal coupons, and other inflows recorded in the database."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = mapOf(
        "month" to "Optional month YYYY-MM"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val monthId = arguments["month"]?.toString()?.take(7)
        val recurring = portfolioRepository.calculateRecurringSummary(monthId)

        if (recurring.totalActiveInflows <= 0.0) {
            return ToolResult.unavailable("I don't have your salary or income information recorded in your local database.")
        }

        val items = recurring.items.filter { it.type == "SALARY" || it.type == "MEAL_COUPON" || it.type == "OTHER" }.map {
            mapOf(
                "name" to it.name,
                "type" to it.type,
                "monthlyAmount" to it.monthlyAmount,
                "formattedAmount" to formatInr(it.monthlyAmount),
                "isActive" to it.isActive
            )
        }

        val data = mapOf(
            "totalMonthlyInflows" to recurring.totalActiveInflows,
            "formattedTotalInflows" to formatInr(recurring.totalActiveInflows),
            "salary" to recurring.totalActiveSalary,
            "mealCoupon" to recurring.totalActiveMealCoupon,
            "otherInflows" to recurring.totalActiveOtherInflows,
            "incomeStreams" to items
        )

        return ToolResult.success(
            data = data,
            summaryText = "Total monthly income inflows: ${formatInr(recurring.totalActiveInflows)} (Salary: ${formatInr(recurring.totalActiveSalary)}, Meal Coupons: ${formatInr(recurring.totalActiveMealCoupon)}, Other: ${formatInr(recurring.totalActiveOtherInflows)}).",
            source = Source(
                title = "Local Inflows & Recurring Streams",
                snippet = "Monthly Inflows: ${formatInr(recurring.totalActiveInflows)}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 7. get_savings_rate
 */
class GetSavingsRateTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_savings_rate"
    override val description: String = "Calculates the deterministic savings rate ((Inflows - Expenses) / Inflows * 100) using verified local data."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val latestSnapshot = snapshots.lastOrNull()
            ?: return ToolResult.unavailable("No snapshots found in database.")

        val summary = portfolioRepository.calculateExpenseSummary(latestSnapshot.id, ExpenseComparisonRange.ONE_MONTH)

        if (summary.monthlyInflows <= 0.0) {
            return ToolResult.unavailable("I don't have your income information recorded, so I cannot calculate your savings rate accurately.")
        }

        val savingsAmount = summary.monthlyInflows - summary.totalExpense
        val savingsRate = if (summary.monthlyInflows > 0) (savingsAmount / summary.monthlyInflows) * 100.0 else 0.0

        val data = mapOf(
            "snapshotId" to latestSnapshot.id,
            "monthlyInflows" to summary.monthlyInflows,
            "formattedInflows" to formatInr(summary.monthlyInflows),
            "monthlyExpenses" to summary.totalExpense,
            "formattedExpenses" to formatInr(summary.totalExpense),
            "savingsAmount" to savingsAmount,
            "formattedSavingsAmount" to formatInr(savingsAmount),
            "savingsRatePercentage" to savingsRate,
            "formattedSavingsRate" to "${String.format("%.1f", savingsRate)}%"
        )

        return ToolResult.success(
            data = data,
            summaryText = "Your savings rate for ${portfolioRepository.formatSnapshotLabel(latestSnapshot.id)} is ${String.format("%.1f", savingsRate)}% (Income: ${formatInr(summary.monthlyInflows)}, Expenses: ${formatInr(summary.totalExpense)}, Net Savings: ${formatInr(savingsAmount)}).",
            source = Source(
                title = "Deterministic Savings Rate Engine",
                snippet = "Formula: (Income - Expenses) / Income = ${String.format("%.1f", savingsRate)}%",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * 8. get_cash_flow
 */
class GetCashFlowTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_cash_flow"
    override val description: String = "Retrieves complete cash flow breakdown: Inflows (Salary/Coupons), Expenses, Active SIP investments, and Net Free Cash Flow."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val latestSnapshot = snapshots.lastOrNull()
            ?: return ToolResult.unavailable("No financial snapshot recorded in database.")

        val recurring = portfolioRepository.calculateRecurringSummary(latestSnapshot.id)
        val expenseSummary = portfolioRepository.calculateExpenseSummary(latestSnapshot.id, ExpenseComparisonRange.ONE_MONTH)

        val totalInflows = recurring.totalActiveInflows
        val totalExpenses = expenseSummary.totalExpense
        val totalSip = recurring.totalActiveSipAmount
        val netFreeCashFlow = totalInflows - totalExpenses - totalSip

        val data = mapOf(
            "snapshotId" to latestSnapshot.id,
            "totalInflows" to totalInflows,
            "formattedInflows" to formatInr(totalInflows),
            "totalExpenses" to totalExpenses,
            "formattedExpenses" to formatInr(totalExpenses),
            "sipInvestments" to totalSip,
            "formattedSipInvestments" to formatInr(totalSip),
            "netCashFlow" to netFreeCashFlow,
            "formattedNetCashFlow" to formatInr(netFreeCashFlow)
        )

        return ToolResult.success(
            data = data,
            summaryText = "Monthly Cash Flow: Inflows ${formatInr(totalInflows)} - Expenses ${formatInr(totalExpenses)} - SIPs ${formatInr(totalSip)} = Net Free Cash Flow: ${formatInr(netFreeCashFlow)}.",
            source = Source(
                title = "Local Cash Flow Ledger",
                snippet = "Net Cash Flow: ${formatInr(netFreeCashFlow)}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 9. get_monthly_comparison
 */
class GetMonthlyComparisonTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "get_monthly_comparison"
    override val description: String = "Compares two specific months for net worth change, organic portfolio gain, expense variance, and category shifts."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = mapOf(
        "monthA" to "Base month in YYYY-MM format",
        "monthB" to "Comparison month in YYYY-MM format"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        if (snapshots.size < 2) {
            return ToolResult.unavailable("Need at least 2 recorded monthly snapshots to perform comparison.")
        }

        val monthA = arguments["monthA"]?.toString()?.take(7) ?: snapshots[snapshots.size - 2].id
        val monthB = arguments["monthB"]?.toString()?.take(7) ?: snapshots.last().id

        val defaultView = PortfolioViewEntity("all", "All Assets", true, "*")
        val comp = portfolioRepository.compareTwoPeriods(monthA, monthB, defaultView)

        val data = mapOf(
            "periodA" to comp.periodAId,
            "periodB" to comp.periodBId,
            "periodALabel" to comp.periodALabel,
            "periodBLabel" to comp.periodBLabel,
            "netWorthA" to comp.totalA,
            "formattedNetWorthA" to formatInr(comp.totalA),
            "netWorthB" to comp.totalB,
            "formattedNetWorthB" to formatInr(comp.totalB),
            "totalDiff" to comp.totalDiff,
            "formattedTotalDiff" to formatInr(comp.totalDiff),
            "growthPercentage" to comp.totalGrowthPercentage,
            "organicDiff" to comp.organicDiff,
            "formattedOrganicDiff" to formatInr(comp.organicDiff),
            "capitalInflows" to comp.totalNetCapitalInflows,
            "formattedCapitalInflows" to formatInr(comp.totalNetCapitalInflows),
            "expenseA" to comp.expenseA,
            "expenseB" to comp.expenseB,
            "expenseDiff" to comp.expenseDiff
        )

        val summaryText = "From ${comp.periodALabel} to ${comp.periodBLabel}, Net Worth changed by ${formatInr(comp.totalDiff)} (${String.format("%.2f", comp.totalGrowthPercentage ?: 0.0)}%). Fresh capital was ${formatInr(comp.totalNetCapitalInflows)} and organic market gain was ${formatInr(comp.organicDiff)}."

        return ToolResult.success(
            data = data,
            summaryText = summaryText,
            source = Source(
                title = "Two-Period Comparison Engine",
                snippet = "${comp.periodALabel} vs ${comp.periodBLabel}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 10. get_fire_projection
 */
class GetFireProjectionTool(
    private val portfolioRepository: PortfolioRepository,
    private val fireSettingsRepository: FireSettingsRepository,
    private val userProfileRepository: UserProfileRepository
) : FinanceTool {
    override val name: String = "get_fire_projection"
    override val description: String = "Computes deterministic FIRE (Financial Independence Retire Early) projections, target corpus, years to FIRE, and Coast FIRE status using the domain engine."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val snapshots = portfolioRepository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val latestSnapshot = snapshots.lastOrNull()
        val defaultView = PortfolioViewEntity("all", "All Assets", true, "*")

        val currentNetWorth = if (latestSnapshot != null) {
            val summary = portfolioRepository.calculatePortfolioSummary(latestSnapshot.id, defaultView, TimeRangeOption.MOM)
            summary.totalValue
        } else 0.0

        val settings = fireSettingsRepository.fireSettings.value
        val forecast = FireCalculator.calculateForecast(settings, currentNetWorth)

        val data = mapOf(
            "currentNetWorth" to forecast.currentNetWorth,
            "formattedNetWorth" to formatInr(forecast.currentNetWorth),
            "targetCorpus" to forecast.targetFireCorpus,
            "formattedTargetCorpus" to formatInr(forecast.targetFireCorpus),
            "leanFireCorpus" to forecast.leanFireCorpus,
            "fatFireCorpus" to forecast.fatFireCorpus,
            "progressPercentage" to forecast.progressPercentage,
            "formattedProgress" to "${String.format("%.1f", forecast.progressPercentage)}%",
            "yearsRemaining" to forecast.yearsToFire,
            "projectedFireAge" to forecast.projectedFireAge,
            "projectedFireYear" to forecast.projectedFireYear,
            "monthlyExpenses" to settings.monthlyExpenses,
            "monthlyInvestment" to settings.monthlyInvestment,
            "expectedReturn" to settings.expectedReturnRate,
            "inflation" to settings.inflationRate,
            "swr" to settings.swrRate,
            "coastFireCorpus" to forecast.coastFireCorpus,
            "isCoastFireAchieved" to forecast.isCoastFireAchieved,
            "isFireAchieved" to forecast.isFireAchieved
        )

        val summaryText = "FIRE Projection: Target Corpus ${formatInr(forecast.targetFireCorpus)} (Current: ${formatInr(forecast.currentNetWorth)}, ${String.format("%.1f", forecast.progressPercentage)}% achieved). Projected FIRE Age: ${String.format("%.1f", forecast.projectedFireAge)} years (${String.format("%.1f", forecast.yearsToFire)} years remaining). Coast FIRE achieved: ${forecast.isCoastFireAchieved}."

        return ToolResult.success(
            data = data,
            summaryText = summaryText,
            source = Source(
                title = "Deterministic FIRE Engine (Safe Withdrawal Rate ${settings.swrRate}%)",
                snippet = "Target: ${formatInr(forecast.targetFireCorpus)}, Projected Age: ${String.format("%.1f", forecast.projectedFireAge)}",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * 11. get_loans_and_liabilities
 */
class GetLoansAndLiabilitiesTool(
    private val loanRepository: LoanRepository
) : FinanceTool {
    override val name: String = "get_loans_and_liabilities"
    override val description: String = "Retrieves the user's active and closed loans, including home loan, car loan, education loan, personal loans, total outstanding balance, interest rates, and monthly EMI commitments."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = emptyMap()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val loans = loanRepository.allLoans.first()
        val summary = loanRepository.loanSummary.first()

        val activeLoans = loans.filter { it.isActive }
        val loansBreakdown = loans.map { loan ->
            mapOf(
                "name" to loan.name,
                "type" to loan.loanType,
                "lender" to loan.lenderBank,
                "principalAmount" to loan.originalPrincipal,
                "outstandingPrincipal" to loan.currentOutstanding,
                "formattedOutstanding" to formatInr(loan.currentOutstanding),
                "interestRate" to "${loan.annualInterestRate}%",
                "monthlyEmi" to loan.monthlyEmi,
                "formattedEmi" to formatInr(loan.monthlyEmi),
                "remainingTenureMonths" to loan.remainingTenureMonths,
                "isActive" to loan.isActive
            )
        }

        val data = mapOf(
            "totalOutstandingDebt" to summary.totalOutstanding,
            "formattedTotalOutstanding" to formatInr(summary.totalOutstanding),
            "totalMonthlyEmi" to summary.totalMonthlyEmi,
            "formattedMonthlyEmi" to formatInr(summary.totalMonthlyEmi),
            "activeLoansCount" to summary.activeLoansCount,
            "weightedAverageInterestRate" to "${String.format("%.2f", summary.weightedAverageInterestRate)}%",
            "loans" to loansBreakdown
        )

        val summaryText = if (activeLoans.isEmpty()) {
            "No active loans recorded in your portfolio."
        } else {
            "Active Loans: ${summary.activeLoansCount} loans with Total Outstanding Debt of ${formatInr(summary.totalOutstanding)} and monthly EMI outflow of ${formatInr(summary.totalMonthlyEmi)} (Weighted Average Interest Rate: ${String.format("%.2f", summary.weightedAverageInterestRate)}%)."
        }

        return ToolResult.success(
            data = data,
            summaryText = summaryText,
            source = Source(
                title = "Local Loan & Liability Ledger",
                snippet = "Total Debt: ${formatInr(summary.totalOutstanding)}, Monthly EMI: ${formatInr(summary.totalMonthlyEmi)}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

/**
 * 12. get_insurance_coverage
 */
class GetInsuranceCoverageTool(
    private val insuranceRepository: InsuranceRepository
) : FinanceTool {
    override val name: String = "get_insurance_coverage"
    override val description: String = "Retrieves user insurance policies and safety net coverage including Term Life cover, Health insurance, Critical Illness, policy numbers, premium schedules, and renewal due dates."
    override val permission: ToolPermission = ToolPermission.READ_FINANCIAL_DATA
    override val parameterSchema: Map<String, String> = emptyMap()

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val policies = insuranceRepository.allPolicies.first()
        val summary = insuranceRepository.insuranceShieldSummary.first()

        val policyList = policies.map { policy ->
            mapOf(
                "policyName" to policy.policyName,
                "type" to policy.insuranceType,
                "provider" to policy.providerName,
                "sumInsured" to policy.sumInsured,
                "formattedSumInsured" to formatInr(policy.sumInsured),
                "annualPremium" to policy.annualPremium,
                "formattedAnnualPremium" to formatInr(policy.annualPremium),
                "premiumFrequency" to policy.premiumFrequency,
                "renewalDate" to policy.renewalDate,
                "isActive" to policy.isActive,
                "nominee" to policy.nomineeName
            )
        }

        val data = mapOf(
            "totalLifeCover" to summary.totalLifeCover,
            "formattedTotalLifeCover" to formatInr(summary.totalLifeCover),
            "totalHealthCover" to summary.totalHealthCover,
            "formattedTotalHealthCover" to formatInr(summary.totalHealthCover),
            "totalAnnualPremiums" to summary.totalAnnualPremiums,
            "formattedAnnualPremiums" to formatInr(summary.totalAnnualPremiums),
            "activePoliciesCount" to summary.activePoliciesCount,
            "policiesExpiringSoonCount" to summary.policiesExpiringSoonCount,
            "policies" to policyList
        )

        val summaryText = if (policies.isEmpty()) {
            "No insurance policies currently tracked in the database."
        } else {
            "Insurance Shield: Life Cover of ${formatInr(summary.totalLifeCover)}, Health Cover of ${formatInr(summary.totalHealthCover)}, across ${summary.activePoliciesCount} active policies with annual premium commitment of ${formatInr(summary.totalAnnualPremiums)}."
        }

        return ToolResult.success(
            data = data,
            summaryText = summaryText,
            source = Source(
                title = "Local Insurance & Protection Shield",
                snippet = "Life Cover: ${formatInr(summary.totalLifeCover)}, Health Cover: ${formatInr(summary.totalHealthCover)}",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }
}

