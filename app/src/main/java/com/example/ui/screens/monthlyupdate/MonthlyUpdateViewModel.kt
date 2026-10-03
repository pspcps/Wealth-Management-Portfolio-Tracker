package com.example.ui.screens.monthlyupdate

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.*
import com.example.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.util.MathExpressionEvaluator

data class AssetItemEntry(
    val asset: AssetEntity,
    val category: CategoryEntity,
    var enteredValue: String = "",
    var enteredInvested: String = "",
    var contributions: String = "0",
    var withdrawals: String = "0",
    val previousValue: Double = 0.0,
    val previousInvested: Double = 0.0,
    val isUpdated: Boolean = false
) {
    val isCashAsset: Boolean get() = category.groupType == "CASH" || category.id == "bank_balance" || category.id == "cash"
    val currentValueDouble: Double get() = MathExpressionEvaluator.evaluate(enteredValue) ?: 0.0
    val investedAmountDouble: Double get() = if (isCashAsset) {
        currentValueDouble
    } else {
        MathExpressionEvaluator.evaluate(enteredInvested) ?: if (asset.initialInvestedAmount > 0) asset.initialInvestedAmount else currentValueDouble
    }
    val gainLossAmount: Double get() = currentValueDouble - investedAmountDouble
    val returnPercentage: Double? get() = if (investedAmountDouble > 0) ((currentValueDouble - investedAmountDouble) / investedAmountDouble) * 100.0 else null
}

data class MonthlyUpdateUiState(
    val isLoading: Boolean = true,
    val snapshots: List<MonthlySnapshotEntity> = emptyList(),
    val selectedSnapshotId: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val assetEntries: List<AssetItemEntry> = emptyList(),
    val previousSnapshotId: String? = null,
    val isSaving: Boolean = false,
    val saveSuccessMessage: String? = null
)

class MonthlyUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)

    private val _uiState = MutableStateFlow(MonthlyUpdateUiState())
    val uiState: StateFlow<MonthlyUpdateUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllSnapshots(),
                repository.getAllCategories(),
                repository.getActiveAssets()
            ) { snapshots, categories, assets ->
                Triple(snapshots, categories, assets)
            }.collect { (snapshots, categories, assets) ->
                val currentSnapId = _uiState.value.selectedSnapshotId.ifEmpty {
                    snapshots.lastOrNull()?.id ?: run {
                        // Default to current year-month
                        val cal = Calendar.getInstance()
                        String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                    }
                }

                _uiState.update {
                    it.copy(
                        snapshots = snapshots,
                        selectedSnapshotId = currentSnapId,
                        categories = categories
                    )
                }

                loadSnapshotValues(currentSnapId, assets, categories, snapshots)
            }
        }
    }

    fun selectSnapshot(snapshotId: String) {
        _uiState.update { it.copy(selectedSnapshotId = snapshotId) }
        viewModelScope.launch {
            val assets = repository.getAllAssetsList().filter { it.status == "ACTIVE" }
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            loadSnapshotValues(snapshotId, assets, categories, snapshots)
        }
    }

    fun createNewSnapshot(year: Int, month: Int) {
        viewModelScope.launch {
            val snapshotId = String.format("%04d-%02d", year, month)
            repository.saveSnapshot(
                MonthlySnapshotEntity(
                    id = snapshotId,
                    year = year,
                    month = month,
                    notes = ""
                )
            )
            selectSnapshot(snapshotId)
        }
    }

    private suspend fun loadSnapshotValues(
        snapshotId: String,
        assets: List<AssetEntity>,
        categories: List<CategoryEntity>,
        snapshots: List<MonthlySnapshotEntity>,
        preserveEdits: Map<Long, AssetItemEntry>? = null
    ) {
        // Ledger rule: An asset only belongs to a snapshot if its investment/creation month <= snapshotId
        val eligibleAssets = assets.filter { asset ->
            val startMonth = if (asset.investmentDate.length >= 7) {
                asset.investmentDate.take(7)
            } else {
                SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(asset.createdAt))
            }
            snapshotId.isEmpty() || startMonth <= snapshotId
        }

        val currentValues = repository.getValuesBySnapshotList(snapshotId).associateBy { it.assetId }
        val allValuesByAsset = repository.getAllAssetValuesList().groupBy { it.assetId }
        
        // Find previous snapshot
        val sortedSnapshots = snapshots.sortedWith(compareBy({ it.year }, { it.month }))
        val currentIndex = sortedSnapshots.indexOfFirst { it.id == snapshotId }
        val prevSnap = if (currentIndex > 0) {
            sortedSnapshots[currentIndex - 1]
        } else {
            sortedSnapshots.lastOrNull { it.id < snapshotId }
        }
        val prevValues = if (prevSnap != null) {
            repository.getValuesBySnapshotList(prevSnap.id).associateBy { it.assetId }
        } else {
            emptyMap()
        }

        val catMap = categories.associateBy { it.id }

        val entries = eligibleAssets.map { asset ->
            val cat = catMap[asset.categoryId] ?: CategoryEntity(
                id = asset.categoryId,
                name = asset.categoryId,
                groupType = "CUSTOM",
                iconName = "category",
                colorHex = "#6366F1"
            )

            // If user has in-progress edits on this screen, preserve them
            val preserved = preserveEdits?.get(asset.id)
            if (preserved != null) {
                return@map preserved.copy(asset = asset, category = cat)
            }

            val currentVal = currentValues[asset.id]
            val histVals = allValuesByAsset[asset.id] ?: emptyList()
            val mostRecentPrior = histVals
                .filter { it.snapshotId < snapshotId }
                .maxByOrNull { it.snapshotId }

            val prevVal = prevValues[asset.id]?.value
                ?: mostRecentPrior?.value
                ?: 0.0

            val prevInvested = prevValues[asset.id]?.investedAmount
                ?: mostRecentPrior?.investedAmount
                ?: asset.initialInvestedAmount

            val (enteredValStr, enteredInvStr, isUpd) = if (currentVal != null && currentVal.value > 0) {
                Triple(
                    Math.round(currentVal.value).toString(),
                    Math.round(if (currentVal.investedAmount > 0) currentVal.investedAmount else currentVal.value).toString(),
                    true
                )
            } else if (prevVal > 0) {
                // Carry forward previous known value so user does not get zeroed out
                Triple(
                    Math.round(prevVal).toString(),
                    Math.round(if (prevInvested > 0) prevInvested else prevVal).toString(),
                    false
                )
            } else if (asset.initialInvestedAmount > 0) {
                Triple(
                    Math.round(asset.initialInvestedAmount).toString(),
                    Math.round(asset.initialInvestedAmount).toString(),
                    false
                )
            } else {
                Triple("", "", false)
            }

            AssetItemEntry(
                asset = asset,
                category = cat,
                enteredValue = enteredValStr,
                enteredInvested = enteredInvStr,
                contributions = currentVal?.contributions?.let { if (it > 0) Math.round(it).toString() else "0" } ?: "0",
                withdrawals = currentVal?.withdrawals?.let { if (it > 0) Math.round(it).toString() else "0" } ?: "0",
                previousValue = prevVal,
                previousInvested = prevInvested,
                isUpdated = isUpd
            )
        }

        _uiState.update {
            it.copy(
                isLoading = false,
                assetEntries = entries,
                previousSnapshotId = prevSnap?.id
            )
        }
    }

    fun onValueChange(assetId: Long, newValue: String) {
        val cleanValue = MathExpressionEvaluator.sanitizeMathInput(newValue)
        _uiState.update { state ->
            val updatedEntries = state.assetEntries.map { entry ->
                if (entry.asset.id == assetId) {
                    val evaluated = MathExpressionEvaluator.evaluate(cleanValue) ?: 0.0
                    entry.copy(
                        enteredValue = cleanValue,
                        isUpdated = cleanValue.isNotEmpty() && evaluated > 0
                    )
                } else entry
            }
            state.copy(assetEntries = updatedEntries)
        }
    }

    fun onInvestedChange(assetId: Long, newInvested: String) {
        val cleanInvested = MathExpressionEvaluator.sanitizeMathInput(newInvested)
        _uiState.update { state ->
            val updatedEntries = state.assetEntries.map { entry ->
                if (entry.asset.id == assetId) {
                    entry.copy(
                        enteredInvested = cleanInvested
                    )
                } else entry
            }
            state.copy(assetEntries = updatedEntries)
        }
    }

    fun onContributionsChange(assetId: Long, newContributions: String) {
        val cleanContrib = MathExpressionEvaluator.sanitizeMathInput(newContributions)
        _uiState.update { state ->
            val updatedEntries = state.assetEntries.map { entry ->
                if (entry.asset.id == assetId) {
                    entry.copy(
                        contributions = cleanContrib
                    )
                } else entry
            }
            state.copy(assetEntries = updatedEntries)
        }
    }

    fun onWithdrawalsChange(assetId: Long, newWithdrawals: String) {
        val cleanWithdr = MathExpressionEvaluator.sanitizeMathInput(newWithdrawals)
        _uiState.update { state ->
            val updatedEntries = state.assetEntries.map { entry ->
                if (entry.asset.id == assetId) {
                    entry.copy(
                        withdrawals = cleanWithdr
                    )
                } else entry
            }
            state.copy(assetEntries = updatedEntries)
        }
    }

    fun transferAsset(
        fromAssetId: Long?,
        toAssetId: Long?,
        amount: Double,
        closeSource: Boolean,
        notes: String
    ) {
        val snapId = _uiState.value.selectedSnapshotId
        if (snapId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val message = repository.transferOrReinvestAsset(
                snapshotId = snapId,
                fromAssetId = fromAssetId,
                toAssetId = toAssetId,
                amount = amount,
                closeSourceAsset = closeSource,
                notes = notes
            )
            val assets = repository.getAllAssetsList().filter { it.status == "ACTIVE" }
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            loadSnapshotValues(snapId, assets, categories, snapshots)
            _uiState.update {
                it.copy(
                    isSaving = false,
                    saveSuccessMessage = message
                )
            }
        }
    }

    fun copyPreviousMonthValues() {
        val prevSnapId = _uiState.value.previousSnapshotId ?: return
        viewModelScope.launch {
            val prevValues = repository.getValuesBySnapshotList(prevSnapId).associateBy { it.assetId }
            _uiState.update { state ->
                val updatedEntries = state.assetEntries.map { entry ->
                    val pVal = prevValues[entry.asset.id]?.value ?: entry.previousValue
                    val pInv = prevValues[entry.asset.id]?.investedAmount ?: entry.previousInvested
                    val newEnteredVal = if (pVal > 0) Math.round(pVal).toString() else entry.enteredValue
                    val newEnteredInv = if (pInv > 0) Math.round(pInv).toString() else entry.enteredInvested
                    entry.copy(
                        enteredValue = newEnteredVal,
                        enteredInvested = newEnteredInv,
                        isUpdated = (newEnteredVal.toDoubleOrNull() ?: 0.0) > 0
                    )
                }
                state.copy(assetEntries = updatedEntries)
            }
        }
    }

    fun saveSnapshotValues() {
        val state = _uiState.value
        val snapId = state.selectedSnapshotId
        if (snapId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }

            // Ensure snapshot entity exists
            val parts = snapId.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 8
            repository.saveSnapshot(
                MonthlySnapshotEntity(
                    id = snapId,
                    year = y,
                    month = m,
                    notes = "",
                    updatedAt = System.currentTimeMillis()
                )
            )

            val valuesToSave = state.assetEntries.mapNotNull { entry ->
                val amount = MathExpressionEvaluator.evaluate(entry.enteredValue)
                val isCash = entry.isCashAsset
                val enteredInv = MathExpressionEvaluator.evaluate(entry.enteredInvested)
                val invested = if (isCash) (amount ?: entry.previousValue) else (enteredInv ?: amount ?: 0.0)
                if (amount != null && amount >= 0) {
                    AssetMonthlyValueEntity(
                        assetId = entry.asset.id,
                        snapshotId = snapId,
                        value = amount,
                        investedAmount = invested,
                        contributions = MathExpressionEvaluator.evaluate(entry.contributions) ?: 0.0,
                        withdrawals = MathExpressionEvaluator.evaluate(entry.withdrawals) ?: 0.0,
                        notes = ""
                    )
                } else if (entry.previousValue > 0) {
                    // Field was left blank: carry forward previous known value so it never becomes zero
                    AssetMonthlyValueEntity(
                        assetId = entry.asset.id,
                        snapshotId = snapId,
                        value = entry.previousValue,
                        investedAmount = if (entry.previousInvested > 0) entry.previousInvested else entry.previousValue,
                        contributions = 0.0,
                        withdrawals = 0.0,
                        notes = ""
                    )
                } else null
            }

            repository.saveAssetValues(valuesToSave)

            val updatedEntries = state.assetEntries.map { entry ->
                val evalVal = MathExpressionEvaluator.evaluate(entry.enteredValue)
                val hasVal = (evalVal ?: entry.previousValue) > 0
                val formattedVal = if (evalVal != null && evalVal > 0) MathExpressionEvaluator.formatEvaluated(evalVal) else entry.enteredValue
                val evalInv = if (entry.isCashAsset) evalVal else MathExpressionEvaluator.evaluate(entry.enteredInvested)
                val formattedInv = if (evalInv != null && evalInv > 0) MathExpressionEvaluator.formatEvaluated(evalInv) else entry.enteredInvested
                entry.copy(
                    enteredValue = formattedVal,
                    enteredInvested = formattedInv,
                    isUpdated = hasVal
                )
            }

            _uiState.update {
                it.copy(
                    isSaving = false,
                    assetEntries = updatedEntries,
                    saveSuccessMessage = "Successfully updated snapshot for ${repository.formatSnapshotLabel(snapId)}"
                )
            }
        }
    }

    fun saveSingleAssetEntry(assetId: Long) {
        val state = _uiState.value
        val snapId = state.selectedSnapshotId
        if (snapId.isEmpty()) return

        val entry = state.assetEntries.firstOrNull { it.asset.id == assetId } ?: return
        val isCash = entry.isCashAsset
        val amount = MathExpressionEvaluator.evaluate(entry.enteredValue) ?: 0.0
        val invested = if (isCash) amount else (MathExpressionEvaluator.evaluate(entry.enteredInvested) ?: amount)
        val contrib = MathExpressionEvaluator.evaluate(entry.contributions) ?: 0.0
        val withdr = MathExpressionEvaluator.evaluate(entry.withdrawals) ?: 0.0

        viewModelScope.launch {
            // Ensure snapshot entity exists
            val parts = snapId.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: 2026
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 8
            repository.saveSnapshot(
                MonthlySnapshotEntity(
                    id = snapId,
                    year = y,
                    month = m,
                    notes = "",
                    updatedAt = System.currentTimeMillis()
                )
            )

            val valueEntity = AssetMonthlyValueEntity(
                assetId = entry.asset.id,
                snapshotId = snapId,
                value = amount,
                investedAmount = invested,
                contributions = contrib,
                withdrawals = withdr,
                notes = ""
            )
            repository.saveAssetValue(valueEntity)

            // If invested amount changed, also sync asset initialInvestedAmount if needed
            if (invested > 0 && entry.asset.initialInvestedAmount <= 0) {
                repository.updateAsset(entry.asset.copy(initialInvestedAmount = invested))
            }

            _uiState.update { s ->
                val updated = s.assetEntries.map { 
                    if (it.asset.id == assetId) {
                        it.copy(
                            enteredValue = if (amount > 0) MathExpressionEvaluator.formatEvaluated(amount) else it.enteredValue,
                            enteredInvested = if (invested > 0) MathExpressionEvaluator.formatEvaluated(invested) else it.enteredInvested,
                            isUpdated = amount > 0
                        )
                    } else it 
                }
                s.copy(
                    assetEntries = updated,
                    saveSuccessMessage = "Saved ${entry.asset.name} (₹${Math.round(amount)})"
                )
            }
        }
    }

    fun importJsonSnapshot(jsonString: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = com.example.util.SnapshotImporter.importSnapshotJson(repository, jsonString)
            val snapshots = repository.getAllSnapshotsList()
            val snapId = if (result.snapshotId.isNotEmpty()) result.snapshotId else _uiState.value.selectedSnapshotId
            _uiState.update {
                it.copy(
                    selectedSnapshotId = snapId,
                    saveSuccessMessage = result.message
                )
            }
            val assets = repository.getAllAssetsList().filter { it.status == "ACTIVE" }
            val categories = repository.getAllCategoriesList()
            loadSnapshotValues(snapId, assets, categories, snapshots)
        }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    fun clearMonthlyEntry(assetId: Long) {
        val snapId = _uiState.value.selectedSnapshotId
        viewModelScope.launch {
            if (snapId.isNotEmpty()) {
                repository.deleteAssetMonthlyValue(assetId, snapId)
            }
            _uiState.update { state ->
                val updatedEntries = state.assetEntries.map { entry ->
                    if (entry.asset.id == assetId) {
                        entry.copy(
                            enteredValue = "",
                            enteredInvested = "",
                            isUpdated = false
                        )
                    } else entry
                }
                state.copy(
                    assetEntries = updatedEntries,
                    saveSuccessMessage = "Entry cleared for this month"
                )
            }
        }
    }

    fun deleteAsset(asset: AssetEntity) {
        viewModelScope.launch {
            val currentEdits = _uiState.value.assetEntries.associateBy { it.asset.id }
            repository.deleteAsset(asset)
            val assets = repository.getAllAssetsList().filter { it.status == "ACTIVE" }
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            loadSnapshotValues(
                _uiState.value.selectedSnapshotId,
                assets,
                categories,
                snapshots,
                preserveEdits = currentEdits.filterKeys { it != asset.id }
            )
            _uiState.update { it.copy(saveSuccessMessage = "Deleted ${asset.name}") }
        }
    }

    fun deleteSnapshot(snapshotId: String) {
        viewModelScope.launch {
            val snapshot = repository.getSnapshotById(snapshotId)
            if (snapshot != null) {
                repository.deleteSnapshot(snapshot)
            }
            val snapshots = repository.getAllSnapshotsList()
            val nextSnapId = snapshots.lastOrNull()?.id ?: run {
                val cal = Calendar.getInstance()
                String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            }
            _uiState.update { it.copy(selectedSnapshotId = nextSnapId) }
            val assets = repository.getAllAssetsList().filter { it.status == "ACTIVE" }
            val categories = repository.getAllCategoriesList()
            loadSnapshotValues(nextSnapId, assets, categories, snapshots)
            _uiState.update { it.copy(saveSuccessMessage = "Deleted month snapshot $snapshotId") }
        }
    }

    fun saveOrUpdateAsset(
        asset: AssetEntity,
        currentValue: Double? = null
    ) {
        viewModelScope.launch {
            val snapId = _uiState.value.selectedSnapshotId
            val defaultDate = if (snapId.isNotBlank()) "$snapId-01" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val assetToSave = if (asset.investmentDate.isBlank()) asset.copy(investmentDate = defaultDate) else asset

            val assetId = if (assetToSave.id != 0L) {
                repository.updateAsset(assetToSave)
                assetToSave.id
            } else {
                repository.saveAsset(assetToSave)
            }

            if (currentValue != null && currentValue >= 0 && snapId.isNotEmpty()) {
                val existingVal = repository.getValuesBySnapshotList(snapId).firstOrNull { it.assetId == assetId }
                val inv = if (assetToSave.initialInvestedAmount > 0) assetToSave.initialInvestedAmount else currentValue
                repository.saveAssetValue(
                    AssetMonthlyValueEntity(
                        assetId = assetId,
                        snapshotId = snapId,
                        value = currentValue,
                        investedAmount = inv,
                        contributions = existingVal?.contributions ?: 0.0,
                        withdrawals = existingVal?.withdrawals ?: 0.0,
                        notes = existingVal?.notes ?: ""
                    )
                )
            }

            val currentEdits = _uiState.value.assetEntries.associateBy { it.asset.id }
            val assets = repository.getAllAssetsList().filter { it.status == "ACTIVE" }
            val categories = repository.getAllCategoriesList()
            val snapshots = repository.getAllSnapshotsList()
            loadSnapshotValues(snapId, assets, categories, snapshots, preserveEdits = currentEdits)
            _uiState.update { it.copy(saveSuccessMessage = "Saved ${assetToSave.name}") }
        }
    }

    fun addNewAsset(name: String, categoryId: String, initialValue: String, initialInvested: String = "", notes: String) {
        viewModelScope.launch {
            val valAmount = initialValue.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            val invAmount = initialInvested.filter { it.isDigit() }.toDoubleOrNull() ?: valAmount
            val snapId = _uiState.value.selectedSnapshotId
            val defaultDate = if (snapId.isNotBlank()) "$snapId-01" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val newAsset = AssetEntity(
                categoryId = categoryId,
                name = name,
                notes = notes,
                status = "ACTIVE",
                initialInvestedAmount = invAmount,
                investmentDate = defaultDate
            )
            saveOrUpdateAsset(newAsset, valAmount)
        }
    }
}
