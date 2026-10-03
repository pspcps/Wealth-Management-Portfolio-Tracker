package com.example.data.local

import com.example.data.local.entity.*

object DatabaseInitializer {

    fun getDefaultCategories(): List<CategoryEntity> {
        return listOf(
            // Cash & Banking
            CategoryEntity("bank_balance", "Bank Balance", "CASH", "account_balance", "#3B82F6", true, 10, "Primary salary and checking accounts"),
            CategoryEntity("savings_account", "Savings Account", "CASH", "savings", "#2563EB", true, 11, "Emergency savings and high-yield deposits"),
            CategoryEntity("cash", "Cash in Hand", "CASH", "payments", "#0EA5E9", true, 12, "Physical cash"),
            CategoryEntity("current_account", "Current Account", "CASH", "account_balance_wallet", "#1D4ED8", true, 13, "Business or current account"),
            CategoryEntity("other_cash", "Other Cash Reserves", "CASH", "wallet", "#60A5FA", true, 14, "Other liquid cash reserves"),

            // Investments
            CategoryEntity("mutual_funds", "Mutual Funds (MF)", "INVESTMENT", "pie_chart", "#8B5CF6", true, 20, "SIP & Lump-sum equity/debt mutual funds"),
            CategoryEntity("indian_stocks", "Indian Stocks", "INVESTMENT", "trending_up", "#10B981", true, 21, "Direct equities NSE/BSE"),
            CategoryEntity("us_stocks", "US Stocks & Global", "INVESTMENT", "public", "#06B6D4", true, 22, "US equity, S&P 500 & global ETFs"),
            CategoryEntity("digital_gold", "Digital Gold / SGB", "INVESTMENT", "monetization_on", "#F59E0B", true, 23, "24K Digital Gold / Sovereign Gold Bonds"),
            CategoryEntity("physical_gold", "Physical Gold & Silver", "INVESTMENT", "diamond", "#D97706", true, 24, "Gold coins, bars & jewelry value"),
            CategoryEntity("fixed_deposits", "Fixed Deposits (FD)", "INVESTMENT", "lock", "#EC4899", true, 25, "Bank & corporate term deposits"),
            CategoryEntity("recurring_deposits", "Recurring Deposits (RD)", "INVESTMENT", "repeat", "#DB2777", true, 26, "Monthly RD accounts"),
            CategoryEntity("bonds", "Bonds & G-Sec", "INVESTMENT", "receipt_long", "#6366F1", true, 27, "Corporate bonds, SDL & Govt securities"),
            CategoryEntity("real_estate", "Real Estate & Property", "INVESTMENT", "home", "#0D9488", true, 28, "Residential / commercial real estate"),
            CategoryEntity("crypto", "Crypto & Web3", "INVESTMENT", "currency_bitcoin", "#F59E0B", true, 29, "Bitcoin, Ethereum & crypto holdings"),
            CategoryEntity("other_investments", "Other Investments / REITs", "INVESTMENT", "account_tree", "#A855F7", true, 30, "REITs, InvITs, commodities & angel funds"),

            // Retirement / Long-Term
            CategoryEntity("epf", "EPF & VPF", "RETIREMENT", "assured_workload", "#14B8A6", true, 40, "Employees' Provident Fund & Voluntary PF"),
            CategoryEntity("nps", "NPS (National Pension)", "RETIREMENT", "security", "#059669", true, 41, "National Pension System Tier 1 & 2"),
            CategoryEntity("ppf", "PPF", "RETIREMENT", "shield", "#0D9488", true, 42, "Public Provident Fund (15-yr lock-in)"),
            CategoryEntity("child_plan", "Child Plan / Sukanya", "RETIREMENT", "child_care", "#F43F5E", true, 43, "Child education, SSY & marriage fund"),
            CategoryEntity("retirement_plans", "Retirement Annuity", "RETIREMENT", "elderly", "#047857", true, 44, "Pension & superannuation annuity"),
            CategoryEntity("other_retirement", "Other Retirement Lock-ins", "RETIREMENT", "umbrella", "#0F766E", true, 45, "Other long-term retirement plans"),

            // Benefits / Stored Value
            CategoryEntity("meal_card", "Meal Card / Sodexo / Pluxee", "BENEFIT", "restaurant", "#FB923C", true, 50, "Pluxee / Sodexo food allowance balance"),
            CategoryEntity("wallet_balance", "Wallets & Stored Balance", "BENEFIT", "credit_card", "#F97316", true, 51, "Amazon Pay, Paytm, Fastag wallets"),
            CategoryEntity("other_stored_value", "Other Vouchers & Credits", "BENEFIT", "card_giftcard", "#EA580C", true, 52, "Store credits, gift cards & travel points")
        )
    }

    fun getDefaultPortfolioViews(): List<PortfolioViewEntity> {
        return listOf(
            PortfolioViewEntity(
                id = "view_complete",
                name = "Complete Portfolio",
                isDefault = true,
                includedCategoryIds = "*" // Everything
            ),
            PortfolioViewEntity(
                id = "view_bank_bonds_fd",
                name = "Bank + Bonds + FD",
                isDefault = true,
                includedCategoryIds = "bank_balance,savings_account,bonds,fixed_deposits,recurring_deposits"
            ),
            PortfolioViewEntity(
                id = "view_liquid",
                name = "Liquid Assets",
                isDefault = true,
                includedCategoryIds = "bank_balance,savings_account,cash,wallet_balance,meal_card,fixed_deposits"
            ),
            PortfolioViewEntity(
                id = "view_equity_growth",
                name = "Equity & Stocks",
                isDefault = true,
                includedCategoryIds = "mutual_funds,indian_stocks,us_stocks,crypto,other_investments"
            ),
            PortfolioViewEntity(
                id = "view_inv_epf",
                name = "Investment + EPF",
                isDefault = true,
                includedCategoryIds = "mutual_funds,indian_stocks,us_stocks,digital_gold,physical_gold,bonds,fixed_deposits,recurring_deposits,other_investments,epf"
            ),
            PortfolioViewEntity(
                id = "view_inv_only",
                name = "Investment Only",
                isDefault = true,
                includedCategoryIds = "mutual_funds,indian_stocks,us_stocks,digital_gold,physical_gold,bonds,fixed_deposits,recurring_deposits,real_estate,crypto,other_investments"
            ),
            PortfolioViewEntity(
                id = "view_retirement",
                name = "Retirement & Long-term",
                isDefault = true,
                includedCategoryIds = "epf,ppf,nps,child_plan,retirement_plans,other_retirement,digital_gold,physical_gold"
            )
        )
    }

    fun getDefaultExpenseCategories(): List<ExpenseCategoryEntity> {
        return listOf(
            ExpenseCategoryEntity("groceries", "Groceries & Supermarket", "shopping_cart", "#16A34A", true, 1),
            ExpenseCategoryEntity("food", "Food & Dining", "restaurant", "#F97316", true, 2),
            ExpenseCategoryEntity("rent", "Rent & Housing", "home", "#3B82F6", true, 3),
            ExpenseCategoryEntity("utilities", "Utilities & Bills", "bolt", "#EAB308", true, 4),
            ExpenseCategoryEntity("shopping", "Shopping", "shopping_bag", "#EC4899", true, 5),
            ExpenseCategoryEntity("travel", "Travel & Holidays", "flight", "#06B6D4", true, 6),
            ExpenseCategoryEntity("transport", "Transport & Fuel", "directions_car", "#6366F1", true, 7),
            ExpenseCategoryEntity("education", "Education & Courses", "school", "#8B5CF6", true, 8),
            ExpenseCategoryEntity("medical", "Medical & Healthcare", "local_hospital", "#EF4444", true, 9),
            ExpenseCategoryEntity("insurance", "Insurance Premiums", "health_and_safety", "#10B981", true, 10),
            ExpenseCategoryEntity("entertainment", "Entertainment & Leisure", "movie", "#F43F5E", true, 11),
            ExpenseCategoryEntity("family", "Family & Child", "family_restroom", "#14B8A6", true, 12),
            ExpenseCategoryEntity("subscriptions", "Subscriptions & Software", "subscriptions", "#A855F7", true, 13),
            ExpenseCategoryEntity("miscellaneous", "Miscellaneous", "more_horiz", "#64748B", true, 14)
        )
    }

    fun getDefaultRecurringCashFlows(): List<RecurringCashFlowEntity> {
        return listOf(
            RecurringCashFlowEntity(
                id = 1,
                name = "Parag Parikh Flexi Cap SIP",
                type = "SIP",
                categoryId = "mutual_funds",
                assetId = 3,
                monthlyAmount = 15000.0,
                startMonth = "2025-01",
                endMonth = null,
                dayOfMonth = 5,
                isActive = true,
                notes = "Core monthly wealth SIP"
            ),
            RecurringCashFlowEntity(
                id = 2,
                name = "HDFC Flexi Cap SIP",
                type = "SIP",
                categoryId = "mutual_funds",
                assetId = 4,
                monthlyAmount = 10000.0,
                startMonth = "2025-01",
                endMonth = null,
                dayOfMonth = 10,
                isActive = true,
                notes = "Large & Mid Cap equity SIP"
            ),
            RecurringCashFlowEntity(
                id = 3,
                name = "SBI Small Cap SIP",
                type = "SIP",
                categoryId = "mutual_funds",
                assetId = 5,
                monthlyAmount = 5000.0,
                startMonth = "2025-06",
                endMonth = null,
                dayOfMonth = 15,
                isActive = true,
                notes = "Small cap alpha SIP"
            ),
            RecurringCashFlowEntity(
                id = 4,
                name = "Tech Corporate Monthly Salary",
                type = "SALARY",
                categoryId = "bank_balance",
                assetId = 1,
                monthlyAmount = 165000.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 1,
                isActive = true,
                notes = "Monthly net take-home salary credit"
            ),
            RecurringCashFlowEntity(
                id = 5,
                name = "Pluxee / Sodexo Meal Allowance",
                type = "MEAL_COUPON",
                categoryId = "meal_card",
                assetId = 17,
                monthlyAmount = 3000.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 1,
                isActive = true,
                notes = "Monthly corporate tax-exempt meal coupon credit"
            ),
            RecurringCashFlowEntity(
                id = 6,
                name = "NPS Tier-1 Monthly Contribution",
                type = "PPF_EPF_CONTRIBUTION",
                categoryId = "nps",
                assetId = 15,
                monthlyAmount = 5000.0,
                startMonth = "2025-01",
                endMonth = null,
                dayOfMonth = 20,
                isActive = true,
                notes = "Tax saving 80CCD(1B) pension SIP"
            ),
            // Recurring Fixed Monthly Expenses (Rent, Bills, Wifi)
            RecurringCashFlowEntity(
                id = 7,
                name = "House Rent",
                type = "FIXED_EXPENSE",
                categoryId = "rent",
                assetId = null,
                monthlyAmount = 25000.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 1,
                isActive = true,
                notes = "Fixed apartment monthly rent"
            ),
            RecurringCashFlowEntity(
                id = 8,
                name = "Airtel Fiber Broadband & Wifi",
                type = "FIXED_EXPENSE",
                categoryId = "utilities",
                assetId = null,
                monthlyAmount = 1199.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 5,
                isActive = true,
                notes = "High-speed home fiber connection"
            ),
            RecurringCashFlowEntity(
                id = 9,
                name = "Society Maintenance & Parking",
                type = "FIXED_EXPENSE",
                categoryId = "rent",
                assetId = null,
                monthlyAmount = 3500.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 3,
                isActive = true,
                notes = "Residential society quarterly/monthly maintenance"
            ),
            RecurringCashFlowEntity(
                id = 10,
                name = "OTT & Cloud Subscriptions",
                type = "FIXED_EXPENSE",
                categoryId = "subscriptions",
                assetId = null,
                monthlyAmount = 1499.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 10,
                isActive = true,
                notes = "Netflix, Prime & iCloud subscriptions"
            ),
            RecurringCashFlowEntity(
                id = 11,
                name = "Electricity & Piped Gas Bill",
                type = "FIXED_EXPENSE",
                categoryId = "utilities",
                assetId = null,
                monthlyAmount = 2800.0,
                startMonth = "2024-01",
                endMonth = null,
                dayOfMonth = 15,
                isActive = true,
                notes = "Power utility and domestic piped gas"
            )
        )
    }

    suspend fun ensureDefaultCategoriesExist(db: AppDatabase) {
        val existingCategories = db.portfolioDao().getAllCategoriesList()
        val existingCategoryIds = existingCategories.map { it.id }.toSet()
        val defaultCategories = getDefaultCategories()
        val missingCategories = defaultCategories.filter { !existingCategoryIds.contains(it.id) }
        if (missingCategories.isNotEmpty()) {
            db.portfolioDao().insertCategories(missingCategories)
        }

        val existingViews = db.portfolioDao().getAllPortfolioViewsList()
        val defaultViews = getDefaultPortfolioViews()
        val existingViewIds = existingViews.map { it.id }.toSet()
        val missingViews = defaultViews.filter { !existingViewIds.contains(it.id) }
        if (missingViews.isNotEmpty()) {
            db.portfolioDao().insertPortfolioViews(missingViews)
        }

        val existingExpenseCategories = db.expenseDao().getAllExpenseCategoriesList()
        val defaultExpenseCategories = getDefaultExpenseCategories()
        val existingExpenseCatIds = existingExpenseCategories.map { it.id }.toSet()
        val missingExpenseCats = defaultExpenseCategories.filter { !existingExpenseCatIds.contains(it.id) }
        if (missingExpenseCats.isNotEmpty()) {
            db.expenseDao().insertExpenseCategories(missingExpenseCats)
        }

        val existingFlows = db.portfolioDao().getAllRecurringCashFlowsList()
        val hasFixedExpenses = existingFlows.any { it.type == "FIXED_EXPENSE" || it.type == "RECURRING_EXPENSE" }
        if (!hasFixedExpenses) {
            val defaultFixed = getDefaultRecurringCashFlows().filter { it.type == "FIXED_EXPENSE" }
            defaultFixed.forEach { flow ->
                db.portfolioDao().insertRecurringCashFlow(flow.copy(id = 0))
            }
        }
    }

    suspend fun populateInitialData(db: AppDatabase) {
        // Only initialize default standard categories and views.
        // Do NOT automatically populate sample data so users start with a clean portfolio.
        ensureDefaultCategoriesExist(db)
    }

    suspend fun seedSampleData(db: AppDatabase) {
        // Clear first to prevent duplicate entries
        db.portfolioDao().clearAllAssetMonthlyValues()
        db.portfolioDao().clearAllSnapshots()
        db.portfolioDao().clearAllAssets()
        db.expenseDao().clearAllExpenses()
        db.portfolioDao().clearAllRecurringCashFlows()

        ensureDefaultCategoriesExist(db)
        
        // Create sample assets with initial invested amounts
        val assets = listOf(
            AssetEntity(id = 1, categoryId = "bank_balance", name = "HDFC Salary Account", notes = "Primary account", initialInvestedAmount = 140000.0, investmentDate = "2024-01-01"),
            AssetEntity(id = 2, categoryId = "bank_balance", name = "ICICI Savings", notes = "Emergency fund cash", initialInvestedAmount = 60000.0, investmentDate = "2024-01-01"),
            AssetEntity(id = 3, categoryId = "mutual_funds", name = "Parag Parikh Flexi Cap", notes = "Core equity SIP ₹15k", initialInvestedAmount = 240000.0, investmentDate = "2024-01-15"),
            AssetEntity(id = 4, categoryId = "mutual_funds", name = "HDFC Flexi Cap", notes = "SIP ₹10k", initialInvestedAmount = 190000.0, investmentDate = "2024-03-10"),
            AssetEntity(id = 5, categoryId = "mutual_funds", name = "SBI Small Cap", notes = "Small cap alpha SIP ₹5k", initialInvestedAmount = 130000.0, investmentDate = "2024-04-05"),
            AssetEntity(id = 6, categoryId = "indian_stocks", name = "Reliance Industries", notes = "100 shares", initialInvestedAmount = 135000.0, investmentDate = "2023-11-20"),
            AssetEntity(id = 7, categoryId = "indian_stocks", name = "TCS & Infosys", notes = "IT basket", initialInvestedAmount = 120000.0, investmentDate = "2023-12-15"),
            AssetEntity(id = 8, categoryId = "indian_stocks", name = "HDFC Bank", notes = "Private banking", initialInvestedAmount = 125000.0, investmentDate = "2024-02-10"),
            AssetEntity(id = 9, categoryId = "us_stocks", name = "Apple (AAPL)", notes = "Tech holdings", initialInvestedAmount = 95000.0, investmentDate = "2023-09-18"),
            AssetEntity(id = 10, categoryId = "us_stocks", name = "Vanguard S&P 500 (VOO)", notes = "US Index", initialInvestedAmount = 115000.0, investmentDate = "2024-01-20"),
            AssetEntity(id = 11, categoryId = "digital_gold", name = "Sovereign Gold Bonds (SGB)", notes = "RBI SGB tranches", initialInvestedAmount = 70000.0, investmentDate = "2023-10-12"),
            AssetEntity(id = 12, categoryId = "bonds", name = "Govt 10-Yr G-Sec Bonds", notes = "Yield 7.2%", initialInvestedAmount = 175000.0, investmentDate = "2024-01-05"),
            AssetEntity(id = 13, categoryId = "fixed_deposits", name = "HDFC 1-Year FD", notes = "Interest reinvestment", initialInvestedAmount = 360000.0, investmentDate = "2025-01-01"),
            AssetEntity(id = 14, categoryId = "epf", name = "EPF & VPF Accumulation", notes = "Employer & Employee", initialInvestedAmount = 560000.0, investmentDate = "2022-04-01"),
            AssetEntity(id = 15, categoryId = "nps", name = "NPS Tier-1 (Active Choice 75% Equity)", notes = "Tier 1 PRAN", initialInvestedAmount = 200000.0, investmentDate = "2023-05-15"),
            AssetEntity(id = 16, categoryId = "child_plan", name = "Sukanya / Child Education Fund", notes = "Long term compound", initialInvestedAmount = 145000.0, investmentDate = "2023-08-10"),
            AssetEntity(id = 17, categoryId = "meal_card", name = "Pluxee / Sodexo Meal Card", notes = "Monthly corporate credit", initialInvestedAmount = 18000.0, investmentDate = "2024-01-01")
        )
        db.portfolioDao().insertAssets(assets)

        // Seed default recurring cash flows & SIPs
        db.portfolioDao().insertRecurringCashFlows(getDefaultRecurringCashFlows())

        // Snapshots from Jan 2026 to Aug 2026
        val months = listOf(
            "2026-01" to (2026 to 1),
            "2026-02" to (2026 to 2),
            "2026-03" to (2026 to 3),
            "2026-04" to (2026 to 4),
            "2026-05" to (2026 to 5),
            "2026-06" to (2026 to 6),
            "2026-07" to (2026 to 7),
            "2026-08" to (2026 to 8)
        )

        val snapshots = months.map { (id, pair) ->
            MonthlySnapshotEntity(id = id, year = pair.first, month = pair.second, notes = "Monthly snapshot for $id")
        }
        db.portfolioDao().insertSnapshots(snapshots)

        // Realistic values growth per month
        val baseValues = mapOf(
            1L to (140000.0 to 140000.0), // (Current, Invested)
            2L to (60000.0 to 60000.0),
            3L to (320000.0 to 240000.0),
            4L to (240000.0 to 190000.0),
            5L to (180000.0 to 130000.0),
            6L to (170000.0 to 135000.0),
            7L to (150000.0 to 120000.0),
            8L to (140000.0 to 125000.0),
            9L to (130000.0 to 95000.0),
            10L to (150000.0 to 115000.0),
            11L to (95000.0 to 70000.0),
            12L to (180000.0 to 175000.0),
            13L to (380000.0 to 360000.0),
            14L to (680000.0 to 560000.0),
            15L to (260000.0 to 200000.0),
            16L to (180000.0 to 145000.0),
            17L to (18000.0 to 18000.0)
        )

        val monthFactors = listOf(
            "2026-01" to (1.000 to 1.000),
            "2026-02" to (1.015 to 1.008),
            "2026-03" to (1.032 to 1.016),
            "2026-04" to (1.048 to 1.025),
            "2026-05" to (1.065 to 1.033),
            "2026-06" to (1.082 to 1.042),
            "2026-07" to (1.104 to 1.050),
            "2026-08" to (1.128 to 1.060)
        )

        val assetValues = mutableListOf<AssetMonthlyValueEntity>()
        monthFactors.forEach { (snapshotId, factors) ->
            val (marketFactor, investedFactor) = factors
            baseValues.forEach { (assetId, pair) ->
                val (baseCurrent, baseInvested) = pair
                val variation = when (assetId) {
                    3L, 4L, 5L -> marketFactor * 1.03
                    6L, 7L, 8L -> marketFactor * 1.02
                    11L -> marketFactor * 1.05
                    14L, 15L -> marketFactor * 1.015
                    else -> marketFactor
                }
                val roundedValue = Math.round(baseCurrent * variation / 500.0) * 500.0
                val roundedInvested = Math.round(baseInvested * investedFactor / 500.0) * 500.0
                assetValues.add(
                    AssetMonthlyValueEntity(
                        assetId = assetId,
                        snapshotId = snapshotId,
                        value = roundedValue,
                        investedAmount = roundedInvested
                    )
                )
            }
        }
        db.portfolioDao().insertAssetValues(assetValues)

        // Seed sample expenses
        val sampleExpenses = mutableListOf<ExpenseEntity>()
        val expenseBases = mapOf(
            "rent" to (25000.0 to "House Rent"),
            "food" to (15000.0 to "Groceries & Dining"),
            "utilities" to (7500.0 to "Electricity, Wifi & Gas"),
            "shopping" to (12000.0 to "Apparel & Electronics"),
            "travel" to (8000.0 to "Weekend Getaway"),
            "transport" to (6000.0 to "Fuel & Metro"),
            "subscriptions" to (3500.0 to "Cloud & Streaming"),
            "miscellaneous" to (5500.0 to "Household supplies")
        )

        months.forEachIndexed { index, (snapshotId, _) ->
            val expenseFactor = 1.0 + (index * 0.02)
            expenseBases.forEach { (catId, pair) ->
                val (amt, title) = pair
                val roundedAmt = Math.round(amt * expenseFactor / 100.0) * 100.0
                sampleExpenses.add(
                    ExpenseEntity(
                        snapshotId = snapshotId,
                        expenseCategoryId = catId,
                        amount = roundedAmt,
                        title = title,
                        dateMillis = System.currentTimeMillis() - ((8 - index) * 30L * 86400000L)
                    )
                )
            }
        }
        db.expenseDao().insertExpenses(sampleExpenses)
        
        // Seed sample loans & insurance policies
        db.loanDao().clearAllLoans()
        db.loanDao().insertLoans(getDefaultSampleLoans())
        db.insuranceDao().clearAllInsurances()
        db.insuranceDao().insertInsurances(getDefaultSampleInsurances())

        // Seed user's 14 credit cards if table is empty
        if (db.creditCardDao().getAllCardsList().isEmpty()) {
            db.creditCardDao().insertCards(getDefaultUserCreditCards())
        }

        // Seed cashback & refund items if table is empty
        if (db.cashbackRefundDao().getAllItemsList().isEmpty()) {
            db.cashbackRefundDao().insertItems(getDefaultCashbackRefunds())
        }
    }

    fun getDefaultSampleLoans(): List<LoanEntity> {
        return listOf(
            LoanEntity(
                id = 1,
                name = "HDFC Home Loan - 3BHK",
                loanType = "HOME_LOAN",
                lenderBank = "HDFC Bank",
                accountNumber = "HL-908234",
                originalPrincipal = 5500000.0,
                currentOutstanding = 4120000.0,
                annualInterestRate = 8.5,
                totalTenureMonths = 240,
                remainingTenureMonths = 168,
                monthlyEmi = 47750.0,
                emiDebitDay = 5,
                startDate = "2020-04",
                notes = "Primary residence flat home loan. Eligible for Sec 24 & 80C tax deduction.",
                isActive = true
            ),
            LoanEntity(
                id = 2,
                name = "SBI Car Loan - Hyundai Creta",
                loanType = "CAR_LOAN",
                lenderBank = "State Bank of India",
                accountNumber = "SBI-AUTO-7712",
                originalPrincipal = 1100000.0,
                currentOutstanding = 480000.0,
                annualInterestRate = 8.9,
                totalTenureMonths = 60,
                remainingTenureMonths = 24,
                monthlyEmi = 22750.0,
                emiDebitDay = 10,
                startDate = "2023-01",
                notes = "Vehicle finance for family car.",
                isActive = true
            ),
            LoanEntity(
                id = 3,
                name = "Axis Bank Education Loan",
                loanType = "EDUCATION_LOAN",
                lenderBank = "Axis Bank",
                accountNumber = "AX-EDU-3310",
                originalPrincipal = 1500000.0,
                currentOutstanding = 210000.0,
                annualInterestRate = 9.25,
                totalTenureMonths = 84,
                remainingTenureMonths = 12,
                monthlyEmi = 19200.0,
                emiDebitDay = 7,
                startDate = "2020-08",
                notes = "Higher education loan with full interest tax deduction under Section 80E.",
                isActive = true
            )
        )
    }

    fun getDefaultSampleInsurances(): List<InsuranceEntity> {
        return listOf(
            InsuranceEntity(
                id = 1,
                policyName = "HDFC Life Click 2 Protect 3D Plus",
                insuranceType = "TERM_LIFE",
                providerName = "HDFC Life",
                policyNumber = "HDFC-TL-9021882",
                sumInsured = 15000000.0, // 1.5 Crore
                annualPremium = 18500.0,
                premiumFrequency = "ANNUAL",
                renewalDate = "2026-11-20",
                nomineeName = "Spouse",
                insuredPerson = "Self",
                notes = "Pure Term life protection with ₹1.5 Cr sum assured & terminal illness waiver.",
                isActive = true
            ),
            InsuranceEntity(
                id = 2,
                policyName = "Niva Bupa ReAssure 2.0 Family Floater",
                insuranceType = "HEALTH",
                providerName = "Niva Bupa Health Insurance",
                policyNumber = "NB-HLTH-663812",
                sumInsured = 2500000.0, // 25 Lakhs
                annualPremium = 24600.0,
                premiumFrequency = "ANNUAL",
                renewalDate = "2026-09-28",
                nomineeName = "Spouse",
                insuredPerson = "Self + Spouse + 1 Child",
                notes = "Comprehensive cashless family floater with unlimited restore & Lock the Clock age lock. Contact: 1860-500-8888",
                isActive = true
            ),
            InsuranceEntity(
                id = 3,
                policyName = "ICICI Lombard Comprehensive Car Policy",
                insuranceType = "MOTOR",
                providerName = "ICICI Lombard",
                policyNumber = "ICICI-MOT-449102",
                sumInsured = 980000.0,
                annualPremium = 14200.0,
                premiumFrequency = "ANNUAL",
                renewalDate = "2027-01-15",
                nomineeName = "Self",
                insuredPerson = "Creta SX (KA-01-MF-7890)",
                notes = "Zero depreciation bumper-to-bumper car policy with engine protect & NCB protection.",
                isActive = true
            ),
            InsuranceEntity(
                id = 4,
                policyName = "Care Health Shield Personal Accident",
                insuranceType = "CRITICAL_ILLNESS",
                providerName = "Care Health Insurance",
                policyNumber = "CARE-PA-882190",
                sumInsured = 5000000.0, // 50 Lakhs
                annualPremium = 6200.0,
                premiumFrequency = "ANNUAL",
                renewalDate = "2026-12-10",
                nomineeName = "Spouse",
                insuredPerson = "Self",
                notes = "Accidental death, permanent disability and child education support. Contact: 1800-102-4455",
                isActive = true
            )
        )
    }

    suspend fun restoreAllDefaultCategories(db: AppDatabase) {
        db.portfolioDao().insertCategories(getDefaultCategories())
        db.portfolioDao().insertPortfolioViews(getDefaultPortfolioViews())
        db.expenseDao().insertExpenseCategories(getDefaultExpenseCategories())
    }

    suspend fun clearAllUserData(db: AppDatabase) {
        db.portfolioDao().clearAllAssetMonthlyValues()
        db.portfolioDao().clearAllSnapshots()
        db.portfolioDao().clearAllAssets()
        db.expenseDao().clearAllExpenses()
        db.portfolioDao().clearAllRecurringCashFlows()
        db.loanDao().clearAllLoans()
        db.insuranceDao().clearAllInsurances()
        // Keep categories and views intact
        ensureDefaultCategoriesExist(db)
    }

    fun getDefaultUserCreditCards(): List<CreditCardEntity> {
        return listOf(
            // 1. ICICI Bank - Amazon Pay
            CreditCardEntity(
                id = 1,
                bankName = "ICICI Bank",
                cardName = "Amazon Pay",
                cardType = "Cashback",
                last4Digits = "6016",
                creditLimit = 140393.0,
                availableLimit = 132341.71,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-09-20",
                statementDate = "2026-09-02",
                billingCycleDay = 2,
                paymentDueDays = 18,
                savedPassword = "prak0105",
                cardNetwork = "VISA",
                colorHex = "#9A3412",
                isActive = true,
                notes = "Unlimited 5% cashback on Amazon. Statement has -₹1,646.84 CR credit balance"
            ),
            // 2. ICICI Bank - MakeMyTrip
            CreditCardEntity(
                id = 2,
                bankName = "ICICI Bank",
                cardName = "MakeMyTrip",
                cardType = "Travel",
                last4Digits = "8832",
                creditLimit = 150000.0,
                availableLimit = 150000.0,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-10-10",
                statementDate = "2026-09-20",
                billingCycleDay = 20,
                paymentDueDays = 20,
                savedPassword = "prak0105",
                cardNetwork = "Mastercard",
                colorHex = "#EA580C",
                isActive = true,
                notes = "MMT My Cash rewards on hotel & flight bookings"
            ),
            // 3. HDFC Bank - Swiggy
            CreditCardEntity(
                id = 3,
                bankName = "HDFC Bank",
                cardName = "Swiggy",
                cardType = "Cashback",
                last4Digits = "1449",
                creditLimit = 121000.0,
                availableLimit = 119260.0,
                totalDue = 1740.0,
                minDue = 200.0,
                dueDate = "2026-10-02",
                statementDate = "2026-09-12",
                billingCycleDay = 12,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "Mastercard",
                colorHex = "#F97316",
                isActive = true,
                notes = "10% cashback on Swiggy food, Instamart & Dineout"
            ),
            // 4. HDFC Bank - Tata Neu
            CreditCardEntity(
                id = 4,
                bankName = "HDFC Bank",
                cardName = "Tata Neu",
                cardType = "UPI RuPay",
                last4Digits = "6472",
                creditLimit = 200000.0,
                availableLimit = 191800.0,
                totalDue = 8200.0,
                minDue = 500.0,
                dueDate = "2026-10-08",
                statementDate = "2026-09-18",
                billingCycleDay = 18,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "RuPay",
                colorHex = "#4F46E5",
                isActive = true,
                notes = "5% NeuCoins on Tata Neu & linked UPI spends"
            ),
            // 5. HDFC Bank - MoneyBack+
            CreditCardEntity(
                id = 5,
                bankName = "HDFC Bank",
                cardName = "MoneyBack+",
                cardType = "Rewards",
                last4Digits = "5091",
                creditLimit = 100000.0,
                availableLimit = 100000.0,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-10-12",
                statementDate = "2026-09-22",
                billingCycleDay = 22,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "VISA",
                colorHex = "#1E40AF",
                isActive = true,
                notes = "10X CashPoints on Amazon, Flipkart, Swiggy, BigBasket"
            ),
            // 6. Axis Bank - Flipkart
            CreditCardEntity(
                id = 6,
                bankName = "Axis Bank",
                cardName = "Flipkart",
                cardType = "Cashback",
                last4Digits = "3319",
                creditLimit = 200000.0,
                availableLimit = 189100.0,
                totalDue = 10900.0,
                minDue = 600.0,
                dueDate = "2026-10-04",
                statementDate = "2026-09-14",
                billingCycleDay = 14,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "VISA",
                colorHex = "#991B1B",
                isActive = true,
                notes = "5% unlimited cashback on Flipkart & Cleartrip"
            ),
            // 7. Axis Bank - My Zone
            CreditCardEntity(
                id = 7,
                bankName = "Axis Bank",
                cardName = "My Zone",
                cardType = "Entertainment",
                last4Digits = "7720",
                creditLimit = 120000.0,
                availableLimit = 120000.0,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-10-18",
                statementDate = "2026-09-28",
                billingCycleDay = 28,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "VISA",
                colorHex = "#BE185D",
                isActive = true,
                notes = "Buy 1 Get 1 free on Paytm Movies & complimentary SonyLIV"
            ),
            // 8. Axis Bank - Neo
            CreditCardEntity(
                id = 8,
                bankName = "Axis Bank",
                cardName = "Neo",
                cardType = "Shopping",
                last4Digits = "2539",
                creditLimit = 120000.0,
                availableLimit = 119714.08,
                totalDue = 285.92,
                minDue = 100.0,
                dueDate = "2026-09-30",
                statementDate = "2026-09-10",
                billingCycleDay = 10,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "Mastercard",
                colorHex = "#831843",
                isActive = true,
                notes = "Discounts on Amazon Pay utility recharges & Zomato"
            ),
            // 9. City Union Bank - SalarySe Level UP
            CreditCardEntity(
                id = 9,
                bankName = "City Union Bank",
                cardName = "SalarySe Level UP",
                cardType = "Cashback",
                last4Digits = "0867",
                creditLimit = 250000.0,
                availableLimit = 242113.63,
                totalDue = 7886.37,
                minDue = 394.31,
                dueDate = "2026-09-05",
                statementDate = "2026-08-18",
                billingCycleDay = 18,
                paymentDueDays = 18,
                savedPassword = "PRAK0105",
                cardNetwork = "RuPay",
                colorHex = "#0369A1",
                isActive = true,
                notes = "CUB SalarySe Level UP RuPay credit card"
            ),
            // 10. IndusInd Bank - Legend
            CreditCardEntity(
                id = 10,
                bankName = "IndusInd Bank",
                cardName = "Legend",
                cardType = "Lifestyle",
                last4Digits = "2717",
                creditLimit = 133000.0,
                availableLimit = 132788.0,
                totalDue = 212.0,
                minDue = 100.0,
                dueDate = "2026-10-05",
                statementDate = "2026-09-15",
                billingCycleDay = 15,
                paymentDueDays = 20,
                savedPassword = "prak0105",
                cardNetwork = "Mastercard",
                colorHex = "#854D0E",
                isActive = true,
                notes = "Complimentary airport lounge access and golf privileges"
            ),
            // 11. Kotak Mahindra Bank - Lounge
            CreditCardEntity(
                id = 11,
                bankName = "Kotak Mahindra Bank",
                cardName = "Lounge",
                cardType = "Travel",
                last4Digits = "4401",
                creditLimit = 150000.0,
                availableLimit = 150000.0,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-10-14",
                statementDate = "2026-09-24",
                billingCycleDay = 24,
                paymentDueDays = 20,
                savedPassword = "prak0105",
                cardNetwork = "VISA",
                colorHex = "#DC2626",
                isActive = true,
                notes = "Kotak League Platinum with domestic lounge access"
            ),
            // 12. IDFC FIRST Bank - Millennia
            CreditCardEntity(
                id = 12,
                bankName = "IDFC FIRST Bank",
                cardName = "Millennia",
                cardType = "Rewards",
                last4Digits = "3028",
                creditLimit = 175000.0,
                availableLimit = 175000.0,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-10-20",
                statementDate = "2026-09-30",
                billingCycleDay = 30,
                paymentDueDays = 20,
                savedPassword = "0105",
                cardNetwork = "VISA",
                colorHex = "#991B1B",
                isActive = true,
                notes = "Lifetime free card with 10X reward points on birthday spends"
            ),
            // 13. AU Small Finance Bank - InstaPay
            CreditCardEntity(
                id = 13,
                bankName = "AU Small Finance Bank",
                cardName = "InstaPay",
                cardType = "UPI RuPay",
                last4Digits = "8494",
                creditLimit = 45000.0,
                availableLimit = 29281.85,
                totalDue = 15718.15,
                minDue = 790.0,
                dueDate = "2026-10-05",
                statementDate = "2026-09-15",
                billingCycleDay = 15,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "RuPay",
                colorHex = "#047857",
                isActive = true,
                notes = "AU InstaPay Virtual RuPay credit card for UPI payments"
            ),
            // 14. AU Small Finance Bank - LIT
            CreditCardEntity(
                id = 14,
                bankName = "AU Small Finance Bank",
                cardName = "LIT",
                cardType = "Cashback",
                last4Digits = "6230",
                creditLimit = 100000.0,
                availableLimit = 100000.0,
                totalDue = 0.0,
                minDue = 0.0,
                dueDate = "2026-10-16",
                statementDate = "2026-09-26",
                billingCycleDay = 26,
                paymentDueDays = 20,
                savedPassword = "PRAK0105",
                cardNetwork = "VISA",
                colorHex = "#0D9488",
                isActive = true,
                notes = "Customizable features with 5% cashback on apparel and travel"
            )
        )
    }

    fun getDefaultCashbackRefunds(): List<CashbackRefundEntity> {
        return listOf(
            CashbackRefundEntity(
                id = 1,
                type = "CASHBACK",
                title = "Amazon",
                amount = 1450.0,
                source = "Amazon",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-09-10",
                receivedDate = "2026-09-10",
                referenceNumber = "",
                category = "Shopping",
                notes = ""
            ),
            CashbackRefundEntity(
                id = 2,
                type = "CASHBACK",
                title = "Swiggy",
                amount = 340.0,
                source = "Swiggy",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-09-15",
                receivedDate = "2026-09-15",
                referenceNumber = "",
                category = "Dining",
                notes = ""
            ),
            CashbackRefundEntity(
                id = 3,
                type = "CASHBACK",
                title = "Flipkart",
                amount = 680.0,
                source = "Flipkart",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-09-18",
                receivedDate = "2026-09-18",
                referenceNumber = "",
                category = "Shopping",
                notes = ""
            ),
            CashbackRefundEntity(
                id = 4,
                type = "CASHBACK",
                title = "Google Pay",
                amount = 410.0,
                source = "Google Pay",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-08-25",
                receivedDate = "2026-08-25",
                referenceNumber = "",
                category = "Groceries",
                notes = ""
            ),
            CashbackRefundEntity(
                id = 5,
                type = "REFUND",
                title = "MakeMyTrip",
                amount = 4850.0,
                source = "MakeMyTrip",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-09-20",
                receivedDate = "2026-09-20",
                referenceNumber = "",
                category = "Travel",
                notes = ""
            ),
            CashbackRefundEntity(
                id = 6,
                type = "REFUND",
                title = "Zomato",
                amount = 420.0,
                source = "Zomato",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-09-14",
                receivedDate = "2026-09-14",
                referenceNumber = "",
                category = "Dining",
                notes = ""
            ),
            CashbackRefundEntity(
                id = 7,
                type = "REFUND",
                title = "Amazon",
                amount = 2299.0,
                source = "Amazon",
                cardId = null,
                status = "RECEIVED",
                expectedDate = "2026-08-10",
                receivedDate = "2026-08-10",
                referenceNumber = "",
                category = "Shopping",
                notes = ""
            )
        )
    }
}
