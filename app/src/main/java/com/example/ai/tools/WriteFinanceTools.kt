package com.example.ai.tools

import com.example.ai.Source
import com.example.ai.SourceCategory
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.AssetMonthlyValueEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MonthlySnapshotEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.data.repository.PortfolioRepository
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val inrFormatter = DecimalFormat("#,##,###.##")

private fun formatInr(amount: Double): String {
    return "₹" + inrFormatter.format(amount)
}

/**
 * add_expense (Write Tool - Requires Explicit User Confirmation)
 */
class AddExpenseTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "add_expense"
    override val description: String = "Records a new expense item into local Room database. Requires explicit user confirmation."
    override val permission: ToolPermission = ToolPermission.WRITE_FINANCIAL_DATA
    override val isWriteOperation: Boolean = true
    override val parameterSchema: Map<String, String> = mapOf(
        "amount" to "Expense amount in INR (e.g. 1500)",
        "category" to "Category name (e.g. 'Food & Dining', 'Groceries', 'Utilities')",
        "title" to "Short description/title of the expense (e.g. 'Dinner at Social')",
        "date" to "Optional date in YYYY-MM-DD format (defaults to today)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val amount = arguments["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
        val categoryName = arguments["category"]?.toString()?.trim() ?: "General"
        val title = arguments["title"]?.toString()?.trim() ?: "Expense"
        val dateStr = arguments["date"]?.toString()?.trim() ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        if (amount <= 0.0) {
            return ToolResult.error("Expense amount must be greater than 0.")
        }

        val snapshotId = dateStr.take(7) // "YYYY-MM"

        val preview = mapOf(
            "Operation" to "Add Expense",
            "Category" to categoryName,
            "Title" to title,
            "Amount" to formatInr(amount),
            "Date" to dateStr,
            "Snapshot" to snapshotId
        )

        return ToolResult.success(
            data = mapOf(
                "action" to "add_expense",
                "amount" to amount,
                "category" to categoryName,
                "title" to title,
                "date" to dateStr,
                "snapshotId" to snapshotId,
                "preview" to preview
            ),
            summaryText = "Action Prepared: Add Expense of ${formatInr(amount)} for '$title' ($categoryName). Explicit user confirmation required.",
            source = Source(
                title = "Local Expense Database (Write Action)",
                snippet = "Pending User Confirmation",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }

    suspend fun performCommit(arguments: Map<String, Any?>): Long {
        val amount = arguments["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
        val categoryName = arguments["category"]?.toString()?.trim() ?: "General"
        val title = arguments["title"]?.toString()?.trim() ?: "Expense"
        val dateStr = arguments["date"]?.toString()?.trim() ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val snapshotId = dateStr.take(7)

        val categories = portfolioRepository.getAllExpenseCategoriesList()
        val matchedCat = categories.find { it.name.equals(categoryName, ignoreCase = true) || it.id.equals(categoryName, ignoreCase = true) }
            ?: categories.firstOrNull()

        val categoryId = matchedCat?.id ?: "food"

        // Ensure snapshot exists
        val existingSnap = portfolioRepository.getSnapshotById(snapshotId)
        if (existingSnap == null) {
            val parts = snapshotId.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 8
            portfolioRepository.saveSnapshot(MonthlySnapshotEntity(id = snapshotId, year = y, month = m))
        }

        val expense = ExpenseEntity(
            snapshotId = snapshotId,
            expenseCategoryId = categoryId,
            amount = amount,
            title = title,
            dateMillis = System.currentTimeMillis()
        )

        return portfolioRepository.saveExpense(expense)
    }
}

/**
 * add_income (Write Tool - Requires Explicit User Confirmation)
 */
class AddIncomeTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "add_income"
    override val description: String = "Records a recurring or monthly income stream (e.g. Salary, Dividend, Bonus). Requires user confirmation."
    override val permission: ToolPermission = ToolPermission.WRITE_FINANCIAL_DATA
    override val isWriteOperation: Boolean = true
    override val parameterSchema: Map<String, String> = mapOf(
        "name" to "Income source name (e.g. 'Tech Corp Salary')",
        "amount" to "Monthly amount in INR (e.g. 150000)",
        "type" to "Income type: 'SALARY', 'MEAL_COUPON', 'OTHER'"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val name = arguments["name"]?.toString()?.trim() ?: "Salary"
        val amount = arguments["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
        val type = arguments["type"]?.toString()?.trim() ?: "SALARY"
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        if (amount <= 0.0) {
            return ToolResult.error("Income amount must be greater than 0.")
        }

        val preview = mapOf(
            "Operation" to "Add Income Stream",
            "Name" to name,
            "Type" to type,
            "Monthly Amount" to formatInr(amount),
            "Start Month" to currentMonth
        )

        return ToolResult.success(
            data = mapOf(
                "action" to "add_income",
                "name" to name,
                "amount" to amount,
                "type" to type,
                "startMonth" to currentMonth,
                "preview" to preview
            ),
            summaryText = "Action Prepared: Add recurring income stream '$name' of ${formatInr(amount)}/month ($type). Explicit user confirmation required.",
            source = Source(
                title = "Local Recurring Cash Flow Ledger (Write Action)",
                snippet = "Pending User Confirmation",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }

    suspend fun performCommit(arguments: Map<String, Any?>): Long {
        val name = arguments["name"]?.toString()?.trim() ?: "Salary"
        val amount = arguments["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
        val type = arguments["type"]?.toString()?.trim() ?: "SALARY"
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val flow = RecurringCashFlowEntity(
            name = name,
            type = type,
            categoryId = "salary_inflow",
            monthlyAmount = amount,
            startMonth = currentMonth,
            isActive = true
        )

        return portfolioRepository.saveRecurringCashFlow(flow)
    }
}

/**
 * add_holding (Write Tool - Requires Explicit User Confirmation)
 */
class AddHoldingTool(
    private val portfolioRepository: PortfolioRepository
) : FinanceTool {
    override val name: String = "add_holding"
    override val description: String = "Adds a new asset/holding into the portfolio. Requires explicit user confirmation."
    override val permission: ToolPermission = ToolPermission.WRITE_FINANCIAL_DATA
    override val isWriteOperation: Boolean = true
    override val parameterSchema: Map<String, String> = mapOf(
        "name" to "Asset name (e.g. 'Nifty 50 Index Fund')",
        "category_id" to "Category ID (e.g. 'mutual_funds', 'stocks', 'gold', 'crypto')",
        "current_value" to "Current valuation in INR",
        "invested_amount" to "Total principal invested amount in INR"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val name = arguments["name"]?.toString()?.trim() ?: "New Asset"
        val categoryId = arguments["category_id"]?.toString()?.trim() ?: "mutual_funds"
        val value = arguments["current_value"]?.toString()?.toDoubleOrNull() ?: 0.0
        val invested = arguments["invested_amount"]?.toString()?.toDoubleOrNull() ?: value
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val preview = mapOf(
            "Operation" to "Add Portfolio Asset",
            "Asset Name" to name,
            "Category" to categoryId,
            "Current Value" to formatInr(value),
            "Invested Amount" to formatInr(invested),
            "Snapshot" to currentMonth
        )

        return ToolResult.success(
            data = mapOf(
                "action" to "add_holding",
                "name" to name,
                "categoryId" to categoryId,
                "currentValue" to value,
                "investedAmount" to invested,
                "snapshotId" to currentMonth,
                "preview" to preview
            ),
            summaryText = "Action Prepared: Add asset '$name' ($categoryId) valued at ${formatInr(value)} (Invested: ${formatInr(invested)}). Explicit user confirmation required.",
            source = Source(
                title = "Local Portfolio Holdings (Write Action)",
                snippet = "Pending User Confirmation",
                category = SourceCategory.LOCAL_ROOM_DATA
            )
        )
    }

    suspend fun performCommit(arguments: Map<String, Any?>): Long {
        val name = arguments["name"]?.toString()?.trim() ?: "New Asset"
        val categoryId = arguments["category_id"]?.toString()?.trim() ?: "mutual_funds"
        val value = arguments["current_value"]?.toString()?.toDoubleOrNull() ?: 0.0
        val invested = arguments["invested_amount"]?.toString()?.toDoubleOrNull() ?: value
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        val asset = AssetEntity(
            categoryId = categoryId,
            name = name,
            initialInvestedAmount = invested,
            status = "ACTIVE"
        )
        val assetId = portfolioRepository.saveAsset(asset)

        val parts = currentMonth.split("-")
        val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 8
        portfolioRepository.saveSnapshot(MonthlySnapshotEntity(id = currentMonth, year = y, month = m))

        portfolioRepository.saveAssetValue(
            AssetMonthlyValueEntity(
                assetId = assetId,
                snapshotId = currentMonth,
                value = value,
                investedAmount = invested
            )
        )

        return assetId
    }
}
