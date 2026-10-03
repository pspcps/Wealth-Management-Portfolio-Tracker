package com.example.util

import com.example.data.local.entity.*
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfile
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.FireSettingsRepository
import com.example.domain.model.FireSettings
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudBackupManager {

    suspend fun createFullBackupJson(
        repository: PortfolioRepository,
        userProfile: UserProfile? = null,
        fireSettings: FireSettings? = null
    ): String {
        val snapshots = repository.getAllSnapshotsList()
        val categories = repository.getAllCategoriesList()
        val assets = repository.getAllAssetsList()
        val assetValues = repository.getAllAssetValuesList()
        val recurringFlows = repository.getAllRecurringCashFlowsList()
        val views = repository.getAllPortfolioViewsList()
        val expenseCats = repository.getAllExpenseCategoriesList()
        val expenses = repository.getAllExpensesList()
        val loans = repository.getAllLoansList()
        val insurances = repository.getAllInsurancePoliciesList()
        val creditCards = repository.getAllCreditCardsList()
        val creditCardStatements = repository.getAllCreditCardStatementsList()
        val cashbackRefunds = repository.getAllCashbackRefundsList()

        val root = JSONObject()
        root.put("version", 2)
        root.put("app", "Wealth & Portfolio Tracker")
        root.put("backupType", "FULL_MASTER_BACKUP")
        root.put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
        root.put("timestamp", System.currentTimeMillis())
        root.put("totalSnapshotsCount", snapshots.size)

        // 0. User Profile
        if (userProfile != null) {
            val profObj = JSONObject()
            profObj.put("name", userProfile.name)
            profObj.put("email", userProfile.email)
            profObj.put("tagline", userProfile.tagline)
            profObj.put("targetNetWorth", userProfile.targetNetWorth)
            profObj.put("colorIndex", userProfile.colorIndex)
            profObj.put("dateOfBirth", userProfile.dateOfBirth)
            root.put("userProfile", profObj)
        }

        // 0.1 Fire Settings
        if (fireSettings != null) {
            val fireObj = JSONObject()
            fireObj.put("expectedReturnRate", fireSettings.expectedReturnRate)
            fireObj.put("inflationRate", fireSettings.inflationRate)
            fireObj.put("currentAge", fireSettings.currentAge)
            fireObj.put("targetFireAge", fireSettings.targetFireAge)
            fireObj.put("monthlyExpenses", fireSettings.monthlyExpenses)
            fireObj.put("monthlyInvestment", fireSettings.monthlyInvestment)
            fireObj.put("swrRate", fireSettings.swrRate)
            fireObj.put("annualStepUpRate", fireSettings.annualStepUpRate)
            root.put("fireSettings", fireObj)
        }

        // 1. Snapshots
        val snapshotsArr = JSONArray()
        snapshots.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("year", s.year)
            obj.put("month", s.month)
            obj.put("notes", s.notes)
            obj.put("updatedAt", s.updatedAt)
            snapshotsArr.put(obj)
        }
        root.put("snapshots", snapshotsArr)

        // 2. Categories
        val catsArr = JSONArray()
        categories.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("name", c.name)
            obj.put("groupType", c.groupType)
            obj.put("iconName", c.iconName)
            obj.put("colorHex", c.colorHex)
            obj.put("isDefault", c.isDefault)
            obj.put("orderIndex", c.orderIndex)
            obj.put("description", c.description)
            catsArr.put(obj)
        }
        root.put("categories", catsArr)

        // 3. Assets
        val assetsArr = JSONArray()
        assets.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("categoryId", a.categoryId)
            obj.put("name", a.name)
            obj.put("notes", a.notes)
            obj.put("status", a.status)
            obj.put("initialInvestedAmount", a.initialInvestedAmount)
            obj.put("investmentDate", a.investmentDate)
            obj.put("createdAt", a.createdAt)
            obj.put("interestRate", a.interestRate)
            obj.put("payoutFrequency", a.payoutFrequency)
            obj.put("payoutType", a.payoutType)
            obj.put("principalPayoutPercent", a.principalPayoutPercent)
            obj.put("principalPayoutFrequency", a.principalPayoutFrequency)
            obj.put("maturityDate", a.maturityDate)
            obj.put("compoundingFrequency", a.compoundingFrequency)
            obj.put("installmentAmount", a.installmentAmount)
            obj.put("installmentFrequency", a.installmentFrequency)
            obj.put("tenureMonths", a.tenureMonths)
            assetsArr.put(obj)
        }
        root.put("assets", assetsArr)

        // 4. Asset Values
        val valsArr = JSONArray()
        assetValues.forEach { v ->
            val obj = JSONObject()
            obj.put("assetId", v.assetId)
            obj.put("snapshotId", v.snapshotId)
            obj.put("value", v.value)
            obj.put("investedAmount", v.investedAmount)
            obj.put("contributions", v.contributions)
            obj.put("withdrawals", v.withdrawals)
            obj.put("notes", v.notes)
            valsArr.put(obj)
        }
        root.put("assetValues", valsArr)

        // 5. Recurring Flows
        val flowsArr = JSONArray()
        recurringFlows.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("type", f.type)
            obj.put("categoryId", f.categoryId)
            obj.put("assetId", f.assetId ?: JSONObject.NULL)
            obj.put("monthlyAmount", f.monthlyAmount)
            obj.put("startMonth", f.startMonth)
            obj.put("endMonth", f.endMonth ?: JSONObject.NULL)
            obj.put("dayOfMonth", f.dayOfMonth)
            obj.put("isActive", f.isActive)
            obj.put("notes", f.notes)
            obj.put("frequency", f.frequency)
            obj.put("installmentAmount", f.installmentAmount)
            obj.put("interestRate", f.interestRate)
            obj.put("principalPayoutPercent", f.principalPayoutPercent)
            flowsArr.put(obj)
        }
        root.put("recurringCashFlows", flowsArr)

        // 6. Portfolio Views
        val viewsArr = JSONArray()
        views.forEach { vw ->
            val obj = JSONObject()
            obj.put("id", vw.id)
            obj.put("name", vw.name)
            obj.put("isDefault", vw.isDefault)
            obj.put("includedCategoryIds", vw.includedCategoryIds)
            viewsArr.put(obj)
        }
        root.put("portfolioViews", viewsArr)

        // 7. Expense Categories
        val expCatArr = JSONArray()
        expenseCats.forEach { ec ->
            val obj = JSONObject()
            obj.put("id", ec.id)
            obj.put("name", ec.name)
            obj.put("iconName", ec.iconName)
            obj.put("colorHex", ec.colorHex)
            obj.put("isDefault", ec.isDefault)
            obj.put("orderIndex", ec.orderIndex)
            expCatArr.put(obj)
        }
        root.put("expenseCategories", expCatArr)

        // 8. Expenses
        val expArr = JSONArray()
        expenses.forEach { exp ->
            val obj = JSONObject()
            obj.put("id", exp.id)
            obj.put("snapshotId", exp.snapshotId)
            obj.put("expenseCategoryId", exp.expenseCategoryId)
            obj.put("amount", exp.amount)
            obj.put("title", exp.title)
            obj.put("dateMillis", exp.dateMillis)
            obj.put("notes", exp.notes)
            expArr.put(obj)
        }
        root.put("expenses", expArr)

        // 9. Loans
        val loansArr = JSONArray()
        loans.forEach { ln ->
            val obj = JSONObject()
            obj.put("id", ln.id)
            obj.put("name", ln.name)
            obj.put("loanType", ln.loanType)
            obj.put("lenderBank", ln.lenderBank)
            obj.put("accountNumber", ln.accountNumber)
            obj.put("originalPrincipal", ln.originalPrincipal)
            obj.put("currentOutstanding", ln.currentOutstanding)
            obj.put("annualInterestRate", ln.annualInterestRate)
            obj.put("totalTenureMonths", ln.totalTenureMonths)
            obj.put("remainingTenureMonths", ln.remainingTenureMonths)
            obj.put("monthlyEmi", ln.monthlyEmi)
            obj.put("emiDebitDay", ln.emiDebitDay)
            obj.put("startDate", ln.startDate)
            obj.put("notes", ln.notes)
            obj.put("isActive", ln.isActive)
            loansArr.put(obj)
        }
        root.put("loans", loansArr)

        // 10. Insurance Policies
        val insArr = JSONArray()
        insurances.forEach { ip ->
            val obj = JSONObject()
            obj.put("id", ip.id)
            obj.put("policyName", ip.policyName)
            obj.put("insuranceType", ip.insuranceType)
            obj.put("providerName", ip.providerName)
            obj.put("policyNumber", ip.policyNumber)
            obj.put("sumInsured", ip.sumInsured)
            obj.put("annualPremium", ip.annualPremium)
            obj.put("premiumFrequency", ip.premiumFrequency)
            obj.put("renewalDate", ip.renewalDate)
            obj.put("nomineeName", ip.nomineeName)
            obj.put("insuredPerson", ip.insuredPerson)
            obj.put("notes", ip.notes)
            obj.put("isActive", ip.isActive)
            insArr.put(obj)
        }
        root.put("insurancePolicies", insArr)

        // 10. Credit Cards
        val ccArr = JSONArray()
        creditCards.forEach { cc ->
            val obj = JSONObject()
            obj.put("id", cc.id)
            obj.put("bankName", cc.bankName)
            obj.put("cardName", cc.cardName)
            obj.put("cardType", cc.cardType)
            obj.put("last4Digits", cc.last4Digits)
            obj.put("creditLimit", cc.creditLimit)
            obj.put("availableLimit", cc.availableLimit)
            obj.put("totalDue", cc.totalDue)
            obj.put("minDue", cc.minDue)
            obj.put("dueDate", cc.dueDate)
            obj.put("statementDate", cc.statementDate)
            obj.put("billingCycleDay", cc.billingCycleDay)
            obj.put("paymentDueDays", cc.paymentDueDays)
            obj.put("savedPassword", cc.savedPassword)
            obj.put("cardNetwork", cc.cardNetwork)
            obj.put("colorHex", cc.colorHex)
            obj.put("isActive", cc.isActive)
            obj.put("notes", cc.notes)
            ccArr.put(obj)
        }
        root.put("creditCards", ccArr)

        // 11. Credit Card Statements
        val stmtsArr = JSONArray()
        creditCardStatements.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("cardId", s.cardId)
            obj.put("statementDate", s.statementDate)
            obj.put("dueDate", s.dueDate)
            obj.put("totalDue", s.totalDue)
            obj.put("minDue", s.minDue)
            obj.put("spends", s.spends)
            obj.put("payments", s.payments)
            obj.put("cashbackEarned", s.cashbackEarned)
            obj.put("rewardPoints", s.rewardPoints)
            obj.put("availableLimit", s.availableLimit)
            obj.put("totalCreditLimit", s.totalCreditLimit)
            obj.put("fileName", s.fileName)
            obj.put("isPaid", s.isPaid)
            if (s.paidDate != null) obj.put("paidDate", s.paidDate)
            if (s.paidAmount != null) obj.put("paidAmount", s.paidAmount)
            obj.put("notes", s.notes)
            stmtsArr.put(obj)
        }
        root.put("creditCardStatements", stmtsArr)

        // 12. Cashback & Refunds
        val cbArr = JSONArray()
        cashbackRefunds.forEach { cb ->
            val obj = JSONObject()
            obj.put("id", cb.id)
            obj.put("type", cb.type)
            obj.put("title", cb.title)
            obj.put("amount", cb.amount)
            obj.put("source", cb.source)
            if (cb.cardId != null) obj.put("cardId", cb.cardId)
            obj.put("status", cb.status)
            obj.put("expectedDate", cb.expectedDate)
            if (cb.receivedDate != null) obj.put("receivedDate", cb.receivedDate)
            obj.put("referenceNumber", cb.referenceNumber)
            obj.put("category", cb.category)
            obj.put("notes", cb.notes)
            obj.put("createdAt", cb.createdAt)
            cbArr.put(obj)
        }
        root.put("cashbackRefunds", cbArr)

        return root.toString(2)
    }

    suspend fun restoreFullBackupJson(
        repository: PortfolioRepository,
        jsonString: String,
        profileRepository: UserProfileRepository? = null,
        fireSettingsRepository: FireSettingsRepository? = null
    ): ImportResult {
        val trimmed = jsonString.trim()
        if (trimmed.isEmpty()) {
            return ImportResult(success = false, message = "Backup content is empty")
        }

        return try {
            val root = JSONObject(trimmed)

            // Check if this is a Single-Month Snapshot JSON instead of a Full Multi-Month backup
            if (!root.has("snapshots") && (root.has("snapshotId") || root.has("holdings"))) {
                return SnapshotImporter.importSnapshotJson(repository, trimmed)
            }

            // Restore User Profile if present
            val profileObj = root.optJSONObject("userProfile")
            if (profileObj != null && profileRepository != null) {
                try {
                    val pName = profileObj.optString("name", "Prakash Seervi")
                    val pEmail = profileObj.optString("email", "")
                    val pTagline = profileObj.optString("tagline", "Wealth Builder")
                    val pGoal = profileObj.optDouble("targetNetWorth", 5000000.0)
                    val pColor = profileObj.optInt("colorIndex", 0)
                    val pDob = profileObj.optString("dateOfBirth", "1998-05-15")
                    profileRepository.updateProfile(pName, pEmail, pTagline, pGoal, pColor, pDob)
                } catch (_: Exception) {}
            }

            // Restore Fire Settings if present
            val fireObj = root.optJSONObject("fireSettings")
            if (fireObj != null && fireSettingsRepository != null) {
                try {
                    val s = FireSettings(
                        expectedReturnRate = fireObj.optDouble("expectedReturnRate", 12.0),
                        inflationRate = fireObj.optDouble("inflationRate", 6.0),
                        currentAge = fireObj.optInt("currentAge", 28),
                        targetFireAge = fireObj.optInt("targetFireAge", 45),
                        monthlyExpenses = fireObj.optDouble("monthlyExpenses", 50000.0),
                        monthlyInvestment = fireObj.optDouble("monthlyInvestment", 50000.0),
                        swrRate = fireObj.optDouble("swrRate", 3.5),
                        annualStepUpRate = fireObj.optDouble("annualStepUpRate", 8.0)
                    )
                    fireSettingsRepository.updateFireSettings(s)
                } catch (_: Exception) {}
            }

            // Ensure baseline defaults exist
            repository.ensureDefaults()

            // 1. Restore Snapshots
            val snapshotsArr = root.optJSONArray("snapshots") ?: JSONArray()
            val snapList = mutableListOf<MonthlySnapshotEntity>()
            for (i in 0 until snapshotsArr.length()) {
                val s = snapshotsArr.optJSONObject(i) ?: continue
                val id = s.optString("id", "").ifEmpty {
                    val y = s.optInt("year", 2026)
                    val m = s.optInt("month", 1)
                    String.format(Locale.US, "%04d-%02d", y, m)
                }
                val year = s.optInt("year", id.split("-").getOrNull(0)?.toIntOrNull() ?: 2026)
                val month = s.optInt("month", id.split("-").getOrNull(1)?.toIntOrNull() ?: 1)
                snapList.add(
                    MonthlySnapshotEntity(
                        id = id,
                        year = year,
                        month = month,
                        notes = s.optString("notes", ""),
                        updatedAt = s.optLong("updatedAt", System.currentTimeMillis())
                    )
                )
            }
            if (snapList.isNotEmpty()) {
                repository.insertSnapshots(snapList)
            }

            // 2. Restore Categories
            val catsArr = root.optJSONArray("categories") ?: JSONArray()
            val catList = mutableListOf<CategoryEntity>()
            for (i in 0 until catsArr.length()) {
                val c = catsArr.optJSONObject(i) ?: continue
                val id = c.optString("id", "").ifEmpty { "cat_${System.currentTimeMillis()}_$i" }
                val name = c.optString("name", "Category $i")
                catList.add(
                    CategoryEntity(
                        id = id,
                        name = name,
                        groupType = c.optString("groupType", "INVESTMENT"),
                        iconName = c.optString("iconName", "account_balance_wallet"),
                        colorHex = c.optString("colorHex", "#3B82F6"),
                        isDefault = c.optBoolean("isDefault", false),
                        orderIndex = c.optInt("orderIndex", i),
                        description = c.optString("description", "")
                    )
                )
            }
            if (catList.isNotEmpty()) {
                repository.insertCategories(catList)
            }

            // 3. Restore Assets
            val assetsArr = root.optJSONArray("assets") ?: JSONArray()
            val assetList = mutableListOf<AssetEntity>()
            for (i in 0 until assetsArr.length()) {
                val a = assetsArr.optJSONObject(i) ?: continue
                val name = a.optString("name", "").trim()
                if (name.isEmpty()) continue

                assetList.add(
                    AssetEntity(
                        id = a.optLong("id", 0L),
                        categoryId = a.optString("categoryId", "mutual_funds"),
                        name = name,
                        notes = a.optString("notes", ""),
                        status = a.optString("status", "ACTIVE"),
                        initialInvestedAmount = a.optDouble("initialInvestedAmount", 0.0),
                        investmentDate = a.optString("investmentDate", ""),
                        createdAt = a.optLong("createdAt", System.currentTimeMillis()),
                        interestRate = a.optDouble("interestRate", 0.0),
                        payoutFrequency = a.optString("payoutFrequency", "NONE"),
                        payoutType = a.optString("payoutType", "CUMULATIVE"),
                        principalPayoutPercent = a.optDouble("principalPayoutPercent", 0.0),
                        principalPayoutFrequency = a.optString("principalPayoutFrequency", "QUARTERLY"),
                        maturityDate = a.optString("maturityDate", ""),
                        compoundingFrequency = a.optString("compoundingFrequency", "QUARTERLY"),
                        installmentAmount = a.optDouble("installmentAmount", 0.0),
                        installmentFrequency = a.optString("installmentFrequency", "MONTHLY"),
                        tenureMonths = a.optInt("tenureMonths", 0)
                    )
                )
            }
            if (assetList.isNotEmpty()) {
                repository.insertAssets(assetList)
            }

            // 4. Restore Asset Values
            val valsArr = root.optJSONArray("assetValues") ?: JSONArray()
            val valList = mutableListOf<AssetMonthlyValueEntity>()
            for (i in 0 until valsArr.length()) {
                val v = valsArr.optJSONObject(i) ?: continue
                val assetId = v.optLong("assetId", 0L)
                val snapshotId = v.optString("snapshotId", "")
                if (assetId != 0L && snapshotId.isNotEmpty()) {
                    valList.add(
                        AssetMonthlyValueEntity(
                            assetId = assetId,
                            snapshotId = snapshotId,
                            value = v.optDouble("value", 0.0),
                            investedAmount = v.optDouble("investedAmount", 0.0),
                            contributions = v.optDouble("contributions", 0.0),
                            withdrawals = v.optDouble("withdrawals", 0.0),
                            notes = v.optString("notes", "")
                        )
                    )
                }
            }
            if (valList.isNotEmpty()) {
                repository.saveAssetValues(valList)
            }

            // 5. Restore Recurring Flows
            val flowsArr = root.optJSONArray("recurringCashFlows") ?: JSONArray()
            val flowList = mutableListOf<RecurringCashFlowEntity>()
            for (i in 0 until flowsArr.length()) {
                val f = flowsArr.optJSONObject(i) ?: continue
                val name = f.optString("name", "").trim()
                if (name.isEmpty()) continue

                val assetId = if (f.has("assetId") && !f.isNull("assetId")) f.optLong("assetId") else null
                val endMonth = if (f.has("endMonth") && !f.isNull("endMonth")) f.optString("endMonth").ifEmpty { null } else null

                flowList.add(
                    RecurringCashFlowEntity(
                        id = f.optLong("id", 0L),
                        name = name,
                        type = f.optString("type", "SIP"),
                        categoryId = f.optString("categoryId", "mutual_funds"),
                        assetId = assetId,
                        monthlyAmount = f.optDouble("monthlyAmount", 0.0),
                        startMonth = f.optString("startMonth", "2026-01"),
                        endMonth = endMonth,
                        dayOfMonth = f.optInt("dayOfMonth", 5),
                        isActive = f.optBoolean("isActive", true),
                        notes = f.optString("notes", ""),
                        frequency = f.optString("frequency", "MONTHLY"),
                        installmentAmount = f.optDouble("installmentAmount", 0.0),
                        interestRate = f.optDouble("interestRate", 0.0),
                        principalPayoutPercent = f.optDouble("principalPayoutPercent", 0.0)
                    )
                )
            }
            if (flowList.isNotEmpty()) {
                repository.insertRecurringCashFlows(flowList)
            }

            // 6. Restore Portfolio Views
            val viewsArr = root.optJSONArray("portfolioViews") ?: JSONArray()
            val viewList = mutableListOf<PortfolioViewEntity>()
            for (i in 0 until viewsArr.length()) {
                val vw = viewsArr.optJSONObject(i) ?: continue
                val id = vw.optString("id", "")
                val name = vw.optString("name", "")
                if (id.isEmpty() || name.isEmpty()) continue
                viewList.add(
                    PortfolioViewEntity(
                        id = id,
                        name = name,
                        isDefault = vw.optBoolean("isDefault", false),
                        includedCategoryIds = vw.optString("includedCategoryIds", "*")
                    )
                )
            }
            if (viewList.isNotEmpty()) {
                repository.insertPortfolioViews(viewList)
            }

            // 7. Restore Expense Categories
            val expCatArr = root.optJSONArray("expenseCategories") ?: JSONArray()
            val expCatList = mutableListOf<ExpenseCategoryEntity>()
            for (i in 0 until expCatArr.length()) {
                val ec = expCatArr.optJSONObject(i) ?: continue
                val id = ec.optString("id", "").ifEmpty { "exp_cat_$i" }
                val name = ec.optString("name", "Expense Category $i")
                expCatList.add(
                    ExpenseCategoryEntity(
                        id = id,
                        name = name,
                        iconName = ec.optString("iconName", "shopping_bag"),
                        colorHex = ec.optString("colorHex", "#EF4444"),
                        isDefault = ec.optBoolean("isDefault", false),
                        orderIndex = ec.optInt("orderIndex", i)
                    )
                )
            }
            if (expCatList.isNotEmpty()) {
                repository.insertExpenseCategories(expCatList)
            }

            // 7. Restore Expenses
            val expArr = root.optJSONArray("expenses") ?: JSONArray()
            val expList = mutableListOf<ExpenseEntity>()
            for (i in 0 until expArr.length()) {
                val exp = expArr.optJSONObject(i) ?: continue
                val snapId = exp.optString("snapshotId", "")
                val catId = exp.optString("expenseCategoryId", "")
                if (snapId.isNotEmpty() && catId.isNotEmpty()) {
                    expList.add(
                        ExpenseEntity(
                            id = exp.optLong("id", 0L),
                            snapshotId = snapId,
                            expenseCategoryId = catId,
                            amount = exp.optDouble("amount", 0.0),
                            title = exp.optString("title", "Expense"),
                            dateMillis = exp.optLong("dateMillis", System.currentTimeMillis()),
                            notes = exp.optString("notes", "")
                        )
                    )
                }
            }
            if (expList.isNotEmpty()) {
                repository.insertExpenses(expList)
            }

            // 8. Restore Loans
            val loansArr = root.optJSONArray("loans") ?: JSONArray()
            val loanList = mutableListOf<LoanEntity>()
            for (i in 0 until loansArr.length()) {
                val ln = loansArr.optJSONObject(i) ?: continue
                val name = ln.optString("name", "").trim()
                if (name.isEmpty()) continue
                loanList.add(
                    LoanEntity(
                        id = ln.optLong("id", 0L),
                        name = name,
                        loanType = ln.optString("loanType", "PERSONAL_LOAN"),
                        lenderBank = ln.optString("lenderBank", ""),
                        accountNumber = ln.optString("accountNumber", ""),
                        originalPrincipal = ln.optDouble("originalPrincipal", 0.0),
                        currentOutstanding = ln.optDouble("currentOutstanding", 0.0),
                        annualInterestRate = ln.optDouble("annualInterestRate", 0.0),
                        totalTenureMonths = ln.optInt("totalTenureMonths", 12),
                        remainingTenureMonths = ln.optInt("remainingTenureMonths", 12),
                        monthlyEmi = ln.optDouble("monthlyEmi", 0.0),
                        emiDebitDay = ln.optInt("emiDebitDay", 5),
                        startDate = ln.optString("startDate", ""),
                        notes = ln.optString("notes", ""),
                        isActive = ln.optBoolean("isActive", true)
                    )
                )
            }
            if (loanList.isNotEmpty()) {
                repository.insertLoans(loanList)
            }

            // 9. Restore Insurance Policies
            val insArr = root.optJSONArray("insurancePolicies") ?: JSONArray()
            val insList = mutableListOf<InsuranceEntity>()
            for (i in 0 until insArr.length()) {
                val ip = insArr.optJSONObject(i) ?: continue
                val pName = ip.optString("policyName", "").trim()
                if (pName.isEmpty()) continue
                insList.add(
                    InsuranceEntity(
                        id = ip.optLong("id", 0L),
                        policyName = pName,
                        insuranceType = ip.optString("insuranceType", "TERM_LIFE"),
                        providerName = ip.optString("providerName", ""),
                        policyNumber = ip.optString("policyNumber", ""),
                        sumInsured = ip.optDouble("sumInsured", 0.0),
                        annualPremium = ip.optDouble("annualPremium", 0.0),
                        premiumFrequency = ip.optString("premiumFrequency", "ANNUAL"),
                        renewalDate = ip.optString("renewalDate", ""),
                        nomineeName = ip.optString("nomineeName", ""),
                        insuredPerson = ip.optString("insuredPerson", ""),
                        notes = ip.optString("notes", ""),
                        isActive = ip.optBoolean("isActive", true)
                    )
                )
            }
            if (insList.isNotEmpty()) {
                repository.insertInsurancePolicies(insList)
            }

            // 10. Restore Credit Cards
            val ccArr = root.optJSONArray("creditCards") ?: JSONArray()
            val ccList = mutableListOf<CreditCardEntity>()
            for (i in 0 until ccArr.length()) {
                val cc = ccArr.optJSONObject(i) ?: continue
                val bName = cc.optString("bankName", "").trim()
                val cName = cc.optString("cardName", "").trim()
                if (bName.isEmpty() && cName.isEmpty()) continue

                ccList.add(
                    CreditCardEntity(
                        id = cc.optLong("id", 0L),
                        bankName = bName,
                        cardName = cName,
                        cardType = cc.optString("cardType", "Cashback"),
                        last4Digits = cc.optString("last4Digits", "0000"),
                        creditLimit = cc.optDouble("creditLimit", 100000.0),
                        availableLimit = cc.optDouble("availableLimit", 100000.0),
                        totalDue = cc.optDouble("totalDue", 0.0),
                        minDue = cc.optDouble("minDue", 0.0),
                        dueDate = cc.optString("dueDate", ""),
                        statementDate = cc.optString("statementDate", ""),
                        billingCycleDay = cc.optInt("billingCycleDay", 15),
                        paymentDueDays = cc.optInt("paymentDueDays", 20),
                        savedPassword = cc.optString("savedPassword", ""),
                        cardNetwork = cc.optString("cardNetwork", "VISA"),
                        colorHex = cc.optString("colorHex", "#1E293B"),
                        isActive = cc.optBoolean("isActive", true),
                        notes = cc.optString("notes", "")
                    )
                )
            }
            if (ccList.isNotEmpty()) {
                repository.insertCreditCards(ccList)
            }

            // 11. Restore Credit Card Statements
            val stmtsArr = root.optJSONArray("creditCardStatements") ?: JSONArray()
            val stmtList = mutableListOf<CreditCardStatementEntity>()
            for (i in 0 until stmtsArr.length()) {
                val s = stmtsArr.optJSONObject(i) ?: continue
                val cardId = s.optLong("cardId", 0L)
                if (cardId == 0L) continue

                stmtList.add(
                    CreditCardStatementEntity(
                        id = s.optLong("id", 0L),
                        cardId = cardId,
                        statementDate = s.optString("statementDate", ""),
                        dueDate = s.optString("dueDate", ""),
                        totalDue = s.optDouble("totalDue", 0.0),
                        minDue = s.optDouble("minDue", 0.0),
                        spends = s.optDouble("spends", 0.0),
                        payments = s.optDouble("payments", 0.0),
                        cashbackEarned = s.optDouble("cashbackEarned", 0.0),
                        rewardPoints = s.optInt("rewardPoints", 0),
                        availableLimit = s.optDouble("availableLimit", 0.0),
                        totalCreditLimit = s.optDouble("totalCreditLimit", 0.0),
                        fileName = s.optString("fileName", ""),
                        isPaid = s.optBoolean("isPaid", false),
                        paidDate = if (s.has("paidDate")) s.optString("paidDate") else null,
                        paidAmount = if (s.has("paidAmount")) s.optDouble("paidAmount") else null,
                        notes = s.optString("notes", "")
                    )
                )
            }
            if (stmtList.isNotEmpty()) {
                repository.insertCreditCardStatements(stmtList)
            }

            // 12. Restore Cashback & Refunds
            val cbArr = root.optJSONArray("cashbackRefunds") ?: JSONArray()
            val cbList = mutableListOf<CashbackRefundEntity>()
            for (i in 0 until cbArr.length()) {
                val cb = cbArr.optJSONObject(i) ?: continue
                val title = cb.optString("title", "").trim()
                if (title.isEmpty()) continue

                cbList.add(
                    CashbackRefundEntity(
                        id = cb.optLong("id", 0L),
                        type = cb.optString("type", "CASHBACK"),
                        title = title,
                        amount = cb.optDouble("amount", 0.0),
                        source = cb.optString("source", ""),
                        cardId = if (cb.has("cardId") && !cb.isNull("cardId")) cb.optLong("cardId") else null,
                        status = "RECEIVED",
                        expectedDate = cb.optString("expectedDate", ""),
                        receivedDate = if (cb.has("receivedDate") && !cb.isNull("receivedDate")) cb.optString("receivedDate") else null,
                        referenceNumber = cb.optString("referenceNumber", ""),
                        category = cb.optString("category", "Shopping"),
                        notes = cb.optString("notes", ""),
                        createdAt = cb.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
            if (cbList.isNotEmpty()) {
                repository.insertCashbackRefunds(cbList)
            }

            val extras = mutableListOf<String>()
            if (loanList.isNotEmpty()) extras.add("${loanList.size} Loans")
            if (insList.isNotEmpty()) extras.add("${insList.size} Insurance Policies")
            if (ccList.isNotEmpty()) extras.add("${ccList.size} Credit Cards")
            if (cbList.isNotEmpty()) extras.add("${cbList.size} Cashback & Refunds")
            if (viewList.isNotEmpty()) extras.add("${viewList.size} Custom Views")
            val extraSuffix = if (extras.isNotEmpty()) ", ${extras.joinToString(", ")}" else ""

            ImportResult(
                success = true,
                message = "Backup Restored Successfully: ${snapList.size} Monthly Snapshots, ${assetList.size} Assets, ${valList.size} Valuations, ${flowList.size} Recurring Flows, ${expList.size} Expenses$extraSuffix restored.",
                holdingsCount = assetList.size,
                sipsCount = flowList.size
            )
        } catch (e: Exception) {
            ImportResult(
                success = false,
                message = "Failed to restore backup: ${e.localizedMessage ?: "Invalid or corrupted JSON file"}"
            )
        }
    }
}

