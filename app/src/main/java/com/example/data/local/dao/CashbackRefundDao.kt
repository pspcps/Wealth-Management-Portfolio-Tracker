package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.CashbackRefundEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CashbackRefundDao {
    @Query("SELECT * FROM cashback_refunds ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<CashbackRefundEntity>>

    @Query("SELECT * FROM cashback_refunds WHERE type = :type ORDER BY createdAt DESC")
    fun getItemsByType(type: String): Flow<List<CashbackRefundEntity>>

    @Query("SELECT * FROM cashback_refunds WHERE status = :status ORDER BY createdAt DESC")
    fun getItemsByStatus(status: String): Flow<List<CashbackRefundEntity>>

    @Query("SELECT * FROM cashback_refunds WHERE cardId = :cardId ORDER BY createdAt DESC")
    fun getItemsForCard(cardId: Long): Flow<List<CashbackRefundEntity>>

    @Query("SELECT * FROM cashback_refunds WHERE id = :id")
    suspend fun getItemById(id: Long): CashbackRefundEntity?

    @Query("SELECT * FROM cashback_refunds")
    suspend fun getAllItemsList(): List<CashbackRefundEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CashbackRefundEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<CashbackRefundEntity>)

    @Update
    suspend fun updateItem(item: CashbackRefundEntity)

    @Delete
    suspend fun deleteItem(item: CashbackRefundEntity)

    @Query("DELETE FROM cashback_refunds WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    @Query("UPDATE cashback_refunds SET status = 'RECEIVED', receivedDate = :receivedDate WHERE id = :id")
    suspend fun markItemReceived(id: Long, receivedDate: String)

    @Query("UPDATE cashback_refunds SET status = 'PENDING', receivedDate = NULL WHERE id = :id")
    suspend fun markItemPending(id: Long)
}
