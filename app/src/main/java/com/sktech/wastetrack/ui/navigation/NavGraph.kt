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
import com.sktech.wastetrack.ui.screens.scrap.ScrapLogScreen
import com.sktech.wastetrack.ui.screens.settings.SettingsScreen
import com.sktech.wastetrack.ui.screens.transfer.TransferScreen

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
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToScrapLog = { navController.navigate(Screen.ScrapLog.route) },
                onNavigateToTransfer = { navController.navigate(Screen.TransferList.route) },
                onNavigateToQRScan = { navController.navigate(Screen.QRScan.route) },
                onNavigateToBids = { navController.navigate(Screen.BidMarket.route) },
                onNavigateToCompliance = { navController.navigate(Screen.Compliance.route) },
                onNavigateToBinMonitor = { navController.navigate(Screen.BinMonitor.route) },
                onNavigateToLedgerScan = { /* TODO: Ledger scan */ },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.ScrapLog.route) {
            ScrapLogScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClassify = { /* TODO: ML classify screen */ }
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

        composable(Screen.Settings.route) {
            SettingsScreen(
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
