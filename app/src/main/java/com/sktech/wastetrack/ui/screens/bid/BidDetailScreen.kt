package com.sktech.wastetrack.ui.screens.bid

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidStatus
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidDetailScreen(
    bidRequestId: String,
    onNavigateBack: () -> Unit,
    viewModel: BidViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsStateWithLifecycle()
    val userRole by viewModel.userRole.collectAsStateWithLifecycle()
    val request = detailState.bidRequest
    val showAwardButton = userRole != UserRole.RECYCLER

    LaunchedEffect(bidRequestId) {
        viewModel.loadBidDetail(bidRequestId)
    }

    var bidPriceText by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            stringResource(R.string.bid_detail),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Auction Batch #${bidRequestId.take(8).uppercase()}",
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
        bottomBar = {
            if (userRole == UserRole.RECYCLER && request?.status == BidStatus.OPEN) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (localError != null) {
                            Text(
                                text = localError!!,
                                color = AlertRed,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = bidPriceText,
                                onValueChange = {
                                    bidPriceText = it
                                    localError = null
                                },
                                placeholder = { Text(stringResource(R.string.reserve_price_kg), color = TextMuted) },
                                prefix = { Text("₹ ", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                                suffix = { Text("/kg", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    val price = bidPriceText.toFloatOrNull()
                                    if (price == null || price <= 0f) {
                                        localError = "Enter a valid bid rate"
                                    } else {
                                        viewModel.submitBid(price)
                                        bidPriceText = ""
                                    }
                                },
                                modifier = Modifier.height(52.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldPrimary,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.Gavel, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.place_bid), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (detailState.isLoading && request == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                // Main Auction Details Card
                if (request != null) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shadowElevation = 1.dp
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = request.scrapCategory.color().copy(alpha = 0.15f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(request.scrapCategory.icon, fontSize = 24.sp)
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            stringResource(request.scrapCategory.nameRes),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            "${stringResource(R.string.origin_label)}: ${request.factoryId}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = if (request.status == BidStatus.OPEN) SafetyOrangeContainer else EmeraldContainer
                                    ) {
                                        Text(
                                            stringResource(request.status.nameRes),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (request.status == BidStatus.OPEN) SafetyOrange else EmeraldPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), thickness = 1.dp)
                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    AuctionMetric(label = stringResource(R.string.total_weight_label), value = "${request.estimatedWeightKg} kg")
                                    AuctionMetric(label = stringResource(R.string.reserve_price_kg), value = "₹${request.reservePricePerKg}/kg")
                                    AuctionMetric(label = stringResource(R.string.active_bids), value = "${detailState.bids.size}")
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = SafetyOrangeContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Outlined.Timer, null, tint = SafetyOrange, modifier = Modifier.size(16.dp))
                                        Text(
                                            text = "Closing in: ${DateUtils.getRemainingTimeString(request.auctionEndTime)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = SafetyOrange,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Bid Leaderboard Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.live_leaderboard),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (detailState.bids.isNotEmpty()) {
                            Text(
                                text = "Highest: ₹${detailState.bids.maxOf { it.pricePerKg }}/kg",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldPrimary
                            )
                        }
                    }
                }

                if (detailState.bids.isEmpty()) {
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
                                Icon(Icons.Outlined.MonetizationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(stringResource(R.string.no_bids_yet), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                } else {
                    itemsIndexed(detailState.bids, key = { _, bid -> bid.id }) { index, bid ->
                        ModernBidRow(
                            bid = bid,
                            rank = index + 1,
                            totalWeightKg = request?.estimatedWeightKg ?: 0f,
                            showAward = showAwardButton && request?.status == BidStatus.OPEN,
                            isAwarding = detailState.isLoading,
                            onAward = { viewModel.awardBid(bid.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuctionMetric(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ModernBidRow(
    bid: Bid,
    rank: Int,
    totalWeightKg: Float,
    showAward: Boolean,
    isAwarding: Boolean,
    onAward: () -> Unit
) {
    val isTopBid = rank == 1
    Surface(
        color = if (isTopBid) EmeraldContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, if (isTopBid) EmeraldPrimary.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
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
                    shape = CircleShape,
                    color = if (isTopBid) EmeraldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isTopBid) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Column {
                    Text(
                        text = bid.recyclerName.ifBlank { "Recycler #${bid.recyclerId.take(6).uppercase()}" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total: ₹${String.format("%.0f", bid.pricePerKg * totalWeightKg)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "₹${bid.pricePerKg}/kg",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = EmeraldPrimary
                )

                if (showAward) {
                    Button(
                        onClick = onAward,
                        enabled = !isAwarding,
                        modifier = Modifier.height(36.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(stringResource(R.string.award), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
