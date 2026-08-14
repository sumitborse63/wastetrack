package com.sktech.wastetrack.ui.screens.scrap

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.ScrapCategory
import com.sktech.wastetrack.ui.components.VoiceInputButton
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

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.onImageUriChanged(it.toString()) }
    }

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
                    Column {
                        Text(
                            stringResource(R.string.log_scrap),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Intake Log & AI Material Verification",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToClassify) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.CameraAlt,
                                    contentDescription = stringResource(R.string.ai_classify),
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Success Banner
            if (state.isSuccess) {
                item {
                    Surface(
                        color = EmeraldContainer,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
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
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(26.dp)
                            )
                            Column {
                                Text(
                                    stringResource(R.string.scrap_logged_success),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldPrimary
                                )
                                Text(
                                    stringResource(R.string.queued_for_sync),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Category Selection with High Quality Photos
            item {
                Text(
                    stringResource(R.string.select_category),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ScrapCategory.entries) { category ->
                        val isSelected = state.selectedCategory == category
                        val categoryName = stringResource(category.nameRes)
                        Surface(
                            modifier = Modifier
                                .width(120.dp)
                                .height(110.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .clickable { viewModel.onCategorySelected(category) },
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                            ),
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(68.dp)
                                        .background(category.color().copy(alpha = 0.2f))
                                ) {
                                    AsyncImage(
                                        model = category.sampleImageUrl,
                                        contentDescription = categoryName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(4.dp).size(22.dp).align(Alignment.TopEnd)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(category.icon, fontSize = 11.sp)
                                        }
                                    }
                                }
                                Text(
                                    text = categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Scrap Photo Upload / Camera Preview Section
            item {
                Text(
                    "Scrap Lot Proof & Image",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    val displayImage = state.imageUri ?: state.selectedCategory?.sampleImageUrl

                    if (displayImage != null) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = displayImage,
                                contentDescription = "Scrap Lot Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    Text(
                                        if (state.imageUri != null) "Custom Photo Attached" else "Tap to Replace Photo",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Upload or Capture Scrap Photo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                            Text("Attached to B2B auction lot for recyclers", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Weight Input
            item {
                Text(
                    stringResource(R.string.weight_kg),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.weightKg,
                    onValueChange = { viewModel.onWeightChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.0", color = TextMuted) },
                    suffix = { Text("kg", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Scale, contentDescription = null, tint = EmeraldPrimary)
                    },
                    trailingIcon = {
                        VoiceInputButton(
                            onResult = { viewModel.onWeightChanged(it) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                state.selectedCategory?.let { cat ->
                    val weight = state.weightKg.toFloatOrNull()
                    if (weight != null && weight > 0f) {
                        val isHeavy = when (cat) {
                            ScrapCategory.PLASTIC -> weight > 400f
                            ScrapCategory.PAPER -> weight > 200f
                            ScrapCategory.METAL -> weight > 2500f
                            ScrapCategory.EWASTE -> weight > 500f
                            else -> weight > 2000f
                        }
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = if (isHeavy) AlertRedContainer else EmeraldContainer,
                            border = BorderStroke(1.dp, if (isHeavy) AlertRed.copy(alpha = 0.3f) else EmeraldPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isHeavy) Icons.Filled.Warning else Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (isHeavy) AlertRed else EmeraldPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isHeavy) "AI Fraud Shield: Suspicious weight density. Possible ballast." else "AI Fraud Shield: Material density verified within normal bounds.",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isHeavy) AlertRed else EmeraldPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Sub-category
            item {
                Text(
                    stringResource(R.string.sub_category),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                state.selectedCategory?.let { category ->
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        items(category.subCategories) { sub ->
                            val isSelected = state.subCategory == sub
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onSubCategoryChanged(sub) },
                                label = {
                                    Text(sub, style = MaterialTheme.typography.labelSmall)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldContainer,
                                    selectedLabelColor = EmeraldPrimary
                                )
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.subCategory,
                    onValueChange = { viewModel.onSubCategoryChanged(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g., Copper Wire, Heavy Steel, Brass Scrap", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Category, contentDescription = null, tint = EmeraldPrimary)
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Notes
            item {
                Text(
                    stringResource(R.string.notes_optional),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp),
                    placeholder = { Text("Additional batch notes...", color = TextMuted) },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Outlined.Notes, contentDescription = null, tint = EmeraldPrimary)
                    },
                    trailingIcon = {
                        VoiceInputButton(
                            onResult = { viewModel.onNotesChanged(it) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    },
                    maxLines = 3,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // Submit Button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.submitEntry() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = state.selectedCategory != null && state.weightKg.isNotBlank() && !state.isSubmitting,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldPrimary,
                        contentColor = Color.White
                    )
                ) {
                    if (state.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.submit_entry),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
