package com.example.ui.components

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.ui.theme.*

enum class TransferMode(val label: String, val description: String) {
    REINVEST_TRANSFER("Asset Reallocation", "Move funds between assets (e.g. FD to Mutual Fund, Stocks to Bank, Bank to Gold)"),
    EXTERNAL_REDEMPTION("External Withdrawal", "Withdraw cash outside the portfolio (e.g. personal spending/vacation)"),
    FRESH_CAPITAL("Fresh Capital Deposit", "Add fresh external funds (e.g. bonus, gift, savings) into an asset"),
    LIQUIDATE_CLOSE("Full Maturity / Liquidation", "Fully redeem and close an asset (e.g. matured FD, liquidated shares)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferAssetDialog(
    snapshotId: String,
    assets: List<AssetEntity>,
    categories: List<CategoryEntity>,
    currentValues: Map<Long, Double> = emptyMap(),
    preselectedFromAssetId: Long? = null,
    preselectedToAssetId: Long? = null,
    onDismiss: () -> Unit,
    onConfirmTransfer: (
        fromAssetId: Long?,
        toAssetId: Long?,
        amount: Double,
        closeSource: Boolean,
        notes: String
    ) -> Unit
) {
    val catMap = remember(categories) { categories.associateBy { it.id } }
    val activeAssets = remember(assets) { assets.filter { it.status == "ACTIVE" } }

    var selectedMode by remember {
        mutableStateOf(
            if (preselectedFromAssetId != null && preselectedToAssetId == null) TransferMode.REINVEST_TRANSFER
            else TransferMode.REINVEST_TRANSFER
        )
    }

    var fromAssetId by remember { mutableStateOf(preselectedFromAssetId ?: activeAssets.firstOrNull()?.id) }
    var toAssetId by remember { 
        mutableStateOf(
            preselectedToAssetId ?: activeAssets.firstOrNull { it.id != (preselectedFromAssetId ?: activeAssets.firstOrNull()?.id) }?.id
        ) 
    }

    var amountText by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var closeSourceAsset by remember { mutableStateOf(selectedMode == TransferMode.LIQUIDATE_CLOSE) }

    var isSelectingSource by remember { mutableStateOf(false) }
    var isSelectingDestination by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMode) {
        if (selectedMode == TransferMode.LIQUIDATE_CLOSE) {
            closeSourceAsset = true
            val fromVal = fromAssetId?.let { currentValues[it] } ?: 0.0
            if (fromVal > 0) {
                amountText = Math.round(fromVal).toString()
            }
        } else if (selectedMode == TransferMode.EXTERNAL_REDEMPTION) {
            toAssetId = null
        } else if (selectedMode == TransferMode.FRESH_CAPITAL) {
            fromAssetId = null
        }
    }

    val transferAmount = amountText.toDoubleOrNull() ?: 0.0
    val fromAsset = assets.firstOrNull { it.id == fromAssetId }
    val toAsset = assets.firstOrNull { it.id == toAssetId }

    val fromAssetBalance = fromAssetId?.let { currentValues[it] ?: 0.0 } ?: 0.0
    val toAssetBalance = toAssetId?.let { currentValues[it] ?: 0.0 } ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("dialog_transfer_asset"),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = HighDensityPrimary.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Transfer & Reinvestment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                    Text(
                        text = "Protect Organic Returns & Benchmark CAGR",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = HighDensityTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Mode Selector Chips
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "OPERATION TYPE",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedMode == TransferMode.REINVEST_TRANSFER) HighDensityPrimary else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = TransferMode.REINVEST_TRANSFER }
                        ) {
                            Text(
                                text = "Transfer / Reinvest",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == TransferMode.REINVEST_TRANSFER) Color.White else HighDensityTextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedMode == TransferMode.LIQUIDATE_CLOSE) Color(0xFFE11D48) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = TransferMode.LIQUIDATE_CLOSE }
                        ) {
                            Text(
                                text = "Matured / Close",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == TransferMode.LIQUIDATE_CLOSE) Color.White else HighDensityTextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedMode == TransferMode.EXTERNAL_REDEMPTION) Color(0xFFD97706) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = TransferMode.EXTERNAL_REDEMPTION }
                        ) {
                            Text(
                                text = "External Cash Out",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == TransferMode.EXTERNAL_REDEMPTION) Color.White else HighDensityTextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedMode == TransferMode.FRESH_CAPITAL) Color(0xFF16A34A) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = TransferMode.FRESH_CAPITAL }
                        ) {
                            Text(
                                text = "Fresh Capital Inflow",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (selectedMode == TransferMode.FRESH_CAPITAL) Color.White else HighDensityTextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                // Source Asset (From)
                if (selectedMode != TransferMode.FRESH_CAPITAL) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "FROM ASSET (SOURCE)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSelectingSource = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val cat = fromAsset?.let { catMap[it.categoryId] }
                                    val catColor = cat?.let { IconMapper.parseColor(it.colorHex) } ?: HighDensityPrimary
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = IconMapper.getIcon(cat?.iconName ?: "category"),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = fromAsset?.name ?: "Select Source Asset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary,
                                            maxLines = 1
                                        )
                                        if (fromAsset != null) {
                                            Text(
                                                text = "${cat?.name ?: fromAsset.categoryId} • Bal: ${CurrencyFormatter.formatInr(fromAssetBalance)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = HighDensityPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = HighDensityTextSecondary)
                            }
                        }
                    }
                }

                // Destination Asset (To)
                if (selectedMode != TransferMode.EXTERNAL_REDEMPTION) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "TO ASSET (DESTINATION)",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            color = Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isSelectingDestination = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val cat = toAsset?.let { catMap[it.categoryId] }
                                    val catColor = cat?.let { IconMapper.parseColor(it.colorHex) } ?: HighDensityPrimary
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = catColor.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = IconMapper.getIcon(cat?.iconName ?: "category"),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = toAsset?.name ?: "Select Destination Asset",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary,
                                            maxLines = 1
                                        )
                                        if (toAsset != null) {
                                            Text(
                                                text = "${cat?.name ?: toAsset.categoryId} • Bal: ${CurrencyFormatter.formatInr(toAssetBalance)}",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                color = HighDensityPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = HighDensityTextSecondary)
                            }
                        }
                    }
                }

                // Amount Field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "AMOUNT TO TRANSFER / REINVEST",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextSecondary
                    )

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_transfer_amount"),
                        prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = HighDensityTextPrimary) },
                        placeholder = { Text("e.g. 50000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = Color.White,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedBorderColor = HighDensityPrimary
                        )
                    )

                    // Quick Preset Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(10000, 25000, 50000, 100000).forEach { preset ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { amountText = preset.toString() }
                            ) {
                                Text(
                                    text = "₹${preset / 1000}k",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }

                        if (fromAssetBalance > 0) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = HighDensityPrimary.copy(alpha = 0.12f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { amountText = Math.round(fromAssetBalance).toString() }
                            ) {
                                Text(
                                    text = "100%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityPrimary,
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Notes / Reason
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Note / Reason (Optional)") },
                    placeholder = { Text("e.g. Redeemed FD proceeds to Bank") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedBorderColor = HighDensityPrimary
                    )
                )

                // Impact Live Preview Card
                if (transferAmount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Accounting & Return Impact:",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                            }

                            if (fromAsset != null) {
                                val newFromBal = maxOf(0.0, fromAssetBalance - transferAmount)
                                Text(
                                    text = "• ${fromAsset.name}: Recorded Outflow (-${CurrencyFormatter.formatInr(transferAmount)}), New Bal: ${CurrencyFormatter.formatInr(newFromBal)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }

                            if (toAsset != null) {
                                val newToBal = toAssetBalance + transferAmount
                                Text(
                                    text = "• ${toAsset.name}: Recorded Inflow (+${CurrencyFormatter.formatInr(transferAmount)}), New Bal: ${CurrencyFormatter.formatInr(newToBal)}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }

                            val netPortfolio = when (selectedMode) {
                                TransferMode.REINVEST_TRANSFER, TransferMode.LIQUIDATE_CLOSE -> if (toAsset != null) "₹0 (Internal Reallocation)" else "-${CurrencyFormatter.formatInr(transferAmount)}"
                                TransferMode.EXTERNAL_REDEMPTION -> "-${CurrencyFormatter.formatInr(transferAmount)}"
                                TransferMode.FRESH_CAPITAL -> "+${CurrencyFormatter.formatInr(transferAmount)}"
                            }

                            Text(
                                text = "• Net External Portfolio Inflow: $netPortfolio",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityTextPrimary)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        onConfirmTransfer(
                            fromAssetId,
                            toAssetId,
                            transferAmount,
                            closeSourceAsset,
                            notesText
                        )
                    },
                    enabled = transferAmount > 0 && (fromAssetId != null || toAssetId != null),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HighDensityPrimary,
                        disabledContainerColor = Color(0xFFE2E8F0),
                        disabledContentColor = Color(0xFF94A3B8)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1.35f)
                        .testTag("btn_confirm_transfer")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Confirm & Record",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        },
        dismissButton = null
    )

    // Searchable & Categorized Asset Picker Dialog
    if (isSelectingSource || isSelectingDestination) {
        val isPickingSource = isSelectingSource
        val excludedId = if (isPickingSource) toAssetId else fromAssetId
        val selectableAssets = remember(activeAssets, excludedId) {
            if (isPickingSource) activeAssets else activeAssets.filter { it.id != excludedId }
        }

        GroupedAssetPickerDialog(
            title = if (isPickingSource) "Select Source Asset (From)" else "Select Destination Asset (To)",
            assets = selectableAssets,
            categories = categories,
            currentValues = currentValues,
            onDismiss = {
                isSelectingSource = false
                isSelectingDestination = false
            },
            onAssetSelected = { asset ->
                if (isPickingSource) {
                    fromAssetId = asset.id
                    if (selectedMode == TransferMode.LIQUIDATE_CLOSE) {
                        val bal = currentValues[asset.id] ?: 0.0
                        if (bal > 0) {
                            amountText = Math.round(bal).toString()
                        }
                    }
                } else {
                    toAssetId = asset.id
                }
                isSelectingSource = false
                isSelectingDestination = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupedAssetPickerDialog(
    title: String,
    assets: List<AssetEntity>,
    categories: List<CategoryEntity>,
    currentValues: Map<Long, Double>,
    onDismiss: () -> Unit,
    onAssetSelected: (AssetEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val catMap = remember(categories) { categories.associateBy { it.id } }

    val filteredAssets = remember(assets, searchQuery) {
        if (searchQuery.isBlank()) assets
        else {
            val q = searchQuery.trim().lowercase()
            assets.filter { asset ->
                val catName = catMap[asset.categoryId]?.name.orEmpty()
                asset.name.lowercase().contains(q) ||
                    catName.lowercase().contains(q) ||
                    asset.categoryId.lowercase().contains(q)
            }
        }
    }

    // Group filtered assets by category
    val groupedAssets = remember(filteredAssets, categories) {
        val map = filteredAssets.groupBy { it.categoryId }
        categories.filter { map.containsKey(it.id) }.map { cat ->
            cat to (map[cat.id] ?: emptyList())
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dialog Title & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or category (e.g. Bank, MF, FD)...", fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HighDensityTextSecondary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedBorderColor = HighDensityPrimary
                    )
                )

                // List of grouped assets
                if (groupedAssets.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching assets found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HighDensityTextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        groupedAssets.forEach { (cat, catAssets) ->
                            val catColor = IconMapper.parseColor(cat.colorHex)
                            item(key = "header_${cat.id}") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF1F5F9), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = IconMapper.getIcon(cat.iconName),
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = cat.name.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.5.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = "(${catAssets.size})",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            items(catAssets, key = { it.id }) { asset ->
                                val bal = currentValues[asset.id] ?: 0.0
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAssetSelected(asset) }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = catColor.copy(alpha = 0.15f),
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = IconMapper.getIcon(cat.iconName),
                                                        contentDescription = null,
                                                        tint = catColor,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }

                                            Column {
                                                Text(
                                                    text = asset.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = HighDensityTextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = cat.name,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                    color = HighDensityTextSecondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = CurrencyFormatter.formatInr(bal),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityPrimary
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
