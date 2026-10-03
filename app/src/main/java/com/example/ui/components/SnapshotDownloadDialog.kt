package com.example.ui.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.AssetEntity
import com.example.data.local.entity.AssetMonthlyValueEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.RecurringCashFlowEntity
import com.example.ui.theme.*
import com.example.util.SnapshotExporter

enum class ExportFormat(val title: String, val extension: String, val desc: String, val icon: ImageVector, val color: Color) {
    CSV("CSV Spreadsheet", ".csv", "Excel & Google Sheets tabular format with all assets & SIPs", Icons.Default.TableChart, Color(0xFF10B981)),
    STATEMENT("Financial Statement", ".txt", "Clean human-readable text statement report", Icons.Default.Description, Color(0xFF3B82F6)),
    JSON("Raw JSON Archive", ".json", "Structured JSON format for data portability & backups", Icons.Default.Code, Color(0xFF8B5CF6))
}

@Composable
fun SnapshotDownloadDialog(
    snapshotId: String,
    snapshotLabel: String,
    totalPortfolioValue: Double,
    categories: List<CategoryEntity>,
    assets: List<AssetEntity>,
    values: List<AssetMonthlyValueEntity>,
    sips: List<RecurringCashFlowEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .testTag("dialog_snapshot_download"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Download Snapshot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Period: $snapshotLabel ($snapshotId)",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = HighDensityPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "SNAPSHOT TOTAL VALUE",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextSecondary
                            )
                            Text(
                                text = CurrencyFormatter.formatInr(totalPortfolioValue),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = HighDensityTextPrimary
                            )
                        }
                        Text(
                            text = "${assets.size} assets",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityPrimary
                        )
                    }
                }

                Text(
                    text = "SELECT EXPORT FORMAT",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExportFormat.values().forEach { format ->
                        val isSelected = selectedFormat == format
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) format.color.copy(alpha = 0.08f) else Color.White,
                            border = BorderStroke(1.dp, if (isSelected) format.color else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFormat = format }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedFormat = format },
                                    colors = RadioButtonDefaults.colors(selectedColor = format.color)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${format.title} (${format.extension})",
                                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = format.desc,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            when (selectedFormat) {
                                ExportFormat.CSV -> SnapshotExporter.exportSnapshotCsv(context, snapshotId, snapshotLabel, categories, assets, values, sips)
                                ExportFormat.STATEMENT -> SnapshotExporter.exportSnapshotStatement(context, snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips)
                                ExportFormat.JSON -> SnapshotExporter.exportSnapshotJson(context, snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_save_snapshot_file"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Files", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            when (selectedFormat) {
                                ExportFormat.CSV -> SnapshotExporter.shareSnapshotCsv(context, snapshotId, snapshotLabel, categories, assets, values, sips)
                                ExportFormat.STATEMENT -> SnapshotExporter.shareSnapshotStatement(context, snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips)
                                ExportFormat.JSON -> SnapshotExporter.shareSnapshotJson(context, snapshotId, snapshotLabel, totalPortfolioValue, categories, assets, values, sips)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_share_snapshot"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Snapshot", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
