package com.example.data.repository

import com.example.data.local.dao.CashbackRefundDao
import com.example.data.local.entity.CashbackRefundEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class CashbackRefundSummary(
    val totalCashbackEarned: Double = 0.0,
    val totalCashbackPending: Double = 0.0,
    val totalRefundsReceived: Double = 0.0,
    val pendingRefundAmount: Double = 0.0,
    val totalRecoveredAndSaved: Double = 0.0,
    val pendingItemsCount: Int = 0,
    val totalItemsCount: Int = 0
)

class CashbackRefundRepository(
    private val dao: CashbackRefundDao
) {
    val allItems: Flow<List<CashbackRefundEntity>> = dao.getAllItems()

    val summary: Flow<CashbackRefundSummary> = allItems.map { list ->
        var cashbackReceived = 0.0
        var refundsReceived = 0.0

        for (item in list) {
            if (item.type.equals("CASHBACK", ignoreCase = true)) {
                cashbackReceived += item.amount
            } else { // REFUND
                refundsReceived += item.amount
            }
        }

        CashbackRefundSummary(
            totalCashbackEarned = cashbackReceived,
            totalCashbackPending = 0.0,
            totalRefundsReceived = refundsReceived,
            pendingRefundAmount = 0.0,
            totalRecoveredAndSaved = cashbackReceived + refundsReceived,
            pendingItemsCount = 0,
            totalItemsCount = list.size
        )
    }

    suspend fun saveItem(item: CashbackRefundEntity): Long {
        return if (item.id == 0L) {
            dao.insertItem(item)
        } else {
            dao.updateItem(item)
            item.id
        }
    }

    suspend fun deleteItem(item: CashbackRefundEntity) {
        dao.deleteItem(item)
    }

    suspend fun deleteItemById(id: Long) {
        dao.deleteItemById(id)
    }

    suspend fun markReceived(id: Long, receivedDate: String) {
        dao.markItemReceived(id, receivedDate)
    }

    suspend fun markPending(id: Long) {
        dao.markItemPending(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: CashbackRefundRepository? = null

        fun getInstance(context: android.content.Context): CashbackRefundRepository {
            return INSTANCE ?: synchronized(this) {
                val db = com.example.data.local.AppDatabase.getInstance(context)
                val repo = CashbackRefundRepository(db.cashbackRefundDao())
                INSTANCE = repo
                repo
            }
        }
    }
}
