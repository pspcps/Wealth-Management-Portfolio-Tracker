package com.example.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.MonthlySnapshotEntity
import com.example.data.local.entity.PortfolioViewEntity
import com.example.data.repository.PortfolioRepository
import com.example.domain.model.TwoPeriodComparison
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportsUiState(
    val isLoading: Boolean = true,
    val snapshots: List<MonthlySnapshotEntity> = emptyList(),
    val portfolioViews: List<PortfolioViewEntity> = emptyList(),
    val selectedView: PortfolioViewEntity? = null,
    val periodAId: String = "",
    val periodBId: String = "",
    val comparison: TwoPeriodComparison? = null
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllSnapshots(),
                repository.getAllPortfolioViews()
            ) { snapshots, views ->
                Pair(snapshots, views)
            }.collect { (snapshots, views) ->
                val sorted = snapshots.sortedWith(compareBy({ it.year }, { it.month }))
                val pA = if (sorted.size >= 2) sorted.first().id else (sorted.firstOrNull()?.id ?: "")
                val pB = sorted.lastOrNull()?.id ?: ""
                val defaultView = views.firstOrNull { it.id == "view_complete" } ?: views.firstOrNull()

                _uiState.update {
                    it.copy(
                        snapshots = snapshots,
                        portfolioViews = views,
                        selectedView = defaultView,
                        periodAId = pA,
                        periodBId = pB
                    )
                }

                if (pA.isNotEmpty() && pB.isNotEmpty() && defaultView != null) {
                    runComparison(pA, pB, defaultView)
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    fun setPeriodA(id: String) {
        _uiState.update { it.copy(periodAId = id) }
        val state = _uiState.value
        state.selectedView?.let { runComparison(id, state.periodBId, it) }
    }

    fun setPeriodB(id: String) {
        _uiState.update { it.copy(periodBId = id) }
        val state = _uiState.value
        state.selectedView?.let { runComparison(state.periodAId, id, it) }
    }

    fun selectPortfolioView(view: PortfolioViewEntity) {
        _uiState.update { it.copy(selectedView = view) }
        val state = _uiState.value
        runComparison(state.periodAId, state.periodBId, view)
    }

    private fun runComparison(pA: String, pB: String, view: PortfolioViewEntity) {
        if (pA.isEmpty() || pB.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val comp = repository.compareTwoPeriods(pA, pB, view)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    comparison = comp
                )
            }
        }
    }
}
