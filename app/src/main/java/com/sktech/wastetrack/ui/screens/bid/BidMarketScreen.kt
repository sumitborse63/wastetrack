package com.sktech.wastetrack.ui.screens.bid

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidMarketScreen(
    onNavigateBack: () -> Unit,
    onNavigateToBidDetail: (String) -> Unit = {},
    viewModel: BidViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()

    // Create Bid Dialog
    if (state.showCreateDialog) {
        CreateBidDialog(
            scrapEntries = state.scrapEntries,
            selectedEntryId = state.selectedScrapEntryId,
            reservePrice = state.reservePrice,
            suggestedPrice = state.suggestedPrice,
            isCreating = state.isCreating,
            error = state.error,
            onSelectEntry = viewModel::onScrapEntrySelected,
            onReservePriceChanged = viewModel::onReservePriceChanged,
            onConfirm = viewModel::createBidRequest,
            onDismiss = viewModel::dismissCreateDialog
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bid Marketplace",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (userRole != com.sktech.wastetrack.domain.model.UserRole.RECYCLER) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Bid", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Success banner
            if (state.successMessage != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = IndustrialGreen.copy(alpha = 0.15f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = IndustrialGreenLight)
                            Text(
                                state.successMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = IndustrialGreenLight
                            )
                        }
                    }
                    LaunchedEffect(state.successMessage) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearSuccess()
                    }
                }
            }

            // Stats header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val openCount = state.bidRequests.count { it.status == BidStatus.OPEN }
                    val awardedCount = state.bidRequests.count { it.status == BidStatus.AWARDED }
                    val totalRevenue = state.bidRequests
                        .filter { it.status == BidStatus.AWARDED }
                        .sumOf { it.estimatedWeightKg.toDouble() * it.reservePricePerKg.toDouble() }

                    BidStatChip(label = "Open", value = "$openCount", color = Gold)
                    BidStatChip(label = "Awarded", value = "$awardedCount", color = IndustrialGreenLight)
                    BidStatChip(
                        label = "Revenue",
                        value = "₹${String.format("%.0f", totalRevenue)}",
                        color = Teal
                    )
                }
            }

            if (state.bidRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Storefront, null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "No bid requests yet",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Create a bid to let recyclers compete for your scrap",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(state.bidRequests, key = { it.id }) { request ->
                    BidRequestCard(
                        request = request,
                        onClick = { onNavigateToBidDetail(request.id) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) } // FAB clearance
        }
    }
}

@Composable
private fun BidStatChip(label: String, value: String, color: Color) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = color.copy(alpha = 0.12f),
        modifier = Modifier.height(48.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun BidRequestCard(request: BidRequest, onClick: () -> Unit) {
    val category = request.scrapCategory
    val statusColor = when (request.status) {
        BidStatus.OPEN -> Gold
        BidStatus.AWARDED -> IndustrialGreenLight
        BidStatus.CLOSED -> SteelGray
        else -> SafetyOrange
    }
    val remaining = DateUtils.getRemainingTimeString(request.auctionEndTime)

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = category.color().copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(category.icon, style = MaterialTheme.typography.titleSmall)
                        }
                    }
                    Column {
                        Text(
                            "${category.displayName} Scrap",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${request.estimatedWeightKg} kg · Reserve ₹${request.reservePricePerKg}/kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        request.status.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Outlined.Timer, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(remaining, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    "Est. ₹${String.format("%.0f", request.estimatedWeightKg * request.reservePricePerKg)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateBidDialog(
    scrapEntries: List<com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity>,
    selectedEntryId: String?,
    reservePrice: String,
    suggestedPrice: Float?,
    isCreating: Boolean,
    error: String?,
    onSelectEntry: (String) -> Unit,
    onReservePriceChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedEntry = scrapEntries.find { it.id == selectedEntryId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Bid Request", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Select a scrap entry and set your minimum reserve price. A 24-hour blind auction will notify certified recyclers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Scrap entry selector
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = selectedEntry?.let {
                            val cat = try { ScrapCategory.valueOf(it.category) } catch (_: Exception) { ScrapCategory.OTHER }
                            "${cat.icon} ${cat.displayName} — ${it.weightKg} kg"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Scrap Entry") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        scrapEntries.take(20).forEach { entry ->
                            val cat = try { ScrapCategory.valueOf(entry.category) } catch (_: Exception) { ScrapCategory.OTHER }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "${cat.icon} ${cat.displayName} — ${entry.weightKg} kg",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = {
                                    onSelectEntry(entry.id)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Reserve price
                OutlinedTextField(
                    value = reservePrice,
                    onValueChange = onReservePriceChanged,
                    label = { Text("Reserve Price") },
                    placeholder = { Text("Min ₹/kg") },
                    prefix = { Text("₹") },
                    suffix = { Text("/kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                if (suggestedPrice != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoGraph,
                            contentDescription = "AI Suggestion",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            "AI Suggested Market Price: ₹$suggestedPrice/kg",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (selectedEntry != null) {
                    val reserve = reservePrice.toFloatOrNull() ?: 0f
                    if (reserve > 0) {
                        Surface(
                            color = IndustrialGreen.copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                "Estimated minimum: ₹${String.format("%.0f", reserve * selectedEntry.weightKg)}",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = IndustrialGreenLight,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (error != null) {
                    Text(error, style = MaterialTheme.typography.bodySmall, color = AlertRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !isCreating) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Start 24h Auction")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
