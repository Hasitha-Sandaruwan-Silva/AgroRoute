package com.agroroute.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.Icon

@Composable
fun ErrorSnackbarEffect(
    error: String?,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit
) {
    LaunchedEffect(error) {
        error?.let { message ->
            snackbarHostState.showSnackbar(presentationError(message))
            onDismiss()
        }
    }
}

/** Preserve useful business feedback while keeping provider diagnostics out of the UI. */
fun presentationError(message: String): String = when {
    message.contains("NOT_FOUND", true) -> "This item is unavailable. Refresh and try again."
    message.contains("PERMISSION_DENIED", true) || message.contains("insufficient permissions", true) ->
        "This action is not available for your account or the current item state. Refresh and try again."
    message.contains("UNAVAILABLE:", true) || message.contains("code=UNAVAILABLE", true) || message.contains("network error", true) ->
        "Could not connect. Check your connection and try again."
    message.contains("Firebase", true) || message.contains("Firestore", true) || message.contains("Exception:", true) || message.length > 400 ->
        "This action could not be completed. Please refresh and try again."
    else -> message
}

@Composable
fun AgroSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(hostState = snackbarHostState, modifier = modifier) { data ->
        Snackbar(
            modifier = Modifier
                .padding(12.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(ForestDeep, ForestMid)),
                    shape = RoundedCornerShape(18.dp)
                ),
            shape = RoundedCornerShape(18.dp),
            containerColor = Color.Transparent,
            contentColor = Color.White
        ) {
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp)),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Info,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = data.visuals.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
            }
        }
    }
}
