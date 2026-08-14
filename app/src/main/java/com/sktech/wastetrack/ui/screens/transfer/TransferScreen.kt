package com.sktech.wastetrack.ui.screens.transfer

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.sktech.wastetrack.R
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.domain.model.TransferStatus
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

private fun generateQRBitmap(content: String, size: Int = 512): Bitmap {
    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    onNavigateBack: () -> Unit,
    onNavigateToGatePass: (String) -> Unit = {},
    viewModel: TransferViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.transfers),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Chain-of-Custody & Gatepass Ledger",
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
        // QR Display Dialog
        if (state.qrPayload != null) {
            AlertDialog(
                onDismissRequest = { viewModel.clearQR() },
                title = {
                    Text(
                        stringResource(R.string.qr_handshake_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            stringResource(R.string.show_qr_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val bitmap = remember(state.qrPayload) {
                            generateQRBitmap(state.qrPayload!!)
                        }
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = Color.White,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 4.dp
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Transfer QR Code",
                                modifier = Modifier
                                    .size(240.dp)
                                    .padding(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = SafetyOrangeContainer
                        ) {
                            Text(
                                stringResource(R.string.valid_30_min),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = SafetyOrange,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearQR() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                    ) {
                        Text(stringResource(R.string.done), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            if (state.transfers.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.LocalShipping,
                                        contentDescription = null,
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.no_transfers_yet),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.log_scrap_first_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(state.transfers, key = { it.id }) { transfer ->
                    ModernTransferCard(
                        transfer = transfer,
                        onClick = { onNavigateToGatePass(transfer.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernTransferCard(
    transfer: TransferEntity,
    onClick: () -> Unit = {}
) {
    val statusEnum = runCatching { TransferStatus.valueOf(transfer.status) }.getOrDefault(TransferStatus.INITIATED)
    val statusColor = when (transfer.status) {
        "QR_GENERATED" -> Gold
        "QR_SCANNED" -> Teal
        "IN_TRANSIT" -> SafetyOrange
        "COMPLETED", "VERIFIED" -> EmeraldPrimary
        else -> TextSecondary
    }
    val statusContainer = when (transfer.status) {
        "QR_GENERATED" -> SafetyOrangeContainer
        "QR_SCANNED" -> TealContainer
        "IN_TRANSIT" -> SafetyOrangeContainer
        "COMPLETED", "VERIFIED" -> EmeraldContainer
        else -> LightSurfaceVariant
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
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
                        color = TealContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.LocalShipping, null, tint = Teal, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column {
                        Text(
                            text = "Dispatch #${transfer.id.take(8).uppercase()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${stringResource(R.string.vehicle_number)}: ${transfer.vehicleNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusContainer
                ) {
                    Text(
                        text = stringResource(statusEnum.nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(stringResource(R.string.load_weight_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${transfer.weightAtSource} kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }

                if (transfer.weightAtDestination != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(R.string.measured_weight_kg), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${transfer.weightAtDestination} kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatTime(transfer.initiatedAt), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
