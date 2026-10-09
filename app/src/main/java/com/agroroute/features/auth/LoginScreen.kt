package com.agroroute.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AgroTextField
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.LoadingButton
import com.github.thelacspace.icons.Lucide
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    Box(Modifier.fillMaxSize()) {
        // BACKGROUND - welcome එකේ high quality photo එකම
        AsyncImage(
            model = ImageRequest.Builder(context).data("https://images.unsplash.com/photo-1500382017468-9049fed747ef?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=90").crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.25f), Color.Black.copy(0.55f)))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(500.dp).blur(30.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))

        Scaffold(
            snackbarHost = { AgroSnackbarHost(snackbarHostState) },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                Modifier.fillMaxSize().padding(paddingValues).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
            ) {
                // LOGO - ලොකුයි, text එක ලං කරලා ලොකුයි
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.agroroute), contentDescription = "Logo", modifier = Modifier.size(68.dp).clip(CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("AgroRoute", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 0.3.sp)
                        Text("From farm to market", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(28.dp))

                // GLASS CARD
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 20.dp,
                    border = BorderStroke(1.dp, Color.White.copy(0.15f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("Welcome Back 👋", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("Sign in to continue to AgroRoute", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f))
                        Spacer(Modifier.height(22.dp))

                        AgroTextField(value = email, onValueChange = { email = it }, label = "Email", leadingIcon = Lucide.Mail)
                        Spacer(Modifier.height(14.dp))
                        AgroTextField(value = password, onValueChange = { password = it }, label = "Password", leadingIcon = Lucide.Lock, visualTransformation = PasswordVisualTransformation())

                        TextButton(onClick = { showPasswordResetDialog = true }, modifier = Modifier.align(Alignment.End).height(40.dp)) {
                            Text("Forgot password?", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Spacer(Modifier.height(8.dp))
                        LoadingButton(text = "Sign In", isLoading = uiState.isLoading, onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth().height(52.dp))
                        Spacer(Modifier.height(12.dp))

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
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Text(if (googleLoading) "Connecting to Google..." else "Continue with Google", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }

                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("Don't have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { navController.navigate(Screen.Register.route) }, contentPadding = PaddingValues(0.dp)) {
                                Text("Register", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
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