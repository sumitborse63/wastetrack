package com.sktech.wastetrack.ui.screens.bin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.data.local.db.entity.BinEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.screens.scrap.color
import com.sktech.wastetrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BinMonitorScreen(
    onNavigateBack: () -> Unit,
    viewModel: BinViewModel = hiltViewModel()
) {
    val bins by viewModel.bins.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Bin Monitor",
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
            FloatingActionButton(
                onClick = { viewModel.addSampleBin() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            }
        }
    ) { padding ->
        if (bins.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Inventory2,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = SafetyOrange.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No Bins Configured",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Add waste bins to monitor fill levels",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(bins) { bin ->
                    BinCard(bin = bin, onClick = { viewModel.updateBinFill(bin.id) })
                }
            }
        }
    }
}

@Composable
private fun BinCard(bin: BinEntity, onClick: () -> Unit) {
    val fillColor = when {
        bin.fillPercentage >= 85f -> AlertRed
        bin.fillPercentage >= 60f -> SafetyOrange
        else -> IndustrialGreenLight
    }

    val category = try {
        ScrapCategory.valueOf(bin.scrapCategory)
    } catch (e: Exception) {
        ScrapCategory.OTHER
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Category icon
            Text(category.icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                category.displayName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Fill level indicator
            CircularProgressIndicator(
                progress = { bin.fillPercentage / 100f },
                modifier = Modifier.size(64.dp),
                color = fillColor,
                trackColor = fillColor.copy(alpha = 0.15f),
                strokeWidth = 6.dp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "${bin.fillPercentage.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = fillColor
            )
            Text(
                "${bin.currentFillKg}/${bin.capacityKg} kg",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
