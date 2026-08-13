package com.sktech.wastetrack.ui.screens.compliance

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.data.local.db.entity.CertificateEntity
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

    // Certificate detail dialog
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
                    Text("ESG Compliance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.completedTransfers.isNotEmpty()) {
                        IconButton(onClick = { viewModel.generateBulkCertificates() }) {
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = "Auto-Generate", tint = Gold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Success / Error banners
            state.successMessage?.let { msg ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = IndustrialGreen.copy(alpha = 0.15f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CheckCircle, null, tint = IndustrialGreenLight)
                            Text(msg, style = MaterialTheme.typography.bodyMedium, color = IndustrialGreenLight)
                        }
                    }
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearMessages()
                    }
                }
            }

            state.error?.let { err ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = AlertRed.copy(alpha = 0.1f))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Error, null, tint = AlertRed)
                            Text(err, style = MaterialTheme.typography.bodyMedium, color = AlertRed)
                        }
                    }
                }
            }

            // Summary stats
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ComplianceStatCard(
                        title = "Certificates",
                        value = "${state.certificates.size}",
                        icon = Icons.Outlined.VerifiedUser,
                        color = IndustrialGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                    ComplianceStatCard(
                        title = "Audit Ready",
                        value = "${state.certificates.count { it.status == "GENERATED" || it.status == "SUBMITTED" }}",
                        icon = Icons.Outlined.FactCheck,
                        color = Teal,
                        modifier = Modifier.weight(1f)
                    )
                    ComplianceStatCard(
                        title = "Pending",
                        value = "${state.completedTransfers.size - state.certificates.size}",
                        icon = Icons.Outlined.PendingActions,
                        color = SafetyOrange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Pending transfers that need certificates
            val uncertifiedTransfers = state.completedTransfers.filter { transfer ->
                state.certificates.none { it.transferId == transfer.id }
            }
            if (uncertifiedTransfers.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Pending Certificate Generation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = SafetyOrange)
                }
                items(uncertifiedTransfers, key = { "pending-${it.id}" }) { transfer ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SafetyOrange.copy(alpha = 0.06f)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.Receipt, null, tint = SafetyOrange, modifier = Modifier.size(24.dp))
                                Column {
                                    Text("Transfer #${transfer.id.take(8)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${transfer.weightAtSource} kg · ${transfer.status}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            FilledTonalButton(
                                onClick = { viewModel.generateCertificateForTransfer(transfer) },
                                enabled = !state.isGenerating,
                                contentPadding = PaddingValues(horizontal = 12.dp)
                            ) {
                                if (state.isGenerating) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Generate", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Generated certificates
            if (state.certificates.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Generated Certificates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                items(state.certificates, key = { it.id }) { cert ->
                    CertificateCard(
                        certificate = cert,
                        onClick = { viewModel.selectCertificate(cert) }
                    )
                }
            }

            if (state.certificates.isEmpty() && uncertifiedTransfers.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.VerifiedUser, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No compliance data yet", style = MaterialTheme.typography.titleSmall)
                            Text("Complete scrap transfers to auto-generate MPCB certificates", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ComplianceStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(title, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun CertificateCard(certificate: CertificateEntity, onClick: () -> Unit) {
    val type = try { CertificateType.valueOf(certificate.type) } catch (_: Exception) { CertificateType.MPCB_DISPOSAL }
    val statusColor = when (certificate.status) {
        "GENERATED" -> IndustrialGreenLight
        "SUBMITTED" -> Teal
        "ACCEPTED" -> IndustrialGreen
        else -> SteelGray
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = IndustrialGreenLight.copy(alpha = 0.15f),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.VerifiedUser, null, tint = IndustrialGreenLight, modifier = Modifier.size(24.dp))
                    }
                }
                Column {
                    Text(type.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Transfer #${certificate.transferId.take(8)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(DateUtils.formatDateTime(certificate.generatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = MaterialTheme.shapes.extraSmall, color = statusColor.copy(alpha = 0.15f)) {
                    Text(certificate.status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall, color = statusColor, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Icon(Icons.Outlined.ChevronRight, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CertificateDetailDialog(certificate: CertificateEntity, onDismiss: () -> Unit) {
    val type = try { CertificateType.valueOf(certificate.type) } catch (_: Exception) { CertificateType.MPCB_DISPOSAL }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.VerifiedUser, null, tint = IndustrialGreenLight)
                Text(type.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("Certificate ID", certificate.id.take(12) + "...")
                DetailRow("Transfer ID", certificate.transferId.take(12) + "...")
                DetailRow("Status", certificate.status)
                DetailRow("Generated", DateUtils.formatDateTime(certificate.generatedAt))
                certificate.submittedAt?.let { DetailRow("Submitted", DateUtils.formatDateTime(it)) }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Text("Digital Signature", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        certificate.digitalSignature.take(48) + "...",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val context = LocalContext.current
                OutlinedButton(onClick = { PdfExporter.exportCertificate(context, certificate) }) {
                    Icon(Icons.Outlined.PictureAsPdf, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export PDF")
                }
                Button(onClick = onDismiss) { Text("Close") }
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}
