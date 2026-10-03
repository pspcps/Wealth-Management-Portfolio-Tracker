package com.example.ui.screens.insurance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.InsuranceEntity
import com.example.domain.model.InsuranceShieldSummary
import com.example.ui.components.CurrencyFormatter
import com.example.ui.components.DateFieldWithPicker
import com.example.ui.components.GlobalPrivacyEyeActionIcon
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsuranceScreen(
    viewModel: InsuranceViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val filteredPolicies = remember(state.policies, state.selectedFilterType) {
        if (state.selectedFilterType == null) {
            state.policies
        } else {
            state.policies.filter { it.insuranceType == state.selectedFilterType }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Insurance & Policies",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "${state.summary.activePoliciesCount} Active Policies · Protection Shield",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("btn_insurance_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = HighDensityTextPrimary
                        )
                    }
                },
                actions = {
                    GlobalPrivacyEyeActionIcon(testTag = "btn_insurance_privacy_eye")
                    IconButton(
                        onClick = { viewModel.openAddPolicy() },
                        modifier = Modifier.testTag("btn_add_policy")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "Add Policy",
                            tint = HighDensityPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openAddPolicy() },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Policy") },
                containerColor = HighDensityPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("fab_add_policy")
            )
        },
        containerColor = HighDensityBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Shield Overview Banner
            InsuranceShieldBanner(summary = state.summary)

            // Category Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = state.selectedFilterType == null,
                        onClick = { viewModel.setFilterType(null) },
                        label = { Text("All (${state.policies.size})") }
                    )
                }
                val types = listOf(
                    "TERM_LIFE" to "Life",
                    "HEALTH" to "Health",
                    "MOTOR" to "Motor",
                    "CRITICAL_ILLNESS" to "Critical Illness",
                    "HOME_PROPERTY" to "Property"
                )
                items(types) { (typeKey, label) ->
                    val count = state.policies.count { it.insuranceType == typeKey }
                    FilterChip(
                        selected = state.selectedFilterType == typeKey,
                        onClick = { viewModel.setFilterType(typeKey) },
                        label = { Text("$label ($count)") }
                    )
                }
            }

            // Policies List
            if (filteredPolicies.isEmpty()) {
                EmptyInsuranceView(onAddClick = { viewModel.openAddPolicy() })
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredPolicies, key = { it.id }) { policy ->
                        InsurancePolicyCard(
                            policy = policy,
                            onCardClick = { viewModel.selectPolicyForDetails(policy) },
                            onEdit = { viewModel.openEditPolicy(policy) },
                            onDelete = { viewModel.deletePolicy(policy.id) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Bottom Sheets
    if (state.isAddEditOpen) {
        AddEditPolicySheet(
            policy = state.editingPolicy,
            onDismiss = { viewModel.closeAddEdit() },
            onSave = { policy -> viewModel.savePolicy(policy) }
        )
    }

    if (state.selectedPolicyForDetails != null) {
        PolicyDetailSheet(
            policy = state.selectedPolicyForDetails!!,
            onDismiss = { viewModel.selectPolicyForDetails(null) },
            onEdit = {
                val pol = state.selectedPolicyForDetails!!
                viewModel.selectPolicyForDetails(null)
                viewModel.openEditPolicy(pol)
            }
        )
    }
}

@Composable
private fun InsuranceShieldBanner(summary: InsuranceShieldSummary) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HighDensityBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = HighDensityIncomeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PROTECTION SHIELD STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (summary.policiesExpiringSoonCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFEF3C7),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Text(
                            text = "${summary.policiesExpiringSoonCount} Due Soon",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFB45309),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else if (summary.activePoliciesCount == 0) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = "No Shield",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFDCFCE7),
                        border = BorderStroke(1.dp, Color(0xFFBBF7D0))
                    ) {
                        Text(
                            text = if (summary.isAdequatelyInsured) "Shield Active" else "Needs Review",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityIncomeGreen,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Cover Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Life Cover
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFDCFCE7))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Life Sum Assured", fontSize = 10.sp, color = HighDensityTextSecondary)
                        Text(
                            text = if (summary.totalLifeCover > 0) CurrencyFormatter.formatInrCompact(summary.totalLifeCover) else "None",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityIncomeGreen
                        )
                    }
                }

                // Health Cover
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFDBEAFE))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Health Cover", fontSize = 10.sp, color = HighDensityTextSecondary)
                        Text(
                            text = if (summary.totalHealthCover > 0) CurrencyFormatter.formatInrCompact(summary.totalHealthCover) else "None",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Annual Premiums:",
                    style = MaterialTheme.typography.bodySmall,
                    color = HighDensityTextSecondary
                )
                Text(
                    text = "${CurrencyFormatter.formatInr(summary.totalAnnualPremiums)} / year",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextPrimary
                )
            }
        }
    }
}

@Composable
private fun InsurancePolicyCard(
    policy: InsuranceEntity,
    onCardClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .testTag("policy_card_${policy.id}"),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, HighDensityBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(getInsuranceColorBg(policy.insuranceType)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getInsuranceIcon(policy.insuranceType),
                            contentDescription = null,
                            tint = getInsuranceColor(policy.insuranceType),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = policy.policyName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${policy.providerName} · ${getInsuranceTypeDisplayName(policy.insuranceType)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { expandedMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Menu",
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Full Details") },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onCardClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Policy") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                expandedMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Policy", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                expandedMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Cover and Premium details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Sum Insured / Cover",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = CurrencyFormatter.formatInr(policy.sumInsured),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = getInsuranceColor(policy.insuranceType)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Premium (${policy.premiumFrequency.lowercase().replaceFirstChar { it.uppercase() }})",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary
                    )
                    Text(
                        text = "${CurrencyFormatter.formatInr(policy.annualPremium)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                }
            }

            HorizontalDivider(color = HighDensityBorder.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (policy.renewalDate.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = HighDensityTextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Renews: ${policy.renewalDate}",
                            style = MaterialTheme.typography.labelSmall,
                            color = HighDensityTextSecondary
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (policy.nomineeName.isNotBlank()) {
                    Text(
                        text = "Nominee: ${policy.nomineeName.take(18)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = HighDensityTextSecondary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PolicyDetailSheet(
    policy: InsuranceEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HighDensityPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getInsuranceIcon(policy.insuranceType),
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = policy.policyName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = getInsuranceTypeDisplayName(policy.insuranceType),
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                }
            }

            HorizontalDivider(color = HighDensityBorder)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Key metrics card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HighDensityBg,
                    border = BorderStroke(1.dp, HighDensityBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailItem(label = "Insurance Type", value = getInsuranceTypeDisplayName(policy.insuranceType))
                        DetailItem(label = "Insurance Company", value = policy.providerName.ifBlank { "N/A" })
                        DetailItem(label = "Policy Number", value = policy.policyNumber.ifBlank { "N/A" })
                        DetailItem(label = "Sum Insured (Cover)", value = CurrencyFormatter.formatInr(policy.sumInsured))
                        DetailItem(label = "Premium Outflow", value = "${CurrencyFormatter.formatInr(policy.annualPremium)} (${policy.premiumFrequency})")
                        DetailItem(label = "Renewal Date", value = policy.renewalDate.ifBlank { "N/A" })
                        DetailItem(label = "Nominee", value = policy.nomineeName.ifBlank { "N/A" })
                        DetailItem(label = "Covered Persons", value = policy.insuredPerson.ifBlank { "Self" })
                    }
                }

                if (policy.notes.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = HighDensityPrimaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, HighDensityPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Notes & Tax Exemptions",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = policy.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = HighDensityTextPrimary
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Policy")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = HighDensityTextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HighDensityTextPrimary)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPolicySheet(
    policy: InsuranceEntity?,
    onDismiss: () -> Unit,
    onSave: (InsuranceEntity) -> Unit
) {
    var policyName by remember { mutableStateOf(policy?.policyName ?: "") }
    var insuranceType by remember { mutableStateOf(policy?.insuranceType ?: "TERM_LIFE") }
    var providerName by remember { mutableStateOf(policy?.providerName ?: "") }
    var policyNumber by remember { mutableStateOf(policy?.policyNumber ?: "") }
    var sumInsured by remember { mutableStateOf(if (policy != null && policy.sumInsured > 0) policy.sumInsured.toString() else "") }
    var annualPremium by remember { mutableStateOf(if (policy != null && policy.annualPremium > 0) policy.annualPremium.toString() else "") }
    var premiumFrequency by remember { mutableStateOf(policy?.premiumFrequency ?: "ANNUAL") }
    var renewalDate by remember { mutableStateOf(policy?.renewalDate ?: "") }
    var nomineeName by remember { mutableStateOf(policy?.nomineeName ?: "") }
    var insuredPerson by remember { mutableStateOf(policy?.insuredPerson ?: "") }
    var notes by remember { mutableStateOf(policy?.notes ?: "") }

    val allPolicyTypes = listOf(
        "TERM_LIFE" to "Term Life",
        "HEALTH" to "Health",
        "MOTOR" to "Motor / Auto",
        "CRITICAL_ILLNESS" to "Critical Illness",
        "HOME_PROPERTY" to "Property",
        "TRAVEL" to "Travel",
        "OTHER" to "Other"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HighDensityPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getInsuranceIcon(insuranceType),
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (policy == null) "Add Insurance Policy" else "Edit Policy Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "Track safety net, sum insured, and renewals",
                            style = MaterialTheme.typography.bodySmall,
                            color = HighDensityTextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = HighDensityTextSecondary)
                }
            }

            HorizontalDivider(color = HighDensityBorder)

            // Scrollable Form Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = policyName,
                    onValueChange = { policyName = it },
                    label = { Text("Policy Name (e.g. HDFC Life Click 2 Protect)*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Policy Category with LazyRow
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Policy Category",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = HighDensityTextSecondary
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allPolicyTypes) { (typeKey, label) ->
                            val isSelected = insuranceType == typeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { insuranceType = typeKey },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getInsuranceIcon(typeKey),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) HighDensityPrimary else HighDensityTextSecondary
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = HighDensityPrimaryContainer,
                                    selectedLabelColor = HighDensityPrimary
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = providerName,
                        onValueChange = { providerName = it },
                        label = { Text("Insurer (e.g. HDFC Life, Star)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = policyNumber,
                        onValueChange = { policyNumber = it },
                        label = { Text("Policy #") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sumInsured,
                        onValueChange = { sumInsured = it },
                        label = { Text("Sum Insured (₹)*") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = annualPremium,
                        onValueChange = { annualPremium = it },
                        label = { Text("Premium (₹)*") },
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, color = HighDensityExpenseRed) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DateFieldWithPicker(
                        value = renewalDate,
                        onValueChange = { renewalDate = it },
                        label = "Renewal Date",
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = nomineeName,
                        onValueChange = { nomineeName = it },
                        label = { Text("Nominee Name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = insuredPerson,
                    onValueChange = { insuredPerson = it },
                    label = { Text("Insured Members (e.g. Self, Family Floater)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Riders / Tax Exemption (Section 80D, 80C)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            HorizontalDivider(color = HighDensityBorder)

            // Fixed Sticky Footer Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (policyName.isNotBlank()) {
                            val cover = sumInsured.toDoubleOrNull() ?: 0.0
                            val prem = annualPremium.toDoubleOrNull() ?: 0.0

                            val updated = (policy ?: InsuranceEntity()).copy(
                                policyName = policyName.trim(),
                                insuranceType = insuranceType,
                                providerName = providerName.trim(),
                                policyNumber = policyNumber.trim(),
                                sumInsured = cover,
                                annualPremium = prem,
                                premiumFrequency = premiumFrequency,
                                renewalDate = renewalDate.trim(),
                                nomineeName = nomineeName.trim(),
                                insuredPerson = insuredPerson.trim(),
                                notes = notes.trim(),
                                isActive = true
                            )
                            onSave(updated)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary),
                    enabled = policyName.isNotBlank() && (sumInsured.isNotBlank() || annualPremium.isNotBlank())
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Policy")
                }
            }
        }
    }
}

@Composable
private fun EmptyInsuranceView(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDCFCE7)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = HighDensityIncomeGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = "No Insurance Policies Added",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = HighDensityTextPrimary
            )
            Text(
                text = "Safeguard your family and wealth. Keep track of Term Life sum assured, Health cover, motor policies, and renewal dates.",
                style = MaterialTheme.typography.bodySmall,
                color = HighDensityTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = HighDensityPrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Insurance Policy")
            }
        }
    }
}

private fun getInsuranceIcon(type: String): ImageVector {
    return when (type) {
        "TERM_LIFE" -> Icons.Default.Favorite
        "HEALTH" -> Icons.Default.HealthAndSafety
        "MOTOR" -> Icons.Default.DirectionsCar
        "CRITICAL_ILLNESS" -> Icons.Default.LocalHospital
        "HOME_PROPERTY" -> Icons.Default.Home
        else -> Icons.Default.Shield
    }
}

private fun getInsuranceColor(type: String): Color {
    return when (type) {
        "TERM_LIFE" -> Color(0xFF16A34A)
        "HEALTH" -> Color(0xFF2563EB)
        "MOTOR" -> Color(0xFFD97706)
        "CRITICAL_ILLNESS" -> Color(0xFFDC2626)
        "HOME_PROPERTY" -> Color(0xFF7C3AED)
        else -> HighDensityPrimary
    }
}

private fun getInsuranceColorBg(type: String): Color {
    return when (type) {
        "TERM_LIFE" -> Color(0xFFDCFCE7)
        "HEALTH" -> Color(0xFFDBEAFE)
        "MOTOR" -> Color(0xFFFEF3C7)
        "CRITICAL_ILLNESS" -> Color(0xFFFEE2E2)
        "HOME_PROPERTY" -> Color(0xFFF3E8FF)
        else -> HighDensityPrimaryContainer
    }
}

private fun getInsuranceTypeDisplayName(type: String): String {
    return when (type) {
        "TERM_LIFE" -> "Term Life"
        "HEALTH" -> "Health"
        "MOTOR" -> "Motor"
        "CRITICAL_ILLNESS" -> "Critical Illness"
        "HOME_PROPERTY" -> "Property"
        else -> "General"
    }
}
