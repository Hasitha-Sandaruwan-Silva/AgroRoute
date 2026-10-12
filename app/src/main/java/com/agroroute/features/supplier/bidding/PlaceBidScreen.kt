package com.agroroute.features.supplier.bidding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.isExpired
import com.agroroute.core.model.availabilityLabel
import com.agroroute.core.model.acceptsNewBids
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.availableStock
import com.agroroute.core.model.UserRole
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
import com.agroroute.core.components.PriceInputField
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
 * Screen for a supplier to place a bid on a [HarvestListing].
 */
@Composable
fun PlaceBidScreen(
    listingId: String,
    navController: NavController,
    viewModel: PlaceBidViewModel = hiltViewModel()
) {
    val presentationTime = com.agroroute.core.components.listingPresentationTime()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listingState by viewModel.listingState.collectAsStateWithLifecycle()
    val canPlaceBids by viewModel.canPlaceBids.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var deliveryLocation by rememberSaveable(listingId) { mutableStateOf("") }

    // Load listing details
    LaunchedEffect(listingId) {
        viewModel.loadListing(listingId)
    }

    // Navigate back on successful bid placement
    LaunchedEffect(uiState.bidPlaced) {
        if (uiState.bidPlaced) {
            navController.popBackStack()
        }
    }

    val listingError = (listingState as? Result.Error)?.message
    ErrorSnackbarEffect(listingError, snackbarHostState) { viewModel.clearError() }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    var requestedQuantity by rememberSaveable(listingId) { mutableStateOf("") }
    var bidAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    val submissionBidId = remember(listingId) { UUID.randomUUID().toString() }

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

            if (canPlaceBids) {
                AppCard {
                    SectionTitle(title = "Your offer")
                    AgroTextField(value = requestedQuantity, onValueChange = { requestedQuantity = it },
                        label = "Requested quantity (${listingState.getOrNull()?.quantityUnit?.name.orEmpty()})")
                    PriceInputField(
                        value = bidAmount,
                        onValueChange = { bidAmount = it },
                        label = "Your Bid Amount per Unit"
                    )
                    Spacer(Modifier.height(12.dp))
                    AgroTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = "Message (optional)",
                        singleLine = false,
                        minLines = 3,
                        maxLines = 5
                    )
                }

                com.agroroute.core.components.DeliveryLocationField(deliveryLocation, { deliveryLocation = it }, !uiState.isLoading)
                val pickupAvailable = com.agroroute.core.location.SelectedLocation.fromStoredText(listingState.getOrNull()?.location.orEmpty()) != null
                if (!pickupAvailable) Text("Farmer pickup location is missing. Ask the farmer to select the listing's pickup pin.",
                    color = MaterialTheme.colorScheme.error)
                if (listingState.getOrNull()?.acceptsNewBids() != true) Text(if (listingState.getOrNull()?.isExpired() == true) "Listing expired" else "This listing is not currently accepting bids.")
                LoadingButton(
                    text = "Place Bid",
                    isLoading = uiState.isLoading,
                    onClick = {
                        val bid = Bid(
                            bidId = submissionBidId,
                            listingId = listingId,
                            supplierId = uiState.supplierId,
                            supplierName = uiState.supplierName,
                            offerPrice = bidAmount.toDoubleOrNull() ?: 0.0,
                            message = message,
                            deliveryLocation = deliveryLocation,
                            requestedQuantity = requestedQuantity.toDoubleOrNull()
                        )
                        viewModel.placeBid(bid)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = listingState.getOrNull()?.acceptsNewBids() == true &&
                        requestedQuantity.toDoubleOrNull()?.let { it.isFinite() && it > 0 && it <= (listingState.getOrNull()?.availableStock() ?: 0.0) } == true &&
                        bidAmount.toDoubleOrNull()?.let { it.isFinite() && it > 0 } == true && pickupAvailable &&
                        com.agroroute.core.location.SelectedLocation.fromStoredText(deliveryLocation) != null
                )
            } else {
                Text(
                    text = "Only Supplier accounts can place bids.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (bidAmount.isNotBlank() && bidAmount.toDoubleOrNull() == null) {
                Text(
                    text = "Enter a valid bid amount, e.g. 150",
                    style = MaterialTheme.typography.bodySmall,
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
private fun ListingSummaryCard(listing: HarvestListing) {
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
            if (!listing.acceptsNewBids()) {
                StatusChip(label = listing.availabilityLabel(), tone = ChipTone.Neutral)
            } else if (listing.biddingEnabled) {
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(label = "Bidding open", tone = ChipTone.Success)
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
            Spacer(modifier = Modifier.width(5.dp))
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
                    Text("Available", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "${listing.availableStock()} ${listing.quantityUnit.name}",
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
                    Text("Reserve price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Harvest: ${HarvestAvailabilityDate.label(harvestAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                Spacer(modifier = Modifier.width(5.dp))
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

/** UI state for the bid placement screen. */
data class BiddingUiState(
    val isLoading: Boolean = false,
    val bidPlaced: Boolean = false,
    val error: String? = null,
    val supplierId: String = "",
    val supplierName: String = ""
)

/**
 * ViewModel for [PlaceBidScreen] — handles bid submission and listing details loading.
 */
@HiltViewModel
class PlaceBidViewModel @Inject constructor(
    private val placeBidUseCase: PlaceBidUseCase,
    private val listingRepository: HarvestListingRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiddingUiState())
    val uiState: StateFlow<BiddingUiState> = _uiState.asStateFlow()

    private val _listingState = MutableStateFlow<Result<HarvestListing?>>(Result.Loading)
    val listingState: StateFlow<Result<HarvestListing?>> = _listingState.asStateFlow()

    private val _canPlaceBids = MutableStateFlow(false)
    val canPlaceBids: StateFlow<Boolean> = _canPlaceBids.asStateFlow()

    init {
        authRepository.getCurrentUserProfile()
            .onEach { result ->
                val profile = result.getOrNull()
                _canPlaceBids.value = profile?.role == UserRole.SUPPLIER &&
                    profile.userId == authRepository.currentUserId
                if (_canPlaceBids.value) {
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

    fun placeBid(bid: Bid) {
        if (_uiState.value.isLoading || _uiState.value.bidPlaced) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = placeBidUseCase(bid)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, bidPlaced = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
