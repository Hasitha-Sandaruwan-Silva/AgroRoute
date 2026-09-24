package com.agroroute.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.common.model.UserRole
import com.agroroute.common.navigation.Screen
import com.agroroute.common.ui.LoadingButton
import com.agroroute.common.ui.RoleBadge

/**
 * Registration screen composable — collects user info and chosen role, then creates the account.
 */
@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: (UserRole) -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            onRegisterSuccess(user.role)
            val destination = when (user.role) {
                UserRole.FARMER   -> Screen.MyListings.route
                UserRole.SUPPLIER -> Screen.SupplierMarketplace.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    var displayName  by remember { mutableStateOf("") }
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var confirmPass  by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.FARMER) }
    var localError   by remember { mutableStateOf<String?>(null) }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Text(
                text  = "Create Account",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(32.dp))

            // ── Name ───────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = displayName,
                onValueChange = { displayName = it },
                label         = { Text("Full Name") },
                leadingIcon   = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // ── Email ──────────────────────────────────────────────────────────
            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text("Email") },
                leadingIcon   = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // ── Password ───────────────────────────────────────────────────────
            OutlinedTextField(
                value               = password,
                onValueChange       = { password = it },
                label               = { Text("Password") },
                leadingIcon         = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine          = true,
                modifier            = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value               = confirmPass,
                onValueChange       = { confirmPass = it },
                label               = { Text("Confirm Password") },
                leadingIcon         = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                isError             = confirmPass.isNotEmpty() && password != confirmPass,
                supportingText      = if (confirmPass.isNotEmpty() && password != confirmPass) {
                    { Text("Passwords do not match") }
                } else null,
                singleLine          = true,
                modifier            = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // ── Role selection ─────────────────────────────────────────────────
            Text(
                text  = "I am a...",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                UserRole.entries.forEach { role ->
                    val selected = selectedRole == role
                    FilterChip(
                        selected = selected,
                        onClick  = { selectedRole = role },
                        label    = { Text(role.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))

            // ── Error banner ───────────────────────────────────────────────────
            val errorMsg = localError ?: uiState.error
            errorMsg?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text  = msg,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Register button ────────────────────────────────────────────────
            LoadingButton(
                text      = "Create Account",
                isLoading = uiState.isLoading,
                onClick   = {
                    if (password != confirmPass) {
                        localError = "Passwords do not match"
                        return@LoadingButton
                    }
                    localError = null
                    viewModel.register(email, password, displayName, selectedRole)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            Spacer(Modifier.height(16.dp))

            TextButton(onClick = { navController.popBackStack() }) {
                Text("Already have an account? Sign In")
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
