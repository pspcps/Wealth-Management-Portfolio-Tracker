package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PortfolioDao {
    // Categories
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    // Assets
    @Query("SELECT * FROM assets ORDER BY id ASC")
    fun getAllAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE status = 'ACTIVE' ORDER BY id ASC")
    fun getActiveAssets(): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets WHERE categoryId = :categoryId ORDER BY id ASC")
    fun getAssetsByCategory(categoryId: String): Flow<List<AssetEntity>>

    @Query("SELECT * FROM assets")
    suspend fun getAllAssetsList(): List<AssetEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: AssetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssets(assets: List<AssetEntity>)

    @Update
    suspend fun updateAsset(asset: AssetEntity)

    @Delete
    suspend fun deleteAsset(asset: AssetEntity)

    @Query("DELETE FROM assets WHERE id = :assetId")
    suspend fun deleteAssetById(assetId: Long)

    // Snapshots
    @Query("SELECT * FROM monthly_snapshots ORDER BY year ASC, month ASC")
    fun getAllSnapshots(): Flow<List<MonthlySnapshotEntity>>

    @Query("SELECT * FROM monthly_snapshots ORDER BY year ASC, month ASC")
    suspend fun getAllSnapshotsList(): List<MonthlySnapshotEntity>

    @Query("SELECT * FROM monthly_snapshots WHERE id = :id")
    suspend fun getSnapshotById(id: String): MonthlySnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: MonthlySnapshotEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<MonthlySnapshotEntity>)

    @Delete
    suspend fun deleteSnapshot(snapshot: MonthlySnapshotEntity)

    @Query("DELETE FROM monthly_snapshots WHERE id = :snapshotId")
    suspend fun deleteSnapshotById(snapshotId: String)

    // Asset Monthly Values
    @Query("SELECT * FROM asset_monthly_values WHERE snapshotId = :snapshotId")
    fun getValuesBySnapshot(snapshotId: String): Flow<List<AssetMonthlyValueEntity>>

    @Query("SELECT * FROM asset_monthly_values WHERE snapshotId = :snapshotId")
    suspend fun getValuesBySnapshotList(snapshotId: String): List<AssetMonthlyValueEntity>

    @Query("SELECT * FROM asset_monthly_values WHERE assetId = :assetId ORDER BY snapshotId ASC")
    fun getValuesByAsset(assetId: Long): Flow<List<AssetMonthlyValueEntity>>

    @Query("SELECT * FROM asset_monthly_values")
    fun getAllValues(): Flow<List<AssetMonthlyValueEntity>>

    @Query("SELECT * FROM asset_monthly_values")
    suspend fun getAllValuesList(): List<AssetMonthlyValueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetValue(value: AssetMonthlyValueEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetValues(values: List<AssetMonthlyValueEntity>)

    @Query("DELETE FROM asset_monthly_values WHERE snapshotId = :snapshotId")
    suspend fun deleteValuesBySnapshot(snapshotId: String)

    @Query("DELETE FROM asset_monthly_values WHERE assetId = :assetId")
    suspend fun deleteValuesByAsset(assetId: Long)

    @Query("DELETE FROM asset_monthly_values WHERE assetId = :assetId AND snapshotId = :snapshotId")
    suspend fun deleteAssetMonthlyValue(assetId: Long, snapshotId: String)

    @Query("DELETE FROM asset_monthly_values")
    suspend fun clearAllAssetMonthlyValues()

    @Query("DELETE FROM monthly_snapshots")
    suspend fun clearAllSnapshots()

    @Query("DELETE FROM assets")
    suspend fun clearAllAssets()

    @Query("DELETE FROM recurring_cash_flows")
    suspend fun clearAllRecurringCashFlows()

    // Portfolio Views
    @Query("SELECT * FROM portfolio_views ORDER BY name ASC")
    fun getAllPortfolioViews(): Flow<List<PortfolioViewEntity>>

    @Query("SELECT * FROM portfolio_views")
    suspend fun getAllPortfolioViewsList(): List<PortfolioViewEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioView(view: PortfolioViewEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPortfolioViews(views: List<PortfolioViewEntity>)

    @Delete
    suspend fun deletePortfolioView(view: PortfolioViewEntity)

    // Recurring Cash Flows & SIPs
    @Query("SELECT * FROM recurring_cash_flows ORDER BY isActive DESC, type ASC, name ASC")
    fun getAllRecurringCashFlows(): Flow<List<RecurringCashFlowEntity>>

    @Query("SELECT * FROM recurring_cash_flows ORDER BY isActive DESC, type ASC, name ASC")
    suspend fun getAllRecurringCashFlowsList(): List<RecurringCashFlowEntity>

    @Query("SELECT * FROM recurring_cash_flows WHERE assetId = :assetId")
    suspend fun getRecurringFlowsByAsset(assetId: Long): List<RecurringCashFlowEntity>

    @Query("SELECT * FROM recurring_cash_flows WHERE categoryId = :categoryId")
    suspend fun getRecurringFlowsByCategory(categoryId: String): List<RecurringCashFlowEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringCashFlow(flow: RecurringCashFlowEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringCashFlows(flows: List<RecurringCashFlowEntity>)

    @Update
    suspend fun updateRecurringCashFlow(flow: RecurringCashFlowEntity)

    @Delete
    suspend fun deleteRecurringCashFlow(flow: RecurringCashFlowEntity)
}
