package com.agroroute.features.supplier.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.UserRole
import com.agroroute.core.navigation.Screen
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.theme.ThemeViewModel
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.features.supplier.components.SupplierCard as AppCard
import com.agroroute.core.components.RoleBadge
import com.agroroute.core.components.SettingsSection
import com.agroroute.core.components.ProfileEditDialog
import com.agroroute.core.components.UserProfileAvatar
import com.agroroute.core.components.ProfileLocationCard
import com.agroroute.core.theme.AgroGradients
import com.agroroute.features.auth.AuthViewModel
import com.github.thelacspace.icons.Lucide
import kotlinx.coroutines.launch

@Composable
fun SupplierProfileScreen(
    navController: NavController,
    userRole: UserRole? = UserRole.SUPPLIER,
    viewModel: AuthViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by themeViewModel.mode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mutationAllowed = com.agroroute.core.runtime.LocalMutationAllowed.current
    var showProfileEditor by remember { mutableStateOf(false) }

    val user = uiState.user
    val displayName = user?.displayName?.takeIf(String::isNotBlank) ?: "Profile unavailable"
    val email = user?.email.orEmpty()
    val role = user?.role ?: userRole ?: UserRole.SUPPLIER
    val initials = displayName.split(" ")
        .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2)
        .joinToString("")
        .ifEmpty { "SP" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // ── Forest gradient account hero card ────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                androidx.compose.foundation.Image(
                    androidx.compose.ui.res.painterResource(com.agroroute.R.drawable.supplier_farm_hero), null,
                    Modifier.fillMaxWidth().height(140.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                Icon(
                    imageVector = Lucide.Leaf,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(88.dp)
                )
                Column(Modifier.padding(top = 100.dp)) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface).padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            UserProfileAvatar(
                                displayName = displayName,
                                imageUrl = uiState.photoPreviewUri?.toString() ?: user?.profileImageUrl,
                            uploading = uiState.photoUploading,
                                size = 88.dp,
                                selected = true,
                                editable = mutationAllowed && !uiState.isLoading && !uiState.photoUploading,
                                onPhotoSelected = viewModel::uploadProfilePhoto
                            )
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (email.isNotBlank()) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 1.dp)
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RoleBadge(role = role)
                        }
                    }
                }
            }

            if (uiState.photoUploading) {
                Text("Uploading profile photo...", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            } else if (uiState.photoPreviewUri != null && uiState.error != null) {
                androidx.compose.material3.TextButton(onClick = viewModel::retryProfilePhoto) { Text("Retry photo upload") }
            } else if (uiState.photoUploadSucceeded) {
                Text("Profile photo updated", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(12.dp))
            }
            uiState.error?.let { error ->
                Surface(
                    modifier = Modifier.padding(top = 12.dp),
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = com.agroroute.core.components.presentationError(error),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Lucide.User,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Role",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = role.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(user?.phone?.ifBlank { "Add your phone number" } ?: "Add your phone number", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Your Points",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${user?.points ?: 0} pts",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    OutlinedButton(
                        onClick = { navController.navigate(Screen.SupplierLeaderboard.route) },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Lucide.Trophy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Leaderboard", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            if (mutationAllowed) androidx.compose.material3.TextButton(onClick = {
                navController.navigate(com.agroroute.features.participants.ParticipantRoutes.MESSAGES)
            }) { Text("Messages") }

            ProfileLocationCard(user?.location, onClick = { if (mutationAllowed) showProfileEditor = true })
            Spacer(Modifier.height(16.dp))
            com.agroroute.features.supplier.components.SupplierMarketBanner()
            Spacer(Modifier.height(16.dp))
            com.agroroute.core.runtime.AppEntryAction(tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            SettingsSection(
                themeMode = themeMode,
                onThemeModeChange = themeViewModel::setMode,
                onChangePassword = {
                    if (email.isNotBlank()) {
                        viewModel.sendPasswordResetEmail(email)
                        scope.launch {
                            snackbarHostState.showSnackbar("Password reset link sent to $email")
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Email address unavailable")
                        }
                    }
                },
                onEditProfile = { if (mutationAllowed) showProfileEditor = true },
                onHelp = { navController.navigate(Screen.HelpSupport.route) },
                onAbout = { navController.navigate(Screen.About.route) },
                onPrivacy = { navController.navigate(Screen.Privacy.route) },
                onNotifications = { navController.navigate(Screen.NotificationInbox.route) }
            )

            if (showProfileEditor && user != null) {
                ProfileEditDialog(
                    displayName = user.displayName,
                    phone = user.phone,
                    location = user.location,
                    isSaving = uiState.isLoading,
                    errorMessage = uiState.error,
                    onDismiss = { showProfileEditor = false },
                    onSave = { name, phoneNumber, locationText ->
                        viewModel.updateProfile(user.copy(displayName = name, phone = phoneNumber, location = locationText)) {
                            showProfileEditor = false
                        }
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    if (mutationAllowed) viewModel.logout {
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                enabled = mutationAllowed && !uiState.isLoading,
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Lucide.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Log out", fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
