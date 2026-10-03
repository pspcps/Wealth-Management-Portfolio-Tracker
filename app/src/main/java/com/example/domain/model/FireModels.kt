package com.example.domain.model

import java.util.Calendar

data class FireSettings(
    val expectedReturnRate: Double = 12.0, // Default 12% as requested
    val inflationRate: Double = 6.0,       // Default 6% as requested
    val currentAge: Int = 28,
    val targetFireAge: Int = 45,
    val monthlyExpenses: Double = 50000.0,
    val monthlyInvestment: Double = 50000.0,
    val swrRate: Double = 3.5,            // Safe Withdrawal Rate (3.5% = ~28.5x rule)
    val annualStepUpRate: Double = 8.0     // Annual SIP increase %
)

data class FireForecastResult(
    val settings: FireSettings,
    val currentNetWorth: Double,
    val annualExpenses: Double,
    val targetFireCorpus: Double,
    val leanFireCorpus: Double,
    val fatFireCorpus: Double,
    val progressPercentage: Double,
    val yearsToFire: Double,
    val projectedFireAge: Double,
    val projectedFireYear: Int,
    val monthlyPassiveIncomeNow: Double,
    val monthlyPassiveIncomeAtFire: Double,
    val coastFireCorpus: Double,
    val isCoastFireAchieved: Boolean,
    val isFireAchieved: Boolean,
    val realAnnualReturnRate: Double
)

object FireCalculator {
    fun calculateForecast(
        settings: FireSettings,
        currentNetWorth: Double
    ): FireForecastResult {
        val annualExpenses = settings.monthlyExpenses * 12.0
        val swrFraction = maxOf(0.01, settings.swrRate / 100.0)
        
        // Base target corpus in today's purchasing power
        val targetFireCorpus = annualExpenses / swrFraction
        val leanFireCorpus = (annualExpenses * 0.75) / swrFraction
        val fatFireCorpus = (annualExpenses * 1.50) / swrFraction

        val progressPct = if (targetFireCorpus > 0) {
            minOf(100.0, (currentNetWorth / targetFireCorpus) * 100.0)
        } else 0.0

        val isFireAchieved = currentNetWorth >= targetFireCorpus

        // Fisher equation for Real Return: (1 + r) / (1 + i) - 1
        val nominalR = settings.expectedReturnRate / 100.0
        val inflationI = settings.inflationRate / 100.0
        val realReturn = if (1.0 + inflationI > 0) ((1.0 + nominalR) / (1.0 + inflationI)) - 1.0 else nominalR - inflationI
        val monthlyRealRate = if (realReturn > -0.99) Math.pow(1.0 + realReturn, 1.0 / 12.0) - 1.0 else 0.0

        // Coast FIRE: Corpus required today such that compounding alone reaches target at targetFireAge
        val yearsToRetire = maxOf(1, settings.targetFireAge - settings.currentAge)
        val coastFireCorpus = if (realReturn > 0) {
            targetFireCorpus / Math.pow(1.0 + realReturn, yearsToRetire.toDouble())
        } else targetFireCorpus

        val isCoastFireAchieved = currentNetWorth >= coastFireCorpus

        // Calculate time to FIRE via monthly compounding simulation
        var months = 0
        var simCorpus = currentNetWorth
        var currentMonthlySip = settings.monthlyInvestment
        val stepUpFraction = settings.annualStepUpRate / 100.0

        if (isFireAchieved) {
            months = 0
        } else {
            val maxMonths = 12 * 60 // 60 years max cap
            while (simCorpus < targetFireCorpus && months < maxMonths) {
                // Compound existing portfolio by monthly real return
                simCorpus = simCorpus * (1.0 + monthlyRealRate) + currentMonthlySip
                months++
                // Step up SIP annually
                if (months % 12 == 0 && stepUpFraction > 0) {
                    currentMonthlySip *= (1.0 + stepUpFraction)
                }
            }
        }

        val yearsToFire = months / 12.0
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val projectedFireYear = currentYear + (months / 12)
        val projectedFireAge = settings.currentAge + yearsToFire

        val monthlyPassiveIncomeNow = (currentNetWorth * swrFraction) / 12.0
        val monthlyPassiveIncomeAtFire = settings.monthlyExpenses

        return FireForecastResult(
            settings = settings,
            currentNetWorth = currentNetWorth,
            annualExpenses = annualExpenses,
            targetFireCorpus = targetFireCorpus,
            leanFireCorpus = leanFireCorpus,
            fatFireCorpus = fatFireCorpus,
            progressPercentage = progressPct,
            yearsToFire = yearsToFire,
            projectedFireAge = projectedFireAge,
            projectedFireYear = projectedFireYear,
            monthlyPassiveIncomeNow = monthlyPassiveIncomeNow,
            monthlyPassiveIncomeAtFire = monthlyPassiveIncomeAtFire,
            coastFireCorpus = coastFireCorpus,
            isCoastFireAchieved = isCoastFireAchieved,
            isFireAchieved = isFireAchieved,
            realAnnualReturnRate = realReturn * 100.0
        )
    }
}
