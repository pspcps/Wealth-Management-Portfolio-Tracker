package com.example.ui.screens.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseCategoryEntity
import com.example.data.local.entity.PortfolioViewEntity
import com.example.data.repository.PortfolioRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val portfolioViews: List<PortfolioViewEntity> = emptyList(),
    val expenseCategories: List<ExpenseCategoryEntity> = emptyList()
)

class CategoriesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = PortfolioRepository(db)

    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            repository.ensureDefaults()
            combine(
                repository.getAllCategories(),
                repository.getAllPortfolioViews(),
                repository.getAllExpenseCategories()
            ) { categories, views, expenseCats ->
                Triple(categories, views, expenseCats)
            }.collect { (categories, views, expenseCats) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        categories = categories,
                        portfolioViews = views,
                        expenseCategories = expenseCats
                    )
                }
            }
        }
    }

    fun restoreDefaultCategories() {
        viewModelScope.launch {
            repository.restoreDefaultCategories()
        }
    }

    fun addCustomCategory(
        name: String,
        groupType: String,
        iconName: String,
        colorHex: String,
        description: String
    ) {
        viewModelScope.launch {
            val id = "cat_" + UUID.randomUUID().toString().take(8)
            repository.saveCategory(
                CategoryEntity(
                    id = id,
                    name = name,
                    groupType = groupType,
                    iconName = iconName,
                    colorHex = colorHex,
                    isDefault = false,
                    orderIndex = 50,
                    description = description
                )
            )
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            if (!category.isDefault) {
                repository.deleteCategory(category)
            }
        }
    }

    fun addCustomExpenseCategory(
        name: String,
        iconName: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            val id = "exp_cat_" + UUID.randomUUID().toString().take(8)
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
        }
    }

    fun deleteExpenseCategory(category: ExpenseCategoryEntity) {
        viewModelScope.launch {
            if (!category.isDefault) {
                repository.deleteExpenseCategory(category)
            }
        }
    }

    fun savePortfolioView(name: String, includedCategoryIds: List<String>) {
        viewModelScope.launch {
            val id = "view_" + UUID.randomUUID().toString().take(8)
            val csv = includedCategoryIds.joinToString(",")
            repository.savePortfolioView(
                PortfolioViewEntity(
                    id = id,
                    name = name,
                    isDefault = false,
                    includedCategoryIds = csv
                )
            )
        }
    }

    fun deletePortfolioView(view: PortfolioViewEntity) {
        viewModelScope.launch {
            if (!view.isDefault) {
                repository.deletePortfolioView(view)
            }
        }
    }
}
