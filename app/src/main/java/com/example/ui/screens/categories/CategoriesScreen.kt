package com.example.ui.screens.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.IconMapper
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Asset Categories, 1: Portfolio Views, 2: Expense Categories
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddViewDialog by remember { mutableStateOf(false) }
    var showAddExpenseCategoryDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = HighDensityBg,
        topBar = {
            Surface(
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_categories")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HighDensityTextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Categories & Views",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Manage classification & definitions",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(
                            onClick = { viewModel.restoreDefaultCategories() },
                            modifier = Modifier.testTag("btn_restore_defaults_topbar")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Restore Defaults",
                                tint = HighDensityPrimary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 2.dp,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (activeTab == 0) showAddCategoryDialog = true
                                    else if (activeTab == 1) showAddViewDialog = true
                                    else if (activeTab == 2) showAddExpenseCategoryDialog = true
                                }
                                .testTag("btn_add_category_top")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // Tab Selector Pill Container
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            0 to "Assets (${state.categories.size})",
                            1 to "Views (${state.portfolioViews.size})",
                            2 to "Expenses (${state.expenseCategories.size})"
                        ).forEach { (tabIdx, label) ->
                            val isSel = activeTab == tabIdx
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { activeTab = tabIdx },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) HighDensityPrimaryContainer else Color.Transparent,
                                border = BorderStroke(1.dp, if (isSel) HighDensityPillBorder else Color.Transparent)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) HighDensityOnPrimaryContainer else HighDensityTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 0: Asset Categories
            if (activeTab == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.restoreDefaultCategories() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Defaults", color = HighDensityPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                items(state.categories, key = { it.id }) { cat ->
                    val catColor = IconMapper.parseColor(cat.colorHex)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = catColor.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = IconMapper.getIcon(cat.iconName),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        cat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = "${cat.groupType} • ${if (cat.isDefault) "Default" else "Custom"}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            if (!cat.isDefault) {
                                IconButton(onClick = { viewModel.deleteCategory(cat) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 1: Portfolio Views
            if (activeTab == 1) {
                item {
                    Button(
                        onClick = { showAddViewDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Custom View / Filter", fontWeight = FontWeight.Bold)
                    }
                }

                items(state.portfolioViews, key = { it.id }) { view ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    view.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                                val count = if (view.includedCategoryIds == "ALL") "All Categories" else "${view.includedCategoryIds.split(",").size} Included Categories"
                                Text(
                                    text = count,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }

                            if (!view.isDefault) {
                                IconButton(onClick = { viewModel.deletePortfolioView(view) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: Expense Categories
            if (activeTab == 2) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddExpenseCategoryDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Expense Category", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { viewModel.restoreDefaultCategories() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Defaults", color = HighDensityPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }

                items(state.expenseCategories, key = { it.id }) { expCat ->
                    val color = IconMapper.parseColor(expCat.colorHex)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = color.copy(alpha = 0.15f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = IconMapper.getIcon(expCat.iconName),
                                            contentDescription = null,
                                            tint = color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        expCat.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = if (expCat.isDefault) "Standard category" else "Custom category",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            if (!expCat.isDefault) {
                                IconButton(onClick = { viewModel.deleteExpenseCategory(expCat) }) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = Color(0xFFEF4444)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { name, group, icon, color, desc ->
                viewModel.addCustomCategory(name, group, icon, color, desc)
                showAddCategoryDialog = false
            }
        )
    }

    if (showAddExpenseCategoryDialog) {
        AddExpenseCategoryDialog(
            onDismiss = { showAddExpenseCategoryDialog = false },
            onAdd = { name, icon, color ->
                viewModel.addCustomExpenseCategory(name, icon, color)
                showAddExpenseCategoryDialog = false
            }
        )
    }

    if (showAddViewDialog) {
        AddViewDialog(
            categories = state.categories,
            onDismiss = { showAddViewDialog = false },
            onSave = { name, cats ->
                viewModel.savePortfolioView(name, cats)
                showAddViewDialog = false
            }
        )
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, group: String, icon: String, color: String, desc: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("Equities & Stocks") }
    var icon by remember { mutableStateOf("ShowChart") }
    var color by remember { mutableStateOf("#3B82F6") }
    var description by remember { mutableStateOf("") }

    val groups = listOf("Equities & Stocks", "Mutual Funds", "Fixed Income & Debt", "Gold & Precious Metals", "Retirement & Pension", "Real Estate", "Cash & Liquid", "Alternative & Crypto")
    val icons = listOf("ShowChart", "AccountBalance", "Paid", "Savings", "MonetizationOn", "Assessment", "Security", "Home", "CurrencyBitcoin", "PieChart")
    val colors = listOf("#3B82F6", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899", "#6366F1", "#14B8A6")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Asset Category", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name (e.g. SmallCap Funds)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Asset Group", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(groups) { g ->
                        val isSel = group == g
                        FilterChip(
                            selected = isSel,
                            onClick = { group = g },
                            label = { Text(g, fontSize = 11.sp) }
                        )
                    }
                }

                Text("Icon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons) { ic ->
                        val isSel = icon == ic
                        Surface(
                            shape = CircleShape,
                            color = if (isSel) HighDensityPrimaryContainer else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) HighDensityPrimary else Color.Transparent),
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { icon = ic }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = IconMapper.getIcon(ic),
                                    contentDescription = null,
                                    tint = if (isSel) HighDensityPrimary else HighDensityTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colors) { c ->
                        val isSel = color == c
                        val parsed = IconMapper.parseColor(c)
                        Surface(
                            shape = CircleShape,
                            color = parsed,
                            border = BorderStroke(2.dp, if (isSel) HighDensityTextPrimary else Color.Transparent),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { color = c }
                        ) {}
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), group, icon, color, description.trim())
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Create Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddViewDialog(
    categories: List<com.example.data.local.entity.CategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (name: String, categoryIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val selectedCats = remember { mutableStateListOf<String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Portfolio View", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("View Name (e.g. Liquid & Debt)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Categories to Include:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories) { cat ->
                        val isChecked = selectedCats.contains(cat.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) selectedCats.remove(cat.id)
                                    else selectedCats.add(cat.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { chk ->
                                    if (chk) selectedCats.add(cat.id)
                                    else selectedCats.remove(cat.id)
                                }
                            )
                            Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && selectedCats.isNotEmpty()) {
                        onSave(name.trim(), selectedCats.toList())
                    }
                },
                enabled = name.isNotBlank() && selectedCats.isNotEmpty()
            ) {
                Text("Save View")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddExpenseCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, icon: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("shopping_cart") }
    var color by remember { mutableStateOf("#16A34A") }

    val expenseIcons = listOf(
        "shopping_cart" to "Groceries",
        "restaurant" to "Dining",
        "local_cafe" to "Cafe",
        "fastfood" to "Snacks",
        "home" to "Rent/House",
        "bolt" to "Utilities",
        "shopping_bag" to "Shopping",
        "directions_car" to "Fuel/Car",
        "directions_bus" to "Transit",
        "school" to "Education",
        "local_hospital" to "Medical",
        "health_and_safety" to "Insurance",
        "fitness_center" to "Fitness",
        "pets" to "Pets",
        "phone_android" to "Mobile/Recharge",
        "subscriptions" to "Software",
        "build" to "Maintenance",
        "flight" to "Travel",
        "movie" to "Entertainment",
        "family_restroom" to "Family",
        "card_giftcard" to "Gifts"
    )

    val colors = listOf(
        "#16A34A", // Green
        "#F97316", // Orange
        "#3B82F6", // Blue
        "#EAB308", // Yellow
        "#EC4899", // Pink
        "#EF4444", // Red
        "#8B5CF6", // Purple
        "#06B6D4", // Cyan
        "#6366F1", // Indigo
        "#14B8A6", // Teal
        "#64748B"  // Slate
    )

    val isEssential = remember(name) {
        val key = name.lowercase().trim()
        val essentialKeywords = listOf(
            "grocer", "supermarket", "ration", "vegetable", "milk", "kirana", "provisions",
            "rent", "hous", "utilit", "bill", "electr", "water", "wifi", "gas", "cylinder",
            "transport", "fuel", "petrol", "diesel", "cab", "auto", "commute", "bus", "metro",
            "educat", "course", "school", "college", "tuition", "book", "fee",
            "medic", "health", "doctor", "hospital", "pharma", "medicine", "clinic",
            "insur", "emi", "loan", "maintenance", "tax", "essential", "need"
        )
        essentialKeywords.any { key.contains(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Category, contentDescription = null, tint = HighDensityPrimary)
                Text("New Expense Category", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Groceries & Daily Needs, Gym, Pets") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Live Preview Pill
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val parsedColor = IconMapper.parseColor(color)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = parsedColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = IconMapper.getIcon(icon),
                                    contentDescription = null,
                                    tint = parsedColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (name.isNotBlank()) name else "Preview Category",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isEssential) Color(0xFFDCFCE7) else Color(0xFFFEF3C7)
                                ) {
                                    Text(
                                        text = if (isEssential) "Essential (Needs)" else "Discretionary (Wants)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEssential) Color(0xFF15803D) else Color(0xFFB45309),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Text("Select Icon", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(expenseIcons) { (icKey, icLabel) ->
                        val isSel = icon == icKey
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) HighDensityPrimaryContainer else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isSel) HighDensityPrimary else Color.Transparent),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { icon = icKey }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = IconMapper.getIcon(icKey),
                                    contentDescription = null,
                                    tint = if (isSel) HighDensityPrimary else HighDensityTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = icLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) HighDensityPrimary else HighDensityTextSecondary
                                )
                            }
                        }
                    }
                }

                Text("Select Color", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colors) { c ->
                        val isSel = color == c
                        val parsed = IconMapper.parseColor(c)
                        Surface(
                            shape = CircleShape,
                            color = parsed,
                            border = BorderStroke(2.dp, if (isSel) HighDensityTextPrimary else Color.Transparent),
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable { color = c }
                        ) {}
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onAdd(name.trim(), icon, color)
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Create Category")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

