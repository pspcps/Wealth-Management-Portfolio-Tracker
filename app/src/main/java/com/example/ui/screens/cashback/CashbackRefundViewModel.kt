package com.example.ui.screens.cashback

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CashbackRefundEntity
import com.example.data.repository.CashbackRefundRepository
import com.example.data.repository.CashbackRefundSummary
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import java.util.Locale

val DEFAULT_SUGGESTED_PLATFORMS = listOf(
    "Amazon",
    "Flipkart",
    "Swiggy",
    "Zomato",
    "Myntra",
    "MakeMyTrip",
    "Google Pay",
    "PhonePe",
    "Store / Shop"
)

enum class CashbackTimeFilter {
    OVERALL,
    MONTHLY,
    YEARLY
}

data class CashbackRefundUiState(
    val allItems: List<CashbackRefundEntity> = emptyList(),
    val filteredItems: List<CashbackRefundEntity> = emptyList(),
    val availablePlatforms: List<String> = DEFAULT_SUGGESTED_PLATFORMS + listOf("Other"),

    // Top Level Filters
    val timeFilter: CashbackTimeFilter = CashbackTimeFilter.OVERALL,
    val selectedYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val selectedMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1, // 1..12
    val activeTypeTab: String = "ALL", // "ALL", "CASHBACK", "REFUND"
    val searchQuery: String = "",

    // Filtered Period Stats
    val periodTotalRecovered: Double = 0.0,
    val periodCashbackEarned: Double = 0.0,
    val periodRefundsReceived: Double = 0.0,
    val periodCount: Int = 0,

    // All-time summary
    val overallSummary: CashbackRefundSummary = CashbackRefundSummary(),

    // Add / Edit
    val isAddEditOpen: Boolean = false,
    val editingItem: CashbackRefundEntity? = null,

    // Feedback
    val messageToast: String? = null
) {
    val selectedMonthLabel: String
        get() {
            val monthName = DateFormatSymbols().months.getOrNull(selectedMonth - 1) ?: "Month $selectedMonth"
            return "$monthName $selectedYear"
        }

    val selectedYearLabel: String
        get() = "$selectedYear"
}

class CashbackRefundViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CashbackRefundRepository.getInstance(application)
    private val prefs = application.getSharedPreferences("cashback_custom_sources_prefs", android.content.Context.MODE_PRIVATE)

    private val _customPlatforms = MutableStateFlow<Set<String>>(loadCustomPlatforms())

    private fun loadCustomPlatforms(): Set<String> {
        return prefs.getStringSet("saved_sources", emptySet()) ?: emptySet()
    }

    fun addCustomPlatform(source: String) {
        val trimmed = source.trim()
        if (trimmed.isBlank() ||
            trimmed.equals("Other", ignoreCase = true) ||
            DEFAULT_SUGGESTED_PLATFORMS.any { it.equals(trimmed, ignoreCase = true) }
        ) {
            return
        }
        val current = _customPlatforms.value.toMutableSet()
        if (current.add(trimmed)) {
            prefs.edit().putStringSet("saved_sources", current).apply()
            _customPlatforms.value = current
        }
    }

    fun removeCustomPlatform(source: String) {
        val current = _customPlatforms.value.toMutableSet()
        if (current.remove(source)) {
            prefs.edit().putStringSet("saved_sources", current).apply()
            _customPlatforms.value = current
        }
    }

    private val _uiState = MutableStateFlow(CashbackRefundUiState())
    val uiState: StateFlow<CashbackRefundUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.allItems,
                repository.summary,
                _customPlatforms,
                _uiState.map { it.timeFilter }.distinctUntilChanged(),
                _uiState.map { it.selectedYear }.distinctUntilChanged(),
                _uiState.map { it.selectedMonth }.distinctUntilChanged(),
                _uiState.map { it.activeTypeTab }.distinctUntilChanged(),
                _uiState.map { it.searchQuery }.distinctUntilChanged()
            ) { args ->
                @Suppress("UNCHECKED_CAST")
                val items = args[0] as List<CashbackRefundEntity>
                val summary = args[1] as CashbackRefundSummary
                val customSet = args[2] as Set<String>
                val timeFilter = args[3] as CashbackTimeFilter
                val year = args[4] as Int
                val month = args[5] as Int
                val typeTab = args[6] as String
                val query = args[7] as String

                computeFilteredState(items, summary, customSet, timeFilter, year, month, typeTab, query)
            }.collect { updatedStateTransform ->
                _uiState.update { current -> updatedStateTransform(current) }
            }
        }
    }

    private fun computeFilteredState(
        items: List<CashbackRefundEntity>,
        summary: CashbackRefundSummary,
        customSet: Set<String>,
        timeFilter: CashbackTimeFilter,
        year: Int,
        month: Int,
        typeTab: String,
        query: String
    ): (CashbackRefundUiState) -> CashbackRefundUiState {
        // 0. Compute Dynamic Platform list: Default + Custom (Prefs & Transactions) + "Other"
        val existingFromItems = items.map { it.source.trim() }
            .filter { it.isNotBlank() && !it.equals("Other", ignoreCase = true) }
        val uniqueCustom = (customSet + existingFromItems)
            .filter { s -> !DEFAULT_SUGGESTED_PLATFORMS.any { it.equals(s, ignoreCase = true) } }
            .distinct()
            .sorted()
        val platforms = DEFAULT_SUGGESTED_PLATFORMS + uniqueCustom + listOf("Other")

        // 1. Filter by Time Period
        val monthPrefix = String.format(Locale.US, "%04d-%02d", year, month)
        val yearPrefix = String.format(Locale.US, "%04d", year)

        val periodItems = items.filter { item ->
            val dateStr = item.receivedDate?.ifBlank { item.expectedDate } ?: item.expectedDate
            when (timeFilter) {
                CashbackTimeFilter.OVERALL -> true
                CashbackTimeFilter.MONTHLY -> dateStr.startsWith(monthPrefix)
                CashbackTimeFilter.YEARLY -> dateStr.startsWith(yearPrefix)
            }
        }

        // 2. Metrics for this period
        val cbEarned = periodItems
            .filter { it.type.equals("CASHBACK", ignoreCase = true) }
            .sumOf { it.amount }
        val rfReceived = periodItems
            .filter { it.type.equals("REFUND", ignoreCase = true) }
            .sumOf { it.amount }
        val totRecovered = cbEarned + rfReceived
        val count = periodItems.size

        // 3. Filter by Type Tab & Search
        val displayItems = periodItems.filter { item ->
            val matchesType = when (typeTab) {
                "CASHBACK" -> item.type.equals("CASHBACK", ignoreCase = true)
                "REFUND" -> item.type.equals("REFUND", ignoreCase = true)
                else -> true
            }
            val matchesQuery = query.isBlank() ||
                    item.source.contains(query, ignoreCase = true) ||
                    item.title.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true)
            matchesType && matchesQuery
        }

        return { current ->
            current.copy(
                allItems = items,
                filteredItems = displayItems,
                availablePlatforms = platforms,
                overallSummary = summary,
                periodTotalRecovered = totRecovered,
                periodCashbackEarned = cbEarned,
                periodRefundsReceived = rfReceived,
                periodCount = count
            )
        }
    }

    fun setTimeFilter(filter: CashbackTimeFilter) {
        _uiState.update { it.copy(timeFilter = filter) }
    }

    fun setTypeTab(tab: String) {
        _uiState.update { it.copy(activeTypeTab = tab) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun prevMonth() {
        _uiState.update { state ->
            if (state.selectedMonth == 1) {
                state.copy(selectedMonth = 12, selectedYear = state.selectedYear - 1)
            } else {
                state.copy(selectedMonth = state.selectedMonth - 1)
            }
        }
    }

    fun nextMonth() {
        _uiState.update { state ->
            if (state.selectedMonth == 12) {
                state.copy(selectedMonth = 1, selectedYear = state.selectedYear + 1)
            } else {
                state.copy(selectedMonth = state.selectedMonth + 1)
            }
        }
    }

    fun prevYear() {
        _uiState.update { state -> state.copy(selectedYear = state.selectedYear - 1) }
    }

    fun nextYear() {
        _uiState.update { state -> state.copy(selectedYear = state.selectedYear + 1) }
    }

    fun setMonthYear(year: Int, month: Int) {
        _uiState.update { state -> state.copy(selectedYear = year, selectedMonth = month) }
    }

    fun resetToCurrentDate() {
        val cal = Calendar.getInstance()
        _uiState.update {
            it.copy(
                selectedYear = cal.get(Calendar.YEAR),
                selectedMonth = cal.get(Calendar.MONTH) + 1
            )
        }
    }

    fun openAdd(prefilledType: String = "CASHBACK") {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingItem = CashbackRefundEntity(
                    type = prefilledType,
                    status = "RECEIVED"
                )
            )
        }
    }

    fun openEdit(item: CashbackRefundEntity) {
        _uiState.update {
            it.copy(
                isAddEditOpen = true,
                editingItem = item
            )
        }
    }

    fun closeAddEdit() {
        _uiState.update {
            it.copy(
                isAddEditOpen = false,
                editingItem = null
            )
        }
    }

    fun saveItem(item: CashbackRefundEntity) {
        val sanitizedTitle = item.source.ifBlank {
            if (item.type.equals("CASHBACK", ignoreCase = true)) "Cashback" else "Refund"
        }
        val cleanDate = item.expectedDate.trim()
        val toSave = item.copy(
            title = sanitizedTitle,
            source = item.source.trim(),
            status = "RECEIVED",
            expectedDate = cleanDate,
            receivedDate = cleanDate,
            referenceNumber = "",
            notes = ""
        )
        viewModelScope.launch {
            repository.saveItem(toSave)
            addCustomPlatform(toSave.source)
            _uiState.update {
                it.copy(
                    isAddEditOpen = false,
                    editingItem = null,
                    messageToast = "Saved ${toSave.source} (₹${Math.round(toSave.amount)})"
                )
            }
        }
    }

    fun deleteItem(item: CashbackRefundEntity) {
        viewModelScope.launch {
            repository.deleteItem(item)
            _uiState.update { it.copy(messageToast = "Deleted ${item.source.ifBlank { item.title }}") }
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(messageToast = null) }
    }
}
