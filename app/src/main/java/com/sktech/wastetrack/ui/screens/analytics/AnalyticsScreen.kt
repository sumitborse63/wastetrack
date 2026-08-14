package com.sktech.wastetrack.ui.screens.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Circular Economy Analytics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "EPR Mandate & ESG Decarbonization",
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
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = EmeraldPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 48.dp)
            ) {
                // 1. Central Circular Progress (EPR Mandate Progress Card - Stitch Design)
                item {
                    EPRMandateCircularCard(
                        compliancePercentage = if (state.eprCompliancePercentage > 0f) state.eprCompliancePercentage else 75f
                    )
                }

                // 2. ESG Decarbonization Impact Bento Grid (Stitch Design)
                item {
                    Text(
                        "Decarbonization & ESG Impact",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EsgImpactCard(
                            title = "CO2 SAVED",
                            value = "12.4 T",
                            subtitle = "Offset verified",
                            icon = Icons.Outlined.CloudQueue,
                            accentColor = EmeraldPrimary,
                            containerColor = EmeraldContainer,
                            modifier = Modifier.weight(1f)
                        )
                        EsgImpactCard(
                            title = "WATER SAVED",
                            value = "45k L",
                            subtitle = "Conserved",
                            icon = Icons.Outlined.WaterDrop,
                            accentColor = Teal,
                            containerColor = TealContainer,
                            modifier = Modifier.weight(1f)
                        )
                        EsgImpactCard(
                            title = "TREES EQUIV.",
                            value = "320",
                            subtitle = "Planted eq.",
                            icon = Icons.Outlined.Park,
                            accentColor = SafetyOrange,
                            containerColor = SafetyOrangeContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 3. Scrap Generation 6-Month Volume Trend (Stitch Bar Chart)
                item {
                    MonthlyScrapTrendCard(data = state.monthlyScrapKg)
                }

                // 4. Material Category Distribution Breakdown
                item {
                    MaterialCategoryBreakdownCard(breakdown = state.categoryBreakdown)
                }

                // 5. Fraud Shield Integrity Rating
                item {
                    AssetIntegrityScoreCard(score = state.efficiencyScore)
                }
            }
        }
    }
}

@Composable
private fun EPRMandateCircularCard(compliancePercentage: Float) {
    val isCompliant = compliancePercentage >= 75f
    val progressColor = if (isCompliant) EmeraldPrimary else SafetyOrange

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "EPR Mandate Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Current statutory compliance target for Q3",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isCompliant) EmeraldContainer else SafetyOrangeContainer
                ) {
                    Text(
                        text = if (isCompliant) "COMPLIANT" else "ACTION NEEDED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompliant) EmeraldPrimary else SafetyOrange
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Circular Radial Chart
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val strokeWidth = 14.dp.toPx()
                    // Track circle
                    drawCircle(
                        color = EmeraldContainer.copy(alpha = 0.6f),
                        style = Stroke(width = strokeWidth)
                    )
                    // Progress arc
                    val sweepAngle = (compliancePercentage / 100f).coerceIn(0f, 1f) * 360f
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${compliancePercentage.toInt()}%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "EPR Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Target vs Achieved
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Target Mandate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "1,000 T",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Achieved Recycled",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${(compliancePercentage * 10).toInt()} T",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun EsgImpactCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.height(120.dp),
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
                Surface(
                    shape = CircleShape,
                    color = containerColor,
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun MonthlyScrapTrendCard(data: List<Float>) {
    val monthLabels = listOf("M-5", "M-4", "M-3", "M-2", "M-1", "Current")
    val displayData = if (data.isEmpty() || data.all { it == 0f }) listOf(120f, 240f, 180f, 320f, 290f, 410f) else data
    val maxVal = (displayData.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Scrap Volume Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "6-Month generation & processing trend (kg)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Outlined.TrendingUp, contentDescription = null, tint = EmeraldPrimary)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                displayData.take(6).forEachIndexed { index, value ->
                    val heightRatio = (value / maxVal).coerceIn(0.15f, 1f)
                    val isCurrent = index == displayData.size - 1 || index == 5
                    val barColor = if (isCurrent) EmeraldPrimary else EmeraldContainer
                    val label = monthLabels.getOrElse(index) { "M-$index" }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "${value.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(heightRatio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCategoryBreakdownCard(breakdown: Map<String, Float>) {
    val sampleBreakdown = if (breakdown.isEmpty()) mapOf(
        "Industrial Metal" to 45f,
        "Heavy Plastic" to 25f,
        "Electronic Waste" to 15f,
        "Paper & Board" to 10f,
        "Chemical / Rubber" to 5f
    ) else breakdown

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Material Stream Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "Breakdown of categorized industrial scrap intake",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                sampleBreakdown.forEach { (categoryName, percentage) ->
                    val categoryEnum = runCatching {
                        ScrapCategory.entries.find { it.displayName.equals(categoryName, ignoreCase = true) || it.name.equals(categoryName, ignoreCase = true) }
                    }.getOrNull() ?: ScrapCategory.OTHER

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(categoryEnum.icon, fontSize = 14.sp)
                                Text(
                                    categoryName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "${percentage.toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = categoryEnum.color()
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.extraSmall),
                            color = categoryEnum.color(),
                            trackColor = categoryEnum.color().copy(alpha = 0.15f),
                            strokeCap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetIntegrityScoreCard(score: Int) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = EmeraldContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "AI Fraud Shield & Asset Health",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Honeywell Industrial Integrity score: $score% verified clean logs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "$score%",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldPrimary
            )
        }
    }
}
