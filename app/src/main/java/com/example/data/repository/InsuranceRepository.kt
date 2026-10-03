package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.InsuranceDao
import com.example.data.local.entity.InsuranceEntity
import com.example.domain.model.InsuranceShieldSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class InsuranceRepository(private val insuranceDao: InsuranceDao) {

    val allPolicies: Flow<List<InsuranceEntity>> = insuranceDao.getAllPolicies()
    val activePolicies: Flow<List<InsuranceEntity>> = insuranceDao.getActivePolicies()

    val insuranceShieldSummary: Flow<InsuranceShieldSummary> = allPolicies.map { policies ->
        val active = policies.filter { it.isActive }

        val lifeCover = active.filter { it.insuranceType == "TERM_LIFE" }.sumOf { it.sumInsured }
        val healthCover = active.filter { it.insuranceType == "HEALTH" || it.insuranceType == "CRITICAL_ILLNESS" }.sumOf { it.sumInsured }
        val otherCover = active.filter { it.insuranceType !in listOf("TERM_LIFE", "HEALTH", "CRITICAL_ILLNESS") }.sumOf { it.sumInsured }

        val totalAnnualPremiums = active.sumOf { policy ->
            when (policy.premiumFrequency.uppercase()) {
                "MONTHLY" -> policy.annualPremium * 12.0
                "QUARTERLY" -> policy.annualPremium * 4.0
                "HALF_YEARLY" -> policy.annualPremium * 2.0
                else -> policy.annualPremium
            }
        }

        val now = System.currentTimeMillis()
        val thirtyDaysFromNow = now + 30L * 24 * 60 * 60 * 1000
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val expiringSoon = active.count { policy ->
            if (policy.renewalDate.isBlank()) false
            else {
                try {
                    val date = dateFormat.parse(policy.renewalDate)
                    date != null && date.time in now..thirtyDaysFromNow
                } catch (e: Exception) {
                    false
                }
            }
        }

        val hasLife = lifeCover > 0
        val hasHealth = healthCover > 0

        InsuranceShieldSummary(
            totalLifeCover = lifeCover,
            totalHealthCover = healthCover,
            totalOtherCover = otherCover,
            totalAnnualPremiums = totalAnnualPremiums,
            activePoliciesCount = active.size,
            policiesExpiringSoonCount = expiringSoon,
            hasLifeInsurance = hasLife,
            hasHealthInsurance = hasHealth,
            isAdequatelyInsured = hasLife && hasHealth
        )
    }

    suspend fun getPolicyById(id: Long): InsuranceEntity? = insuranceDao.getPolicyById(id)

    suspend fun savePolicy(policy: InsuranceEntity): Long {
        return if (policy.id == 0L) {
            insuranceDao.insertPolicy(policy)
        } else {
            insuranceDao.updatePolicy(policy)
            policy.id
        }
    }

    suspend fun deletePolicy(id: Long) {
        insuranceDao.deletePolicyById(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: InsuranceRepository? = null

        fun getInstance(context: Context): InsuranceRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repo = InsuranceRepository(db.insuranceDao())
                INSTANCE = repo
                repo
            }
        }
    }
}
