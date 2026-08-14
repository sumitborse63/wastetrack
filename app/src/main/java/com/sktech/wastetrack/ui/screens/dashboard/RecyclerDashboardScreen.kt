package com.sktech.wastetrack.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecyclerDashboardScreen(
    onNavigateToBids: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToCompliance: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFleet: () -> Unit,
    viewModel: RecyclerDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showPickupDialog by remember { mutableStateOf<BidRequestEntity?>(null) }
    var showVerifyDialog by remember { mutableStateOf<TransferEntity?>(null) }

    showPickupDialog?.let { request ->
        InitiatePickupDialog(
            request = request,
            onConfirm = { vehicleNo ->
                viewModel.initiatePickup(request.id, vehicleNo)
                showPickupDialog = null
            },
            onDismiss = { showPickupDialog = null }
        )
    }

    showVerifyDialog?.let { transfer ->
        VerifyWeightDialog(
            transfer = transfer,
            onConfirm = { weight ->
                viewModel.verifyReceivedWeight(transfer.id, weight)
                showVerifyDialog = null
            },
            onDismiss = { showVerifyDialog = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Modern Recycler Header
        RecyclerHeader(
            user = state.currentUser,
            onSettingsClick = onNavigateToSettings
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Message Banners
        state.successMessage?.let { msg ->
            Surface(
                color = EmeraldContainer,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, null, tint = EmeraldPrimary)
                    Text(msg, style = MaterialTheme.typography.bodyMedium, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(3000)
                viewModel.clearMessages()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats Overview Title
        Text(
            text = "Recycling Procurement Metrics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Metrics Carousel
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                RecyclerStatCard(
                    title = "Won Auctions",
                    value = "${state.wonAuctions.size}",
                    subtitle = "Action required",
                    icon = Icons.Outlined.LocalShipping,
                    accentColor = EmeraldPrimary,
                    containerColor = EmeraldContainer
                )
            }
            item {
                RecyclerStatCard(
                    title = "In Transit",
                    value = "${state.incomingShipments.size}",
                    subtitle = "Trucks on route",
                    icon = Icons.Outlined.Navigation,
                    accentColor = Teal,
                    containerColor = TealContainer
                )
            }
            item {
                RecyclerStatCard(
                    title = "EPR Certified",
                    value = "${state.certificateCount}",
                    subtitle = "Verified shipments",
                    icon = Icons.Outlined.FactCheck,
                    accentColor = SyncBlue,
                    containerColor = TealContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Actions Grid
        Text(
            text = stringResource(R.string.quick_actions),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RecyclerQuickActionCard(
                title = "Bid Market",
                subtitle = "Browse factory lots",
                icon = Icons.Outlined.Storefront,
                accentColor = EmeraldPrimary,
                containerColor = EmeraldContainer,
                onClick = onNavigateToBids,
                modifier = Modifier.weight(1f)
            )
            RecyclerQuickActionCard(
                title = "Fleet Tracker",
                subtitle = "Live inbound trucks",
                icon = Icons.Outlined.LocalShipping,
                accentColor = Teal,
                containerColor = TealContainer,
                onClick = onNavigateToFleet,
                modifier = Modifier.weight(1f)
            )
            RecyclerQuickActionCard(
                title = "EPR Certs",
                subtitle = "Form 10 ledger",
                icon = Icons.Outlined.Verified,
                accentColor = Gold,
                containerColor = SafetyOrangeContainer,
                onClick = onNavigateToCompliance,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Pending Dispatches Requiring Pickup
        if (state.wonAuctions.isNotEmpty()) {
            Text(
                text = "Won Auctions Ready for Truck Dispatch",
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
                state.wonAuctions.forEach { request ->
                    PendingPickupCard(
                        request = request,
                        onInitiate = { showPickupDialog = request }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Active In-Transit Trucks
        if (state.incomingShipments.isNotEmpty()) {
            Text(
                text = "Inbound Shipments (Weighbridge Check)",
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
                state.incomingShipments.forEach { transfer ->
                    InTransitCard(
                        transfer = transfer,
                        onVerify = { showVerifyDialog = transfer }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun RecyclerStatCard(
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
private fun RecyclerQuickActionCard(
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
private fun PendingPickupCard(
    request: BidRequestEntity,
    onInitiate: () -> Unit
) {
    val category = runCatching { ScrapCategory.valueOf(request.scrapCategory) }.getOrDefault(ScrapCategory.OTHER)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            text = category.displayName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Origin: ${request.factoryId}",
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
                        text = "${request.estimatedWeightKg} kg",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onInitiate,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dispatch Truck & Assign Driver", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun InTransitCard(
    transfer: TransferEntity,
    onVerify: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Vehicle: ${transfer.vehicleNumber}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Dispatched: ${DateUtils.formatTime(transfer.initiatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = TealContainer
                ) {
                    Text(
                        text = "${transfer.weightAtSource} kg",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Teal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify Arrival Weighbridge", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun RecyclerHeader(
    user: com.sktech.wastetrack.domain.model.User?,
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
                        text = user?.organizationName?.ifBlank { "Certified Recycler Hub" } ?: "Certified Recycler Hub",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
                Text(
                    text = "MPCB Certified Agency · ${user?.industrialArea.orEmpty().ifBlank { "MIDC Zone" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = EmeraldContainer
                ) {
                    Text(
                        "CERTIFIED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
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

@Composable
private fun InitiatePickupDialog(
    request: BidRequestEntity,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var vehicleNo by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Assign Truck for Pickup", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Factory: ${request.factoryId} (${request.estimatedWeightKg} kg ${request.scrapCategory})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it.uppercase() },
                    label = { Text("Vehicle Registration No.") },
                    placeholder = { Text("e.g. MH-15-EG-4521") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(vehicleNo) },
                enabled = vehicleNo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Confirm Dispatch")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun VerifyWeightDialog(
    transfer: TransferEntity,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    val weight = weightText.toFloatOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weighbridge Verification", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Truck: ${transfer.vehicleNumber} (Dispatched: ${transfer.weightAtSource} kg)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Measured Destination Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { weight?.let(onConfirm) },
                enabled = weight != null && weight > 0f,
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text("Verify & Complete Handshake")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
