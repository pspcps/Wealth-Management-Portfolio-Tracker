package com.example.domain.model

data class LoanPortfolioSummary(
    val totalOutstanding: Double = 0.0,
    val totalMonthlyEmi: Double = 0.0,
    val totalOriginalPrincipal: Double = 0.0,
    val activeLoansCount: Int = 0,
    val closedLoansCount: Int = 0,
    val weightedAverageInterestRate: Double = 0.0
)

data class LoanAmortizationScheduleItem(
    val monthNumber: Int,
    val beginningBalance: Double,
    val emi: Double,
    val principalComponent: Double,
    val interestComponent: Double,
    val endingBalance: Double
)

data class PrepaymentScenario(
    val monthlyExtraPrepayment: Double = 0.0,
    val oneTimeLumpSum: Double = 0.0,
    val revisedTenureMonths: Int = 0,
    val monthsSaved: Int = 0,
    val totalInterestSaved: Double = 0.0
)

data class InsuranceShieldSummary(
    val totalLifeCover: Double = 0.0,
    val totalHealthCover: Double = 0.0,
    val totalOtherCover: Double = 0.0,
    val totalAnnualPremiums: Double = 0.0,
    val activePoliciesCount: Int = 0,
    val policiesExpiringSoonCount: Int = 0,
    val hasLifeInsurance: Boolean = false,
    val hasHealthInsurance: Boolean = false,
    val isAdequatelyInsured: Boolean = false
)
