package com.agroroute.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.agroroute.core.model.OrderPickupVerification
import com.agroroute.core.model.OrderStatus

@Composable
fun OrderPickupSection(
    orderId: String,
    status: OrderStatus,
    forSupplier: Boolean,
    verification: OrderPickupVerification?,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onVerify: (String) -> Unit = {},
    onCollect: () -> Unit = {}
) {
    if (status == OrderStatus.CANCELLED) return
    var enteredCode by remember(orderId) { mutableStateOf("") }
    AppCard {
        Text(if (forSupplier) "Pickup Verification" else "Pickup Handover",
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        when {
            verification?.legacy == true -> Text("This older order has no pickup verification. No pickup reward will be added.",
                style = MaterialTheme.typography.bodySmall)
            !forSupplier && verification?.farmerConfirmed == true ->
                Text("Handover confirmed ✓ · Points awarded", color = MaterialTheme.colorScheme.primary)
            forSupplier -> {
                if (verification?.supplierConfirmed == true) {
                    Text("Pickup confirmed ✓ · Points awarded", color = MaterialTheme.colorScheme.primary)
                }
                if (verification?.farmerConfirmed == true) {
                    Text("Farmer handover confirmed", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Show this code to the Farmer when collecting the harvest.", style = MaterialTheme.typography.bodySmall)
                    verification?.code?.let { code ->
                        Text(code, style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                    } ?: Text("Code unavailable or expired. Refresh before pickup.", style = MaterialTheme.typography.bodySmall)
                    Text("Code is valid for 24 hours. Refresh if it expires.", style = MaterialTheme.typography.labelSmall)
                }
                if (status == OrderStatus.AWAITING_PICKUP) {
                    LoadingButton(text = "Confirm Pickup", isLoading = loading,
                        onClick = onCollect, enabled = verification != null &&
                            (verification.code != null || verification.farmerConfirmed),
                        modifier = Modifier.fillMaxWidth())
                }
            }
            else -> {
                Text("Ask the Supplier for the pickup code shown on their order.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = enteredCode,
                    onValueChange = { enteredCode = it.filter { character -> character in '0'..'9' }.take(6) },
                    label = { Text("Enter pickup code") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    enabled = !loading, isError = error != null,
                    shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth())
                LoadingButton(text = "Confirm Handover", isLoading = loading,
                    enabled = enteredCode.length == 6 && verification != null,
                    onClick = { onVerify(enteredCode) }, modifier = Modifier.fillMaxWidth())
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = onRefresh, enabled = !loading) {
            Text(if (loading) "Loading pickup verification…" else "Refresh pickup verification")
        }
    }
}
