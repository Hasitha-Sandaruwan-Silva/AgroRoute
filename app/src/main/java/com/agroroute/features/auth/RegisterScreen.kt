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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.agroroute.R
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AgroTextField
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.LoadingButton
import com.agroroute.core.components.RoleSelectCard
import com.github.thelacspace.icons.Lucide
import androidx.compose.foundation.BorderStroke

@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (UserRole) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            onRegisterSuccess(user.role)
            val destination = when (user.role) {
                UserRole.FARMER -> Screen.FarmerHome.route
                UserRole.SUPPLIER -> Screen.SupplierHome.route
            }
            navController.navigate(destination) { popUpTo(Screen.Welcome.route) { inclusive = false } }
            viewModel.consumeUser()
        }
    }

    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.FARMER) }
    var localError by remember { mutableStateOf<String?>(null) }

    val errorMsg = localError ?: uiState.error
    ErrorSnackbarEffect(errorMsg, snackbarHostState) {
        localError = null
        viewModel.clearError()
    }

    Box(Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(context).data("https://images.unsplash.com/photo-1500382017468-9049fed747ef?ixlib=rb-4.0.3&auto=format&fit=crop&w=1920&q=90").crossfade(true).build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.25f), Color.Black.copy(0.6f)))))
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(600.dp).blur(30.dp).background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))

        Scaffold(snackbarHost = { AgroSnackbarHost(snackbarHostState) }, containerColor = Color.Transparent) { paddingValues ->
            Column(
                Modifier.fillMaxSize().padding(paddingValues).statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(id = R.drawable.agroroute), contentDescription = "Logo", modifier = Modifier.size(68.dp).clip(CircleShape))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("AgroRoute", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 0.3.sp)
                        Text("Join the fair harvest marketplace", color = Color.White.copy(0.7f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shadowElevation = 20.dp,
                    border = BorderStroke(1.dp, Color.White.copy(0.15f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(22.dp)) {
                        Text("Create Account 🌱", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text("Join the fair harvest marketplace", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.8f))
                        Spacer(Modifier.height(18.dp))

                        AgroTextField(value = displayName, onValueChange = { displayName = it }, label = "Full Name", leadingIcon = Lucide.User)
                        Spacer(Modifier.height(12.dp))
                        AgroTextField(value = email, onValueChange = { email = it }, label = "Email", leadingIcon = Lucide.Mail)
                        Spacer(Modifier.height(12.dp))
                        AgroTextField(value = password, onValueChange = { password = it }, label = "Password", leadingIcon = Lucide.Lock, visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation())
                        Spacer(Modifier.height(12.dp))
                        AgroTextField(
                            value = confirmPass, onValueChange = { confirmPass = it }, label = "Confirm Password", leadingIcon = Lucide.Lock,
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            isError = confirmPass.isNotEmpty() && password != confirmPass,
                            supportingText = if (confirmPass.isNotEmpty() && password != confirmPass) { { Text("Passwords do not match") } } else null
                        )

                        Spacer(Modifier.height(18.dp))
                        Text("I am a…", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(bottom = 10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            UserRole.entries.forEach { role ->
                                RoleSelectCard(role = role, selected = selectedRole == role, onClick = { selectedRole = role })
                            }
                        }

                        Spacer(Modifier.height(22.dp))
                        LoadingButton(
                            text = "Create Account",
                            isLoading = uiState.isLoading,
                            onClick = {
                                if (password != confirmPass) {
                                    localError = "Passwords do not match"
                                    return@LoadingButton
                                }
                                localError = null
                                viewModel.register(email, password, displayName, selectedRole)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Text("Sign In", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}