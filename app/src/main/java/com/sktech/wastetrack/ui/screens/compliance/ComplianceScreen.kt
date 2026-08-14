package com.sktech.wastetrack.ui.screens.compliance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
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
    var selectedFilter by remember { mutableStateOf("ALL") }

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
                            "MPCB Form 10 & Statutory Manifests",
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
                            Surface(
                                shape = CircleShape,
                                color = EmeraldContainer,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.AutoAwesome,
                                        contentDescription = "Auto-Generate All",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
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
            contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
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
                            Text(
                                msg,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMessages()
                    }
                }
            }

            // Summary Stats KPI Row (Stitch Style)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModernComplianceStatCard(
                        title = "TOTAL CERTS",
                        value = "${state.certificates.size}",
                        icon = Icons.Outlined.Verified,
                        accentColor = EmeraldPrimary,
                        containerColor = EmeraldContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ModernComplianceStatCard(
                        title = "AUDIT READY",
                        value = "${state.certificates.count { it.status == "GENERATED" || it.status == "SUBMITTED" }}",
                        icon = Icons.Outlined.FactCheck,
                        accentColor = Teal,
                        containerColor = TealContainer,
                        modifier = Modifier.weight(1f)
                    )
                    ModernComplianceStatCard(
                        title = "MPCB FORM 10",
                        value = "${state.completedTransfers.size}",
                        icon = Icons.Outlined.Description,
                        accentColor = SafetyOrange,
                        containerColor = SafetyOrangeContainer,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }

            // Filter Tabs (Stitch UI)
            item {
                val filterOptions = listOf(
                    "ALL" to "All Manifests",
                    "GENERATED" to "Generated",
                    "PENDING" to "Pending MPCB",
                    "AUDITED" to "Audited"
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    items(filterOptions) { (key, label) ->
                        val isSelected = selectedFilter == key
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = key },
                            label = {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            // Pending Transfers Header & List
            val uncertifiedTransfers = state.completedTransfers.filter { transfer ->
                state.certificates.none { it.transferId == transfer.id }
            }
            if (uncertifiedTransfers.isNotEmpty() && (selectedFilter == "ALL" || selectedFilter == "PENDING")) {
                item {
                    Text(
                        stringResource(R.string.pending_mpcb_generation),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = SafetyOrangeContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.LocalShipping, null, tint = SafetyOrange, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text(
                                        "Transfer #${transfer.id.take(8).uppercase()}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${transfer.weightAtSource} kg · Vehicle: ${transfer.vehicleNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.generateCertificateForTransfer(transfer) },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White),
                                shape = MaterialTheme.shapes.small,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Generate",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Generated Certificates Header
            if (selectedFilter != "PENDING") {
                item {
                    Text(
                        stringResource(R.string.issued_certificates),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val filteredCerts = when (selectedFilter) {
                    "GENERATED" -> state.certificates.filter { it.status == "GENERATED" }
                    "AUDITED" -> state.certificates.filter { it.status == "SUBMITTED" || it.status == "AUDITED" }
                    else -> state.certificates
                }

                if (filteredCerts.isEmpty()) {
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
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.VerifiedUser,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    stringResource(R.string.no_certificates_yet),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    stringResource(R.string.certificates_empty_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(filteredCerts, key = { it.id }) { cert ->
                        StitchCertificateCard(
                            certificate = cert,
                            onClick = { viewModel.selectCertificate(cert) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernComplianceStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(95.dp),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier.size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accentColor, modifier = Modifier.size(13.dp))
                    }
                }
            }
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StitchCertificateCard(
    certificate: CertificateEntity,
    onClick: () -> Unit
) {
    val typeEnum = runCatching { CertificateType.valueOf(certificate.type) }.getOrDefault(CertificateType.MPCB_DISPOSAL)
    val statusEnum = runCatching { CertificateStatus.valueOf(certificate.status) }.getOrDefault(CertificateStatus.GENERATED)

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
                        color = EmeraldContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.FactCheck, null, tint = EmeraldPrimary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column {
                        Text(
                            stringResource(typeEnum.nameRes),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Manifest ID: #${certificate.id.take(8).uppercase()}",
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
                        stringResource(statusEnum.nameRes),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "SHA-256 DIGITAL SIGNATURE",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        certificate.digitalSignature.take(18) + "...",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = Teal
                    )
                }
                Text(
                    DateUtils.formatDate(certificate.generatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            Text(
                "${stringResource(typeEnum.nameRes)} Details",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Certificate ID: ${certificate.id}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Transfer Ref: ${certificate.transferId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Factory Node: ${certificate.factoryId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Issued Timestamp: ${DateUtils.formatDate(certificate.generatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(
                    "Cryptographic Signature:\n${certificate.digitalSignature}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Teal
                )
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
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}
