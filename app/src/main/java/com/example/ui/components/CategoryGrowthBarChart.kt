package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.CategoryGrowthItem
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Ruby500

@Composable
fun CategoryGrowthBarChart(
    growthItems: List<CategoryGrowthItem>,
    modifier: Modifier = Modifier
) {
    val validItems = growthItems
        .filter { it.growthPercentage != null && (it.currentValue > 0 || it.previousValue > 0) }
        .sortedByDescending { it.growthPercentage ?: 0.0 }

    if (validItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No category growth comparisons available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(growthItems) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(durationMillis = 750))
    }

    val maxAbsPct = validItems.maxOfOrNull { Math.abs(it.growthPercentage ?: 0.0) }?.coerceAtLeast(5.0) ?: 10.0

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        validItems.take(7).forEach { item ->
            val pct = item.growthPercentage ?: 0.0
            val isPos = pct >= 0
            val barColor = if (isPos) Emerald500 else Ruby500
            val barRatio = ((Math.abs(pct) / maxAbsPct).toFloat() * animatedProgress.value).coerceIn(0.04f, 1f)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    Text(
                        text = CurrencyFormatter.formatGrowthPercentage(pct),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress track & fill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = barRatio)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor)
                    )
                }
            }
        }
    }
}
