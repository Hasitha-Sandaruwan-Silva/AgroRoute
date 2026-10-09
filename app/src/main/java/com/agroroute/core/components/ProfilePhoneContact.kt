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

/** Loads a saved phone number from the user's Firestore profile and opens the system dialer. */
@Composable
fun ProfilePhoneContact(
    userId: String,
    personLabel: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            phone = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .get()
                .await()
                .getString("phone")
                ?.trim()
                ?.takeIf(String::isNotBlank)
        } catch (_: Exception) {
            phone = null
            loadFailed = true
        } finally {
            isLoading = false
        }
    }

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
                    val dialIntent = Intent(
                        Intent.ACTION_DIAL,
                        Uri.fromParts("tel", phone, null)
                    )
                    if (dialIntent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(dialIntent)
                    }
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
