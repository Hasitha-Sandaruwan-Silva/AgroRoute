package com.agroroute.features.farmer.calendar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.navigation.Screen
import com.agroroute.core.widgets.AgroSnackbarHost
import com.agroroute.core.widgets.AppCard
import com.agroroute.core.widgets.EmptyState
import com.agroroute.core.widgets.ErrorSnackbarEffect
import com.agroroute.core.widgets.ListSkeleton
import com.agroroute.core.widgets.SectionTitle
import com.agroroute.features.farmer.listing.ListingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HarvestCalendarScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = remember { startOfToday() }
    val upcoming = remember(uiState.listings, today) {
        uiState.listings
            .filter { !it.isDraft && it.expectedHarvestAt?.let { date -> date >= today } == true }
            .sortedBy { it.expectedHarvestAt }
    }

    LaunchedEffect(Unit) {
        viewModel.loadFarmerListings("current_farmer_id")
    }
    LaunchedEffect(upcoming) {
        upcoming.forEach(viewModel::scheduleHarvestReminder)
    }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Scaffold(snackbarHost = { AgroSnackbarHost(snackbarHostState) }) { padding ->
        when {
            uiState.isLoading && uiState.listings.isEmpty() -> ListSkeleton()
            upcoming.isEmpty() -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.Center
            ) {
                EmptyState(
                    icon = com.github.thelacspace.icons.Lucide.Plus,
                    title = "No upcoming harvests",
                    subtitle = "Add an expected harvest date to a listing to see it here"
                )
                TextButton(
                    onClick = { navController.navigate(Screen.CreateListing.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create a listing")
                }
            }
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val groups = upcoming.groupBy { formatDate(it.expectedHarvestAt ?: 0L) }
                groups.forEach { (date, listings) ->
                    item(key = "date-$date") {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(listings, key = { it.listingId }) { listing ->
                        HarvestCard(listing)
                    }
                }
            }
        }
    }
}

@Composable
private fun HarvestCard(listing: HarvestListing) {
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(listing.cropName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Expected quantity: ${listing.quantity} ${listing.quantityUnit.name}",
            style = MaterialTheme.typography.bodyMedium
        )
        listing.qualityGrade?.takeIf(String::isNotBlank)?.let { grade ->
            Text("Quality: $grade", style = MaterialTheme.typography.bodySmall)
        }
        listing.description.takeIf(String::isNotBlank)?.let { notes ->
            Spacer(Modifier.height(4.dp))
            SectionTitle(title = "Notes")
            Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun startOfToday(): Long =
    java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
