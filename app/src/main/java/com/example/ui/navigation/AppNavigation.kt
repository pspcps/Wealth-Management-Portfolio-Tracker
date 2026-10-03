package com.example.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.categories.CategoriesScreen
import com.example.ui.screens.categories.CategoriesViewModel
import com.example.ui.screens.calculators.CalculatorsScreen
import com.example.ui.screens.ai.WealthAiScreen
import com.example.ui.screens.ai.WealthAiViewModel
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.dashboard.DashboardViewModel
import com.example.ui.screens.expenses.ExpensesScreen
import com.example.ui.screens.expenses.ExpensesViewModel
import com.example.ui.screens.monthlyupdate.MonthlyUpdateScreen
import com.example.ui.screens.monthlyupdate.MonthlyUpdateViewModel
import com.example.ui.screens.more.MoreScreen
import com.example.ui.screens.portfolio.PortfolioScreen
import com.example.ui.screens.portfolio.PortfolioViewModel
import com.example.ui.screens.recurring.RecurringCashFlowScreen
import com.example.ui.screens.recurring.RecurringCashFlowViewModel
import com.example.ui.screens.reports.ReportsScreen
import com.example.ui.screens.reports.ReportsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.loans.LoansScreen
import com.example.ui.screens.loans.LoansViewModel
import com.example.ui.screens.insurance.InsuranceScreen
import com.example.ui.screens.insurance.InsuranceViewModel
import com.example.ui.screens.creditcards.CreditCardsScreen
import com.example.ui.screens.creditcards.CreditCardsViewModel
import com.example.ui.screens.cashback.CashbackRefundScreen
import com.example.ui.screens.cashback.CashbackRefundViewModel
import com.example.ui.theme.*

import com.example.ui.screens.security.AppLockScreen
import com.example.util.SecurityManager
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.AppDatabase
import com.example.data.repository.PortfolioRepository
import com.example.data.repository.UserProfileRepository
import com.example.ui.components.AutoBackupPermissionPromptDialog
import com.example.util.AutoBackupManager
import com.example.util.FinancialPrivacyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String, val title: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Dashboard : Screen("dashboard", "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    object Portfolio : Screen("portfolio", "Holdings", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    object MonthlyUpdate : Screen("monthly_update", "Entry", Icons.Filled.EditCalendar, Icons.Outlined.EditCalendar)
    object Expenses : Screen("expenses", "Expenses", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object More : Screen("more", "More", Icons.Filled.GridView, Icons.Outlined.GridView)
    
    // Sub-screens accessible from More & Dashboard
    object WealthAi : Screen("wealth_ai", "Wealth AI", Icons.Filled.SmartToy, Icons.Outlined.SmartToy)
    object Calculators : Screen("calculators", "Calculators", Icons.Filled.Calculate, Icons.Outlined.Calculate)
    object RecurringFlows : Screen("recurring_flows", "SIPs & Recurring", Icons.Filled.Repeat, Icons.Outlined.Repeat)
    object Loans : Screen("loans", "Loans", Icons.Filled.AccountBalance, Icons.Outlined.AccountBalance)
    object Insurance : Screen("insurance", "Insurance", Icons.Filled.Shield, Icons.Outlined.Shield)
    object CreditCards : Screen("credit_cards", "Cards", Icons.Filled.CreditCard, Icons.Outlined.CreditCard)
    object CashbackRefund : Screen("cashback_refund", "Cashback", Icons.Filled.Savings, Icons.Outlined.Savings)
    object Reports : Screen("reports", "Reports", Icons.Filled.InsertChart, Icons.Outlined.InsertChart)
    object Categories : Screen("categories", "Categories", Icons.Filled.Category, Icons.Outlined.Category)
    object Settings : Screen("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val primaryBottomNavScreens = listOf(
    Screen.Dashboard,
    Screen.Portfolio,
    Screen.MonthlyUpdate,
    Screen.Expenses,
    Screen.CreditCards,
    Screen.CashbackRefund,
    Screen.Loans,
    Screen.Insurance,
    Screen.More
)

@Composable
fun MainAppContainer() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        FinancialPrivacyManager.init(context)
    }

    val securityManager = remember { SecurityManager.getInstance(context) }
    var isUnlocked by remember { mutableStateOf(!securityManager.isAppLockEnabled) }

    if (!isUnlocked && securityManager.isAppLockEnabled) {
        AppLockScreen(
            securityManager = securityManager,
            onUnlocked = { isUnlocked = true }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // ViewModels scoped to application
    val dashboardViewModel: DashboardViewModel = viewModel()
    val monthlyUpdateViewModel: MonthlyUpdateViewModel = viewModel()
    val portfolioViewModel: PortfolioViewModel = viewModel()
    val expensesViewModel: ExpensesViewModel = viewModel()
    val reportsViewModel: ReportsViewModel = viewModel()
    val categoriesViewModel: CategoriesViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val recurringViewModel: RecurringCashFlowViewModel = viewModel()
    val wealthAiViewModel: WealthAiViewModel = viewModel()
    val loansViewModel: LoansViewModel = viewModel()
    val insuranceViewModel: InsuranceViewModel = viewModel()
    val creditCardsViewModel: CreditCardsViewModel = viewModel()
    val cashbackRefundViewModel: CashbackRefundViewModel = viewModel()
    val coroutineScope = rememberCoroutineScope()

    // Daily Auto-Backup: Prompt user on first run & trigger background daily backup
    var showAutoBackupPrompt by remember {
        mutableStateOf(
            !AutoBackupManager.isAutoBackupConfigured(context) ||
            (!AutoBackupManager.hasRequiredPermissions(context) && AutoBackupManager.isAutoBackupEnabled(context) && AutoBackupManager.getLastBackupTimestamp(context) == 0L)
        )
    }

    LaunchedEffect(isUnlocked) {
        if (isUnlocked && AutoBackupManager.isAutoBackupEnabled(context)) {
            withContext(Dispatchers.IO) {
                try {
                    val db = AppDatabase.getInstance(context)
                    AutoBackupManager.checkAndPerformDailyBackup(
                        context = context.applicationContext,
                        repository = PortfolioRepository(db),
                        profileRepository = UserProfileRepository(context.applicationContext),
                        force = false
                    )
                } catch (_: Exception) {}
            }
        }
    }

    if (showAutoBackupPrompt) {
        AutoBackupPermissionPromptDialog(
            onDismiss = { showAutoBackupPrompt = false },
            onEnabled = {
                showAutoBackupPrompt = false
                settingsViewModel.setDailyAutoBackupEnabled(true)
                coroutineScope.launch {
                    try {
                        val db = AppDatabase.getInstance(context)
                        val result = withContext(Dispatchers.IO) {
                            AutoBackupManager.checkAndPerformDailyBackup(
                                context = context.applicationContext,
                                repository = PortfolioRepository(db),
                                profileRepository = UserProfileRepository(context.applicationContext),
                                force = true
                            )
                        }
                        if (result.success) {
                            android.widget.Toast.makeText(
                                context,
                                "✓ Daily backup saved to Documents / ${AutoBackupManager.FOLDER_NAME}",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (_: Exception) {}
                }
            }
        )
    }

    val isMainTab = currentRoute in primaryBottomNavScreens.map { it.route }

    val navScrollState = rememberScrollState()
    val selectedIndex = primaryBottomNavScreens.indexOfFirst { screen ->
        if (screen == Screen.More) {
            currentRoute in listOf(
                Screen.More.route,
                Screen.Calculators.route,
                Screen.RecurringFlows.route,
                Screen.Reports.route,
                Screen.Categories.route,
                Screen.Settings.route
            )
        } else {
            currentRoute == screen.route
        }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex >= 0) {
            val density = context.resources.displayMetrics.density
            val itemWidthPx = (72 * density).toInt()
            val targetScroll = (selectedIndex * itemWidthPx - (120 * density).toInt()).coerceAtLeast(0)
            navScrollState.animateScrollTo(targetScroll)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
        floatingActionButton = {
            if (currentRoute != Screen.WealthAi.route) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(Screen.WealthAi.route) {
                            launchSingleTop = true
                        }
                    },
                    containerColor = Color(0xFF0F766E),
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 6.dp),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("fab_wealth_ai_copilot")
                ) {
                    Text("🤖", fontSize = 20.sp)
                }
            }
        },
        bottomBar = {
            if (isMainTab) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .horizontalScroll(navScrollState)
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        primaryBottomNavScreens.forEach { screen ->
                            val isSelected = when (screen) {
                                Screen.More -> currentRoute in listOf(
                                    Screen.More.route,
                                    Screen.Calculators.route,
                                    Screen.RecurringFlows.route,
                                    Screen.Reports.route,
                                    Screen.Categories.route,
                                    Screen.Settings.route
                                )
                                else -> currentRoute == screen.route
                            }

                            Box(
                                modifier = Modifier
                                    .width(68.dp)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                    .testTag("nav_item_${screen.route}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(if (isSelected) HighDensityPrimaryContainer else Color.Transparent)
                                            .padding(horizontal = 12.dp, vertical = 3.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title,
                                            tint = if (isSelected) HighDensityOnPrimaryContainer else Color(0xFF64748B),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = 10.5.sp,
                                            letterSpacing = (-0.2).sp
                                        ),
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) HighDensityPrimary else Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToMonthlyUpdate = {
                        navController.navigate(Screen.MonthlyUpdate.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToExpenses = {
                        navController.navigate(Screen.Expenses.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToPortfolio = {
                        navController.navigate(Screen.Portfolio.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRecurring = {
                        navController.navigate(Screen.RecurringFlows.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCalculators = {
                        navController.navigate(Screen.Calculators.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToWealthAi = {
                        navController.navigate(Screen.WealthAi.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLoans = {
                        navController.navigate(Screen.Loans.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToInsurance = {
                        navController.navigate(Screen.Insurance.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCashbackRefund = {
                        navController.navigate(Screen.CashbackRefund.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Portfolio.route) {
                PortfolioScreen(
                    viewModel = portfolioViewModel,
                    onNavigateToMonthlyUpdate = {
                        navController.navigate(Screen.MonthlyUpdate.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.MonthlyUpdate.route) {
                MonthlyUpdateScreen(
                    viewModel = monthlyUpdateViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }

            composable(Screen.Expenses.route) {
                ExpensesScreen(
                    viewModel = expensesViewModel
                )
            }

            composable(Screen.More.route) {
                MoreScreen(
                    settingsViewModel = settingsViewModel,
                    onNavigateToWealthAi = {
                        navController.navigate(Screen.WealthAi.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCalculators = {
                        navController.navigate(Screen.Calculators.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToRecurring = {
                        navController.navigate(Screen.RecurringFlows.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToLoans = {
                        navController.navigate(Screen.Loans.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToInsurance = {
                        navController.navigate(Screen.Insurance.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToReports = {
                        navController.navigate(Screen.Reports.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCreditCards = {
                        navController.navigate(Screen.CreditCards.route) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToCashbackRefund = {
                        navController.navigate(Screen.CashbackRefund.route) {
                            launchSingleTop = true
                        }
                    },
                    portfolioViewModel = portfolioViewModel,
                    onNavigateToPortfolio = {
                        navController.navigate(Screen.Portfolio.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.WealthAi.route) {
                WealthAiScreen(
                    viewModel = wealthAiViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable(Screen.Calculators.route) {
                CalculatorsScreen(
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.More.route)
                        }
                    }
                )
            }

            composable(Screen.RecurringFlows.route) {
                RecurringCashFlowScreen(
                    viewModel = recurringViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.More.route)
                        }
                    }
                )
            }

            composable(Screen.Loans.route) {
                LoansScreen(
                    viewModel = loansViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route)
                        }
                    },
                    onNavigateToCalculators = {
                        navController.navigate(Screen.Calculators.route) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Insurance.route) {
                InsuranceScreen(
                    viewModel = insuranceViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable(Screen.Reports.route) {
                ReportsScreen(
                    viewModel = reportsViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.More.route)
                        }
                    }
                )
            }

            composable(Screen.Categories.route) {
                CategoriesScreen(
                    viewModel = categoriesViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.More.route)
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.More.route)
                        }
                    }
                )
            }

            composable(Screen.CreditCards.route) {
                CreditCardsScreen(
                    viewModel = creditCardsViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route)
                        }
                    }
                )
            }

            composable(Screen.CashbackRefund.route) {
                CashbackRefundScreen(
                    viewModel = cashbackRefundViewModel,
                    onNavigateBack = {
                        if (!navController.popBackStack()) {
                            navController.navigate(Screen.Dashboard.route)
                        }
                    }
                )
            }
        }
    }
}
