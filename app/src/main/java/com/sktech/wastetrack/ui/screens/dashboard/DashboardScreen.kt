package com.sktech.wastetrack.ui.screens.dashboard

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToScrapLog: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToQRScan: () -> Unit,
    onNavigateToBids: () -> Unit,
    onNavigateToCompliance: () -> Unit,
    onNavigateToBinMonitor: () -> Unit,
    onNavigateToLedgerScan: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with sync status
        DashboardHeader(
            isOnline = state.isOnline,
            pendingSyncCount = state.pendingSyncCount,
            onSettingsClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Stat Cards Row
        Text(
            text = "Today's Overview",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatCard(
                    title = "Scrap Logged",
                    value = "${state.todayScrapCount}",
                    subtitle = "${String.format("%.1f", state.todayWeightKg)} kg",
                    icon = Icons.Filled.DeleteSweep,
                    gradientColors = listOf(IndustrialGreen, IndustrialGreenLight)
                )
            }
            item {
                StatCard(
                    title = "Transfers",
                    value = "${state.pendingTransfers}",
                    subtitle = "Pending",
                    icon = Icons.Filled.LocalShipping,
                    gradientColors = listOf(Teal, SyncBlue)
                )
            }
            item {
                StatCard(
                    title = "Bin Alerts",
                    value = "${state.binAlerts}",
                    subtitle = "Near full",
                    icon = Icons.Filled.Warning,
                    gradientColors = if (state.binAlerts > 0)
                        listOf(SafetyOrange, AlertRed)
                    else
                        listOf(SteelGray, GraphiteLight)
                )
            }
            item {
                StatCard(
                    title = "Certificates",
                    value = "${state.certificateCount}",
                    subtitle = "Generated",
                    icon = Icons.Filled.Description,
                    gradientColors = listOf(Gold, SafetyOrangeLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionCard(
                    title = "Log Scrap",
                    subtitle = "AI classify & weigh",
                    icon = Icons.Outlined.AddCircle,
                    color = IndustrialGreenLight,
                    onClick = onNavigateToScrapLog,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Scan QR",
                    subtitle = "Verify handshake",
                    icon = Icons.Outlined.QrCodeScanner,
                    color = Teal,
                    onClick = onNavigateToQRScan,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionCard(
                    title = "Transfer",
                    subtitle = "New dispatch",
                    icon = Icons.Outlined.LocalShipping,
                    color = SyncBlue,
                    onClick = onNavigateToTransfer,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "Bin Monitor",
                    subtitle = "Fill levels",
                    icon = Icons.Outlined.Inventory2,
                    color = SafetyOrange,
                    onClick = onNavigateToBinMonitor,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionCard(
                    title = "Scan Ledger",
                    subtitle = "OCR digitize",
                    icon = Icons.Outlined.DocumentScanner,
                    color = Gold,
                    onClick = onNavigateToLedgerScan,
                    modifier = Modifier.weight(1f)
                )
                QuickActionCard(
                    title = "ESG Certs",
                    subtitle = "View & export",
                    icon = Icons.Outlined.VerifiedUser,
                    color = IndustrialGreenLight,
                    onClick = onNavigateToCompliance,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                QuickActionCard(
                    title = "Analytics",
                    subtitle = "Enterprise charts",
                    icon = Icons.Outlined.Analytics,
                    color = SyncBlue,
                    onClick = onNavigateToAnalytics,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recent Activity
        if (state.recentEntries.isNotEmpty()) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.recentEntries.forEach { entry ->
                    RecentActivityCard(entry = entry)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun RecentActivityCard(entry: com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val category = try {
                com.sktech.wastetrack.domain.model.ScrapCategory.valueOf(entry.category)
            } catch (e: Exception) {
                com.sktech.wastetrack.domain.model.ScrapCategory.OTHER
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = IndustrialGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(category.icon, style = MaterialTheme.typography.titleMedium)
                    }
                }
                Column {
                    Text(category.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        com.sktech.wastetrack.util.DateUtils.formatTime(entry.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${entry.weightKg} kg",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun DashboardHeader(
    isOnline: Boolean,
    pendingSyncCount: Int,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, bottom = 16.dp, start = 20.dp, end = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WasteTrack",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ambad MIDC Pilot",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sync Status
                    SyncStatusChip(isOnline = isOnline, pendingCount = pendingSyncCount)
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusChip(isOnline: Boolean, pendingCount: Int) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (isOnline) IndustrialGreen.copy(alpha = 0.15f) else SafetyOrange.copy(alpha = 0.15f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (isOnline) IndustrialGreenLight else SafetyOrange
            )
            Text(
                text = if (isOnline) "Synced" else "$pendingCount pending",
                style = MaterialTheme.typography.labelSmall,
                color = if (isOnline) IndustrialGreenLight else SafetyOrange
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradientColors),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = color.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
