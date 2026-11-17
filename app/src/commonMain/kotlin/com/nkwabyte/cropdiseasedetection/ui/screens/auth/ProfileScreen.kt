package com.nkwabyte.cropdiseasedetection.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthState
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthViewModel
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.ProfileViewModel
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedCard
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedDivider
import com.nkwabyte.cropdiseasedetection.ui.components.GroupedRow
import com.nkwabyte.cropdiseasedetection.ui.components.InfoColor
import com.nkwabyte.cropdiseasedetection.ui.components.PrimaryActionButton
import com.nkwabyte.cropdiseasedetection.ui.components.SectionHeader
import com.nkwabyte.cropdiseasedetection.ui.components.WarningColor
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * The Account tab — the counterpart of iOS `ProfileView`. Signed in, it shows the user
 * card and sign-out; as a guest, an inline sign-in / register form. Both end with the data
 * and privacy actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    onOpenPrivacy: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = koinInject(),
) {
    val firebaseUser by Firebase.auth.authStateChanged.collectAsState(initial = Firebase.auth.currentUser)
    val authState by authViewModel.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var statusMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppBar(title = stringResource(Res.string.tab_account), scrollBehavior = scrollBehavior) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            statusMessage?.let { msg -> StatusToast(msg, onDismiss = { statusMessage = null }) }

            val user = firebaseUser
            if (user != null) {
                SignedInCard(
                    user = user,
                    profileViewModel = profileViewModel,
                    onSignOut = {
                        scope.launch {
                            runCatching { Firebase.auth.signOut() }
                            authViewModel.resetState()
                            profileViewModel.resetProfile()
                        }
                    },
                )
            } else {
                GuestAuthCard(
                    authState = authState,
                    authViewModel = authViewModel,
                    onForgotPassword = onForgotPassword,
                )
            }

            PrivacySection(
                isSignedIn = user != null,
                profileViewModel = profileViewModel,
                authViewModel = authViewModel,
                onOpenPrivacy = onOpenPrivacy,
                onStatus = { statusMessage = it },
            )
        }
    }
}

@Composable
private fun StatusToast(message: String, onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SignedInCard(
    user: FirebaseUser,
    profileViewModel: ProfileViewModel,
    onSignOut: () -> Unit,
) {
    val profile by profileViewModel.userProfileState.collectAsState()
    val fallbackName = stringResource(Res.string.account_farmer)
    val name = user.displayName?.ifEmpty { null } ?: profile.userName.ifEmpty { fallbackName }

    GroupedCard(cornerRadius = 20) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                user.email?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Stat(value = profile.detections, label = stringResource(Res.string.account_detections))
                VerticalDivider()
                Stat(
                    value = profile.successRate,
                    label = stringResource(Res.string.account_success_rate),
                    valueColor = MaterialTheme.colorScheme.primary,
                )
            }

            PrimaryActionButton(
                text = stringResource(Res.string.account_sign_out),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = onSignOut,
                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                contentColor = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = valueColor)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GuestAuthCard(
    authState: AuthState,
    authViewModel: AuthViewModel,
    onForgotPassword: () -> Unit,
) {
    var isRegistering by rememberSaveable { mutableStateOf(false) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var fullName by rememberSaveable { mutableStateOf("") }
    var roleName by rememberSaveable { mutableStateOf(UserRole.FARMER.name) }
    var localError by remember { mutableStateOf<String?>(null) }
    val role = UserRole.valueOf(roleName)

    val isLoading = authState is AuthState.Loading
    val error = localError ?: (authState as? AuthState.Error)?.message

    LaunchedEffect(authState) {
        if (authState is AuthState.Success) {
            email = ""
            password = ""
            fullName = ""
        }
    }

    val fillFieldsMsg = stringResource(Res.string.account_fill_fields)
    val farmerLabel = stringResource(Res.string.account_farmer)
    val agentLabel = stringResource(Res.string.account_field_agent)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GroupedCard(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
                Text(stringResource(Res.string.account_guest_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(Res.string.account_guest_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        GroupedCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(false, true).forEachIndexed { index, registering ->
                        SegmentedButton(
                            selected = isRegistering == registering,
                            onClick = {
                                isRegistering = registering
                                localError = null
                                authViewModel.resetState()
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        ) {
                            Text(stringResource(if (registering) Res.string.account_register else Res.string.account_sign_in))
                        }
                    }
                }

                if (error != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                            .padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
                        )
                        IconButton(onClick = {
                            localError = null
                            authViewModel.resetState()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                if (isRegistering) {
                    AuthField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = stringResource(Res.string.account_full_name),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    )
                }
                AuthField(
                    value = email,
                    onValueChange = { email = it },
                    label = stringResource(Res.string.account_email),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, autoCorrectEnabled = false),
                )
                AuthField(
                    value = password,
                    onValueChange = { password = it },
                    label = stringResource(Res.string.account_password),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isPassword = true,
                )

                if (isRegistering) {
                    Text(
                        stringResource(Res.string.account_role),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        listOf(UserRole.FARMER to "$farmerLabel 🧑‍🌾", UserRole.FIELD_AGENT to "$agentLabel 🕵️")
                            .forEachIndexed { index, (option, label) ->
                                SegmentedButton(
                                    selected = role == option,
                                    onClick = { roleName = option.name },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                                ) { Text(label) }
                            }
                    }
                }

                PrimaryActionButton(
                    text = stringResource(if (isRegistering) Res.string.account_create else Res.string.account_sign_in),
                    loading = isLoading,
                    onClick = {
                        localError = null
                        val e = email.trim()
                        val p = password.trim()
                        if (e.isEmpty() || p.isEmpty()) {
                            localError = fillFieldsMsg
                        } else if (isRegistering) {
                            val name = fullName.trim().ifEmpty { if (role == UserRole.FIELD_AGENT) agentLabel else farmerLabel }
                            authViewModel.registerWithEmail(e, p, name, role)
                        } else {
                            authViewModel.loginWithEmail(e, p)
                        }
                    },
                )

                if (!isRegistering) {
                    TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(Res.string.account_forgot_password))
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardOptions: KeyboardOptions,
    isPassword: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label) },
        singleLine = true,
        keyboardOptions = keyboardOptions,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedBorderColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

private enum class PendingAction { CLEAR_CACHE, ANONYMIZE, DELETE_ACCOUNT }

@Composable
private fun PrivacySection(
    isSignedIn: Boolean,
    profileViewModel: ProfileViewModel,
    authViewModel: AuthViewModel,
    onOpenPrivacy: () -> Unit,
    onStatus: (String) -> Unit,
) {
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    val cacheCleared = stringResource(Res.string.settings_clear_cache_success)
    val anonymized = stringResource(Res.string.settings_anonymize_success)

    Column {
        SectionHeader(stringResource(Res.string.account_privacy_section))
        GroupedCard(padding = 0) {
            GroupedRow(
                icon = Icons.Default.Security,
                iconTint = InfoColor,
                title = stringResource(Res.string.title_privacy_policy),
                subtitle = stringResource(Res.string.account_privacy_subtitle),
                onClick = onOpenPrivacy,
            )
            GroupedDivider()
            GroupedRow(
                icon = Icons.Default.AutoAwesome,
                iconTint = MaterialTheme.colorScheme.primary,
                title = stringResource(Res.string.settings_clear_cache_title),
                subtitle = stringResource(Res.string.account_clear_cache_subtitle),
                onClick = { pending = PendingAction.CLEAR_CACHE },
            )
            GroupedDivider()
            GroupedRow(
                icon = Icons.Default.PersonOff,
                iconTint = WarningColor,
                title = stringResource(Res.string.settings_anonymize_title),
                titleColor = WarningColor,
                subtitle = stringResource(Res.string.account_anonymize_subtitle),
                onClick = { pending = PendingAction.ANONYMIZE },
            )
            if (isSignedIn) {
                GroupedDivider()
                GroupedRow(
                    icon = Icons.Default.NoAccounts,
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(Res.string.account_delete),
                    titleColor = MaterialTheme.colorScheme.error,
                    subtitle = stringResource(Res.string.account_delete_subtitle),
                    onClick = { pending = PendingAction.DELETE_ACCOUNT },
                )
            }
        }
    }

    when (pending) {
        PendingAction.CLEAR_CACHE -> ConfirmDialog(
            title = stringResource(Res.string.settings_clear_cache_title),
            message = stringResource(Res.string.settings_clear_cache_message),
            confirm = stringResource(Res.string.clear_button_text),
            onConfirm = { onStatus(cacheCleared) },
            onDismiss = { pending = null },
        )

        PendingAction.ANONYMIZE -> ConfirmDialog(
            title = stringResource(Res.string.settings_anonymize_title),
            message = stringResource(Res.string.settings_anonymize_message),
            confirm = stringResource(Res.string.proceed_button_text),
            onConfirm = {
                profileViewModel.deleteUserData()
                onStatus(anonymized)
            },
            onDismiss = { pending = null },
        )

        PendingAction.DELETE_ACCOUNT -> ConfirmDialog(
            title = stringResource(Res.string.account_delete),
            message = stringResource(Res.string.account_delete_message),
            confirm = stringResource(Res.string.account_delete),
            onConfirm = { authViewModel.deleteAccount() },
            onDismiss = { pending = null },
        )

        null -> Unit
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onConfirm()
            }) {
                Text(confirm, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel_text)) } },
    )
}
