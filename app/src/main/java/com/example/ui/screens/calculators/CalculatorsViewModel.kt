package com.example.ui.screens.calculators

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfile
import com.example.data.repository.UserProfileRepository
import com.example.domain.model.TimeRangeOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalculatorAutoPrefillData(
    val currentNetWorth: Double = 0.0,
    val monthlySipAmount: Double = 0.0,
    val avgMonthlyExpense: Double = 0.0,
    val currentAge: Int = 28,
    val userProfile: UserProfile = UserProfile(),
    val hasUserData: Boolean = false
)

class CalculatorsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)
    private val profileRepository = UserProfileRepository(application)

    private val _prefillState = MutableStateFlow(CalculatorAutoPrefillData())
    val prefillState: StateFlow<CalculatorAutoPrefillData> = _prefillState.asStateFlow()

    init {
        loadPrefillData()
    }

    fun loadPrefillData() {
        viewModelScope.launch {
            repository.ensureDefaults()

            // 1. Profile & Age
            val profile = profileRepository.userProfile.value
            val currentAge = profile.calculatedAge ?: 28

            // 2. Latest Snapshot for Net Worth & Expenses
            val snapshots = repository.getAllSnapshotsList().sortedWith(compareBy({ it.year }, { it.month }))
            val latestSnap = snapshots.lastOrNull()

            var latestNetWorth = 0.0
            var avgMonthlyExpenses = 0.0

            if (latestSnap != null) {
                val values = repository.getValuesBySnapshotList(latestSnap.id)
                latestNetWorth = values.sumOf { it.value }

                val allExpenses = repository.getAllExpensesList()
                val recentSnapshots = snapshots.takeLast(3)
                val recentSnapIds = recentSnapshots.map { it.id }.toSet()
                val recentExpenses = allExpenses.filter { it.snapshotId in recentSnapIds }
                
                if (recentSnapshots.isNotEmpty()) {
                    avgMonthlyExpenses = recentExpenses.sumOf { it.amount } / recentSnapshots.size
                }
                if (avgMonthlyExpenses <= 0.0) {
                    val currentMonthExpenses = allExpenses.filter { it.snapshotId == latestSnap.id }.sumOf { it.amount }
                    avgMonthlyExpenses = currentMonthExpenses
                }
            }

            // 3. Active SIP / Recurring flows
            val recurringSummary = repository.calculateRecurringSummary(latestSnap?.id)
            val activeSip = recurringSummary.totalActiveSipAmount

            val hasData = latestNetWorth > 0 || activeSip > 0 || avgMonthlyExpenses > 0 || profile.dateOfBirth.isNotBlank()

            _prefillState.update {
                it.copy(
                    currentNetWorth = if (latestNetWorth > 0) latestNetWorth else (profile.targetNetWorth * 0.2).coerceAtLeast(1000000.0),
                    monthlySipAmount = if (activeSip > 0) activeSip else 10000.0,
                    avgMonthlyExpense = if (avgMonthlyExpenses > 0) avgMonthlyExpenses else 50000.0,
                    currentAge = currentAge,
                    userProfile = profile,
                    hasUserData = hasData
                )
            }
        }
    }
}
