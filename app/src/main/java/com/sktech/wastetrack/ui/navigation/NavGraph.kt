package com.sktech.wastetrack.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.sktech.wastetrack.ui.screens.bid.BidMarketScreen
import com.sktech.wastetrack.ui.screens.bin.BinMonitorScreen
import com.sktech.wastetrack.ui.screens.compliance.ComplianceScreen
import com.sktech.wastetrack.ui.screens.dashboard.DashboardScreen
import com.sktech.wastetrack.ui.screens.dashboard.LedgerScanScreen
import com.sktech.wastetrack.ui.screens.scrap.ScrapLogScreen
import com.sktech.wastetrack.ui.screens.settings.SettingsScreen
import com.sktech.wastetrack.ui.screens.transfer.TransferScreen
import com.sktech.wastetrack.ui.screens.scrap.ScrapClassifyScreen
import com.sktech.wastetrack.ui.screens.analytics.AnalyticsScreen
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.auth.LoginScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    innerPadding: PaddingValues,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(innerPadding)
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToScrapLog = { navController.navigate(Screen.ScrapLog.route) },
                onNavigateToTransfer = { navController.navigate(Screen.TransferList.route) },
                onNavigateToQRScan = { navController.navigate(Screen.QRScan.route) },
                onNavigateToBids = { navController.navigate(Screen.BidMarket.route) },
                onNavigateToCompliance = { navController.navigate(Screen.Compliance.route) },
                onNavigateToBinMonitor = { navController.navigate(Screen.BinMonitor.route) },
                onNavigateToLedgerScan = { navController.navigate(Screen.LedgerScan.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.ScrapLog.route) { backStackEntry ->
            val savedStateHandle = backStackEntry.savedStateHandle
            val classifiedCategory = savedStateHandle.get<String>("classifiedCategory")
            
            val viewModel: com.sktech.wastetrack.ui.screens.scrap.ScrapLogViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            
            // Apply classified category if returned
            androidx.compose.runtime.LaunchedEffect(classifiedCategory) {
                if (classifiedCategory != null) {
                    try {
                        val cat = ScrapCategory.valueOf(classifiedCategory)
                        viewModel.onCategorySelected(cat)
                        savedStateHandle.remove<String>("classifiedCategory")
                    } catch (e: Exception) {}
                }
            }

            ScrapLogScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassify = { navController.navigate(Screen.ScrapClassify.route) },
                viewModel = viewModel
            )
        }

        composable(Screen.ScrapClassify.route) {
            ScrapClassifyScreen(
                onNavigateBack = { navController.popBackStack() },
                onClassificationComplete = { category ->
                    navController.previousBackStackEntry?.savedStateHandle?.set("classifiedCategory", category.name)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.TransferList.route) {
            TransferScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BidMarket.route) {
            BidMarketScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Compliance.route) {
            ComplianceScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.BinMonitor.route) {
            BinMonitorScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.LedgerScan.route) {
            LedgerScanScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QRScan.route) {
            TransferScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
