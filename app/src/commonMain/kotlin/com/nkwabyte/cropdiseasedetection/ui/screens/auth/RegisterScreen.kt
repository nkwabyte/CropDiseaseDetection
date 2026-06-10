package com.nkwabyte.cropdiseasedetection.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nkwabyte.cropdiseasedetection.generated.resources.*
import com.nkwabyte.cropdiseasedetection.common.navigation.appbar.AppBar
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthState
import com.nkwabyte.cropdiseasedetection.common.navigation.viewmodel.AuthViewModel
import com.nkwabyte.cropdiseasedetection.common.model.UserRole
import com.nkwabyte.cropdiseasedetection.ui.components.GradientSnackBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onLoginClick: () -> Unit,
    onDrawerButtonClick: () -> Unit = { },
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf(UserRole.FARMER) }

    val authState by authViewModel.authState.collectAsState()

    var snackBarMessage by remember { mutableStateOf<String?>(null) }
    var isErrorSnackBar by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Success -> {
                snackBarMessage = null
                authViewModel.resetState()
                onRegisterSuccess()
            }
            is AuthState.Error -> {
                snackBarMessage = (authState as AuthState.Error).message
                isErrorSnackBar = true
                authViewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppBar(
                title = {
                    Text(
                        text = stringResource(Res.string.app_name),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.primary,
                        ),
                    )
                },
                onDrawerButtonClick = onDrawerButtonClick,
                isHomeScreen = false,
            )
        },
        snackbarHost = {
            snackBarMessage?.let { msg ->
                GradientSnackBar(
                    message = msg,
                    isError = isErrorSnackBar,
                    onAction = { snackBarMessage = null }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Image(
                painter = painterResource(Res.drawable.corn_on_the_cob),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(Res.string.register_title_text),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Username Field
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    placeholder = { Text(stringResource(Res.string.register_username_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(Res.string.username_icon_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    enabled = authState != AuthState.Loading
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email Address") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    enabled = authState != AuthState.Loading
                )

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text(stringResource(Res.string.login_password_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = stringResource(Res.string.lock_icon_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff
                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            enabled = authState != AuthState.Loading
                        ) {
                            Icon(
                                imageVector = image,
                                contentDescription = stringResource(Res.string.password_toggle_description),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        errorBorderColor = MaterialTheme.colorScheme.error
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    enabled = authState != AuthState.Loading
                )

                // Role Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.register_role_label),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRole == UserRole.FARMER,
                            onClick = { selectedRole = UserRole.FARMER },
                            label = { Text(stringResource(Res.string.register_role_farmer)) },
                            modifier = Modifier.weight(1f),
                            enabled = authState != AuthState.Loading
                        )
                        FilterChip(
                            selected = selectedRole == UserRole.FIELD_AGENT,
                            onClick = { selectedRole = UserRole.FIELD_AGENT },
                            label = { Text(stringResource(Res.string.register_role_field_agent)) },
                            modifier = Modifier.weight(1f),
                            enabled = authState != AuthState.Loading
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Register Button
                Button(
                    onClick = {
                        if (userName.isBlank() || email.isBlank() || password.isBlank()) {
                            snackBarMessage = "Username, email, and password cannot be empty"
                            isErrorSnackBar = true
                        } else if (password.length < 6) {
                            snackBarMessage = "Password must be at least 6 characters"
                            isErrorSnackBar = true
                        } else {
                            authViewModel.registerWithEmail(email, password, userName, selectedRole)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    enabled = authState != AuthState.Loading
                ) {
                    if (authState == AuthState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onTertiary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(Res.string.register_button_text),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Already have an account? Login redirect
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(Res.string.register_already_account_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    TextButton(
                        onClick = onLoginClick,
                        enabled = authState != AuthState.Loading
                    ) {
                        Text(
                            text = stringResource(Res.string.register_login_link),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    }
}
