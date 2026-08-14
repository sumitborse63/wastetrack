package com.sktech.wastetrack.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sktech.wastetrack.R
import com.sktech.wastetrack.data.local.db.entity.TransferEntity
import com.sktech.wastetrack.data.local.db.entity.BidRequestEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecyclerDashboardScreen(
    onNavigateToBids: () -> Unit,
    onNavigateToTransfer: () -> Unit,
    onNavigateToCompliance: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFleet: () -> Unit,
    viewModel: RecyclerDashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showPickupDialog by remember { mutableStateOf<BidRequestEntity?>(null) }
    var showVerifyDialog by remember { mutableStateOf<TransferEntity?>(null) }

    showPickupDialog?.let { request ->
        InitiatePickupDialog(
            request = request,
            onConfirm = { vehicleNo ->
                viewModel.initiatePickup(request.id, vehicleNo)
                showPickupDialog = null
            },
            onDismiss = { showPickupDialog = null }
        )
    }

    showVerifyDialog?.let { transfer ->
        VerifyWeightDialog(
            transfer = transfer,
            onConfirm = { weight ->
                viewModel.verifyReceivedWeight(transfer.id, weight)
                showVerifyDialog = null
            },
            onDismiss = { showVerifyDialog = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.fillMaxWidth().padding(end = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Filled.Recycling,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = state.currentUser?.organizationName?.ifBlank { stringResource(R.string.recycler_overview) } ?: stringResource(R.string.recycler_overview),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = "${stringResource(R.string.mpcb_approved)} · ${state.currentUser?.industrialArea.orEmpty().ifBlank { stringResource(R.string.midc_zone_default) }}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = EmeraldContainer,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text(
                            stringResource(R.string.certified_badge),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Message Banners
            state.successMessage?.let { msg ->
                Surface(
                    color = EmeraldContainer,
                    shape = MaterialTheme.shapes.medium,
                    border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
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
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Continuously Looping Live Scrap Bidding Lots Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = CircleShape, color = AlertRed, modifier = Modifier.size(8.dp)) {}
                    Text(
                        text = stringResource(R.string.live_micro_auctions),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
                TextButton(onClick = onNavigateToBids) {
                    Text(stringResource(R.string.view_market), color = EmeraldPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Auto-scrolling Continuous Live Carousel
            AutoScrollingAuctionCarousel(
                openAuctions = state.openAuctions,
                onBidClick = onNavigateToBids
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Stats Overview Title
            Text(
                text = stringResource(R.string.recycling_procurement_metrics),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Metrics Carousel
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    RecyclerStatCard(
                        title = stringResource(R.string.auctions_won),
                        value = "${state.wonAuctions.size}",
                        subtitle = stringResource(R.string.ready_for_pickup),
                        icon = Icons.Outlined.Storefront,
                        accentColor = EmeraldPrimary,
                        containerColor = EmeraldContainer
                    )
                }
                item {
                    RecyclerStatCard(
                        title = stringResource(R.string.inbound_fleet),
                        value = "${state.incomingShipments.size}",
                        subtitle = stringResource(R.string.trucks_in_transit),
                        icon = Icons.Outlined.Navigation,
                        accentColor = Teal,
                        containerColor = TealContainer
                    )
                }
                item {
                    RecyclerStatCard(
                        title = stringResource(R.string.total_recycled),
                        value = "${String.format("%.0f", state.totalWeightRecycledKg)} kg",
                        subtitle = stringResource(R.string.verified_tonnage),
                        icon = Icons.Outlined.FactCheck,
                        accentColor = SyncBlue,
                        containerColor = TealContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Quick Actions Grid
            Text(
                text = stringResource(R.string.quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RecyclerQuickActionCard(
                    title = stringResource(R.string.bid_market),
                    subtitle = stringResource(R.string.browse_factory_lots),
                    icon = Icons.Outlined.Storefront,
                    accentColor = EmeraldPrimary,
                    containerColor = EmeraldContainer,
                    onClick = onNavigateToBids,
                    modifier = Modifier.weight(1f)
                )
                RecyclerQuickActionCard(
                    title = stringResource(R.string.fleet_tracker),
                    subtitle = stringResource(R.string.live_inbound_trucks),
                    icon = Icons.Outlined.LocalShipping,
                    accentColor = Teal,
                    containerColor = TealContainer,
                    onClick = onNavigateToFleet,
                    modifier = Modifier.weight(1f)
                )
                RecyclerQuickActionCard(
                    title = stringResource(R.string.epr_certs),
                    subtitle = stringResource(R.string.form10_ledger),
                    icon = Icons.Outlined.Verified,
                    accentColor = Gold,
                    containerColor = SafetyOrangeContainer,
                    onClick = onNavigateToCompliance,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Won Auctions Requiring Truck Dispatch
            if (state.wonAuctions.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.won_auctions_dispatch_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.wonAuctions.forEach { request ->
                        PendingPickupCard(
                            request = request,
                            onInitiate = { showPickupDialog = request }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Active Inbound Shipments
            if (state.incomingShipments.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.inbound_shipments_weighbridge_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.incomingShipments.forEach { transfer ->
                        InTransitCard(
                            transfer = transfer,
                            onVerify = { showVerifyDialog = transfer }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun AutoScrollingAuctionCarousel(
    openAuctions: List<BidRequestEntity>,
    onBidClick: () -> Unit
) {
    val listState = rememberLazyListState()

    // Default mock lots if database is fresh
    val displayLots = if (openAuctions.isNotEmpty()) openAuctions else listOf(
        BidRequestEntity(id = "lot-1", factoryId = "Ambad MIDC Forgings", scrapEntryId = "e1", scrapCategory = "METAL", estimatedWeightKg = 2400f, reservePricePerKg = 48f, auctionStartTime = System.currentTimeMillis(), auctionEndTime = System.currentTimeMillis() + 86400000L, status = "OPEN"),
        BidRequestEntity(id = "lot-2", factoryId = "Nashik Polymers Unit 4", scrapEntryId = "e2", scrapCategory = "PLASTIC", estimatedWeightKg = 1200f, reservePricePerKg = 26f, auctionStartTime = System.currentTimeMillis(), auctionEndTime = System.currentTimeMillis() + 86400000L, status = "OPEN"),
        BidRequestEntity(id = "lot-3", factoryId = "Techno E-Waste Hub", scrapEntryId = "e3", scrapCategory = "EWASTE", estimatedWeightKg = 450f, reservePricePerKg = 135f, auctionStartTime = System.currentTimeMillis(), auctionEndTime = System.currentTimeMillis() + 86400000L, status = "OPEN"),
        BidRequestEntity(id = "lot-4", factoryId = "Maharashtra Paper Mills", scrapEntryId = "e4", scrapCategory = "PAPER", estimatedWeightKg = 3100f, reservePricePerKg = 16f, auctionStartTime = System.currentTimeMillis(), auctionEndTime = System.currentTimeMillis() + 86400000L, status = "OPEN")
    )

    // Continuously loop/scroll automatically
    LaunchedEffect(displayLots) {
        while (true) {
            delay(3500)
            val nextIndex = (listState.firstVisibleItemIndex + 1) % displayLots.size
            listState.animateScrollToItem(nextIndex)
        }
    }

    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(displayLots, key = { it.id }) { lot ->
            val category = runCatching { ScrapCategory.valueOf(lot.scrapCategory) }.getOrDefault(ScrapCategory.METAL)
            Surface(
                modifier = Modifier
                    .width(260.dp)
                    .height(200.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(onClick = onBidClick),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp)
                    ) {
                        AsyncImage(
                            model = category.sampleImageUrl,
                            contentDescription = stringResource(category.nameRes),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                    )
                                )
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.padding(8.dp).align(Alignment.TopStart)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(category.icon, fontSize = 11.sp)
                                Text(stringResource(category.nameRes), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = SafetyOrange,
                            modifier = Modifier.padding(8.dp).align(Alignment.TopEnd)
                        ) {
                            Text(
                                stringResource(R.string.live_auction_badge),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Text(
                            text = stringResource(R.string.weight_metric_ton_format, lot.estimatedWeightKg, lot.estimatedWeightKg / 1000f),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(R.string.reserve_price_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${lot.reservePricePerKg}/kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = EmeraldPrimary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(stringResource(R.string.lot_value_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("₹${String.format("%.0f", lot.estimatedWeightKg * lot.reservePricePerKg)}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Button(
                            onClick = onBidClick,
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.Gavel, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.place_competitive_bid), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecyclerStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color
) {
    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(115.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RecyclerQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(105.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
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
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = containerColor,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PendingPickupCard(
    request: BidRequestEntity,
    onInitiate: () -> Unit
) {
    val category = runCatching { ScrapCategory.valueOf(request.scrapCategory) }.getOrDefault(ScrapCategory.OTHER)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                AsyncImage(
                    model = category.sampleImageUrl,
                    contentDescription = stringResource(category.nameRes),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = EmeraldPrimary,
                    modifier = Modifier.padding(10.dp).align(Alignment.TopEnd)
                ) {
                    Text(
                        stringResource(R.string.won_auction_badge),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Text(
                    text = "${stringResource(category.nameRes)} · ${request.estimatedWeightKg} kg",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.BottomStart).padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.origin_factory_format, request.factoryId),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onInitiate,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Icon(Icons.Filled.LocalShipping, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.dispatch_truck_assign_driver), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun InTransitCard(
    transfer: TransferEntity,
    onVerify: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.vehicle_format, transfer.vehicleNumber),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stringResource(R.string.dispatched_label)}: ${DateUtils.formatTime(transfer.initiatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = TealContainer
                ) {
                    Text(
                        text = "${transfer.weightAtSource} kg",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Teal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onVerify,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.verify_arrival_weighbridge), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun InitiatePickupDialog(
    request: BidRequestEntity,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var vehicleNo by remember { mutableStateOf("") }
    val category = runCatching { ScrapCategory.valueOf(request.scrapCategory) }.getOrDefault(ScrapCategory.OTHER)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.assign_truck_pickup_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.factory_pickup_format, request.factoryId, request.estimatedWeightKg, stringResource(category.nameRes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = vehicleNo,
                    onValueChange = { vehicleNo = it.uppercase() },
                    label = { Text(stringResource(R.string.vehicle_reg_no)) },
                    placeholder = { Text(stringResource(R.string.vehicle_reg_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(vehicleNo) },
                enabled = vehicleNo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text(stringResource(R.string.confirm_dispatch))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun VerifyWeightDialog(
    transfer: TransferEntity,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf("") }
    val weight = weightText.toFloatOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weighbridge_verification_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.truck_dispatched_format, transfer.vehicleNumber, transfer.weightAtSource),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.measured_dest_weight_kg)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { weight?.let(onConfirm) },
                enabled = weight != null && weight > 0f,
                colors = ButtonDefaults.buttonColors(containerColor = Teal)
            ) {
                Text(stringResource(R.string.verify_complete_handshake))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
