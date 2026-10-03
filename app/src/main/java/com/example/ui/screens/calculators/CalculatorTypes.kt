package com.example.ui.screens.calculators

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.theme.Emerald500

enum class CalculatorCategory(val title: String) {
    ALL("All"),
    SIP_INVESTMENT("SIP & Growth"),
    RETIREMENT_FIRE("Retirement & Pension"),
    GOVT_SAVINGS("Govt Schemes"),
    LOANS_BANKING("Loans & Deposits"),
    TAX_SALARY("Tax & Salary"),
    TRADING_MARKETS("Trading & Stocks")
}

enum class CalculatorType(
    val id: String,
    val title: String,
    val shortName: String,
    val description: String,
    val category: CalculatorCategory,
    val icon: ImageVector,
    val accentColor: Color,
    val badge: String? = null
) {
    // 1. Target Amount / Goal SIP (Requested specifically by user)
    TARGET_SIP(
        "target_sip",
        "Target Amount (Goal SIP)",
        "Goal SIP",
        "Calculate the monthly SIP needed to achieve your specific target amount in chosen years",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.TrackChanges,
        Color(0xFF0284C7),
        "Goal Solver"
    ),

    // 2. Regular SIP
    SIP(
        "sip",
        "SIP Calculator",
        "SIP",
        "Calculate how much you need to save or how much you will accumulate with your SIP",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.TrendingUp,
        Emerald500,
        "Popular"
    ),

    // 3. Step-Up SIP
    STEP_UP_SIP(
        "step_up",
        "Step Up SIP",
        "Step-Up SIP",
        "Calculate SIP Returns with a yearly contribution raise (Top-up)",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.NorthEast,
        Color(0xFF8B5CF6)
    ),

    // 4. Lumpsum
    LUMPSUM(
        "lumpsum",
        "Lumpsum Calculator",
        "Lumpsum",
        "Calculate returns for lumpsum investments to achieve your financial goals",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.AccountBalance,
        Color(0xFF2563EB)
    ),

    // 5. Mutual Fund Returns
    MF_RETURNS(
        "mf_returns",
        "Mutual Fund (MF)",
        "MF Returns",
        "Calculate the returns and wealth growth on your mutual fund investments",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.BarChart,
        Color(0xFF0D9488)
    ),

    // 6. SWP (Systematic Withdrawal Plan)
    SWP(
        "swp",
        "SWP Calculator",
        "SWP",
        "Calculate your final amount and monthly income with Systematic Withdrawal Plans",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.Payments,
        Color(0xFF059669)
    ),

    // 7. FIRE Planner
    FIRE(
        "fire",
        "FIRE (Financial Independence)",
        "FIRE Freedom",
        "Calculate target retirement corpus (25x rule), freedom age, and SIP required with portfolio compounding",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.LocalFireDepartment,
        Color(0xFFEA580C),
        "Freedom"
    ),

    // 8. Retirement Calculator
    RETIREMENT(
        "retirement",
        "Retirement Calculator",
        "Retirement",
        "Calculate how much you need for a relaxed and inflation-proof retirement",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.Elderly,
        Color(0xFFD97706)
    ),

    // 9. NPS Calculator
    NPS(
        "nps",
        "NPS Calculator",
        "NPS",
        "Calculate returns, 60% lump sum, and monthly pension for your National Pension Scheme",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.VolunteerActivism,
        Color(0xFF9333EA)
    ),

    // 10. APY (Atal Pension Yojana)
    APY(
        "apy",
        "APY Calculator",
        "APY",
        "Calculate your monthly contribution under Atal Pension Yojana for guaranteed pension",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.Savings,
        Color(0xFF6366F1)
    ),

    // 11. EPF Calculator
    EPF(
        "epf",
        "EPF Calculator",
        "EPF",
        "Calculate returns for your Employee's Provident Fund (EPF) with annual salary hike",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.WorkHistory,
        Color(0xFF0284C7)
    ),

    // 12. Gratuity Calculator
    GRATUITY(
        "gratuity",
        "Gratuity Calculator",
        "Gratuity",
        "Calculate how much statutory gratuity payout you will get when you leave/retire",
        CalculatorCategory.RETIREMENT_FIRE,
        Icons.Default.CardGiftcard,
        Color(0xFF4F46E5)
    ),

    // 13. PPF Calculator
    PPF(
        "ppf",
        "PPF Calculator",
        "PPF",
        "Calculate your sovereign 7.1% tax-free returns on Public Provident Fund",
        CalculatorCategory.GOVT_SAVINGS,
        Icons.Default.Shield,
        Emerald500,
        "Tax Free"
    ),

    // 14. SSY (Sukanya Samriddhi Yojana)
    SSY(
        "ssy",
        "SSY Calculator",
        "SSY",
        "Calculate returns for Sukanya Samriddhi Yojana (SSY) at 8.2% sovereign rate for girl child",
        CalculatorCategory.GOVT_SAVINGS,
        Icons.Default.Face,
        Color(0xFFEC4899),
        "8.2% Sovereign"
    ),

    // 15. NSC (National Savings Certificate)
    NSC(
        "nsc",
        "NSC Calculator",
        "NSC",
        "Calculate your returns under National Savings Certificate (7.7% compounded annually)",
        CalculatorCategory.GOVT_SAVINGS,
        Icons.Default.WorkspacePremium,
        Color(0xFFF59E0B)
    ),

    // 16. SCSS (Senior Citizen Savings Scheme)
    SCSS(
        "scss",
        "SCSS Calculator",
        "SCSS",
        "Calculate senior citizens savings scheme returns (8.2% quarterly interest payout)",
        CalculatorCategory.GOVT_SAVINGS,
        Icons.Default.AccessibilityNew,
        Color(0xFF0D9488)
    ),

    // 17. Post Office MIS
    POST_OFFICE_MIS(
        "po_mis",
        "Post Office MIS",
        "PO MIS",
        "Calculate Post Office Monthly Income Scheme (7.4% monthly guaranteed cash flow)",
        CalculatorCategory.GOVT_SAVINGS,
        Icons.Default.MarkunreadMailbox,
        Color(0xFFDC2626)
    ),

    // 18. FD (Fixed Deposit)
    FD(
        "fd",
        "FD Calculator",
        "FD",
        "Check returns on your bank fixed deposits (FDs) with compounding frequency without hassle",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.LockClock,
        Color(0xFF2563EB)
    ),

    // 19. RD (Recurring Deposit)
    RD(
        "rd",
        "RD Calculator",
        "RD",
        "Check returns on your Recurring Deposit (RD) in just a few clicks",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.AccountBalanceWallet,
        Color(0xFF0891B2)
    ),

    // 20. General Loan EMI
    EMI(
        "emi",
        "EMI Calculator",
        "EMI",
        "Calculate EMI on your loans - home loan, car loan or personal loan",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.CreditScore,
        Color(0xFFDC2626)
    ),

    // 21. Home Loan EMI
    HOME_LOAN_EMI(
        "home_loan_emi",
        "Home Loan EMI",
        "Home Loan",
        "Calculate your home loan EMI and principal vs interest amortization schedule",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.Home,
        Color(0xFF0284C7)
    ),

    // 22. Car Loan EMI
    CAR_LOAN_EMI(
        "car_loan_emi",
        "Car Loan EMI",
        "Car Loan",
        "Calculate your auto/vehicle loan monthly installments and total financing cost",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.DirectionsCar,
        Color(0xFF7C3AED)
    ),

    // 23. Flat vs Reducing Rate
    FLAT_VS_REDUCING(
        "flat_vs_reducing",
        "Flat vs Reducing Rate",
        "Flat vs Reducing",
        "Compare monthly EMI and real effective interest in Flat and Reducing balance loan schemes",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.CompareArrows,
        Color(0xFFE11D48)
    ),

    // 24. Simple Interest
    SIMPLE_INTEREST(
        "simple_interest",
        "Simple Interest",
        "Simple Interest",
        "Calculate simple interest on your loans and saving schemes investments (P × R × T / 100)",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.Calculate,
        Color(0xFF475569)
    ),

    // 25. Compound Interest
    COMPOUND_INTEREST(
        "compound_interest",
        "Compound Interest",
        "Compound Interest",
        "Calculate compound interest with annual, semi-annual, quarterly or monthly compounding",
        CalculatorCategory.LOANS_BANKING,
        Icons.Default.Savings,
        Emerald500
    ),

    // 26. ROI (Return on Investment)
    ROI(
        "roi",
        "ROI Calculator",
        "ROI",
        "Calculate the absolute and percentage return on investment for your assets",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.Percent,
        Color(0xFF16A34A)
    ),

    // 27. CAGR Calculator
    CAGR(
        "cagr",
        "CAGR Calculator",
        "CAGR",
        "The simplest compound annual growth rate calculator for any multi-year investment",
        CalculatorCategory.SIP_INVESTMENT,
        Icons.Default.ShowChart,
        Color(0xFF4F46E5)
    ),

    // 28. XIRR Calculator
    XIRR(
        "xirr",
        "XIRR Calculator",
        "XIRR",
        "Calculate the extended internal rate of return (XIRR) for irregular cash flows",
        CalculatorCategory.TRADING_MARKETS,
        Icons.Default.Analytics,
        Color(0xFF9333EA)
    ),

    // 29. Stock Average Calculator
    STOCK_AVERAGE(
        "stock_average",
        "Stock Average Calculator",
        "Stock Average",
        "Calculate average price and total quantity of multiple stock purchase tranches",
        CalculatorCategory.TRADING_MARKETS,
        Icons.Default.QueryStats,
        Color(0xFF0284C7)
    ),

    // 30. Brokerage Charges Calculator
    BROKERAGE(
        "brokerage",
        "Brokerage & Charges",
        "Brokerage",
        "Calculate brokerage, STT, exchange turnover fees, GST, stamp duty and net profit/loss",
        CalculatorCategory.TRADING_MARKETS,
        Icons.Default.ReceiptLong,
        Color(0xFFEA580C)
    ),

    // 31. Margin Calculator
    MARGIN(
        "margin",
        "Margin Calculator",
        "Margin",
        "Calculate margin and total trading leverage available for delivery & intraday trades",
        CalculatorCategory.TRADING_MARKETS,
        Icons.Default.Balance,
        Color(0xFF059669)
    ),

    // 32. Income Tax Calculator
    INCOME_TAX(
        "income_tax",
        "Income Tax Calculator",
        "Income Tax",
        "Calculate your payable income tax under New Tax Regime vs Old Tax Regime with rebates",
        CalculatorCategory.TAX_SALARY,
        Icons.Default.RequestQuote,
        Color(0xFFDC2626),
        "New vs Old"
    ),

    // 33. Salary / Take-Home Pay
    SALARY(
        "salary",
        "Salary (Take Home)",
        "Take Home Pay",
        "Calculate your monthly net take-home salary after PF, Professional Tax, and Income Tax TDS",
        CalculatorCategory.TAX_SALARY,
        Icons.Default.AccountBalanceWallet,
        Emerald500
    ),

    // 34. HRA Exemption Calculator
    HRA(
        "hra",
        "HRA Exemption",
        "HRA",
        "Calculate your tax-exempt House Rent Allowance (HRA) under Section 10(13A)",
        CalculatorCategory.TAX_SALARY,
        Icons.Default.Apartment,
        Color(0xFF8B5CF6)
    ),

    // 35. GST Calculator
    GST(
        "gst",
        "GST Calculator",
        "GST",
        "Calculate your payable GST amount (inclusive or exclusive) with CGST/SGST split",
        CalculatorCategory.TAX_SALARY,
        Icons.Default.PointOfSale,
        Color(0xFF0284C7)
    ),

    // 36. TDS Calculator
    TDS(
        "tds",
        "TDS Calculator",
        "TDS",
        "Calculate your TDS deduction rates on salary, rent, interest, contractor, and professional fees",
        CalculatorCategory.TAX_SALARY,
        Icons.Default.Receipt,
        Color(0xFF7C3AED)
    ),

    // 37. Inflation / Purchasing Power
    INFLATION(
        "inflation",
        "Inflation Calculator",
        "Inflation",
        "Calculate inflation-adjusted prices and real purchasing power decline over time",
        CalculatorCategory.TAX_SALARY,
        Icons.Default.PriceChange,
        Color(0xFFEA580C)
    )
}
