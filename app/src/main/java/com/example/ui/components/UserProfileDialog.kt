package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.repository.UserProfile
import com.example.ui.theme.*

@Composable
fun UserProfileDialog(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, email: String, tagline: String, targetNetWorth: Double, colorIndex: Int, dateOfBirth: String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    onRemoveImage: () -> Unit
) {
    var name by remember { mutableStateOf(userProfile.name) }
    var email by remember { mutableStateOf(userProfile.email) }
    var tagline by remember { mutableStateOf(userProfile.tagline) }
    var targetNetWorthText by remember { mutableStateOf(userProfile.targetNetWorth.toLong().toString()) }
    var selectedColorIndex by remember { mutableIntStateOf(userProfile.colorIndex) }
    var dateOfBirthText by remember { mutableStateOf(userProfile.dateOfBirth) }

    // Live calculated age from entered DOB
    val calculatedAge = remember(dateOfBirthText) {
        val tempProfile = UserProfile(dateOfBirth = dateOfBirthText)
        tempProfile.calculatedAge
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onImageSelected(it) }
    }

    // Dynamic preview profile object
    val previewProfile = remember(name, userProfile.profileImagePath, selectedColorIndex) {
        userProfile.copy(
            name = name.ifBlank { "User" },
            colorIndex = selectedColorIndex
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = Color.White,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Profile & Logo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Customize your name, logo avatar, and profile image",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Profile Avatar Preview & Upload Action
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        UserAvatarView(
                            userProfile = previewProfile,
                            size = 88.dp,
                            fontSize = 30.sp,
                            showEditBadge = true,
                            onClick = { imagePickerLauncher.launch("image/*") }
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("btn_upload_profile_image")
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (userProfile.profileImagePath != null) "Change Photo" else "Upload Photo",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            if (userProfile.profileImagePath != null) {
                                OutlinedButton(
                                    onClick = onRemoveImage,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Use Logo", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        Text(
                            text = if (userProfile.profileImagePath != null)
                                "Custom photo active"
                            else
                                "Initials Logo: \"${previewProfile.initials}\" generated from your name",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Logo Gradient Color Picker (Active when no custom image or for theme)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "AVATAR LOGO THEME",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AvatarGradients.forEachIndexed { index, colors ->
                                val isSelected = selectedColorIndex == index
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(colors))
                                        .clickable { selectedColorIndex = index }
                                        .then(
                                            if (isSelected) Modifier.border(2.5.dp, HighDensityPrimary, CircleShape)
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Input: Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name") },
                        placeholder = { Text("e.g. Prakash Seervi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_profile_name"),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = HighDensityPrimary)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Input: Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("e.g. prakash.seervi9460@gmail.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = HighDensityTextSecondary)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Input: Tagline / Bio
                    OutlinedTextField(
                        value = tagline,
                        onValueChange = { tagline = it },
                        label = { Text("Headline / Tagline") },
                        placeholder = { Text("e.g. Long-term Wealth Builder") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Badge, contentDescription = null, tint = HighDensityTextSecondary)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Input: Date of Birth (DOB)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        OutlinedTextField(
                            value = dateOfBirthText,
                            onValueChange = { input ->
                                // Format as YYYY-MM-DD
                                val filtered = input.filter { it.isDigit() || it == '-' }.take(10)
                                dateOfBirthText = filtered
                            },
                            label = { Text("Date of Birth (DOB)") },
                            placeholder = { Text("YYYY-MM-DD (e.g. 1998-05-15)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_profile_dob"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFFE11D48))
                            },
                            trailingIcon = {
                                if (calculatedAge != null) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = HighDensityPrimary.copy(alpha = 0.12f),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = "$calculatedAge yrs old",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = HighDensityPrimary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            },
                            supportingText = {
                                Text(
                                    text = if (calculatedAge != null)
                                        "Auto-calculated Age: $calculatedAge yrs (Used in FIRE, NPS & Retirement Calculators)"
                                    else
                                        "Enter as YYYY-MM-DD to auto-compute your age for calculators",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (calculatedAge != null) HighDensityPrimary else HighDensityTextSecondary
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Input: Target Net Worth (Financial Goal)
                    OutlinedTextField(
                        value = targetNetWorthText,
                        onValueChange = { targetNetWorthText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Target Net Worth Goal") },
                        prefix = { Text("₹ ") },
                        placeholder = { Text("5000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFEAB308))
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // Bottom Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val goal = targetNetWorthText.toDoubleOrNull() ?: 5000000.0
                            onSaveProfile(name, email, tagline, goal, selectedColorIndex, dateOfBirthText)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("btn_save_profile"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile")
                    }
                }
            }
        }
    }
}
