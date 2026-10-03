package com.example.domain.finance

import com.example.domain.model.LoanAmortizationScheduleItem
import com.example.domain.model.PrepaymentScenario
import kotlin.math.pow
import kotlin.math.max

object LoanCalculators {

    fun calculateEmi(principal: Double, annualRatePercent: Double, tenureMonths: Int): Double {
        if (principal <= 0.0 || tenureMonths <= 0) return 0.0
        if (annualRatePercent <= 0.0) return principal / tenureMonths
        val r = (annualRatePercent / 100.0) / 12.0
        val factor = (1.0 + r).pow(tenureMonths.toDouble())
        return principal * r * factor / (factor - 1.0)
    }

    fun generateAmortizationSchedule(
        principal: Double,
        annualRatePercent: Double,
        tenureMonths: Int,
        maxItems: Int = 120
    ): List<LoanAmortizationScheduleItem> {
        if (principal <= 0.0 || tenureMonths <= 0) return emptyList()
        val r = (annualRatePercent / 100.0) / 12.0
        val emi = calculateEmi(principal, annualRatePercent, tenureMonths)
        var balance = principal
        val schedule = mutableListOf<LoanAmortizationScheduleItem>()

        val limit = minOf(tenureMonths, maxItems)
        for (month in 1..limit) {
            val interest = balance * r
            val principalPart = if (month == tenureMonths) balance else (emi - interest).coerceAtMost(balance)
            val ending = max(0.0, balance - principalPart)

            schedule.add(
                LoanAmortizationScheduleItem(
                    monthNumber = month,
                    beginningBalance = balance,
                    emi = emi,
                    principalComponent = principalPart,
                    interestComponent = interest,
                    endingBalance = ending
                )
            )
            balance = ending
            if (balance <= 0.01) break
        }
        return schedule
    }

    fun calculatePrepaymentImpact(
        currentBalance: Double,
        annualRatePercent: Double,
        currentRemainingTenureMonths: Int,
        currentMonthlyEmi: Double,
        extraMonthlyPayment: Double = 0.0,
        lumpSumPayment: Double = 0.0
    ): PrepaymentScenario {
        if (currentBalance <= 0.0 || currentRemainingTenureMonths <= 0) {
            return PrepaymentScenario()
        }

        val r = (annualRatePercent / 100.0) / 12.0
        val standardEmi = if (currentMonthlyEmi > 0) currentMonthlyEmi else calculateEmi(currentBalance, annualRatePercent, currentRemainingTenureMonths)

        // Baseline total interest
        var tempBalance = currentBalance
        var baselineTotalInterest = 0.0
        for (m in 1..currentRemainingTenureMonths) {
            val intPart = tempBalance * r
            baselineTotalInterest += intPart
            val princPart = (standardEmi - intPart).coerceAtMost(tempBalance)
            tempBalance = max(0.0, tempBalance - princPart)
            if (tempBalance <= 0.01) break
        }

        // Scenario with lump sum and extra monthly
        var scenarioBalance = max(0.0, currentBalance - lumpSumPayment)
        val scenarioMonthly = standardEmi + extraMonthlyPayment
        var scenarioMonths = 0
        var scenarioTotalInterest = 0.0

        while (scenarioBalance > 0.01 && scenarioMonths < 600) {
            scenarioMonths++
            val intPart = scenarioBalance * r
            scenarioTotalInterest += intPart
            val princPart = (scenarioMonthly - intPart).coerceAtMost(scenarioBalance)
            scenarioBalance = max(0.0, scenarioBalance - princPart)
        }

        val monthsSaved = max(0, currentRemainingTenureMonths - scenarioMonths)
        val interestSaved = max(0.0, baselineTotalInterest - scenarioTotalInterest)

        return PrepaymentScenario(
            monthlyExtraPrepayment = extraMonthlyPayment,
            oneTimeLumpSum = lumpSumPayment,
            revisedTenureMonths = scenarioMonths,
            monthsSaved = monthsSaved,
            totalInterestSaved = interestSaved
        )
    }
}
