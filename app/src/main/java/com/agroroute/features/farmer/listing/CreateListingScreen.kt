@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.features.farmer.listing

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.QuantityUnit
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AgroTextField
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListingImage
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.PhotoPicker
import com.agroroute.core.widgets.PriceInputField
import com.agroroute.core.widgets.SectionTitle
import java.util.UUID

@Composable
fun CreateListingScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearActionSuccess()
        }
    }

    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    var cropName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(QuantityUnit.KG) }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCategory by remember { mutableStateOf<ListingCategory?>(null) }
    var categorySelectionError by remember { mutableStateOf(false) }

    fun publishListing() {
        val category = selectedCategory
        if (category == null) {
            categorySelectionError = true
            return
        }
        categorySelectionError = false

        val listing = HarvestListing(
            listingId = UUID.randomUUID().toString(),
            farmerId = "current_farmer_id",
            farmerName = "Current Farmer",
            cropName = cropName,
            description = description,
            quantity = quantity.toDoubleOrNull() ?: 0.0,
            quantityUnit = selectedUnit,
            reservePrice = price.toDoubleOrNull() ?: 0.0,
            imageUrls = if (selectedPhotoUri != null) listOf(selectedPhotoUri.toString()) else emptyList(),
            location = location,
            status = ListingStatus.OPEN,
            biddingEnabled = true,
            category = category
        )
        viewModel.createListing(listing)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                LoadingButton(
                    text = "Create Listing",
                    isLoading = uiState.isLoading,
                    onClick = { publishListing() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    enabled = cropName.isNotBlank() && quantity.isNotBlank() && price.isNotBlank()
                )
            }
        },
        snackbarHost = { AgroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Add a harvest listing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Share the details of your harvest with potential buyers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            uiState.error?.let { error ->
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            AppCard {
                SectionTitle(title = "Harvest details")
                AgroTextField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = "Crop or harvest name"
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantity = it },
                        label = { Text("Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier.weight(1f)
                    )
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
                            shape = MaterialTheme.shapes.medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            QuantityUnit.entries.forEach { unit ->
                                androidx.compose.material3.DropdownMenuItem(
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
            }

            AppCard {
                SectionTitle(title = "Harvest Category")
                Text(
                    text = "Select how this harvest was produced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ListingCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.medium
                                )
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = category
                                        categorySelectionError = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = null)
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                if (categorySelectionError) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Select Organic or Inorganic before creating the listing.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            AppCard {
                SectionTitle(title = "Quality photo")
                Text(
                    text = "Add a photo of the harvest. Select it again to replace it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                ListingImage(
                    imageUrl = selectedPhotoUri?.toString(),
                    height = 220.dp
                )
                Spacer(Modifier.height(12.dp))
                PhotoPicker(
                    selectedUri = selectedPhotoUri,
                    onImageSelected = { selectedPhotoUri = it },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Text(
                    text = if (selectedPhotoUri == null) "Choose a harvest photo" else "Photo selected. Tap to replace it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                )
            }

            AppCard {
                SectionTitle(title = "Price and location")
                PriceInputField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Reserve price per ${selectedUnit.name}"
                )
                Spacer(Modifier.height(12.dp))
                AgroTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Location"
                )
            }

            AppCard {
                SectionTitle(title = "Description")
                AgroTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5
                )
            }

        }
    }
}
