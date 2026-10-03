package com.example.ui.screens.loans

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.LoanEntity
import com.example.data.repository.LoanRepository
import com.example.domain.finance.LoanCalculators
import com.example.domain.model.LoanAmortizationScheduleItem
import com.example.domain.model.LoanPortfolioSummary
import com.example.domain.model.PrepaymentScenario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoansUiState(
    val loans: List<LoanEntity> = emptyList(),
    val summary: LoanPortfolioSummary = LoanPortfolioSummary(),
    val selectedLoan: LoanEntity? = null,
    val amortizationSchedule: List<LoanAmortizationScheduleItem> = emptyList(),
    val prepaymentScenario: PrepaymentScenario = PrepaymentScenario(),
    val isAddEditOpen: Boolean = false,
    val editingLoan: LoanEntity? = null,
    val isPrepayDialogOpen: Boolean = false
)

class LoansViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LoanRepository.getInstance(application)

    private val _uiState = MutableStateFlow(LoansUiState())
    val uiState: StateFlow<LoansUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allLoans.collect { loans ->
                _uiState.update { state ->
                    val updatedSelected = state.selectedLoan?.let { sel ->
                        loans.find { it.id == sel.id }
                    } ?: loans.firstOrNull { it.isActive }
                    
                    val schedule = if (updatedSelected != null) {
                        LoanCalculators.generateAmortizationSchedule(
                            principal = updatedSelected.currentOutstanding,
                            annualRatePercent = updatedSelected.annualInterestRate,
                            tenureMonths = updatedSelected.remainingTenureMonths
                        )
                    } else emptyList()

                    state.copy(
                        loans = loans,
                        selectedLoan = updatedSelected,
                        amortizationSchedule = schedule
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.loanSummary.collect { summary ->
                _uiState.update { it.copy(summary = summary) }
            }
        }
    }

    fun selectLoan(loan: LoanEntity) {
        val schedule = LoanCalculators.generateAmortizationSchedule(
            principal = loan.currentOutstanding,
            annualRatePercent = loan.annualInterestRate,
            tenureMonths = loan.remainingTenureMonths
        )
        val scenario = LoanCalculators.calculatePrepaymentImpact(
            currentBalance = loan.currentOutstanding,
            annualRatePercent = loan.annualInterestRate,
            currentRemainingTenureMonths = loan.remainingTenureMonths,
            currentMonthlyEmi = loan.monthlyEmi,
            extraMonthlyPayment = 5000.0,
            lumpSumPayment = 0.0
        )
        _uiState.update {
            it.copy(
                selectedLoan = loan,
                amortizationSchedule = schedule,
                prepaymentScenario = scenario
            )
        }
    }

    fun updatePrepaymentSimulation(extraMonthly: Double, lumpSum: Double) {
        val loan = _uiState.value.selectedLoan ?: return
        val scenario = LoanCalculators.calculatePrepaymentImpact(
            currentBalance = loan.currentOutstanding,
            annualRatePercent = loan.annualInterestRate,
            currentRemainingTenureMonths = loan.remainingTenureMonths,
            currentMonthlyEmi = loan.monthlyEmi,
            extraMonthlyPayment = extraMonthly,
            lumpSumPayment = lumpSum
        )
        _uiState.update { it.copy(prepaymentScenario = scenario) }
    }

    fun openAddLoan() {
        _uiState.update { it.copy(isAddEditOpen = true, editingLoan = null) }
    }

    fun openEditLoan(loan: LoanEntity) {
        _uiState.update { it.copy(isAddEditOpen = true, editingLoan = loan) }
    }

    fun closeAddEdit() {
        _uiState.update { it.copy(isAddEditOpen = false, editingLoan = null) }
    }

    fun openPrepayDialog(loan: LoanEntity) {
        _uiState.update { it.copy(isPrepayDialogOpen = true, selectedLoan = loan) }
    }

    fun closePrepayDialog() {
        _uiState.update { it.copy(isPrepayDialogOpen = false) }
    }

    fun saveLoan(loan: LoanEntity) {
        viewModelScope.launch {
            repository.saveLoan(loan)
            closeAddEdit()
        }
    }

    fun applyPrepayment(loanId: Long, amount: Double) {
        viewModelScope.launch {
            repository.recordPrepayment(loanId, amount)
            closePrepayDialog()
        }
    }

    fun deleteLoan(id: Long) {
        viewModelScope.launch {
            repository.deleteLoan(id)
        }
    }
}
