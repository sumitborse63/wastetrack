package com.sktech.wastetrack.ui.screens.bid

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.domain.model.UserRole
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
                    Column {
                        Text(
                            stringResource(R.string.bid_market),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (userRole == UserRole.RECYCLER) stringResource(R.string.browse_and_bid) else stringResource(R.string.micro_auctions_subtitle),
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
        floatingActionButton = {
            if (userRole != UserRole.RECYCLER) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showCreateDialog() },
                    containerColor = EmeraldPrimary,
                    contentColor = Color.White,
                    shape = MaterialTheme.shapes.medium,
                    elevation = FloatingActionButtonDefaults.elevation(3.dp)
                ) {
                    Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.new_bid),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
        ) {
            // Success Notification Banner
            if (state.successMessage != null) {
                item {
                    Surface(
                        color = EmeraldContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, null, tint = EmeraldPrimary)
                            Text(
                                state.successMessage!!,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                    LaunchedEffect(state.successMessage) {
                        kotlinx.coroutines.delay(3000)
                        viewModel.clearSuccess()
                    }
                }
            }

            // Market Summary Row
            item {
                val openCount = state.bidRequests.count { it.status == BidStatus.OPEN }
                val awardedCount = state.bidRequests.count { it.status == BidStatus.AWARDED }
                val totalRevenue = state.bidRequests
                    .filter { it.status == BidStatus.AWARDED }
                    .sumOf { it.estimatedWeightKg.toDouble() * it.reservePricePerKg.toDouble() }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BidStatCard(
                        title = stringResource(R.string.live_auctions),
                        value = "$openCount",
                        accentColor = Gold,
                        containerColor = SafetyOrangeContainer,
                        icon = Icons.Outlined.Gavel,
                        modifier = Modifier.weight(1f)
                    )
                    BidStatCard(
                        title = stringResource(R.string.awarded_auctions),
                        value = "$awardedCount",
                        accentColor = EmeraldPrimary,
                        containerColor = EmeraldContainer,
                        icon = Icons.Outlined.Verified,
                        modifier = Modifier.weight(1f)
                    )
                    BidStatCard(
                        title = stringResource(R.string.volume_value),
                        value = "₹${String.format("%.0f", totalRevenue)}",
                        accentColor = Teal,
                        containerColor = TealContainer,
                        icon = Icons.Outlined.TrendingUp,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }

            if (state.bidRequests.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
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
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Outlined.Gavel,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.no_active_auctions),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.no_active_auctions_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(state.bidRequests, key = { it.id }) { request ->
                    ModernBidRequestCard(
                        request = request,
                        onClick = { onNavigateToBidDetail(request.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BidStatCard(
    title: String,
    value: String,
    accentColor: Color,
    containerColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
        }
    }
}

@Composable
private fun ModernBidRequestCard(request: BidRequest, onClick: () -> Unit) {
    val category = request.scrapCategory
    val isOpen = request.status == BidStatus.OPEN
    val statusColor = when (request.status) {
        BidStatus.OPEN -> Gold
        BidStatus.AWARDED -> EmeraldPrimary
        BidStatus.CLOSED -> TextSecondary
        else -> SafetyOrange
    }
    val statusContainer = when (request.status) {
        BidStatus.OPEN -> SafetyOrangeContainer
        BidStatus.AWARDED -> EmeraldContainer
        BidStatus.CLOSED -> LightSurfaceVariant
        else -> SafetyOrangeContainer
    }
    val remaining = DateUtils.getRemainingTimeString(request.auctionEndTime)

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Material Hero Image Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(category.color().copy(alpha = 0.2f))
            ) {
                coil.compose.AsyncImage(
                    model = category.sampleImageUrl,
                    contentDescription = category.displayName,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )

                // Top Badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(category.icon, fontSize = 12.sp)
                            Text(
                                stringResource(category.nameRes),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusContainer
                    ) {
                        Text(
                            text = stringResource(request.status.nameRes),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // Bottom Lot weight on image
                Text(
                    text = "${request.estimatedWeightKg} kg (${String.format("%.2f", request.estimatedWeightKg / 1000f)} MT)",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = if (isOpen) Gold else TextMuted
                    )
                    Text(
                        text = if (isOpen) remaining else stringResource(R.string.auction_concluded),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isOpen) MaterialTheme.colorScheme.onSurface else TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.reserve_price_format, request.reservePricePerKg),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.est_total_format, String.format("%.0f", request.estimatedWeightKg * request.reservePricePerKg)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}
}

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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(R.string.new_bid),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (error != null) {
                    Text(error, color = AlertRed, style = MaterialTheme.typography.bodySmall)
                }

                Text(
                    stringResource(R.string.select_scrap_batch),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (scrapEntries.isEmpty()) {
                    Text(stringResource(R.string.log_scrap_first_desc), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(scrapEntries) { entry ->
                            val isSelected = entry.id == selectedEntryId
                            val cat = try { ScrapCategory.valueOf(entry.category) } catch (e: Exception) { ScrapCategory.OTHER }
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectEntry(entry.id) },
                                label = { Text("${cat.icon} ${entry.weightKg} kg") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                Text(
                    stringResource(R.string.reserve_price_kg),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = reservePrice,
                    onValueChange = onReservePriceChanged,
                    placeholder = { Text("e.g. 45", color = TextMuted) },
                    prefix = { Text("₹ ", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                suggestedPrice?.let {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = SafetyOrangeContainer
                    ) {
                        Text(
                            stringResource(R.string.market_rate_format, it),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SafetyOrange
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isCreating && selectedEntryId != null && reservePrice.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
            ) {
                if (isCreating) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.publish_auction), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        }
    )
}
