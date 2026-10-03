package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CreditCardEntity
import com.example.data.local.entity.CreditCardStatementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY isActive DESC, totalDue DESC, bankName ASC")
    fun getAllCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE isActive = 1 ORDER BY totalDue DESC, bankName ASC")
    fun getActiveCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: Long): CreditCardEntity?

    @Query("SELECT * FROM credit_cards")
    suspend fun getAllCardsList(): List<CreditCardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CreditCardEntity>)

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards WHERE id = :id")
    suspend fun deleteCardById(id: Long)

    @Query("UPDATE credit_cards SET savedPassword = :password WHERE id = :cardId")
    suspend fun updateSavedPassword(cardId: Long, password: String)

    @Query("UPDATE credit_cards SET savedPassword = :password WHERE bankName = :bankName")
    suspend fun updateSavedPasswordForBank(bankName: String, password: String)

    @Query("SELECT * FROM credit_card_statements ORDER BY statementDate DESC")
    fun getAllStatements(): Flow<List<CreditCardStatementEntity>>

    @Query("SELECT * FROM credit_card_statements WHERE cardId = :cardId ORDER BY statementDate DESC")
    fun getStatementsForCard(cardId: Long): Flow<List<CreditCardStatementEntity>>

    @Query("SELECT * FROM credit_card_statements WHERE cardId = :cardId ORDER BY statementDate DESC")
    suspend fun getStatementsForCardList(cardId: Long): List<CreditCardStatementEntity>

    @Query("SELECT * FROM credit_card_statements")
    suspend fun getAllStatementsList(): List<CreditCardStatementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatement(statement: CreditCardStatementEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatements(statements: List<CreditCardStatementEntity>)

    @Update
    suspend fun updateStatement(statement: CreditCardStatementEntity)

    @Delete
    suspend fun deleteStatement(statement: CreditCardStatementEntity)

    @Query("UPDATE credit_card_statements SET isPaid = :isPaid, paidDate = :paidDate, paidAmount = :paidAmount WHERE id = :statementId")
    suspend fun markStatementPaid(statementId: Long, isPaid: Boolean, paidDate: String?, paidAmount: Double?)

    @Query("UPDATE credit_cards SET totalDue = 0.0, minDue = 0.0 WHERE id = :cardId")
    suspend fun clearCardDue(cardId: Long)
}
