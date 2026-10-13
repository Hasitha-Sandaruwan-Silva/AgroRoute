@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.features.farmer.listing

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.agroroute.core.runtime.appViewModel as hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.ListingCategory
import com.agroroute.core.model.ListingStatus
import com.agroroute.core.model.QuantityUnit
import com.agroroute.core.model.availableStock
import com.agroroute.core.theme.AgroRouteTheme
import com.agroroute.core.theme.ForestDeep
import com.agroroute.core.theme.ForestMid
import com.agroroute.core.theme.ForestSoft
import com.agroroute.core.theme.LeafAccent
import com.agroroute.core.util.HarvestAvailabilityDate
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AgroTextField
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.LoadingButton
import com.agroroute.core.components.PriceInputField
import com.agroroute.core.components.SectionTitle
import com.github.thelacspace.icons.Lucide
import java.util.UUID

internal data class ListingDraftFormValues(
    val listingId: String,
    val cropName: String,
    val quantity: String,
    val unit: QuantityUnit,
    val price: String,
    val description: String,
    val location: String,
    val photoUri: String?,
    val category: ListingCategory?,
    val expectedHarvestAt: Long?,
    val qualityGrade: String,
    val harvestStatus: HarvestStatus,
    val estimatedQuantity: String,
    val harvestFailureReason: String?
)

internal fun HarvestListing.toDraftFormValues() = ListingDraftFormValues(
    listingId = listingId,
    cropName = cropName,
    quantity = quantity.takeIf { it > 0 }?.toString().orEmpty(),
    unit = quantityUnit,
    price = reservePrice.takeIf { it > 0 }?.toString().orEmpty(),
    description = description,
    location = location,
    photoUri = imageUrls.firstOrNull(),
    category = category,
    expectedHarvestAt = expectedHarvestAt,
    qualityGrade = qualityGrade.orEmpty(),
    harvestStatus = harvestStatus,
    estimatedQuantity = estimatedQuantity?.takeIf { it > 0 }?.toString().orEmpty(),
    harvestFailureReason = harvestFailureReason
)

internal fun shouldShowPhotoUploadRetry(
    hasSelectedPhoto: Boolean,
    isLoading: Boolean,
    photoUploadError: String?
): Boolean = hasSelectedPhoto && !isLoading && photoUploadError != null

internal fun hasLocalPhotoUri(uri: Uri?): Boolean =
    uri?.scheme == "content" || uri?.scheme == "file"

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun CreateListingScreen(
    navController: NavController,
    editListingId: String? = null,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var cropName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableStateOf(QuantityUnit.KG) }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var expiryHours by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var showLocationPicker by remember { mutableStateOf(false) }
    if (showLocationPicker) {
        com.agroroute.core.components.LocationPickerDialog(
            initialValue = location,
            onDismiss = { showLocationPicker = false },
            onConfirm = { location = it; showLocationPicker = false }
        )
    }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var listingId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var selectedCategory by remember { mutableStateOf<ListingCategory?>(null) }
    var categorySelectionError by remember { mutableStateOf(false) }
    var expectedHarvestAt by remember { mutableStateOf<Long?>(null) }
    var qualityGrade by remember { mutableStateOf("") }
    var showHarvestDatePicker by remember { mutableStateOf(false) }
    var showNotificationRationale by remember { mutableStateOf(false) }
    var showNotificationDenied by remember { mutableStateOf(false) }
    var harvestStatus by remember { mutableStateOf(HarvestStatus.READY) }
    var estimatedQuantity by remember { mutableStateOf("") }
    var formLoadedFor by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(uiState.actionSuccess) {
        if (uiState.actionSuccess) {
            navController.popBackStack()
            viewModel.clearActionSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFarmerDrafts(viewModel.currentFarmerId)
        viewModel.loadCurrentFarmerName()
    }
    LaunchedEffect(editListingId) {
        editListingId?.let(viewModel::loadListingForEdit)
    }
    LaunchedEffect(editListingId, uiState.editingListing) {
        val listing = uiState.editingListing
        if (editListingId != null && listing?.listingId == editListingId && formLoadedFor != editListingId) {
            listingId = listing.listingId
            cropName = listing.cropName
            quantity = listing.quantity.toString()
            selectedUnit = listing.quantityUnit
            price = listing.reservePrice.toString()
            description = listing.description
            location = listing.location
            selectedPhotoUri = listing.imageUrls.firstOrNull()?.let(Uri::parse)
            selectedCategory = listing.category
            expectedHarvestAt = listing.expectedHarvestAt
            qualityGrade = listing.qualityGrade.orEmpty()
            harvestStatus = listing.harvestStatus
            estimatedQuantity = listing.estimatedQuantity?.takeIf { it > 0 }?.toString().orEmpty()
            formLoadedFor = editListingId
        }
    }
    LaunchedEffect(uiState.draftSaved) {
        if (uiState.draftSaved) {
            snackbarHostState.showSnackbar("Draft saved on this device")
            viewModel.clearDraftSaved()
        }
    }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) {
        if (editListingId == null || uiState.editingListing != null) viewModel.clearError()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) showHarvestDatePicker = true else showNotificationDenied = true
    }

    fun requestHarvestDatePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showNotificationRationale = true
        } else {
            showHarvestDatePicker = true
        }
    }

    if (showNotificationRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationRationale = false },
            title = { Text("Harvest reminders") },
            text = { Text("Allow AgroRoute notifications to receive a reminder on the expected harvest date. You can still save the date if you choose not to allow alerts.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationRationale = false
                    showHarvestDatePicker = true
                }) { Text("Not now") }
            }
        )
    }

    if (showNotificationDenied) {
        AlertDialog(
            onDismissRequest = { showNotificationDenied = false },
            title = { Text("Notifications are off") },
            text = { Text("The harvest date can still be saved. To receive reminders, open Android settings, choose Apps > AgroRoute > Notifications, and turn them on.") },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationDenied = false
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                }) { Text("Open settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNotificationDenied = false
                    showHarvestDatePicker = true
                }) { Text("Continue without alerts") }
            }
        )
    }

    fun buildListing(isDraft: Boolean): HarvestListing {
        val parsedQuantity = quantity.toDoubleOrNull() ?: 0.0
        val isExpected = harvestStatus == HarvestStatus.EXPECTED
        val parsedEstimate = estimatedQuantity.toDoubleOrNull() ?: parsedQuantity
        val existing = uiState.editingListing
        return HarvestListing(
            listingId = existing?.listingId ?: listingId,
            farmerId = existing?.farmerId ?: viewModel.currentFarmerId,
            farmerName = existing?.farmerName ?: uiState.farmerName,
            cropName = cropName,
            description = description,
            quantity = if (isExpected) parsedEstimate else parsedQuantity,
            quantityUnit = selectedUnit,
            reservePrice = price.toDoubleOrNull() ?: 0.0,
            imageUrls = selectedPhotoUri?.toString()?.let(::listOf)
                ?: existing?.imageUrls.orEmpty(),
            location = location,
            status = existing?.status ?: ListingStatus.OPEN,
            biddingEnabled = existing?.biddingEnabled ?: true,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            expiresAt = if (existing != null) existing.expiresAt else expiryHours.toDoubleOrNull()
                ?.takeIf { it.isFinite() && it > 0 && it <= 8760 }
                ?.let { System.currentTimeMillis() + (it * 3_600_000).toLong() },
            category = selectedCategory,
            expectedHarvestAt = if (isExpected) expectedHarvestAt else null,
            qualityGrade = qualityGrade.trim().ifBlank { null },
            availableQuantity = if (isExpected) {
                0.0
            } else existing?.let {
                (it.availableStock() + parsedQuantity - it.quantity).coerceAtLeast(0.0)
            } ?: parsedQuantity,
            soldQuantity = existing?.soldQuantity ?: 0.0,
            isDraft = if (existing != null) false else isDraft,
            harvestStatus = if (isExpected) HarvestStatus.EXPECTED else existing?.harvestStatus ?: HarvestStatus.READY,
            estimatedQuantity = if (isExpected) parsedEstimate.takeIf { it > 0.0 } else null,
            harvestFailureReason = existing?.harvestFailureReason,
            harvestUpdatedAt = existing?.harvestUpdatedAt ?: System.currentTimeMillis()
        )
    }

    fun publishListing() {
        val category = selectedCategory
        if (category == null) {
            categorySelectionError = true
            return
        }
        if (harvestStatus == HarvestStatus.EXPECTED && expectedHarvestAt == null) {
            showHarvestDatePicker = true
            return
        }
        categorySelectionError = false
        val listing = buildListing(isDraft = false).copy(category = category)
        if (editListingId == null) viewModel.createListing(listing) else viewModel.updateListing(listing)
    }

    fun resumeDraft(draft: HarvestListing) {
        val values = draft.toDraftFormValues()
        listingId = values.listingId
        cropName = values.cropName
        quantity = values.quantity
        selectedUnit = values.unit
        price = values.price
        description = values.description
        location = values.location
        selectedPhotoUri = values.photoUri?.let(Uri::parse)
        selectedCategory = values.category
        expectedHarvestAt = values.expectedHarvestAt
        qualityGrade = values.qualityGrade
        harvestStatus = values.harvestStatus
        estimatedQuantity = values.estimatedQuantity
    }

    // ── Date picker dialog ────────────────────────────────────────────────────
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
                }) { Text("Save date") }
            },
            dismissButton = {
                TextButton(onClick = { showHarvestDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // ── Scaffold ──────────────────────────────────────────────────────────────
    Scaffold(
        bottomBar = {
            CreateListingBottomBar(
                isLoading = uiState.isLoading,
                isPhotoUploadRequired = hasLocalPhotoUri(selectedPhotoUri),
                isEditing = editListingId != null,
                canSubmit = (expiryHours.isBlank() || expiryHours.toDoubleOrNull()?.let { it.isFinite() && it > 0 && it <= 8760 } == true) && cropName.isNotBlank() && quantity.isNotBlank() && price.isNotBlank() &&
                    (harvestStatus != HarvestStatus.EXPECTED || expectedHarvestAt != null) &&
                    (editListingId == null || uiState.editingListing?.listingId == editListingId),
                onPublish = ::publishListing,
                onSaveDraft = { viewModel.saveDraft(buildListing(isDraft = true)) }
            )
        },
        snackbarHost = { AgroSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Hero header ───────────────────────────────────────────────────
            CreateListingHeroHeader(isEditing = editListingId != null)

            if (editListingId != null && uiState.isLoading && uiState.editingListing == null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Loading your listing…", style = MaterialTheme.typography.bodySmall)
                }
            }

            // ── Error banner ──────────────────────────────────────────────────
            uiState.error?.let { error ->
                ErrorBanner(
                    message = error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // ── Saved local drafts ────────────────────────────────────────────
            if (editListingId == null && uiState.drafts.isNotEmpty()) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SectionTitle(title = "Saved local drafts")
                    uiState.drafts.forEach { draft ->
                        AppCard(
                            onClick = { resumeDraft(draft) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Lucide.FileText,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = draft.cropName.ifBlank { "Untitled listing" },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
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
                }
            }

            // ── Section: Harvest details ──────────────────────────────────────
            FormSection(
                title = "Harvest details",
                icon = Lucide.Wheat,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AgroTextField(
                    value = cropName,
                    onValueChange = { cropName = it },
                    label = "Crop or harvest name",
                    supportingText = { Text("Use the name buyers will recognize.") }
                )
                Spacer(Modifier.height(12.dp))
                HarvestStatusChoiceRow(
                    selected = harvestStatus,
                    isEditing = editListingId != null,
                    existingStatus = uiState.editingListing?.harvestStatus,
                    onSelect = { harvestStatus = it }
                )
                Spacer(Modifier.height(8.dp))
                if (harvestStatus == HarvestStatus.EXPECTED) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Lucide.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Expected-harvest listings are estimates. Pre-harvest requests are not confirmed orders until you mark the harvest ready.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantity = it
                        },
                        label = { Text(if (harvestStatus == HarvestStatus.EXPECTED) "Estimated quantity" else "Quantity") },
                        supportingText = {
                            Text(if (harvestStatus == HarvestStatus.EXPECTED) "How much do you expect? (estimate)" else "How much is available?")
                        },
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
                Spacer(Modifier.height(8.dp))
                // Harvest date row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Lucide.CalendarDays,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    TextButton(
                        onClick = ::requestHarvestDatePicker,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = expectedHarvestAt?.let {
                                if (harvestStatus == HarvestStatus.EXPECTED) {
                                    "Estimated harvest date: ${HarvestAvailabilityDate.format(it)}"
                                } else {
                                    "Availability: ${HarvestAvailabilityDate.format(it)}"
                                }
                            } ?: if (harvestStatus == HarvestStatus.EXPECTED) {
                                "Set estimated harvest date (required)"
                            } else {
                                "Set expected availability date (optional)"
                            }
                        )
                    }
                }
                if (harvestStatus == HarvestStatus.EXPECTED && expectedHarvestAt == null) {
                    Text(
                        text = "An estimated harvest date is required for an expected-harvest listing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                AgroTextField(
                    value = qualityGrade,
                    onValueChange = { qualityGrade = it },
                    label = "Quality grade (optional)",
                    supportingText = { Text("Add a grade only if it is known.") }
                )
            }

            FormDivider()

            // ── Section: Category ─────────────────────────────────────────────
            FormSection(
                title = "Harvest category",
                icon = Lucide.Tag,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Select how this harvest was produced. Organic does not indicate third-party certification.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListingCategory.entries.forEach { category ->
                        val isSelected = selectedCategory == category
                        val borderColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                            label = "categoryBorder"
                        )
                        val bgColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface,
                            label = "categoryBg"
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.medium)
                                .background(bgColor)
                                .border(1.5.dp, borderColor, MaterialTheme.shapes.medium)
                                .selectable(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategory = category
                                        categorySelectionError = false
                                    },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = categorySelectionError) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Lucide.AlertCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Select Organic or Inorganic before publishing.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            FormDivider()

            // ── Section: Photo ────────────────────────────────────────────────
            FormSection(
                title = "Harvest photo",
                icon = Lucide.Camera,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Photo preview / picker — always visible
                PhotoPickerCard(
                    selectedUri = selectedPhotoUri,
                    isLoading = uiState.isLoading && hasLocalPhotoUri(selectedPhotoUri),
                    isEditing = editListingId != null,
                    enabled = !uiState.isLoading,
                    onImageSelected = {
                        selectedPhotoUri = it
                        viewModel.clearPhotoUploadError()
                    }
                )
                // Upload progress indicator
                AnimatedVisibility(
                    visible = uiState.isLoading && hasLocalPhotoUri(selectedPhotoUri),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Uploading to Cloudinary…",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                // Photo upload error
                uiState.photoUploadError?.let { photoError ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    Lucide.AlertCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = photoError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            if (shouldShowPhotoUploadRetry(
                                    hasSelectedPhoto = selectedPhotoUri != null,
                                    isLoading = uiState.isLoading,
                                    photoUploadError = photoError
                                )
                            ) {
                                FilledTonalButton(
                                    onClick = ::publishListing,
                                    modifier = Modifier
                                        .align(Alignment.End)
                                        .padding(top = 8.dp)
                                        .heightIn(min = 48.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(Lucide.RefreshCw, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Retry upload", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            FormDivider()

            // ── Section: Price & location ─────────────────────────────────────
            FormSection(
                title = "Price & location",
                icon = Lucide.MapPin,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                PriceInputField(
                    value = price,
                    onValueChange = { price = it },
                    label = "Reserve price per ${selectedUnit.name}",
                    supportingText = "Your minimum acceptable price for 1 ${selectedUnit.name} (LKR)."
                )
                Spacer(Modifier.height(12.dp))
                AgroTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = "Pickup location",
                    leadingIcon = Lucide.MapPin,
                    supportingText = { Text("Nearest town or farm area.") }
                )
                TextButton(onClick = { showLocationPicker = true }, enabled = !uiState.isLoading) {
                    Text("Choose pickup location on map")
                }
                if (editListingId == null) {
                    AgroTextField(value = expiryHours, onValueChange = { expiryHours = it },
                        label = "Available for (hours, optional)",
                        supportingText = { Text("Leave blank for no expiry. Deadline is fixed when published.") })
                } else uiState.editingListing?.expiresAt?.let {
                    Text("Expires: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}",
                        style = MaterialTheme.typography.bodySmall)
                }
            }

            FormDivider()

            // ── Section: Description ──────────────────────────────────────────
            FormSection(
                title = "Description",
                icon = Lucide.FileText,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                AgroTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Tell buyers about this harvest",
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    supportingText = { Text("Mention storage conditions, colour, freshness, or any special details.") }
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Private composables ───────────────────────────────────────────────────────

@Composable
private fun CreateListingHeroHeader(isEditing: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(
                        ForestDeep,
                        ForestMid,
                        ForestSoft
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icon badge
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Lucide.Sprout,
                        contentDescription = null,
                        tint = LeafAccent,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isEditing) "UPDATE HARVEST" else "NEW HARVEST",
                    style = MaterialTheme.typography.labelMedium,
                    color = LeafAccent,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = if (isEditing) "Edit your listing" else "Create a listing",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isEditing) "Update your harvest details for buyers." else "Share your harvest with potential buyers.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 4.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun FormDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

private data class HarvestStatusOption(
    val status: HarvestStatus,
    val title: String,
    val subtitle: String
)

@Composable
private fun HarvestStatusChoiceRow(
    selected: HarvestStatus,
    isEditing: Boolean,
    existingStatus: HarvestStatus?,
    onSelect: (HarvestStatus) -> Unit
) {
    val options = listOf(
        HarvestStatusOption(
            HarvestStatus.READY,
            "Ready now",
            "Harvested and ready to sell today"
        ),
        HarvestStatusOption(
            HarvestStatus.EXPECTED,
            "Expected harvest",
            "Harvest date & quantity are estimates"
        )
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Is this harvest ready to sell?",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                val isSelected = selected == option.status
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    label = "harvestStatusBorder"
                )
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    label = "harvestStatusBg"
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.medium)
                        .background(bgColor)
                        .border(1.5.dp, borderColor, MaterialTheme.shapes.medium)
                        .selectable(
                            selected = isSelected,
                            onClick = { onSelect(option.status) },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = option.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        if (isEditing && selected == HarvestStatus.READY) {
            Text(
                text = "This harvest is already confirmed. To change an estimate, first set it back to “Expected”.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Lucide.AlertCircle, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Full-width photo picker card with an integrated image preview overlay, a
 * camera/change-photo icon button, and a state label.
 */
@Composable
private fun PhotoPickerCard(
    selectedUri: Uri?,
    isLoading: Boolean,
    isEditing: Boolean,
    enabled: Boolean,
    onImageSelected: (Uri) -> Unit
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onImageSelected(it)
        }
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        var imageFailed by remember(selectedUri) { mutableStateOf(false) }
        Box(contentAlignment = Alignment.Center) {
            if (selectedUri != null && !imageFailed) {
                AsyncImage(
                    model = selectedUri,
                    contentDescription = "Selected listing photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(MaterialTheme.shapes.large),
                    onError = { imageFailed = true }
                )
                // Darken overlay so the action button is readable
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.28f))
                        .clip(MaterialTheme.shapes.large)
                )
            }
            // Empty, failed or change-photo prompt
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (selectedUri == null || imageFailed) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (selectedUri == null) Lucide.ImagePlus else Lucide.AlertCircle,
                                contentDescription = if (selectedUri == null) "Pick a photo" else "Selected photo unavailable",
                                tint = if (selectedUri == null) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (imageFailed) {
                        Text(
                            text = "This photo could not be loaded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Button(
                    onClick = { if (enabled) launcher.launch(arrayOf("image/*")) },
                    enabled = enabled,
                    modifier = Modifier.heightIn(min = 48.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedUri != null) Color.White.copy(alpha = 0.22f)
                        else MaterialTheme.colorScheme.primaryContainer,
                        contentColor = if (selectedUri != null) Color.White
                        else MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Lucide.ImagePlus,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (selectedUri == null) "Choose photo" else "Change photo",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                if (selectedUri == null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "JPEG · PNG · WebP · up to 10 MB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    // Caption below the photo card
    val captionText = if (selectedUri == null) {
        "A clear photo helps buyers assess quality and builds trust."
    } else {
        if (isEditing) "Keep the current photo or choose a replacement. Changes upload when saved."
        else "Photo selected. Upload happens when you tap \"Publish listing\"."
    }
    Text(
        text = captionText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp)
    )
}

/**
 * Sticky bottom bar — publish button (primary) + save-draft link.
 */
@Composable
private fun CreateListingBottomBar(
    isLoading: Boolean,
    isPhotoUploadRequired: Boolean,
    isEditing: Boolean,
    canSubmit: Boolean,
    onPublish: () -> Unit,
    onSaveDraft: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            LoadingButton(
                text = when {
                    isLoading && isPhotoUploadRequired -> "Uploading photo…"
                    isLoading && isEditing -> "Saving changes…"
                    isLoading -> "Publishing listing…"
                    isEditing -> "Save changes"
                    else -> "Publish listing"
                },
                isLoading = isLoading,
                onClick = onPublish,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = canSubmit
            )
            if (!isEditing) {
                TextButton(
                    onClick = onSaveDraft,
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(Lucide.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save draft on this device")
                }
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, backgroundColor = 0xFFEAF9E7)
@Preview(name = "Dark", showBackground = true, backgroundColor = 0xFF013237, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HarvestStatusSelectorPreview() {
    AgroRouteTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            HarvestStatusChoiceRow(
                selected = HarvestStatus.READY,
                isEditing = false,
                existingStatus = null,
                onSelect = {}
            )
            HarvestStatusChoiceRow(
                selected = HarvestStatus.EXPECTED,
                isEditing = false,
                existingStatus = null,
                onSelect = {}
            )
        }
    }
}
