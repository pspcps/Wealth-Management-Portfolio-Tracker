package com.example.ui.screens.portfolio

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.AssetMonthlyValueEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MonthlySnapshotEntity
import com.example.data.repository.PortfolioRepository
import com.example.domain.finance.AssetXirrSummary
import com.example.domain.finance.XirrEngine
import com.example.util.InvestmentImportResult
import com.example.util.ParsedInvestmentStatement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AssetWithHolding(
    val asset: AssetEntity,
    val category: CategoryEntity,
    val currentValue: Double = 0.0,
    val previousValue: Double = 0.0,
    val investedAmount: Double = 0.0,
    val gainLossAmount: Double = currentValue - investedAmount,
    val returnPercentage: Double? = if (investedAmount > 0) ((currentValue - investedAmount) / investedAmount) * 100.0 else null,
    val growthPercentage: Double? = null,
    val xirrSummary: AssetXirrSummary? = null
)

data class PortfolioUiState(
    val isLoading: Boolean = true,
    val selectedSnapshotId: String = "",
    val snapshots: List<MonthlySnapshotEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val assetsWithHoldings: List<AssetWithHolding> = emptyList(),
    val selectedGroupFilter: String = "ALL", // "ALL", "CASH", "INVESTMENT", "RETIREMENT", "BENEFIT", "CUSTOM"
    val searchQuery: String = "",
    val portfolioXirr: Double? = null,
    val categoryXirrMap: Map<String, Double> = emptyMap(),
    val totalInvested: Double = 0.0,
    val totalCurrentValue: Double = 0.0,
    val totalGainLoss: Double = 0.0,
    val totalReturnPercentage: Double? = null
)

class PortfolioViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)

    private val _uiState = MutableStateFlow(PortfolioUiState())
    val uiState: StateFlow<PortfolioUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllSnapshots(),
                repository.getAllCategories(),
                repository.getAllAssets()
            ) { snapshots, categories, assets ->
                Triple(snapshots, categories, assets)
            }.collect { (snapshots, categories, assets) ->
                val activeSnapId = _uiState.value.selectedSnapshotId.ifEmpty {
                    snapshots.lastOrNull()?.id ?: ""
                }

                _uiState.update {
                    it.copy(
                        snapshots = snapshots,
                        selectedSnapshotId = activeSnapId,
                        categories = categories
                    )
                }

                refreshHoldings(activeSnapId, assets, categories, snapshots)
            }
        }
    }

    private suspend fun refreshHoldings(
        snapshotId: String,
        assets: List<AssetEntity>,
        categories: List<CategoryEntity>,
        snapshots: List<MonthlySnapshotEntity>
    ) {
        val currentValues = if (snapshotId.isNotEmpty()) {
            repository.getValuesBySnapshotList(snapshotId).associateBy { it.assetId }
        } else emptyMap()

        val sortedSnapshots = snapshots.sortedWith(compareBy({ it.year }, { it.month }))
        val currentIndex = sortedSnapshots.indexOfFirst { it.id == snapshotId }
        val prevSnapId = if (currentIndex > 0) sortedSnapshots[currentIndex - 1].id else null
        val prevValues = if (prevSnapId != null) {
            repository.getValuesBySnapshotList(prevSnapId).associateBy { it.assetId }
        } else emptyMap()

        val catMap = categories.associateBy { it.id }
        val allValuesByAsset = repository.getAllAssetValuesList().groupBy { it.assetId }
        val activeSnap = sortedSnapshots.firstOrNull { it.id == snapshotId } ?: sortedSnapshots.lastOrNull()
        val asOfDate = activeSnap?.let {
            "${it.year}-${String.format("%02d", it.month)}-28"
        } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val holdings = assets.mapNotNull { asset ->
            // Ledger rule: An asset only exists in snapshotId if its start month <= snapshotId
            val assetStartMonth = if (asset.investmentDate.length >= 7) {
                asset.investmentDate.take(7)
            } else {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(asset.createdAt))
            }
            if (snapshotId.isNotEmpty() && assetStartMonth > snapshotId) {
                // Asset was created in a future month; do not include in historical snapshot
                return@mapNotNull null
            }

            val cat = catMap[asset.categoryId] ?: CategoryEntity(
                id = asset.categoryId,
                name = asset.categoryId,
                groupType = "CUSTOM",
                iconName = "category",
                colorHex = "#6366F1"
            )

            val curItem = currentValues[asset.id]
            val histVals = allValuesByAsset[asset.id] ?: emptyList()

            // Find the most recent value recorded for this asset on or prior to snapshotId
            val mostRecentPriorEntry = histVals
                .filter { it.snapshotId <= snapshotId }
                .maxByOrNull { it.snapshotId }

            val curVal = curItem?.value
                ?: mostRecentPriorEntry?.value
                ?: asset.initialInvestedAmount

            val curInvested = curItem?.let { if (it.investedAmount > 0) it.investedAmount else it.value }
                ?: mostRecentPriorEntry?.let { if (it.investedAmount > 0) it.investedAmount else it.value }
                ?: asset.initialInvestedAmount

            val prevVal = prevValues[asset.id]?.value
                ?: histVals.filter { it.snapshotId < snapshotId }.maxByOrNull { it.snapshotId }?.value
                ?: 0.0

            val growthPct = if (prevVal > 0) ((curVal - prevVal) / prevVal) * 100.0 else null

            val xirrSummary = XirrEngine.calculateAssetXirr(
                asset = asset,
                historicalValues = histVals,
                snapshots = snapshots,
                currentValue = curVal,
                asOfSnapshotId = snapshotId.ifEmpty { null }
            )

            val effectiveInvested = if (xirrSummary.totalInvested > 0) xirrSummary.totalInvested else curInvested

            AssetWithHolding(
                asset = asset,
                category = cat,
                currentValue = curVal,
                previousValue = prevVal,
                investedAmount = effectiveInvested,
                gainLossAmount = curVal - effectiveInvested,
                returnPercentage = if (effectiveInvested > 0) ((curVal - effectiveInvested) / effectiveInvested) * 100.0 else null,
                growthPercentage = growthPct,
                xirrSummary = xirrSummary
            )
        }

        // Category XIRR map
        val categoryXirrMap = mutableMapOf<String, Double>()
        categories.forEach { category ->
            val catHoldings = holdings.filter { it.category.id == category.id && it.asset.status == "ACTIVE" }
            val catSummaries = catHoldings.mapNotNull { it.xirrSummary }
            val catXirr = XirrEngine.calculateAggregatedXirr(catSummaries, asOfDate)
            if (catXirr != null) {
                categoryXirrMap[category.id] = catXirr
            }
        }

        // Portfolio XIRR (active assets)
        val activeHoldings = holdings.filter { it.asset.status == "ACTIVE" }
        val portfolioXirr = XirrEngine.calculateAggregatedXirr(activeHoldings.mapNotNull { it.xirrSummary }, asOfDate)

        val totalInvested = activeHoldings.sumOf { it.investedAmount }
        val totalCurrentVal = activeHoldings.sumOf { it.currentValue }
        val totalGainLoss = totalCurrentVal - totalInvested
        val totalReturnPct = if (totalInvested > 0) (totalGainLoss / totalInvested) * 100.0 else null

        _uiState.update {
            it.copy(
                isLoading = false,
                assetsWithHoldings = holdings,
                portfolioXirr = portfolioXirr,
                categoryXirrMap = categoryXirrMap,
                totalInvested = totalInvested,
                totalCurrentValue = totalCurrentVal,
                totalGainLoss = totalGainLoss,
                totalReturnPercentage = totalReturnPct
            )
        }
    }

    fun selectSnapshot(snapshotId: String) {
        _uiState.update { it.copy(selectedSnapshotId = snapshotId) }
        viewModelScope.launch {
            val assets = repository.getAllAssetsList()
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            refreshHoldings(snapshotId, assets, categories, snapshots)
        }
    }

    fun setGroupFilter(filter: String) {
        _uiState.update { it.copy(selectedGroupFilter = filter) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun saveAsset(asset: AssetEntity) {
        viewModelScope.launch {
            if (asset.id == 0L) {
                repository.saveAsset(asset)
            } else {
                repository.updateAsset(asset)
            }
            val assets = repository.getAllAssetsList()
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            refreshHoldings(_uiState.value.selectedSnapshotId, assets, categories, snapshots)
        }
    }

    fun updateAssetStatus(asset: AssetEntity, newStatus: String) {
        viewModelScope.launch {
            repository.updateAsset(asset.copy(status = newStatus))
            val assets = repository.getAllAssetsList()
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            refreshHoldings(_uiState.value.selectedSnapshotId, assets, categories, snapshots)
        }
    }

    fun importJsonSnapshot(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            com.example.util.SnapshotImporter.importSnapshotJson(repository, jsonString)
            val assets = repository.getAllAssetsList()
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            val activeSnapId = snapshots.lastOrNull()?.id ?: _uiState.value.selectedSnapshotId
            _uiState.update { it.copy(selectedSnapshotId = activeSnapId) }
            refreshHoldings(activeSnapId, assets, categories, snapshots)
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            repository.deleteAsset(asset)
            val assets = repository.getAllAssetsList()
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            refreshHoldings(_uiState.value.selectedSnapshotId, assets, categories, snapshots)
        }
    }

    fun transferAsset(
        fromAssetId: Long?,
        toAssetId: Long?,
        amount: Double,
        closeSource: Boolean,
        notes: String
    ) {
        val snapId = _uiState.value.selectedSnapshotId.ifEmpty {
            _uiState.value.snapshots.lastOrNull()?.id ?: return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.transferOrReinvestAsset(
                snapshotId = snapId,
                fromAssetId = fromAssetId,
                toAssetId = toAssetId,
                amount = amount,
                closeSourceAsset = closeSource,
                notes = notes
            )
            val assets = repository.getAllAssetsList()
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            refreshHoldings(snapId, assets, categories, snapshots)
        }
    }

    suspend fun importInvestmentStatement(
        statement: ParsedInvestmentStatement,
        targetSnapshotId: String,
        updateExisting: Boolean
    ): InvestmentImportResult {
        val snapId = targetSnapshotId.ifEmpty { statement.targetSnapshotId }.ifEmpty { "2026-09" }
        val parts = snapId.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: 2026
        val month = parts.getOrNull(1)?.toIntOrNull() ?: 9

        var snap = repository.getSnapshotById(snapId)
        if (snap == null) {
            snap = MonthlySnapshotEntity(
                id = snapId,
                year = year,
                month = month,
                notes = "Imported from ${statement.investmentType.displayName} statement on ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())}",
                updatedAt = System.currentTimeMillis()
            )
            repository.saveSnapshot(snap)
        }

        repository.ensureDefaults()
        val existingAssets = repository.getAllAssetsList()
        val assetMap = existingAssets.associateBy { it.name.trim().lowercase() }.toMutableMap()

        var importedCount = 0
        var updatedCount = 0
        val newValues = mutableListOf<AssetMonthlyValueEntity>()

        for (holding in statement.holdings) {
            val cleanKey = holding.name.trim().lowercase()
            val existing = assetMap[cleanKey]

            val currentAmount = if (holding.currentValue > 0.0) holding.currentValue else holding.investedAmount
            val investedAmount = if (holding.investedAmount > 0.0) holding.investedAmount else currentAmount

            val assetId: Long = if (existing != null && updateExisting) {
                val updatedNotes = if (holding.identifier.isNotEmpty()) {
                    val prefix = if (holding.categoryId == "mutual_funds") "Folio: " else "ISIN: "
                    if (existing.notes.contains(holding.identifier)) existing.notes
                    else if (existing.notes.isBlank()) "$prefix${holding.identifier}"
                    else "${existing.notes} | $prefix${holding.identifier}"
                } else existing.notes

                val updatedAsset = existing.copy(
                    notes = updatedNotes,
                    initialInvestedAmount = if (existing.initialInvestedAmount <= 0.0 && investedAmount > 0.0) investedAmount else existing.initialInvestedAmount,
                    investmentDate = if (existing.investmentDate.isBlank() && holding.investmentDate.isNotBlank()) holding.investmentDate else existing.investmentDate.ifEmpty { "$snapId-01" }
                )
                repository.updateAsset(updatedAsset)
                updatedCount++
                existing.id
            } else if (existing != null) {
                existing.id
            } else {
                val notes = buildString {
                    if (holding.amcOrIssuer.isNotBlank()) append(holding.amcOrIssuer)
                    if (holding.identifier.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        val prefix = if (holding.categoryId == "mutual_funds") "Folio: " else "ISIN: "
                        append(prefix).append(holding.identifier)
                    }
                    if (holding.units > 0.0) {
                        if (isNotEmpty()) append(" • ")
                        append("Units: ${holding.units}")
                    }
                }
                val holdingDate = holding.investmentDate.ifEmpty { statement.statementDate.ifEmpty { "$snapId-01" } }
                val newAsset = AssetEntity(
                    name = holding.name,
                    categoryId = holding.categoryId,
                    notes = notes,
                    status = "ACTIVE",
                    initialInvestedAmount = investedAmount,
                    investmentDate = holdingDate
                )
                val id = repository.saveAsset(newAsset)
                assetMap[cleanKey] = newAsset.copy(id = id)
                importedCount++
                id
            }

            newValues.add(
                AssetMonthlyValueEntity(
                    assetId = assetId,
                    snapshotId = snapId,
                    value = currentAmount,
                    investedAmount = investedAmount,
                    notes = if (holding.units > 0) "Units: ${holding.units}" else ""
                )
            )
        }

        repository.saveAssetValues(newValues)

        // Reload UI state
        val assets = repository.getAllAssetsList()
        val categories = repository.getAllCategoriesList()
        val snapshots = repository.getAllSnapshotsList()
        _uiState.update { it.copy(selectedSnapshotId = snapId) }
        refreshHoldings(snapId, assets, categories, snapshots)

        return InvestmentImportResult(
            success = true,
            message = "Successfully imported ${statement.holdings.size} holdings into $snapId ($importedCount new, $updatedCount updated)",
            importedCount = importedCount,
            updatedCount = updatedCount,
            snapshotId = snapId,
            totalInvested = statement.totalInvested,
            totalValue = statement.totalCurrentValue
        )
    }
}
