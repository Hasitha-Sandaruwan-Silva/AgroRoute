package com.agroroute.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.data.model.UserRole
import com.agroroute.ui.components.LoadingButton
import com.agroroute.ui.navigation.Screen

/**
 * Login screen composable — handles email/password sign-in and navigates to the
 * appropriate home screen based on the returned [UserRole].
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate when login succeeds
    LaunchedEffect(uiState.user) {
        uiState.user?.let { user ->
            val destination = when (user.role) {
                UserRole.FARMER   -> Screen.FarmerHome.route
                UserRole.SUPPLIER -> Screen.SupplierMarketplace.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.Login.route) { inclusive = true }
            }
        }
    }

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Spacer(Modifier.height(48.dp))
            Text(
                text  = "🌾 AgroRoute",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Connect farms to markets",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(48.dp))

            // ── Email field ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = email,
                onValueChange = { email = it },
                label         = { Text("Email") },
                leadingIcon   = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            // ── Password field ─────────────────────────────────────────────────
            OutlinedTextField(
                value               = password,
                onValueChange       = { password = it },
                label               = { Text("Password") },
                leadingIcon         = { Icon(Icons.Default.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine          = true,
                modifier            = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // ── Error banner ───────────────────────────────────────────────────
            uiState.error?.let { msg ->
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

            // ── Login button ───────────────────────────────────────────────────
            LoadingButton(
                text      = "Sign In",
                isLoading = uiState.isLoading,
                onClick   = { viewModel.login(email, password) },
                modifier  = Modifier.fillMaxWidth().height(52.dp)
            )
            Spacer(Modifier.height(16.dp))

            // ── Register link ──────────────────────────────────────────────────
            TextButton(onClick = { navController.navigate(Screen.Register.route) }) {
                Text("Don't have an account? Register")
            }
            Spacer(Modifier.height(48.dp))
        }
    }
}
