package com.example.domain.finance

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.pow

data class SipResult(
    val monthlyInvestment: Double,
    val years: Double,
    val annualReturnRate: Double,
    val totalInvested: Double,
    val estimatedWealthGain: Double,
    val futureValue: Double
)

data class StepUpSipResult(
    val initialMonthlyInvestment: Double,
    val annualStepUpPercentage: Double,
    val years: Double,
    val annualReturnRate: Double,
    val totalInvested: Double,
    val estimatedWealthGain: Double,
    val futureValue: Double,
    val finalMonthlyInvestment: Double
)

data class LumpsumResult(
    val initialInvestment: Double,
    val years: Double,
    val annualReturnRate: Double,
    val estimatedWealthGain: Double,
    val futureValue: Double
)

data class CashFlowItem(
    val date: String, // "YYYY-MM-DD" or timestamp
    val amount: Double // Negative for investments/outflows, positive for redemptions/current value
)

data class AssetXirrSummary(
    val xirrPercentage: Double?,
    val isValid: Boolean,
    val holdingPeriodDays: Long = 0,
    val holdingPeriodFormatted: String = "",
    val totalInvested: Double = 0.0,
    val currentValue: Double = 0.0,
    val absoluteGain: Double = 0.0,
    val returnPercentage: Double? = null,
    val cashFlows: List<CashFlowItem> = emptyList(),
    val message: String = ""
)

data class XirrResult(
    val annualRatePercentage: Double,
    val isValid: Boolean,
    val message: String = ""
)

data class ModifiedDietzResult(
    val returnPercentage: Double,
    val startValue: Double,
    val endValue: Double,
    val netInflow: Double,
    val absoluteGain: Double
)

data class FireCorpusResult(
    val monthlyExpenses: Double,
    val annualExpenses: Double,
    val swrPercentage: Double,
    val targetCorpus: Double,
    val leanFireCorpus: Double,
    val fatFireCorpus: Double
)

data class CoastFireResult(
    val currentAge: Int,
    val targetFireAge: Int,
    val targetCorpus: Double,
    val expectedAnnualReturn: Double,
    val inflationRate: Double,
    val realReturnRate: Double,
    val requiredCoastCorpusToday: Double,
    val currentNetWorth: Double,
    val isCoastFireAchieved: Boolean,
    val surplusOrDeficit: Double
)

data class RealReturnResult(
    val nominalReturnPercentage: Double,
    val inflationPercentage: Double,
    val realReturnPercentage: Double,
    val explanation: String
)

data class InflationAdjustedResult(
    val presentValue: Double,
    val inflationPercentage: Double,
    val years: Double,
    val futurePurchasingCost: Double,
    val futureValueMultiplier: Double
)

enum class PayoutFrequency(val label: String, val periodsPerYear: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    HALF_YEARLY("Half-Yearly", 2),
    ANNUALLY("Annually", 1),
    AT_MATURITY("At Maturity", 0)
}

enum class PayoutMode(val label: String) {
    CUMULATIVE("Cumulative (At Maturity)"),
    INTEREST_ONLY("Regular Interest Payout"),
    INTEREST_AND_PRINCIPAL("Interest + Principal Payout (% Amortizing)")
}

enum class RdFrequency(val label: String, val periodsPerYear: Int) {
    MONTHLY("Monthly", 12),
    QUARTERLY("Quarterly", 4),
    ANNUALLY("Annually", 1)
}

data class PayoutScheduleItem(
    val periodIndex: Int,
    val periodLabel: String,
    val openingPrincipal: Double,
    val interestPayout: Double,
    val principalPayout: Double,
    val totalPayout: Double,
    val closingPrincipal: Double
)

data class FixedIncomePayoutResult(
    val initialPrincipal: Double,
    val annualRatePercentage: Double,
    val tenureMonths: Int,
    val payoutFrequency: PayoutFrequency,
    val payoutMode: PayoutMode,
    val principalPayoutPercent: Double = 0.0,
    val principalPayoutFrequency: PayoutFrequency = PayoutFrequency.QUARTERLY,
    val periodicInterestPayout: Double = 0.0,
    val periodicPrincipalPayout: Double = 0.0,
    val totalPeriodicPayout: Double = 0.0,
    val totalInterestEarned: Double = 0.0,
    val totalPrincipalReturned: Double = 0.0,
    val totalCashReceived: Double = 0.0,
    val finalMaturityPayout: Double = 0.0,
    val schedule: List<PayoutScheduleItem> = emptyList()
)

data class RdScheduleItem(
    val installmentNumber: Int,
    val periodLabel: String,
    val installmentAmount: Double,
    val cumulativeDeposited: Double,
    val interestEarnedPeriod: Double,
    val accruedBalance: Double
)

data class RdResult(
    val installmentAmount: Double,
    val frequency: RdFrequency,
    val annualRatePercentage: Double,
    val totalMonths: Int,
    val totalInstallments: Int,
    val totalDeposited: Double,
    val totalInterestEarned: Double,
    val maturityAmount: Double,
    val schedule: List<RdScheduleItem> = emptyList()
)

/**
 * Deterministic Financial Calculations Engine for Wealth Tracker.
 * All computations are purely mathematical without LLM hallucinations.
 */
object FinanceCalculators {

    /**
     * Standard SIP Future Value calculation:
     * FV = P × [((1 + i)^n - 1) / i] × (1 + i)
     * where:
     * P = monthly investment
     * i = monthly return rate (r / 12 / 100)
     * n = total months (years * 12)
     */
    fun calculateSip(
        monthlyInvestment: Double,
        years: Double,
        annualReturnRate: Double
    ): SipResult {
        val safeMonthly = monthlyInvestment.coerceAtLeast(0.0)
        val safeYears = years.coerceAtLeast(0.0)
        val safeReturn = annualReturnRate.coerceAtLeast(0.0)

        val months = (safeYears * 12.0).toInt()
        val totalInvested = safeMonthly * months

        if (months <= 0 || safeMonthly <= 0.0) {
            return SipResult(safeMonthly, safeYears, safeReturn, 0.0, 0.0, 0.0)
        }

        val monthlyRate = (safeReturn / 100.0) / 12.0

        val futureValue = if (monthlyRate > 0.0) {
            safeMonthly * (((1.0 + monthlyRate).pow(months) - 1.0) / monthlyRate) * (1.0 + monthlyRate)
        } else {
            totalInvested
        }

        val estimatedWealthGain = (futureValue - totalInvested).coerceAtLeast(0.0)

        return SipResult(
            monthlyInvestment = safeMonthly,
            years = safeYears,
            annualReturnRate = safeReturn,
            totalInvested = round2(totalInvested),
            estimatedWealthGain = round2(estimatedWealthGain),
            futureValue = round2(futureValue)
        )
    }

    /**
     * Step-Up SIP Calculator:
     * Monthly investment increases by stepUpRate% annually.
     */
    fun calculateStepUpSip(
        initialMonthlyInvestment: Double,
        annualStepUpPercentage: Double,
        years: Double,
        annualReturnRate: Double
    ): StepUpSipResult {
        val safeInitial = initialMonthlyInvestment.coerceAtLeast(0.0)
        val safeStepUp = annualStepUpPercentage.coerceAtLeast(0.0)
        val safeYears = years.coerceAtLeast(0.0)
        val safeReturn = annualReturnRate.coerceAtLeast(0.0)

        val totalMonths = (safeYears * 12.0).toInt()
        val monthlyRate = (safeReturn / 100.0) / 12.0
        val stepUpFraction = safeStepUp / 100.0

        var currentSip = safeInitial
        var totalInvested = 0.0
        var accumulatedValue = 0.0

        for (m in 1..totalMonths) {
            // Apply compound interest on previous balance
            accumulatedValue = accumulatedValue * (1.0 + monthlyRate) + currentSip * (1.0 + monthlyRate)
            totalInvested += currentSip

            // Step up at each year boundary
            if (m % 12 == 0 && stepUpFraction > 0.0) {
                currentSip *= (1.0 + stepUpFraction)
            }
        }

        val estimatedWealthGain = (accumulatedValue - totalInvested).coerceAtLeast(0.0)

        return StepUpSipResult(
            initialMonthlyInvestment = safeInitial,
            annualStepUpPercentage = safeStepUp,
            years = safeYears,
            annualReturnRate = safeReturn,
            totalInvested = round2(totalInvested),
            estimatedWealthGain = round2(estimatedWealthGain),
            futureValue = round2(accumulatedValue),
            finalMonthlyInvestment = round2(currentSip)
        )
    }

    /**
     * Lumpsum Compounding calculation:
     * FV = P × (1 + r/100)^t
     */
    fun calculateLumpsum(
        initialInvestment: Double,
        years: Double,
        annualReturnRate: Double
    ): LumpsumResult {
        val safePrincipal = initialInvestment.coerceAtLeast(0.0)
        val safeYears = years.coerceAtLeast(0.0)
        val safeReturn = annualReturnRate.coerceAtLeast(0.0)

        val futureValue = if (safeYears > 0 && safeReturn > 0) {
            safePrincipal * (1.0 + safeReturn / 100.0).pow(safeYears)
        } else {
            safePrincipal
        }

        val wealthGain = (futureValue - safePrincipal).coerceAtLeast(0.0)

        return LumpsumResult(
            initialInvestment = safePrincipal,
            years = safeYears,
            annualReturnRate = safeReturn,
            estimatedWealthGain = round2(wealthGain),
            futureValue = round2(futureValue)
        )
    }

    /**
     * XIRR (Extended Internal Rate of Return) via Newton-Raphson method with bisection fallback.
     */
    fun calculateXirr(cashFlows: List<CashFlowItem>): XirrResult {
        if (cashFlows.size < 2) {
            return XirrResult(0.0, false, "At least two cash flows (investment & valuation) are required.")
        }

        val hasNegative = cashFlows.any { it.amount < 0 }
        val hasPositive = cashFlows.any { it.amount > 0 }
        if (!hasNegative || !hasPositive) {
            return XirrResult(0.0, false, "Cash flows must contain both outflows (negative) and inflows/redemptions (positive).")
        }

        try {
            val dates = cashFlows.map { parseDate(it.date) }
            val minDate = dates.minOrNull() ?: dates[0]
            val maxDate = dates.maxOrNull() ?: dates[0]
            val totalDays = ChronoUnit.DAYS.between(minDate, maxDate).toDouble()
            if (totalDays < 1.0) {
                return XirrResult(0.0, false, "Holding period is less than 1 day.")
            }

            val dayDiffs = dates.map { ChronoUnit.DAYS.between(minDate, it).toDouble() }
            val amounts = cashFlows.map { it.amount }

            // Approximate initial rate from simple CAGR estimate
            val totalOutflows = amounts.filter { it < 0 }.sumOf { -it }
            val totalInflows = amounts.filter { it > 0 }.sumOf { it }
            val years = (totalDays / 365.25).coerceAtLeast(0.05)
            val cagrEstimate = if (totalOutflows > 0 && totalInflows > 0) {
                (totalInflows / totalOutflows).pow(1.0 / years) - 1.0
            } else 0.10

            var rate = cagrEstimate.coerceIn(-0.80, 2.0)
            val maxIterations = 100
            val tolerance = 1e-5

            for (i in 0 until maxIterations) {
                var npv = 0.0
                var dNpv = 0.0
                val base = (1.0 + rate).coerceAtLeast(0.001)

                for (j in amounts.indices) {
                    val yearsFraction = dayDiffs[j] / 365.0
                    val factor = base.pow(yearsFraction)
                    if (factor.isNaN() || factor.isInfinite() || factor == 0.0) continue

                    npv += amounts[j] / factor
                    if (yearsFraction != 0.0) {
                        dNpv -= (yearsFraction * amounts[j]) / (factor * base)
                    }
                }

                if (abs(npv) < tolerance) {
                    return XirrResult(round2(rate * 100.0), true, "Calculated successfully.")
                }

                if (abs(dNpv) < 1e-12) {
                    rate += if (npv > 0) 0.05 else -0.05
                } else {
                    val nextRate = rate - npv / dNpv
                    rate = nextRate.coerceIn(-0.95, 10.0)
                }
            }

            // Bisection fallback between -0.90 and 5.0
            var low = -0.90
            var high = 5.0
            fun computeNpv(r: Double): Double {
                val b = (1.0 + r).coerceAtLeast(0.001)
                return amounts.indices.sumOf { j ->
                    val t = dayDiffs[j] / 365.0
                    amounts[j] / b.pow(t)
                }
            }

            var npvLow = computeNpv(low)
            var npvHigh = computeNpv(high)

            if (npvLow * npvHigh <= 0) {
                for (iter in 0 until 50) {
                    val mid = (low + high) / 2.0
                    val npvMid = computeNpv(mid)
                    if (abs(npvMid) < tolerance) {
                        return XirrResult(round2(mid * 100.0), true, "Calculated successfully via bisection.")
                    }
                    if (npvLow * npvMid <= 0) {
                        high = mid
                        npvHigh = npvMid
                    } else {
                        low = mid
                        npvLow = npvMid
                    }
                }
                return XirrResult(round2(((low + high) / 2.0) * 100.0), true, "Estimated XIRR.")
            }

            return XirrResult(round2(rate * 100.0), true, "Converged with XIRR = ${round2(rate * 100.0)}%")
        } catch (e: Exception) {
            return XirrResult(0.0, false, "Calculation error: ${e.message}")
        }
    }

    /**
     * Formats holding period into human-readable format (e.g., "1 yr 4 mos" or "45 days").
     */
    fun formatHoldingPeriod(days: Long): String {
        return when {
            days < 30 -> "$days days"
            days < 365 -> {
                val months = days / 30
                val remainingDays = days % 30
                if (remainingDays > 0) "$months mos $remainingDays d" else "$months mos"
            }
            else -> {
                val years = days / 365
                val remainingMonths = (days % 365) / 30
                if (remainingMonths > 0) "$years yr $remainingMonths mos" else "$years yr"
            }
        }
    }

    /**
     * Modified Dietz Return Rate:
     * R = (EndValue - StartValue - NetInflow) / (StartValue + WeightedInflows)
     */
    fun calculateModifiedDietz(
        startValue: Double,
        endValue: Double,
        netInflows: Double,
        dayWeightInflows: Double
    ): ModifiedDietzResult {
        val absoluteGain = (endValue - startValue) - netInflows
        val denominator = startValue + dayWeightInflows

        val returnPercentage = if (denominator > 0.0) {
            (absoluteGain / denominator) * 100.0
        } else if (startValue > 0.0) {
            (absoluteGain / startValue) * 100.0
        } else 0.0

        return ModifiedDietzResult(
            returnPercentage = round2(returnPercentage),
            startValue = round2(startValue),
            endValue = round2(endValue),
            netInflow = round2(netInflows),
            absoluteGain = round2(absoluteGain)
        )
    }

    /**
     * Target FIRE Corpus based on Safe Withdrawal Rate (e.g. 3.5% = ~28.5x or 4% = 25x).
     */
    fun calculateFireCorpus(
        monthlyExpenses: Double,
        swrPercentage: Double
    ): FireCorpusResult {
        val safeMonthly = monthlyExpenses.coerceAtLeast(0.0)
        val safeSwr = swrPercentage.coerceIn(1.0, 10.0)
        val annualExpenses = safeMonthly * 12.0
        val swrFraction = safeSwr / 100.0

        val targetCorpus = annualExpenses / swrFraction
        val leanFire = (annualExpenses * 0.75) / swrFraction
        val fatFire = (annualExpenses * 1.50) / swrFraction

        return FireCorpusResult(
            monthlyExpenses = round2(safeMonthly),
            annualExpenses = round2(annualExpenses),
            swrPercentage = safeSwr,
            targetCorpus = round2(targetCorpus),
            leanFireCorpus = round2(leanFire),
            fatFireCorpus = round2(fatFire)
        )
    }

    /**
     * Coast FIRE:
     * Corpus required today such that compounding alone at real return reaches target at retirement.
     */
    fun calculateCoastFire(
        currentAge: Int,
        targetFireAge: Int,
        targetCorpus: Double,
        expectedAnnualReturn: Double,
        inflationRate: Double,
        currentNetWorth: Double = 0.0
    ): CoastFireResult {
        val yearsToRetire = maxOf(1, targetFireAge - currentAge)
        val nominalR = expectedAnnualReturn / 100.0
        val inflationI = inflationRate / 100.0

        // Real return via Fisher equation: (1 + r)/(1 + i) - 1
        val realReturn = if (1.0 + inflationI > 0) ((1.0 + nominalR) / (1.0 + inflationI)) - 1.0 else nominalR - inflationI
        val realReturnPct = realReturn * 100.0

        val requiredCoast = if (realReturn > 0) {
            targetCorpus / (1.0 + realReturn).pow(yearsToRetire.toDouble())
        } else {
            targetCorpus
        }

        val isAchieved = currentNetWorth >= requiredCoast
        val diff = currentNetWorth - requiredCoast

        return CoastFireResult(
            currentAge = currentAge,
            targetFireAge = targetFireAge,
            targetCorpus = round2(targetCorpus),
            expectedAnnualReturn = expectedAnnualReturn,
            inflationRate = inflationRate,
            realReturnRate = round2(realReturnPct),
            requiredCoastCorpusToday = round2(requiredCoast),
            currentNetWorth = round2(currentNetWorth),
            isCoastFireAchieved = isAchieved,
            surplusOrDeficit = round2(diff)
        )
    }

    /**
     * Real Rate of Return (Fisher Equation):
     * Real Rate = ((1 + Nominal Rate) / (1 + Inflation Rate)) - 1
     */
    fun calculateRealReturn(
        nominalReturnRate: Double,
        inflationRate: Double
    ): RealReturnResult {
        val nomFraction = nominalReturnRate / 100.0
        val infFraction = inflationRate / 100.0

        val realFraction = if (1.0 + infFraction > 0) {
            ((1.0 + nomFraction) / (1.0 + infFraction)) - 1.0
        } else {
            nomFraction - infFraction
        }

        val realPct = realFraction * 100.0
        val explanation = "A nominal return of $nominalReturnRate% with $inflationRate% inflation yields an actual real purchasing power growth of ${round2(realPct)}% per year."

        return RealReturnResult(
            nominalReturnPercentage = nominalReturnRate,
            inflationPercentage = inflationRate,
            realReturnPercentage = round2(realPct),
            explanation = explanation
        )
    }

    /**
     * Inflation-Adjusted Future Value / Purchasing Power.
     * FV = PresentValue × (1 + Inflation/100)^Years
     */
    fun calculateInflationAdjustedValue(
        presentValue: Double,
        inflationPercentage: Double,
        years: Double
    ): InflationAdjustedResult {
        val safePv = presentValue.coerceAtLeast(0.0)
        val safeInf = inflationPercentage.coerceAtLeast(0.0)
        val safeYears = years.coerceAtLeast(0.0)

        val multiplier = (1.0 + safeInf / 100.0).pow(safeYears)
        val futureCost = safePv * multiplier

        return InflationAdjustedResult(
            presentValue = round2(safePv),
            inflationPercentage = safeInf,
            years = safeYears,
            futurePurchasingCost = round2(futureCost),
            futureValueMultiplier = round2(multiplier)
        )
    }

    /**
     * Calculates Fixed Income, Bonds, and FD payouts.
     * Supports:
     * 1. Cumulative (At Maturity, compounded quarterly)
     * 2. Regular Interest Payout (Monthly, Quarterly, Half-Yearly, Annually)
     * 3. Regular Interest + Periodic % of Principal Return (Amortizing Structured Bonds / SDIs)
     */
    fun calculateFixedIncomePayout(
        principal: Double,
        annualRatePercentage: Double,
        tenureMonths: Int,
        payoutFrequency: PayoutFrequency,
        payoutMode: PayoutMode = PayoutMode.INTEREST_ONLY,
        principalPayoutPercent: Double = 0.0,
        principalPayoutFrequency: PayoutFrequency = PayoutFrequency.QUARTERLY
    ): FixedIncomePayoutResult {
        val safePrincipal = principal.coerceAtLeast(0.0)
        val safeRate = annualRatePercentage.coerceAtLeast(0.0)
        val safeMonths = tenureMonths.coerceAtLeast(1)

        if (safePrincipal <= 0.0 || safeRate <= 0.0) {
            return FixedIncomePayoutResult(
                initialPrincipal = round2(safePrincipal),
                annualRatePercentage = safeRate,
                tenureMonths = safeMonths,
                payoutFrequency = payoutFrequency,
                payoutMode = payoutMode,
                principalPayoutPercent = principalPayoutPercent,
                principalPayoutFrequency = principalPayoutFrequency,
                totalPrincipalReturned = round2(safePrincipal),
                totalCashReceived = round2(safePrincipal),
                finalMaturityPayout = round2(safePrincipal)
            )
        }

        if (payoutMode == PayoutMode.CUMULATIVE || payoutFrequency == PayoutFrequency.AT_MATURITY) {
            // Quarterly compounding standard
            val quarters = safeMonths / 3.0
            val quarterlyRate = (safeRate / 100.0) / 4.0
            val maturityAmount = safePrincipal * (1.0 + quarterlyRate).pow(quarters)
            val totalInterest = maturityAmount - safePrincipal

            val schedule = mutableListOf<PayoutScheduleItem>()
            val numQuarters = kotlin.math.max(1, (safeMonths / 3.0).toInt())
            var currentBal = safePrincipal
            for (q in 1..numQuarters) {
                val interest = currentBal * quarterlyRate
                val closing = currentBal + interest
                schedule.add(
                    PayoutScheduleItem(
                        periodIndex = q,
                        periodLabel = "Quarter $q (${q * 3} Mo)",
                        openingPrincipal = round2(currentBal),
                        interestPayout = round2(interest),
                        principalPayout = 0.0,
                        totalPayout = 0.0,
                        closingPrincipal = round2(closing)
                    )
                )
                currentBal = closing
            }

            return FixedIncomePayoutResult(
                initialPrincipal = round2(safePrincipal),
                annualRatePercentage = safeRate,
                tenureMonths = safeMonths,
                payoutFrequency = PayoutFrequency.AT_MATURITY,
                payoutMode = PayoutMode.CUMULATIVE,
                totalInterestEarned = round2(totalInterest),
                totalPrincipalReturned = round2(safePrincipal),
                totalCashReceived = round2(maturityAmount),
                finalMaturityPayout = round2(maturityAmount),
                schedule = schedule
            )
        }

        // Periodic Payouts
        val interestPeriodsPerYear = payoutFrequency.periodsPerYear.coerceAtLeast(1)
        val monthsPerInterestPeriod = 12 / interestPeriodsPerYear
        val totalInterestPeriods = kotlin.math.max(1, (safeMonths + monthsPerInterestPeriod - 1) / monthsPerInterestPeriod)

        val principalPeriodsPerYear = principalPayoutFrequency.periodsPerYear.coerceAtLeast(1)
        val monthsPerPrincipalPeriod = 12 / principalPeriodsPerYear

        val schedule = mutableListOf<PayoutScheduleItem>()
        var currentPrincipal = safePrincipal
        var totalInterestEarned = 0.0
        var totalPrincipalReturned = 0.0

        val isAmortizing = payoutMode == PayoutMode.INTEREST_AND_PRINCIPAL && principalPayoutPercent > 0.0
        val periodicPrincipalReturnAmt = if (isAmortizing) safePrincipal * (principalPayoutPercent / 100.0) else 0.0

        for (p in 1..totalInterestPeriods) {
            val periodMonth = p * monthsPerInterestPeriod
            val opening = currentPrincipal
            val interestPeriodRate = (safeRate / 100.0) / interestPeriodsPerYear
            val interestPayout = opening * interestPeriodRate

            var principalPayout = 0.0
            val isFinalPeriod = p == totalInterestPeriods

            if (isAmortizing) {
                val isPrincipalPeriod = (periodMonth % monthsPerPrincipalPeriod == 0) || isFinalPeriod
                if (isPrincipalPeriod) {
                    principalPayout = if (isFinalPeriod) {
                        currentPrincipal // return all remaining principal at end
                    } else {
                        periodicPrincipalReturnAmt.coerceAtMost(currentPrincipal)
                    }
                }
            } else if (isFinalPeriod) {
                principalPayout = currentPrincipal
            }

            val closing = (opening - principalPayout).coerceAtLeast(0.0)
            val totalPayout = interestPayout + principalPayout

            totalInterestEarned += interestPayout
            totalPrincipalReturned += principalPayout
            currentPrincipal = closing

            val label = when (payoutFrequency) {
                PayoutFrequency.MONTHLY -> "Month $p"
                PayoutFrequency.QUARTERLY -> "Quarter $p (${p * 3}M)"
                PayoutFrequency.HALF_YEARLY -> "Half-Year $p (${p * 6}M)"
                PayoutFrequency.ANNUALLY -> "Year $p"
                else -> "Period $p"
            }

            schedule.add(
                PayoutScheduleItem(
                    periodIndex = p,
                    periodLabel = label,
                    openingPrincipal = round2(opening),
                    interestPayout = round2(interestPayout),
                    principalPayout = round2(principalPayout),
                    totalPayout = round2(totalPayout),
                    closingPrincipal = round2(closing)
                )
            )
        }

        val basePeriodicInterest = safePrincipal * ((safeRate / 100.0) / interestPeriodsPerYear)
        val basePeriodicPrincipal = if (isAmortizing) periodicPrincipalReturnAmt else 0.0

        return FixedIncomePayoutResult(
            initialPrincipal = round2(safePrincipal),
            annualRatePercentage = safeRate,
            tenureMonths = safeMonths,
            payoutFrequency = payoutFrequency,
            payoutMode = payoutMode,
            principalPayoutPercent = principalPayoutPercent,
            principalPayoutFrequency = principalPayoutFrequency,
            periodicInterestPayout = round2(basePeriodicInterest),
            periodicPrincipalPayout = round2(basePeriodicPrincipal),
            totalPeriodicPayout = round2(basePeriodicInterest + basePeriodicPrincipal),
            totalInterestEarned = round2(totalInterestEarned),
            totalPrincipalReturned = round2(totalPrincipalReturned),
            totalCashReceived = round2(totalInterestEarned + totalPrincipalReturned),
            finalMaturityPayout = round2(schedule.lastOrNull()?.totalPayout ?: 0.0),
            schedule = schedule
        )
    }

    /**
     * Calculates Recurring Deposit (RD) corpus and interest.
     * Supports Monthly, Quarterly, and Annually deposit frequencies.
     * Standard quarterly compounding applied.
     */
    fun calculateRecurringDeposit(
        installmentAmount: Double,
        frequency: RdFrequency = RdFrequency.MONTHLY,
        annualRatePercentage: Double,
        tenureMonths: Int
    ): RdResult {
        val safeInstallment = installmentAmount.coerceAtLeast(0.0)
        val safeRate = annualRatePercentage.coerceAtLeast(0.0)
        val safeMonths = tenureMonths.coerceAtLeast(1)

        val intervalMonths = 12 / frequency.periodsPerYear
        val totalInstallments = kotlin.math.max(1, safeMonths / intervalMonths)
        val totalDeposited = safeInstallment * totalInstallments

        if (safeInstallment <= 0.0 || safeRate <= 0.0) {
            return RdResult(
                installmentAmount = round2(safeInstallment),
                frequency = frequency,
                annualRatePercentage = safeRate,
                totalMonths = safeMonths,
                totalInstallments = totalInstallments,
                totalDeposited = round2(totalDeposited),
                totalInterestEarned = 0.0,
                maturityAmount = round2(totalDeposited)
            )
        }

        val quarterlyRate = (safeRate / 100.0) / 4.0
        var maturityCorpus = 0.0
        val schedule = mutableListOf<RdScheduleItem>()
        var runningDeposit = 0.0

        for (i in 1..totalInstallments) {
            val depositMonth = (i - 1) * intervalMonths
            val monthsCompounded = safeMonths - depositMonth
            val quarters = monthsCompounded / 3.0
            val fvInstallment = safeInstallment * (1.0 + quarterlyRate).pow(quarters)
            maturityCorpus += fvInstallment

            runningDeposit += safeInstallment
            val label = when (frequency) {
                RdFrequency.MONTHLY -> "Month $i"
                RdFrequency.QUARTERLY -> "Quarter $i (${i * 3}M)"
                RdFrequency.ANNUALLY -> "Year $i"
            }

            val estimatedInterestSoFar = (maturityCorpus * (depositMonth.toDouble() / safeMonths)).coerceAtLeast(0.0)

            schedule.add(
                RdScheduleItem(
                    installmentNumber = i,
                    periodLabel = label,
                    installmentAmount = round2(safeInstallment),
                    cumulativeDeposited = round2(runningDeposit),
                    interestEarnedPeriod = round2(fvInstallment - safeInstallment),
                    accruedBalance = round2(runningDeposit + estimatedInterestSoFar)
                )
            )
        }

        val totalInterest = maturityCorpus - totalDeposited

        return RdResult(
            installmentAmount = round2(safeInstallment),
            frequency = frequency,
            annualRatePercentage = safeRate,
            totalMonths = safeMonths,
            totalInstallments = totalInstallments,
            totalDeposited = round2(totalDeposited),
            totalInterestEarned = round2(totalInterest),
            maturityAmount = round2(maturityCorpus),
            schedule = schedule
        )
    }

    private fun parseDate(dateStr: String): LocalDate {
        return try {
            LocalDate.parse(dateStr.take(10))
        } catch (e: Exception) {
            LocalDate.now()
        }
    }

    private fun round2(value: Double): Double {
        if (value.isNaN() || value.isInfinite()) return 0.0
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}
