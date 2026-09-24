package com.agroroute.common.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

/**
 * A specialised [OutlinedTextField] for numeric price entry, enforcing decimal input
 * and displaying a currency prefix label.
 */
@Composable
fun PriceInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Price (LKR)",
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null
) {
    OutlinedTextField(
        value         = value,
        onValueChange = { input ->
            // Only allow valid decimal numbers
            if (input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                onValueChange(input)
            }
        },
        label         = { Text(label) },
        prefix        = { Text("Rs. ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine    = true,
        isError       = isError,
        supportingText = supportingText?.let { { Text(it) } },
        modifier      = modifier.fillMaxWidth()
    )
}
