package com.agroroute.demo

import com.agroroute.core.location.SelectedLocation
import com.agroroute.core.model.*

/** Fictional presentation fixtures, never authoritative transactions or scoring receipts. */
data class DemoDataset(
    val profiles: List<UserProfile>, val listings: List<HarvestListing>, val bids: List<Bid>,
    val requests: List<OrderRequest>, val orders: List<Order>, val reviews: List<Review>
) {
    companion object {
        const val DAY = 86_400_000L
        fun create(now: Long): DemoDataset {
            fun id(kind: String, n: Int) = "demo_${kind}_${n.toString().padStart(3, '0')}"
            fun pin(address: String, lat: Double, lng: Double) = SelectedLocation(address, lat, lng).toStoredText()
            val places = listOf(pin("Green Valley Farm, Kurunegala", 7.4863, 80.3647),
                pin("Sunrise Agro Farm, Dambadeniya", 7.3701, 80.1467),
                pin("Fresh Harvest Farm, Giriulla", 7.3273, 80.1263),
                pin("Demo collection point, Kuliyapitiya", 7.4688, 80.0401),
                pin("Demo collection point, Narammala", 7.4317, 80.2184))
            val profiles = listOf("Nimal Perera", "Kasun Bandara", "Saman Kumara",
                "FreshMart Traders", "Lanka Fresh Distributors", "City Vegetable Centre").mapIndexed { i, name ->
                UserProfile(userId = id(if (i < 3) "farmer" else "supplier", i % 3 + 1),
                    displayName = name, email = "sample${i + 1}@example.invalid",
                    role = if (i < 3) UserRole.FARMER else UserRole.SUPPLIER,
                    location = if (i < 3) places[i] else when (i) {
                        3 -> pin("FreshMart demo depot, Kurunegala", 7.4800, 80.3600)
                        4 -> pin("Lanka Fresh demo depot, Negombo", 7.2083, 79.8358)
                        else -> pin("City Vegetable demo depot, Colombo", 6.9271, 79.8612)
                    }, points = listOf(120, 90, 105, 80, 70, 110)[i], createdAt = now - 30 * DAY)
            }
            val crops = listOf("Tomato", "Carrot", "Cabbage", "Potato", "Beans", "Pumpkin", "Brinjal", "Chilli", "Onion", "Banana")
            val quantities = listOf(100.0, 75.0, 150.0, 50.0, 80.0, 100.0, 60.0, 10.0, 90.0, 120.0)
            val prices = listOf(320.0, 400.0, 280.0, 450.0, 380.0, 180.0, 240.0, 650.0, 420.0, 200.0)
            val listings = crops.mapIndexed { i, crop ->
                val farmer = profiles[if (i <= 1) 0 else if (i == 2) 1 else i % 3]
                HarvestListing(listingId = id("listing", i + 1), farmerId = farmer.userId,
                    farmerName = farmer.displayName, cropName = crop,
                    description = "Fictional local demo harvest: $crop", quantity = quantities[i],
                    reservePrice = prices[i], location = if (i == 0 || i == 1) places[0] else if (i == 2) places[1] else places[i % places.size],
                    biddingEnabled = i in listOf(0, 2, 4, 6, 8),
                    availableQuantity = if (i == 9) 0.0 else quantities[i], createdAt = now - 7 * DAY,
                    expiresAt = if (i == 8) now - DAY else now + 30 * DAY,
                    harvestStatus = if (i == 9) HarvestStatus.EXPECTED else HarvestStatus.READY,
                    expectedHarvestAt = if (i == 9) now + 14 * DAY else null,
                    estimatedQuantity = if (i == 9) quantities[i] else null,
                    category = ListingCategory.ORGANIC, qualityGrade = "Grade A")
            }.toMutableList()
            val delivery = pin("Demo delivery depot, Giriulla", 7.3273, 80.1263)
            fun bid(n: Int, listing: Int, supplier: Int, qty: Double, status: BidStatus = BidStatus.PENDING): Bid {
                val l = listings[listing - 1]; val s = profiles[supplier + 2]
                return Bid(bidId = id("bid", n), listingId = l.listingId, supplierId = s.userId,
                    supplierName = s.displayName, offerPrice = l.reservePrice + 10,
                    requestedQuantity = qty, status = status, deliveryLocation = if (n == 4) profiles[3].location else delivery,
                    message = "Local demo proposal", createdAt = now - 5 * DAY, updatedAt = now - 4 * DAY)
            }
            val bids = listOf(bid(1,1,1,40.0), bid(2,1,2,30.0), bid(3,1,3,70.0),
                bid(4,3,1,40.0,BidStatus.ACCEPTED), bid(5,5,2,30.0,BidStatus.ACCEPTED),
                bid(6,7,3,25.0,BidStatus.ACCEPTED), bid(7,1,2,20.0,BidStatus.REJECTED),
                bid(8,3,2,120.0), bid(9,7,1,20.0))
            fun request(n: Int, listing: Int, supplier: Int, qty: Double, accepted: Boolean): OrderRequest {
                val l = listings[listing - 1]; val s = profiles[supplier + 2]
                return OrderRequest(requestId = id("request",n), listingId = l.listingId,
                    farmerId = l.farmerId, farmerName = l.farmerName, supplierId = s.userId,
                    supplierName = s.displayName, cropName = l.cropName, requestedQuantity = qty,
                    listedPrice = l.reservePrice, status = if (accepted) OrderRequestStatus.ACCEPTED else OrderRequestStatus.PENDING,
                    deliveryLocation = delivery, isPreHarvest = l.harvestStatus == HarvestStatus.EXPECTED,
                    createdAt = now - 5 * DAY, updatedAt = now - 4 * DAY)
            }
            val requests = listOf(request(1,2,1,20.0,true), request(2,4,2,50.0,true),
                request(3,6,3,20.0,true), request(4,8,1,10.0,true),
                request(5,2,2,15.0,false), request(6,10,3,40.0,false))
            fun order(sourceId: String, listingId: String, supplierId: String, qty: Double, price: Double,
                      deliveryLocation: String, status: OrderStatus, bidId: String = ""): Order {
                val index = listings.indexOfFirst { it.listingId == listingId }; val l = listings[index]
                // Cancelled reservations were restored exactly once; all others remain sold/reserved.
                if (status != OrderStatus.CANCELLED) listings[index] = l.recordSale(qty)
                return Order(orderId = sourceId, listingId = listingId, bidId = bidId, farmerId = l.farmerId,
                    supplierId = supplierId, cropName = l.cropName, quantity = qty, agreedPrice = price,
                    status = status, pickupLocation = l.location, deliveryLocation = deliveryLocation,
                    notes = "Fictional local demo. Lifecycle controls are unavailable.",
                    createdAt = now - 4 * DAY, updatedAt = now - 2 * DAY)
            }
            val orders = requests.take(4).mapIndexed { i, r -> order(r.requestId,r.listingId,r.supplierId,
                r.requestedQuantity,r.listedPrice,r.deliveryLocation,
                listOf(OrderStatus.AWAITING_PICKUP,OrderStatus.DELIVERED,OrderStatus.CANCELLED,OrderStatus.DELIVERED)[i]) } +
                bids.filter { it.status == BidStatus.ACCEPTED }.mapIndexed { i,b -> order(b.bidId,b.listingId,b.supplierId,
                    b.requestedQuantity!!,b.offerPrice,b.deliveryLocation,
                    listOf(OrderStatus.IN_TRANSIT,OrderStatus.DELIVERED,OrderStatus.AWAITING_PICKUP)[i],b.bidId) }
            val reviews = orders.filter { it.status == OrderStatus.DELIVERED }.take(2).flatMap { o ->
                listOf(Review("${o.orderId}_${o.farmerId}",o.orderId,o.farmerId,o.supplierId,5,"Prompt collection and careful handling.",now-DAY),
                    Review("${o.orderId}_${o.supplierId}",o.orderId,o.supplierId,o.farmerId,4,"Fresh produce and clear communication.",now-DAY))
            }
            return DemoDataset(profiles,listings,bids,requests,orders,reviews)
        }
    }
}
