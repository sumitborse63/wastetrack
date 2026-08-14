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
    onNavigateToTransfer: () -> Unit = {},
    onNavigateToFleet: () -> Unit = {},
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
    var pendingAwardBid by remember { mutableStateOf<Bid?>(null) }

    val marketState by viewModel.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    marketState.successMessage?.let { msg ->
        LaunchedEffect(msg) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearSuccess()
        }
    }

    // Award confirmation dialog
    if (pendingAwardBid != null) {
        val selectedBid = pendingAwardBid!!
        val totalValueStr = String.format("%.0f", selectedBid.pricePerKg * (request?.estimatedWeightKg ?: 0f))
        AlertDialog(
            onDismissRequest = { pendingAwardBid = null },
            icon = { Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(32.dp)) },
            title = {
                Text(
                    stringResource(R.string.confirm_award_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.confirm_award_desc,
                        selectedBid.recyclerName.ifBlank { "Certified Recycler" },
                        selectedBid.pricePerKg.toString(),
                        totalValueStr
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bidToAward = selectedBid
                        pendingAwardBid = null
                        viewModel.awardBid(bidToAward.id, bidRequestId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    ),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(stringResource(R.string.confirm_award_btn), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingAwardBid = null }) {
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
                            stringResource(R.string.bid_detail),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.auction_batch_format, bidRequestId.take(8).uppercase()),
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
                val validBidErrorMsg = stringResource(R.string.enter_valid_bid_rate)
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
                                        localError = validBidErrorMsg
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
                                    color = if (request.status == BidStatus.OPEN) SafetyOrangeContainer else EmeraldContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (request.status == BidStatus.OPEN) Icons.Outlined.Timer else Icons.Filled.CheckCircle,
                                            null,
                                            tint = if (request.status == BidStatus.OPEN) SafetyOrange else EmeraldPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (request.status == BidStatus.OPEN) {
                                                stringResource(R.string.closing_in_format, DateUtils.getRemainingTimeString(request.auctionEndTime))
                                            } else {
                                                stringResource(R.string.auction_concluded)
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (request.status == BidStatus.OPEN) SafetyOrange else EmeraldPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Awarded Dispatch Banner
                if (request != null && request.status == BidStatus.AWARDED) {
                    val winningBid = detailState.bids.find { it.isWinning } ?: detailState.bids.firstOrNull()
                    item {
                        Surface(
                            color = EmeraldContainer,
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Gold, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text(
                                            text = "AUCTION AWARDED & DISPATCHED",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = EmeraldPrimary
                                        )
                                        Text(
                                            text = "Winner: ${winningBid?.recyclerName ?: "Certified Recycler"} · ₹${winningBid?.pricePerKg ?: request.reservePricePerKg}/kg",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = onNavigateToTransfer,
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Transfers", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = onNavigateToFleet,
                                        modifier = Modifier.weight(1f).height(38.dp),
                                        shape = MaterialTheme.shapes.small,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                                    ) {
                                        Icon(Icons.Filled.Navigation, null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Live Fleet", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
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
                                text = stringResource(R.string.highest_bid_format, detailState.bids.maxOf { it.pricePerKg }.toString()),
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
                            onAward = { pendingAwardBid = bid }
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
    val isWinning = bid.isWinning
    Surface(
        color = when {
            isWinning -> EmeraldContainer
            isTopBid -> EmeraldContainer.copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(
            1.dp,
            when {
                isWinning -> EmeraldPrimary
                isTopBid -> EmeraldPrimary.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.outline
            }
        ),
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
                    color = when {
                        isWinning -> Gold
                        isTopBid -> EmeraldPrimary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = if (isWinning) "★" else "#$rank",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isWinning) Color.Black else if (isTopBid) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = bid.recyclerName.ifBlank { stringResource(R.string.recycler_id_format, bid.recyclerId.take(6).uppercase()) },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isWinning) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = EmeraldPrimary
                            ) {
                                Text(
                                    text = "WINNER",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = stringResource(R.string.total_bid_value_format, String.format("%.0f", bid.pricePerKg * totalWeightKg)),
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
