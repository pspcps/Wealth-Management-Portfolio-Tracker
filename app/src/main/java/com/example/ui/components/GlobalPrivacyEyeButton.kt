package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.HighDensityPrimary
import com.example.util.FinancialPrivacyManager

/**
 * Global Eye Toggle button for financial privacy.
 * - Default: Balances are hidden (shows VisibilityOff icon with privacy indicator)
 * - Tapped: Balances are revealed (shows Visibility icon)
 * Updates numbers globally across all screens via FinancialPrivacyManager.
 */
@Composable
fun GlobalPrivacyEyeButton(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 20.dp,
    testTag: String = "btn_global_privacy_eye"
) {
    val context = LocalContext.current
    val isVisible = FinancialPrivacyManager.isAmountsVisible.value

    Surface(
        shape = CircleShape,
        color = if (isVisible) Color.White else Color(0xFFEFF6FF),
        shadowElevation = 2.dp,
        border = BorderStroke(
            1.dp,
            if (isVisible) Color(0xFFE2E8F0) else Color(0xFF93C5FD)
        ),
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable {
                FinancialPrivacyManager.toggleAmountsVisibility(context)
            }
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                contentDescription = if (isVisible) "Hide Financial Numbers" else "Reveal Financial Numbers",
                tint = if (isVisible) Color(0xFF16A34A) else Color(0xFF2563EB),
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

/**
 * TopAppBar Action Icon variant for Material 3 TopAppBars
 */
@Composable
fun GlobalPrivacyEyeActionIcon(
    modifier: Modifier = Modifier,
    testTag: String = "btn_topbar_privacy_eye"
) {
    val context = LocalContext.current
    val isVisible = FinancialPrivacyManager.isAmountsVisible.value

    IconButton(
        onClick = {
            FinancialPrivacyManager.toggleAmountsVisibility(context)
        },
        modifier = modifier.testTag(testTag)
    ) {
        Icon(
            imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (isVisible) "Hide Financial Numbers" else "Reveal Financial Numbers",
            tint = if (isVisible) Color(0xFF16A34A) else Color(0xFF2563EB)
        )
    }
}
