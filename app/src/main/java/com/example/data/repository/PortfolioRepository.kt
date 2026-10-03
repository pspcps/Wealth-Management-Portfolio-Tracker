package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseInitializer
import com.example.data.local.entity.*
import com.example.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.text.DateFormatSymbols
import java.util.Locale

class PortfolioRepository(private val db: AppDatabase) {

    private val portfolioDao = db.portfolioDao()
    private val expenseDao = db.expenseDao()

    // Ensure defaults exist
    suspend fun ensureDefaults() {
        DatabaseInitializer.ensureDefaultCategoriesExist(db)
    }

    suspend fun restoreDefaultCategories() {
        DatabaseInitializer.restoreAllDefaultCategories(db)
    }

    // Categories
    fun getAllCategories(): Flow<List<CategoryEntity>> = portfolioDao.getAllCategories()
    suspend fun getAllCategoriesList(): List<CategoryEntity> {
        val list = portfolioDao.getAllCategoriesList()
        if (list.isEmpty()) {
            DatabaseInitializer.ensureDefaultCategoriesExist(db)
            return portfolioDao.getAllCategoriesList()
        }
        return list
    }
    suspend fun saveCategory(category: CategoryEntity) = portfolioDao.insertCategory(category)
    suspend fun deleteCategory(category: CategoryEntity) = portfolioDao.deleteCategory(category)

    // Assets
    fun getAllAssets(): Flow<List<AssetEntity>> = portfolioDao.getAllAssets()
    fun getActiveAssets(): Flow<List<AssetEntity>> = portfolioDao.getActiveAssets()
    fun getAssetsByCategory(categoryId: String): Flow<List<AssetEntity>> = portfolioDao.getAssetsByCategory(categoryId)
    suspend fun getAllAssetsList(): List<AssetEntity> = portfolioDao.getAllAssetsList()
    suspend fun saveAsset(asset: AssetEntity): Long = portfolioDao.insertAsset(asset)
    suspend fun updateAsset(asset: AssetEntity) = portfolioDao.updateAsset(asset)
    suspend fun deleteAsset(asset: AssetEntity) {
        portfolioDao.deleteValuesByAsset(asset.id)
        portfolioDao.deleteAsset(asset)
    }

    suspend fun deleteAssetMonthlyValue(assetId: Long, snapshotId: String) {
        portfolioDao.deleteAssetMonthlyValue(assetId, snapshotId)
    }

    // Snapshots
    fun getAllSnapshots(): Flow<List<MonthlySnapshotEntity>> = portfolioDao.getAllSnapshots()
    suspend fun getAllSnapshotsList(): List<MonthlySnapshotEntity> = portfolioDao.getAllSnapshotsList()
    suspend fun getSnapshotById(id: String): MonthlySnapshotEntity? = portfolioDao.getSnapshotById(id)
    suspend fun saveSnapshot(snapshot: MonthlySnapshotEntity) = portfolioDao.insertSnapshot(snapshot)
    suspend fun deleteSnapshot(snapshot: MonthlySnapshotEntity) {
        portfolioDao.deleteSnapshot(snapshot)
        portfolioDao.deleteValuesBySnapshot(snapshot.id)
        expenseDao.deleteExpensesBySnapshot(snapshot.id)
    }

    // Asset Values
    fun getValuesBySnapshot(snapshotId: String): Flow<List<AssetMonthlyValueEntity>> = portfolioDao.getValuesBySnapshot(snapshotId)
    suspend fun getValuesBySnapshotList(snapshotId: String): List<AssetMonthlyValueEntity> = portfolioDao.getValuesBySnapshotList(snapshotId)
    suspend fun saveAssetValues(values: List<AssetMonthlyValueEntity>) = portfolioDao.insertAssetValues(values)
    suspend fun saveAssetValue(value: AssetMonthlyValueEntity) = portfolioDao.insertAssetValue(value)

    // Batch operations for Backup & Sync
    suspend fun insertSnapshots(snapshots: List<MonthlySnapshotEntity>) = portfolioDao.insertSnapshots(snapshots)
    suspend fun insertCategories(categories: List<CategoryEntity>) = portfolioDao.insertCategories(categories)
    suspend fun insertAssets(assets: List<AssetEntity>) = portfolioDao.insertAssets(assets)
    suspend fun insertRecurringCashFlows(flows: List<RecurringCashFlowEntity>) = portfolioDao.insertRecurringCashFlows(flows)
    suspend fun insertExpenseCategories(cats: List<ExpenseCategoryEntity>) = expenseDao.insertExpenseCategories(cats)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>) = expenseDao.insertExpenses(expenses)
    suspend fun getAllAssetValuesList(): List<AssetMonthlyValueEntity> = portfolioDao.getAllValuesList()
    suspend fun getAllExpensesList(): List<ExpenseEntity> = expenseDao.getAllExpensesList()

    // Views
    fun getAllPortfolioViews(): Flow<List<PortfolioViewEntity>> = portfolioDao.getAllPortfolioViews()
    suspend fun getAllPortfolioViewsList(): List<PortfolioViewEntity> = portfolioDao.getAllPortfolioViewsList()
    suspend fun savePortfolioView(view: PortfolioViewEntity) = portfolioDao.insertPortfolioView(view)
    suspend fun insertPortfolioViews(views: List<PortfolioViewEntity>) = portfolioDao.insertPortfolioViews(views)
    suspend fun deletePortfolioView(view: PortfolioViewEntity) = portfolioDao.deletePortfolioView(view)

    // Recurring Cash Flows / SIPs / Salary / Meal Coupons
    fun getAllRecurringCashFlows(): Flow<List<RecurringCashFlowEntity>> = portfolioDao.getAllRecurringCashFlows()
    suspend fun getAllRecurringCashFlowsList(): List<RecurringCashFlowEntity> = portfolioDao.getAllRecurringCashFlowsList()
    suspend fun saveRecurringCashFlow(flow: RecurringCashFlowEntity): Long = portfolioDao.insertRecurringCashFlow(flow)
    suspend fun updateRecurringCashFlow(flow: RecurringCashFlowEntity) = portfolioDao.updateRecurringCashFlow(flow)
    suspend fun deleteRecurringCashFlow(flow: RecurringCashFlowEntity) = portfolioDao.deleteRecurringCashFlow(flow)
    suspend fun getRecurringFlowsByAsset(assetId: Long) = portfolioDao.getRecurringFlowsByAsset(assetId)
    suspend fun getRecurringFlowsByCategory(categoryId: String) = portfolioDao.getRecurringFlowsByCategory(categoryId)

    // Check if recurring cash flow is active in a specific month "YYYY-MM"
    fun isFlowActiveInMonth(flow: RecurringCashFlowEntity, monthId: String): Boolean {
        if (!flow.isActive) return false
        if (flow.startMonth > monthId) return false
        if (flow.endMonth != null && flow.endMonth < monthId) return false
        return true
    }

    // Calculate recurring cash flow summary for a given month or overall
    suspend fun calculateRecurringSummary(monthId: String? = null): RecurringCashFlowSummary {
        val allFlows = portfolioDao.getAllRecurringCashFlowsList()
        val activeFlows = if (monthId != null) {
            allFlows.filter { isFlowActiveInMonth(it, monthId) }
        } else {
            allFlows.filter { it.isActive }
        }

        val totalSip = activeFlows.filter { it.type == "SIP" || it.type == "RECURRING_INVESTMENT" || it.type == "PPF_EPF_CONTRIBUTION" }
            .sumOf { it.monthlyAmount }
        val totalRd = activeFlows.filter { it.type == "RD" }.sumOf { it.monthlyAmount }
        val totalInterestPayout = activeFlows.filter { it.type == "INTEREST_PAYOUT" }.sumOf { it.monthlyAmount }
        val totalPrincipalPayout = activeFlows.filter { it.type == "PRINCIPAL_PAYOUT" }.sumOf { it.monthlyAmount }
        val totalSalary = activeFlows.filter { it.type == "SALARY" }.sumOf { it.monthlyAmount }
        val totalMealCoupon = activeFlows.filter { it.type == "MEAL_COUPON" }.sumOf { it.monthlyAmount }
        val totalOther = activeFlows.filter { it.type == "OTHER" }.sumOf { it.monthlyAmount }

        return RecurringCashFlowSummary(
            totalActiveSipAmount = totalSip,
            totalActiveRdAmount = totalRd,
            totalActiveInterestPayout = totalInterestPayout,
            totalActivePrincipalPayout = totalPrincipalPayout,
            totalActiveSalary = totalSalary,
            totalActiveMealCoupon = totalMealCoupon,
            totalActiveOtherInflows = totalOther,
            items = allFlows
        )
    }

    // Expenses
    fun getAllExpenseCategories(): Flow<List<ExpenseCategoryEntity>> = expenseDao.getAllExpenseCategories()
    suspend fun getAllExpenseCategoriesList(): List<ExpenseCategoryEntity> = expenseDao.getAllExpenseCategoriesList()
    suspend fun saveExpenseCategory(cat: ExpenseCategoryEntity) = expenseDao.insertExpenseCategory(cat)
    suspend fun deleteExpenseCategory(cat: ExpenseCategoryEntity) = expenseDao.deleteExpenseCategory(cat)

    fun getAllExpenses(): Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    fun getExpensesBySnapshot(snapshotId: String): Flow<List<ExpenseEntity>> = expenseDao.getExpensesBySnapshot(snapshotId)
    suspend fun getExpensesBySnapshotList(snapshotId: String): List<ExpenseEntity> = expenseDao.getExpensesBySnapshotList(snapshotId)
    suspend fun saveExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    // Recurring Fixed Expenses (Rent, Wifi, Maintenance, Subscriptions)
    suspend fun getActiveRecurringExpenses(snapshotId: String? = null): List<RecurringCashFlowEntity> {
        val allFlows = portfolioDao.getAllRecurringCashFlowsList()
        return allFlows.filter { flow ->
            flow.isActive && 
            (flow.type == "FIXED_EXPENSE" || flow.type == "RECURRING_EXPENSE") &&
            (snapshotId == null || isFlowActiveInMonth(flow, snapshotId))
        }
    }

    suspend fun autoPopulateRecurringExpensesForSnapshot(snapshotId: String): Int {
        val recurringExpenses = getActiveRecurringExpenses(snapshotId)
        val existingExpenses = expenseDao.getExpensesBySnapshotList(snapshotId)
        val existingTitles = existingExpenses.map { it.title.trim().lowercase() }.toSet()

        var addedCount = 0
        for (flow in recurringExpenses) {
            if (!existingTitles.contains(flow.name.trim().lowercase())) {
                expenseDao.insertExpense(
                    ExpenseEntity(
                        snapshotId = snapshotId,
                        expenseCategoryId = flow.categoryId.ifEmpty { "utilities" },
                        amount = flow.monthlyAmount,
                        title = flow.name,
                        dateMillis = System.currentTimeMillis(),
                        notes = "Auto-added recurring fixed expense"
                    )
                )
                addedCount++
            }
        }
        return addedCount
    }

    // Helper to format snapshot ID e.g. "2026-08" to "Aug 2026"
    fun formatSnapshotLabel(snapshotId: String): String {
        val parts = snapshotId.split("-")
        if (parts.size != 2) return snapshotId
        val year = parts[0]
        val monthNum = parts[1].toIntOrNull() ?: return snapshotId
        val monthName = DateFormatSymbols().shortMonths.getOrNull(monthNum - 1) ?: parts[1]
        return "$monthName $year"
    }

    // Helper to find previous snapshot given an offset
    fun findComparisonSnapshot(
        allSnapshots: List<MonthlySnapshotEntity>,
        currentSnapshotId: String,
        timeRange: TimeRangeOption
    ): MonthlySnapshotEntity? {
        if (allSnapshots.isEmpty()) return null
        val sorted = allSnapshots.sortedWith(compareBy({ it.year }, { it.month }))
        val currentIndex = sorted.indexOfFirst { it.id == currentSnapshotId }
        if (currentIndex == -1) return null

        return when (timeRange) {
            TimeRangeOption.MOM -> if (currentIndex > 0) sorted[currentIndex - 1] else null
            TimeRangeOption.THREE_MONTHS -> {
                val targetIndex = currentIndex - 3
                if (targetIndex >= 0) sorted[targetIndex] else if (currentIndex > 0) sorted[0] else null
            }
            TimeRangeOption.SIX_MONTHS -> {
                val targetIndex = currentIndex - 6
                if (targetIndex >= 0) sorted[targetIndex] else if (currentIndex > 0) sorted[0] else null
            }
            TimeRangeOption.TWELVE_MONTHS -> {
                val targetIndex = currentIndex - 12
                if (targetIndex >= 0) sorted[targetIndex] else if (currentIndex > 0) sorted[0] else null
            }
            TimeRangeOption.ALL_TIME -> if (currentIndex > 0) sorted[0] else null
        }
    }

    // Helper to check if a category is included in a Portfolio View
    fun isCategoryIncludedInView(category: CategoryEntity, view: PortfolioViewEntity): Boolean {
        if (view.includedCategoryIds == "*") return true
        val includedIds = view.includedCategoryIds.split(",").map { it.trim() }.toSet()
        if (includedIds.contains(category.id)) return true

        if (view.id == "view_inv_epf") {
            if (category.groupType == "INVESTMENT" || category.id == "epf") return true
        } else if (view.id == "view_inv_only") {
            if (category.groupType == "INVESTMENT" && category.id != "epf" && category.id != "nps" && category.id != "child_plan") return true
        }
        return false
    }

    // Calculate Portfolio Summary for a Snapshot & View & TimeRange
    suspend fun calculatePortfolioSummary(
        snapshotId: String,
        view: PortfolioViewEntity,
        timeRange: TimeRangeOption
    ): PortfolioSummary {
        val allSnapshots = portfolioDao.getAllSnapshotsList()
        val allCategories = portfolioDao.getAllCategoriesList()
        val allAssets = portfolioDao.getAllAssetsList()
        val allSips = portfolioDao.getAllRecurringCashFlowsList()
        val currentValues = portfolioDao.getValuesBySnapshotList(snapshotId).associateBy { it.assetId }

        val includedCategories = allCategories.filter { isCategoryIncludedInView(it, view) }.map { it.id }.toSet()
        val includedAssets = allAssets.filter { includedCategories.contains(it.categoryId) }.map { it.id }.toSet()

        val totalCurrent = currentValues.filterKeys { includedAssets.contains(it) }.values.sumOf { it.value }
        val totalInvested = currentValues.filterKeys { includedAssets.contains(it) }.values.sumOf { 
            if (it.investedAmount > 0) it.investedAmount else it.value 
        }

        val compSnapshot = findComparisonSnapshot(allSnapshots, snapshotId, timeRange)
        val compValues = if (compSnapshot != null) {
            portfolioDao.getValuesBySnapshotList(compSnapshot.id).associateBy { it.assetId }
        } else {
            emptyMap()
        }
        val totalPrevious = compValues.filterKeys { includedAssets.contains(it) }.values.sumOf { it.value }
        val totalPrevInvested = compValues.filterKeys { includedAssets.contains(it) }.values.sumOf {
            if (it.investedAmount > 0) it.investedAmount else it.value
        }

        // Active SIPs for the current snapshot period that fall under the view
        val activeSips = allSips.filter { flow ->
            flow.isActive &&
            isFlowActiveInMonth(flow, snapshotId) &&
            includedCategories.contains(flow.categoryId) &&
            (flow.type == "SIP" || flow.type == "RECURRING_INVESTMENT" || flow.type == "PPF_EPF_CONTRIBUTION")
        }
        val totalSipInflows = activeSips.sumOf { it.monthlyAmount }
        val totalContributions = currentValues.filterKeys { includedAssets.contains(it) }.values.sumOf { it.contributions }
        val totalWithdrawals = currentValues.filterKeys { includedAssets.contains(it) }.values.sumOf { it.withdrawals }

        // Capital Inflow & New Investment Adjustment:
        // When new stocks/assets are bought from bank account:
        // Invested cost basis increases from totalPrevInvested to totalInvested.
        // This new capital taken from bank must NOT be treated as organic market returns!
        val investedIncrease = if (totalInvested > totalPrevInvested && totalPrevInvested > 0) {
            totalInvested - totalPrevInvested
        } else 0.0

        val netCapitalInflows = maxOf(investedIncrease, totalSipInflows + totalContributions - totalWithdrawals)
        val organicDiff = (totalCurrent - totalPrevious) - netCapitalInflows
        val organicGrowthPct = if (totalPrevious > 0) (organicDiff / totalPrevious) * 100.0 else null

        val growth = GrowthMetric(
            currentValue = totalCurrent,
            previousValue = totalPrevious,
            sipInflows = totalSipInflows,
            contributions = totalContributions + (if (investedIncrease > totalSipInflows) investedIncrease - totalSipInflows else 0.0),
            withdrawals = totalWithdrawals
        )

        // Investment Return Metric:
        // Must ONLY be calculated on true investment & retirement assets (Stocks, MFs, EPF, Gold, Bonds, etc.)
        // Cash/Bank Balance and salary deposits are NOT investment holdings!
        val investmentCatIds = allCategories.filter {
            (it.groupType == "INVESTMENT" || it.groupType == "RETIREMENT") && includedCategories.contains(it.id)
        }.map { it.id }.toSet()
        val investmentAssetIds = allAssets.filter { investmentCatIds.contains(it.categoryId) }.map { it.id }.toSet()

        val totalInvestmentCurrent = currentValues.filterKeys { investmentAssetIds.contains(it) }.values.sumOf { it.value }
        val totalInvestmentCost = currentValues.filterKeys { investmentAssetIds.contains(it) }.values.sumOf {
            if (it.investedAmount > 0) it.investedAmount else it.value
        }

        val investmentReturn = if (totalInvestmentCost > 0) {
            InvestmentReturnMetric(totalInvested = totalInvestmentCost, currentValue = totalInvestmentCurrent)
        } else {
            InvestmentReturnMetric(totalInvested = totalInvested, currentValue = totalCurrent)
        }

        // Calculate Net Monthly Savings (Salary/Inflow - Expenses for this month)
        val activeSalary = allSips.filter { flow ->
            flow.isActive && isFlowActiveInMonth(flow, snapshotId) && flow.type == "SALARY"
        }.sumOf { it.monthlyAmount }
        val monthExpenses = expenseDao.getExpensesBySnapshotList(snapshotId).sumOf { it.amount }
        val netMonthlySavings = (activeSalary - monthExpenses).coerceAtLeast(0.0)

        val compLabel = compSnapshot?.let { formatSnapshotLabel(it.id) }

        return PortfolioSummary(
            selectedSnapshotId = snapshotId,
            view = view,
            totalValue = totalCurrent,
            previousValue = totalPrevious,
            growth = growth,
            timeRange = timeRange,
            totalInvested = if (totalInvestmentCost > 0) totalInvestmentCost else totalInvested,
            investmentReturn = investmentReturn,
            activeMonthlySips = totalSipInflows,
            comparisonSnapshotId = compSnapshot?.id,
            comparisonSnapshotLabel = compLabel,
            marketOrganicGain = organicDiff,
            marketOrganicPercentage = organicGrowthPct,
            netMonthlySavings = netMonthlySavings,
            newCapitalInvested = netCapitalInflows
        )
    }

    // Category-wise Breakdown and Growth calculation
    suspend fun calculateCategoryGrowth(
        snapshotId: String,
        view: PortfolioViewEntity,
        timeRange: TimeRangeOption
    ): List<CategoryGrowthItem> {
        val allSnapshots = portfolioDao.getAllSnapshotsList()
        val allCategories = portfolioDao.getAllCategoriesList()
        val allAssets = portfolioDao.getAllAssetsList()
        val allSips = portfolioDao.getAllRecurringCashFlowsList()
        val currentValues = portfolioDao.getValuesBySnapshotList(snapshotId).associateBy { it.assetId }

        val compSnapshot = findComparisonSnapshot(allSnapshots, snapshotId, timeRange)
        val compValues = if (compSnapshot != null) {
            portfolioDao.getValuesBySnapshotList(compSnapshot.id).associateBy { it.assetId }
        } else {
            emptyMap()
        }
        val compLabel = compSnapshot?.let { formatSnapshotLabel(it.id) } ?: timeRange.label

        val includedCategories = allCategories.filter { isCategoryIncludedInView(it, view) }
        val assetsByCategory = allAssets.groupBy { it.categoryId }
        val sipsByCategory = allSips.filter { isFlowActiveInMonth(it, snapshotId) }.groupBy { it.categoryId }

        return includedCategories.map { category ->
            val assetsInCat = assetsByCategory[category.id] ?: emptyList()
            val assetIdsInCat = assetsInCat.map { it.id }.toSet()

            val currentVal = currentValues.filterKeys { assetIdsInCat.contains(it) }.values.sumOf { it.value }
            val investedVal = currentValues.filterKeys { assetIdsInCat.contains(it) }.values.sumOf { 
                if (it.investedAmount > 0) it.investedAmount else it.value 
            }
            val prevVal = compValues.filterKeys { assetIdsInCat.contains(it) }.values.sumOf { it.value }
            val prevInvestedVal = compValues.filterKeys { assetIdsInCat.contains(it) }.values.sumOf {
                if (it.investedAmount > 0) it.investedAmount else it.value
            }

            val catSips = sipsByCategory[category.id]?.sumOf { it.monthlyAmount } ?: 0.0
            val catContributions = currentValues.filterKeys { assetIdsInCat.contains(it) }.values.sumOf { it.contributions }
            val catWithdrawals = currentValues.filterKeys { assetIdsInCat.contains(it) }.values.sumOf { it.withdrawals }

            // Adjusted for new investments transferred from bank / SIP:
            val investedDiff = if (investedVal > prevInvestedVal && prevInvestedVal > 0) (investedVal - prevInvestedVal) else 0.0
            val netCatCapital = maxOf(investedDiff, catSips + catContributions - catWithdrawals)
            val organicDiff = (currentVal - prevVal) - netCatCapital
            val organicGrowthPercentage = if (prevVal > 0) (organicDiff / prevVal) * 100.0 else null

            CategoryGrowthItem(
                category = category,
                currentValue = currentVal,
                previousValue = prevVal,
                assetCount = assetsInCat.count { it.status == "ACTIVE" },
                investedAmount = investedVal,
                gainLossAmount = currentVal - investedVal,
                returnPercentage = if (investedVal > 0) ((currentVal - investedVal) / investedVal) * 100.0 else null,
                sipInflows = catSips,
                contributions = catContributions + (if (investedDiff > catSips) investedDiff - catSips else 0.0),
                withdrawals = catWithdrawals,
                netCapitalInflows = netCatCapital,
                organicDiff = organicDiff,
                organicGrowthPercentage = organicGrowthPercentage,
                comparisonLabel = compLabel
            )
        }.filter { it.currentValue > 0 || it.previousValue > 0 || it.assetCount > 0 }
    }

    // Asset Allocation calculation
    suspend fun calculateAssetAllocation(
        snapshotId: String,
        view: PortfolioViewEntity
    ): List<AllocationSlice> {
        val categoriesGrowth = calculateCategoryGrowth(snapshotId, view, TimeRangeOption.MOM)
        val total = categoriesGrowth.sumOf { it.currentValue }
        if (total <= 0.0) return emptyList()

        return categoriesGrowth
            .filter { it.currentValue > 0 }
            .map {
                AllocationSlice(
                    id = it.category.id,
                    name = it.category.name,
                    amount = it.currentValue,
                    percentage = (it.currentValue / total) * 100.0,
                    colorHex = it.category.colorHex,
                    groupType = it.category.groupType
                )
            }
            .sortedByDescending { it.amount }
    }

    // Historical Points (Jan -> Feb -> Mar ...) for charts
    suspend fun getHistoricalPoints(view: PortfolioViewEntity): List<HistoricalDataPoint> {
        val allSnapshots = portfolioDao.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val allCategories = portfolioDao.getAllCategoriesList()
        val allAssets = portfolioDao.getAllAssetsList()
        val allValues = portfolioDao.getAllValuesList().groupBy { it.snapshotId }
        val allExpenses = expenseDao.getAllExpensesList().groupBy { it.snapshotId }
        val allSips = portfolioDao.getAllRecurringCashFlowsList()

        val includedCategories = allCategories.filter { isCategoryIncludedInView(it, view) }.map { it.id }.toSet()
        val includedAssets = allAssets.filter { includedCategories.contains(it.categoryId) }.map { it.id }.toSet()

        return allSnapshots.map { snapshot ->
            val valuesInSnapshot = allValues[snapshot.id] ?: emptyList()
            val totalPortfolio = valuesInSnapshot
                .filter { includedAssets.contains(it.assetId) }
                .sumOf { it.value }

            val totalInvested = valuesInSnapshot
                .filter { includedAssets.contains(it.assetId) }
                .sumOf { if (it.investedAmount > 0) it.investedAmount else it.value }

            val expensesInSnapshot = allExpenses[snapshot.id] ?: emptyList()
            val totalExpense = expensesInSnapshot.sumOf { it.amount }

            val sipsInMonth = allSips.filter { isFlowActiveInMonth(it, snapshot.id) && includedCategories.contains(it.categoryId) }
                .sumOf { it.monthlyAmount }

            val monthName = DateFormatSymbols().shortMonths.getOrNull(snapshot.month - 1) ?: "${snapshot.month}"
            val shortYear = snapshot.year.toString().takeLast(2)

            HistoricalDataPoint(
                snapshotId = snapshot.id,
                year = snapshot.year,
                month = snapshot.month,
                label = "$monthName '$shortYear",
                portfolioValue = totalPortfolio,
                investedValue = totalInvested,
                expenseValue = totalExpense,
                sipInflows = sipsInMonth
            )
        }
    }

    // Expense Summary and Analytics with Multi-Range (1M, 3M, 6M, 12M) Comparison
    suspend fun calculateExpenseSummary(
        snapshotId: String,
        range: ExpenseComparisonRange = ExpenseComparisonRange.ONE_MONTH
    ): ExpenseSummary {
        val allSnapshots = portfolioDao.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val allExpenses = expenseDao.getAllExpensesList()
        val allExpenseCategories = expenseDao.getAllExpenseCategoriesList().associateBy { it.id }
        val allFlows = portfolioDao.getAllRecurringCashFlowsList()
        val allValues = portfolioDao.getValuesBySnapshotList(snapshotId)

        val totalPortfolioNetWorth = allValues.sumOf { it.value }

        val activeFlows = allFlows.filter {
            it.isActive && it.startMonth <= snapshotId && (it.endMonth == null || it.endMonth >= snapshotId)
        }
        val monthlyInflows = activeFlows.filter { it.type == "SALARY" || it.type == "MEAL_COUPON" || it.type == "INFLOW" || it.type == "OTHER_INFLOW" }
            .sumOf { it.monthlyAmount }

        val currentMonthExpenses = allExpenses.filter { it.snapshotId == snapshotId }
        val currentTotal = currentMonthExpenses.sumOf { it.amount }

        val currentIndex = allSnapshots.indexOfFirst { it.id == snapshotId }
        val priorSnapshots = if (currentIndex > 0) allSnapshots.subList(0, currentIndex) else emptyList()
        val prevSnapshot = priorSnapshots.lastOrNull()
        val prevTotal = if (prevSnapshot != null) {
            allExpenses.filter { it.snapshotId == prevSnapshot.id }.sumOf { it.amount }
        } else {
            0.0
        }

        val last3 = priorSnapshots.takeLast(3).map { snap -> allExpenses.filter { it.snapshotId == snap.id }.sumOf { it.amount } }
        val last6 = priorSnapshots.takeLast(6).map { snap -> allExpenses.filter { it.snapshotId == snap.id }.sumOf { it.amount } }
        val last12 = priorSnapshots.takeLast(12).map { snap -> allExpenses.filter { it.snapshotId == snap.id }.sumOf { it.amount } }

        val avg3 = if (last3.isNotEmpty()) last3.average() else 0.0
        val avg6 = if (last6.isNotEmpty()) last6.average() else 0.0
        val avg12 = if (last12.isNotEmpty()) last12.average() else 0.0

        val (benchmarkExpense, benchmarkLabel) = when (range) {
            ExpenseComparisonRange.ONE_MONTH -> Pair(prevTotal, "vs Prev Month")
            ExpenseComparisonRange.THREE_MONTHS -> Pair(if (avg3 > 0) avg3 else prevTotal, "vs 3M Avg")
            ExpenseComparisonRange.SIX_MONTHS -> Pair(if (avg6 > 0) avg6 else prevTotal, "vs 6M Avg")
            ExpenseComparisonRange.TWELVE_MONTHS -> Pair(if (avg12 > 0) avg12 else prevTotal, "vs 12M Avg")
        }

        val byCat = currentMonthExpenses.groupBy { it.expenseCategoryId }
        val catTotals = byCat.mapValues { it.value.sumOf { exp -> exp.amount } }
        val highest = catTotals.maxByOrNull { it.value }
        val lowest = catTotals.minByOrNull { it.value }

        val savings = if (monthlyInflows > 0) monthlyInflows - currentTotal else 0.0
        val savingsRate = if (monthlyInflows > 0) (savings / monthlyInflows) * 100.0 else null
        val runway = if (currentTotal > 0 && totalPortfolioNetWorth > 0) totalPortfolioNetWorth / currentTotal else null

        return ExpenseSummary(
            selectedSnapshotId = snapshotId,
            totalExpense = currentTotal,
            previousTotalExpense = prevTotal,
            growth = GrowthMetric(currentTotal, prevTotal),
            threeMonthAvg = avg3,
            sixMonthAvg = avg6,
            twelveMonthAvg = avg12,
            highestCategoryName = highest?.key?.let { allExpenseCategories[it]?.name ?: it },
            highestCategoryAmount = highest?.value ?: 0.0,
            lowestCategoryName = lowest?.key?.let { allExpenseCategories[it]?.name ?: it },
            lowestCategoryAmount = lowest?.value ?: 0.0,
            monthlyInflows = monthlyInflows,
            savingsAmount = savings,
            savingsRatePercentage = savingsRate,
            runwayMonths = runway,
            comparisonRange = range,
            benchmarkExpense = benchmarkExpense,
            benchmarkLabel = benchmarkLabel,
            benchmarkGrowth = GrowthMetric(currentTotal, benchmarkExpense)
        )
    }

    // Expense Category Breakdown with dynamic timeframe comparison (1M, 3M, 6M, 12M)
    suspend fun getExpenseCategoryBreakdown(
        snapshotId: String,
        range: ExpenseComparisonRange = ExpenseComparisonRange.ONE_MONTH
    ): List<ExpenseCategoryBreakdown> {
        val categories = expenseDao.getAllExpenseCategoriesList()
        val allExpenses = expenseDao.getAllExpensesList()
        val allSnapshots = portfolioDao.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))

        val currentIndex = allSnapshots.indexOfFirst { it.id == snapshotId }
        val priorSnapshots = if (currentIndex > 0) allSnapshots.subList(0, currentIndex) else emptyList()
        val prevSnapshotId = priorSnapshots.lastOrNull()?.id

        val currentExpenses = allExpenses.filter { it.snapshotId == snapshotId }
        val total = currentExpenses.sumOf { it.amount }
        val groupedCurrent = currentExpenses.groupBy { it.expenseCategoryId }

        val comparisonLabel = when (range) {
            ExpenseComparisonRange.ONE_MONTH -> "Prev Month"
            ExpenseComparisonRange.THREE_MONTHS -> "3M Avg"
            ExpenseComparisonRange.SIX_MONTHS -> "6M Avg"
            ExpenseComparisonRange.TWELVE_MONTHS -> "12M Avg"
        }

        return categories.mapNotNull { cat ->
            val catExpenses = groupedCurrent[cat.id] ?: emptyList()
            val sum = catExpenses.sumOf { it.amount }

            val compAmount = when (range) {
                ExpenseComparisonRange.ONE_MONTH -> {
                    if (prevSnapshotId != null) {
                        allExpenses.filter { it.snapshotId == prevSnapshotId && it.expenseCategoryId == cat.id }.sumOf { it.amount }
                    } else 0.0
                }
                ExpenseComparisonRange.THREE_MONTHS -> {
                    val snaps = priorSnapshots.takeLast(3)
                    if (snaps.isNotEmpty()) {
                        snaps.map { snap -> allExpenses.filter { it.snapshotId == snap.id && it.expenseCategoryId == cat.id }.sumOf { it.amount } }.average()
                    } else 0.0
                }
                ExpenseComparisonRange.SIX_MONTHS -> {
                    val snaps = priorSnapshots.takeLast(6)
                    if (snaps.isNotEmpty()) {
                        snaps.map { snap -> allExpenses.filter { it.snapshotId == snap.id && it.expenseCategoryId == cat.id }.sumOf { it.amount } }.average()
                    } else 0.0
                }
                ExpenseComparisonRange.TWELVE_MONTHS -> {
                    val snaps = priorSnapshots.takeLast(12)
                    if (snaps.isNotEmpty()) {
                        snaps.map { snap -> allExpenses.filter { it.snapshotId == snap.id && it.expenseCategoryId == cat.id }.sumOf { it.amount } }.average()
                    } else 0.0
                }
            }

            if (sum > 0 || compAmount > 0 || catExpenses.isNotEmpty()) {
                ExpenseCategoryBreakdown(
                    category = cat,
                    totalAmount = sum,
                    comparisonAmount = compAmount,
                    comparisonLabel = comparisonLabel,
                    percentage = if (total > 0) (sum / total) * 100.0 else 0.0,
                    itemCount = catExpenses.size
                )
            } else null
        }.sortedByDescending { it.totalAmount }
    }

    // Two Period Comparison Report with SIP & Inflows Subtraction
    suspend fun compareTwoPeriods(
        periodAId: String,
        periodBId: String,
        view: PortfolioViewEntity
    ): TwoPeriodComparison {
        val allCategories = portfolioDao.getAllCategoriesList()
        val allAssets = portfolioDao.getAllAssetsList()
        val allSips = portfolioDao.getAllRecurringCashFlowsList()
        val valuesA = portfolioDao.getValuesBySnapshotList(periodAId).associateBy { it.assetId }
        val valuesB = portfolioDao.getValuesBySnapshotList(periodBId).associateBy { it.assetId }

        val includedCategories = allCategories.filter { isCategoryIncludedInView(it, view) }
        val includedCategoryIds = includedCategories.map { it.id }.toSet()
        val includedAssets = allAssets.filter { includedCategoryIds.contains(it.categoryId) }
        val includedAssetIds = includedAssets.map { it.id }.toSet()

        val totalA = valuesA.filterKeys { includedAssetIds.contains(it) }.values.sumOf { it.value }
        val totalB = valuesB.filterKeys { includedAssetIds.contains(it) }.values.sumOf { it.value }

        // Total active SIPs in period B
        val activeSipsB = allSips.filter { isFlowActiveInMonth(it, periodBId) && includedCategoryIds.contains(it.categoryId) }
        val totalSipInflows = activeSipsB.sumOf { it.monthlyAmount }
        val totalContributions = valuesB.filterKeys { includedAssetIds.contains(it) }.values.sumOf { it.contributions }
        val totalWithdrawals = valuesB.filterKeys { includedAssetIds.contains(it) }.values.sumOf { it.withdrawals }

        // Category comparisons
        val assetsByCat = includedAssets.groupBy { it.categoryId }
        val sipsByCat = activeSipsB.groupBy { it.categoryId }

        val catComparisons = includedCategories.map { cat ->
            val catAssets = assetsByCat[cat.id] ?: emptyList()
            val catAssetIds = catAssets.map { it.id }.toSet()
            val valA = valuesA.filterKeys { catAssetIds.contains(it) }.values.sumOf { it.value }
            val valB = valuesB.filterKeys { catAssetIds.contains(it) }.values.sumOf { it.value }
            val catSip = sipsByCat[cat.id]?.sumOf { it.monthlyAmount } ?: 0.0
            val catContrib = valuesB.filterKeys { catAssetIds.contains(it) }.values.sumOf { it.contributions }
            val catWithdr = valuesB.filterKeys { catAssetIds.contains(it) }.values.sumOf { it.withdrawals }

            CategoryComparisonItem(
                category = cat,
                valueA = valA,
                valueB = valB,
                sipInflows = catSip,
                contributions = catContrib,
                withdrawals = catWithdr
            )
        }.filter { it.valueA > 0 || it.valueB > 0 }

        // Asset comparisons
        val catMap = allCategories.associateBy { it.id }
        val sipsByAsset = activeSipsB.filter { it.assetId != null }.groupBy { it.assetId }

        val assetComparisons = includedAssets.map { asset ->
            val valA = valuesA[asset.id]?.value ?: 0.0
            val valB = valuesB[asset.id]?.value ?: 0.0
            val assetSip = sipsByAsset[asset.id]?.sumOf { it.monthlyAmount } ?: 0.0
            val assetContrib = valuesB[asset.id]?.contributions ?: 0.0
            val assetWithdr = valuesB[asset.id]?.withdrawals ?: 0.0

            AssetComparisonItem(
                asset = asset,
                categoryName = catMap[asset.categoryId]?.name ?: asset.categoryId,
                valueA = valA,
                valueB = valB,
                sipInflows = assetSip,
                contributions = assetContrib,
                withdrawals = assetWithdr
            )
        }.filter { it.valueA > 0 || it.valueB > 0 }

        // Expense comparison
        val expA = expenseDao.getExpensesBySnapshotList(periodAId).sumOf { it.amount }
        val expB = expenseDao.getExpensesBySnapshotList(periodBId).sumOf { it.amount }

        return TwoPeriodComparison(
            periodAId = periodAId,
            periodBId = periodBId,
            periodALabel = formatSnapshotLabel(periodAId),
            periodBLabel = formatSnapshotLabel(periodBId),
            totalA = totalA,
            totalB = totalB,
            totalSipInflows = totalSipInflows,
            totalContributions = totalContributions,
            totalWithdrawals = totalWithdrawals,
            categoryComparisons = catComparisons.sortedByDescending { it.valueB },
            assetComparisons = assetComparisons.sortedByDescending { it.valueB },
            expenseA = expA,
            expenseB = expB
        )
    }

    // Comprehensive Transfer / Reinvestment / Redemption Engine
    suspend fun transferOrReinvestAsset(
        snapshotId: String,
        fromAssetId: Long?,
        toAssetId: Long?,
        amount: Double,
        isExternalWithdrawal: Boolean = false,
        isExternalDeposit: Boolean = false,
        closeSourceAsset: Boolean = false,
        notes: String = ""
    ): String {
        if (amount <= 0.0) return "Please enter a valid amount greater than 0"
        if (snapshotId.isEmpty()) return "Please select a valid month snapshot"
        if (fromAssetId == null && toAssetId == null) return "Source or Destination asset must be specified"

        // Ensure snapshot entity exists
        val parts = snapshotId.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 8
        saveSnapshot(
            MonthlySnapshotEntity(
                id = snapshotId,
                year = y,
                month = m,
                notes = "",
                updatedAt = System.currentTimeMillis()
            )
        )

        val currentValues = getValuesBySnapshotList(snapshotId).associateBy { it.assetId }
        val allAssets = getAllAssetsList().associateBy { it.id }

        // 1. Process Source Asset (Redemption / Outflow / Liquidation)
        if (fromAssetId != null && fromAssetId > 0) {
            val sourceAsset = allAssets[fromAssetId]
            val existingSourceVal = currentValues[fromAssetId]
            val prevSourceValues = findComparisonSnapshot(getAllSnapshotsList(), snapshotId, TimeRangeOption.MOM)?.let { prev ->
                getValuesBySnapshotList(prev.id).associateBy { it.assetId }
            }
            val baseVal = existingSourceVal?.value ?: prevSourceValues?.get(fromAssetId)?.value ?: sourceAsset?.initialInvestedAmount ?: 0.0
            val baseInvested = existingSourceVal?.investedAmount ?: prevSourceValues?.get(fromAssetId)?.investedAmount ?: sourceAsset?.initialInvestedAmount ?: baseVal

            val newWithdrawals = (existingSourceVal?.withdrawals ?: 0.0) + amount
            val newSourceVal = if (closeSourceAsset) 0.0 else maxOf(0.0, baseVal - amount)
            val newInvested = if (closeSourceAsset) 0.0 else maxOf(0.0, baseInvested - minOf(amount, baseInvested))

            saveAssetValue(
                AssetMonthlyValueEntity(
                    assetId = fromAssetId,
                    snapshotId = snapshotId,
                    value = newSourceVal,
                    investedAmount = newInvested,
                    contributions = existingSourceVal?.contributions ?: 0.0,
                    withdrawals = newWithdrawals,
                    notes = if (notes.isNotEmpty()) notes else "Redeemed ₹${Math.round(amount)}"
                )
            )

            if (closeSourceAsset && sourceAsset != null) {
                updateAsset(sourceAsset.copy(status = "CLOSED"))
            }
        }

        // 2. Process Destination Asset (Reinvestment / Contribution / Inflow)
        if (toAssetId != null && toAssetId > 0) {
            val destAsset = allAssets[toAssetId]
            val existingDestVal = currentValues[toAssetId]
            val prevDestValues = findComparisonSnapshot(getAllSnapshotsList(), snapshotId, TimeRangeOption.MOM)?.let { prev ->
                getValuesBySnapshotList(prev.id).associateBy { it.assetId }
            }
            val baseVal = existingDestVal?.value ?: prevDestValues?.get(toAssetId)?.value ?: destAsset?.initialInvestedAmount ?: 0.0
            val baseInvested = existingDestVal?.investedAmount ?: prevDestValues?.get(toAssetId)?.investedAmount ?: destAsset?.initialInvestedAmount ?: baseVal

            val newContributions = (existingDestVal?.contributions ?: 0.0) + amount
            val newDestVal = baseVal + amount
            val newInvested = baseInvested + amount

            saveAssetValue(
                AssetMonthlyValueEntity(
                    assetId = toAssetId,
                    snapshotId = snapshotId,
                    value = newDestVal,
                    investedAmount = newInvested,
                    contributions = newContributions,
                    withdrawals = existingDestVal?.withdrawals ?: 0.0,
                    notes = if (notes.isNotEmpty()) notes else "Reinvested / Inflow ₹${Math.round(amount)}"
                )
            )

            if (destAsset != null && destAsset.status == "CLOSED") {
                updateAsset(destAsset.copy(status = "ACTIVE"))
            }
        }

        val fromName = fromAssetId?.let { allAssets[it]?.name } ?: "External / Bank"
        val toName = toAssetId?.let { allAssets[it]?.name } ?: "External / Cash"
        return "Recorded ₹${Math.round(amount)} transfer: $fromName ➔ $toName (Organic returns preserved)"
    }

    // Monthly Update helper: Copy previous month values to a target month
    suspend fun copyPreviousMonthValues(targetSnapshotId: String): Int {
        val allSnapshots = portfolioDao.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
        val currentIndex = allSnapshots.indexOfFirst { it.id == targetSnapshotId }
        val prevSnapshotId = if (currentIndex > 0) {
            allSnapshots[currentIndex - 1].id
        } else {
            allSnapshots.lastOrNull { it.id < targetSnapshotId }?.id
        } ?: return 0

        val prevValues = portfolioDao.getValuesBySnapshotList(prevSnapshotId)
        val activeAssets = portfolioDao.getAllAssetsList().filter { it.status == "ACTIVE" }.map { it.id }.toSet()

        val newValues = prevValues
            .filter { activeAssets.contains(it.assetId) }
            .map {
                AssetMonthlyValueEntity(
                    assetId = it.assetId,
                    snapshotId = targetSnapshotId,
                    value = it.value,
                    investedAmount = it.investedAmount,
                    contributions = 0.0,
                    withdrawals = 0.0,
                    notes = "Copied from ${formatSnapshotLabel(prevSnapshotId)}"
                )
            }
        portfolioDao.insertAssetValues(newValues)
        return newValues.size
    }

    // Backup & Restore helpers for Loans and Insurances
    suspend fun getAllLoansList(): List<LoanEntity> = db.loanDao().getAllLoansList()
    suspend fun insertLoans(loans: List<LoanEntity>) = db.loanDao().insertLoans(loans)
    suspend fun getAllInsurancePoliciesList(): List<InsuranceEntity> = db.insuranceDao().getAllPoliciesList()
    suspend fun insertInsurancePolicies(policies: List<InsuranceEntity>) = db.insuranceDao().insertPolicies(policies)

    // Backup & Restore helpers for Credit Cards and Cashback/Refunds
    suspend fun getAllCreditCardsList(): List<CreditCardEntity> = db.creditCardDao().getAllCardsList()
    suspend fun insertCreditCards(cards: List<CreditCardEntity>) = db.creditCardDao().insertCards(cards)
    suspend fun getAllCreditCardStatementsList(): List<CreditCardStatementEntity> = db.creditCardDao().getAllStatementsList()
    suspend fun insertCreditCardStatements(stmts: List<CreditCardStatementEntity>) = db.creditCardDao().insertStatements(stmts)
    suspend fun getAllCashbackRefundsList(): List<CashbackRefundEntity> = db.cashbackRefundDao().getAllItemsList()
    suspend fun insertCashbackRefunds(items: List<CashbackRefundEntity>) = db.cashbackRefundDao().insertItems(items)
}
