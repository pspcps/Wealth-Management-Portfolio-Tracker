package com.example.ui.screens.portfolio

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.util.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentImportDialog(
    onDismiss: () -> Unit,
    onImportConfirmed: suspend (statement: ParsedInvestmentStatement, targetSnapshotId: String, updateExisting: Boolean) -> InvestmentImportResult
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedType by remember { mutableStateOf(InvestmentStatementType.AUTO_DETECT) }
    var parsedStatement by remember { mutableStateOf<ParsedInvestmentStatement?>(null) }
    var targetSnapshotId by remember { mutableStateOf("2026-09") }
    var updateExisting by remember { mutableStateOf(true) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var importResult by remember { mutableStateOf<InvestmentImportResult?>(null) }

    var showPasteSheet by remember { mutableStateOf(false) }
    var pastedText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                isProcessing = true
                errorMessage = null
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val statement = InvestmentStatementParser.parseStream(
                            context = context,
                            inputStream = inputStream,
                            requestedType = selectedType
                        )
                        if (statement.holdings.isEmpty()) {
                            errorMessage = "No investment holdings could be identified in this file. Please check format or select statement type explicitly."
                        } else {
                            parsedStatement = statement
                            targetSnapshotId = statement.targetSnapshotId
                        }
                    } else {
                        errorMessage = "Could not open selected file."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error parsing file: ${e.localizedMessage ?: "Unknown error"}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .testTag("dialog_investment_import"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = null,
                                    tint = HighDensityPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Import Investment Statement",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Groww, Zerodha, CAMS Excel (.xlsx) & CSV",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        modifier = Modifier.testTag("btn_close_import_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))

                // If import has completed, show Success Screen
                if (importResult != null) {
                    val res = importResult!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDCFCE7),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF16A34A),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Import Successful!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = res.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = HighDensityTextSecondary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("New Assets", fontSize = 11.sp, color = HighDensityTextSecondary)
                                    Text("${res.importedCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HighDensityTextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Updated", fontSize = 11.sp, color = HighDensityTextSecondary)
                                    Text("${res.updatedCount}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HighDensityTextPrimary)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Snapshot Month", fontSize = 11.sp, color = HighDensityTextSecondary)
                                    Text(res.snapshotId, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = HighDensityPrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_done_import"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View in Portfolio", fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (parsedStatement == null) {
                    // Step 1: Upload / Select File
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "1. SELECT INVESTMENT TYPE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedType == InvestmentStatementType.AUTO_DETECT,
                                onClick = { selectedType = InvestmentStatementType.AUTO_DETECT },
                                label = { Text("Auto-Detect", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedType == InvestmentStatementType.MUTUAL_FUNDS,
                                onClick = { selectedType = InvestmentStatementType.MUTUAL_FUNDS },
                                label = { Text("Mutual Funds", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedType == InvestmentStatementType.STOCKS,
                                onClick = { selectedType = InvestmentStatementType.STOCKS },
                                label = { Text("Stocks & ETFs", fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "2. CHOOSE SOURCE FILE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        // File Upload Card
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.5.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFDBEAFE),
                                    modifier = Modifier.size(54.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = null,
                                            tint = HighDensityPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "Upload Broker Excel or CSV",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = HighDensityTextPrimary
                                )

                                Text(
                                    text = "Supports Groww, Zerodha, and CAMS holdings statements (.xlsx, .xls, .csv). Automatically extracts Scheme/Stock name, Folio/ISIN, Units, Invested Value, and Current Value.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )

                                Button(
                                    onClick = {
                                        filePickerLauncher.launch(
                                            arrayOf(
                                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                                "application/vnd.ms-excel",
                                                "text/csv",
                                                "text/comma-separated-values",
                                                "text/plain",
                                                "*/*"
                                            )
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .testTag("btn_select_statement_file"),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                    enabled = !isProcessing
                                ) {
                                    if (isProcessing) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reading File...", fontSize = 12.sp)
                                    } else {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Select Statement File", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        // Quick Test Samples Section
                        Text(
                            text = "OR TEST WITH SAMPLE STATEMENTS",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val stmt = InvestmentStatementParser.getSampleMutualFundStatement()
                                    parsedStatement = stmt
                                    targetSnapshotId = stmt.targetSnapshotId
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sample_mf"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF6366F1))
                            ) {
                                Icon(Icons.Default.PieChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF6366F1))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load MF Sample (Groww)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF6366F1))
                            }

                            OutlinedButton(
                                onClick = {
                                    val stmt = InvestmentStatementParser.getSampleStockStatement()
                                    parsedStatement = stmt
                                    targetSnapshotId = stmt.targetSnapshotId
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_sample_stocks"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF0F766E))
                            ) {
                                Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F766E))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Load Stocks Sample (Groww)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F766E))
                            }
                        }

                        // Paste Text option
                        TextButton(
                            onClick = { showPasteSheet = !showPasteSheet },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(14.dp), tint = HighDensityTextSecondary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (showPasteSheet) "Hide Paste Option" else "Or Paste CSV/TSV Text Directly", fontSize = 12.sp, color = HighDensityTextSecondary)
                        }

                        if (showPasteSheet) {
                            OutlinedTextField(
                                value = pastedText,
                                onValueChange = { pastedText = it },
                                label = { Text("Paste CSV/TSV rows here") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Button(
                                onClick = {
                                    if (pastedText.isNotBlank()) {
                                        val stmt = InvestmentStatementParser.parseText(pastedText, selectedType)
                                        if (stmt.holdings.isNotEmpty()) {
                                            parsedStatement = stmt
                                            targetSnapshotId = stmt.targetSnapshotId
                                        } else {
                                            errorMessage = "Could not identify any holdings in the pasted text."
                                        }
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Parse Text")
                            }
                        }

                        if (errorMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEE2E2),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                                    Text(errorMessage ?: "", color = Color(0xFFDC2626), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Step 2: Preview Parsed Statement
                    val statement = parsedStatement!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        // Summary Banner
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (statement.investmentType == InvestmentStatementType.MUTUAL_FUNDS) Color(0xFFEEF2FF) else Color(0xFFF0FDF4)
                                        ) {
                                            Text(
                                                text = statement.investmentType.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (statement.investmentType == InvestmentStatementType.MUTUAL_FUNDS) Color(0xFF4F46E5) else Color(0xFF15803D),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }

                                        if (statement.investorName.isNotBlank()) {
                                            Text(
                                                text = statement.investorName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = HighDensityTextPrimary
                                            )
                                        }
                                    }

                                    if (statement.clientOrPan.isNotBlank()) {
                                        Text(
                                            text = statement.clientOrPan,
                                            fontSize = 11.sp,
                                            color = HighDensityTextSecondary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Statement Date: ${statement.statementDate.ifEmpty { "Latest" }}",
                                        fontSize = 11.sp,
                                        color = HighDensityTextSecondary
                                    )

                                    Text(
                                        text = "${statement.holdings.size} holdings identified",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HighDensityTextPrimary
                                    )
                                }

                                HorizontalDivider(color = Color(0xFFDCFCE7), modifier = Modifier.padding(vertical = 2.dp))

                                // Totals Grid
                                val curFmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Total Invested", fontSize = 10.sp, color = HighDensityTextSecondary)
                                        Text(
                                            text = curFmt.format(statement.totalInvested).replace(".00", ""),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HighDensityTextPrimary
                                        )
                                    }

                                    Column {
                                        Text("Current Value", fontSize = 10.sp, color = HighDensityTextSecondary)
                                        Text(
                                            text = curFmt.format(statement.totalCurrentValue).replace(".00", ""),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F766E)
                                        )
                                    }

                                    Column {
                                        Text("Total Gain/Loss", fontSize = 10.sp, color = HighDensityTextSecondary)
                                        val pnlColor = if (statement.totalPnl >= 0) Emerald500 else Color(0xFFEF4444)
                                        val prefix = if (statement.totalPnl >= 0) "+" else ""
                                        Text(
                                            text = "$prefix${curFmt.format(statement.totalPnl).replace(".00", "")}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = pnlColor
                                        )
                                    }

                                    if (statement.totalXirr != null) {
                                        Column {
                                            Text("Overall XIRR", fontSize = 10.sp, color = HighDensityTextSecondary)
                                            Text(
                                                text = "${String.format("%.2f", statement.totalXirr)}%",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF6366F1)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Target Snapshot Selector & Update Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Apply to Month:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighDensityTextPrimary)
                                OutlinedTextField(
                                    value = targetSnapshotId,
                                    onValueChange = { targetSnapshotId = it },
                                    singleLine = true,
                                    modifier = Modifier.width(110.dp),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Update existing", fontSize = 11.sp, color = HighDensityTextSecondary)
                                Switch(
                                    checked = updateExisting,
                                    onCheckedChange = { updateExisting = it },
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "HOLDINGS READY FOR IMPORT (${statement.holdings.size})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Holdings List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(statement.holdings) { holding ->
                                HoldingPreviewItem(holding)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bottom Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { parsedStatement = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                enabled = !isProcessing
                            ) {
                                Text("Back", fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        try {
                                            val result = onImportConfirmed(statement, targetSnapshotId, updateExisting)
                                            importResult = result
                                        } catch (e: Exception) {
                                            errorMessage = "Import failed: ${e.localizedMessage}"
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(2f)
                                    .testTag("btn_confirm_import"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                enabled = !isProcessing
                            ) {
                                if (isProcessing) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Importing...", fontSize = 12.sp)
                                } else {
                                    Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import ${statement.holdings.size} Holdings", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HoldingPreviewItem(holding: ParsedInvestmentHolding) {
    val curFmt = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = holding.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = HighDensityTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = curFmt.format(holding.currentValue).replace(".00", ""),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = HighDensityTextPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (holding.subCategory.isNotBlank()) {
                        Text(
                            text = holding.subCategory,
                            fontSize = 10.sp,
                            color = HighDensityTextSecondary
                        )
                        Text("•", fontSize = 10.sp, color = HighDensityTextSecondary)
                    }
                    if (holding.identifier.isNotBlank()) {
                        Text(
                            text = "${if (holding.categoryId == "mutual_funds") "Folio" else "ISIN"}: ${holding.identifier}",
                            fontSize = 10.sp,
                            color = HighDensityTextSecondary
                        )
                    }
                }

                val pnlColor = if (holding.pnl >= 0) Emerald500 else Color(0xFFEF4444)
                val prefix = if (holding.pnl >= 0) "+" else ""
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Cost: ${curFmt.format(holding.investedAmount).replace(".00", "")}",
                        fontSize = 10.sp,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "($prefix${curFmt.format(holding.pnl).replace(".00", "")})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pnlColor
                    )
                    if (holding.xirrPercent != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEEF2FF)
                        ) {
                            Text(
                                text = "XIRR ${String.format("%.1f", holding.xirrPercent)}%",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4F46E5),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
