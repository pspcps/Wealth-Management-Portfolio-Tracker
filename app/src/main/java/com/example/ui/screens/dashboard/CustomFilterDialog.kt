package com.example.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.PortfolioViewEntity
import com.example.ui.components.IconMapper
import com.example.ui.theme.*

@Composable
fun CreateEditFilterDialog(
    initialFilter: PortfolioViewEntity? = null,
    allCategories: List<CategoryEntity>,
    allAssets: List<AssetEntity>,
    onDismiss: () -> Unit,
    onSaveFilter: (name: String, categoryIds: List<String>) -> Unit,
    onDeleteFilter: ((PortfolioViewEntity) -> Unit)? = null
) {
    var filterName by remember { mutableStateOf(initialFilter?.name ?: "") }
    
    // Parse initially selected category IDs
    val initialSelectedIds: Set<String> = remember(initialFilter, allCategories) {
        if (initialFilter == null) {
            emptySet()
        } else if (initialFilter.includedCategoryIds == "*") {
            allCategories.map { it.id }.toSet()
        } else {
            initialFilter.includedCategoryIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
    }

    var selectedCategoryIds by remember { mutableStateOf(initialSelectedIds) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Map assets by category
    val assetCountByCategory = remember(allAssets) {
        allAssets.groupBy { it.categoryId }.mapValues { it.value.size }
    }

    // Group categories by groupType
    val groupedCategories = remember(allCategories) {
        allCategories.groupBy { cat ->
            when (cat.groupType.uppercase()) {
                "INVESTMENT" -> "Investments & Growth"
                "RETIREMENT" -> "Retirement & Long-term"
                "BENEFIT" -> "Benefits & Stored Value"
                else -> "Cash, Bank & Liquid"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialFilter == null) "Create Custom Filter" else "Edit Portfolio Filter",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Filter dashboard calculations by selected categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Filter Name Input
                OutlinedTextField(
                    value = filterName,
                    onValueChange = {
                        filterName = it
                        if (errorMessage != null) errorMessage = null
                    },
                    label = { Text("Filter Name") },
                    placeholder = { Text("e.g. Bank + Bonds + FD, Liquid Assets, My Equities") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_filter_name"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HighDensityPrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0)
                    ),
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Color(0xFFDC2626))
                        }
                    }
                )

                // Quick Preset Selection Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "QUICK PRESETS",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Preset: Bank + Bonds + FD
                        PresetChip(label = "Bank + Bonds + FD") {
                            val target = setOf("bank_balance", "savings_accounts", "bonds", "fixed_deposits", "recurring_deposits")
                            selectedCategoryIds = allCategories.filter { target.contains(it.id) }.map { it.id }.toSet()
                            if (filterName.isBlank()) filterName = "Bank + Bonds + FD"
                        }

                        // Preset: All Investments
                        PresetChip(label = "All Investments") {
                            selectedCategoryIds = allCategories.filter { it.groupType == "INVESTMENT" }.map { it.id }.toSet()
                            if (filterName.isBlank()) filterName = "Investments Only"
                        }

                        // Preset: Liquid Only
                        PresetChip(label = "Liquid / Cash") {
                            val target = setOf("bank_balance", "savings_accounts", "fixed_deposits", "wallet_balance", "meal_card")
                            selectedCategoryIds = allCategories.filter { target.contains(it.id) }.map { it.id }.toSet()
                            if (filterName.isBlank()) filterName = "Liquid Assets"
                        }

                        // Preset: Retirement
                        PresetChip(label = "Retirement") {
                            selectedCategoryIds = allCategories.filter { it.groupType == "RETIREMENT" }.map { it.id }.toSet()
                            if (filterName.isBlank()) filterName = "Retirement Assets"
                        }

                        // Preset: Select All
                        PresetChip(label = "Select All") {
                            selectedCategoryIds = allCategories.map { it.id }.toSet()
                        }

                        // Preset: Clear
                        PresetChip(label = "Clear", isClear = true) {
                            selectedCategoryIds = emptySet()
                        }
                    }
                }

                // Selection Counter Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HighDensityPrimaryContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, HighDensityPillBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${selectedCategoryIds.size} of ${allCategories.size} Categories Selected",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityOnPrimaryContainer
                        )

                        Text(
                            text = if (selectedCategoryIds.size == allCategories.size) "Complete Portfolio" else "${selectedCategoryIds.size} active",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Multi-select Categories List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    groupedCategories.forEach { (groupTitle, categories) ->
                        item {
                            Text(
                                text = groupTitle.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }

                        items(categories, key = { it.id }) { category ->
                            val isSelected = selectedCategoryIds.contains(category.id)
                            val catColor = IconMapper.parseColor(category.colorHex)
                            val assetCount = assetCountByCategory[category.id] ?: 0

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) HighDensityPrimaryContainer.copy(alpha = 0.35f) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) HighDensityPrimary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedCategoryIds = if (isSelected) {
                                            selectedCategoryIds - category.id
                                        } else {
                                            selectedCategoryIds + category.id
                                        }
                                        if (errorMessage != null) errorMessage = null
                                    }
                                    .testTag("category_select_${category.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Category Icon
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = IconMapper.getIcon(category.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = category.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = if (assetCount > 0) "$assetCount holding${if (assetCount > 1) "s" else ""}" else "No holdings added",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedCategoryIds = if (checked) {
                                                selectedCategoryIds + category.id
                                            } else {
                                                selectedCategoryIds - category.id
                                            }
                                            if (errorMessage != null) errorMessage = null
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = HighDensityPrimary,
                                            checkmarkColor = Color.White
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delete Button (if editing existing custom filter)
                    if (initialFilter != null && !initialFilter.isDefault && onDeleteFilter != null) {
                        OutlinedButton(
                            onClick = {
                                onDeleteFilter(initialFilter)
                                onDismiss()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFDC2626)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextSecondary),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (filterName.isBlank()) {
                                errorMessage = "Please enter a filter name"
                                return@Button
                            }
                            if (selectedCategoryIds.isEmpty()) {
                                errorMessage = "Select at least one category"
                                return@Button
                            }

                            val categoryList = if (selectedCategoryIds.size == allCategories.size) {
                                listOf("*")
                            } else {
                                selectedCategoryIds.toList()
                            }

                            onSaveFilter(filterName.trim(), categoryList)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("btn_save_custom_filter"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    label: String,
    isClear: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = if (isClear) Color(0xFFFEE2E2) else Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, if (isClear) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.SemiBold,
            color = if (isClear) Color(0xFFDC2626) else HighDensityTextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ManageFiltersDialog(
    views: List<PortfolioViewEntity>,
    selectedView: PortfolioViewEntity?,
    allCategories: List<CategoryEntity>,
    onDismiss: () -> Unit,
    onSelectView: (PortfolioViewEntity) -> Unit,
    onOpenCreateFilter: () -> Unit,
    onEditFilter: (PortfolioViewEntity) -> Unit,
    onDeleteFilter: (PortfolioViewEntity) -> Unit
) {
    val categoryMap = remember(allCategories) { allCategories.associateBy { it.id } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Portfolio Filters & Views",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Select, customize, or create custom portfolio views",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Create Filter Button
                Button(
                    onClick = {
                        onDismiss()
                        onOpenCreateFilter()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_open_create_filter"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Custom Filter")
                }

                // List of Filters
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(views, key = { it.id }) { view ->
                        val isSelected = view.id == selectedView?.id
                        val isDefault = view.isDefault
                        val includedCount = if (view.includedCategoryIds == "*") {
                            allCategories.size
                        } else {
                            view.includedCategoryIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }.size
                        }

                        val includedNames = if (view.includedCategoryIds == "*") {
                            "All ${allCategories.size} Categories"
                        } else {
                            view.includedCategoryIds.split(",")
                                .mapNotNull { categoryMap[it.trim()]?.name }
                                .take(4)
                                .joinToString(", ") + if (includedCount > 4) " +${includedCount - 4} more" else ""
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) HighDensityPrimaryContainer else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) HighDensityPillBorder else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    onSelectView(view)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        onSelectView(view)
                                        onDismiss()
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = HighDensityPrimary)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = view.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSelected) HighDensityOnPrimaryContainer else HighDensityTextPrimary
                                        )

                                        if (isDefault) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFE2E8F0)
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = HighDensityTextSecondary,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = includedNames,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary,
                                        maxLines = 2
                                    )
                                }

                                // Edit or Delete actions for custom filters
                                if (!isDefault) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                onDismiss()
                                                onEditFilter(view)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = HighDensityPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = { onDeleteFilter(view) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
