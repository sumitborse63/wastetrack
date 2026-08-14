package com.sktech.wastetrack.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

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
        // Modern Minimalist Top Header
        DashboardHeader(
            user = state.currentUser,
            isOnline = state.isOnline,
            pendingSyncCount = state.pendingSyncCount,
            onSettingsClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Performance Overview Title Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dashboard),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.ambad_pilot_subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = if (state.isOnline) EmeraldPrimary else SafetyOrange,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Stat KPI Cards Horizontal Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ModernStatCard(
                    title = stringResource(R.string.today_scrap),
                    value = "${state.todayScrapCount}",
                    subtitle = "${String.format("%.1f", state.todayWeightKg)} kg",
                    icon = Icons.Outlined.DeleteSweep,
                    accentColor = EmeraldPrimary,
                    containerColor = EmeraldContainer
                )
            }
            item {
                ModernStatCard(
                    title = stringResource(R.string.transfers),
                    value = "${state.pendingTransfers}",
                    subtitle = stringResource(R.string.pending_sub),
                    icon = Icons.Outlined.LocalShipping,
                    accentColor = Teal,
                    containerColor = TealContainer
                )
            }
            item {
                ModernStatCard(
                    title = stringResource(R.string.bin_alerts),
                    value = "${state.binAlerts}",
                    subtitle = if (state.binAlerts > 0) stringResource(R.string.overflow_alert) else stringResource(R.string.normal_capacity),
                    icon = Icons.Outlined.Warning,
                    accentColor = if (state.binAlerts > 0) AlertRed else EmeraldPrimary,
                    containerColor = if (state.binAlerts > 0) AlertRedContainer else EmeraldContainer
                )
            }
            item {
                ModernStatCard(
                    title = stringResource(R.string.certificates),
                    value = "${state.certificateCount}",
                    subtitle = stringResource(R.string.generated_sub),
                    icon = Icons.Outlined.Verified,
                    accentColor = Gold,
                    containerColor = SafetyOrangeContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bento Quick Actions Grid
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModernQuickActionCard(
                    title = stringResource(R.string.log_scrap),
                    subtitle = stringResource(R.string.ai_classify_subtitle),
                    icon = Icons.Outlined.AddCircle,
                    accentColor = EmeraldPrimary,
                    containerColor = EmeraldContainer,
                    onClick = onNavigateToScrapLog,
                    modifier = Modifier.weight(1f)
                )
                ModernQuickActionCard(
                    title = stringResource(R.string.scan_qr),
                    subtitle = stringResource(R.string.verify_handshake_sub),
                    icon = Icons.Outlined.QrCodeScanner,
                    accentColor = Teal,
                    containerColor = TealContainer,
                    onClick = onNavigateToQRScan,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModernQuickActionCard(
                    title = stringResource(R.string.transfer_action),
                    subtitle = stringResource(R.string.new_dispatch_sub),
                    icon = Icons.Outlined.LocalShipping,
                    accentColor = SyncBlue,
                    containerColor = TealContainer,
                    onClick = onNavigateToTransfer,
                    modifier = Modifier.weight(1f)
                )
                ModernQuickActionCard(
                    title = stringResource(R.string.bin_monitor),
                    subtitle = stringResource(R.string.bin_monitor_sub),
                    icon = Icons.Outlined.Sensors,
                    accentColor = SafetyOrange,
                    containerColor = SafetyOrangeContainer,
                    onClick = onNavigateToBinMonitor,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ModernQuickActionCard(
                    title = stringResource(R.string.scan_ledger),
                    subtitle = stringResource(R.string.scan_ledger_sub),
                    icon = Icons.Outlined.DocumentScanner,
                    accentColor = Gold,
                    containerColor = SafetyOrangeContainer,
                    onClick = onNavigateToLedgerScan,
                    modifier = Modifier.weight(1f)
                )
                ModernQuickActionCard(
                    title = stringResource(R.string.compliance),
                    subtitle = stringResource(R.string.esg_certs_sub),
                    icon = Icons.Outlined.FactCheck,
                    accentColor = PurpleViolet,
                    containerColor = TealContainer,
                    onClick = onNavigateToCompliance,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Scrap Activity Timeline
        if (state.recentEntries.isNotEmpty()) {
            Text(
                text = stringResource(R.string.recent_activity),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.recentEntries.forEach { entry ->
                    ModernRecentActivityCard(entry = entry)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ModernStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(115.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ModernQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(90.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.Center) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun ModernRecentActivityCard(entry: com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity) {
    val category = runCatching { ScrapCategory.valueOf(entry.category) }.getOrDefault(ScrapCategory.OTHER)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = category.color().copy(alpha = 0.15f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(category.icon, fontSize = 18.sp)
                    }
                }
                Column {
                    Text(
                        text = if (entry.subCategory.isNotBlank()) "${entry.subCategory} (${stringResource(category.nameRes)})" else stringResource(category.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = DateUtils.formatTime(entry.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = EmeraldContainer
            ) {
                Text(
                    text = "${entry.weightKg} kg",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    user: com.sktech.wastetrack.domain.model.User?,
    isOnline: Boolean,
    pendingSyncCount: Int,
    onSettingsClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.Recycling,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = user?.organizationName?.ifBlank { stringResource(R.string.app_name) } ?: stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (!user?.industrialArea.isNullOrBlank()) "${user?.industrialArea} · Unit #${user?.factoryId.orEmpty().take(6).uppercase()}" else "Enterprise Floor Terminal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Online/Sync Status Pill
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (isOnline) EmeraldContainer else AlertRedContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isOnline) EmeraldPrimary else AlertRed
                        )
                        Text(
                            text = if (isOnline) stringResource(R.string.synced_label) else stringResource(R.string.pending_sync_format, pendingSyncCount),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isOnline) EmeraldPrimary else AlertRed
                        )
                    }
                }

                IconButton(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
