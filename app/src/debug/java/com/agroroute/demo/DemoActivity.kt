package com.agroroute.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agroroute.BuildConfig
import com.agroroute.core.components.OrderLocationSection
import com.agroroute.core.components.listingAcceptanceBlockedReason
import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.*
import com.agroroute.core.theme.AgroRouteTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Separate debug launcher utility. No authentication switching or business-action ViewModels. */
class DemoActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.DEBUG)
        database = DemoDatabase.open(this)
        setContent { AgroRouteTheme { DemoBrowser(database) } }
    }
    override fun onDestroy() {
        super.onDestroy()
        if (::database.isInitialized) database.close()
    }
}

private data class DemoRows(val profiles: List<UserProfile>, val listings: List<HarvestListing>,
    val bids: List<Bid>, val requests: List<OrderRequest>, val orders: List<Order>, val reviews: List<Review>)

private suspend fun readRows(db: AppDatabase): DemoRows {
    val profiles = UserRole.entries.flatMap { db.userProfileDao().getUsersByRoleSortedByPoints(it.name).first() }
        .filter { it.userId.startsWith("demo_") }
    val listings = db.harvestListingDao().getAllListings().first().filter { it.listingId.startsWith("demo_") }
    val orders = db.orderDao().getAllOrders().first().filter { it.orderId.startsWith("demo_") }
    return DemoRows(profiles, listings, listings.flatMap { db.bidDao().getBidsForListing(it.listingId).first() },
        listings.flatMap { db.orderRequestDao().getRequestsForListing(it.listingId).first() }, orders,
        orders.flatMap { db.reviewDao().getReviewsByOrderId(it.orderId).first() })
}

@Composable internal fun DemoBrowser(db: AppDatabase) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var rows by remember { mutableStateOf<DemoRows?>(null) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var section by remember { mutableStateOf("Listings") }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var supplierView by remember { mutableStateOf(true) }
    suspend fun refresh() { rows = withContext(Dispatchers.IO) { readRows(db) } }
    LaunchedEffect(db) { refresh() }
    fun action(clear: Boolean) {
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.IO) {
                    if (clear) DemoDataSeeder(db).clearDemoData() else DemoDataSeeder(db).loadDemoData()
                }
                selectedOrder = null
                refresh()
                message = if (clear) "Demo records cleared." else "Demo records loaded locally."
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Could not update local demo data. Try again." }
            finally { busy = false }
        }
    }
    Surface(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(), state = listState, contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("AgroRoute local demo", style = MaterialTheme.typography.headlineSmall)
                Text("Fictional records ? Read-only browsing ? Stored separately on this device", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { action(false) }, enabled = !busy) { Text("Load Demo Data") }
                    OutlinedButton(onClick = { action(true) }, enabled = !busy) { Text("Clear Demo Data") }
                }
                message?.let { Text(it) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("${rows?.profiles?.size ?: 0} profiles ? ${rows?.listings?.size ?: 0} listings ? ${rows?.orders?.size ?: 0} orders")
            }
            item {
                Column {
                    listOf(listOf("Profiles", "Listings", "Bids"), listOf("Requests", "Orders", "Reviews")).forEach { group ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { group.forEach { label ->
                            FilterChip(selected = section == label, onClick = { section = label; selectedOrder = null }, label = { Text(label) })
                        } }
                    }
                }
            }
            if (selectedOrder != null) {
                item {
                    Text("${selectedOrder!!.cropName} ? ${selectedOrder!!.status}")
                    Row {
                        FilterChip(supplierView, { supplierView = true }, label = { Text("Supplier map") })
                        Spacer(Modifier.width(8.dp))
                        FilterChip(!supplierView, { supplierView = false }, label = { Text("Farmer map") })
                    }
                    OrderLocationSection(selectedOrder!!, forSupplier = supplierView)
                    Text("Saved demo snapshots. Pickup, cancellation, scoring and review submission are available only through real authenticated orders.")
                }
            }
            val r = rows
            if (r != null) when (section) {
                "Profiles" -> items(r.profiles, key = { it.userId }) { p -> DemoCard(p.displayName, "${p.role} ? ${p.points} sample points\n${p.location.substringBefore('\n')}") }
                "Listings" -> items(r.listings, key = { it.listingId }) { l ->
                    DemoCard(l.cropName, "${l.farmerName} ? ${l.availabilityLabel()} ? ${l.harvestStatus}\n${l.availableStock()} ${l.quantityUnit} available ? LKR ${l.reservePrice}/unit\n${if (l.biddingEnabled) "Bidding" else "Fixed price"} ? ${l.location.substringBefore('\n')}")
                }
                "Bids" -> items(r.bids, key = { it.bidId }) { b ->
                    val l = r.listings.find { it.listingId == b.listingId }
                    val reason = if (b.status == BidStatus.PENDING) listingAcceptanceBlockedReason(l,true,b.requestedQuantity,b.deliveryLocation) else null
                    DemoCard("${l?.cropName} ? ${b.supplierName}", "${b.requestedQuantity} KG ? LKR ${b.offerPrice}/unit ? ${b.status}\n${reason ?: if (b.status == BidStatus.PENDING) "Stock permits acceptance (read-only demo)" else "Historical proposal"}")
                }
                "Requests" -> items(r.requests, key = { it.requestId }) { q -> DemoCard("${q.cropName} ? ${q.supplierName}", "${q.requestedQuantity} KG ? ${q.status}${if (q.isPreHarvest) " ? Pre-harvest" else ""}") }
                "Orders" -> items(r.orders, key = { it.orderId }) { o ->
                    DemoCard(o.cropName, "${o.quantity} KG ? ${o.status}\n${o.pickupLocation.substringBefore('\n')} ? ${o.deliveryLocation.substringBefore('\n')}")
                    TextButton(onClick = { selectedOrder = o; scope.launch { listState.animateScrollToItem(2) } }) { Text("View saved order map") }
                }
                "Reviews" -> items(r.reviews, key = { it.reviewId }) { review -> DemoCard("${review.rating}/5 ? ${r.profiles.find { it.userId == review.reviewerId }?.displayName}", review.comment) }
            }
            if (r != null && r.listings.isEmpty()) item { Text("Load the sample records to browse offline. Map tiles and external directions still need their usual connectivity.") }
        }
    }
}

@Composable private fun DemoCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium)
    } }
}
