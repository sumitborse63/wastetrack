package com.sktech.wastetrack.ui.screens.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onProfileCompleted: () -> Unit,
    viewModel: SignUpViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onProfileCompleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Organization Registration",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Profile Setup for ${state.role.fullName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))

            // Pre-selected Role Confirmation Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = EmeraldContainer,
                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = EmeraldPrimary,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (state.role == UserRole.RECYCLER) Icons.Filled.Recycling else Icons.Filled.Factory,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.role.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldPrimary
                        )
                        Text(
                            text = if (state.role == UserRole.RECYCLER)
                                "B2B Scrap Auctions, Live Weighbridge, and Fleet Truck Pickup"
                            else
                                "Shop Floor Scrap Logging, IoT Telemetry, and Gate Pass Handshake",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Setup Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.error != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = AlertRedContainer,
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = AlertRed, modifier = Modifier.size(18.dp))
                                Text(state.error!!, color = AlertRed, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Role Switcher Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Account Role Type",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                UserRole.SUPERVISOR to ("Plant Supervisor" to Icons.Filled.Factory),
                                UserRole.RECYCLER to ("Authorized Recycler" to Icons.Filled.Recycling)
                            ).forEach { (role, data) ->
                                val (label, icon) = data
                                val isSelected = state.role == role
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { viewModel.onRoleSelected(role) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Full Name Field
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Authorized Representative Name", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = viewModel::onNameChanged,
                            placeholder = { Text("e.g. Rajesh Sharma") },
                            leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Organization / Factory Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (state.role == UserRole.RECYCLER) "Recycling Enterprise / Agency Name" else "Manufacturing Plant / Factory Name",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = state.organizationName,
                            onValueChange = viewModel::onOrganizationNameChanged,
                            placeholder = {
                                Text(
                                    if (state.role == UserRole.RECYCLER) "e.g. Apex Green Metal Refiners Pvt Ltd" else "e.g. Tata Motors Forgings Plant 2"
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (state.role == UserRole.RECYCLER) Icons.Outlined.Recycling else Icons.Outlined.Business,
                                    contentDescription = null,
                                    tint = EmeraldPrimary
                                )
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Industrial Zone / Area / Location
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Industrial Zone / City", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = state.industrialArea,
                            onValueChange = viewModel::onIndustrialAreaChanged,
                            placeholder = { Text("e.g. Ambad MIDC, Nashik") },
                            leadingIcon = { Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Registration / MPCB License Number
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (state.role == UserRole.RECYCLER) "MPCB Authorization / Consent Number" else "GSTIN / Factory License Number (Optional)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = state.registrationNumber,
                            onValueChange = viewModel::onRegistrationNumberChanged,
                            placeholder = {
                                Text(
                                    if (state.role == UserRole.RECYCLER) "e.g. MPCB/MH/NAS/RECY/2024/09" else "e.g. 27AAAAA0000A1Z5"
                                )
                            },
                            leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = EmeraldPrimary) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = viewModel::completeProfile,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !state.isSaving,
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        if (state.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.RocketLaunch, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Launch WasteTrack Workspace", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
