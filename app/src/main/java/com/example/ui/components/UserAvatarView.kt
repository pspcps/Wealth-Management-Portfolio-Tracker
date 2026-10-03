package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.repository.UserProfile
import com.example.ui.theme.*
import java.io.File

val AvatarGradients = listOf(
    listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6)), // Deep Indigo
    listOf(Color(0xFF065F46), Color(0xFF10B981)), // Emerald Green
    listOf(Color(0xFF5B21B6), Color(0xFF8B5CF6)), // Royal Purple
    listOf(Color(0xFF9F1239), Color(0xFFF43F5E)), // Rose Crimson
    listOf(Color(0xFF9A3412), Color(0xFFF97316)), // Amber Sunset
    listOf(Color(0xFF164E63), Color(0xFF06B6D4))  // Teal Ocean
)

@Composable
fun UserAvatarView(
    userProfile: UserProfile,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: TextUnit = 16.sp,
    showEditBadge: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val gradientColors = AvatarGradients.getOrElse(userProfile.colorIndex) { AvatarGradients[0] }
    val hasImage = !userProfile.profileImagePath.isNullOrEmpty() && File(userProfile.profileImagePath).exists()

    Box(
        modifier = modifier
            .size(size)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        if (hasImage) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(File(userProfile.profileImagePath!!))
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Photo of ${userProfile.name}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            )
        } else {
            // High Density Initials Logo Box with Gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = gradientColors
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userProfile.initials,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Optional Camera Edit Badge
        if (showEditBadge) {
            Surface(
                shape = CircleShape,
                color = HighDensityPrimary,
                border = BorderStroke(1.5.dp, Color.White),
                modifier = Modifier
                    .size(size * 0.35f)
                    .align(Alignment.BottomEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change Photo",
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.2f)
                    )
                }
            }
        }
    }
}
