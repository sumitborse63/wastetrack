package com.sktech.wastetrack.ui.navigation

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sktech.wastetrack.R

import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import com.sktech.wastetrack.domain.model.UserRole

data class BottomNavItem(
    val labelRes: Int,
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val supervisorNavItems = listOf(
    BottomNavItem(R.string.dashboard, Screen.Dashboard.route, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(R.string.log_scrap, Screen.ScrapLog.route, Icons.Filled.DeleteSweep, Icons.Outlined.DeleteSweep),
    BottomNavItem(R.string.transfers, Screen.TransferList.route, Icons.Filled.LocalShipping, Icons.Outlined.LocalShipping),
    BottomNavItem(R.string.nav_bids, Screen.BidMarket.route, Icons.Filled.Storefront, Icons.Outlined.Storefront),
    BottomNavItem(R.string.nav_compliance, Screen.Compliance.route, Icons.Filled.Description, Icons.Outlined.Description),
)

val recyclerNavItems = listOf(
    BottomNavItem(R.string.dashboard, Screen.Dashboard.route, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(R.string.transfers, Screen.FleetTracker.route, Icons.Filled.Navigation, Icons.Outlined.Navigation),
    BottomNavItem(R.string.nav_bids, Screen.BidMarket.route, Icons.Filled.Storefront, Icons.Outlined.Storefront),
    BottomNavItem(R.string.nav_compliance, Screen.Compliance.route, Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem(R.string.settings, Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings),
)

val driverNavItems = listOf(
    BottomNavItem(R.string.dashboard, Screen.Dashboard.route, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(R.string.transfers, Screen.FleetTracker.route, Icons.Filled.Navigation, Icons.Outlined.Navigation),
    BottomNavItem(R.string.scan_qr, Screen.QRScan.route, Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    BottomNavItem(R.string.settings, Screen.Settings.route, Icons.Filled.Settings, Icons.Outlined.Settings),
)

val adminNavItems = listOf(
    BottomNavItem(R.string.dashboard, Screen.Dashboard.route, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    BottomNavItem(R.string.bin_monitor, Screen.BinMonitor.route, Icons.Filled.Delete, Icons.Outlined.Delete),
    BottomNavItem(R.string.nav_bids, Screen.BidMarket.route, Icons.Filled.Storefront, Icons.Outlined.Storefront),
    BottomNavItem(R.string.nav_compliance, Screen.Compliance.route, Icons.Filled.Description, Icons.Outlined.Description),
    BottomNavItem(R.string.analytics, Screen.Analytics.route, Icons.Filled.BarChart, Icons.Outlined.BarChart),
)

val bottomNavItems = supervisorNavItems

fun getBottomNavItemsForRole(role: UserRole?): List<BottomNavItem> {
    return when (role) {
        UserRole.RECYCLER -> recyclerNavItems
        UserRole.DRIVER -> driverNavItems
        UserRole.ADMIN -> adminNavItems
        else -> supervisorNavItems
    }
}

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    items: List<BottomNavItem> = bottomNavItems
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val labelText = stringResource(item.labelRes)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = labelText,
                        modifier = Modifier.size(26.dp)
                    )
                },
                label = {
                    Text(
                        text = labelText,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

