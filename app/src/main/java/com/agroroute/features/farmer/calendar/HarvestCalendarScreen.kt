package com.agroroute.features.farmer.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.agroroute.core.model.HarvestListing
import com.agroroute.core.navigation.Screen
import com.agroroute.core.components.*
import com.agroroute.features.farmer.listing.ListingViewModel
import com.github.thelacspace.icons.Lucide
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HarvestCalendarScreen(
    navController: NavController,
    viewModel: ListingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val today = remember { startOfToday() }
    val upcoming = remember(uiState.listings, today) {
        uiState.listings.filter { !it.isDraft && it.expectedHarvestAt?.let { date -> date >= today } == true }.sortedBy { it.expectedHarvestAt }
    }

    LaunchedEffect(Unit) { viewModel.loadFarmerListings(viewModel.currentFarmerId) }
    LaunchedEffect(upcoming) { upcoming.forEach(viewModel::scheduleHarvestReminder) }
    ErrorSnackbarEffect(uiState.error, snackbarHostState) { viewModel.clearError() }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { AgroSnackbarHost(snackbarHostState) }
        ) { padding ->
            when {
                uiState.isLoading && uiState.listings.isEmpty() -> ListSkeleton()
                upcoming.isEmpty() -> Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp).shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Lucide.CalendarDays, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    EmptyState(icon = Lucide.Plus, title = "No upcoming harvests", subtitle = "Add an expected harvest date to a listing to see it here")
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { navController.navigate(Screen.CreateListing.route) },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.height(48.dp).shadow(6.dp, RoundedCornerShape(14.dp))
                    ) {
                        Icon(Lucide.Plus, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create a listing", fontWeight = FontWeight.Bold)
                    }
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val groups = upcoming.groupBy { formatDate(it.expectedHarvestAt ?: 0L) }
                    groups.forEach { (date, listings) ->
                        item(key = "date-$date") {
                            // PREMIUM DATE HEADER
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))))
                                        .shadow(4.dp, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Lucide.CalendarDays, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(date, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${listings.size} harvest${if (listings.size > 1) "s" else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                                }
                                Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                                    Text("${listings.size}", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                        items(listings, key = { it.listingId }) { listing ->
                            PremiumHarvestCard(listing = listing)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumHarvestCard(listing: HarvestListing) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(8.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.06f), spotColor = Color.Black.copy(alpha = 0.06f)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.Wheat, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        listing.cropName,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(shape = RoundedCornerShape(50.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Lucide.Package, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(5.dp))
                            Text("${listing.quantity} ${listing.quantityUnit.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Lucide.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                }
            }

            listing.qualityGrade?.takeIf { it.isNotBlank() }?.let { grade ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                        Icon(Lucide.Star, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Quality: $grade", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
            }

            listing.description.takeIf { it.isNotBlank() }?.let { notes ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Lucide.FileText, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
                    }
                    Text(notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

private fun startOfToday(): Long = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
private fun formatDate(timestamp: Long): String = SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
