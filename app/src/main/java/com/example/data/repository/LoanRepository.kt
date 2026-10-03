package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.dao.LoanDao
import com.example.data.local.entity.LoanEntity
import com.example.domain.finance.LoanCalculators
import com.example.domain.model.LoanPortfolioSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LoanRepository(private val loanDao: LoanDao) {

    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()
    val activeLoans: Flow<List<LoanEntity>> = loanDao.getActiveLoans()

    val loanSummary: Flow<LoanPortfolioSummary> = allLoans.map { loans ->
        val active = loans.filter { it.isActive }
        val closed = loans.filter { !it.isActive }

        val totalOutstanding = active.sumOf { it.currentOutstanding }
        val totalMonthlyEmi = active.sumOf { it.monthlyEmi }
        val totalOriginal = loans.sumOf { it.originalPrincipal }

        val weightedRate = if (totalOutstanding > 0) {
            active.sumOf { it.currentOutstanding * it.annualInterestRate } / totalOutstanding
        } else if (active.isNotEmpty()) {
            active.map { it.annualInterestRate }.average()
        } else {
            0.0
        }

        LoanPortfolioSummary(
            totalOutstanding = totalOutstanding,
            totalMonthlyEmi = totalMonthlyEmi,
            totalOriginalPrincipal = totalOriginal,
            activeLoansCount = active.size,
            closedLoansCount = closed.size,
            weightedAverageInterestRate = weightedRate
        )
    }

    suspend fun getLoanById(id: Long): LoanEntity? = loanDao.getLoanById(id)

    suspend fun saveLoan(loan: LoanEntity): Long {
        val calculatedEmi = if (loan.monthlyEmi <= 0 && loan.originalPrincipal > 0 && loan.totalTenureMonths > 0) {
            LoanCalculators.calculateEmi(loan.originalPrincipal, loan.annualInterestRate, loan.totalTenureMonths)
        } else {
            loan.monthlyEmi
        }
        val entityToSave = loan.copy(monthlyEmi = calculatedEmi)

        return if (loan.id == 0L) {
            loanDao.insertLoan(entityToSave)
        } else {
            loanDao.updateLoan(entityToSave)
            loan.id
        }
    }

    suspend fun recordPrepayment(loanId: Long, amount: Double) {
        val loan = loanDao.getLoanById(loanId) ?: return
        val newOutstanding = (loan.currentOutstanding - amount).coerceAtLeast(0.0)
        val isClosed = newOutstanding <= 0.0
        loanDao.updateLoan(loan.copy(
            currentOutstanding = newOutstanding,
            isActive = !isClosed
        ))
    }

    suspend fun deleteLoan(id: Long) {
        loanDao.deleteLoanById(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: LoanRepository? = null

        fun getInstance(context: Context): LoanRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getInstance(context)
                val repo = LoanRepository(db.loanDao())
                INSTANCE = repo
                repo
            }
        }
    }
}
