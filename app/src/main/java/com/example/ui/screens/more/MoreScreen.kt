package com.example.ui.screens.more

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.GlobalPrivacyEyeButton
import com.example.ui.components.UserAvatarView
import com.example.ui.components.UserProfileDialog
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.theme.*

@Composable
fun MoreScreen(
    settingsViewModel: SettingsViewModel,
    portfolioViewModel: com.example.ui.screens.portfolio.PortfolioViewModel? = null,
    onNavigateToWealthAi: () -> Unit = {},
    onNavigateToCalculators: () -> Unit,
    onNavigateToRecurring: () -> Unit,
    onNavigateToLoans: () -> Unit = {},
    onNavigateToInsurance: () -> Unit = {},
    onNavigateToCreditCards: () -> Unit = {},
    onNavigateToCashbackRefund: () -> Unit = {},
    onNavigateToReports: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPortfolio: () -> Unit = {}
) {
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    var showProfileDialog by remember { mutableStateOf(false) }
    var showInvestmentImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = HighDensityBg,
        topBar = {
            Surface(
                color = HighDensityBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "More Options",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextPrimary
                        )
                        Text(
                            text = "SIPs, analytics, categories, profile & backup",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary
                        )
                    }

                    GlobalPrivacyEyeButton(
                        size = 36.dp,
                        iconSize = 18.dp,
                        testTag = "btn_more_privacy_eye"
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
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showProfileDialog = true }
                        .testTag("card_more_profile"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UserAvatarView(
                            userProfile = settingsState.userProfile,
                            size = 56.dp,
                            fontSize = 20.sp,
                            showEditBadge = true,
                            onClick = { showProfileDialog = true }
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = settingsState.userProfile.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityTextPrimary
                            )
                            Text(
                                text = settingsState.userProfile.tagline,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = HighDensityPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap to edit name, logo & upload photo",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = HighDensityTextSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = HighDensityTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Quick Menu Items Section
            item {
                Text(
                    text = "SECTIONS & TOOLS",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    fontWeight = FontWeight.Bold,
                    color = HighDensityTextSecondary
                )
            }

            // Wealth AI Copilot Card
            item {
                MoreNavigationCard(
                    title = "🤖 Wealth AI Copilot (Local LLM + MCP)",
                    subtitle = "100% on-device private financial AI, deterministic math tools & optional web search",
                    icon = Icons.Default.SmartToy,
                    iconBgColor = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0369A1),
                    onClick = onNavigateToWealthAi,
                    testTag = "btn_more_wealth_ai"
                )
            }

            // Import Investment Statements (Excel / CSV) Card
            item {
                MoreNavigationCard(
                    title = "Import Investment Statements (Excel / CSV)",
                    subtitle = "Import Groww, Zerodha, CAMS Mutual Funds & Stocks (.xlsx, .csv) with Folio, ISIN, Units & XIRR",
                    icon = Icons.Default.FileDownload,
                    iconBgColor = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF15803D),
                    badgeText = "NEW",
                    onClick = {
                        if (portfolioViewModel != null) {
                            showInvestmentImportDialog = true
                        } else {
                            onNavigateToPortfolio()
                        }
                    },
                    testTag = "btn_more_import_investments"
                )
            }

            // Financial Calculators Card
            item {
                MoreNavigationCard(
                    title = "Wealth & Financial Calculators",
                    subtitle = "SIP, Step-Up SIP, FIRE freedom, Lumpsum, SWP, CAGR, Loans/EMI, FD/RD, PPF & NPS planners",
                    icon = Icons.Default.Calculate,
                    iconBgColor = Color(0xFFDCFCE7),
                    iconTint = Color(0xFF16A34A),
                    onClick = onNavigateToCalculators,
                    testTag = "btn_more_calculators"
                )
            }

            // Recurring Cash Flows & SIP Management Card
            item {
                MoreNavigationCard(
                    title = "Recurring SIPs & Cash Inflows",
                    subtitle = "Monthly mutual fund SIPs, salary, meal coupons & date ranges for true organic return calculations",
                    icon = Icons.Default.Repeat,
                    iconBgColor = Color(0xFFEDE9FE),
                    iconTint = Color(0xFF7C3AED),
                    onClick = onNavigateToRecurring,
                    testTag = "btn_more_recurring"
                )
            }

            // Loans & Liabilities Management Card
            item {
                MoreNavigationCard(
                    title = "Loans & Liabilities Tracker",
                    subtitle = "Home, car, education & personal loans, EMI schedules, amortization breakdown & prepayment payoff planner",
                    icon = Icons.Default.AccountBalance,
                    iconBgColor = Color(0xFFFEE2E2),
                    iconTint = HighDensityExpenseRed,
                    onClick = onNavigateToLoans,
                    testTag = "btn_more_loans"
                )
            }

            // Insurance Shield & Policies Card
            item {
                MoreNavigationCard(
                    title = "Insurance Shield & Policies",
                    subtitle = "Term Life, Health cover, critical illness, policy numbers, nominees, sum assured & renewal due alerts",
                    icon = Icons.Default.Shield,
                    iconBgColor = Color(0xFFDCFCE7),
                    iconTint = HighDensityIncomeGreen,
                    onClick = onNavigateToInsurance,
                    testTag = "btn_more_insurance"
                )
            }

            // Credit Cards & PDF Statement Vault Card
            item {
                MoreNavigationCard(
                    title = "💳 Credit Cards & Statement Vault",
                    subtitle = "Manage cards (ICICI, HDFC, Axis, etc.), encrypted PDF statements, auto bank passwords & bill dues",
                    icon = Icons.Default.CreditCard,
                    iconBgColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFB45309),
                    badgeText = "NEW",
                    onClick = onNavigateToCreditCards,
                    testTag = "btn_more_credit_cards"
                )
            }

            // Cashback & Refunds Tracker Card
            item {
                MoreNavigationCard(
                    title = "💰 Cashback & Refunds Tracker",
                    subtitle = "Track standalone cashbacks, merchant returns, expected refund dates & recovered savings",
                    icon = Icons.Default.Savings,
                    iconBgColor = Color(0xFFECFDF5),
                    iconTint = Color(0xFF047857),
                    badgeText = "NEW",
                    onClick = onNavigateToCashbackRefund,
                    testTag = "btn_more_cashback_refund"
                )
            }

            // Reports Navigation Card
            item {
                MoreNavigationCard(
                    title = "Analytics & Performance Reports",
                    subtitle = "Monthly trajectory, CAGR metrics, allocations & two-period comparison with SIP deduction",
                    icon = Icons.Default.InsertChart,
                    iconBgColor = HighDensityPrimaryContainer,
                    iconTint = HighDensityPrimary,
                    onClick = onNavigateToReports,
                    testTag = "btn_more_reports"
                )
            }

            // Categories Management Card
            item {
                MoreNavigationCard(
                    title = "Asset Categories Management",
                    subtitle = "Mutual funds, gold, crypto, real estate, RD, bonds & custom asset groups",
                    icon = Icons.Default.Category,
                    iconBgColor = Color(0xFFF3E8FF),
                    iconTint = Color(0xFF7E22CE),
                    onClick = onNavigateToCategories,
                    testTag = "btn_more_categories"
                )
            }

            // Settings & Data Management Card
            item {
                MoreNavigationCard(
                    title = "Settings & Data Controls",
                    subtitle = "Load sample demo portfolio, restore default categories, clear data & isolation rules",
                    icon = Icons.Default.Settings,
                    iconBgColor = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    onClick = onNavigateToSettings,
                    testTag = "btn_more_settings"
                )
            }

            // Built By & Developer Details Card
            item {
                val uriHandler = LocalUriHandler.current

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .testTag("card_built_by"),
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

            // App info
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Wealth & Portfolio Tracker",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = HighDensityTextSecondary
                        )
                        Text(
                            text = "Built by Prakash Seervi • v1.0.0",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = HighDensityTextSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        UserProfileDialog(
            userProfile = settingsState.userProfile,
            onDismiss = { showProfileDialog = false },
            onSaveProfile = { name, email, tagline, targetNetWorth, colorIndex, dob ->
                settingsViewModel.updateUserProfile(name, email, tagline, targetNetWorth, colorIndex, dob)
            },
            onImageSelected = { uri ->
                settingsViewModel.saveProfileImage(uri)
            },
            onRemoveImage = {
                settingsViewModel.removeProfileImage()
            }
        )
    }

    if (showInvestmentImportDialog && portfolioViewModel != null) {
        com.example.ui.screens.portfolio.InvestmentImportDialog(
            onDismiss = { showInvestmentImportDialog = false },
            onImportConfirmed = { statement, targetSnapshotId, updateExisting ->
                portfolioViewModel.importInvestmentStatement(statement, targetSnapshotId, updateExisting)
            }
        )
    }
}

@Composable
private fun MoreNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    badgeText: String? = null,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconBgColor,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = HighDensityTextPrimary
                    )
                    if (badgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = iconTint.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = iconTint,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = HighDensityTextSecondary,
                    maxLines = 2
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
