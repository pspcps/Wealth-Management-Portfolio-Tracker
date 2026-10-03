package com.example.ui.screens.insurance

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.InsuranceEntity
import com.example.data.repository.InsuranceRepository
import com.example.domain.model.InsuranceShieldSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InsuranceUiState(
    val policies: List<InsuranceEntity> = emptyList(),
    val summary: InsuranceShieldSummary = InsuranceShieldSummary(),
    val selectedFilterType: String? = null,
    val isAddEditOpen: Boolean = false,
    val editingPolicy: InsuranceEntity? = null,
    val selectedPolicyForDetails: InsuranceEntity? = null
)

class InsuranceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = InsuranceRepository.getInstance(application)

    private val _uiState = MutableStateFlow(InsuranceUiState())
    val uiState: StateFlow<InsuranceUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allPolicies.collect { policies ->
                _uiState.update { it.copy(policies = policies) }
            }
        }

        viewModelScope.launch {
            repository.insuranceShieldSummary.collect { summary ->
                _uiState.update { it.copy(summary = summary) }
            }
        }
    }

    fun setFilterType(type: String?) {
        _uiState.update { it.copy(selectedFilterType = type) }
    }

    fun openAddPolicy() {
        _uiState.update { it.copy(isAddEditOpen = true, editingPolicy = null) }
    }

    fun openEditPolicy(policy: InsuranceEntity) {
        _uiState.update { it.copy(isAddEditOpen = true, editingPolicy = policy) }
    }

    fun closeAddEdit() {
        _uiState.update { it.copy(isAddEditOpen = false, editingPolicy = null) }
    }

    fun selectPolicyForDetails(policy: InsuranceEntity?) {
        _uiState.update { it.copy(selectedPolicyForDetails = policy) }
    }

    fun savePolicy(policy: InsuranceEntity) {
        viewModelScope.launch {
            repository.savePolicy(policy)
            closeAddEdit()
        }
    }

    fun deletePolicy(id: Long) {
        viewModelScope.launch {
            repository.deletePolicy(id)
            if (_uiState.value.selectedPolicyForDetails?.id == id) {
                _uiState.update { it.copy(selectedPolicyForDetails = null) }
            }
        }
    }
}
