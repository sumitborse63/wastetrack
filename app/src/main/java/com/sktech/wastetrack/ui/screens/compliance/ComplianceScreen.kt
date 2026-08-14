package com.sktech.wastetrack.ui.screens.compliance

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import com.sktech.wastetrack.domain.model.CertificateStatus
import com.sktech.wastetrack.domain.model.CertificateType
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils
import com.sktech.wastetrack.util.PdfExporter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplianceScreen(
    onNavigateBack: () -> Unit,
    viewModel: ComplianceViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    state.selectedCertificate?.let { cert ->
        CertificateDetailDialog(
            certificate = cert,
            onDismiss = { viewModel.clearSelection() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.compliance),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.compliance_manifests_sub),
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
                actions = {
                    if (state.completedTransfers.isNotEmpty()) {
                        IconButton(onClick = { viewModel.generateBulkCertificates() }) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = "Auto-Generate", tint = Gold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Success / Error banners
            state.successMessage?.let { msg ->
                item {
                    Surface(
                        color = EmeraldContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
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
                }
            }

            // Summary Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ComplianceStatCard(
                        title = stringResource(R.string.certificates),
                        value = "${state.certificates.size}",
                        icon = Icons.Outlined.Verified,
                        color = EmeraldPrimary,
                        containerColor = EmeraldContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ComplianceStatCard(
                        title = stringResource(R.string.audit_ready),
                        value = "${state.certificates.count { it.status == "GENERATED" || it.status == "SUBMITTED" }}",
                        icon = Icons.Outlined.FactCheck,
                        color = Teal,
                        containerColor = TealContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ComplianceStatCard(
                        title = stringResource(R.string.mpcb_certificates),
                        value = "${state.completedTransfers.size}",
                        icon = Icons.Outlined.Description,
                        color = Gold,
                        containerColor = SafetyOrangeContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pending Transfers Header & List
            val uncertifiedTransfers = state.completedTransfers.filter { transfer ->
                state.certificates.none { it.transferId == transfer.id }
            }
            if (uncertifiedTransfers.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.pending_mpcb_generation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SafetyOrange
                    )
                }
                items(uncertifiedTransfers, key = { "pending-${it.id}" }) { transfer ->
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, SafetyOrange.copy(alpha = 0.4f)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(R.string.transfer_number_format, transfer.id.take(8).uppercase()), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("${transfer.weightAtSource} kg · Vehicle: ${transfer.vehicleNumber}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = { viewModel.generateCertificateForTransfer(transfer) },
                                colors = ButtonDefaults.buttonColors(containerColor = SafetyOrange, contentColor = Color.White),
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.generate_certificate_btn), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Generated Certificates Header
            item {
                Text(
                    stringResource(R.string.issued_certificates),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (state.certificates.isEmpty()) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.VerifiedUser, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.no_certificates_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.certificates_empty_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                items(state.certificates, key = { it.id }) { cert ->
                    ModernCertificateCard(
                        certificate = cert,
                        onClick = { viewModel.selectCertificate(cert) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ComplianceStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
            }
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
private fun ModernCertificateCard(
    certificate: CertificateEntity,
    onClick: () -> Unit
) {
    val typeEnum = runCatching { CertificateType.valueOf(certificate.type) }.getOrDefault(CertificateType.MPCB_DISPOSAL)
    val statusEnum = runCatching { CertificateStatus.valueOf(certificate.status) }.getOrDefault(CertificateStatus.GENERATED)
    val typeColor = when (certificate.type) {
        "MPCB_DISPOSAL", "FORM_38" -> EmeraldPrimary
        "ESG_CREDIT", "FORM_4" -> Teal
        "AUDIT_REPORT", "RECYCLING_RECEIPT" -> Gold
        else -> SyncBlue
    }
    val typeContainer = when (certificate.type) {
        "MPCB_DISPOSAL", "FORM_38" -> EmeraldContainer
        "ESG_CREDIT", "FORM_4" -> TealContainer
        "AUDIT_REPORT", "RECYCLING_RECEIPT" -> SafetyOrangeContainer
        else -> TealContainer
    }

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = typeContainer,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Description, null, tint = typeColor, modifier = Modifier.size(22.dp))
                    }
                }
                Column {
                    Text(stringResource(typeEnum.nameRes), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("Ref: #${certificate.id.take(8).uppercase()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                shape = MaterialTheme.shapes.small,
                color = EmeraldContainer
            ) {
                Text(
                    stringResource(statusEnum.nameRes),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun CertificateDetailDialog(
    certificate: CertificateEntity,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isExporting by remember { mutableStateOf(false) }
    val typeEnum = runCatching { CertificateType.valueOf(certificate.type) }.getOrDefault(CertificateType.MPCB_DISPOSAL)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.cert_details_format, stringResource(typeEnum.nameRes)), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Certificate ID: ${certificate.id}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Transfer ID: ${certificate.transferId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Factory ID: ${certificate.factoryId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Generated At: ${DateUtils.formatDate(certificate.generatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text("Digital Hash: ${certificate.digitalSignature.take(24)}...", style = MaterialTheme.typography.labelSmall, color = Teal, fontFamily = FontFamily.Monospace)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isExporting = true
                    PdfExporter.exportCertificate(context, certificate)
                    isExporting = false
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.export_pdf), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    )
}
