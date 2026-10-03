package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HistoricalDataPoint
import com.example.ui.theme.Ruby400
import com.example.ui.theme.Ruby500

@Composable
fun ExpenseTrendChart(
    dataPoints: List<HistoricalDataPoint>,
    modifier: Modifier = Modifier,
    selectedSnapshotId: String? = null
) {
    val expensePoints = dataPoints.filter { it.expenseValue > 0 || dataPoints.size <= 6 }
    if (expensePoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No monthly expenses recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(expensePoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 750))
    }

    val textMeasurer = rememberTextMeasurer()
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val barColorDefault = Ruby500.copy(alpha = 0.7f)
    val barColorActive = Ruby400

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val width = size.width
        val height = size.height
        val paddingBottom = 26.dp.toPx()
        val chartHeight = height - paddingBottom

        val maxVal = expensePoints.maxOfOrNull { it.expenseValue }?.coerceAtLeast(1000.0) ?: 10000.0

        val count = expensePoints.size
        val totalSlotWidth = width / count
        val barWidth = (totalSlotWidth * 0.55f).coerceIn(12.dp.toPx(), 36.dp.toPx())

        expensePoints.forEachIndexed { index, point ->
            val slotCenter = index * totalSlotWidth + (totalSlotWidth / 2f)
            val barHeight = ((point.expenseValue / maxVal).toFloat() * chartHeight * animatedProgress.value).coerceAtLeast(4.dp.toPx())
            val topLeft = Offset(slotCenter - barWidth / 2f, chartHeight - barHeight)

            val isSelected = point.snapshotId == selectedSnapshotId
            val color = if (isSelected) barColorActive else barColorDefault

            drawRoundRect(
                color = color,
                topLeft = topLeft,
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Month Label below bar
            val textLayout = textMeasurer.measure(point.label.take(3), TextStyle(color = textSecondary, fontSize = 10.sp))
            drawText(
                textLayoutResult = textLayout,
                topLeft = Offset(slotCenter - textLayout.size.width / 2f, height - paddingBottom + 4.dp.toPx())
            )
        }
    }
}
