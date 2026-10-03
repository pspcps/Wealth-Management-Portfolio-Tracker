package com.example.ai.tools

import com.example.ai.Source
import com.example.ai.SourceCategory
import com.example.domain.finance.CashFlowItem
import com.example.domain.finance.FinanceCalculators
import java.text.DecimalFormat

private val inrFormatter = DecimalFormat("#,##,###.##")

private fun formatInr(amount: Double): String {
    return "₹" + inrFormatter.format(amount)
}

/**
 * calculate_sip
 */
class CalculateSipTool : FinanceTool {
    override val name: String = "calculate_sip"
    override val description: String = "Deterministically calculates SIP (Systematic Investment Plan) future wealth creation and compound returns."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "monthly_investment" to "Monthly SIP amount in INR (e.g. 25000)",
        "years" to "Duration in years (e.g. 15)",
        "annual_return" to "Expected annual return percentage (e.g. 12)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val monthly = (arguments["monthly_investment"]?.toString()?.toDoubleOrNull()) ?: 10000.0
        val years = (arguments["years"]?.toString()?.toDoubleOrNull()) ?: 10.0
        val returnRate = (arguments["annual_return"]?.toString()?.toDoubleOrNull()) ?: 12.0

        val result = FinanceCalculators.calculateSip(monthly, years, returnRate)

        val data = mapOf(
            "monthlyInvestment" to result.monthlyInvestment,
            "years" to result.years,
            "annualReturnRate" to result.annualReturnRate,
            "totalInvested" to result.totalInvested,
            "formattedTotalInvested" to formatInr(result.totalInvested),
            "estimatedWealthGain" to result.estimatedWealthGain,
            "formattedWealthGain" to formatInr(result.estimatedWealthGain),
            "futureValue" to result.futureValue,
            "formattedFutureValue" to formatInr(result.futureValue)
        )

        val summary = "An SIP of ${formatInr(monthly)}/month for ${result.years} years at ${result.annualReturnRate}% return results in a total corpus of ${formatInr(result.futureValue)} (Invested: ${formatInr(result.totalInvested)}, Wealth Gain: ${formatInr(result.estimatedWealthGain)})."

        return ToolResult.success(
            data = data,
            summaryText = summary,
            source = Source(
                title = "Deterministic SIP Calculator",
                snippet = "Formula: FV = P × [((1+r)^n - 1) / r] × (1+r)",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_step_up_sip
 */
class CalculateStepUpSipTool : FinanceTool {
    override val name: String = "calculate_step_up_sip"
    override val description: String = "Calculates Step-Up SIP where the monthly investment increases annually by a given percentage."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "initial_monthly" to "Initial monthly investment (e.g. 25000)",
        "step_up_percentage" to "Annual increase percentage (e.g. 10)",
        "years" to "Duration in years (e.g. 15)",
        "annual_return" to "Expected return rate % (e.g. 12)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val initial = arguments["initial_monthly"]?.toString()?.toDoubleOrNull() ?: 10000.0
        val stepUp = arguments["step_up_percentage"]?.toString()?.toDoubleOrNull() ?: 10.0
        val years = arguments["years"]?.toString()?.toDoubleOrNull() ?: 10.0
        val returnRate = arguments["annual_return"]?.toString()?.toDoubleOrNull() ?: 12.0

        val res = FinanceCalculators.calculateStepUpSip(initial, stepUp, years, returnRate)

        val data = mapOf(
            "initialMonthly" to res.initialMonthlyInvestment,
            "stepUpPercentage" to res.annualStepUpPercentage,
            "years" to res.years,
            "annualReturnRate" to res.annualReturnRate,
            "totalInvested" to res.totalInvested,
            "formattedTotalInvested" to formatInr(res.totalInvested),
            "wealthGain" to res.estimatedWealthGain,
            "formattedWealthGain" to formatInr(res.estimatedWealthGain),
            "futureValue" to res.futureValue,
            "formattedFutureValue" to formatInr(res.futureValue),
            "finalMonthly" to res.finalMonthlyInvestment
        )

        val summary = "Step-Up SIP starting at ${formatInr(initial)} with $stepUp% annual hike over $years years at $returnRate% delivers a future corpus of ${formatInr(res.futureValue)} (Invested: ${formatInr(res.totalInvested)}, Gain: ${formatInr(res.estimatedWealthGain)})."

        return ToolResult.success(
            data = data,
            summaryText = summary,
            source = Source(
                title = "Deterministic Step-Up SIP Engine",
                snippet = "Initial: ${formatInr(initial)}, Step-Up: $stepUp%/yr, FV: ${formatInr(res.futureValue)}",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_lumpsum
 */
class CalculateLumpsumTool : FinanceTool {
    override val name: String = "calculate_lumpsum"
    override val description: String = "Calculates compound future growth for one-time lumpsum investments."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "investment_amount" to "Lumpsum principal in INR (e.g. 500000)",
        "years" to "Duration in years (e.g. 10)",
        "annual_return" to "Expected return rate % (e.g. 12)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val amount = arguments["investment_amount"]?.toString()?.toDoubleOrNull() ?: 100000.0
        val years = arguments["years"]?.toString()?.toDoubleOrNull() ?: 10.0
        val returnRate = arguments["annual_return"]?.toString()?.toDoubleOrNull() ?: 12.0

        val res = FinanceCalculators.calculateLumpsum(amount, years, returnRate)

        val data = mapOf(
            "principal" to res.initialInvestment,
            "formattedPrincipal" to formatInr(res.initialInvestment),
            "years" to res.years,
            "annualReturn" to res.annualReturnRate,
            "wealthGain" to res.estimatedWealthGain,
            "formattedGain" to formatInr(res.estimatedWealthGain),
            "futureValue" to res.futureValue,
            "formattedFutureValue" to formatInr(res.futureValue)
        )

        val summary = "A lumpsum investment of ${formatInr(amount)} for $years years at $returnRate% CAGR compounds to ${formatInr(res.futureValue)} (Wealth Gain: ${formatInr(res.estimatedWealthGain)})."

        return ToolResult.success(
            data = data,
            summaryText = summary,
            source = Source(
                title = "Deterministic Lumpsum Compound Calculator",
                snippet = "Formula: FV = P × (1 + r)^t",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_xirr
 */
class CalculateXirrTool : FinanceTool {
    override val name: String = "calculate_xirr"
    override val description: String = "Calculates Extended Internal Rate of Return (XIRR) using Newton-Raphson method for irregular cash flows."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "cash_flows" to "List of cash flow points with date and amount (negative for investment, positive for redemption/valuation)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val rawFlows = arguments["cash_flows"]
        val flows = mutableListOf<CashFlowItem>()

        if (rawFlows is List<*>) {
            for (item in rawFlows) {
                if (item is Map<*, *>) {
                    val date = item["date"]?.toString() ?: ""
                    val amount = item["amount"]?.toString()?.toDoubleOrNull() ?: 0.0
                    if (date.isNotBlank() && amount != 0.0) {
                        flows.add(CashFlowItem(date, amount))
                    }
                }
            }
        }

        if (flows.size < 2) {
            return ToolResult.unavailable("Need at least two valid cash flows (with negative outflow and positive inflow) to calculate XIRR.")
        }

        val res = FinanceCalculators.calculateXirr(flows)

        if (!res.isValid) {
            return ToolResult.error(res.message)
        }

        val data = mapOf(
            "xirrPercentage" to res.annualRatePercentage,
            "formattedXirr" to "${String.format("%.2f", res.annualRatePercentage)}%",
            "cashFlowCount" to flows.size
        )

        return ToolResult.success(
            data = data,
            summaryText = "Calculated annualized XIRR: ${String.format("%.2f", res.annualRatePercentage)}%.",
            source = Source(
                title = "Newton-Raphson XIRR Engine",
                snippet = "XIRR: ${String.format("%.2f", res.annualRatePercentage)}%",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_modified_dietz
 */
class CalculateModifiedDietzTool : FinanceTool {
    override val name: String = "calculate_modified_dietz"
    override val description: String = "Calculates money-weighted rate of return using Modified Dietz method."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "start_value" to "Beginning portfolio valuation",
        "end_value" to "Ending portfolio valuation",
        "net_inflow" to "Net external capital added/withdrawn during the period",
        "day_weight_inflows" to "Time-weighted cash flows (defaults to net_inflow * 0.5)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val start = arguments["start_value"]?.toString()?.toDoubleOrNull() ?: 0.0
        val end = arguments["end_value"]?.toString()?.toDoubleOrNull() ?: 0.0
        val inflow = arguments["net_inflow"]?.toString()?.toDoubleOrNull() ?: 0.0
        val weight = arguments["day_weight_inflows"]?.toString()?.toDoubleOrNull() ?: (inflow * 0.5)

        val res = FinanceCalculators.calculateModifiedDietz(start, end, inflow, weight)

        val data = mapOf(
            "returnPercentage" to res.returnPercentage,
            "formattedReturn" to "${String.format("%.2f", res.returnPercentage)}%",
            "startValue" to res.startValue,
            "endValue" to res.endValue,
            "netInflow" to res.netInflow,
            "organicGain" to res.absoluteGain
        )

        return ToolResult.success(
            data = data,
            summaryText = "Modified Dietz Return is ${String.format("%.2f", res.returnPercentage)}% (Organic Gain: ${formatInr(res.absoluteGain)} on net capital inflow ${formatInr(res.netInflow)}).",
            source = Source(
                title = "Modified Dietz Return Engine",
                snippet = "Return: ${String.format("%.2f", res.returnPercentage)}%",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_fire_corpus
 */
class CalculateFireCorpusTool : FinanceTool {
    override val name: String = "calculate_fire_corpus"
    override val description: String = "Calculates Target, Lean, and Fat FIRE corpus targets based on monthly expenses and safe withdrawal rate (SWR)."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "monthly_expenses" to "Monthly living expenses in INR (e.g. 50000)",
        "swr_percentage" to "Safe withdrawal rate percentage (e.g. 3.5 or 4.0)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val monthly = arguments["monthly_expenses"]?.toString()?.toDoubleOrNull() ?: 50000.0
        val swr = arguments["swr_percentage"]?.toString()?.toDoubleOrNull() ?: 3.5

        val res = FinanceCalculators.calculateFireCorpus(monthly, swr)

        val data = mapOf(
            "monthlyExpenses" to res.monthlyExpenses,
            "annualExpenses" to res.annualExpenses,
            "swrPercentage" to res.swrPercentage,
            "targetCorpus" to res.targetCorpus,
            "formattedTargetCorpus" to formatInr(res.targetCorpus),
            "leanFireCorpus" to res.leanFireCorpus,
            "formattedLeanCorpus" to formatInr(res.leanFireCorpus),
            "fatFireCorpus" to res.fatFireCorpus,
            "formattedFatCorpus" to formatInr(res.fatFireCorpus)
        )

        val summary = "With ${formatInr(monthly)}/month expenses and a $swr% Safe Withdrawal Rate (SWR), your target FIRE corpus is ${formatInr(res.targetCorpus)} (Lean FIRE: ${formatInr(res.leanFireCorpus)}, Fat FIRE: ${formatInr(res.fatFireCorpus)})."

        return ToolResult.success(
            data = data,
            summaryText = summary,
            source = Source(
                title = "Deterministic FIRE Corpus Calculator",
                snippet = "Formula: Annual Expenses (${formatInr(res.annualExpenses)}) / $swr% SWR",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_real_return
 */
class CalculateRealReturnTool : FinanceTool {
    override val name: String = "calculate_real_return"
    override val description: String = "Calculates true inflation-adjusted purchasing power return using the Fisher equation."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "nominal_return" to "Nominal annual return % (e.g. 12)",
        "inflation_rate" to "Annual inflation rate % (e.g. 6)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val nom = arguments["nominal_return"]?.toString()?.toDoubleOrNull() ?: 12.0
        val inf = arguments["inflation_rate"]?.toString()?.toDoubleOrNull() ?: 6.0

        val res = FinanceCalculators.calculateRealReturn(nom, inf)

        val data = mapOf(
            "nominalReturn" to res.nominalReturnPercentage,
            "inflation" to res.inflationPercentage,
            "realReturn" to res.realReturnPercentage,
            "formattedRealReturn" to "${String.format("%.2f", res.realReturnPercentage)}%"
        )

        return ToolResult.success(
            data = data,
            summaryText = res.explanation,
            source = Source(
                title = "Fisher Real Return Formula",
                snippet = "((1 + $nom%) / (1 + $inf%)) - 1 = ${String.format("%.2f", res.realReturnPercentage)}%",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_inflation_adjusted_value
 */
class CalculateInflationAdjustedValueTool : FinanceTool {
    override val name: String = "calculate_inflation_adjusted_value"
    override val description: String = "Calculates future cost or purchasing power erosion caused by inflation over time."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "present_value" to "Current cost or amount (e.g. 50000)",
        "inflation_rate" to "Annual inflation rate % (e.g. 6)",
        "years" to "Number of years into the future (e.g. 15)"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val pv = arguments["present_value"]?.toString()?.toDoubleOrNull() ?: 50000.0
        val inf = arguments["inflation_rate"]?.toString()?.toDoubleOrNull() ?: 6.0
        val years = arguments["years"]?.toString()?.toDoubleOrNull() ?: 15.0

        val res = FinanceCalculators.calculateInflationAdjustedValue(pv, inf, years)

        val data = mapOf(
            "presentValue" to res.presentValue,
            "inflationRate" to res.inflationPercentage,
            "years" to res.years,
            "futurePurchasingCost" to res.futurePurchasingCost,
            "formattedFutureCost" to formatInr(res.futurePurchasingCost),
            "multiplier" to res.futureValueMultiplier
        )

        val summary = "An expense of ${formatInr(pv)} today will require ${formatInr(res.futurePurchasingCost)} in $years years at $inf% annual inflation (a ${String.format("%.2f", res.futureValueMultiplier)}x increase)."

        return ToolResult.success(
            data = data,
            summaryText = summary,
            source = Source(
                title = "Inflation Compounding Engine",
                snippet = "Future cost: ${formatInr(res.futurePurchasingCost)} in $years yrs",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}

/**
 * calculate_coast_fire
 */
class CalculateCoastFireTool : FinanceTool {
    override val name: String = "calculate_coast_fire"
    override val description: String = "Calculates Coast FIRE milestone — the corpus required today so compounding alone reaches retirement target without further investments."
    override val permission: ToolPermission = ToolPermission.CALCULATE
    override val parameterSchema: Map<String, String> = mapOf(
        "current_age" to "Current age (e.g. 28)",
        "target_fire_age" to "Target retirement age (e.g. 45)",
        "target_corpus" to "Target retirement corpus in INR (e.g. 30000000)",
        "expected_return" to "Expected nominal annual return % (e.g. 12)",
        "inflation_rate" to "Inflation % (e.g. 6)",
        "current_net_worth" to "Optional current net worth to check achievement status"
    )

    override suspend fun execute(arguments: Map<String, Any?>): ToolResult {
        val age = arguments["current_age"]?.toString()?.toIntOrNull() ?: 28
        val fireAge = arguments["target_fire_age"]?.toString()?.toIntOrNull() ?: 45
        val targetCorpus = arguments["target_corpus"]?.toString()?.toDoubleOrNull() ?: 20000000.0
        val expReturn = arguments["expected_return"]?.toString()?.toDoubleOrNull() ?: 12.0
        val inf = arguments["inflation_rate"]?.toString()?.toDoubleOrNull() ?: 6.0
        val netWorth = arguments["current_net_worth"]?.toString()?.toDoubleOrNull() ?: 0.0

        val res = FinanceCalculators.calculateCoastFire(age, fireAge, targetCorpus, expReturn, inf, netWorth)

        val data = mapOf(
            "currentAge" to res.currentAge,
            "targetFireAge" to res.targetFireAge,
            "targetCorpus" to res.targetCorpus,
            "formattedTargetCorpus" to formatInr(res.targetCorpus),
            "realReturnRate" to res.realReturnRate,
            "requiredCoastCorpus" to res.requiredCoastCorpusToday,
            "formattedRequiredCoast" to formatInr(res.requiredCoastCorpusToday),
            "currentNetWorth" to res.currentNetWorth,
            "isCoastFireAchieved" to res.isCoastFireAchieved,
            "surplusOrDeficit" to res.surplusOrDeficit
        )

        val statusText = if (netWorth > 0) {
            if (res.isCoastFireAchieved) "🎉 You have achieved Coast FIRE with a surplus of ${formatInr(res.surplusOrDeficit)}!" else "You need ${formatInr(-res.surplusOrDeficit)} more to achieve Coast FIRE."
        } else ""

        val summary = "Coast FIRE Corpus required today is ${formatInr(res.requiredCoastCorpusToday)} to reach ${formatInr(targetCorpus)} at age $fireAge at ${String.format("%.2f", res.realReturnRate)}% real return. $statusText".trim()

        return ToolResult.success(
            data = data,
            summaryText = summary,
            source = Source(
                title = "Deterministic Coast FIRE Calculator",
                snippet = "Required today: ${formatInr(res.requiredCoastCorpusToday)}",
                category = SourceCategory.DETERMINISTIC_CALCULATOR
            )
        )
    }
}
