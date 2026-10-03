package com.example.ui.screens.expenses

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.local.entity.MonthlySnapshotEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.data.repository.PortfolioRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpensesUiState(
    val isLoading: Boolean = true,
    val selectedSnapshotId: String = "",
    val snapshots: List<MonthlySnapshotEntity> = emptyList(),
    val expenseCategories: List<ExpenseCategoryEntity> = emptyList(),
    val currentMonthExpenses: List<ExpenseEntity> = emptyList(),
    val activeRecurringExpenses: List<RecurringCashFlowEntity> = emptyList(),
    val expenseSummary: ExpenseSummary? = null,
    val categoryBreakdowns: List<ExpenseCategoryBreakdown> = emptyList(),
    val historicalExpensePoints: List<HistoricalDataPoint> = emptyList(),
    val portfolioMonthGrowth: Double = 0.0,
    val selectedExpenseRange: ExpenseComparisonRange = ExpenseComparisonRange.ONE_MONTH,
    val lastAddedExpenseDateMillis: Long? = null
)

class ExpensesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)

    private val _uiState = MutableStateFlow(ExpensesUiState())
    val uiState: StateFlow<ExpensesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllSnapshots(),
                repository.getAllExpenseCategories(),
                repository.getAllExpenses()
            ) { snapshots, categories, _ ->
                Pair(snapshots, categories)
            }.collect { (snapshots, categories) ->
                val activeSnapId = _uiState.value.selectedSnapshotId.ifEmpty {
                    snapshots.lastOrNull()?.id ?: ""
                }

                _uiState.update {
                    it.copy(
                        snapshots = snapshots,
                        selectedSnapshotId = activeSnapId,
                        expenseCategories = categories
                    )
                }

                refreshExpenseAnalytics(activeSnapId)
            }
        }
    }

    fun selectSnapshot(snapshotId: String) {
        _uiState.update { it.copy(selectedSnapshotId = snapshotId) }
        refreshExpenseAnalytics(snapshotId)
    }

    fun setExpenseRange(range: ExpenseComparisonRange) {
        _uiState.update { it.copy(selectedExpenseRange = range) }
        val snapId = _uiState.value.selectedSnapshotId
        if (snapId.isNotEmpty()) {
            refreshExpenseAnalytics(snapId)
        }
    }

    fun refreshExpenseAnalytics(snapshotId: String) {
        if (snapshotId.isEmpty()) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentMonthExpenses = emptyList(),
                    expenseSummary = null,
                    categoryBreakdowns = emptyList(),
                    historicalExpensePoints = emptyList(),
                    portfolioMonthGrowth = 0.0
                )
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentRange = _uiState.value.selectedExpenseRange
            val expenses = repository.getExpensesBySnapshotList(snapshotId).sortedByDescending { it.dateMillis }
            val lastAddedDate = expenses.maxOfOrNull { it.dateMillis }
            val summary = repository.calculateExpenseSummary(snapshotId, currentRange)
            val breakdowns = repository.getExpenseCategoryBreakdown(snapshotId, currentRange)
            val completeView = repository.getAllPortfolioViewsList().firstOrNull { it.id == "view_complete" }
                ?: repository.getAllPortfolioViewsList().firstOrNull()

            val historicalPoints = if (completeView != null) repository.getHistoricalPoints(completeView) else emptyList()
            val portfolioSummary = if (completeView != null) repository.calculatePortfolioSummary(snapshotId, completeView, TimeRangeOption.MOM) else null
            val activeRecurring = repository.getActiveRecurringExpenses(snapshotId)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    currentMonthExpenses = expenses,
                    activeRecurringExpenses = activeRecurring,
                    expenseSummary = summary,
                    categoryBreakdowns = breakdowns,
                    historicalExpensePoints = historicalPoints,
                    portfolioMonthGrowth = portfolioSummary?.growth?.diffAmount ?: 0.0,
                    lastAddedExpenseDateMillis = lastAddedDate
                )
            }
        }
    }

    fun autoAddRecurringExpenses(onComplete: ((Int) -> Unit)? = null) {
        val snapId = _uiState.value.selectedSnapshotId
        if (snapId.isEmpty()) return
        viewModelScope.launch {
            val added = repository.autoPopulateRecurringExpensesForSnapshot(snapId)
            refreshExpenseAnalytics(snapId)
            onComplete?.invoke(added)
        }
    }

    fun saveRecurringExpense(
        name: String,
        amount: Double,
        categoryId: String,
        dayOfMonth: Int = 1,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val snapId = _uiState.value.selectedSnapshotId.ifEmpty { "2026-01" }
            repository.saveRecurringCashFlow(
                RecurringCashFlowEntity(
                    name = name,
                    type = "FIXED_EXPENSE",
                    categoryId = categoryId,
                    assetId = null,
                    monthlyAmount = amount,
                    startMonth = snapId,
                    endMonth = null,
                    dayOfMonth = dayOfMonth,
                    isActive = true,
                    notes = notes
                )
            )
            refreshExpenseAnalytics(_uiState.value.selectedSnapshotId)
        }
    }

    fun deleteRecurringExpense(flow: RecurringCashFlowEntity) {
        viewModelScope.launch {
            repository.deleteRecurringCashFlow(flow)
            refreshExpenseAnalytics(_uiState.value.selectedSnapshotId)
        }
    }

    fun addExpense(
        title: String,
        categoryId: String,
        amount: Double,
        notes: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        val snapId = _uiState.value.selectedSnapshotId
        if (snapId.isEmpty()) return
        viewModelScope.launch {
            repository.saveExpense(
                ExpenseEntity(
                    snapshotId = snapId,
                    expenseCategoryId = categoryId,
                    amount = amount,
                    title = title,
                    dateMillis = dateMillis,
                    notes = notes
                )
            )
            refreshExpenseAnalytics(snapId)
        }
    }

    fun addBulkExpenses(expenses: List<ExpenseEntity>) {
        val snapId = _uiState.value.selectedSnapshotId
        if (snapId.isEmpty() || expenses.isEmpty()) return
        viewModelScope.launch {
            expenses.forEach { repository.saveExpense(it) }
            refreshExpenseAnalytics(snapId)
        }
    }

    fun updateExpense(expense: ExpenseEntity) {
        val snapId = _uiState.value.selectedSnapshotId
        if (snapId.isEmpty()) return
        viewModelScope.launch {
            repository.saveExpense(expense)
            refreshExpenseAnalytics(snapId)
        }
    }

    fun addExpenseCategory(
        name: String,
        iconName: String,
        colorHex: String,
        onComplete: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val id = "exp_cat_" + java.util.UUID.randomUUID().toString().take(8)
            repository.saveExpenseCategory(
                ExpenseCategoryEntity(
                    id = id,
                    name = name,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = false,
                    orderIndex = 50
                )
            )
            onComplete?.invoke(id)
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            refreshExpenseAnalytics(_uiState.value.selectedSnapshotId)
        }
    }
}
