@file:OptIn(ExperimentalMaterial3Api::class)

package com.agroroute.supplier

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.agroroute.common.model.Bid
import com.agroroute.common.model.HarvestListing
import com.agroroute.common.ui.LoadingButton
import com.agroroute.common.ui.PriceInputField
import com.agroroute.common.util.Result
import com.agroroute.farmer.HarvestListingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    var bidAmount by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place Bid") },
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
            // Listing summary
            when (val result = listingState) {
                is Result.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is Result.Success -> {
                    if (result.data != null) {
                        ListingSummaryCard(listing = result.data)
                    } else {
                        Text("Listing not found")
                    }
                }
                is Result.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = result.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Bid amount
            PriceInputField(
                value = bidAmount,
                onValueChange = { bidAmount = it },
                label = "Your Bid Amount per Unit"
            )

            // Message
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message (optional)") },
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
        }
    }
}

@Composable
private fun ListingSummaryCard(listing: HarvestListing) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = listing.cropName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
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
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
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
    private val listingRepository: HarvestListingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BiddingUiState())
    val uiState: StateFlow<BiddingUiState> = _uiState.asStateFlow()

    private val _listingState = MutableStateFlow<Result<HarvestListing?>>(Result.Loading)
    val listingState: StateFlow<Result<HarvestListing?>> = _listingState.asStateFlow()

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
