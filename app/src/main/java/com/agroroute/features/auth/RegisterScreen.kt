package com.agroroute.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AgroTextField
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.GradientHeader
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.RoleSelectCard
import com.agroroute.core.widgets.SectionTitle

@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (UserRole) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
        snackbarHost = { AgroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            GradientHeader(
                title = "Create account",
                subtitle = "Join the fair harvest marketplace"
            )
            AppCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .offset(y = (-24).dp)
            ) {
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
                Spacer(Modifier.height(16.dp))
                SectionTitle(title = "I am a…")
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    UserRole.entries.forEach { role ->
                        RoleSelectCard(
                            role = role,
                            selected = selectedRole == role,
                            onClick = { selectedRole = role }
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
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
                TextButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Already have an account? Sign In")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
