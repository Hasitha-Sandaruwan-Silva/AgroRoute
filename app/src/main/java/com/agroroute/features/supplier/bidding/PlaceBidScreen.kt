package com.agroroute.features.supplier.bidding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.github.thelacspace.icons.Lucide
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.agroroute.core.model.Bid
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.model.UserRole
import com.agroroute.core.repository.AuthRepository
import com.agroroute.core.repository.HarvestListingRepository
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AgroTextField
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.ChipTone
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.ListingImage
import com.agroroute.core.widgets.LoadingButton
import com.agroroute.core.widgets.PriceInputField
import com.agroroute.core.widgets.SectionTitle
import com.agroroute.core.widgets.StatusChip
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
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listingState by viewModel.listingState.collectAsStateWithLifecycle()
    val canPlaceBids by viewModel.canPlaceBids.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
    ErrorSnackbarEffect(listingError, snackbarHostState) { }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { }

    var bidAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
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

                LoadingButton(
                    text = "Place Bid",
                    isLoading = uiState.isLoading,
                    onClick = {
                        val bid = Bid(
                            bidId = UUID.randomUUID().toString(),
                            listingId = listingId,
                            supplierId = "current_supplier_id", // TODO: Get from auth context
                            supplierName = "Current Supplier", // TODO: Get from auth context
                            offerPrice = bidAmount.toDoubleOrNull() ?: 0.0,
                            message = message
                        )
                        viewModel.placeBid(bid)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = bidAmount.isNotBlank()
                )
            } else {
                Text(
                    text = "Only Supplier accounts can place bids.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
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
        ListingImage(imageUrl = listing.imageUrls.firstOrNull(), height = 180.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = listing.cropName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        listing.category?.let { category ->
            Spacer(modifier = Modifier.height(8.dp))
            StatusChip(label = category.displayName, tone = ChipTone.Secondary)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${listing.quantity} ${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Reserve Price: Rs. ${listing.reservePrice}/${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
        if (listing.location.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listing.location,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (listing.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = listing.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** UI state for the bid placement screen. */
data class BiddingUiState(
    val isLoading: Boolean = false,
    val bidPlaced: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for [PlaceBidScreen] — handles bid submission and listing details loading.
 */
@HiltViewModel
class PlaceBidViewModel @Inject constructor(
    private val placeBidUseCase: PlaceBidUseCase,
    private val listingRepository: HarvestListingRepository,
    authRepository: AuthRepository
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
                _canPlaceBids.value = result.getOrNull()?.role == UserRole.SUPPLIER
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
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val r = placeBidUseCase(bid)) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false, bidPlaced = true) }
                is Result.Error   -> _uiState.update { it.copy(isLoading = false, error = r.message) }
                is Result.Loading -> Unit
            }
        }
    }
}
