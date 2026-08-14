package com.sktech.wastetrack.ui.screens.auth

import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sktech.wastetrack.R
import com.sktech.wastetrack.domain.model.UserRole
import com.sktech.wastetrack.ui.theme.*
import com.sktech.wastetrack.util.LocaleHelper

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    viewModel: LoginViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current as Activity
    val currentLang by LocaleHelper.currentLanguage.collectAsStateWithLifecycle()
    var showLanguageMenu by remember { mutableStateOf(false) }

    LaunchedEffect(state.isSuccess, state.needsProfileSetup) {
        if (state.needsProfileSetup) {
            onNavigateToSignUp()
        } else if (state.isSuccess) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Language Switcher Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                FilterChip(
                    selected = true,
                    onClick = { showLanguageMenu = true },
                    label = {
                        Text(
                            when (currentLang) {
                                "mr" -> "मराठी (MR)"
                                "hi" -> "हिंदी (HI)"
                                else -> "English (EN)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Language,
                            contentDescription = "Language",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldContainer,
                        selectedLabelColor = EmeraldPrimary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = EmeraldPrimary.copy(alpha = 0.3f),
                        selectedBorderColor = EmeraldPrimary,
                        enabled = true,
                        selected = true
                    )
                )

                DropdownMenu(
                    expanded = showLanguageMenu,
                    onDismissRequest = { showLanguageMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("English (EN)") },
                        onClick = {
                            LocaleHelper.setLanguage(context, "en")
                            showLanguageMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("हिंदी (Hindi)") },
                        onClick = {
                            LocaleHelper.setLanguage(context, "hi")
                            showLanguageMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("मराठी (Marathi)") },
                        onClick = {
                            LocaleHelper.setLanguage(context, "mr")
                            showLanguageMenu = false
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon & Brand Title
            Surface(
                shape = CircleShape,
                color = EmeraldContainer,
                border = BorderStroke(1.5.dp, EmeraldPrimary.copy(alpha = 0.25f)),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Recycling,
                        contentDescription = "WasteTrack Logo",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = (-0.5).sp
            )

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Clean Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (state.error != null) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = AlertRedContainer,
                            border = BorderStroke(1.dp, AlertRed.copy(alpha = 0.2f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = AlertRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = state.error!!,
                                    color = AlertRed,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (!state.isOtpSent) {
                        // Full User Role Selection
                        Text(
                            text = stringResource(R.string.select_user_role),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                listOf(
                                    UserRole.SUPERVISOR to Icons.Filled.Factory,
                                    UserRole.RECYCLER to Icons.Filled.Recycling
                                ),
                                listOf(
                                    UserRole.DRIVER to Icons.Filled.LocalShipping,
                                    UserRole.ADMIN to Icons.Filled.AdminPanelSettings
                                )
                            ).forEach { rowRoles ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowRoles.forEach { (role, icon) ->
                                        val isSelected = state.selectedRole == role
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
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
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Start
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = null,
                                                    tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stringResource(role.nameRes),
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontSize = 12.5.sp,
                                                        color = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = stringResource(role.descRes),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.enter_phone_number),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.phoneNumber,
                            onValueChange = viewModel::onPhoneNumberChanged,
                            placeholder = { Text("94035 80730", color = TextMuted) },
                            prefix = { Text("+91 ", color = EmeraldPrimary, fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = EmeraldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = { viewModel.sendOtp(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = state.phoneNumber.length >= 10 && !state.isLoading,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.send_otp),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast 1-Tap Login Button
                        OutlinedButton(
                            onClick = { viewModel.quickDemoLogin(state.selectedRole) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = MaterialTheme.shapes.medium,
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldPrimary)
                        ) {
                            Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp), tint = Gold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                stringResource(R.string.instant_sign_in, stringResource(state.selectedRole.nameRes)),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    } else {
                        // OTP Verification State
                        Text(
                            text = stringResource(R.string.enter_otp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = stringResource(R.string.sent_to_phone, state.phoneNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                        )

                        OutlinedTextField(
                            value = state.otpCode,
                            onValueChange = viewModel::onOtpCodeChanged,
                            placeholder = { Text("123456", color = TextMuted, textAlign = TextAlign.Center) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = EmeraldPrimary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = viewModel::verifyOtp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            enabled = state.otpCode.length == 6 && !state.isLoading,
                            shape = MaterialTheme.shapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldPrimary,
                                contentColor = Color.White
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.verify_and_login),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    viewModel.onPhoneNumberChanged("")
                                    viewModel.onOtpCodeChanged("")
                                }
                            ) {
                                Text(
                                    stringResource(R.string.change_number),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            TextButton(
                                onClick = { viewModel.sendOtp(context) },
                                enabled = !state.isLoading
                            ) {
                                Text(
                                    stringResource(R.string.resend_otp),
                                    color = EmeraldPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
