package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.dao.CreditCardDao
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.CreditCardStatementEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

data class CreditCardPortfolioSummary(
    val totalCreditLimit: Double = 0.0,
    val totalDue: Double = 0.0,
    val availableLimit: Double = 0.0,
    val utilizationPercent: Double = 0.0,
    val cardsCount: Int = 0,
    val activeCardsCount: Int = 0,
    val nextDueCardName: String = "",
    val nextDueDate: String = "",
    val nextDueAmount: Double = 0.0
)

class CreditCardRepository(
    private val creditCardDao: CreditCardDao,
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("credit_card_passwords_vault", Context.MODE_PRIVATE)
    }

    val allCards: Flow<List<CreditCardEntity>> = creditCardDao.getAllCards()
    val activeCards: Flow<List<CreditCardEntity>> = creditCardDao.getActiveCards()
    val allStatements: Flow<List<CreditCardStatementEntity>> = creditCardDao.getAllStatements()

    val cardSummary: Flow<CreditCardPortfolioSummary> = allCards.map { cards ->
        val active = cards.filter { it.isActive }
        val totalLimit = active.sumOf { it.creditLimit }
        val totalDue = active.sumOf { it.totalDue }
        val availLimit = (totalLimit - totalDue).coerceAtLeast(0.0)
        val utilization = if (totalLimit > 0) (totalDue / totalLimit) * 100.0 else 0.0

        // Find nearest due card with due amount > 0
        val withDues = active.filter { it.totalDue > 0 && it.dueDate.isNotBlank() }
            .sortedBy { it.dueDate }
        val nextDue = withDues.firstOrNull()

        CreditCardPortfolioSummary(
            totalCreditLimit = totalLimit,
            totalDue = totalDue,
            availableLimit = availLimit,
            utilizationPercent = utilization,
            cardsCount = cards.size,
            activeCardsCount = active.size,
            nextDueCardName = nextDue?.let { "${it.bankName} ${it.cardName}" } ?: "",
            nextDueDate = nextDue?.dueDate ?: "",
            nextDueAmount = nextDue?.totalDue ?: 0.0
        )
    }

    fun getStatementsForCard(cardId: Long): Flow<List<CreditCardStatementEntity>> {
        return creditCardDao.getStatementsForCard(cardId)
    }

    suspend fun getCardById(id: Long): CreditCardEntity? = creditCardDao.getCardById(id)

    suspend fun saveCard(card: CreditCardEntity): Long {
        return if (card.id == 0L) {
            val insertedId = creditCardDao.insertCard(card)
            if (card.savedPassword.isNotBlank()) {
                savePasswordForBank(card.bankName, card.savedPassword)
            }
            insertedId
        } else {
            creditCardDao.updateCard(card)
            if (card.savedPassword.isNotBlank()) {
                savePasswordForBank(card.bankName, card.savedPassword)
            }
            card.id
        }
    }

    suspend fun deleteCard(card: CreditCardEntity) {
        creditCardDao.deleteCard(card)
    }

    suspend fun deleteCardById(id: Long) {
        creditCardDao.deleteCardById(id)
    }

    suspend fun saveStatement(
        statement: CreditCardStatementEntity,
        updateCardInfo: Boolean = true
    ): Long {
        val id = if (statement.id == 0L) {
            creditCardDao.insertStatement(statement)
        } else {
            creditCardDao.updateStatement(statement)
            statement.id
        }

        if (updateCardInfo) {
            val card = creditCardDao.getCardById(statement.cardId)
            if (card != null) {
                val updatedCard = card.copy(
                    totalDue = statement.totalDue,
                    minDue = statement.minDue,
                    dueDate = if (statement.dueDate.isNotBlank()) statement.dueDate else card.dueDate,
                    statementDate = if (statement.statementDate.isNotBlank()) statement.statementDate else card.statementDate,
                    availableLimit = if (statement.availableLimit > 0) statement.availableLimit else (card.creditLimit - statement.totalDue).coerceAtLeast(0.0)
                )
                creditCardDao.updateCard(updatedCard)
            }
        }
        return id
    }

    suspend fun deleteStatement(statement: CreditCardStatementEntity) {
        creditCardDao.deleteStatement(statement)
    }

    suspend fun deleteStatementById(statementId: Long) {
        val stmt = creditCardDao.getAllStatementsList().firstOrNull { it.id == statementId }
        if (stmt != null) {
            creditCardDao.deleteStatement(stmt)
        }
    }

    suspend fun markStatementPaid(statementId: Long, cardId: Long, paidAmount: Double, paidDate: String) {
        creditCardDao.markStatementPaid(
            statementId = statementId,
            isPaid = true,
            paidDate = paidDate,
            paidAmount = paidAmount
        )
        val card = creditCardDao.getCardById(cardId)
        if (card != null) {
            val newDue = (card.totalDue - paidAmount).coerceAtLeast(0.0)
            val newAvail = (card.availableLimit + paidAmount).coerceAtMost(card.creditLimit)
            creditCardDao.updateCard(card.copy(totalDue = newDue, minDue = (newDue * 0.05).coerceAtLeast(0.0), availableLimit = newAvail))
        }
    }

    suspend fun clearCardBill(cardId: Long) {
        val card = creditCardDao.getCardById(cardId)
        if (card != null) {
            creditCardDao.updateCard(
                card.copy(
                    totalDue = 0.0,
                    minDue = 0.0,
                    availableLimit = card.creditLimit
                )
            )
        }
    }

    fun getSavedPasswordForBank(bankName: String): String? {
        val key = normalizeBankKey(bankName)
        val saved = prefs.getString("pwd_bank_$key", null)
        return if (!saved.isNullOrBlank()) saved else null
    }

    fun savePasswordForBank(bankName: String, password: String) {
        if (password.isBlank()) return
        val key = normalizeBankKey(bankName)
        prefs.edit().putString("pwd_bank_$key", password.trim()).apply()
    }

    private fun normalizeBankKey(bankName: String): String {
        return bankName.lowercase(Locale.ROOT)
            .replace("[^a-z0-9]".toRegex(), "")
    }

    companion object {
        @Volatile
        private var INSTANCE: CreditCardRepository? = null

        fun getInstance(context: Context): CreditCardRepository {
            return INSTANCE ?: synchronized(this) {
                val db = com.example.data.local.AppDatabase.getInstance(context)
                val repo = CreditCardRepository(db.creditCardDao(), context.applicationContext)
                INSTANCE = repo
                repo
            }
        }
    }
}
