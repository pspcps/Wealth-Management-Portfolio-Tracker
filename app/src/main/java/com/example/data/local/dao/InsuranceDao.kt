package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.InsuranceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InsuranceDao {
    @Query("SELECT * FROM insurance_policies ORDER BY isActive DESC, renewalDate ASC")
    fun getAllPolicies(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurance_policies WHERE isActive = 1 ORDER BY renewalDate ASC")
    fun getActivePolicies(): Flow<List<InsuranceEntity>>

    @Query("SELECT * FROM insurance_policies WHERE id = :id")
    suspend fun getPolicyById(id: Long): InsuranceEntity?

    @Query("SELECT * FROM insurance_policies")
    suspend fun getAllPoliciesList(): List<InsuranceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: InsuranceEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicies(policies: List<InsuranceEntity>)

    @Update
    suspend fun updatePolicy(policy: InsuranceEntity)

    @Delete
    suspend fun deletePolicy(policy: InsuranceEntity)

    @Query("DELETE FROM insurance_policies WHERE id = :id")
    suspend fun deletePolicyById(id: Long)

    @Query("DELETE FROM insurance_policies")
    suspend fun clearAllPolicies()

    suspend fun clearAllInsurances() = clearAllPolicies()
    suspend fun getAllInsurancesList(): List<InsuranceEntity> = getAllPoliciesList()
    suspend fun insertInsurances(policies: List<InsuranceEntity>) = insertPolicies(policies)
}
