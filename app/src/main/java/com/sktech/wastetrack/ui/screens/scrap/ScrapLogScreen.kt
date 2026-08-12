package com.sktech.wastetrack.ui.screens.scrap

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.DateUtils

fun ScrapCategory.color(): Color = when (this) {
    ScrapCategory.METAL -> MetalColor
    ScrapCategory.PLASTIC -> PlasticColor
    ScrapCategory.RUBBER -> RubberColor
    ScrapCategory.EWASTE -> EWasteColor
    ScrapCategory.CHEMICAL -> ChemicalColor
    ScrapCategory.WOOD -> WoodColor
    ScrapCategory.PAPER -> PaperColor
    ScrapCategory.GLASS -> GlassColor
    ScrapCategory.OTHER -> OtherColor
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScrapLogScreen(
    onNavigateBack: () -> Unit,
    onNavigateToClassify: () -> Unit,
    viewModel: ScrapLogViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            kotlinx.coroutines.delay(1500)
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Log Scrap Entry",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToClassify) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = "AI Classify",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Success Banner
            if (state.isSuccess) {
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
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = IndustrialGreenLight,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    "Scrap entry logged successfully!",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IndustrialGreenLight
                                )
                                Text(
                                    "Queued for sync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IndustrialGreenLight.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            // Category Selection
            item {
                Text(
                    "Scrap Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ScrapCategory.entries) { category ->
                        val isSelected = state.selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onCategorySelected(category) },
                            label = {
                                Text(
                                    "${category.icon} ${category.displayName}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = category.color().copy(alpha = 0.2f),
                                selectedLabelColor = category.color()
                            ),
                            border = if (isSelected) FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = category.color()
                            ) else null,
                            modifier = Modifier.height(48.dp)
                        )
                    }
                }
            }

            // Weight Input
            item {
                Text(
                    "Weight (kg)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.weightKg,
                    onValueChange = { viewModel.onWeightChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    placeholder = { Text("Enter weight in kilograms") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Scale, contentDescription = null)
                    },
                    suffix = { Text("kg", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Sub-category
            item {
                Text(
                    "Sub-Category (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.subCategory,
                    onValueChange = { viewModel.onSubCategoryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., brass turnings, HDPE defective") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Category, contentDescription = null)
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Notes
            item {
                Text(
                    "Notes (Optional)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = { Text("Additional notes about this scrap batch...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Notes, contentDescription = null)
                    },
                    maxLines = 3,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Error
            if (state.error != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = AlertRed.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Error,
                                contentDescription = null,
                                tint = AlertRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                state.error!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = AlertRed
                            )
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = { viewModel.submitEntry() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isSubmitting && !state.isSuccess,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Log Scrap Entry",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Recent entries header
            if (state.recentEntries.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Recent Entries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(state.recentEntries) { entry ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val cat = try {
                                    ScrapCategory.valueOf(entry.category)
                                } catch (e: Exception) {
                                    ScrapCategory.OTHER
                                }
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = cat.color().copy(alpha = 0.15f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(cat.icon, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                Column {
                                    Text(
                                        cat.displayName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        DateUtils.getRelativeTimeString(entry.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${entry.weightKg} kg",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    imageVector = if (entry.syncStatus == "SYNCED")
                                        Icons.Filled.CloudDone else Icons.Filled.CloudOff,
                                    contentDescription = entry.syncStatus,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (entry.syncStatus == "SYNCED")
                                        IndustrialGreenLight else SafetyOrange
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
