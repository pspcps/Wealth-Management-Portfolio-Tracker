package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.CashbackRefundDao
import com.example.data.local.dao.CreditCardDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.dao.InsuranceDao
import com.example.data.local.dao.LoanDao
import com.example.data.local.dao.PortfolioDao
import com.example.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        AssetEntity::class,
        MonthlySnapshotEntity::class,
        AssetMonthlyValueEntity::class,
        ExpenseCategoryEntity::class,
        ExpenseEntity::class,
        PortfolioViewEntity::class,
        RecurringCashFlowEntity::class,
        LoanEntity::class,
        InsuranceEntity::class,
        CreditCardEntity::class,
        CreditCardStatementEntity::class,
        CashbackRefundEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun portfolioDao(): PortfolioDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun loanDao(): LoanDao
    abstract fun insuranceDao(): InsuranceDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun cashbackRefundDao(): CashbackRefundDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE assets ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE assets ADD COLUMN payoutFrequency TEXT NOT NULL DEFAULT 'NONE'")
                db.execSQL("ALTER TABLE assets ADD COLUMN payoutType TEXT NOT NULL DEFAULT 'CUMULATIVE'")
                db.execSQL("ALTER TABLE assets ADD COLUMN principalPayoutPercent REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE assets ADD COLUMN principalPayoutFrequency TEXT NOT NULL DEFAULT 'QUARTERLY'")
                db.execSQL("ALTER TABLE assets ADD COLUMN maturityDate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE assets ADD COLUMN compoundingFrequency TEXT NOT NULL DEFAULT 'QUARTERLY'")
                db.execSQL("ALTER TABLE assets ADD COLUMN installmentAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE assets ADD COLUMN installmentFrequency TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE assets ADD COLUMN tenureMonths INTEGER NOT NULL DEFAULT 0")

                db.execSQL("ALTER TABLE recurring_cash_flows ADD COLUMN frequency TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE recurring_cash_flows ADD COLUMN installmentAmount REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE recurring_cash_flows ADD COLUMN interestRate REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE recurring_cash_flows ADD COLUMN principalPayoutPercent REAL NOT NULL DEFAULT 0.0")
            }
        }

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS credit_cards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        bankName TEXT NOT NULL,
                        cardName TEXT NOT NULL,
                        cardType TEXT NOT NULL,
                        last4Digits TEXT NOT NULL,
                        creditLimit REAL NOT NULL,
                        availableLimit REAL NOT NULL,
                        totalDue REAL NOT NULL,
                        minDue REAL NOT NULL,
                        dueDate TEXT NOT NULL,
                        statementDate TEXT NOT NULL,
                        billingCycleDay INTEGER NOT NULL,
                        paymentDueDays INTEGER NOT NULL,
                        savedPassword TEXT NOT NULL,
                        cardNetwork TEXT NOT NULL,
                        colorHex TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        notes TEXT NOT NULL
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS credit_card_statements (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardId INTEGER NOT NULL,
                        statementDate TEXT NOT NULL,
                        dueDate TEXT NOT NULL,
                        totalDue REAL NOT NULL,
                        minDue REAL NOT NULL,
                        spends REAL NOT NULL,
                        payments REAL NOT NULL,
                        cashbackEarned REAL NOT NULL,
                        rewardPoints INTEGER NOT NULL,
                        availableLimit REAL NOT NULL,
                        totalCreditLimit REAL NOT NULL,
                        fileName TEXT NOT NULL,
                        isPaid INTEGER NOT NULL,
                        paidDate TEXT,
                        paidAmount REAL,
                        notes TEXT NOT NULL,
                        FOREIGN KEY(cardId) REFERENCES credit_cards(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_credit_card_statements_cardId ON credit_card_statements(cardId)")

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS cashback_refunds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        title TEXT NOT NULL,
                        amount REAL NOT NULL,
                        source TEXT NOT NULL,
                        cardId INTEGER,
                        status TEXT NOT NULL,
                        expectedDate TEXT NOT NULL,
                        receivedDate TEXT,
                        referenceNumber TEXT NOT NULL,
                        category TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wealth_portfolio_tracker.db"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                
                // Trigger initial data population asynchronously once instance is assigned
                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseInitializer.populateInitialData(instance)
                }
                
                instance
            }
        }
    }
}
