@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.features.farmer.listing

import android.Manifest
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    LaunchedEffect(Unit) {
        viewModel.loadFarmerDrafts("current_farmer_id")
    }
    LaunchedEffect(uiState.draftSaved) {
        if (uiState.draftSaved) {
            snackbarHostState.showSnackbar("Draft saved on this device")
            viewModel.clearDraftSaved()
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
    var listingId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var selectedCategory by remember { mutableStateOf<ListingCategory?>(null) }
    var categorySelectionError by remember { mutableStateOf(false) }
    var expectedHarvestAt by remember { mutableStateOf<Long?>(null) }
    var qualityGrade by remember { mutableStateOf("") }
    var showHarvestDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { showHarvestDatePicker = true }

    fun requestHarvestDatePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            showHarvestDatePicker = true
        }
    }

    fun buildListing(isDraft: Boolean): HarvestListing {
        val parsedQuantity = quantity.toDoubleOrNull() ?: 0.0
        return HarvestListing(
            listingId = listingId,
            farmerId = "current_farmer_id",
            farmerName = "Current Farmer",
            cropName = cropName,
            description = description,
            quantity = parsedQuantity,
            quantityUnit = selectedUnit,
            reservePrice = price.toDoubleOrNull() ?: 0.0,
            imageUrls = if (selectedPhotoUri != null) listOf(selectedPhotoUri.toString()) else emptyList(),
            location = location,
            status = ListingStatus.OPEN,
            biddingEnabled = true,
            category = selectedCategory,
            expectedHarvestAt = expectedHarvestAt,
            qualityGrade = qualityGrade.trim().ifBlank { null },
            availableQuantity = parsedQuantity,
            isDraft = isDraft
        )
    }

    fun publishListing() {
        val category = selectedCategory
        if (category == null) {
            categorySelectionError = true
            return
        }
        categorySelectionError = false

        viewModel.createListing(buildListing(isDraft = false).copy(category = category))
    }

    fun resumeDraft(draft: HarvestListing) {
        listingId = draft.listingId
        cropName = draft.cropName
        quantity = draft.quantity.takeIf { it > 0 }?.toString().orEmpty()
        selectedUnit = draft.quantityUnit
        price = draft.reservePrice.takeIf { it > 0 }?.toString().orEmpty()
        description = draft.description
        location = draft.location
        selectedPhotoUri = draft.imageUrls.firstOrNull()?.let(Uri::parse)
        selectedCategory = draft.category
        expectedHarvestAt = draft.expectedHarvestAt
        qualityGrade = draft.qualityGrade.orEmpty()
    }

    if (showHarvestDatePicker) {
        val datePickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = expectedHarvestAt ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showHarvestDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    expectedHarvestAt = datePickerState.selectedDateMillis
                    showHarvestDatePicker = false
                }) {
                    Text("Save date")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHarvestDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
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

            if (uiState.drafts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(title = "Saved local drafts")
                    uiState.drafts.forEach { draft ->
                        AppCard(
                            onClick = { resumeDraft(draft) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = draft.cropName.ifBlank { "Untitled listing" },
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Draft · not published · tap to resume",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                TextButton(onClick = ::requestHarvestDatePicker) {
                    Text(
                        text = expectedHarvestAt?.let(::formatHarvestDate)
                            ?: "Set expected harvest date (optional)"
                    )
                }
                AgroTextField(
                    value = qualityGrade,
                    onValueChange = { qualityGrade = it },
                    label = "Quality grade (optional)"
                )
            }

            AppCard {
                SectionTitle(title = "Harvest Category")
                Text(
                    text = "Select how this harvest was produced.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Farmer-provided category; Organic does not indicate certification.",
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

            TextButton(
                onClick = { viewModel.saveDraft(buildListing(isDraft = true)) },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save draft on this device")
            }

        }
    }
}

private fun formatHarvestDate(timestamp: Long): String =
    "Expected harvest: ${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))}"
