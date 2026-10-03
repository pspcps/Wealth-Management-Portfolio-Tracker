package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.MonthlySnapshotEntity
import com.example.data.local.entity.PortfolioViewEntity
import com.example.domain.model.GrowthMetric
import com.example.domain.model.TimeRangeOption
import com.example.ui.theme.*
import java.text.DateFormatSymbols

@Composable
fun GrowthIndicatorBadge(
    growth: GrowthMetric,
    modifier: Modifier = Modifier,
    showAmount: Boolean = true,
    compact: Boolean = false
) {
    val bgColor: Color
    val contentColor: Color
    val icon: ImageVector

    when {
        growth.isPositive -> {
            bgColor = Emerald500.copy(alpha = 0.15f)
            contentColor = Emerald400
            icon = Icons.Default.ArrowUpward
        }
        growth.isNegative -> {
            bgColor = Ruby500.copy(alpha = 0.15f)
            contentColor = Ruby400
            icon = Icons.Default.ArrowDownward
        }
        else -> {
            bgColor = MaterialTheme.colorScheme.surfaceVariant
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            icon = Icons.Default.Remove
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 3.dp else 6.dp
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (compact) 12.dp else 16.dp)
            )

            val pctText = CurrencyFormatter.formatGrowthPercentage(growth.growthPercentage)
            if (showAmount && growth.hasPreviousData && !compact) {
                val amtText = CurrencyFormatter.formatInr(growth.diffAmount, showSign = true)
                Text(
                    text = "$amtText ($pctText)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            } else {
                Text(
                    text = pctText,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun PeriodSelector(
    selectedRange: TimeRangeOption,
    onRangeSelected: (TimeRangeOption) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TimeRangeOption.values().forEach { option ->
            val isSelected = option == selectedRange
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRangeSelected(option) },
                color = if (isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) HighDensityPrimary else Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerBottomSheet(
    snapshots: List<MonthlySnapshotEntity>,
    selectedSnapshotId: String?,
    onSnapshotSelected: (String) -> Unit,
    onCreateSnapshot: (year: Int, month: Int) -> Unit,
    onDeleteSnapshot: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var showCreateNew by remember { mutableStateOf(false) }
    var newYear by remember { mutableStateOf(2026) }
    var newMonth by remember { mutableStateOf(8) }
    var snapshotToDelete by remember { mutableStateOf<MonthlySnapshotEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showCreateNew) "Create Snapshot" else "Select Month Snapshot",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
                IconButton(onClick = { showCreateNew = !showCreateNew }) {
                    Icon(
                        imageVector = if (showCreateNew) Icons.Default.List else Icons.Default.Add,
                        contentDescription = if (showCreateNew) "View List" else "Add Month",
                        tint = HighDensityPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (!showCreateNew) {
                if (snapshots.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No snapshots recorded yet. Tap + to start your first month snapshot.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = HighDensityTextSecondary
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        snapshots.sortedWith(compareByDescending<MonthlySnapshotEntity> { it.year }.thenByDescending { it.month }).forEach { snapshot ->
                            val isSelected = snapshot.id == selectedSnapshotId
                            val monthName = DateFormatSymbols().months.getOrNull(snapshot.month - 1) ?: "${snapshot.month}"
                            
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSnapshotSelected(snapshot.id)
                                        onDismiss()
                                    },
                                color = if (isSelected) HighDensityPrimaryContainer else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) HighDensityPillBorder else Color(0xFFE2E8F0)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (isSelected) HighDensityPrimary else HighDensityTextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "$monthName ${snapshot.year}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) HighDensityOnPrimaryContainer else HighDensityTextPrimary
                                            )
                                            if (snapshot.notes.isNotEmpty()) {
                                                Text(
                                                    text = snapshot.notes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = HighDensityTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = HighDensityPrimary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        if (onDeleteSnapshot != null) {
                                            IconButton(
                                                onClick = { snapshotToDelete = snapshot },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Delete Snapshot",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { showCreateNew = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Month Snapshot")
                }
            } else {
                // New Snapshot Form
                Text(
                    text = "Select Year",
                    style = MaterialTheme.typography.labelLarge,
                    color = HighDensityTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(2025, 2026, 2027).forEach { yr ->
                        val isYrSel = newYear == yr
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { newYear = yr },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isYrSel) HighDensityPrimaryContainer else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isYrSel) HighDensityPillBorder else Color.Transparent)
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$yr",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isYrSel) HighDensityOnPrimaryContainer else HighDensityTextPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Select Month",
                    style = MaterialTheme.typography.labelLarge,
                    color = HighDensityTextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Months grid
                val shortMonths = DateFormatSymbols().shortMonths
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..2) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..3) {
                                val mIndex = row * 4 + col
                                val mNumber = mIndex + 1
                                val mName = shortMonths.getOrNull(mIndex) ?: "$mNumber"
                                val isSelected = newMonth == mNumber

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { newMonth = mNumber },
                                    color = if (isSelected) HighDensityPrimaryContainer else Color(0xFFF1F5F9),
                                    border = BorderStroke(1.dp, if (isSelected) HighDensityPillBorder else Color.Transparent),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) HighDensityOnPrimaryContainer else HighDensityTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        onCreateSnapshot(newYear, newMonth)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                ) {
                    Text("Create Snapshot")
                }
            }
        }
    }

    if (snapshotToDelete != null) {
        val snap = snapshotToDelete!!
        val mName = DateFormatSymbols().months.getOrNull(snap.month - 1) ?: "${snap.month}"
        AlertDialog(
            onDismissRequest = { snapshotToDelete = null },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Delete Month Snapshot?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '$mName ${snap.year}'? This will delete all monthly asset valuations and expense entries recorded for this period.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HighDensityTextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSnapshot?.invoke(snap.id)
                        snapshotToDelete = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { snapshotToDelete = null }) {
                    Text("Cancel", color = HighDensityTextSecondary)
                }
            }
        )
    }
}
