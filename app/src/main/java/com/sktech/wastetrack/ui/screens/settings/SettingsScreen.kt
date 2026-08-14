package com.sktech.wastetrack.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.LocaleHelper
import com.sktech.wastetrack.util.SecurityHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val user = state.currentUser
    val context = LocalContext.current
    var biometricsEnabled by remember { mutableStateOf(false) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    val currentLangCode by LocaleHelper.currentLanguage.collectAsStateWithLifecycle()

    state.message?.let { msg ->
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        viewModel.clearMessage()
    }

    // Language selection dialog
    if (showLanguageDialog) {
        val languages = listOf(
            Triple("en", stringResource(R.string.english), "🇬🇧"),
            Triple("hi", stringResource(R.string.hindi), "🇮🇳"),
            Triple("mr", stringResource(R.string.marathi), "🇮🇳")
        )

        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            icon = { Icon(Icons.Outlined.Language, null, tint = EmeraldPrimary) },
            title = { Text(stringResource(R.string.select_language), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    languages.forEach { (code, name, flag) ->
                        val isSelected = currentLangCode == code
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) EmeraldContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    LocaleHelper.setLanguage(context, code)
                                    showLanguageDialog = false
                                    Toast.makeText(context, "$name Selected", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(flag, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog && user != null) {
        var editName by remember { mutableStateOf(user.name) }
        var editOrg by remember { mutableStateOf(user.organizationName) }
        var editArea by remember { mutableStateOf(user.industrialArea) }
        var editRegNo by remember { mutableStateOf(user.registrationNumber) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = {
                Text("Edit Organization Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editOrg,
                        onValueChange = { editOrg = it },
                        label = { Text(if (user.role == UserRole.RECYCLER) "Recycling Agency Name" else "Factory / Plant Name") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editArea,
                        onValueChange = { editArea = it },
                        label = { Text("Industrial Zone / City") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editRegNo,
                        onValueChange = { editRegNo = it },
                        label = { Text(if (user.role == UserRole.RECYCLER) "MPCB Authorization No" else "GSTIN / Plant License No") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProfile(editName, editOrg, editArea, editRegNo)
                        showEditProfileDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary, contentColor = Color.White)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AlertRed) },
            title = { Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(stringResource(R.string.logout_confirm), color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutConfirm = false
                        viewModel.logout { onLogout() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed, contentColor = Color.White)
                ) {
                    Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
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
                            stringResource(R.string.settings),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Enterprise Configuration & Biometric Security",
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
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Dynamic User Profile Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (user?.role == UserRole.RECYCLER) Icons.Outlined.Recycling else Icons.Outlined.Factory,
                                    null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user?.name?.ifBlank { "Unassigned Operator" } ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                user?.organizationName?.ifBlank { "Organization Profile Incomplete" } ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.extraSmall,
                                    color = EmeraldContainer
                                ) {
                                    Text(
                                        user?.role?.displayName?.uppercase() ?: "OPERATOR",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    user?.phone ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit Organization Profile", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Organization & Location Data Tiles
            ModernSettingsItem(
                icon = Icons.Outlined.Business,
                title = if (user?.role == UserRole.RECYCLER) "Recycling Enterprise" else "Manufacturing Plant",
                subtitle = if (!user?.organizationName.isNullOrBlank()) "${user?.organizationName} (${user?.industrialArea.orEmpty().ifBlank { "Ambad MIDC" }})" else "Not Configured"
            )

            ModernSettingsItem(
                icon = Icons.Outlined.Pin,
                title = if (user?.role == UserRole.RECYCLER) "Recycler Identifier" else "Plant Unit Code",
                subtitle = user?.factoryId?.ifBlank { "Unassigned" } ?: "Unassigned"
            )

            if (!user?.registrationNumber.isNullOrBlank()) {
                ModernSettingsItem(
                    icon = Icons.Outlined.Badge,
                    title = if (user?.role == UserRole.RECYCLER) "MPCB Authorization No" else "GSTIN / License No",
                    subtitle = user.registrationNumber
                )
            }

            // Language Selector Item
            ModernSettingsItem(
                icon = Icons.Outlined.Language,
                title = stringResource(R.string.language),
                subtitle = LocaleHelper.getLanguageDisplayName(currentLangCode),
                onClick = { showLanguageDialog = true }
            )

            ModernSettingsItem(
                icon = Icons.Outlined.CloudSync,
                title = stringResource(R.string.sync_status),
                subtitle = "Active Continuous Sync (Room DB + Firestore)"
            )

            // Biometric Toggle Card
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = EmeraldContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Security, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.biometric_security), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Require biometric authentication for scrap dispatches", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                if (SecurityHelper.isBiometricAvailable(context)) {
                                    val activity = context as? FragmentActivity
                                    if (activity != null) {
                                        SecurityHelper.showBiometricPrompt(
                                            activity = activity,
                                            title = "Enable Biometrics",
                                            subtitle = "Verify identity to enable biometric security",
                                            onSuccess = {
                                                biometricsEnabled = true
                                                Toast.makeText(context, "Biometrics Enabled", Toast.LENGTH_SHORT).show()
                                            },
                                            onError = { err ->
                                                Toast.makeText(context, "Failed: $err", Toast.LENGTH_SHORT).show()
                                            }
                                        )
                                    } else {
                                        Toast.makeText(context, "Context is not FragmentActivity", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Biometrics not available on this device", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                biometricsEnabled = false
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = EmeraldPrimary,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            ModernSettingsItem(
                icon = Icons.Outlined.Info,
                title = "System Information",
                subtitle = "WasteTrack Enterprise v1.2.0 (IIT Bombay Sustainability Edition)"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Logout Button
            Surface(
                onClick = { showLogoutConfirm = true },
                color = AlertRedContainer,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = AlertRed)
                    Text(
                        stringResource(R.string.logout),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlertRed
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernSettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = EmeraldContainer,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (onClick != null) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
            }
        }
    }
}
