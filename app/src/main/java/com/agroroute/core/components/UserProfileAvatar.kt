package com.agroroute.core.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.agroroute.core.theme.AgroGradients
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** Round profile image with initials fallback. Tapping an editable avatar opens the system picker. */
@Composable
fun UserProfileAvatar(
    displayName: String,
    imageUrl: String?,
    size: Dp = 72.dp,
    editable: Boolean = false,
    uploading: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPhotoSelected: (Uri) -> Unit = {},
    selected: Boolean = false
) {
    val context = LocalContext.current
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onPhotoSelected(uri)
        }
    }
    val initials = displayName.trim().split(Regex("\\s+")).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
        .take(2).joinToString("").ifBlank { "A" }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(if (selected) 2.dp else 1.dp, AgroGradients.avatar, CircleShape)
            .padding(if (selected) 4.dp else 3.dp)
            .clip(CircleShape)
            .then(if (editable || onClick != null) Modifier.clickable {
                if (editable) launcher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) else onClick?.invoke()
            } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = if (size < 48.dp) MaterialTheme.typography.labelMedium else MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        if (!imageUrl.isNullOrBlank() && !imageFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$displayName profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                onError = { imageFailed = true }
            )
        }
        if (uploading) {
            Box(Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
        if (editable) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).size(25.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(com.github.thelacspace.icons.Lucide.Camera, contentDescription = "Change profile photo", modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileEditDialog(
    displayName: String,
    phone: String,
    location: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (displayName: String, phone: String, location: String) -> Unit,
    errorMessage: String? = null
) {
    var editedName by remember(displayName) { mutableStateOf(displayName) }
    var editedPhone by remember(phone) { mutableStateOf(phone) }
    var editedLocation by remember(location) { mutableStateOf(location) }
    var showLocationPicker by remember { mutableStateOf(false) }
    if (showLocationPicker) {
        LocationPickerDialog(
            initialValue = editedLocation,
            onDismiss = { showLocationPicker = false },
            onConfirm = { editedLocation = it; showLocationPicker = false }
        )
    }
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        shape = RoundedCornerShape(24.dp),
        title = { Text("Edit profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                if (com.agroroute.features.supplier.components.LocalSupplierPresentation.current) {
                    com.agroroute.features.supplier.components.SupplierSectionHeader(
                        "Your details", "Keep your contact and delivery information up to date")
                    Spacer(Modifier.height(16.dp))
                }
                if (com.agroroute.features.farmer.components.LocalFarmerPresentation.current) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserProfileAvatar(displayName = displayName, imageUrl = null, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("Your farm profile", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Full name") },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = editedPhone,
                    onValueChange = { editedPhone = it },
                    label = { Text("Phone number") },
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = editedLocation,
                    onValueChange = { editedLocation = it },
                    label = { Text("Location") },
                    shape = RoundedCornerShape(16.dp),
                    placeholder = { Text("Enter your farm / business location") },
                    minLines = 1,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { showLocationPicker = true }, enabled = !isSaving) {
                    Text("Choose location on map")
                }
                errorMessage?.let { Text(presentationError(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(editedName.trim(), editedPhone.trim(), editedLocation.trim()) },
                enabled = !isSaving && editedName.isNotBlank()
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isSaving) { Text("Cancel") } }
    )
}
