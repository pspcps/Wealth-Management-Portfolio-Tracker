package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val groupType: String, // "CASH", "INVESTMENT", "RETIREMENT", "BENEFIT", "CUSTOM"
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0,
    val description: String = ""
)

@Entity(tableName = "assets")
data class AssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: String,
    val name: String,
    val notes: String = "",
    val status: String = "ACTIVE", // "ACTIVE", "INACTIVE", "CLOSED"
    val initialInvestedAmount: Double = 0.0,
    val investmentDate: String = "", // "YYYY-MM-DD" initial investment/purchase date
    val createdAt: Long = System.currentTimeMillis(),
    val interestRate: Double = 0.0, // e.g. 7.5 for 7.5% p.a.
    val payoutFrequency: String = "NONE", // "NONE", "MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY", "AT_MATURITY"
    val payoutType: String = "CUMULATIVE", // "CUMULATIVE", "INTEREST_ONLY", "INTEREST_AND_PRINCIPAL"
    val principalPayoutPercent: Double = 0.0, // e.g. 5.0 for 5% quarterly/annual return of invested principal
    val principalPayoutFrequency: String = "QUARTERLY", // "MONTHLY", "QUARTERLY", "ANNUALLY"
    val maturityDate: String = "", // "YYYY-MM-DD"
    val compoundingFrequency: String = "QUARTERLY", // "MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY"
    val installmentAmount: Double = 0.0, // for RD: e.g. 5000 / month or quarter
    val installmentFrequency: String = "MONTHLY", // for RD: "MONTHLY", "QUARTERLY", "ANNUALLY"
    val tenureMonths: Int = 0 // duration in months
)

@Entity(tableName = "monthly_snapshots")
data class MonthlySnapshotEntity(
    @PrimaryKey
    val id: String, // e.g. "2026-08"
    val year: Int,
    val month: Int, // 1 to 12
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "asset_monthly_values",
    primaryKeys = ["assetId", "snapshotId"]
)
data class AssetMonthlyValueEntity(
    val assetId: Long,
    val snapshotId: String, // e.g. "2026-08"
    val value: Double,
    val investedAmount: Double = 0.0, // amount invested / principal cost
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val notes: String = ""
)

@Entity(tableName = "expense_categories")
data class ExpenseCategoryEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val iconName: String,
    val colorHex: String,
    val isDefault: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val snapshotId: String, // "2026-08"
    val expenseCategoryId: String,
    val amount: Double,
    val title: String,
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = ""
)

@Entity(tableName = "portfolio_views")
data class PortfolioViewEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val isDefault: Boolean = false,
    val includedCategoryIds: String // Comma-separated category IDs or "*" for all
)

@Entity(tableName = "recurring_cash_flows")
data class RecurringCashFlowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String, // e.g., "Parag Parikh Flexi Cap SIP", "HDFC Salary Account", "Meal Coupon (Pluxee)", "HDFC 3-Yr RD", "Edelweiss Bond Payout"
    val type: String = "SIP", // "SIP", "RD", "INTEREST_PAYOUT", "PRINCIPAL_PAYOUT", "SALARY", "MEAL_COUPON", "RECURRING_INVESTMENT", "PPF_EPF_CONTRIBUTION", "OTHER"
    val categoryId: String = "mutual_funds", // linked category ID
    val assetId: Long? = null, // optional linked asset ID
    val monthlyAmount: Double = 0.0, // normalized monthly amount
    val startMonth: String, // "YYYY-MM" e.g. "2025-01"
    val endMonth: String? = null, // "YYYY-MM" or null if ongoing
    val dayOfMonth: Int = 5,
    val isActive: Boolean = true,
    val notes: String = "",
    val frequency: String = "MONTHLY", // "MONTHLY", "QUARTERLY", "HALF_YEARLY", "ANNUALLY"
    val installmentAmount: Double = 0.0, // actual installment or periodic payout amount (before monthly normalization)
    val interestRate: Double = 0.0, // interest rate % p.a.
    val principalPayoutPercent: Double = 0.0 // amortizing % return of principal per period
)

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String = "",
    val loanType: String = "HOME_LOAN", // HOME_LOAN, CAR_LOAN, PERSONAL_LOAN, EDUCATION_LOAN, CREDIT_CARD_EMI, GOLD_LOAN, OTHER
    val lenderBank: String = "",
    val accountNumber: String = "",
    val originalPrincipal: Double = 0.0,
    val currentOutstanding: Double = 0.0,
    val annualInterestRate: Double = 8.5,
    val totalTenureMonths: Int = 120,
    val remainingTenureMonths: Int = 60,
    val monthlyEmi: Double = 0.0,
    val emiDebitDay: Int = 5,
    val startDate: String = "",
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "insurance_policies")
data class InsuranceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val policyName: String = "",
    val insuranceType: String = "TERM_LIFE", // TERM_LIFE, HEALTH, MOTOR, CRITICAL_ILLNESS, HOME_PROPERTY, ACCIDENTAL, OTHER
    val providerName: String = "",
    val policyNumber: String = "",
    val sumInsured: Double = 0.0,
    val annualPremium: Double = 0.0,
    val premiumFrequency: String = "ANNUAL", // ANNUAL, HALF_YEARLY, QUARTERLY, MONTHLY
    val renewalDate: String = "", // "YYYY-MM-DD"
    val nomineeName: String = "",
    val insuredPerson: String = "",
    val notes: String = "",
    val isActive: Boolean = true
)

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bankName: String = "",       // "ICICI Bank", "HDFC Bank", "Axis Bank", "Citi Bank", "IndusInd Bank", "Kotak Mahindra Bank", "IDFC FIRST Bank", "AU Small Finance Bank"
    val cardName: String = "",       // "Amazon Pay", "MakeMyTrip", "Swiggy", "Tata Neu", "MoneyBack+", "Flipkart", "My Zone", "Neo", "Salary Say", "Legend", "Lounge", "Millennia", "RuPay", "LIT"
    val cardType: String = "Cashback", // "Cashback", "Shopping", "Travel", "Rewards", "UPI RuPay", "Lifestyle", "Fuel"
    val last4Digits: String = "0000",
    val creditLimit: Double = 100000.0,
    val availableLimit: Double = 100000.0,
    val totalDue: Double = 0.0,
    val minDue: Double = 0.0,
    val dueDate: String = "",        // "YYYY-MM-DD"
    val statementDate: String = "",  // "YYYY-MM-DD"
    val billingCycleDay: Int = 15,   // Statement generation day of month
    val paymentDueDays: Int = 20,    // Days after statement date
    val savedPassword: String = "",  // Saved password for PDF statements
    val cardNetwork: String = "VISA", // VISA, Mastercard, RuPay, Diners, Amex
    val colorHex: String = "#1E293B",
    val isActive: Boolean = true,
    val notes: String = ""
)

@Entity(
    tableName = "credit_card_statements",
    foreignKeys = [
        ForeignKey(
            entity = CreditCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["cardId"])]
)
data class CreditCardStatementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val cardId: Long,
    val statementDate: String = "",     // "YYYY-MM-DD"
    val dueDate: String = "",           // "YYYY-MM-DD"
    val totalDue: Double = 0.0,
    val minDue: Double = 0.0,
    val spends: Double = 0.0,
    val payments: Double = 0.0,
    val cashbackEarned: Double = 0.0,
    val rewardPoints: Int = 0,
    val availableLimit: Double = 0.0,
    val totalCreditLimit: Double = 0.0,
    val fileName: String = "",
    val isPaid: Boolean = false,
    val paidDate: String? = null,
    val paidAmount: Double? = null,
    val notes: String = ""
)

@Entity(tableName = "cashback_refunds")
data class CashbackRefundEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String = "CASHBACK",       // "CASHBACK" or "REFUND"
    val title: String = "",              // e.g., "Amazon 5% Cashback", "Swiggy Order Refund"
    val amount: Double = 0.0,
    val source: String = "",             // e.g., "ICICI Amazon Pay", "HDFC Swiggy", "Axis Flipkart", "Zomato", "Myntra", "IRCTC", "Amazon"
    val cardId: Long? = null,            // Optional link to CreditCardEntity
    val status: String = "PENDING",      // "PENDING" or "RECEIVED"
    val expectedDate: String = "",       // "YYYY-MM-DD"
    val receivedDate: String? = null,    // "YYYY-MM-DD"
    val referenceNumber: String = "",    // ARN / Txn ID / Order Ref
    val category: String = "Shopping",   // Shopping, Travel, Dining, Groceries, Utility, Other
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

