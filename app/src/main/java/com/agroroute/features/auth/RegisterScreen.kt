package com.agroroute.features.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.LoadingButton
import com.github.thelacspace.icons.Lucide

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

    AuthLayout("Create account", "Join the fair harvest marketplace.", snackbarHostState, onBack = { navController.popBackStack() }) {
        Text("Choose your role", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            UserRole.entries.forEach { role ->
                AuthRoleOption(role, selectedRole == role, onClick = { selectedRole = role })
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("YOUR DETAILS", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        AuthField(displayName, { displayName = it }, "Full Name", Lucide.User)
        Spacer(Modifier.height(14.dp))
        AuthField(email, { email = it }, "Email", Lucide.Mail,
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Email)
        Spacer(Modifier.height(24.dp))
        Text("ACCOUNT SECURITY", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        AuthField(password, { password = it }, "Password", Lucide.Lock, password = true)
        Spacer(Modifier.height(14.dp))
        AuthField(confirmPass, { confirmPass = it }, "Confirm Password", Lucide.Lock,
            password = true, last = true,
            isError = confirmPass.isNotEmpty() && password != confirmPass,
            supportingText = if (confirmPass.isNotEmpty() && password != confirmPass) {
                { Text("Passwords do not match") }
            } else null)
        Spacer(Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Create Account",
            isLoading = uiState.isLoading,
            onClick = {
                if (password != confirmPass) {
                    localError = "Passwords do not match"
                    return@AuthPrimaryButton
                }
                localError = null
                viewModel.register(email, password, displayName, selectedRole)
            },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Already have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium)
                Text("Sign In", fontWeight = FontWeight.Bold)
            }
        }
    }
}
