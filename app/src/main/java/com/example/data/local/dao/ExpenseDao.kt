package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    // Categories
    @Query("SELECT * FROM expense_categories ORDER BY orderIndex ASC, name ASC")
    fun getAllExpenseCategories(): Flow<List<ExpenseCategoryEntity>>

    @Query("SELECT * FROM expense_categories ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllExpenseCategoriesList(): List<ExpenseCategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategories(categories: List<ExpenseCategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseCategory(category: ExpenseCategoryEntity)

    @Delete
    suspend fun deleteExpenseCategory(category: ExpenseCategoryEntity)

    // Expenses
    @Query("SELECT * FROM expenses ORDER BY dateMillis DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE snapshotId = :snapshotId ORDER BY dateMillis DESC")
    fun getExpensesBySnapshot(snapshotId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses WHERE snapshotId = :snapshotId")
    suspend fun getExpensesBySnapshotList(snapshotId: String): List<ExpenseEntity>

    @Query("SELECT * FROM expenses")
    suspend fun getAllExpensesList(): List<ExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<ExpenseEntity>)

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE snapshotId = :snapshotId")
    suspend fun deleteExpensesBySnapshot(snapshotId: String)

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()
}
