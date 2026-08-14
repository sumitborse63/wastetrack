package com.sktech.wastetrack.ui.screens.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.ScrapCategory
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
                title = { Text(stringResource(R.string.enterprise_analytics), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    EPRTargetCard(compliancePercentage = state.eprCompliancePercentage)
                }

                item {
                    EfficiencyScoreCard(score = state.efficiencyScore)
                }
                
                item {
                    TrendChartCard(data = state.monthlyScrapKg)
                }

                item {
                    CategoryBreakdownCard(breakdown = state.categoryBreakdown)
                }
            }
        }
    }
}

@Composable
fun EPRTargetCard(compliancePercentage: Float) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.epr_ledger_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                stringResource(R.string.epr_mandate_subtitle), 
                style = MaterialTheme.typography.bodySmall, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val isCompliant = compliancePercentage >= 75f
            val progressColor = if (isCompliant) IndustrialGreen else SafetyOrange
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { compliancePercentage / 100f },
                    modifier = Modifier.weight(1f).height(12.dp),
                    color = progressColor,
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    "${compliancePercentage.toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            if (isCompliant) {
                Text(
                    stringResource(R.string.epr_compliant_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = IndustrialGreen
                )
            } else {
                Text(
                    stringResource(R.string.epr_risk_desc),
                    style = MaterialTheme.typography.labelSmall,
                    color = AlertRed
                )
            }
        }
    }
}

@Composable
fun EfficiencyScoreCard(score: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.efficiency_score_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.size(120.dp),
                    strokeWidth = 12.dp,
                    color = IndustrialGreen,
                    trackColor = MaterialTheme.colorScheme.surface
                )
                Text(
                    text = "$score%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Based on Honeywell Forge Asset Health", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TrendChartCard(data: List<Float>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.trend_chart_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(24.dp))
            
            // Simple Bar Chart
            val maxVal = data.maxOrNull() ?: 1f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { value ->
                    val heightRatio = value / maxVal
                    val barColor = MaterialTheme.colorScheme.primary
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .fillMaxHeight(heightRatio)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRoundRect(
                                color = barColor,
                                size = Size(size.width, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryBreakdownCard(breakdown: Map<String, Float>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(stringResource(R.string.category_breakdown_title), style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            breakdown.forEach { (categoryStr, percentage) ->
                val categoryEnum = runCatching { ScrapCategory.valueOf(categoryStr) }.getOrNull()
                val catName = if (categoryEnum != null) stringResource(categoryEnum.nameRes) else categoryStr
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(catName, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    LinearProgressIndicator(
                        progress = { percentage / 100f },
                        modifier = Modifier.weight(2f).height(8.dp),
                        strokeCap = StrokeCap.Round,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text("${percentage.toInt()}%", modifier = Modifier.width(40.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

