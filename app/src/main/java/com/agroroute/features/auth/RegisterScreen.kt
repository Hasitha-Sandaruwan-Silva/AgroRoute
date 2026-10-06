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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.R
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AgroTextField
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.RoleSelectCard

@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (UserRole) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ LOGIC UNCHANGED
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            onRegisterSuccess(user.role)
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

    Scaffold(
        snackbarHost = { AgroSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Compact forest gradient brand header ───────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Brush.linearGradient(listOf(ForestDeep, ForestMid)))
            ) {
                Icon(
                    imageVector = Lucide.Leaf,
                    contentDescription = null,
                    tint = LeafAccent.copy(alpha = 0.12f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 18.dp, y = (-12).dp)
                        .size(110.dp)
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 24.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.10f),
                        modifier = Modifier.size(84.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Image(
                                painter = painterResource(id = R.drawable.logo),
                                contentDescription = "AgroRoute Logo",
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Create Account 🌱",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        ),
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Join the fair harvest marketplace",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = LeafAccent,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = "Register Details",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    AgroTextField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Full Name",
                        leadingIcon = Lucide.User
                    )

                    Spacer(Modifier.height(12.dp))

                    AgroTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        leadingIcon = Lucide.Mail
                    )

                    Spacer(Modifier.height(12.dp))

                    AgroTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        leadingIcon = Lucide.Lock,
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(Modifier.height(12.dp))

                    AgroTextField(
                        value = confirmPass,
                        onValueChange = { confirmPass = it },
                        label = "Confirm Password",
                        leadingIcon = Lucide.Lock,
                        visualTransformation = PasswordVisualTransformation(),
                        isError = confirmPass.isNotEmpty() && password != confirmPass,
                        supportingText = if (confirmPass.isNotEmpty() && password != confirmPass) {
                            { Text("Passwords do not match") }
                        } else null
                    )

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "I am a…",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        UserRole.entries.forEach { role ->
                            RoleSelectCard(
                                role = role,
                                selected = selectedRole == role,
                                onClick = { selectedRole = role }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Already have an account? ",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Sign In",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.navigationBarsPadding())
        }
    }
}
