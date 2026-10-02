package com.agroroute.features.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.RoleSelectCard

@Composable
fun GoogleRoleSelectionScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { profile ->
            val destination = when (profile.role) {
                UserRole.FARMER -> Screen.FarmerHome.route
                UserRole.SUPPLIER -> Screen.SupplierHome.route
            }
            navController.navigate(destination) {
                popUpTo(Screen.RoleSelection.route) { inclusive = true }
            }
            viewModel.consumeUser()
        }
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Scaffold(snackbarHost = { AgroSnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Choose your account type",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Select how you'll use AgroRoute",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                text = "Continue",
                isLoading = uiState.isLoading,
                enabled = selectedRole != null,
                onClick = { selectedRole?.let(viewModel::completeGoogleSignIn) },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
            TextButton(
                onClick = {
                    viewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.RoleSelection.route) { inclusive = true }
                        }
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}