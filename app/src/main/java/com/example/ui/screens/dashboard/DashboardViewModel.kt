package com.example.ui.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.CashbackRefundRepository
import com.example.data.repository.CashbackRefundSummary
import com.example.data.repository.FireSettingsRepository
import com.example.data.repository.InsuranceRepository
import com.example.data.repository.LoanRepository
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfile
import com.example.data.repository.UserProfileRepository
import com.example.domain.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile = UserProfile(),
    val fireSettings: FireSettings = FireSettings(),
    val fireForecast: FireForecastResult? = null,
    val snapshots: List<MonthlySnapshotEntity> = emptyList(),
    val selectedSnapshotId: String? = null,
    val portfolioViews: List<PortfolioViewEntity> = emptyList(),
    val selectedView: PortfolioViewEntity? = null,
    val selectedTimeRange: TimeRangeOption = TimeRangeOption.MOM,
    val portfolioSummary: PortfolioSummary? = null,
    val categoryGrowths: List<CategoryGrowthItem> = emptyList(),
    val allocations: List<AllocationSlice> = emptyList(),
    val historicalPoints: List<HistoricalDataPoint> = emptyList(),
    val expenseSummary: ExpenseSummary? = null,
    val multiRangeGrowths: Map<TimeRangeOption, GrowthMetric> = emptyMap(),
    val categories: List<CategoryEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val currentValues: List<AssetMonthlyValueEntity> = emptyList(),
    val recurringFlows: List<RecurringCashFlowEntity> = emptyList(),
    val recurringSummary: RecurringCashFlowSummary = RecurringCashFlowSummary(),
    val loanSummary: LoanPortfolioSummary = LoanPortfolioSummary(),
    val insuranceSummary: InsuranceShieldSummary = InsuranceShieldSummary(),
    val cashbackSummary: CashbackRefundSummary = CashbackRefundSummary(),
    val thisMonthCashbackRefund: Double = 0.0,
    val expenseBreakdowns: List<ExpenseCategoryBreakdown> = emptyList(),
    val selectedExpenseRange: ExpenseComparisonRange = ExpenseComparisonRange.ONE_MONTH
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)
    private val profileRepository = UserProfileRepository(application)
    private val fireSettingsRepository = FireSettingsRepository(application)
    private val loanRepository = LoanRepository.getInstance(application)
    private val insuranceRepository = InsuranceRepository.getInstance(application)
    private val cashbackRepository = CashbackRefundRepository.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeUserProfile()
        observeFireSettings()
        observeLoans()
        observeInsurance()
        observeCashbackRefunds()
    }

    private fun observeCashbackRefunds() {
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
        viewModelScope.launch {
            combine(
                cashbackRepository.summary,
                cashbackRepository.allItems
            ) { summary, items ->
                val thisMonthTotal = items.filter { item ->
                    val d = item.receivedDate?.ifBlank { item.expectedDate } ?: item.expectedDate
                    d.startsWith(currentMonthPrefix)
                }.sumOf { it.amount }
                summary to thisMonthTotal
            }.collect { (summary, thisMonthTotal) ->
                _uiState.update {
                    it.copy(
                        cashbackSummary = summary,
                        thisMonthCashbackRefund = thisMonthTotal
                    )
                }
            }
        }
    }

    private fun observeLoans() {
        viewModelScope.launch {
            loanRepository.loanSummary.collect { summary ->
                _uiState.update { it.copy(loanSummary = summary) }
            }
        }
    }

    private fun observeInsurance() {
        viewModelScope.launch {
            insuranceRepository.insuranceShieldSummary.collect { summary ->
                _uiState.update { it.copy(insuranceSummary = summary) }
            }
        }
    }

    private fun observeFireSettings() {
        viewModelScope.launch {
            fireSettingsRepository.fireSettings.collect { settings ->
                _uiState.update { state ->
                    val forecast = state.portfolioSummary?.let { sum ->
                        FireCalculator.calculateForecast(settings, sum.totalValue)
                    }
                    state.copy(
                        fireSettings = settings,
                        fireForecast = forecast
                    )
                }
            }
        }
    }

    fun updateFireSettings(settings: FireSettings) {
        viewModelScope.launch {
            fireSettingsRepository.updateFireSettings(settings)
            val currentNetWorth = _uiState.value.portfolioSummary?.totalValue ?: 0.0
            val forecast = FireCalculator.calculateForecast(settings, currentNetWorth)
            _uiState.update {
                it.copy(
                    fireSettings = settings,
                    fireForecast = forecast
                )
            }
        }
    }

    private fun observeUserProfile() {
        viewModelScope.launch {
            profileRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
    }

    fun updateUserProfile(name: String, email: String, tagline: String, targetNetWorth: Double, colorIndex: Int, dateOfBirth: String = "") {
        viewModelScope.launch {
            profileRepository.updateProfile(name, email, tagline, targetNetWorth, colorIndex, dateOfBirth)
        }
    }

    fun saveProfileImage(uri: android.net.Uri) {
        viewModelScope.launch {
            profileRepository.saveProfileImageFromUri(uri)
        }
    }

    fun removeProfileImage() {
        viewModelScope.launch {
            profileRepository.removeProfileImage()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.ensureDefaults()
            combine(
                repository.getAllSnapshots(),
                repository.getAllPortfolioViews(),
                repository.getAllCategories(),
                repository.getAllAssets(),
                repository.getAllRecurringCashFlows()
            ) { snapshots, views, categories, assets, flows ->
                DashboardDataBundle(snapshots, views, categories, assets, flows)
            }.collect { bundle ->
                val snapshots = bundle.snapshots
                val views = bundle.views
                val categories = bundle.categories
                val assets = bundle.assets
                val flows = bundle.flows

                val currentSelectedSnap = _uiState.value.selectedSnapshotId
                val activeSnapId = if (currentSelectedSnap != null && snapshots.any { it.id == currentSelectedSnap }) {
                    currentSelectedSnap
                } else {
                    snapshots.lastOrNull()?.id
                }

                val currentView = _uiState.value.selectedView
                val activeView = if (currentView != null && views.any { it.id == currentView.id }) {
                    views.first { it.id == currentView.id }
                } else {
                    views.firstOrNull { it.id == "view_complete" } ?: views.firstOrNull()
                }

                _uiState.update {
                    it.copy(
                        snapshots = snapshots,
                        selectedSnapshotId = activeSnapId,
                        portfolioViews = views,
                        selectedView = activeView,
                        categories = categories,
                        assets = assets,
                        recurringFlows = flows
                    )
                }

                refreshDashboardCalculations()
            }
        }
    }

    private data class DashboardDataBundle(
        val snapshots: List<MonthlySnapshotEntity>,
        val views: List<PortfolioViewEntity>,
        val categories: List<CategoryEntity>,
        val assets: List<AssetEntity>,
        val flows: List<RecurringCashFlowEntity>
    )

    fun selectSnapshot(snapshotId: String) {
        _uiState.update { it.copy(selectedSnapshotId = snapshotId) }
        refreshDashboardCalculations()
    }

    fun selectPortfolioView(view: PortfolioViewEntity) {
        _uiState.update { it.copy(selectedView = view) }
        refreshDashboardCalculations()
    }

    fun createCustomPortfolioView(name: String, categoryIds: List<String>) {
        if (name.isBlank() || categoryIds.isEmpty()) return
        viewModelScope.launch {
            val viewId = "view_custom_${System.currentTimeMillis()}"
            val newView = PortfolioViewEntity(
                id = viewId,
                name = name.trim(),
                isDefault = false,
                includedCategoryIds = categoryIds.joinToString(",")
            )
            repository.savePortfolioView(newView)
            _uiState.update { it.copy(selectedView = newView) }
            refreshDashboardCalculations()
        }
    }

    fun updateCustomPortfolioView(viewId: String, name: String, categoryIds: List<String>) {
        if (name.isBlank() || categoryIds.isEmpty()) return
        viewModelScope.launch {
            val updatedView = PortfolioViewEntity(
                id = viewId,
                name = name.trim(),
                isDefault = false,
                includedCategoryIds = categoryIds.joinToString(",")
            )
            repository.savePortfolioView(updatedView)
            if (_uiState.value.selectedView?.id == viewId) {
                _uiState.update { it.copy(selectedView = updatedView) }
            }
            refreshDashboardCalculations()
        }
    }

    fun deleteCustomPortfolioView(view: PortfolioViewEntity) {
        viewModelScope.launch {
            repository.deletePortfolioView(view)
            if (_uiState.value.selectedView?.id == view.id) {
                val fallbackView = _uiState.value.portfolioViews.firstOrNull { it.id != view.id && it.id == "view_complete" }
                    ?: _uiState.value.portfolioViews.firstOrNull { it.id != view.id }
                _uiState.update { it.copy(selectedView = fallbackView) }
            }
            refreshDashboardCalculations()
        }
    }

    fun selectTimeRange(range: TimeRangeOption) {
        _uiState.update { it.copy(selectedTimeRange = range) }
        refreshDashboardCalculations()
    }

    fun createNewSnapshot(year: Int, month: Int) {
        viewModelScope.launch {
            val snapshotId = String.format("%04d-%02d", year, month)
            val existing = repository.getSnapshotById(snapshotId)
            if (existing == null) {
                repository.saveSnapshot(
                    MonthlySnapshotEntity(
                        id = snapshotId,
                        year = year,
                        month = month,
                        notes = ""
                    )
                )
            }
            selectSnapshot(snapshotId)
        }
    }

    fun restoreDefaultCategories() {
        viewModelScope.launch {
            repository.restoreDefaultCategories()
            refreshDashboardCalculations()
        }
    }

    fun setExpenseRange(range: ExpenseComparisonRange) {
        _uiState.update { it.copy(selectedExpenseRange = range) }
        val snapId = _uiState.value.selectedSnapshotId ?: return
        viewModelScope.launch {
            val expenseSum = repository.calculateExpenseSummary(snapId, range)
            val expenseBreakdowns = repository.getExpenseCategoryBreakdown(snapId, range)
            _uiState.update {
                it.copy(
                    expenseSummary = expenseSum,
                    expenseBreakdowns = expenseBreakdowns
                )
            }
        }
    }

    fun refreshDashboardCalculations() {
        val state = _uiState.value
        val snapId = state.selectedSnapshotId
        val view = state.selectedView

        if (snapId == null || view == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    portfolioSummary = null,
                    categoryGrowths = emptyList(),
                    allocations = emptyList(),
                    historicalPoints = emptyList(),
                    expenseSummary = null,
                    multiRangeGrowths = emptyMap(),
                    recurringSummary = RecurringCashFlowSummary(),
                    currentValues = emptyList()
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val summary = repository.calculatePortfolioSummary(snapId, view, state.selectedTimeRange)
            val categories = repository.calculateCategoryGrowth(snapId, view, state.selectedTimeRange)
            val allocations = repository.calculateAssetAllocation(snapId, view)
            val historical = repository.getHistoricalPoints(view)
            val expenseSum = repository.calculateExpenseSummary(snapId, state.selectedExpenseRange)
            val expenseBreakdowns = repository.getExpenseCategoryBreakdown(snapId, state.selectedExpenseRange)
            val recurringSum = repository.calculateRecurringSummary(snapId)
            val values = repository.getValuesBySnapshotList(snapId)

            // Multi range metrics (1M, 3M, 6M, 12M, All)
            val multiMetrics = mutableMapOf<TimeRangeOption, GrowthMetric>()
            TimeRangeOption.values().forEach { range ->
                val s = repository.calculatePortfolioSummary(snapId, view, range)
                multiMetrics[range] = s.growth
            }

            val forecast = FireCalculator.calculateForecast(state.fireSettings, summary.totalValue)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    portfolioSummary = summary,
                    fireForecast = forecast,
                    categoryGrowths = categories,
                    allocations = allocations,
                    historicalPoints = historical,
                    expenseSummary = expenseSum,
                    expenseBreakdowns = expenseBreakdowns,
                    multiRangeGrowths = multiMetrics,
                    recurringSummary = recurringSum,
                    currentValues = values
                )
            }
        }
    }
}
