package com.example.ui.screens.monthlyupdate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.AssetEntity
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.DateFieldWithPicker
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.IconMapper
import com.example.ui.components.MonthPickerBottomSheet
import com.example.ui.components.SnapshotImportDialog
import com.example.ui.components.MathAmountTextField
import com.example.util.MathExpressionEvaluator
import com.example.ui.theme.*
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyUpdateScreen(
    viewModel: MonthlyUpdateViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMonthPicker by remember { mutableStateOf(false) }
    var showAddAssetDialog by remember { mutableStateOf(false) }
    var assetToEdit by remember { mutableStateOf<AssetEntity?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferFromAssetId by remember { mutableStateOf<Long?>(null) }
    var assetToDelete by remember { mutableStateOf<AssetEntity?>(null) }
    var expandedCategoryIds by remember { mutableStateOf(setOf<String>()) }
    var expandedCapitalFlowAssetIds by remember { mutableStateOf(setOf<Long>()) }

    LaunchedEffect(state.saveSuccessMessage) {
        state.saveSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSuccessMessage()
        }
    }

    val currentTotal = remember(state.assetEntries) {
        state.assetEntries.sumOf { it.enteredValue.toDoubleOrNull() ?: 0.0 }
    }
    val previousTotal = remember(state.assetEntries) {
        state.assetEntries.sumOf { it.previousValue }
    }
    val updatedCount = remember(state.assetEntries) {
        state.assetEntries.count { it.isUpdated }
    }
    val totalAssets = state.assetEntries.size

    val groupedEntries = remember(state.assetEntries) {
        state.assetEntries.groupBy { it.category }
    }

    val allActiveAssets = remember(state.assetEntries) {
        state.assetEntries.map { it.asset }
    }
    val currentValuesMap = remember(state.assetEntries) {
        state.assetEntries.associate { it.asset.id to (it.enteredValue.toDoubleOrNull() ?: it.previousValue) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HighDensityBg)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Surface(
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 8.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(38.dp).testTag("btn_back_to_dashboard")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HighDensityTextPrimary
                            )
                        }

                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = "Monthly Entry",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary,
                                maxLines = 1
                            )
                            val snapLabel = state.selectedSnapshotId.let { id ->
                                val parts = id.split("-")
                                val m = parts.getOrNull(1)?.toIntOrNull() ?: 1
                                val y = parts.getOrNull(0) ?: ""
                                "${DateFormatSymbols().months.getOrNull(m - 1) ?: m} $y"
                            }
                            Text(
                                text = "Updating $snapLabel",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityPrimary,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlobalPrivacyEyeButton(
                            size = 36.dp,
                            iconSize = 18.dp,
                            testTag = "btn_monthly_privacy_eye"
                        )

                        Button(
                            onClick = { 
                                transferFromAssetId = null
                                showTransferDialog = true 
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighDensityPrimaryContainer,
                                contentColor = HighDensityOnPrimaryContainer
                            ),
                            border = BorderStroke(1.dp, HighDensityPillBorder),
                            modifier = Modifier.height(36.dp).testTag("btn_transfer_top")
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Transfer", tint = HighDensityOnPrimaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }

                        Surface(
                            shape = CircleShape,
                            color = HighDensityPrimary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .clickable { showAddAssetDialog = true }
                                .testTag("btn_add_asset_top")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add New Asset",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Lazy Column with Asset Entries
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 16.dp)
            ) {
            // Month Picker Chip, Copy Previous, and Add Asset Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Month Picker Chip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMonthPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f, fill = false),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = state.selectedSnapshotId.ifEmpty { "Select Month" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary,
                                    maxLines = 1
                                )
                            }
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = HighDensityTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Copy Previous Month Values Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HighDensityPrimaryContainer,
                        border = BorderStroke(1.dp, HighDensityPillBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.copyPreviousMonthValues() }
                            .testTag("btn_copy_previous")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = HighDensityOnPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Copy Prev",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityOnPrimaryContainer,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Status Progress Card
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
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
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Assets Update Progress",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "$updatedCount of $totalAssets assets recorded",
                                style = MaterialTheme.typography.bodySmall,
                                color = HighDensityTextSecondary
                            )
                        }

                        Surface(
                            shape = CircleShape,
                            color = if (updatedCount == totalAssets && totalAssets > 0) Color(0xFF22C55E).copy(alpha = 0.15f) else HighDensityPrimaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (totalAssets > 0) "${(updatedCount * 100) / totalAssets}%" else "0%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (updatedCount == totalAssets && totalAssets > 0) Color(0xFF16A34A) else HighDensityOnPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // Category Show / Hide Quick Bar
            if (groupedEntries.isNotEmpty()) {
                item {
                    val allExpanded = groupedEntries.keys.all { expandedCategoryIds.contains(it.id) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CATEGORIES (${groupedEntries.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        TextButton(
                            onClick = {
                                expandedCategoryIds = if (allExpanded) {
                                    emptySet()
                                } else {
                                    groupedEntries.keys.map { it.id }.toSet()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = if (allExpanded) Icons.Default.UnfoldLess else Icons.Default.UnfoldMore,
                                contentDescription = null,
                                tint = HighDensityPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (allExpanded) "Collapse All" else "Expand All",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                        }
                    }
                }
            }

            if (state.assetEntries.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, HighDensityCardBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No assets added yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Add your first asset (e.g. Parag Parikh Flexi Cap, HDFC Bank, EPF, NPS) to enter monthly values.",
                                style = MaterialTheme.typography.bodySmall,
                                color = HighDensityTextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Button(
                                onClick = { showAddAssetDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add New Asset")
                            }
                        }
                    }
                }
            } else {
                // Grouped by Category Cards
                groupedEntries.forEach { (category, entries) ->
                    val isExpanded = expandedCategoryIds.contains(category.id)
                    val catSubtotal = entries.sumOf { it.enteredValue.toDoubleOrNull() ?: 0.0 }
                    val catColor = IconMapper.parseColor(category.colorHex)

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Category Header (Clickable for Show / Hide)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedCategoryIds = if (expandedCategoryIds.contains(category.id)) {
                                                expandedCategoryIds - category.id
                                            } else {
                                                expandedCategoryIds + category.id
                                            }
                                        },
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = IconMapper.getIcon(category.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = category.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = CurrencyFormatter.formatInr(catSubtotal),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityPrimary
                                        )
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = HighDensityTextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                if (isExpanded) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9))

                                    // Assets in this category
                                    entries.forEach { entry ->
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable { assetToEdit = entry.asset }
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = entry.asset.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = HighDensityTextPrimary
                                                        )
                                                        if (entry.asset.status != "ACTIVE") {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFE2E8F0)
                                                            ) {
                                                                Text(
                                                                    text = entry.asset.status,
                                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                                    color = HighDensityTextSecondary,
                                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                    if (entry.previousValue > 0) {
                                                        Text(
                                                            text = "Prev: ${CurrencyFormatter.formatInr(entry.previousValue)}",
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                                            color = HighDensityTextSecondary
                                                        )
                                                    }
                                                }

                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    // Quick Transfer / Reinvest button for this asset
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = HighDensityPrimary.copy(alpha = 0.10f),
                                                        modifier = Modifier
                                                            .clickable {
                                                                transferFromAssetId = entry.asset.id
                                                                showTransferDialog = true
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = HighDensityPrimary, modifier = Modifier.size(13.dp))
                                                            Text(
                                                                text = "Transfer",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = FontWeight.Bold,
                                                                color = HighDensityPrimary
                                                            )
                                                        }
                                                    }

                                                    // Status Badge
                                                    if (entry.isUpdated) {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFF22C55E).copy(alpha = 0.15f)
                                                        ) {
                                                            Text(
                                                                text = "Entered",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF16A34A),
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }

                                                        // Clear Entry Button
                                                        IconButton(
                                                            onClick = { viewModel.clearMonthlyEntry(entry.asset.id) },
                                                            modifier = Modifier.size(24.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = "Clear Entry",
                                                                tint = Color(0xFF94A3B8),
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFF1F5F9)
                                                        ) {
                                                            Text(
                                                                text = "Pending",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                color = HighDensityTextSecondary,
                                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                            )
                                                        }
                                                    }

                                                    // Edit Asset Button
                                                    IconButton(
                                                        onClick = { assetToEdit = entry.asset },
                                                        modifier = Modifier.size(24.dp).testTag("btn_edit_asset_${entry.asset.id}")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Edit,
                                                            contentDescription = "Edit Asset",
                                                            tint = HighDensityPrimary,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }

                                                    // Delete Asset Button
                                                    IconButton(
                                                        onClick = { assetToDelete = entry.asset },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.DeleteOutline,
                                                            contentDescription = "Delete Asset",
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            // Inputs Row: Current Value, Invested Amount & Individual Save Button
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                if (entry.isCashAsset) {
                                                    // Bank / Cash account: Only Current Balance is needed
                                                    MonthlySnapshotAmountField(
                                                        value = entry.enteredValue,
                                                        onValueChange = { 
                                                            viewModel.onValueChange(entry.asset.id, it)
                                                            viewModel.onInvestedChange(entry.asset.id, it)
                                                        },
                                                        label = "Current Balance",
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("input_asset_${entry.asset.id}")
                                                    )
                                                } else {
                                                    // Current Value Text Field (Supports arithmetic e.g. 5000 + 2000 - 300)
                                                    MonthlySnapshotAmountField(
                                                        value = entry.enteredValue,
                                                        onValueChange = { viewModel.onValueChange(entry.asset.id, it) },
                                                        label = "Current Value",
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("input_asset_${entry.asset.id}")
                                                    )

                                                    // Invested Amount Text Field
                                                    MonthlySnapshotAmountField(
                                                        value = entry.enteredInvested,
                                                        onValueChange = { viewModel.onInvestedChange(entry.asset.id, it) },
                                                        label = "Invested Amt",
                                                        placeholder = entry.enteredValue.ifEmpty { "0" },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .testTag("input_invested_${entry.asset.id}")
                                                    )
                                                }

                                                // Individual Save Button
                                                FilledTonalIconButton(
                                                    onClick = { viewModel.saveSingleAssetEntry(entry.asset.id) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                        containerColor = HighDensityPrimary,
                                                        contentColor = Color.White
                                                    ),
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .testTag("btn_save_entry_${entry.asset.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Save,
                                                        contentDescription = "Save ${entry.asset.name}",
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }

                                            // Real-time Math calculation previews if arithmetic is detected
                                            val evalCurrent = remember(entry.enteredValue) {
                                                if (MathExpressionEvaluator.hasExpression(entry.enteredValue)) {
                                                    MathExpressionEvaluator.evaluate(entry.enteredValue)
                                                } else null
                                            }
                                            if (evalCurrent != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = Color(0xFFECFDF5),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .clickable {
                                                            val formatted = MathExpressionEvaluator.formatEvaluated(evalCurrent)
                                                            viewModel.onValueChange(entry.asset.id, formatted)
                                                            if (entry.isCashAsset) {
                                                                viewModel.onInvestedChange(entry.asset.id, formatted)
                                                            }
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Current Value: = ₹${CurrencyFormatter.formatInr(evalCurrent, ignorePrivacy = true).removePrefix("₹")}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF059669)
                                                        )
                                                        Text(
                                                            text = "Tap to apply",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFF059669)
                                                        )
                                                    }
                                                }
                                            }

                                            if (!entry.isCashAsset) {
                                                val evalInv = remember(entry.enteredInvested) {
                                                    if (MathExpressionEvaluator.hasExpression(entry.enteredInvested)) {
                                                        MathExpressionEvaluator.evaluate(entry.enteredInvested)
                                                    } else null
                                                }
                                                if (evalInv != null) {
                                                    Surface(
                                                        shape = RoundedCornerShape(6.dp),
                                                        color = Color(0xFFEFF6FF),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .clickable {
                                                                val formatted = MathExpressionEvaluator.formatEvaluated(evalInv)
                                                                viewModel.onInvestedChange(entry.asset.id, formatted)
                                                            }
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Invested: = ₹${CurrencyFormatter.formatInr(evalInv, ignorePrivacy = true).removePrefix("₹")}",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = HighDensityPrimary
                                                            )
                                                            Text(
                                                                text = "Tap to apply",
                                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = HighDensityPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Capital Flows (Contributions & Withdrawals) toggle
                                            val isFlowExpanded = expandedCapitalFlowAssetIds.contains(entry.asset.id)
                                            val cVal = entry.contributions.toDoubleOrNull() ?: 0.0
                                            val wVal = entry.withdrawals.toDoubleOrNull() ?: 0.0
                                            val hasFlows = cVal > 0 || wVal > 0

                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable {
                                                        expandedCapitalFlowAssetIds = if (isFlowExpanded) {
                                                            expandedCapitalFlowAssetIds - entry.asset.id
                                                        } else {
                                                            expandedCapitalFlowAssetIds + entry.asset.id
                                                        }
                                                    },
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        if (isFlowExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                        contentDescription = null,
                                                        tint = if (hasFlows) HighDensityPrimary else HighDensityTextSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = if (hasFlows) "Capital Flows: +₹${Math.round(cVal)} / -₹${Math.round(wVal)}" else "Adjust Inflows / Redemptions",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                        fontWeight = if (hasFlows) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (hasFlows) HighDensityPrimary else HighDensityTextSecondary
                                                    )
                                                }
                                            }

                                            if (isFlowExpanded) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    MonthlySnapshotAmountField(
                                                        value = entry.contributions,
                                                        onValueChange = { viewModel.onContributionsChange(entry.asset.id, it) },
                                                        label = "Deposit/Inflow (+)",
                                                        modifier = Modifier.weight(1f),
                                                        prefixText = "+₹ ",
                                                        prefixColor = Color(0xFF16A34A)
                                                    )

                                                    MonthlySnapshotAmountField(
                                                        value = entry.withdrawals,
                                                        onValueChange = { viewModel.onWithdrawalsChange(entry.asset.id, it) },
                                                        label = "Redeemed/Outflow (-)",
                                                        modifier = Modifier.weight(1f),
                                                        prefixText = "-₹ ",
                                                        prefixColor = Color(0xFFDC2626)
                                                    )
                                                }
                                            }

                                            // Real-time Gain/Loss Indicator
                                            if (entry.enteredValue.isNotEmpty() && entry.enteredInvested.isNotEmpty()) {
                                                val gainLoss = entry.gainLossAmount
                                                val returnPct = entry.returnPercentage
                                                val isGain = gainLoss >= 0
                                                val badgeColor = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                val badgeBg = if (isGain) Color(0xFF22C55E).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)

                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = badgeBg,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isGain) "📈 Gain: " else "📉 Loss: ",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeColor
                                                        )
                                                        Text(
                                                            text = "${CurrencyFormatter.formatInr(gainLoss, showSign = true)} (${CurrencyFormatter.formatGrowthPercentage(returnPct)})",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                            fontWeight = FontWeight.Bold,
                                                            color = badgeColor
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
        }

            // Bottom Action Bar
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Entered Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = HighDensityTextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(currentTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        }

                        if (previousTotal > 0) {
                            val diff = currentTotal - previousTotal
                            val isPos = diff >= 0
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isPos) Color(0xFF22C55E).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${if (isPos) "+" else ""}${CurrencyFormatter.formatInr(diff)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPos) Color(0xFF16A34A) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { viewModel.saveSnapshotValues() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_save_snapshot"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        enabled = !state.isSaving
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Saving Snapshot...")
                        } else {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Save Monthly Snapshot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }

    // Month Picker Modal Sheet
    if (showMonthPicker) {
        MonthPickerBottomSheet(
            snapshots = state.snapshots,
            selectedSnapshotId = state.selectedSnapshotId,
            onSnapshotSelected = { viewModel.selectSnapshot(it) },
            onCreateSnapshot = { y, m -> viewModel.createNewSnapshot(y, m) },
            onDeleteSnapshot = { viewModel.deleteSnapshot(it) },
            onDismiss = { showMonthPicker = false }
        )
    }

    // Delete Asset Confirmation Dialog
    if (assetToDelete != null) {
        val asset = assetToDelete!!
        AlertDialog(
            onDismissRequest = { assetToDelete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Delete Asset Holding?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${asset.name}'? This will permanently remove this asset and all its historical valuations from your portfolio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HighDensityTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAsset(asset)
                        assetToDelete = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete Asset", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { assetToDelete = null }) {
                    Text("Cancel", color = HighDensityTextSecondary)
                }
            }
        )
    }

    // Add / Edit Asset Dialog
    if (showAddAssetDialog || assetToEdit != null) {
        val editingAsset = assetToEdit
        val existingEntry = editingAsset?.let { a -> state.assetEntries.firstOrNull { it.asset.id == a.id } }

        val defaultStartDate = if (state.selectedSnapshotId.isNotBlank()) "${state.selectedSnapshotId}-01"
            else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        var assetName by remember(editingAsset) { mutableStateOf(editingAsset?.name ?: "") }
        var selectedCatId by remember(editingAsset) { mutableStateOf(editingAsset?.categoryId ?: state.categories.firstOrNull()?.id ?: "mutual_funds") }
        var assetStatus by remember(editingAsset) { mutableStateOf(editingAsset?.status ?: "ACTIVE") }
        var assetInvestmentDate by remember(editingAsset) {
            mutableStateOf(editingAsset?.investmentDate?.ifEmpty { defaultStartDate } ?: defaultStartDate)
        }
        var initialAmount by remember(editingAsset) {
            mutableStateOf(existingEntry?.enteredValue?.filter { it.isDigit() } ?: "")
        }
        var initialInvested by remember(editingAsset) {
            mutableStateOf(
                if (editingAsset != null && editingAsset.initialInvestedAmount > 0) editingAsset.initialInvestedAmount.toLong().toString()
                else existingEntry?.enteredInvested?.filter { it.isDigit() } ?: ""
            )
        }
        var expandAdvancedYield by remember(editingAsset) {
            mutableStateOf(
                (editingAsset != null && (editingAsset.interestRate > 0 || editingAsset.installmentAmount > 0 || editingAsset.payoutFrequency != "NONE" || editingAsset.maturityDate.isNotEmpty()))
            )
        }
        var assetInterestRate by remember(editingAsset) {
            mutableStateOf(if (editingAsset != null && editingAsset.interestRate > 0) editingAsset.interestRate.toString() else "")
        }
        var assetPayoutFrequency by remember(editingAsset) {
            mutableStateOf(editingAsset?.payoutFrequency ?: "NONE")
        }
        var assetPrincipalPayoutPercent by remember(editingAsset) {
            mutableStateOf(if (editingAsset != null && editingAsset.principalPayoutPercent > 0) editingAsset.principalPayoutPercent.toString() else "")
        }
        var isRecurringDeposit by remember(editingAsset) {
            mutableStateOf(editingAsset?.installmentAmount != null && editingAsset.installmentAmount > 0)
        }
        var assetInstallmentAmount by remember(editingAsset) {
            mutableStateOf(if (editingAsset != null && editingAsset.installmentAmount > 0) editingAsset.installmentAmount.toLong().toString() else "")
        }
        var assetInstallmentFrequency by remember(editingAsset) {
            mutableStateOf(editingAsset?.installmentFrequency?.ifEmpty { "MONTHLY" } ?: "MONTHLY")
        }
        var assetMaturityDate by remember(editingAsset) {
            mutableStateOf(editingAsset?.maturityDate ?: "")
        }
        var assetNotes by remember(editingAsset) { mutableStateOf(editingAsset?.notes ?: "") }

        AlertDialog(
            onDismissRequest = {
                showAddAssetDialog = false
                assetToEdit = null
            },
            containerColor = Color.White,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingAsset != null) "Edit Asset / Holding" else "Add New Asset / Holding",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = HighDensityTextPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = HighDensityPrimary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (editingAsset != null) "ID #${editingAsset.id}" else "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = assetName,
                        onValueChange = { assetName = it },
                        label = { Text("Asset / Holding Name *") },
                        placeholder = { Text("e.g. Parag Parikh Flexi Cap, HDFC FD") },
                        modifier = Modifier.fillMaxWidth().testTag("input_modal_asset_name"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Category Selection
                    var catDropdownExpanded by remember { mutableStateOf(false) }
                    val selectedCategoryName = state.categories.firstOrNull { it.id == selectedCatId }?.name ?: selectedCatId

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCategoryName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Asset Category") },
                            trailingIcon = {
                                Icon(
                                    imageVector = if (catDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = HighDensityTextPrimary
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("select_modal_category"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { catDropdownExpanded = true }
                        )

                        DropdownMenu(
                            expanded = catDropdownExpanded,
                            onDismissRequest = { catDropdownExpanded = false }
                        ) {
                            state.categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCatId = cat.id
                                        catDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Status selection
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Status", style = MaterialTheme.typography.labelSmall, color = HighDensityTextSecondary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ACTIVE", "INACTIVE", "CLOSED").forEach { statusOption ->
                                val isSelected = assetStatus == statusOption
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) HighDensityPrimaryContainer else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) HighDensityPrimary else Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { assetStatus = statusOption }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = statusOption,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) HighDensityOnPrimaryContainer else HighDensityTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Valuation & Invested Amount
                    val selectedCategory = state.categories.firstOrNull { it.id == selectedCatId }
                    val isBankOrCash = selectedCategory?.groupType == "CASH" || selectedCatId == "bank_balance" || selectedCatId == "cash"

                    if (isBankOrCash) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MathAmountTextField(
                                value = initialAmount,
                                onValueChange = {
                                    initialAmount = it
                                    initialInvested = it
                                },
                                label = "Current Bank Balance *",
                                placeholder = "e.g. 75000 + 25000",
                                testTag = "input_modal_current_value",
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = HighDensityTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "For bank accounts & cash, current balance is sufficient (no separate invested amount needed).",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            MathAmountTextField(
                                value = initialAmount,
                                onValueChange = { initialAmount = it },
                                label = "Current Value *",
                                placeholder = "e.g. 50000 + 15000",
                                testTag = "input_modal_current_value",
                                modifier = Modifier.fillMaxWidth()
                            )

                            MathAmountTextField(
                                value = initialInvested,
                                onValueChange = { initialInvested = it },
                                label = "Invested Amount",
                                placeholder = "e.g. 40000 + 10000",
                                testTag = "input_modal_invested_amount",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Purchase / Investment Date (Supports typing and DatePicker popup)
                    DateFieldWithPicker(
                        value = assetInvestmentDate,
                        onValueChange = { assetInvestmentDate = it },
                        label = "Investment / Start Date",
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Advanced Yield & Fixed Income Section (Expandable)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expandAdvancedYield = !expandAdvancedYield },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Percent,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Interest, RD & Payout Options",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HighDensityTextPrimary
                                    )
                                }
                                Icon(
                                    imageVector = if (expandAdvancedYield) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Toggle",
                                    tint = HighDensityTextSecondary
                                )
                            }

                            if (expandAdvancedYield) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Interest Rate
                                    OutlinedTextField(
                                        value = assetInterestRate,
                                        onValueChange = { assetInterestRate = it },
                                        label = { Text("Interest Rate (% p.a.)") },
                                        suffix = { Text("%") },
                                        placeholder = { Text("e.g. 7.5") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(8.dp)
                                    )

                                    // Payout Frequency Dropdown
                                    var freqExpanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = when (assetPayoutFrequency) {
                                                "NONE" -> "Cumulative (At Maturity)"
                                                "MONTHLY" -> "Monthly Payout"
                                                "QUARTERLY" -> "Quarterly Payout"
                                                "ANNUALLY" -> "Annual Payout"
                                                else -> assetPayoutFrequency
                                            },
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Interest Payout") },
                                            trailingIcon = {
                                                Icon(
                                                    imageVector = if (freqExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                    contentDescription = null,
                                                    tint = HighDensityTextPrimary
                                                )
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .matchParentSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { freqExpanded = true }
                                        )

                                        DropdownMenu(
                                            expanded = freqExpanded,
                                            onDismissRequest = { freqExpanded = false }
                                        ) {
                                            mapOf(
                                                "NONE" to "Cumulative (At Maturity)",
                                                "MONTHLY" to "Monthly Payout",
                                                "QUARTERLY" to "Quarterly Payout",
                                                "ANNUALLY" to "Annual Payout"
                                            ).forEach { (freqKey, label) ->
                                                DropdownMenuItem(
                                                    text = { Text(label) },
                                                    onClick = {
                                                        assetPayoutFrequency = freqKey
                                                        freqExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Periodic Principal Return
                                    if (assetPayoutFrequency != "NONE") {
                                        OutlinedTextField(
                                            value = assetPrincipalPayoutPercent,
                                            onValueChange = { assetPrincipalPayoutPercent = it },
                                            label = { Text("Periodic Principal Return (%)") },
                                            suffix = { Text("%") },
                                            placeholder = { Text("0 for interest-only") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }

                                    // Recurring Deposit (RD) Checkbox
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { isRecurringDeposit = !isRecurringDeposit }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Checkbox(
                                            checked = isRecurringDeposit,
                                            onCheckedChange = { isRecurringDeposit = it }
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Recurring Deposit (RD) / Periodic Installment",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (isRecurringDeposit) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            MathAmountTextField(
                                                value = assetInstallmentAmount,
                                                onValueChange = { assetInstallmentAmount = it },
                                                label = "Installment (₹)",
                                                placeholder = "5000",
                                                modifier = Modifier.weight(1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )

                                            var instFreqExpanded by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.weight(1f)) {
                                                OutlinedTextField(
                                                    value = assetInstallmentFrequency,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Frequency") },
                                                    trailingIcon = {
                                                        Icon(
                                                            imageVector = if (instFreqExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                            contentDescription = null,
                                                            tint = HighDensityTextPrimary
                                                        )
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(8.dp)
                                                )

                                                Box(
                                                    modifier = Modifier
                                                        .matchParentSize()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .clickable { instFreqExpanded = true }
                                                )

                                                DropdownMenu(
                                                    expanded = instFreqExpanded,
                                                    onDismissRequest = { instFreqExpanded = false }
                                                ) {
                                                    listOf("MONTHLY", "QUARTERLY").forEach { freq ->
                                                        DropdownMenuItem(
                                                            text = { Text(freq) },
                                                            onClick = {
                                                                assetInstallmentFrequency = freq
                                                                instFreqExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Maturity Date (Supports typing and DatePicker popup)
                                    DateFieldWithPicker(
                                        value = assetMaturityDate,
                                        onValueChange = { assetMaturityDate = it },
                                        label = "Maturity Date",
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Notes
                    OutlinedTextField(
                        value = assetNotes,
                        onValueChange = { assetNotes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("Account / Folio number, advisor info...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (editingAsset != null) {
                        OutlinedButton(
                            onClick = {
                                val toDel = editingAsset
                                showAddAssetDialog = false
                                assetToEdit = null
                                assetToDelete = toDel
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFEF4444)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            showAddAssetDialog = false
                            assetToEdit = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextPrimary)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (assetName.isNotBlank()) {
                                val enteredInvDbl = MathExpressionEvaluator.evaluate(initialInvested)
                                val enteredValDbl = MathExpressionEvaluator.evaluate(initialAmount)
                                val valDbl = enteredValDbl ?: enteredInvDbl
                                val selectedCat = state.categories.firstOrNull { it.id == selectedCatId }
                                val isBankOrCash = selectedCat?.groupType == "CASH" || selectedCatId == "bank_balance" || selectedCatId == "cash"
                                val investedDbl = if (isBankOrCash) (valDbl ?: 0.0) else (enteredInvDbl ?: valDbl ?: 0.0)
                                val finalInvestDate = assetInvestmentDate.trim().ifEmpty { defaultStartDate }
                                val rateDbl = assetInterestRate.toDoubleOrNull() ?: 0.0
                                val prinDbl = assetPrincipalPayoutPercent.toDoubleOrNull() ?: 0.0
                                val instDbl = if (isRecurringDeposit) (MathExpressionEvaluator.evaluate(assetInstallmentAmount) ?: 0.0) else 0.0

                                val assetToSave = if (editingAsset != null) {
                                    editingAsset.copy(
                                        name = assetName.trim(),
                                        categoryId = selectedCatId,
                                        notes = assetNotes.trim(),
                                        status = assetStatus,
                                        investmentDate = finalInvestDate,
                                        initialInvestedAmount = investedDbl,
                                        interestRate = rateDbl,
                                        payoutFrequency = assetPayoutFrequency,
                                        payoutType = if (assetPayoutFrequency == "NONE") "CUMULATIVE" else if (prinDbl > 0) "INTEREST_AND_PRINCIPAL" else "INTEREST_ONLY",
                                        principalPayoutPercent = prinDbl,
                                        maturityDate = assetMaturityDate.trim(),
                                        installmentAmount = instDbl,
                                        installmentFrequency = assetInstallmentFrequency
                                    )
                                } else {
                                    AssetEntity(
                                        name = assetName.trim(),
                                        categoryId = selectedCatId,
                                        notes = assetNotes.trim(),
                                        status = assetStatus,
                                        investmentDate = finalInvestDate,
                                        initialInvestedAmount = investedDbl,
                                        interestRate = rateDbl,
                                        payoutFrequency = assetPayoutFrequency,
                                        payoutType = if (assetPayoutFrequency == "NONE") "CUMULATIVE" else if (prinDbl > 0) "INTEREST_AND_PRINCIPAL" else "INTEREST_ONLY",
                                        principalPayoutPercent = prinDbl,
                                        maturityDate = assetMaturityDate.trim(),
                                        installmentAmount = instDbl,
                                        installmentFrequency = assetInstallmentFrequency
                                    )
                                }

                                viewModel.saveOrUpdateAsset(assetToSave, valDbl)
                                showAddAssetDialog = false
                                assetToEdit = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.4f).testTag("btn_save_asset_modal"),
                        enabled = assetName.isNotBlank()
                    ) {
                        Text(
                            text = if (editingAsset != null) "Save Asset" else "Add Asset",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = null
        )
    }

    if (showImportDialog) {
        SnapshotImportDialog(
            onDismiss = { showImportDialog = false },
            onImportConfirmed = { json ->
                showImportDialog = false
                viewModel.importJsonSnapshot(json)
            }
        )
    }

    if (showTransferDialog) {
        com.example.ui.components.TransferAssetDialog(
            snapshotId = state.selectedSnapshotId,
            assets = allActiveAssets,
            categories = state.categories,
            currentValues = currentValuesMap,
            preselectedFromAssetId = transferFromAssetId,
            onDismiss = { showTransferDialog = false },
            onConfirmTransfer = { fromId, toId, amount, closeSource, notes ->
                showTransferDialog = false
                viewModel.transferAsset(fromId, toId, amount, closeSource, notes)
            }
        )
    }
}

@Composable
private fun MonthlySnapshotAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "0",
    prefixText: String = "₹ ",
    prefixColor: Color = HighDensityTextPrimary,
    testTag: String? = null
) {
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(value) {
        if (value != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = value,
                selection = TextRange(value.length)
            )
        }
    }

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { tfv ->
            val sanitized = MathExpressionEvaluator.sanitizeMathInput(tfv.text)
            val diff = tfv.text.length - sanitized.length
            val newSelection = if (diff != 0) {
                val newCursor = (tfv.selection.end - diff).coerceIn(0, sanitized.length)
                TextRange(newCursor)
            } else {
                tfv.selection
            }
            textFieldValue = tfv.copy(text = sanitized, selection = newSelection)
            onValueChange(sanitized)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier
            .focusRequester(focusRequester)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        prefix = { Text(prefixText, fontWeight = FontWeight.Bold, color = prefixColor) },
        trailingIcon = {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFEFF6FF),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val currentText = textFieldValue.text
                        val trimmed = currentText.trimEnd()
                        if (trimmed.isNotEmpty() && !trimmed.endsWith("+") && !trimmed.endsWith("-")) {
                            val newText = "$trimmed + "
                            textFieldValue = TextFieldValue(
                                text = newText,
                                selection = TextRange(newText.length)
                            )
                            onValueChange(newText)
                            focusRequester.requestFocus()
                        }
                    }
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HighDensityPrimary)
                }
            }
        },
        placeholder = { Text(placeholder, color = HighDensityTextSecondary.copy(alpha = 0.5f)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color(0xFFF8FAFC),
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFE2E8F0),
            focusedBorderColor = HighDensityPrimary
        )
    )
}

