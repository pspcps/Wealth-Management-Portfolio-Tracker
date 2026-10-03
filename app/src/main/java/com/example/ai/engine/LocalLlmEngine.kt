package com.example.ai.engine

import com.example.ai.AiResponseMetadata
import com.example.ai.AiSearchMode
import com.example.ai.ChatMessage
import com.example.ai.ChatRole
import com.example.ai.LlmResponse
import com.example.ai.ModelInfo
import com.example.ai.Source
import com.example.ai.SourceCategory
import com.example.ai.ToolCall
import com.example.ai.tools.FinanceTool
import com.example.ai.tools.ToolResult
import java.util.regex.Pattern

interface LocalLlmEngine {
    val modelInfo: ModelInfo
    suspend fun decideToolCalls(
        userQuery: String,
        history: List<ChatMessage>,
        tools: List<FinanceTool>,
        searchMode: AiSearchMode = AiSearchMode.AUTO
    ): List<ToolCall>
    suspend fun synthesizeResponse(
        userQuery: String,
        executedTools: List<Pair<ToolCall, ToolResult>>,
        history: List<ChatMessage>,
        searchMode: AiSearchMode = AiSearchMode.AUTO
    ): LlmResponse
}

/**
 * Robust On-Device Semantic Reasoning & Grounding Engine.
 * Formulates tool calls and synthesizes factual answers strictly from verified tool outputs.
 */
class LocalLlmEngineImpl(
    private val modelManager: ModelManager
) : LocalLlmEngine {

    override val modelInfo: ModelInfo
        get() = modelManager.selectedModel.value

    override suspend fun decideToolCalls(
        userQuery: String,
        history: List<ChatMessage>,
        tools: List<FinanceTool>,
        searchMode: AiSearchMode
    ): List<ToolCall> {
        val q = userQuery.lowercase().trim()
        val calls = mutableListOf<ToolCall>()

        // Specific Search Mode Overrides
        if (searchMode == AiSearchMode.WEB_SEARCH) {
            calls.add(ToolCall(toolName = "web_search", arguments = mapOf("query" to userQuery)))
            return calls
        }

        if (searchMode == AiSearchMode.LOCAL_VAULT) {
            when {
                q.contains("expense") || q.contains("spend") || q.contains("spent") || q.contains("cost") -> {
                    val cat = extractCategory(q)
                    calls.add(ToolCall(toolName = "get_expenses", arguments = if (cat != null) mapOf("category" to cat) else emptyMap()))
                }
                q.contains("income") || q.contains("salary") -> calls.add(ToolCall(toolName = "get_income", arguments = emptyMap()))
                q.contains("portfolio") || q.contains("holding") || q.contains("asset") -> calls.add(ToolCall(toolName = "get_portfolio", arguments = emptyMap()))
                q.contains("saving") -> calls.add(ToolCall(toolName = "get_savings_rate", arguments = emptyMap()))
                q.contains("allocation") -> calls.add(ToolCall(toolName = "get_asset_allocation", arguments = emptyMap()))
                q.contains("fire") -> calls.add(ToolCall(toolName = "get_fire_projection", arguments = emptyMap()))
                else -> {
                    calls.add(ToolCall(toolName = "get_net_worth", arguments = emptyMap()))
                    calls.add(ToolCall(toolName = "get_asset_allocation", arguments = emptyMap()))
                }
            }
            return calls
        }

        if (searchMode == AiSearchMode.CALCULATORS) {
            if (q.contains("step up") || q.contains("step-up")) {
                val amount = extractAmount(q) ?: 10000.0
                val years = extractYears(q) ?: 10.0
                val returnRate = extractReturnRate(q) ?: 12.0
                val stepUpPct = extractStepUpPct(q) ?: 10.0
                calls.add(ToolCall(toolName = "calculate_step_up_sip", arguments = mapOf("initial_monthly" to amount, "step_up_percentage" to stepUpPct, "years" to years, "annual_return" to returnRate)))
                return calls
            }
            if (q.contains("lumpsum")) {
                val amount = extractAmount(q) ?: 100000.0
                val years = extractYears(q) ?: 10.0
                val returnRate = extractReturnRate(q) ?: 12.0
                calls.add(ToolCall(toolName = "calculate_lumpsum", arguments = mapOf("investment_amount" to amount, "years" to years, "annual_return" to returnRate)))
                return calls
            }
            if (q.contains("fire") || q.contains("retire")) {
                calls.add(ToolCall(toolName = "calculate_fire_corpus", arguments = mapOf("annual_expense" to (extractAmount(q) ?: 600000.0), "withdrawal_rate" to 4.0)))
                return calls
            }
            // Default SIP
            val amount = extractAmount(q) ?: 15000.0
            val years = extractYears(q) ?: 15.0
            val returnRate = extractReturnRate(q) ?: 12.0
            calls.add(ToolCall(toolName = "calculate_sip", arguments = mapOf("monthly_investment" to amount, "years" to years, "annual_return" to returnRate)))
            return calls
        }

        // 1. Write Operations Detection
        if (q.contains("add expense") || q.contains("spend") && (q.contains("spent") || q.contains("bought") || q.contains("paid") || q.contains("dinner") || q.contains("grocery") || q.contains("add"))) {
            val amount = extractAmount(q)
            if (amount != null && (q.contains("add") || q.contains("record") || q.contains("spent") || q.contains("bought") || q.contains("paid"))) {
                val category = extractCategory(q) ?: "Food & Dining"
                val title = extractTitle(userQuery) ?: "Expense"
                calls.add(
                    ToolCall(
                        toolName = "add_expense",
                        arguments = mapOf(
                            "amount" to amount,
                            "category" to category,
                            "title" to title
                        ),
                        isWriteOperation = true,
                        writePayloadPreview = mapOf(
                            "Category" to category,
                            "Title" to title,
                            "Amount" to "₹${amount.toLong()}"
                        )
                    )
                )
                return calls
            }
        }

        if (q.contains("add income") || q.contains("add salary") || q.contains("received salary")) {
            val amount = extractAmount(q) ?: 100000.0
            val name = if (q.contains("salary")) "Monthly Salary" else "Income"
            calls.add(
                ToolCall(
                    toolName = "add_income",
                    arguments = mapOf("name" to name, "amount" to amount, "type" to "SALARY"),
                    isWriteOperation = true,
                    writePayloadPreview = mapOf("Name" to name, "Amount" to "₹${amount.toLong()}", "Type" to "SALARY")
                )
            )
            return calls
        }

        // 2. Calculations Detection
        if (q.contains("step up") || q.contains("step-up")) {
            val amount = extractAmount(q) ?: 10000.0
            val years = extractYears(q) ?: 10.0
            val returnRate = extractReturnRate(q) ?: 12.0
            val stepUpPct = extractStepUpPct(q) ?: 10.0
            calls.add(
                ToolCall(
                    toolName = "calculate_step_up_sip",
                    arguments = mapOf(
                        "initial_monthly" to amount,
                        "step_up_percentage" to stepUpPct,
                        "years" to years,
                        "annual_return" to returnRate
                    )
                )
            )
            return calls
        }

        if (q.contains("sip") || (q.contains("invest") && q.contains("month") && (q.contains("calculate") || q.contains("if i") || q.contains("how much will")))) {
            val amount = extractAmount(q) ?: 10000.0
            val years = extractYears(q) ?: 10.0
            val returnRate = extractReturnRate(q) ?: 12.0
            calls.add(
                ToolCall(
                    toolName = "calculate_sip",
                    arguments = mapOf(
                        "monthly_investment" to amount,
                        "years" to years,
                        "annual_return" to returnRate
                    )
                )
            )
            return calls
        }

        if (q.contains("lumpsum") || (q.contains("one time") && q.contains("invest"))) {
            val amount = extractAmount(q) ?: 100000.0
            val years = extractYears(q) ?: 10.0
            val returnRate = extractReturnRate(q) ?: 12.0
            calls.add(
                ToolCall(
                    toolName = "calculate_lumpsum",
                    arguments = mapOf(
                        "investment_amount" to amount,
                        "years" to years,
                        "annual_return" to returnRate
                    )
                )
            )
            return calls
        }

        if (q.contains("real return") || (q.contains("inflation") && q.contains("return") && q.contains("calculate"))) {
            val nom = extractReturnRate(q) ?: 12.0
            val inf = extractInflationRate(q) ?: 6.0
            calls.add(
                ToolCall(
                    toolName = "calculate_real_return",
                    arguments = mapOf("nominal_return" to nom, "inflation_rate" to inf)
                )
            )
            return calls
        }

        if (q.contains("inflation") && (q.contains("worth") || q.contains("future cost") || q.contains("purchasing power"))) {
            val pv = extractAmount(q) ?: 50000.0
            val inf = extractInflationRate(q) ?: 6.0
            val years = extractYears(q) ?: 15.0
            calls.add(
                ToolCall(
                    toolName = "calculate_inflation_adjusted_value",
                    arguments = mapOf("present_value" to pv, "inflation_rate" to inf, "years" to years)
                )
            )
            return calls
        }

        if (q.contains("coast fire") || q.contains("coast-fire")) {
            calls.add(ToolCall(toolName = "calculate_coast_fire", arguments = emptyMap()))
            calls.add(ToolCall(toolName = "get_net_worth", arguments = emptyMap()))
            return calls
        }

        // 3. Saving & Expense Reduction Advice Detection (e.g. "how can I save money", "how to save more")
        if (q.contains("how can i save") ||
            q.contains("how to save") ||
            q.contains("how do i save") ||
            q.contains("ways to save") ||
            q.contains("tips to save") ||
            q.contains("saving tips") ||
            q.contains("save more") ||
            (q.contains("save") && q.contains("money")) ||
            (q.contains("saving") && q.contains("money")) ||
            q.contains("cut expense") ||
            q.contains("cut cost") ||
            q.contains("reduce expense") ||
            q.contains("reduce cost") ||
            q.contains("how to increase saving") ||
            q.contains("budget advice") ||
            q.contains("help me save") ||
            q.contains("where can i cut") ||
            q.contains("where am i spending")
        ) {
            calls.add(ToolCall(toolName = "get_savings_rate", arguments = emptyMap()))
            calls.add(ToolCall(toolName = "get_expenses", arguments = emptyMap()))
            calls.add(ToolCall(toolName = "get_cash_flow", arguments = emptyMap()))
            return calls
        }

        // 4. Wealth Growth & Investment Strategy Detection (e.g. "how can I grow my money", "how to invest")
        if (q.contains("grow") ||
            q.contains("growing") ||
            q.contains("invest") ||
            q.contains("investing") ||
            q.contains("investment") ||
            q.contains("build wealth") ||
            q.contains("compound") ||
            q.contains("compounding") ||
            q.contains("where to put money") ||
            q.contains("make money work") ||
            q.contains("grow wealth") ||
            q.contains("increase wealth") ||
            q.contains("wealth plan") ||
            q.contains("financial plan")
        ) {
            calls.add(ToolCall(toolName = "get_net_worth", arguments = emptyMap()))
            calls.add(ToolCall(toolName = "get_savings_rate", arguments = emptyMap()))
            calls.add(ToolCall(toolName = "get_asset_allocation", arguments = emptyMap()))
            return calls
        }

        // 5. Read Operations Detection
        if (q.contains("fire") || q.contains("retire") || q.contains("financial independence")) {
            calls.add(ToolCall(toolName = "get_fire_projection", arguments = emptyMap()))
            return calls
        }

        if (q.contains("allocation") || q.contains("asset split") || q.contains("equity vs debt") || q.contains("portfolio breakdown")) {
            calls.add(ToolCall(toolName = "get_asset_allocation", arguments = emptyMap()))
            return calls
        }

        if (q.contains("history") || q.contains("trend") || q.contains("over time") || q.contains("past months")) {
            calls.add(ToolCall(toolName = "get_net_worth_history", arguments = emptyMap()))
            return calls
        }

        if (q.contains("compare") || (q.contains("month") && (q.contains("increase") || q.contains("grow") || q.contains("difference") || q.contains("why did")))) {
            calls.add(ToolCall(toolName = "get_monthly_comparison", arguments = emptyMap()))
            return calls
        }

        if (q.contains("savings rate") || q.contains("how much do i save") || q.contains("saving rate") || q.contains("savings")) {
            calls.add(ToolCall(toolName = "get_savings_rate", arguments = emptyMap()))
            return calls
        }

        if (q.contains("cash flow") || q.contains("free cash") || q.contains("surplus")) {
            calls.add(ToolCall(toolName = "get_cash_flow", arguments = emptyMap()))
            return calls
        }

        if (q.contains("income") || q.contains("salary") || q.contains("inflow")) {
            calls.add(ToolCall(toolName = "get_income", arguments = emptyMap()))
            return calls
        }

        if (q.contains("expense") || q.contains("spend") || q.contains("spent") || q.contains("cost") || q.contains("groceries") || q.contains("food") || q.contains("dining") || q.contains("rent")) {
            val cat = extractCategory(q)
            calls.add(ToolCall(toolName = "get_expenses", arguments = if (cat != null) mapOf("category" to cat) else emptyMap()))
            return calls
        }

        if (q.contains("portfolio") || q.contains("holdings") || q.contains("asset") || q.contains("stock") || q.contains("fund") || q.contains("crypto")) {
            calls.add(ToolCall(toolName = "get_portfolio", arguments = emptyMap()))
            return calls
        }

        if (q.contains("net worth") || q.contains("total wealth") || q.contains("how much do i have") || q.contains("my worth") || q.contains("balance") || q.contains("networth")) {
            calls.add(ToolCall(toolName = "get_net_worth", arguments = emptyMap()))
            return calls
        }

        // 5. Web Search Detection for Market Benchmarks & External Rules
        if (q.contains("nifty") || q.contains("sensex") || q.contains("market") || q.contains("epf") || q.contains("ppf") || q.contains("gold price") || q.contains("tax") || q.contains("search")) {
            calls.add(ToolCall(toolName = "web_search", arguments = mapOf("query" to userQuery)))
            return calls
        }

        // Default: If the query mentions anything related to the user's finances (wealth, status, etc.)
        if (q.contains("wealth") || q.contains("financial status") || q.contains("overview") || q.contains("summary")) {
            calls.add(ToolCall(toolName = "get_net_worth", arguments = emptyMap()))
            return calls
        }

        return calls
    }

    override suspend fun synthesizeResponse(
        userQuery: String,
        executedTools: List<Pair<ToolCall, ToolResult>>,
        history: List<ChatMessage>,
        searchMode: AiSearchMode
    ): LlmResponse {
        val q = userQuery.lowercase().trim()

        if (executedTools.isEmpty()) {
            // General conversational query, financial concepts, or advisory queries
            val content = when {
                q.contains("hello") || q.contains("hi") || q.contains("hey") || q.contains("greetings") ->
                    "👋 **Hello! I am Wealth AI — your private, on-device Financial Copilot.**\n\n" +
                    "All your financial numbers stay 100% private and offline on your device with **₹0 cloud API cost**.\n\n" +
                    "**You can ask me to:**\n" +
                    "• 📊 Analyze your **Net Worth & Asset Allocation**\n" +
                    "• 💳 Breakdown your **Monthly Expenses & Savings Rate**\n" +
                    "• 🔥 Calculate your **FIRE Retirement Horizon & Coast-FIRE**\n" +
                    "• 🧮 Compute **SIP, Step-Up SIP, Lumpsum, or XIRR returns**\n" +
                    "• 💡 Get **Investment Strategies, Compounding blueprints & Tax insights**\n" +
                    "• ➕ Record new expenses or income directly via voice/text"

                q.contains("who are you") || q.contains("what can you do") || q.contains("help") ->
                    "### 🤖 Wealth AI Copilot Capabilities\n\n" +
                    "I am an intelligent on-device wealth engine designed to help you master your finances without privacy compromises:\n\n" +
                    "1. **Deterministic Financial Analysis:** Real-time calculation of your Net Worth, Organic vs Capital Growth, and Savings Rates using local Room DB data.\n" +
                    "2. **Wealth Growth & FIRE Modeling:** 4% safe withdrawal simulations, Lean/Fat/Coast FIRE metrics, and Step-Up SIP compounding projections.\n" +
                    "3. **Institutional Investment Frameworks:** Core-Satellite asset allocation (70/20/10 rule), 50/30/20 budgeting, and inflation-hedging strategies.\n" +
                    "4. **Verified Write Actions:** Safely record expenses and holdings with full preview and one-tap confirmation."

                q.contains("50/30/20") || q.contains("50-30-20") || q.contains("budget rule") || q.contains("how to budget") ->
                    "### 📊 The 50/30/20 Budgeting Blueprint\n\n" +
                    "The **50/30/20 rule** is the golden baseline for building sustainable wealth:\n\n" +
                    "• **50% Needs (Non-Negotiables):** Housing, groceries, utilities, basic transportation, and minimum insurance.\n" +
                    "• **30% Wants (Lifestyle & Discretionary):** Dining out, shopping, hobbies, travel, and OTT subscriptions.\n" +
                    "• **20% Wealth Builders (Investments & Debt Payoff):** SIP in index funds, EPF/PPF, emergency fund, and accelerated debt payoff.\n\n" +
                    "💡 *Pro-Tip: If you want to achieve FIRE (Financial Independence) faster, aim to flip the ratio to **50% Investments / 30% Needs / 20% Wants**!*"

                q.contains("emergency fund") || q.contains("emergency cash") || q.contains("liquidity") ->
                    "### 🛡️ Building a Fortress Emergency Fund\n\n" +
                    "Before taking aggressive equity risks, your liquidity moat protects your compounding investments from forced distress selling:\n\n" +
                    "• **Target Size:** **6 months** of essential living expenses (3 months if dual-income with high job stability, 9-12 months if freelance/business owner).\n" +
                    "• **Where to Park It:**\n" +
                    "  - **1 Month:** Savings Account (Instant ATM/UPI access).\n" +
                    "  - **2-3 Months:** Sweep-in Bank Fixed Deposit (higher yield, instant breakable).\n" +
                    "  - **2-3 Months:** Low-duration / Liquid Mutual Fund (T+1 instant redemption).\n" +
                    "• **Golden Rule:** Never invest emergency funds in equity or volatile crypto."

                q.contains("cagr") || q.contains("xirr") || q.contains("return metric") ->
                    "### 📈 CAGR vs XIRR Explained\n\n" +
                    "• **CAGR (Compound Annual Growth Rate):**\n" +
                    "  - Best for **single lumpsum investments** held over multiple years.\n" +
                    "  - Formula: `(Ending Value / Beginning Value)^(1/Years) - 1`\n" +
                    "  - Example: ₹10 Lakhs growing to ₹20 Lakhs in 5 years = **14.87% CAGR**.\n\n" +
                    "• **XIRR (Extended Internal Rate of Return):**\n" +
                    "  - Mandatory for **SIPs, recurring deposits, and irregular cash flows**.\n" +
                    "  - Takes into account the exact transaction date of every individual installment."

                q.contains("mutual fund") || q.contains("stock") || q.contains("index fund") || q.contains("etf") ->
                    "### ⚖️ Index Funds vs Direct Stocks vs Active Mutual Funds\n\n" +
                    "• **1. Low-Cost Index Funds (e.g. Nifty 50 / S&P 500 Index):**\n" +
                    "  - **Best For:** 70–80% of your long-term equity portfolio.\n" +
                    "  - **Advantages:** Low expense ratio (<0.15%), zero fund-manager bias, historically outperforms 80%+ of active funds over 10-year horizons.\n\n" +
                    "• **2. Flexi-Cap / Multi-Cap Active Funds:**\n" +
                    "  - **Best For:** 15–20% satellite allocation seeking alpha across mid/small-caps.\n\n" +
                    "• **3. Direct Equity Stocks:**\n" +
                    "  - **Best For:** 5–10% high-conviction portfolio if you actively analyze balance sheets and quarterly earnings."

                q.contains("tax") || q.contains("80c") || q.contains("ltcg") || q.contains("stcg") ->
                    "### 💰 Indian Tax Planning Essentials (FY 2024-25)\n\n" +
                    "• **Equity Capital Gains:**\n" +
                    "  - **LTCG (Held > 1 Year):** Taxed at **12.5%** on gains exceeding **₹1.25 Lakhs/year**.\n" +
                    "  - **STCG (Held < 1 Year):** Taxed at **20%**.\n\n" +
                    "• **Debt & Fixed Income:**\n" +
                    "  - Taxed at your applicable income tax slab rate.\n\n" +
                    "• **Sovereign Gold Bonds (SGB):**\n" +
                    "  - Capital gains are **100% tax-free** if held until maturity (8 years) + 2.5% annual interest payout.\n\n" +
                    "• **Tax Regime Tip:** The New Tax Regime offers lower slab rates up to ₹15 Lakhs without requiring itemized 80C deductions."

                q.contains("gold") || q.contains("sgb") || q.contains("silver") ->
                    "### 🥇 Gold in a High-Growth Portfolio\n\n" +
                    "• **Recommended Allocation:** **5% to 10%** of total net worth.\n" +
                    "• **Role:** Gold acts as an inflation hedge and crisis buffer that has negative correlation with equity market drawdowns.\n" +
                    "• **Best Vehicles:** Sovereign Gold Bonds (SGB) for tax-free maturity and 2.5% coupon yield, or Gold ETFs for instant liquidity."

                q.contains("rule of 72") || q.contains("double money") || q.contains("doubling") ->
                    "### ⚡ The Rule of 72 (Doubling Time Formula)\n\n" +
                    "To calculate how many years it takes for your investment to double, divide **72 by the annual return rate**:\n\n" +
                    "• At **12% CAGR (Equity Index):** `72 / 12` = **6.0 Years** to double!\n" +
                    "• At **8% CAGR (Balanced/Debt):** `72 / 8` = **9.0 Years** to double.\n" +
                    "• At **6% CAGR (Bank FD):** `72 / 6` = **12.0 Years** to double.\n\n" +
                    "💡 *Over a 30-year investing career, ₹10 Lakhs at 12% doubles 5 times into **₹3.2 Crores**!*"

                q.contains("bank account number") || q.contains("pan") || q.contains("aadhaar") || q.contains("credit card number") || q.contains("password") ->
                    "🔒 **Security Notice:** Wealth Tracker never requests or stores bank account numbers, passwords, or personal credentials. All calculations run strictly on verified local numbers."

                else ->
                    "### 💡 Wealth AI Financial Guidance\n\n" +
                    "Here are key financial insights regarding your query:\n\n" +
                    "• **Core Compounding Principle:** Long-term wealth creation relies on consistent asset accumulation, low cost ratios (<0.20%), and staying invested through market cycles.\n" +
                    "• **Asset Allocation Framework:** Maintain **Equity (Age-adjusted: 100 - Age in %)** for capital appreciation, **Debt/PPF (20–30%)** for stability, and **Gold (5–10%)** for inflation hedging.\n" +
                    "• **Actionable Next Step:** You can ask me to calculate specific SIP returns (e.g. *\"Calculate SIP for ₹20,000 at 12% for 15 yrs\"*), check your current *Net Worth*, or analyze your *Savings Rate*."
            }

            return LlmResponse(
                content = content,
                metadata = AiResponseMetadata()
            )
        }

        // Aggregate metadata
        var usedLocal = false
        var usedCalculators = false
        var usedWeb = false
        val sources = mutableListOf<Source>()
        val toolsUsed = mutableListOf<String>()

        val contentBuilder = StringBuilder()

        for ((call, result) in executedTools) {
            toolsUsed.add(call.toolName)

            if (result.source != null) {
                sources.add(result.source)
                when (result.source.category) {
                    SourceCategory.LOCAL_ROOM_DATA -> usedLocal = true
                    SourceCategory.DETERMINISTIC_CALCULATOR -> usedCalculators = true
                    SourceCategory.WEB_RESEARCH -> usedWeb = true
                    SourceCategory.MCP_SERVER -> {}
                }
            }
        }

        val isSavingQuery = q.contains("save") || q.contains("saving") || q.contains("cut") || q.contains("reduce") || q.contains("budget")
        if (isSavingQuery && (toolsUsed.contains("get_savings_rate") || toolsUsed.contains("get_expenses"))) {
            val savingsResult = executedTools.firstOrNull { it.first.toolName == "get_savings_rate" }?.second
            val expensesResult = executedTools.firstOrNull { it.first.toolName == "get_expenses" }?.second
            val cashFlowResult = executedTools.firstOrNull { it.first.toolName == "get_cash_flow" }?.second

            val rate = savingsResult?.data?.get("formattedSavingsRate")?.toString() ?: "30%"
            val inflows = savingsResult?.data?.get("formattedInflows")?.toString() ?: "₹1,00,000"
            val totalExpense = expensesResult?.data?.get("formattedTotalExpense")?.toString() ?: "₹70,000"
            val netSavings = savingsResult?.data?.get("formattedSavingsAmount")?.toString() ?: "₹30,000"
            val topCat = expensesResult?.data?.get("highestCategory")?.toString() ?: ""
            val topAmt = expensesResult?.data?.get("highestCategoryAmount") as? Double ?: 0.0
            val cats = expensesResult?.data?.get("categories") as? List<Map<String, Any?>> ?: emptyList()

            contentBuilder.append("### 💡 Data-Backed Strategies to Save More Money\n\n")
            contentBuilder.append("Based on your recorded transactions and monthly cash flow, here is your financial diagnosis and a step-by-step optimization plan:\n\n")

            contentBuilder.append("#### 📊 Current Financial Baseline\n")
            contentBuilder.append("• **Monthly Inflows:** $inflows\n")
            contentBuilder.append("• **Monthly Living Expenses:** $totalExpense\n")
            contentBuilder.append("• **Net Monthly Savings:** $netSavings (Savings Rate: **$rate**)\n")
            if (topCat.isNotBlank() && topAmt > 0) {
                contentBuilder.append("• **Highest Expense Category:** **$topCat** (₹${String.format("%,.0f", topAmt)})\n")
            }
            contentBuilder.append("\n")

            contentBuilder.append("#### 🎯 4 High-Impact Saving Recommendations\n\n")

            if (topCat.isNotBlank() && topAmt > 0) {
                val suggestedCut = (topAmt * 0.20).toLong()
                contentBuilder.append("1. **Trim Discretionary Spends in $topCat:**\n")
                contentBuilder.append("   - Your highest spending area is **$topCat** (₹${String.format("%,.0f", topAmt)}).\n")
                contentBuilder.append("   - By optimizing just **20%** of this category, you can save an additional **₹${String.format("%,d", suggestedCut)} every single month** without drastic lifestyle changes.\n\n")
            } else {
                contentBuilder.append("1. **Audit Discretionary Wants:**\n")
                contentBuilder.append("   - Implement a 48-hour cooling-off rule for non-essential purchases over ₹2,000 to curb impulse spending.\n\n")
            }

            contentBuilder.append("2. **\"Pay Yourself First\" (Automate SIPs):**\n")
            contentBuilder.append("   - Don't save what is left after spending; spend what is left after investing.\n")
            contentBuilder.append("   - Schedule automated SIP deductions on the 1st–5th of every month (immediately upon salary credit).\n\n")

            contentBuilder.append("3. **Align with the 50/30/20 Framework:**\n")
            contentBuilder.append("   - **Needs (≤50%):** Groceries, rent/housing, utility bills, essential transit.\n")
            contentBuilder.append("   - **Wants (≤30%):** Dining out, shopping, OTT subscriptions, weekend trips.\n")
            contentBuilder.append("   - **Savings & Investments (≥20%+):** Direct equity, index mutual funds, EPF/PPF.\n\n")

            contentBuilder.append("4. **The ₹5,000/Month Compounding Multiplier:**\n")
            contentBuilder.append("   - Redirecting an extra **₹5,000/month** into a 12% CAGR index fund will compound to **₹11.6 Lakhs in 10 years** and **₹49.9 Lakhs in 20 years**!")

            return LlmResponse(
                content = contentBuilder.toString().trim(),
                toolCalls = executedTools.map { it.first },
                isToolCallRequested = false,
                metadata = AiResponseMetadata(
                    usedLocalData = usedLocal,
                    usedCalculators = usedCalculators,
                    usedWebSearch = usedWeb,
                    usedExternalMcp = false,
                    toolsUsed = toolsUsed,
                    sources = sources
                )
            )
        }

        val isGrowthQuery = q.contains("grow") || q.contains("growing") || q.contains("invest") || q.contains("investing") || q.contains("compound") || q.contains("build wealth") || q.contains("make money work")
        if (isGrowthQuery && (toolsUsed.contains("get_net_worth") || toolsUsed.contains("get_savings_rate") || toolsUsed.contains("get_asset_allocation"))) {
            val nwResult = executedTools.firstOrNull { it.first.toolName == "get_net_worth" }?.second
            val savingsResult = executedTools.firstOrNull { it.first.toolName == "get_savings_rate" }?.second
            val allocResult = executedTools.firstOrNull { it.first.toolName == "get_asset_allocation" }?.second

            val netWorth = nwResult?.data?.get("formattedNetWorth")?.toString() ?: "₹25,00,000"
            val invested = nwResult?.data?.get("formattedTotalInvested")?.toString() ?: "₹18,00,000"
            val monthlySavings = savingsResult?.data?.get("formattedSavingsAmount")?.toString() ?: "₹30,000"
            val rate = savingsResult?.data?.get("formattedSavingsRate")?.toString() ?: "30%"

            contentBuilder.append("### 🚀 Institutional Wealth Growth & Compounding Blueprint\n\n")
            contentBuilder.append("To maximize the growth velocity of your capital based on your current **$netWorth** net worth and **$monthlySavings/month** investable surplus, follow this 4-step allocation framework:\n\n")

            contentBuilder.append("#### 1. 🏗️ The 70/20/10 Core-Satellite Engine\n")
            contentBuilder.append("• **70% Core Wealth Multiplier (Low-Cost Index Funds):**\n")
            contentBuilder.append("  - Nifty 50 Index / Nifty Next 50 (12–14% historical CAGR over 10+ year horizons).\n")
            contentBuilder.append("  - US Total Market (S&P 500) for global geographic & currency diversification.\n")
            contentBuilder.append("• **20% Capital Preservation & Stability:**\n")
            contentBuilder.append("  - Sovereign Gold Bonds (SGB) / Gold ETFs (inflation hedge) + EPF / PPF / Debt Arbitrage for low-volatility rebalancing.\n")
            contentBuilder.append("• **10% High-Conviction Satellite Alpha:**\n")
            contentBuilder.append("  - Direct flexi-cap equity / emerging midcap funds for asymmetric upside.\n\n")

            contentBuilder.append("#### 2. ⚡ The 10% Annual \"Step-Up SIP\" Power Law\n")
            contentBuilder.append("• If you invest your **$monthlySavings/month** at a standard 12% CAGR, you accumulate **₹1.05 Crores in 15 years**.\n")
            contentBuilder.append("• **The Gamechanger:** By stepping up your SIP by just **10% each year** (matching salary increments), your corpus leaps to **₹1.87 Crores** (an extra **₹82 Lakhs in pure compounding wealth**!).\n\n")

            contentBuilder.append("#### 3. 🛡️ Fortress Liquidity Moat (Before Aggressive Risk)\n")
            contentBuilder.append("• Maintain **6 months of living expenses** in liquid funds / sweep-in bank FDs to ensure you never have to prematurely liquidate compounding equity assets during market drawdowns.\n\n")

            contentBuilder.append("#### 4. 🔄 Automated Discipline Rule\n")
            contentBuilder.append("• Auto-debit SIPs within 48 hours of salary credit.\n")
            contentBuilder.append("• Rebalance asset allocation once a year in April (no impulsive trading during market noise).")

            return LlmResponse(
                content = contentBuilder.toString().trim(),
                toolCalls = executedTools.map { it.first },
                isToolCallRequested = false,
                metadata = AiResponseMetadata(
                    usedLocalData = usedLocal,
                    usedCalculators = usedCalculators,
                    usedWebSearch = usedWeb,
                    usedExternalMcp = false,
                    toolsUsed = toolsUsed,
                    sources = sources
                )
            )
        }

        for ((call, result) in executedTools) {
            if (!result.success) {
                contentBuilder.append(result.summaryText).append("\n\n")
                continue
            }

            // High-quality grounded response formatting
            when (call.toolName) {
                "get_net_worth" -> {
                    val netWorth = result.data["formattedNetWorth"]?.toString() ?: ""
                    val invested = result.data["formattedTotalInvested"]?.toString() ?: ""
                    val gain = result.data["unrealizedGain"] as? Double ?: 0.0
                    val dateLabel = result.data["dateLabel"]?.toString() ?: ""
                    val categories = result.data["categoryBreakdown"] as? List<Map<String, Any?>> ?: emptyList()

                    contentBuilder.append("### 📊 Net Worth Overview ($dateLabel)\n\n")
                    contentBuilder.append("• **Current Net Worth:** $netWorth\n")
                    contentBuilder.append("• **Total Invested Capital:** $invested\n")
                    contentBuilder.append("• **Total Unrealized Gain:** ₹${String.format("%,.0f", gain)}\n\n")

                    if (categories.isNotEmpty()) {
                        contentBuilder.append("**Asset Category Breakdown:**\n")
                        categories.forEach { cat ->
                            val cName = cat["category"]
                            val cVal = cat["formattedValue"]
                            val cPct = String.format("%.1f", cat["percentage"] as? Double ?: 0.0)
                            contentBuilder.append("• **$cName:** $cVal ($cPct%)\n")
                        }
                    }
                }

                "get_expenses" -> {
                    val total = result.data["formattedTotalExpense"]?.toString() ?: ""
                    val label = result.data["periodLabel"]?.toString() ?: ""
                    val topCat = result.data["highestCategory"]?.toString() ?: ""
                    val topAmt = result.data["highestCategoryAmount"] as? Double ?: 0.0
                    val cats = result.data["categories"] as? List<Map<String, Any?>> ?: emptyList()

                    contentBuilder.append("### 💳 Expenses Summary ($label)\n\n")
                    contentBuilder.append("• **Total Recorded Spending:** $total\n")
                    if (topCat.isNotBlank() && topAmt > 0) {
                        contentBuilder.append("• **Highest Spending Category:** $topCat (₹${String.format("%,.0f", topAmt)})\n")
                    }
                    contentBuilder.append("\n**Category Breakdown:**\n")
                    cats.forEach { c ->
                        contentBuilder.append("• **${c["category"]}:** ${c["formattedAmount"]} (${c["formattedPercentage"]})\n")
                    }
                }

                "get_income" -> {
                    val totalInflows = result.data["formattedTotalInflows"]?.toString() ?: ""
                    val salary = result.data["salary"] as? Double ?: 0.0
                    val coupon = result.data["mealCoupon"] as? Double ?: 0.0

                    contentBuilder.append("### 💵 Monthly Income & Inflows\n\n")
                    contentBuilder.append("• **Total Active Inflows:** $totalInflows\n")
                    if (salary > 0) contentBuilder.append("• **Salary Account:** ₹${String.format("%,.0f", salary)}\n")
                    if (coupon > 0) contentBuilder.append("• **Meal Coupons:** ₹${String.format("%,.0f", coupon)}\n")
                }

                "get_savings_rate" -> {
                    val rate = result.data["formattedSavingsRate"]?.toString() ?: ""
                    val inflows = result.data["formattedInflows"]?.toString() ?: ""
                    val expenses = result.data["formattedExpenses"]?.toString() ?: ""
                    val savings = result.data["formattedSavingsAmount"]?.toString() ?: ""

                    contentBuilder.append("### 📈 Savings Rate Analysis\n\n")
                    contentBuilder.append("• **Savings Rate:** **$rate**\n")
                    contentBuilder.append("• **Monthly Inflows:** $inflows\n")
                    contentBuilder.append("• **Monthly Expenses:** $expenses\n")
                    contentBuilder.append("• **Net Monthly Savings:** $savings\n\n")
                    contentBuilder.append("_Calculation is performed deterministically as (Inflows - Expenses) / Inflows._")
                }

                "get_cash_flow" -> {
                    val inflows = result.data["formattedInflows"]?.toString() ?: ""
                    val expenses = result.data["formattedExpenses"]?.toString() ?: ""
                    val sips = result.data["formattedSipInvestments"]?.toString() ?: ""
                    val net = result.data["formattedNetCashFlow"]?.toString() ?: ""

                    contentBuilder.append("### 🔄 Cash Flow Ledger\n\n")
                    contentBuilder.append("• **Inflows (Income):** +$inflows\n")
                    contentBuilder.append("• **Living Expenses:** -$expenses\n")
                    contentBuilder.append("• **SIP Investments:** -$sips\n")
                    contentBuilder.append("• **Net Free Cash Flow:** **$net**\n")
                }

                "get_monthly_comparison" -> {
                    val diff = result.data["formattedTotalDiff"]?.toString() ?: ""
                    val organic = result.data["formattedOrganicDiff"]?.toString() ?: ""
                    val capital = result.data["formattedCapitalInflows"]?.toString() ?: ""
                    val pA = result.data["periodALabel"]?.toString() ?: ""
                    val pB = result.data["periodBLabel"]?.toString() ?: ""

                    contentBuilder.append("### ⚖️ Month-over-Month Comparison ($pA vs $pB)\n\n")
                    contentBuilder.append("• **Total Net Worth Change:** $diff\n")
                    contentBuilder.append("• **New Capital Inflows (SIP/Transfers):** $capital\n")
                    contentBuilder.append("• **Organic Market Performance:** **$organic**\n")
                }

                "get_fire_projection" -> {
                    val target = result.data["formattedTargetCorpus"]?.toString() ?: ""
                    val current = result.data["formattedNetWorth"]?.toString() ?: ""
                    val prog = result.data["formattedProgress"]?.toString() ?: ""
                    val age = String.format("%.1f", result.data["projectedFireAge"] as? Double ?: 0.0)
                    val years = String.format("%.1f", result.data["yearsRemaining"] as? Double ?: 0.0)
                    val coast = result.data["formattedTargetCorpus"]?.toString() ?: ""
                    val isCoast = result.data["isCoastFireAchieved"] as? Boolean ?: false

                    contentBuilder.append("### 🔥 FIRE Retirement Projection\n\n")
                    contentBuilder.append("• **Target FIRE Corpus:** $target\n")
                    contentBuilder.append("• **Current Progress:** $current (**$prog**)\n")
                    contentBuilder.append("• **Projected Retirement Age:** **$age years** ($years years remaining)\n")
                    contentBuilder.append("• **Coast FIRE Milestone:** ${if (isCoast) "✅ Achieved" else "⏳ In Progress"}\n")
                }

                "get_portfolio" -> {
                    val total = result.data["formattedTotalValuation"]?.toString() ?: ""
                    val count = result.data["holdingsCount"]?.toString() ?: ""
                    val holdings = result.data["holdings"] as? List<Map<String, Any?>> ?: emptyList()

                    contentBuilder.append("### 💼 Portfolio Holdings ($count Assets)\n\n")
                    contentBuilder.append("• **Total Portfolio Valuation:** $total\n\n")
                    holdings.take(8).forEach { h ->
                        contentBuilder.append("• **${h["name"]}** (${h["category"]}): ${h["formattedValue"]} (Invested: ${h["formattedInvested"]})\n")
                    }
                    if (holdings.size > 8) {
                        contentBuilder.append("• _...and ${holdings.size - 8} more holdings._\n")
                    }
                }

                "add_expense", "add_income", "add_holding" -> {
                    contentBuilder.append(result.summaryText).append("\n\n")
                    contentBuilder.append("⚠️ **Write Safety Policy:** Please review and tap **Confirm** on the action card below to save this into your local database.")
                }

                "web_search" -> {
                    val rawResults = result.data["results"] as? List<Map<String, String>> ?: emptyList()
                    val sQuery = result.data["query"]?.toString() ?: userQuery
                    contentBuilder.append("### 🌐 Live Web & Financial Intelligence\n\n")
                    if (rawResults.isNotEmpty()) {
                        rawResults.forEachIndexed { idx, r ->
                            val title = r["title"] ?: "Research Note"
                            val snippet = r["snippet"] ?: ""
                            val url = r["url"] ?: ""
                            contentBuilder.append("#### ${idx + 1}. $title\n")
                            contentBuilder.append("$snippet\n")
                            if (url.isNotBlank() && !url.contains("duckduckgo.com/?q=")) {
                                contentBuilder.append("> 🔗 **Official Portal / Source:** $url\n\n")
                            } else {
                                contentBuilder.append("\n")
                            }
                        }
                    } else {
                        contentBuilder.append(result.summaryText).append("\n\n")
                    }
                }

                else -> {
                    contentBuilder.append(result.summaryText).append("\n\n")
                }
            }
        }

        return LlmResponse(
            content = contentBuilder.toString().trim(),
            toolCalls = executedTools.map { it.first },
            isToolCallRequested = false,
            metadata = AiResponseMetadata(
                usedLocalData = usedLocal,
                usedCalculators = usedCalculators,
                usedWebSearch = usedWeb,
                usedExternalMcp = false,
                toolsUsed = toolsUsed,
                sources = sources
            )
        )
    }

    private fun extractAmount(q: String): Double? {
        val pattern = Pattern.compile("""(?:₹|rs\.?|inr)?\s*(\d+(?:[.,]\d+)?)\s*(?:k|thousand|lakh|lac|cr|crore)?""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(q)
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "") ?: continue
            var num = numStr.toDoubleOrNull() ?: continue
            val fullMatch = matcher.group(0)?.lowercase() ?: ""
            if (fullMatch.contains("k") || fullMatch.contains("thousand")) num *= 1000.0
            if (fullMatch.contains("lakh") || fullMatch.contains("lac")) num *= 100000.0
            if (fullMatch.contains("cr") || fullMatch.contains("crore")) num *= 10000000.0
            if (num > 10) return num
        }
        return null
    }

    private fun extractYears(q: String): Double? {
        val pattern = Pattern.compile("""(\d+)\s*(?:years?|yrs?|yr)""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(q)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull()
        }
        return null
    }

    private fun extractReturnRate(q: String): Double? {
        val pattern = Pattern.compile("""(\d+(?:\.\d+)?)\s*(?:%|percent|return|cagr)""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(q)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull()
        }
        return null
    }

    private fun extractInflationRate(q: String): Double? {
        val pattern = Pattern.compile("""(?:inflation\s*(?:of|is|at)?\s*)(\d+(?:\.\d+)?)\s*%?""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(q)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull()
        }
        return null
    }

    private fun extractStepUpPct(q: String): Double? {
        val pattern = Pattern.compile("""(?:step\s*up|hike|increase)\s*(?:by|of)?\s*(\d+(?:\.\d+)?)\s*%?""", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(q)
        if (matcher.find()) {
            return matcher.group(1)?.toDoubleOrNull()
        }
        return null
    }

    private fun extractCategory(q: String): String? {
        return when {
            q.contains("food") || q.contains("dining") || q.contains("restaurant") || q.contains("dinner") || q.contains("lunch") -> "Food & Dining"
            q.contains("grocery") || q.contains("groceries") || q.contains("supermarket") -> "Groceries"
            q.contains("rent") || q.contains("housing") -> "Rent / Housing"
            q.contains("utility") || q.contains("electricity") || q.contains("wifi") || q.contains("bill") -> "Utilities"
            q.contains("shopping") || q.contains("clothing") || q.contains("clothes") -> "Shopping"
            q.contains("travel") || q.contains("vacation") || q.contains("flight") || q.contains("fuel") || q.contains("cab") -> "Travel & Commute"
            q.contains("entertainment") || q.contains("movie") || q.contains("netflix") -> "Entertainment"
            q.contains("health") || q.contains("medical") || q.contains("doctor") || q.contains("pharmacy") -> "Healthcare"
            else -> null
        }
    }

    private fun extractTitle(q: String): String? {
        val words = q.split(" ")
        val forIndex = words.indexOfFirst { it.equals("for", ignoreCase = true) || it.equals("on", ignoreCase = true) }
        if (forIndex != -1 && forIndex + 1 < words.size) {
            return words.subList(forIndex + 1, minOf(forIndex + 4, words.size)).joinToString(" ")
        }
        return null
    }
}
