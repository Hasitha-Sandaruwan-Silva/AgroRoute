package com.agroroute.core.components

import com.github.thelacspace.icons.Lucide
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/** Launch directly: package visibility can hide a dialer from resolveActivity. */
internal fun openPhoneDialer(context: android.content.Context, phone: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: android.content.ActivityNotFoundException) {
        android.widget.Toast.makeText(context, "No phone app is available on this device.", android.widget.Toast.LENGTH_LONG).show()
    } catch (_: SecurityException) {
        android.widget.Toast.makeText(context, "Couldn't open the phone app. Please dial the number manually.", android.widget.Toast.LENGTH_LONG).show()
    }
}

/** Loads a saved phone number from the user's Firestore profile and opens the system dialer. */
@Composable
fun ProfilePhoneContact(
    userId: String,
    personLabel: String,
    modifier: Modifier = Modifier,
    showName: Boolean = false,
    onNameLoaded: (String?) -> Unit = {}
) {
    val context = LocalContext.current
    val lookup = com.agroroute.core.runtime.LocalContactLookup.current
    var name by remember(userId) { mutableStateOf<String?>(null) }
    var phone by remember(userId) { mutableStateOf<String?>(null) }
    var isLoading by remember(userId) { mutableStateOf(false) }
    var loadFailed by remember(userId) { mutableStateOf(false) }
    var retryKey by remember(userId) { mutableStateOf(0) }

    LaunchedEffect(userId, retryKey) {
        if (userId.isBlank()) {
            phone = null
            isLoading = false
            loadFailed = false
            return@LaunchedEffect
        }
        isLoading = true
        loadFailed = false
        try {
            if (lookup != null) {
                val saved = lookup(userId)
                name = saved.first; phone = saved.second
                onNameLoaded(name)
                return@LaunchedEffect
            }
            val profile = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
            name = profile.getString("displayName")?.trim()?.takeIf(String::isNotBlank)
            onNameLoaded(name)
            phone = profile.getString("phone")
                ?.trim()
                ?.takeIf(String::isNotBlank)
        } catch (_: Exception) {
            onNameLoaded(null)
            phone = null
            loadFailed = true
        } finally {
            isLoading = false
        }
    }

    if (showName) Text("$personLabel: ${name ?: "Name unavailable"}", style = MaterialTheme.typography.bodyMedium)
    when {
        isLoading -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.width(16.dp),
                strokeWidth = 2.dp
            )
            Text("Loading $personLabel contact…", style = MaterialTheme.typography.bodySmall)
        }
        phone != null -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$personLabel: $phone",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = {
                    phone?.let { openPhoneDialer(context, it) }
                }
            ) {
                Icon(Lucide.Phone, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Call", fontSize = 13.sp)
            }
        }
        loadFailed -> TextButton(
            onClick = { retryKey += 1 },
            modifier = modifier
        ) {
            Text("Could not load $personLabel phone number. Retry")
        }
        else -> Text(
            text = "$personLabel phone number has not been added yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}
