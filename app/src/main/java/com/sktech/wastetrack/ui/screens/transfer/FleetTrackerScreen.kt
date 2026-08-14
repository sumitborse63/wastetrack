package com.sktech.wastetrack.ui.screens.transfer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.sktech.wastetrack.domain.model.TransferStatus
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetTrackerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = In Transit, 1 = Completed
    var verifyTransferDialog by remember { mutableStateOf<TransferEntity?>(null) }

    verifyTransferDialog?.let { transfer ->
        var receivedWeightText by remember { mutableStateOf("${transfer.weightAtSource}") }
        val parsedWeight = receivedWeightText.toFloatOrNull()

        AlertDialog(
            onDismissRequest = { verifyTransferDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Scale, contentDescription = null, tint = EmeraldPrimary)
                    Text(stringResource(R.string.weighbridge_dialog_title), fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.vehicle_dispatched_format, transfer.vehicleNumber, transfer.weightAtSource.toString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = receivedWeightText,
                        onValueChange = { receivedWeightText = it },
                        label = { Text(stringResource(R.string.measured_dest_weight_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.mpcb_form10_cert_notice),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        parsedWeight?.let { weight ->
                            viewModel.verifyDestinationArrival(transfer.id, weight)
                        }
                        verifyTransferDialog = null
                    },
                    enabled = parsedWeight != null && parsedWeight > 0f,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text(stringResource(R.string.verify_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { verifyTransferDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.fleet_transport_hub),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.live_gps_telemetry_sub),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
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
        val activeShipments = state.transfers.filter { it.status == "IN_TRANSIT" || it.status == "QR_SCANNED" || it.status == "INITIATED" || it.status == "QR_GENERATED" }
        val completedShipments = state.transfers.filter { it.status == "VERIFIED" || it.status == "DELIVERED" || it.status == "DISPUTED" }
        val currentList = if (selectedTab == 0) activeShipments else completedShipments

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Status Message Banners
            state.successMessage?.let { msg ->
                Surface(
                    color = EmeraldContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = EmeraldPrimary)
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = EmeraldPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
                LaunchedEffect(msg) {
                    kotlinx.coroutines.delay(4000)
                    viewModel.clearMessages()
                }
            }

            state.error?.let { err ->
                Surface(
                    color = AlertRedContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Error, null, tint = AlertRed)
                        Text(err, style = MaterialTheme.typography.bodySmall, color = AlertRed, fontWeight = FontWeight.SemiBold)
                    }
                }
                LaunchedEffect(err) {
                    kotlinx.coroutines.delay(4000)
                    viewModel.clearMessages()
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            stringResource(R.string.in_transit_tab_format, activeShipments.size),
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            stringResource(R.string.completed_tab_format, completedShipments.size),
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            if (currentList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(68.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.LocalShipping,
                                    contentDescription = null,
                                    modifier = Modifier.size(34.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            if (selectedTab == 0) stringResource(R.string.no_trucks_in_transit) else stringResource(R.string.no_completed_deliveries),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (selectedTab == 0) stringResource(R.string.trucks_in_transit_sub) else stringResource(R.string.completed_deliveries_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp)
                ) {
                    items(currentList, key = { it.id }) { transfer ->
                        InteractiveFleetTruckCard(
                            transfer = transfer,
                            onVerifyClick = { verifyTransferDialog = transfer },
                            onCallDriver = {
                                val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:+919876543210")
                                }
                                runCatching { context.startActivity(intent) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveFleetTruckCard(
    transfer: TransferEntity,
    onVerifyClick: () -> Unit,
    onCallDriver: () -> Unit
) {
    val statusEnum = runCatching { TransferStatus.valueOf(transfer.status) }.getOrDefault(TransferStatus.IN_TRANSIT)
    val isInTransit = transfer.status == "IN_TRANSIT" || transfer.status == "QR_SCANNED"
    val isVerified = transfer.status == "VERIFIED"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Vehicle Registration & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (isVerified) EmeraldContainer else TealContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.LocalShipping,
                                contentDescription = "Truck",
                                tint = if (isVerified) EmeraldPrimary else Teal,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            transfer.vehicleNumber.ifBlank { stringResource(R.string.unassigned_truck) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.gatepass_dispatched_format, transfer.id.take(8).uppercase(), DateUtils.formatDateTime(transfer.initiatedAt)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = when (transfer.status) {
                        "VERIFIED" -> EmeraldContainer
                        "DISPUTED" -> AlertRedContainer
                        else -> SafetyOrangeContainer
                    }
                ) {
                    Text(
                        text = stringResource(statusEnum.nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when (transfer.status) {
                            "VERIFIED" -> EmeraldPrimary
                            "DISPUTED" -> AlertRed
                            else -> SafetyOrange
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Route Progress Visualization
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.Navigation, contentDescription = null, tint = Teal, modifier = Modifier.size(14.dp))
                            Text(
                                if (isInTransit) stringResource(R.string.live_route_demo) else stringResource(R.string.origin_ambad_demo),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            if (isInTransit) stringResource(R.string.eta_demo) else stringResource(R.string.delivered_label),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isInTransit) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LinearProgressIndicator(
                        progress = { if (isVerified) 1.0f else if (isInTransit) 0.65f else 0.2f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isVerified) EmeraldPrimary else Teal,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.factory_plant_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(stringResource(R.string.status_in_transit), style = MaterialTheme.typography.labelSmall, color = Teal, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.recycler_yard_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Load Weight & Driver Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.source_weight_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${transfer.weightAtSource} kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (transfer.weightAtDestination != null && transfer.weightAtDestination > 0f) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.dest_weight_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${transfer.weightAtDestination} kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.driver_contact_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Suresh Patil", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            if (isInTransit) {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCallDriver,
                        modifier = Modifier.weight(1f).height(42.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(15.dp), tint = EmeraldPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.call_driver),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Button(
                        onClick = onVerifyClick,
                        modifier = Modifier.weight(1.3f).height(42.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Filled.Scale, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.weighbridge_check),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
