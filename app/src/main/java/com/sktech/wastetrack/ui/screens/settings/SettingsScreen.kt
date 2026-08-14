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
            title = {
                Text(
                    stringResource(R.string.select_language),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
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
                    Text("Close", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
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
                Text("Edit Business Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editOrg,
                        onValueChange = { editOrg = it },
                        label = { Text(if (user.role == UserRole.RECYCLER) "Recycling Agency Name" else "Factory / Plant Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editArea,
                        onValueChange = { editArea = it },
                        label = { Text("Industrial Zone / City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editRegNo,
                        onValueChange = { editRegNo = it },
                        label = { Text(if (user.role == UserRole.RECYCLER) "MPCB Authorization No" else "GSTIN / Plant License No") },
                        singleLine = true,
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Logout confirmation dialog
    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            icon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = AlertRed) },
            title = { Text(stringResource(R.string.logout), fontWeight = FontWeight.Bold, color = AlertRed) },
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // 1. User Profile Card (Stitch Style)
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = EmeraldContainer,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (user?.role == UserRole.RECYCLER) Icons.Filled.Recycling else Icons.Filled.Factory,
                                    null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                user?.name?.ifBlank { "Industrial Operator" } ?: "Loading...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                user?.organizationName?.ifBlank { "Enterprise Profile" } ?: "Enterprise Profile",
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
                                        user?.role?.displayName?.uppercase() ?: "SUPERVISOR",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    user?.phone ?: "+91 94035 80730",
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

            // 2. Preferences Section (Stitch Style)
            SectionHeader(title = "Preferences")
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    StitchSettingsRow(
                        icon = Icons.Outlined.Language,
                        title = stringResource(R.string.language),
                        subtitle = LocaleHelper.getLanguageDisplayName(currentLangCode),
                        onClick = { showLanguageDialog = true }
                    )
                }
            }

            // 3. Security Section (Stitch Style)
            SectionHeader(title = "Security")
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
                        shape = CircleShape,
                        color = EmeraldContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Fingerprint,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.biometric_security),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Use Face ID / Fingerprint for gatepass authorize",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }

            // 4. System Section (Stitch Style)
            SectionHeader(title = "System")
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Cloud Synchronization",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Filled.CloudDone, null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                Text(
                                    "Last synced: Today, Live (Room + Firestore)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            Toast.makeText(context, "Syncing queued changes to Firestore...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldContainer, contentColor = EmeraldPrimary)
                    ) {
                        Icon(Icons.Filled.Sync, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Force Sync Now", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // 5. Danger Zone / Logout (Stitch Style)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.logout),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun StitchSettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = CircleShape,
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
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
    }
}
