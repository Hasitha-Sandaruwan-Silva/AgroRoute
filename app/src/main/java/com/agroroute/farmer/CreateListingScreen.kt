@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.farmer

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.common.model.HarvestListing
import com.agroroute.common.model.ListingStatus
import com.agroroute.common.model.QuantityUnit
import com.agroroute.common.ui.LoadingButton
import com.agroroute.common.ui.PhotoPicker
import com.agroroute.common.ui.PriceInputField
import java.util.UUID

/**
 * Screen for creating a new [HarvestListing] with form fields for crop details.
 */
@Composable
fun CreateListingScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate back to MyListings on successful creation
    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearActionSuccess()
        }
    }

    var cropName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(QuantityUnit.KG) }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Photo picker
            PhotoPicker(
                selectedUri = selectedPhotoUri,
                onImageSelected = { selectedPhotoUri = it },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // Crop name
            OutlinedTextField(
                value = cropName,
                onValueChange = { cropName = it },
                label = { Text("Crop Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantity = it },
                    label = { Text("Quantity") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Unit dropdown
                var unitExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = unitExpanded,
                    onExpandedChange = { unitExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = unitExpanded,
                        onDismissRequest = { unitExpanded = false }
                    ) {
                        QuantityUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit.name) },
                                onClick = {
                                    selectedUnit = unit
                                    unitExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Price
            PriceInputField(
                value = price,
                onValueChange = { price = it },
                label = "Reserve Price per Unit"
            )

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth()
            )

            // Error message
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Submit button
            LoadingButton(
                text = "Create Listing",
                isLoading = uiState.isLoading,
                onClick = {
                    val listing = HarvestListing(
                        listingId = UUID.randomUUID().toString(),
                        farmerId = "current_farmer_id", // TODO: Get from auth context
                        farmerName = "Current Farmer", // TODO: Get from auth context
                        cropName = cropName,
                        description = description,
                        quantity = quantity.toDoubleOrNull() ?: 0.0,
                        quantityUnit = selectedUnit,
                        reservePrice = price.toDoubleOrNull() ?: 0.0,
                        imageUrls = if (selectedPhotoUri != null) listOf(selectedPhotoUri.toString()) else emptyList(),
                        location = location,
                        status = ListingStatus.OPEN,
                        biddingEnabled = true
                    )
                    viewModel.createListing(listing)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = cropName.isNotBlank() && quantity.isNotBlank() && price.isNotBlank()
            )
        }
    }
}
