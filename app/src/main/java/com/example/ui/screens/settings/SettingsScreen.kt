package com.example.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.util.AutoBackupManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.UserAvatarView
import com.example.ui.components.UserProfileDialog
import com.example.ui.components.BrowseBackupFilesDialog
import com.example.ui.theme.*
import com.example.util.FinancialPrivacyManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val backupPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        viewModel.refreshAutoBackupState()
        val anyGranted = results.values.any { it }
        if (anyGranted) {
            Toast.makeText(context, "Permissions granted! Taking daily backup...", Toast.LENGTH_SHORT).show()
            viewModel.triggerAutoBackupNow()
        } else {
            Toast.makeText(context, "Permissions updated. Backups will run with available storage.", Toast.LENGTH_SHORT).show()
        }
    }

    var showClearConfirmation by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.operationSuccessMessage) {
        state.operationSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        containerColor = HighDensityBg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_settings")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = HighDensityTextPrimary
                            )
                        }

                        Column {
                            Text(
                                text = "Settings & Profile",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = "Account, profile logo & database controls",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }
                    }

                    GlobalPrivacyEyeButton(
                        size = 36.dp,
                        iconSize = 18.dp,
                        testTag = "btn_settings_privacy_eye"
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // User Profile Section
            item {
                Text(
                    text = "USER PROFILE & LOGO",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )
            }

            // User Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            UserAvatarView(
                                userProfile = state.userProfile,
                                size = 56.dp,
                                fontSize = 20.sp,
                                showEditBadge = true,
                                onClick = { showProfileDialog = true }
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = state.userProfile.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                                Text(
                                    text = state.userProfile.tagline,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = HighDensityPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (state.userProfile.email.isNotBlank()) {
                                    Text(
                                        text = state.userProfile.email,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }
                        }

                        // Target Net Worth Chip
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF9C3),
                            border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.EmojiEvents,
                                        contentDescription = null,
                                        tint = Color(0xFFCA8A04),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Target Net Worth Goal",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF854D0E)
                                    )
                                }
                                Text(
                                    text = CurrencyFormatter.formatInr(state.userProfile.targetNetWorth),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF854D0E)
                                )
                            }
                        }

                        Button(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_edit_profile_settings"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Edit Profile & Change Logo / Photo",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Security & App Lock Section
            item {
                Text(
                    text = "SECURITY & PRIVACY",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )
            }

            item {
                var showPinDialog by remember { mutableStateOf(false) }
                var pinInput by remember { mutableStateOf("") }
                var pinConfirmInput by remember { mutableStateOf("") }
                var pinError by remember { mutableStateOf<String?>(null) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
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
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (state.isAppLockEnabled) Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (state.isAppLockEnabled) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (state.isAppLockEnabled) Emerald500 else Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "App Lock on Launch",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = if (state.isAppLockEnabled) "Protected with Fingerprint / Passcode" else "Off • App opens without security prompt",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (state.isAppLockEnabled) Emerald500 else HighDensityTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = state.isAppLockEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        if (state.hasPin) {
                                            viewModel.setAppLockEnabled(true)
                                        } else {
                                            showPinDialog = true
                                        }
                                    } else {
                                        viewModel.setAppLockEnabled(false)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HighDensityPrimary
                                )
                            )
                        }

                        if (state.isAppLockEnabled) {
                            HorizontalDivider(color = Color(0xFFF1F5F9))

                            // Biometrics Sub-Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Fingerprint / Phone Lock",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HighDensityTextPrimary
                                        )
                                        Text(
                                            text = "Use device biometrics or phone pattern/password",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = HighDensityTextSecondary
                                        )
                                    }
                                }

                                Switch(
                                    checked = state.isBiometricEnabled,
                                    onCheckedChange = { viewModel.setBiometricEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = HighDensityPrimary
                                    )
                                )
                            }

                            // Change 4-digit Passcode PIN
                            OutlinedButton(
                                onClick = {
                                    pinInput = ""
                                    pinConfirmInput = ""
                                    pinError = null
                                    showPinDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Icon(Icons.Default.Pin, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (state.hasPin) "Change 4-Digit Passcode PIN" else "Set Backup Passcode PIN",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextPrimary
                                )
                            }
                        }
                    }
                }

                // PIN Setup Dialog
                if (showPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showPinDialog = false },
                        containerColor = Color.White,
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = HighDensityPrimary)
                                Text("Set 4-Digit Passcode", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Enter a 4-digit PIN to secure your wealth tracker on launch as a passcode fallback.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = HighDensityTextSecondary
                                )

                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                                    label = { Text("Enter 4-Digit PIN") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = pinConfirmInput,
                                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinConfirmInput = it },
                                    label = { Text("Confirm PIN") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (pinError != null) {
                                    Text(
                                        text = pinError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (pinInput.length != 4) {
                                        pinError = "PIN must be exactly 4 digits."
                                    } else if (pinInput != pinConfirmInput) {
                                        pinError = "PINs do not match."
                                    } else {
                                        viewModel.setAppLockEnabled(true, pinInput)
                                        showPinDialog = false
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                            ) {
                                Text("Save & Enable Lock", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPinDialog = false }) {
                                Text("Cancel", color = HighDensityTextSecondary)
                            }
                        }
                    )
                }
            }

            // Financial Privacy & Number Masking Section
            item {
                Text(
                    text = "FINANCIAL PRIVACY & NUMBER MASKING",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )
            }

            item {
                var hideByDefault by remember {
                    mutableStateOf(FinancialPrivacyManager.isHideByDefault(context))
                }
                val isAmountsVisible = FinancialPrivacyManager.isAmountsVisible.value

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Option 1: Hide balances by default on app launch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (hideByDefault) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (hideByDefault) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = if (hideByDefault) HighDensityPrimary else Color(0xFF64748B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Hide Numbers by Default",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = if (hideByDefault)
                                            "Enabled • Net worth & balances start hidden as ₹ ••••••"
                                        else
                                            "Disabled • Numbers are shown immediately on app open",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = HighDensityTextSecondary
                                    )
                                }
                            }

                            Switch(
                                checked = hideByDefault,
                                onCheckedChange = { isChecked ->
                                    hideByDefault = isChecked
                                    FinancialPrivacyManager.setHideByDefault(context, isChecked)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = HighDensityPrimary
                                ),
                                modifier = Modifier.testTag("switch_hide_numbers_by_default")
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Option 2: Live Global Privacy Eye toggle with preview
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current Privacy Mode Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HighDensityTextPrimary
                                )
                                Text(
                                    text = if (isAmountsVisible)
                                        "👁 Numbers are currently visible (tap eye to hide)"
                                    else
                                        "🔒 Numbers are currently masked (₹ ••••••)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = if (isAmountsVisible) Color(0xFF16A34A) else HighDensityPrimary
                                )
                            }

                            GlobalPrivacyEyeButton(
                                size = 38.dp,
                                iconSize = 20.dp,
                                testTag = "btn_settings_privacy_mode_toggle"
                            )
                        }
                    }
                }
            }

            // Data Management Section
            item {
                Text(
                    text = "DATA MANAGEMENT & BACKUP",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )
            }

            // Daily Auto-Backup (portfolioBackup Folder) Card
            item {
                var showBrowseBackupsDialog by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val autoBackup = state.autoBackupState
                val isEnabled = autoBackup?.isEnabled ?: true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    val hasPermissions = AutoBackupManager.hasRequiredPermissions(context)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFDCFCE7),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.FolderZip,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "Daily Auto-Backup",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityTextPrimary
                                    )
                                    Text(
                                        text = if (isEnabled) "Active • Mobile folder: portfolioBackup" else "Disabled • Backups paused",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = if (isEnabled) Emerald500 else Color(0xFF64748B),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Switch(
                                checked = isEnabled,
                                onCheckedChange = { checked ->
                                    if (checked && !hasPermissions) {
                                        val perms = AutoBackupManager.getRequiredPermissions()
                                        if (perms.isNotEmpty()) {
                                            backupPermissionLauncher.launch(perms.toTypedArray())
                                        }
                                    }
                                    viewModel.setDailyAutoBackupEnabled(checked)
                                },
                                modifier = Modifier.testTag("switch_daily_auto_backup")
                            )
                        }

                        Text(
                            text = "Every day you open the app, your complete portfolio data (investments, valuations, SIPs, loans, insurance, and expenses) is automatically archived to your phone's storage in standard JSON format.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary,
                            lineHeight = 16.sp
                        )

                        // Permission Alert Banner if permissions needed
                        if (!hasPermissions) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFEF3C7),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = null,
                                            tint = Color(0xFFD97706),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Text(
                                                text = "Storage & Alerts Permission",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF92400E)
                                            )
                                            Text(
                                                text = "Grant permission to save directly to phone Documents folder",
                                                fontSize = 10.sp,
                                                color = Color(0xFFB45309)
                                            )
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            val perms = AutoBackupManager.getRequiredPermissions()
                                            if (perms.isNotEmpty()) {
                                                backupPermissionLauncher.launch(perms.toTypedArray())
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("btn_grant_backup_permission")
                                    ) {
                                        Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                                Text("Storage & Notification Access Active", fontSize = 11.sp, color = Emerald500, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Folder: Documents / portfolioBackup",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                val lastTime = autoBackup?.lastBackupTimestamp ?: 0L
                                val formattedLast = if (lastTime > 0) {
                                    java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault()).format(java.util.Date(lastTime))
                                } else "No backup taken yet"

                                Text(
                                    text = "Last Saved: $formattedLast (${state.backupFiles.size} archives found)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val perms = AutoBackupManager.getRequiredPermissions()
                                    if (perms.isNotEmpty() && !AutoBackupManager.hasRequiredPermissions(context)) {
                                        backupPermissionLauncher.launch(perms.toTypedArray())
                                    } else {
                                        viewModel.triggerAutoBackupNow()
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_auto_backup_now"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                                enabled = !state.isOperating
                            ) {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.refreshAutoBackupState()
                                    showBrowseBackupsDialog = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_browse_portfolio_backups"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFF0F766E)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F766E)),
                                enabled = !state.isOperating
                            ) {
                                Icon(Icons.Default.RestorePage, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Browse Backups", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (showBrowseBackupsDialog) {
                    BrowseBackupFilesDialog(
                        backupFiles = state.backupFiles,
                        onDismiss = { showBrowseBackupsDialog = false },
                        onRestoreFile = { file ->
                            viewModel.restoreFromBackupFile(file)
                        },
                        onShareFile = { file ->
                            com.example.util.AutoBackupManager.shareBackupFile(context, file)
                        }
                    )
                }
            }

            // Google Drive & Cloud Backup Card
            item {
                var showBackupExportDialog by remember { mutableStateOf(false) }
                var showBackupRestoreDialog by remember { mutableStateOf(false) }
                var generatedBackupJson by remember { mutableStateOf("") }
                var isGeneratingBackup by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()
                val context = LocalContext.current

                // Launcher to save backup directly as a .json file into Google Drive or device
                val exportFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.CreateDocument("application/json")
                ) { uri: Uri? ->
                    if (uri != null && generatedBackupJson.isNotBlank()) {
                        try {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                stream.write(generatedBackupJson.toByteArray())
                            }
                            showBackupExportDialog = false
                        } catch (_: Exception) {}
                    }
                }

                // Launcher to pick a .json file directly from Google Drive or device
                val importFileLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocument()
                ) { uri: Uri? ->
                    if (uri != null) {
                        try {
                            val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            if (!content.isNullOrBlank()) {
                                viewModel.restoreFullBackup(content.trim())
                                showBackupRestoreDialog = false
                            }
                        } catch (_: Exception) {}
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE0F2FE),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.CloudSync,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Google Drive & Cloud Backup",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                                Text(
                                    text = "100% Free • Private personal backup",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Emerald500,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "Back up or restore all historical monthly snapshots, asset valuations, recurring flows, expenses, and profile in one secure archive file.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isGeneratingBackup = true
                                        generatedBackupJson = viewModel.getFullBackupJson()
                                        isGeneratingBackup = false
                                        showBackupExportDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_backup_drive_export"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                enabled = !state.isOperating && !isGeneratingBackup
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { showBackupRestoreDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_backup_drive_restore"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, HighDensityPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = HighDensityPrimary),
                                enabled = !state.isOperating
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Backup", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Backup Export & Drive Sync Dialog
                if (showBackupExportDialog) {
                    val clipboardManager = LocalClipboardManager.current

                    AlertDialog(
                        onDismissRequest = { showBackupExportDialog = false },
                        containerColor = Color.White,
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Emerald500)
                                Text("Portfolio Cloud Backup", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                                ) {
                                    Text(
                                        text = "✓ Full portfolio archive generated (all past snapshots, asset histories, flows, and profile). Choose how you'd like to store it:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                Button(
                                    onClick = {
                                        val fileName = "Portfolio_Backup_${java.text.SimpleDateFormat("yyyy_MM_dd", java.util.Locale.US).format(java.util.Date())}.json"
                                        exportFileLauncher.launch(fileName)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FolderSpecial, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Directly to Google Drive / Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(generatedBackupJson))
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Copy JSON", fontSize = 11.sp)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, generatedBackupJson)
                                                putExtra(Intent.EXTRA_SUBJECT, "Portfolio_Backup_${System.currentTimeMillis()}.json")
                                                type = "application/json"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share via Google Drive, Mail or WhatsApp")
                                            context.startActivity(shareIntent)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Share Archive", fontSize = 11.sp)
                                    }
                                }

                                OutlinedTextField(
                                    value = generatedBackupJson.take(300) + if (generatedBackupJson.length > 300) "\n... [${generatedBackupJson.length} bytes total]" else "",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Backup Preview") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showBackupExportDialog = false }) {
                                Text("Close", color = HighDensityPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                // Restore Backup Dialog
                if (showBackupRestoreDialog) {
                    var restoreJsonText by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showBackupRestoreDialog = false },
                        containerColor = Color.White,
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = HighDensityPrimary)
                                Text("Restore Backup", fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Restore multi-month snapshots, asset valuations, recurring flows, expenses, and profile from Google Drive or a backup file.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                    color = HighDensityTextSecondary
                                )

                                Button(
                                    onClick = {
                                        importFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Pick File (.json) from Google Drive / Files", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                    Text(" or paste text ", fontSize = 11.sp, color = HighDensityTextSecondary)
                                    HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                                }

                                OutlinedTextField(
                                    value = restoreJsonText,
                                    onValueChange = { restoreJsonText = it },
                                    label = { Text("Paste Backup JSON Content") },
                                    placeholder = { Text("{\n  \"snapshots\": [...],\n  \"assets\": [...]\n}") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (restoreJsonText.isNotBlank()) {
                                        viewModel.restoreFullBackup(restoreJsonText.trim())
                                        showBackupRestoreDialog = false
                                    }
                                },
                                enabled = restoreJsonText.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Restore Data", fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showBackupRestoreDialog = false }) {
                                Text("Cancel", color = HighDensityTextSecondary)
                            }
                        }
                    )
                }
            }

            // Restore Default Categories Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEDE9FE),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Category,
                                        contentDescription = null,
                                        tint = Color(0xFF7C3AED),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Restore Default Categories",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                                Text(
                                    text = "Re-populates any missing standard asset categories",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }
                        }

                        Text(
                            text = "Ensures all default categories (Mutual Funds, Indian Stocks, US Stocks, Gold, EPF, NPS, RD, Real Estate, Crypto, Sodexo, etc.) exist without affecting existing asset entries.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )

                        FilledTonalButton(
                            onClick = { viewModel.restoreDefaultCategories() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_restore_default_categories"),
                            shape = RoundedCornerShape(10.dp),
                            enabled = !state.isOperating
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Restore Standard Categories", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Seed Demo Data Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = HighDensityPrimaryContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AutoGraph,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Load Sample Demo Portfolio",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                                Text(
                                    text = "Populates 8 months of historical data (Jan 2026 - Aug 2026)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }
                        }

                        Text(
                            text = "Includes realistic balances across Mutual Funds, Indian Stocks, US Stocks, Digital Gold, EPF, NPS, Sodexo, monthly SIPs, expenses, active loans, and insurance policies to experience all charts and protection dashboards.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )

                        Button(
                            onClick = { viewModel.seedDemoData() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_seed_demo_data"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                            enabled = !state.isOperating
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Load 8-Month Sample Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Clear All Data Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Ruby500.copy(alpha = 0.12f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = null,
                                        tint = Ruby500,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Reset to Zero State",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Ruby500
                                )
                                Text(
                                    text = "Clear all assets, values, and expenses",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = HighDensityTextSecondary
                                )
                            }
                        }

                        Text(
                            text = "Clears all entered asset holdings and expense transactions while retaining default asset categories and portfolio views for a clean fresh start.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )

                        OutlinedButton(
                            onClick = { showClearConfirmation = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_clear_all_data"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Ruby500.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Ruby500
                            ),
                            enabled = !state.isOperating
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset / Clear All Data", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Built By & Developer Details Card
            item {
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = HighDensityPrimary,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "PS",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "Built & Created By",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityPrimary
                                )
                                Text(
                                    text = "Prakash Seervi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = HighDensityTextPrimary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://diffwise.vercel.app/#/portfolio")
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(15.dp), tint = HighDensityPrimary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Portfolio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                            }

                            OutlinedButton(
                                onClick = {
                                    try {
                                        uriHandler.openUri("https://www.linkedin.com/in/prakashseervi63/")
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(15.dp), tint = Color(0xFF0A66C2))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("LinkedIn", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A66C2))
                            }
                        }
                    }
                }
            }

            // Privacy & Architecture Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = HighDensitySecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Privacy & Local Storage",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                        }
                        Text(
                            text = "• 100% on-device SQLite / Room database storage.\n• No cloud sync or credentials required.\n• Complete privacy for your net worth balances.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                            color = HighDensityTextSecondary
                        )
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        com.example.ui.components.SnapshotImportDialog(
            onDismiss = { showImportDialog = false },
            onImportConfirmed = { json ->
                showImportDialog = false
                viewModel.importJsonSnapshot(json)
            }
        )
    }

    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = state.userProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, email, tagline, targetNetWorth, colorIndex, dob ->
                viewModel.updateUserProfile(name, email, tagline, targetNetWorth, colorIndex, dob)
            },
            onImageSelected = { uri ->
                viewModel.saveProfileImage(uri)
            },
            onRemoveImage = {
                viewModel.removeProfileImage()
            }
        )
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = {
                Text(
                    text = "Clear All Portfolio Data?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all assets, valuations, recurring SIPs, and expense records? Default categories will remain intact.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Ruby500)
                ) {
                    Text("Yes, Clear Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
