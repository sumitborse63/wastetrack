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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Recycling,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = state.currentUser?.organizationName?.ifBlank { stringResource(R.string.app_name) } ?: stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = if (!state.currentUser?.industrialArea.isNullOrBlank())
                                stringResource(R.string.unit_code_format, state.currentUser?.industrialArea.orEmpty(), state.currentUser?.factoryId.orEmpty().take(6).uppercase())
                            else
                                stringResource(R.string.enterprise_floor_terminal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (state.isOnline) EmeraldContainer else AlertRedContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (state.isOnline) Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = if (state.isOnline) EmeraldPrimary else AlertRed
                            )
                            Text(
                                text = if (state.isOnline) stringResource(R.string.synced_label) else stringResource(R.string.pending_sync_format, state.pendingSyncCount),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isOnline) EmeraldPrimary else AlertRed
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

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
                        subtitle = stringResource(R.string.audit_ready),
                        icon = Icons.Outlined.FactCheck,
                        accentColor = Gold,
                        containerColor = SafetyOrangeContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions Section Header
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bento Grid: 2 rows of 3 columns
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernQuickActionButton(
                        title = stringResource(R.string.log_scrap),
                        subtitle = stringResource(R.string.record_scrap_weight_sub),
                        icon = Icons.Outlined.AddCircleOutline,
                        accentColor = EmeraldPrimary,
                        containerColor = EmeraldContainer,
                        onClick = onNavigateToScrapLog,
                        modifier = Modifier.weight(1f)
                    )
                    ModernQuickActionButton(
                        title = stringResource(R.string.live_auctions),
                        subtitle = stringResource(R.string.b2b_lot_auctions_sub),
                        icon = Icons.Outlined.Storefront,
                        accentColor = Teal,
                        containerColor = TealContainer,
                        onClick = onNavigateToBids,
                        modifier = Modifier.weight(1f)
                    )
                    ModernQuickActionButton(
                        title = stringResource(R.string.bin_telemetry_title),
                        subtitle = stringResource(R.string.iot_level_sensors_sub),
                        icon = Icons.Outlined.Sensors,
                        accentColor = Gold,
                        containerColor = SafetyOrangeContainer,
                        onClick = onNavigateToBinMonitor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernQuickActionButton(
                        title = stringResource(R.string.gate_pass_title),
                        subtitle = stringResource(R.string.qr_handshake_sub),
                        icon = Icons.Outlined.QrCodeScanner,
                        accentColor = EmeraldPrimary,
                        containerColor = EmeraldContainer,
                        onClick = onNavigateToQRScan,
                        modifier = Modifier.weight(1f)
                    )
                    ModernQuickActionButton(
                        title = stringResource(R.string.digitize_ledger),
                        subtitle = stringResource(R.string.camera_ocr_scanner_sub),
                        icon = Icons.Outlined.DocumentScanner,
                        accentColor = Teal,
                        containerColor = TealContainer,
                        onClick = onNavigateToLedgerScan,
                        modifier = Modifier.weight(1f)
                    )
                    ModernQuickActionButton(
                        title = stringResource(R.string.esg_reports_title),
                        subtitle = stringResource(R.string.mpcb_manifests_sub),
                        icon = Icons.Outlined.Assessment,
                        accentColor = SyncBlue,
                        containerColor = TealContainer,
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Activity Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.recent_shop_floor_entries),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                TextButton(onClick = onNavigateToScrapLog) {
                    Text(
                        stringResource(R.string.log_entry_btn),
                        color = EmeraldPrimary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (state.recentEntries.isEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.Inventory2,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.no_scrap_logged_today),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.no_scrap_logged_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.recentEntries.take(5).forEach { entry ->
                        ModernScrapEntryRow(entry = entry)
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
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
            .width(155.dp)
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
private fun ModernQuickActionButton(
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
            .height(105.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
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
private fun ModernScrapEntryRow(entry: ScrapEntryEntity) {
    val category = runCatching { ScrapCategory.valueOf(entry.category) }.getOrDefault(ScrapCategory.OTHER)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(category.icon, fontSize = 18.sp)
                    }
                }

                Column {
                    Text(
                        text = "${stringResource(category.nameRes)} · ${entry.weightKg} kg",
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
                    text = entry.syncStatus,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary
                )
            }
        }
    }
}
