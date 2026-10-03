# 💰 Wealth Management & Portfolio Tracker

An offline-first, comprehensive **Personal Finance, Multi-Asset Portfolio, Expense, Liability, Credit Card, Cashback/Refund, and Financial Planning Android Application** built with **Kotlin**, **Jetpack Compose (Material 3)**, and **Room Database**.

Designed for complete financial visibility and privacy, the app unifies net worth tracking, monthly portfolio snapshots, organic return & XIRR analytics, recurring cash flows (SIPs, RDs, Salary, Fixed Expenses), loans, insurance policies, credit card statement PDF parsing, cashback & refund tracking, 37+ financial calculators, and full local/JSON master backups.

---

## ✨ Key Highlights & Core Modules

### 1. 📊 Executive Dashboard & Net Worth Engine
- **Total Net Worth & Live P&L**: Tracks total portfolio value, total invested capital, absolute profit/loss, and overall return percentage across all active asset categories.
- **Period-over-Period Comparison**: Compare portfolio growth across **MoM (Month-over-Month)**, **QoQ (Quarter-over-Quarter)**, **Half-Yearly (6M)**, **YoY (Year-over-Year)**, or **Custom Portfolio Views**.
- **Organic Return Isolation**: Separates market growth from fresh capital inflows (`Contributions`) and outflows (`Withdrawals`) so performance metrics reflect true investment performance.
- **Interactive Visualizations**:
  - Multi-Snapshot Portfolio Trajectory Line Chart
  - Asset Allocation Donut Chart with category breakdowns
  - Category Growth Bar Chart
  - Monthly Expense Trend Chart
- **Financial Pulse & FIRE Readiness Card**: Instant snapshot of savings rate, investment rate, emergency fund coverage, and FIRE (Financial Independence, Retire Early) progress.
- **Quick Summary Cards**:
  - **Recurring Flows & SIPs**: Active monthly SIPs, RDs, interest/principal payouts, and isolated Salary & Coupons inflow.
  - **Cashback & Refunds Summary**: Total recovered & saved, current month recovery, and Cashback vs. Refund breakdown.
  - **Debt & Protection Overview**: Active loans, total outstanding debt, monthly EMI burden, and total life/health insurance sum assured.

---

### 2. 🔐 Multi-Layer Privacy & Security Controls
- **Global Privacy Eye (`FinancialPrivacyManager`)**: One-tap toggle in the top bar to mask or reveal all monetary balances (`₹ ••••••`) across the app when viewing in public. Configurable in Settings to start hidden by default.
- **Isolated Salary Privacy Toggle**: The **Salary & Coupons Inflow** metric on the Dashboard and Recurring Cash Flows screen uses an **independent, isolated eye toggle** that defaults to **hidden (`false`)** on every launch and is completely decoupled from the global top privacy eye and global settings.
- **App Lock (`SecurityManager`)**: Optional PIN passcode and Android Biometric / Fingerprint authentication gate on app startup.

---

### 3. 💼 Holdings & Multi-Asset Portfolio Management
- **24+ Pre-configured & Custom Asset Categories**: Grouped into **Investments** (Mutual Funds, Stocks, Gold, SGB, Bonds, Crypto, Real Estate, REITs), **Fixed Income & Cash** (Savings Accounts, Fixed Deposits, Recurring Deposits, Liquid Funds, Arbitrage), **Retirement** (EPF, PPF, NPS), and **Custom Categories**.
- **Fixed Income & Deposit Intelligence**: Dedicated tracking for FDs, RDs, and Bonds including `interestRate`, `compoundingFrequency`, `payoutFrequency` (Monthly, Quarterly, Half-Yearly, Annually, At Maturity), `payoutType` (Cumulative, Interest Only, Interest + Principal), `maturityDate`, `installmentAmount`, and `tenureMonths`.
- **Internal Asset Transfers (`TransferAssetDialog`)**: Move funds between assets (e.g., Bank Account ➔ Mutual Fund or FD Maturity ➔ Equity) while preserving organic return calculations.
- **Asset XIRR Analytics (`XirrEngine`)**: Newton-Raphson / bisection solver computing accurate annualized **XIRR (%)** for individual assets and the overall portfolio across irregular cash flows.
- **Brokerage & Statement Import (`InvestmentImportDialog` & `InvestmentStatementParser`)**: Import holdings from CSV/JSON or statement files.

---

### 4. 🗓️ Monthly Valuation Entry (`MonthlyUpdateScreen`)
- **Snapshot-Based Tracking**: Record monthly asset valuations (`YYYY-MM`) with one-click **"Copy Previous Month Values"** to speed up monthly bookkeeping.
- **Per-Asset Cash Flows**: Log monthly `Current Value`, `Invested Principal`, `Contributions (+)` and `Withdrawals (-)` with inline math expression support.
- **Inline Math Expression Evaluator (`MathAmountTextField`)**: Every amount field across the app supports inline arithmetic (e.g., `15000+4500+250`) with live evaluation badges.

---

### 5. 🔁 Recurring Cash Flows, SIPs & Salary (`RecurringCashFlowScreen`)
- **Unified Recurring Ledger**: Track **SIPs**, **Recurring Deposits (RDs)**, **Interest Payouts**, **Principal Payouts**, **Salary Inflows**, **Meal Coupons (Pluxee/Sodexo)**, **PPF/EPF Contributions**, and **Fixed Monthly Expenses** (Rent, Wi-Fi, Maintenance).
- **Frequency Normalization**: Supports Monthly, Quarterly, Half-Yearly, and Annual frequencies with automatic monthly normalization.
- **Auto-Populate Fixed Expenses**: Automatically inject active recurring fixed expenses into new monthly expense snapshots.

---

### 6. 🧾 Expenses Ledger (`ExpensesScreen`)
- **Monthly Categorized Expense Tracking**: Log daily expenses against customizable categories (Groceries, Rent, Dining, Travel, Utilities, Shopping, Medical, etc.).
- **Inline Addition & Math Input**: Quickly sum multiple bills inside the amount box using `+`, `-`, `*`, `/`.
- **Category Breakdown & Budget Insights**: Visual breakdown of top spending categories per month and historical spending trends.

---

### 7. 💳 Credit Cards & PDF Statement Parser (`CreditCardsScreen`)
- **Card Portfolio Management**: Track credit limits, available limits, total & minimum dues, billing cycle days, payment due dates, card networks (VISA, Mastercard, RuPay, Amex, Diners), and custom card themes.
- **Utilization & Due Alerts**: Real-time credit utilization percentage and upcoming due date reminders.
- **Automated PDF Statement Parser (`CreditCardStatementPdfParser`)**:
  - Uses `PdfBox-Android` to parse password-protected or unlocked bank credit card statements locally on-device.
  - Supports saved PDF passwords per card and automatic DOB-based password hints.
  - Extracts statement dates, due dates, total dues, minimum dues, monthly spends, payments, reward points, and cashback earned.

---

### 8. 💰 Cashback & Refunds Tracker (`CashbackRefundScreen`)
- **Standalone Entity Architecture**: Completely independent of credit cards—tracks all cashbacks and merchant refunds as individual, already-received entries.
- **Dual Quick-Add Top Bar Buttons**:
  - 🔵 **Blue "+ Refund" Button**: Opens the streamlined entry dialog pre-selected for a **Refund**.
  - 🟢 **Green "+ Cashback" Button**: Opens the streamlined entry dialog pre-selected for a **Cashback**.
- **Streamlined Entry Experience**:
  - **Amount (`MathAmountTextField`)**: Full inline `+` calculator support.
  - **Date (`DateFieldWithPicker`)**: Clean `YYYY-MM-DD` date field with an interactive Material 3 Calendar DatePicker dialog.
  - **Smart Source / Merchant Chips with Auto-Memorization**: Includes popular platforms (*Amazon, Flipkart, Swiggy, Zomato, Myntra, MakeMyTrip, Google Pay, PhonePe, Store / Shop*) plus an **Other** option. Whenever you select **Other** and enter a custom merchant (e.g., *Blinkit, Uber, Zepto*), it is **automatically saved as a selectable chip** for all future entries.
- **Period Filtering & Analytics**:
  - Top-level **Overall**, **Monthly** (with `< Month Year >` navigator), and **Yearly** (with `< Year >` navigator) filters.
  - Dynamic hero summary card displaying **Total Recovered & Saved**, **Cashback Earned**, **Refunds Returned**, and **Transaction Count** for the selected period.
  - Sub-filters for *All*, *💰 Cashbacks*, and *🔄 Refunds*, plus real-time search.

---

### 9. 🏦 Loans & Insurance Vault (`LoansScreen` & `InsuranceScreen`)
- **Loan & Liability Tracker**:
  - Track Home Loans, Car Loans, Personal Loans, Education Loans, Gold Loans, and Credit Card EMIs.
  - Monitors original principal, current outstanding balance, interest rate (% p.a.), total & remaining tenure, monthly EMI, and repayment progress bars.
- **Insurance Policy Vault**:
  - Track Term Life, Health, Motor, Critical Illness, Home, and Accidental policies.
  - Stores provider name, policy number, sum insured, annual premium, premium frequency, renewal date alerts, nominee, and insured members.

---

### 10. 🧮 37+ Built-In Financial Calculators (`CalculatorsScreen`)
Organized across 6 specialized categories with interactive sliders, charts, and breakdowns:
1. **SIP & Wealth Growth**:
   - Target Amount (Goal SIP Solver), Regular SIP, Step-Up SIP, Lumpsum, Mutual Fund Returns, ROI Calculator, CAGR Calculator.
2. **Retirement & FIRE Planning**:
   - FIRE (Financial Independence 25x/SWR Rule), Retirement Corpus Planner, SWP (Systematic Withdrawal Plan), NPS (National Pension System), APY (Atal Pension Yojana), EPF (Employees' Provident Fund), Statutory Gratuity Calculator.
3. **Government Savings Schemes**:
   - PPF (Public Provident Fund), SSY (Sukanya Samriddhi Yojana), NSC (National Savings Certificate), SCSS (Senior Citizen Savings Scheme), Post Office MIS (Monthly Income Scheme).
4. **Loans & Banking Deposits**:
   - FD (Fixed Deposit), RD (Recurring Deposit), General Loan EMI, Home Loan EMI & Amortization, Car Loan EMI, Flat vs. Reducing Balance Interest Comparator, Simple Interest, Compound Interest.
5. **Tax, Salary & Inflation**:
   - Income Tax Calculator (New vs. Old Tax Regime), Salary Take-Home Pay Calculator, HRA Exemption (Sec 10(13A)), GST Calculator (CGST/SGST), TDS Calculator, Inflation & Purchasing Power Calculator.
6. **Trading & Capital Markets**:
   - XIRR Calculator, Stock Average Price Calculator, Brokerage & Statutory Charges Calculator (STT, GST, Sebi, Stamp Duty), Intraday/Delivery Margin Calculator.

---

### 11. 🤖 Wealth AI Assistant (`WealthAiScreen`)
- Local & tool-assisted financial assistant (`FinanceAgent`, `ToolRegistry`) capable of querying portfolio metrics, running financial calculations, and answering personal finance queries using read/write/calculation finance tools.

---

### 12. ☁️ Complete Master Backup & Auto-Backup (`CloudBackupManager` & `AutoBackupManager`)
- **100% App-Wide Data Coverage**: Exports and restores a single structured JSON master backup (`FULL_MASTER_BACKUP` v2) containing all **15 data domains**:
  1. `userProfile` (Name, Email, Tagline, Target Net Worth Goal, Avatar Theme Color, Date of Birth)
  2. `fireSettings` (Expected Return, Inflation, Current Age, Target FIRE Age, Monthly Expenses, Monthly Investment, SWR Rate, Annual Step-Up Rate)
  3. `snapshots` (All Monthly Snapshots `YYYY-MM`)
  4. `categories` (Default & Custom Asset Categories)
  5. `assets` (All Holdings + full FD/RD/Bond interest, payout, compounding, installment, maturity, and tenure fields)
  6. `assetValues` (All Monthly Valuations, Invested Amounts, Contributions, and Withdrawals)
  7. `recurringCashFlows` (All SIPs, RDs, Payouts, Salary, Coupons, and Fixed Expenses with frequencies and rates)
  8. `portfolioViews` (Custom Category Filter Views)
  9. `expenseCategories` (Default & Custom Expense Categories)
  10. `expenses` (All Monthly Expense Transactions)
  11. `loans` (All Loans, Principals, Outstandings, Rates, Tenures, and EMIs)
  12. `insurancePolicies` (All Policies, Sum Assured, Premiums, Renewals, and Nominees)
  13. `creditCards` (All Cards, Limits, Billing Cycles, Due Dates, Networks, Colors, and Saved Statement Passwords)
  14. `creditCardStatements` (All Parsed & Manual Card Statements, Dues, Spends, Rewards, and Payment Statuses)
  15. `cashbackRefunds` (All Cashback & Refund Entries, Sources, Dates, and Amounts)
- **Daily Auto-Backup**: Automatically saves daily timestamped (`portfolio_backup_YYYY-MM-DD.json`) and latest (`portfolio_backup_latest.json`) backups to device storage (`Documents/portfolioBackup`) with one-tap file restore and share options.
- **Single-Month Snapshot Export**: Export individual monthly reports as **CSV Spreadsheets (`.csv`)**, **Human-Readable Financial Statements (`.txt`)**, or **JSON Archives (`.json`)**.

---

## 🏗️ Architecture & Tech Stack

### Architecture Pattern
The project follows **Clean Architecture + MVVM (Model-View-ViewModel)** with unidirectional data flow:
- **UI Layer (`com.example.ui`)**: 100% Jetpack Compose screens, reusable Material 3 components, custom Canvas charts, and `StateFlow`-driven ViewModels.
- **Domain Layer (`com.example.domain`)**: Pure Kotlin financial engines (`XirrEngine`, `FinanceCalculators`, `LoanCalculators`) and domain models (`PortfolioModels`, `FireModels`, `LoanInsuranceModels`).
- **Data Layer (`com.example.data`)**: Room SQLite database (`AppDatabase` v7), 6 DAOs, 13 `@Entity` tables, and reactive Repositories exposing Kotlin `Flow`s.
- **Utility Layer (`com.example.util`)**: `CloudBackupManager`, `AutoBackupManager`, `CreditCardStatementPdfParser`, `InvestmentStatementParser`, `MathExpressionEvaluator`, `FinancialPrivacyManager`, and `SecurityManager`.

### Technology Stack
| Category | Technology / Library |
| :--- | :--- |
| **Language** | Kotlin 2.x (Coroutines, StateFlow, Flow) |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Navigation** | Jetpack Navigation Compose (`AppNavigation.kt`) |
| **Local Database** | AndroidX Room (`2.x` with KSP, SQLite Migrations v1 ➔ v7) |
| **PDF Parsing** | `com.tom-roush:pdfbox-android:2.0.27.0` |
| **Networking & Serialization** | OkHttp, Retrofit/Moshi, `org.json` |
| **Image Loading** | Coil Compose |
| **Unit & UI Testing** | JUnit 4, Robolectric, Roborazzi, Compose UI Test (`testTag` instrumentation) |

---

## 📂 Project Package Structure

```text
app/src/main/java/com/example/
├── MainActivity.kt                      # Edge-to-edge entry activity
├── ai/                                  # Wealth AI agent, MCP tools & LLM engine
│   ├── agent/                           # FinanceAgent & ToolRegistry
│   ├── engine/                          # LocalLlmEngine & ModelManager
│   └── tools/                           # Read, Write, Calculation & WebSearch tools
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt               # Room Database (13 Entities, Migrations)
│   │   ├── DatabaseInitializer.kt       # Default categories & optional demo data seeder
│   │   ├── dao/                         # PortfolioDao, ExpenseDao, LoanDao, InsuranceDao, CreditCardDao, CashbackRefundDao
│   │   └── entity/Entities.kt           # All 13 Room @Entity definitions
│   └── repository/                      # PortfolioRepository, CashbackRefundRepository, CreditCardRepository, LoanRepository, InsuranceRepository, FireSettingsRepository, UserProfileRepository
├── domain/
│   ├── finance/                         # XirrEngine, FinanceCalculators, LoanCalculators
│   └── model/                           # PortfolioModels, FireModels, LoanInsuranceModels
├── ui/
│   ├── components/                      # Charts, MathAmountTextField, DateFieldWithPicker, Dialogs, Privacy Eye
│   ├── navigation/AppNavigation.kt      # Bottom Navigation & Screen routing
│   ├── screens/
│   │   ├── dashboard/                   # DashboardScreen & DashboardViewModel
│   │   ├── portfolio/                   # PortfolioScreen, PortfolioViewModel & ImportDialog
│   │   ├── monthlyupdate/               # MonthlyUpdateScreen & MonthlyUpdateViewModel
│   │   ├── expenses/                    # ExpensesScreen & ExpensesViewModel
│   │   ├── creditcards/                 # CreditCardsScreen & CreditCardsViewModel
│   │   ├── cashback/                    # CashbackRefundScreen & CashbackRefundViewModel
│   │   ├── recurring/                   # RecurringCashFlowScreen & RecurringCashFlowViewModel
│   │   ├── loans/                       # LoansScreen & LoansViewModel
│   │   ├── insurance/                   # InsuranceScreen & InsuranceViewModel
│   │   ├── calculators/                 # CalculatorsScreen & 37+ Calculator Views
│   │   ├── reports/                     # ReportsScreen & ReportsViewModel
│   │   ├── categories/                  # CategoriesScreen & CategoriesViewModel
│   │   ├── ai/                          # WealthAiScreen & WealthAiViewModel
│   │   ├── settings/                    # SettingsScreen & SettingsViewModel
│   │   ├── security/                    # AppLockScreen
│   │   └── more/                        # MoreScreen hub
│   └── theme/                           # Color, Type, Theme (Material 3)
└── util/                                # CloudBackupManager, AutoBackupManager, PDF/CSV Parsers, MathExpressionEvaluator, SecurityManager, FinancialPrivacyManager
```

---

## 🚀 Getting Started & Building

### Prerequisites
- **Android Studio** Ladybug / Meerkat or newer
- **JDK 11+**
- **Android SDK**: `minSdk = 24` (Android 7.0), `targetSdk = 36`, `compileSdk = 36`

### Build & Run
1. Clone the repository and open the root folder in Android Studio.
2. Allow Gradle to sync dependencies.
3. Run the app on an Android device or emulator, or build the debug APK via CLI:
   ```bash
   gradle assembleDebug
   ```

### Running Unit & Robolectric Tests
Execute the local JVM and Robolectric test suite with:
```bash
gradle :app:testDebugUnitTest
```
