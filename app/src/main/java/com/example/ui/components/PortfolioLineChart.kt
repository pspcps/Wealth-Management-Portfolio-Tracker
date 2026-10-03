package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.HistoricalDataPoint
import com.example.ui.theme.Emerald400
import com.example.ui.theme.Emerald500
import kotlinx.coroutines.launch

@Composable
fun PortfolioLineChart(
    dataPoints: List<HistoricalDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = Emerald500,
    accentColor: Color = Emerald400
) {
    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No historical snapshots recorded yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(dataPoints) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Top highlight header when a point is touched or default to latest
        val activePoint = selectedIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.lastOrNull()
        if (activePoint != null) {
            val gainLoss = activePoint.portfolioValue - activePoint.investedValue
            val roiPct = if (activePoint.investedValue > 0) (gainLoss / activePoint.investedValue) * 100.0 else null
            val isGain = gainLoss >= 0.0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activePoint.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(activePoint.portfolioValue),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = lineColor
                    )
                }

                if (activePoint.investedValue > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Invested: ${CurrencyFormatter.formatInr(activePoint.investedValue)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = textSecondary
                        )
                        Text(
                            text = "P&L: ${CurrencyFormatter.formatInr(gainLoss, showSign = true)} (${CurrencyFormatter.formatGrowthPercentage(roiPct)})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = if (isGain) Color(0xFF16A34A) else Color(0xFFDC2626)
                        )
                    }
                }
            }
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp, 3.dp)
                        .background(lineColor, RoundedCornerShape(2.dp))
                )
                Text(
                    text = "Portfolio Value",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = textSecondary
                )
            }

            val hasInvested = dataPoints.any { it.investedValue > 0 }
            if (hasInvested) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 2.dp)
                            .background(Color(0xFF64748B), RoundedCornerShape(1.dp))
                    )
                    Text(
                        text = "Invested Capital",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = textSecondary
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val pointWidth = size.width / (if (dataPoints.size > 1) (dataPoints.size - 1) else 1)
                        val index = ((offset.x + pointWidth / 2) / pointWidth).toInt().coerceIn(0, dataPoints.size - 1)
                        selectedIndex = index
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val paddingBottom = 28.dp.toPx()
            val paddingTop = 16.dp.toPx()
            val graphHeight = height - paddingBottom - paddingTop

            val allValues = dataPoints.flatMap { listOf(it.portfolioValue, it.investedValue) }.filter { it > 0 }
            val minVal = allValues.minOrNull()?.let { it * 0.92 } ?: 0.0
            val maxVal = allValues.maxOrNull()?.let { if (it == minVal) it * 1.1 + 1000.0 else it * 1.08 } ?: 1000.0
            val valueRange = (maxVal - minVal).coerceAtLeast(1.0)

            // Draw horizontal grid lines (3 lines)
            val gridSteps = 3
            for (i in 0..gridSteps) {
                val y = paddingTop + (graphHeight / gridSteps) * i
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            if (dataPoints.size == 1) {
                // Single point render
                val x = width / 2f
                val y = paddingTop + graphHeight / 2f
                drawCircle(
                    color = lineColor,
                    radius = 8.dp.toPx(),
                    center = Offset(x, y)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = dataPoints[0].label,
                    topLeft = Offset(x - 20.dp.toPx(), height - paddingBottom + 6.dp.toPx()),
                    style = TextStyle(color = textSecondary, fontSize = 11.sp)
                )
                return@Canvas
            }

            val stepX = width / (dataPoints.size - 1)

            // Calculate points for Invested Capital line
            val hasInvestedValues = dataPoints.any { it.investedValue > 0 }
            if (hasInvestedValues) {
                val investedPoints = dataPoints.mapIndexed { index, point ->
                    val x = index * stepX
                    val inv = if (point.investedValue > 0) point.investedValue else point.portfolioValue
                    val normalizedY = ((inv - minVal) / valueRange).toFloat()
                    val y = paddingTop + graphHeight * (1f - (normalizedY * animatedProgress.value))
                    Offset(x, y)
                }

                val investedPath = Path().apply {
                    if (investedPoints.isNotEmpty()) {
                        moveTo(investedPoints[0].x, investedPoints[0].y)
                        for (i in 0 until investedPoints.size - 1) {
                            val p0 = investedPoints[i]
                            val p1 = investedPoints[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                    }
                }

                // Draw dashed Invested Capital line
                drawPath(
                    path = investedPath,
                    color = Color(0xFF94A3B8),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                        cap = StrokeCap.Round
                    )
                )
            }

            // Calculate points for Portfolio Value curve
            val points = dataPoints.mapIndexed { index, point ->
                val x = index * stepX
                val normalizedY = ((point.portfolioValue - minVal) / valueRange).toFloat()
                val y = paddingTop + graphHeight * (1f - (normalizedY * animatedProgress.value))
                Offset(x, y)
            }

            // Path for smooth cubic curve
            val strokePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points[0].x, points[0].y)
                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                        val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                        cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                    }
                }
            }

            // Gradient Fill Path
            val fillPath = Path().apply {
                addPath(strokePath)
                lineTo(points.last().x, paddingTop + graphHeight)
                lineTo(points.first().x, paddingTop + graphHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.30f * animatedProgress.value),
                        lineColor.copy(alpha = 0.0f)
                    ),
                    startY = paddingTop,
                    endY = paddingTop + graphHeight
                )
            )

            // Draw Value line
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw data point dots & labels
            points.forEachIndexed { index, pt ->
                val isSelected = selectedIndex == index || (selectedIndex == null && index == points.size - 1)
                if (isSelected) {
                    // Outer glow
                    drawCircle(
                        color = accentColor.copy(alpha = 0.3f),
                        radius = 10.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = accentColor,
                        radius = 5.dp.toPx(),
                        center = pt
                    )
                } else {
                    drawCircle(
                        color = lineColor,
                        radius = 3.dp.toPx(),
                        center = pt
                    )
                }

                // Month labels on X axis
                val skip = if (dataPoints.size > 8) 2 else 1
                if (index % skip == 0 || index == dataPoints.size - 1) {
                    val label = dataPoints[index].label
                    val textLayout = textMeasurer.measure(label, TextStyle(fontSize = 10.sp))
                    drawText(
                        textLayoutResult = textLayout,
                        topLeft = Offset((pt.x - textLayout.size.width / 2f).coerceIn(0f, width - textLayout.size.width), height - paddingBottom + 4.dp.toPx())
                    )
                }
            }
        }
    }
}
