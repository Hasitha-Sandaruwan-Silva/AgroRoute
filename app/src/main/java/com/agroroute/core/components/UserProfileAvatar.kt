package com.agroroute.core.components

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import java.util.Locale

/** Round profile image with initials fallback. Tapping an editable avatar opens the system picker. */
@Composable
fun UserProfileAvatar(
    displayName: String,
    imageUrl: String?,
    size: Dp = 72.dp,
    editable: Boolean = false,
    onClick: (() -> Unit)? = null,
    onPhotoSelected: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
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
            .clickable(enabled = editable || onClick != null) {
                if (editable) launcher.launch(arrayOf("image/*")) else onClick?.invoke()
            },
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank() && !imageFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "$displayName profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
                onError = { imageFailed = true }
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        if (editable) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd).size(25.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Edit, contentDescription = "Change profile photo", modifier = Modifier.size(14.dp))
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
    onSave: (displayName: String, phone: String, location: String) -> Unit
) {
    val context = LocalContext.current
    var editedName by remember(displayName) { mutableStateOf(displayName) }
    var editedPhone by remember(phone) { mutableStateOf(phone) }
    var editedLocation by remember(location) { mutableStateOf(location) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var waitingForLocation by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.any { it }) {
            requestCurrentLocation(context) { picked ->
                waitingForLocation = false
                if (picked.isNullOrBlank()) {
                    locationMessage = "Could not get a location. Turn on device location or enter it manually."
                } else {
                    editedLocation = picked
                    locationMessage = "Current location added. You can edit it before saving."
                }
            }
        } else {
            waitingForLocation = false
            locationMessage = "Location permission was not granted. You can enter a location manually."
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Edit profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Full name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = editedPhone,
                    onValueChange = { editedPhone = it },
                    label = { Text("Phone number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = editedLocation,
                    onValueChange = { editedLocation = it },
                    label = { Text("Location") },
                    placeholder = { Text("Enter your farm / business location") },
                    minLines = 1,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(
                    onClick = {
                        waitingForLocation = true
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
                        )
                    },
                    enabled = !waitingForLocation && !isSaving
                ) {
                    if (waitingForLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Use current location")
                    }
                }
                locationMessage?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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

@Suppress("MissingPermission", "DEPRECATION")
private fun requestCurrentLocation(context: Context, onResult: (String?) -> Unit) {
    val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!fineGranted && !coarseGranted) return onResult(null)

    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return onResult(null)
    val providers = manager.getProviders(true)
    val fallback = providers
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
    val provider = when {
        LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
        LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
        else -> providers.firstOrNull()
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && provider != null) {
        runCatching {
            manager.getCurrentLocation(provider, null, context.mainExecutor) { freshLocation ->
                onResult(locationLabel(context, freshLocation ?: fallback))
            }
        }.onFailure { onResult(locationLabel(context, fallback)) }
    } else {
        onResult(locationLabel(context, fallback))
    }
}

@Suppress("DEPRECATION")
private fun locationLabel(context: Context, location: Location?): String? {
    location ?: return null
    val address = runCatching {
        if (!Geocoder.isPresent()) null
        else Geocoder(context, Locale.getDefault())
            .getFromLocation(location.latitude, location.longitude, 1)
            ?.firstOrNull()?.getAddressLine(0)
    }.getOrNull()
    return address?.takeIf { it.isNotBlank() }
        ?: String.format(Locale.getDefault(), "%.5f, %.5f", location.latitude, location.longitude)
}
