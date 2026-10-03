package com.example.ui.screens.recurring

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.data.repository.PortfolioRepository
import com.example.domain.model.RecurringCashFlowSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RecurringUiState(
    val isLoading: Boolean = true,
    val summary: RecurringCashFlowSummary = RecurringCashFlowSummary(),
    val items: List<RecurringCashFlowEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val assets: List<AssetEntity> = emptyList(),
    val selectedFilterType: String? = null // null for All
)

class RecurringCashFlowViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.ensureDefaults()
            val categories = repository.getAllCategoriesList()
            val assets = repository.getAllAssetsList()
            val flows = repository.getAllRecurringCashFlowsList()
            val summary = repository.calculateRecurringSummary()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    summary = summary,
                    items = flows,
                    categories = categories,
                    assets = assets
                )
            }
        }
    }

    fun setFilterType(type: String?) {
        _uiState.update { it.copy(selectedFilterType = type) }
    }

    fun saveRecurringCashFlow(
        id: Long = 0,
        name: String,
        type: String,
        categoryId: String,
        assetId: Long?,
        monthlyAmount: Double,
        startMonth: String,
        endMonth: String?,
        dayOfMonth: Int,
        isActive: Boolean,
        notes: String,
        frequency: String = "MONTHLY",
        installmentAmount: Double = 0.0,
        interestRate: Double = 0.0,
        principalPayoutPercent: Double = 0.0
    ) {
        viewModelScope.launch {
            val normalizedMonthly = if (monthlyAmount > 0.0) {
                monthlyAmount
            } else if (installmentAmount > 0.0) {
                when (frequency) {
                    "QUARTERLY" -> installmentAmount / 3.0
                    "HALF_YEARLY" -> installmentAmount / 6.0
                    "ANNUALLY" -> installmentAmount / 12.0
                    else -> installmentAmount
                }
            } else 0.0

            val effectiveInstallment = if (installmentAmount > 0.0) {
                installmentAmount
            } else {
                when (frequency) {
                    "QUARTERLY" -> normalizedMonthly * 3.0
                    "HALF_YEARLY" -> normalizedMonthly * 6.0
                    "ANNUALLY" -> normalizedMonthly * 12.0
                    else -> normalizedMonthly
                }
            }

            val entity = RecurringCashFlowEntity(
                id = id,
                name = name,
                type = type,
                categoryId = categoryId,
                assetId = assetId,
                monthlyAmount = normalizedMonthly,
                startMonth = startMonth,
                endMonth = endMonth,
                dayOfMonth = dayOfMonth,
                isActive = isActive,
                notes = notes,
                frequency = frequency,
                installmentAmount = effectiveInstallment,
                interestRate = interestRate,
                principalPayoutPercent = principalPayoutPercent
            )
            repository.saveRecurringCashFlow(entity)
            loadData()
        }
    }

    fun toggleFlowActive(flow: RecurringCashFlowEntity) {
        viewModelScope.launch {
            val updated = flow.copy(isActive = !flow.isActive)
            repository.updateRecurringCashFlow(updated)
            loadData()
        }
    }

    fun deleteFlow(flow: RecurringCashFlowEntity) {
        viewModelScope.launch {
            repository.deleteRecurringCashFlow(flow)
            loadData()
        }
    }
}
