package com.sktech.wastetrack.ui.screens.scrap

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.data.local.db.entity.ScrapEntryEntity
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapHistoryScreen(
    onNavigateBack: () -> Unit,
    viewModel: ScrapLogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedFilter by remember { mutableStateOf<ScrapCategory?>(null) }

    val filteredEntries = if (selectedFilter != null) {
        state.recentEntries.filter { it.category == selectedFilter!!.name }
    } else {
        state.recentEntries
    }

    // Compute stats
    val totalWeight = filteredEntries.sumOf { it.weightKg.toDouble() }
    val totalEntries = filteredEntries.size
    val anomalyCount = filteredEntries.count { it.anomalyFlagged }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scrap History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Summary stats
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HistoryStatCard("Entries", "$totalEntries", IndustrialGreenLight, Modifier.weight(1f))
                    HistoryStatCard("Total Weight", "${String.format("%.1f", totalWeight)} kg", Teal, Modifier.weight(1f))
                    HistoryStatCard("Anomalies", "$anomalyCount", if (anomalyCount > 0) AlertRed else SteelGray, Modifier.weight(1f))
                }
            }

            // Category filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All") },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                    items(ScrapCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedFilter == cat,
                            onClick = { selectedFilter = if (selectedFilter == cat) null else cat },
                            label = { Text("${cat.icon} ${cat.displayName}") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = cat.color().copy(alpha = 0.2f),
                                selectedLabelColor = cat.color()
                            ),
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }

            // Entries
            if (filteredEntries.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Inventory, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No entries found", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
            }

            items(filteredEntries, key = { it.id }) { entry ->
                HistoryEntryCard(entry = entry)
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HistoryStatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)), shape = MaterialTheme.shapes.medium) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: ScrapEntryEntity) {
    val cat = try { ScrapCategory.valueOf(entry.category) } catch (_: Exception) { ScrapCategory.OTHER }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (entry.anomalyFlagged)
                AlertRed.copy(alpha = 0.06f)
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = MaterialTheme.shapes.small, color = cat.color().copy(alpha = 0.15f), modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(cat.icon, style = MaterialTheme.typography.titleMedium) }
                    }
                    Column {
                        Text(cat.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        if (entry.subCategory.isNotBlank()) {
                            Text(entry.subCategory, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(DateUtils.formatDateTime(entry.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${entry.weightKg} kg", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            if (entry.syncStatus == "SYNCED") Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                            null, modifier = Modifier.size(14.dp),
                            tint = if (entry.syncStatus == "SYNCED") IndustrialGreenLight else SafetyOrange
                        )
                        Text(entry.syncStatus, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (entry.anomalyFlagged) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(color = AlertRed.copy(alpha = 0.1f), shape = MaterialTheme.shapes.extraSmall) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Warning, null, tint = AlertRed, modifier = Modifier.size(14.dp))
                        Text("Anomaly detected · Score: ${String.format("%.2f", entry.anomalyScore)}", style = MaterialTheme.typography.labelSmall, color = AlertRed)
                    }
                }
            }
            if (entry.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("\"${entry.notes}\"", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // Audit hash
            Spacer(modifier = Modifier.height(4.dp))
            Text("Hash: ${entry.contentHash.take(16)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
        }
    }
}
