package com.sktech.wastetrack.ui.screens.bid

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.domain.model.Bid
import com.sktech.wastetrack.domain.model.BidRequest
import com.sktech.wastetrack.domain.model.ScrapCategory
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

    LaunchedEffect(bidRequestId) {
        viewModel.loadBidDetail(bidRequestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Bid Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (detailState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val request = detailState.bidRequest
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                // Request summary card
                if (request != null) {
                    item {
                        val category = try { ScrapCategory.valueOf(request.scrapCategory) } catch (_: Exception) { ScrapCategory.OTHER }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = category.color().copy(alpha = 0.15f),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(category.icon, style = MaterialTheme.typography.headlineSmall)
                                        }
                                    }
                                    Column {
                                        Text(
                                            "${category.displayName} Scrap Auction",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "ID: ${request.id.take(8)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    InfoColumn("Weight", "${request.estimatedWeightKg} kg")
                                    InfoColumn("Reserve", "₹${request.reservePricePerKg}/kg")
                                    InfoColumn("Status", request.status.name)
                                    InfoColumn("Bids", "${detailState.bids.size}")
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Outlined.Timer, null, Modifier.size(16.dp), tint = SafetyOrange)
                                    Text(
                                        DateUtils.getRemainingTimeString(request.auctionEndTime),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SafetyOrange
                                    )
                                }
                            }
                        }
                    }
                }

                // Bids header
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Recycler Bids", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                // Best price summary
                val bestBid = detailState.bids.maxByOrNull { it.pricePerKg }
                if (bestBid != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = IndustrialGreen.copy(alpha = 0.1f)
                            ),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Filled.EmojiEvents, null, tint = Gold, modifier = Modifier.size(28.dp))
                                    Column {
                                        Text("Best Offer", style = MaterialTheme.typography.labelMedium, color = IndustrialGreenLight)
                                        Text(bestBid.recyclerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "₹${String.format("%.1f", bestBid.pricePerKg)}/kg",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = IndustrialGreenLight
                                    )
                                    Text(
                                        "Total: ₹${String.format("%.0f", bestBid.totalBidAmount)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // All bids
                items(detailState.bids.sortedByDescending { it.pricePerKg }, key = { it.id }) { bid ->
                    BidCard(
                        bid = bid,
                        isTop = bid.id == bestBid?.id,
                        onAward = {
                            if (request != null) viewModel.awardBid(bid.id, request.id)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BidCard(bid: Bid, isTop: Boolean, onAward: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (bid.isWinning)
                IndustrialGreen.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
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
                    color = if (isTop) Gold.copy(alpha = 0.15f) else SteelGray.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isTop) Icons.Filled.EmojiEvents else Icons.Outlined.Business,
                            null,
                            tint = if (isTop) Gold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column {
                    Text(bid.recyclerName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Bid at ${DateUtils.formatTime(bid.submittedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "₹${String.format("%.1f", bid.pricePerKg)}/kg",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isTop) IndustrialGreenLight else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "₹${String.format("%.0f", bid.totalBidAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (bid.isWinning) {
                    Surface(
                        shape = MaterialTheme.shapes.extraSmall,
                        color = IndustrialGreenLight.copy(alpha = 0.15f),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            "WINNER",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = IndustrialGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!bid.isWinning) {
                    TextButton(onClick = onAward, contentPadding = PaddingValues(0.dp)) {
                        Text("Award", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
