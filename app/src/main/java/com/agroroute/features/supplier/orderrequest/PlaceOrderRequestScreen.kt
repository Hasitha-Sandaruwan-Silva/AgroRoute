package com.agroroute.features.supplier.orderrequest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.agroroute.core.model.acceptsDirectOrders
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.HarvestStatus
import com.agroroute.core.model.OrderRequest
import com.agroroute.core.model.QuantityUnit
import com.agroroute.core.model.UserRole
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.estimatedAvailable
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.util.HarvestAvailabilityDate
import com.agroroute.core.components.AgroSnackbarHost
import com.agroroute.core.components.AgroTextField
import com.agroroute.core.components.AppCard
import com.agroroute.core.components.ChipTone
import com.agroroute.core.components.EmptyState
import com.agroroute.core.components.ErrorSnackbarEffect
import com.agroroute.core.components.ListSkeleton
import com.agroroute.core.components.ListingImage
import com.agroroute.core.components.LoadingButton
import com.agroroute.core.components.SectionTitle
import com.agroroute.core.components.StatusChip
import com.agroroute.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * Screen for a supplier to place a direct order request on a fixed-price [HarvestListing].
 */
@Composable
fun PlaceOrderRequestScreen(
    listingId: String,
    navController: NavController,
    viewModel: PlaceOrderRequestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listingState by viewModel.listingState.collectAsStateWithLifecycle()
    val canPlaceRequests by viewModel.canPlaceRequests.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deliveryLocation by rememberSaveable(listingId) { mutableStateOf("") }

    // Load listing details
    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
    }

    // Navigate back on successful request placement
    LaunchedEffect(uiState.requestPlaced) {
        if (uiState.requestPlaced) {
            navController.popBackStack()
        }
    }

    val listingError = (listingState as? Result.Error)?.message
    ErrorSnackbarEffect(listingError, snackbarHostState) { viewModel.clearError() }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    var quantityText by remember { mutableStateOf("") }
    val submissionRequestId = remember(listingId) { UUID.randomUUID().toString() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp, vertical = 18.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (val result = listingState) {
                is Result.Loading -> ListSkeleton(count = 1)
                is Result.Success -> {
                    if (result.data != null) {
                        ListingSummaryCard(listing = result.data)
                        if (result.data.harvestStatus == HarvestStatus.EXPECTED) {
                            PreHarvestInfoBanner()
                        }
                    } else {
                        EmptyState(
                            icon = Lucide.Search,
                            title = "Listing not found",
                            subtitle = "This harvest listing is no longer available"
                        )
                    }
                }
                is Result.Error -> EmptyState(
                    icon = Lucide.Search,
                    title = "Unable to load listing",
                    subtitle = "Pull back and try again"
                )
            }

            if (canPlaceRequests && listingState is Result.Success && listingState.getOrNull() != null) {
                val listing = listingState.getOrNull()!!
                val isPreHarvest = listing.harvestStatus == HarvestStatus.EXPECTED
                AppCard {
                    SectionTitle(title = if (isPreHarvest) "Your pre-harvest request" else "Your order request")
                    AgroTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = "Quantity (${listing.quantityUnit.name})",
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (isPreHarvest) {
                            "Estimated available (estimate): ${listing.estimatedAvailable()} ${listing.quantityUnit.name}"
                        } else {
                            "Available: ${listing.availableStock()} ${listing.quantityUnit.name}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Price: Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (isPreHarvest) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "This is a pre-harvest request, not a confirmed order. Nothing is " +
                                "reserved or paid. The farmer can accept it only after marking the " +
                                "harvest ready with the actual available quantity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                com.agroroute.core.components.DeliveryLocationField(deliveryLocation, { deliveryLocation = it }, !uiState.isLoading)
                val pickupAvailable = com.agroroute.core.location.SelectedLocation.fromStoredText(listing.location) != null
                if (!pickupAvailable) Text("Farmer pickup location is missing. Ask the farmer to select the listing's pickup pin.",
                    color = MaterialTheme.colorScheme.error)
                if (!listing.acceptsDirectOrders()) Text("This listing is not currently accepting requests.")
                LoadingButton(
                    text = if (isPreHarvest) "Send pre-harvest request" else "Request Order",
                    isLoading = uiState.isLoading,
                    onClick = {
                        val quantity = quantityText.toDoubleOrNull() ?: 0.0
                        val request = OrderRequest(
                            requestId = submissionRequestId,
                            listingId = listingId,
                            supplierId = uiState.supplierId,
                            supplierName = uiState.supplierName,
                            requestedQuantity = quantity,
                            deliveryLocation = deliveryLocation
                        )
                        val maxAllowed = if (isPreHarvest) listing.estimatedAvailable() else listing.availableStock()
                        viewModel.placeOrderRequest(request, maxAllowed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = listing.acceptsDirectOrders() && quantityText.toDoubleOrNull()?.let {
                        it.isFinite() && it > 0 && it <= (if (isPreHarvest) listing.estimatedAvailable() else listing.availableStock())
                    } == true && pickupAvailable &&
                        com.agroroute.core.location.SelectedLocation.fromStoredText(deliveryLocation) != null
                )
            } else if (listingState is Result.Success && listingState.getOrNull() != null) {
                Text(
                    text = "Only Supplier accounts can place order requests.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Placing a request or bid is not a payment.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }

        AgroSnackbarHost(
            snackbarHostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun PreHarvestInfoBanner() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Lucide.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Pre-harvest request: no stock is reserved and no order is created until " +
                    "the farmer marks the harvest ready and accepts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreHarvestInfoBannerPreview() {
    MaterialTheme {
        PreHarvestInfoBanner()
    }
}

@Composable
private fun ListingSummaryCard(listing: HarvestListing) {
    val isExpected = listing.harvestStatus == HarvestStatus.EXPECTED
    AppCard {
        ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 164.dp)
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = listing.cropName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isExpected) {
                    StatusChip(label = "Expected harvest", tone = ChipTone.Tertiary)
                }
                StatusChip(label = "Fixed price", tone = ChipTone.Success)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Icon(
                imageVector = Lucide.User,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "by ${listing.farmerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        listing.category?.let { category ->
            Spacer(modifier = Modifier.height(12.dp))
            StatusChip(label = category.displayName, tone = ChipTone.Secondary)
        }
        listing.qualityGrade?.takeIf(String::isNotBlank)?.let { grade ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Quality: $grade",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Column(modifier = Modifier.padding(11.dp)) {
                    Text(
                        text = if (isExpected) "Estimated qty (estimate)" else "Available",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isExpected) {
                            "${listing.estimatedAvailable()} ${listing.quantityUnit.name}"
                        } else {
                            "${listing.availableStock()} ${listing.quantityUnit.name}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            ) {
                Column(modifier = Modifier.padding(11.dp)) {
                    Text("Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        listing.expectedHarvestAt?.let { harvestAt ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Lucide.CalendarDays,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = if (isExpected) {
                        "Expected harvest: ${HarvestAvailabilityDate.format(harvestAt)} (estimate)"
                    } else {
                        "Harvest: ${HarvestAvailabilityDate.label(harvestAt)}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        listing.harvestUpdatedAt?.let { updatedAt ->
            Text(
                text = "Harvest details updated ${HarvestAvailabilityDate.format(updatedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        if (listing.location.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Icon(
                    imageVector = Lucide.MapPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = "Location: ${listing.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (listing.description.isNotBlank()) {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = listing.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

/** UI state for the order request placement screen. */
data class OrderRequestUiState(
    val isLoading: Boolean = false,
    val requestPlaced: Boolean = false,
    val error: String? = null,
    val supplierId: String = "",
    val supplierName: String = ""
)

/**
 * ViewModel for [PlaceOrderRequestScreen] — handles order request submission and listing details loading.
 */
@HiltViewModel
class PlaceOrderRequestViewModel @Inject constructor(
    private val placeOrderRequestUseCase: PlaceOrderRequestUseCase,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderRequestUiState())
    val uiState: StateFlow<OrderRequestUiState> = _uiState.asStateFlow()

    private val _listingState = MutableStateFlow<Result<HarvestListing?>>(Result.Loading)
    val listingState: StateFlow<Result<HarvestListing?>> = _listingState.asStateFlow()

    private val _canPlaceRequests = MutableStateFlow(false)
    val canPlaceRequests: StateFlow<Boolean> = _canPlaceRequests.asStateFlow()

    init {
        authRepository.getCurrentUserProfile()
            .onEach { result ->
                val profile = result.getOrNull()
                _canPlaceRequests.value = profile?.role == UserRole.SUPPLIER &&
                    profile.userId == authRepository.currentUserId
                if (_canPlaceRequests.value) {
                    _uiState.update {
                        it.copy(
                            supplierId = profile?.userId.orEmpty(),
                            supplierName = profile?.displayName.orEmpty()
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun loadListing(listingId: String) {
        viewModelScope.launch {
            _listingState.value = Result.Loading
            listingRepository.getListingById(listingId).collect { result ->
                _listingState.value = result
            }
        }
    }

    private var submissionJob: kotlinx.coroutines.Job? = null

    fun placeOrderRequest(request: OrderRequest, availableStock: Double) {
        if (submissionJob?.isActive == true || _uiState.value.requestPlaced) return
        submissionJob = viewModelScope.launch {
            if (request.requestedQuantity <= 0.0) {
                _uiState.update { it.copy(error = "Quantity must be greater than zero") }
                return@launch
            }
            if (request.requestedQuantity > availableStock) {
                _uiState.update { it.copy(error = "Requested quantity exceeds available stock") }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = placeOrderRequestUseCase(request)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, requestPlaced = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
