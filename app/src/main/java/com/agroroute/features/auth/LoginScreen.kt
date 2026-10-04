package com.agroroute.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AgroTextField
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.LoadingButton

private val PrimaryGreen = Color(0xFF2E7D32)
private val DarkGreenText = Color(0xFF14532D)
private val SoftMint = Color(0xFFE8F5E9)
private val PageBg = Color(0xFFF6FAF6)
private val BodyTextCharcoal = Color(0xFF4B5563)
private val LineGray = Color(0xFFE5E7EB)

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

    // ✅ LOGIC UNCHANGED
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            onLoginSuccess(user.role)
            val destination = when (user.role) {
                UserRole.FARMER -> Screen.FarmerHome.route
                UserRole.SUPPLIER -> Screen.SupplierHome.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.Welcome.route) { inclusive = false }
            }
            viewModel.consumeUser()
        }
    }

    LaunchedEffect(uiState.requiresGoogleRole) {
        if (uiState.requiresGoogleRole) onGoogleRoleRequired()
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    // Pre-filled Email & Password
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Password reset dialog state
    var showPasswordResetDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    LaunchedEffect(uiState.passwordResetSent) {
        if (uiState.passwordResetSent) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "If the email exists, a reset link was sent to your inbox"
                )
                showPasswordResetDialog = false
                resetEmail = ""
                viewModel.consumePasswordResetSent()
            }
        }
    }

    Scaffold(
        snackbarHost = { AgroSnackbarHost(snackbarHostState) },
        containerColor = PageBg
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.White,
                            SoftMint.copy(alpha = 0.6f),
                            PageBg
                        )
                    )
                )
        ) {
            // Ambient background circles
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 30.dp, y = 20.dp)
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(SoftMint.copy(alpha = 0.8f))
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = (-50).dp)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.05f))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(28.dp))

                // Brand Header with App Logo
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "AgroRoute Logo",
                    modifier = Modifier.size(76.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Welcome Back 👋",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    ),
                    color = DarkGreenText
                )

                Text(
                    text = "Sign in to continue to AgroRoute",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = PrimaryGreen,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
                )

                // Main Form Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(26.dp),
                            ambientColor = PrimaryGreen.copy(alpha = 0.12f),
                            spotColor = PrimaryGreen.copy(alpha = 0.12f)
                        ),
                    shape = RoundedCornerShape(26.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, LineGray.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = DarkGreenText,
                            modifier = Modifier.padding(bottom = 18.dp)
                        )

                        AgroTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            leadingIcon = Lucide.Mail
                        )

                        Spacer(Modifier.height(14.dp))

                        AgroTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = "Password",
                            leadingIcon = Lucide.Lock,
                            visualTransformation = PasswordVisualTransformation()
                        )

                        Spacer(Modifier.height(8.dp))

                        TextButton(
                            onClick = { showPasswordResetDialog = true },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = "Forgot password?",
                                color = PrimaryGreen,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        LoadingButton(
                            text = "Sign In",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.login(email, password) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    googleLoading = true
                                    try {
                                        val googleOption = GetGoogleIdOption.Builder()
                                            .setFilterByAuthorizedAccounts(false)
                                            .setServerClientId(context.getString(R.string.default_web_client_id))
                                            .setAutoSelectEnabled(false)
                                            .build()
                                        val request = GetCredentialRequest.Builder()
                                            .addCredentialOption(googleOption)
                                            .build()
                                        val credential = credentialManager.getCredential(context, request).credential
                                        if (credential is CustomCredential &&
                                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                                        ) {
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
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = if (googleLoading) "Connecting to Google..." else "Continue with Google",
                                color = DarkGreenText,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        TextButton(
                            onClick = { navController.navigate(Screen.Register.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Don't have an account? ",
                                    color = BodyTextCharcoal,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Register",
                                    color = PrimaryGreen,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(28.dp))

                // Trust & Security Footer Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftMint.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Secure access for farmers and suppliers across Sri Lanka.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = DarkGreenText,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Password Reset Dialog
    if (showPasswordResetDialog) {
        Dialog(
            onDismissRequest = { showPasswordResetDialog = false },
            properties = DialogProperties(dismissOnClickOutside = true)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = DarkGreenText
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enter your email address and we'll send you a link to reset your password.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BodyTextCharcoal
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showPasswordResetDialog = false },
                            enabled = !uiState.isLoading
                        ) {
                            Text("Cancel")
                        }
                        Spacer(Modifier.width(8.dp))
                        LoadingButton(
                            text = "Send Reset Link",
                            isLoading = uiState.isLoading,
                            onClick = { viewModel.sendPasswordResetEmail(resetEmail) },
                            modifier = Modifier.height(40.dp)
                        )
                    }
                }
            }
        }
    }
}