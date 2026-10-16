package com.agroroute.features.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import com.agroroute.R
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.LoadingButton
import com.github.thelacspace.icons.Lucide
import androidx.compose.foundation.BorderStroke

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: (UserRole) -> Unit = {},
    onGoogleRoleRequired: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    var googleLoading by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            onLoginSuccess(user.role)
            val destination = when (user.role) {
                UserRole.FARMER -> Screen.FarmerHome.route
                UserRole.SUPPLIER -> Screen.SupplierHome.route
            }
            navController.navigate(destination) { popUpTo(Screen.Welcome.route) { inclusive = false } }
            viewModel.consumeUser()
        }
    }

    LaunchedEffect(uiState.requiresGoogleRole) {
        if (uiState.requiresGoogleRole) onGoogleRoleRequired()
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    LaunchedEffect(uiState.passwordResetSent) {
        if (uiState.passwordResetSent) {
            scope.launch {
                snackbarHostState.showSnackbar("If the email exists, a reset link was sent to your inbox")
                showPasswordResetDialog = false
                resetEmail = ""
                viewModel.consumePasswordResetSent()
            }
        }
    }

    AuthLayout("Welcome Back", "Sign in to continue your AgroRoute journey.", snackbarHostState,
        heroImage = R.drawable.auth_login, onBack = { navController.popBackStack() }) {
        AuthField(email, { email = it }, "Email", Lucide.Mail,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        AuthField(password, { password = it }, "Password", Lucide.Lock, password = true, last = true)
        TextButton(onClick = { showPasswordResetDialog = true }, modifier = Modifier.align(Alignment.End)) {
            Text("Forgot password?", style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(12.dp))
        AuthPrimaryButton(text = "Sign In", isLoading = uiState.isLoading,
            onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp))
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            Text("or continue with", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = {
                scope.launch {
                    googleLoading = true
                    try {
                        val googleOption = GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(context.getString(R.string.default_web_client_id)).setAutoSelectEnabled(false).build()
                        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()
                        val credential = credentialManager.getCredential(context, request).credential
                        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.signInWithGoogle(googleCredential.idToken)
                        } else {
                            viewModel.showError("Google sign-in did not return a supported account.")
                        }
                    } catch (_: GetCredentialCancellationException) {
                        viewModel.showError("Google sign-in was cancelled.")
                    } catch (_: GetCredentialException) {
                        viewModel.showError("Google sign-in couldn't be started. Please try again.")
                    } catch (_: Exception) {
                        viewModel.showError("Google sign-in couldn't be completed. Check your connection and try again.")
                    } finally {
                        googleLoading = false
                    }
                }
            },
            enabled = !uiState.isLoading && !googleLoading,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            if (googleLoading) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
            }
            Text(if (googleLoading) "Connecting to Google..." else "Continue with Google",
                style = MaterialTheme.typography.labelLarge)
        }
        Spacer(Modifier.height(20.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                Text("Create account", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showPasswordResetDialog) {
        Dialog(onDismissRequest = { showPasswordResetDialog = false }, properties = DialogProperties(dismissOnClickOutside = true)) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Column(Modifier.padding(24.dp)) {
                    Text("Reset Password", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    Text("Enter your email address and we'll send you a link to reset your password.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    OutlinedTextField(value = resetEmail, onValueChange = { resetEmail = it }, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(14.dp))
                    Spacer(Modifier.height(20.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showPasswordResetDialog = false }, enabled = !uiState.isLoading) { Text("Cancel") }
                        Spacer(Modifier.width(8.dp))
                        LoadingButton(text = "Send Reset Link", isLoading = uiState.isLoading, onClick = { viewModel.sendPasswordResetEmail(resetEmail) }, modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}
