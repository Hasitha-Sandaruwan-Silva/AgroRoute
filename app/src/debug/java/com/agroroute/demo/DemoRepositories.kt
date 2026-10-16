package com.agroroute.demo

import com.agroroute.core.database.AppDatabase
import com.agroroute.core.model.*
import com.agroroute.core.repository.*
import com.agroroute.core.util.Result
import kotlinx.coroutines.flow.*
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

/** Allowlisted Room reads; every other operation fails closed without any remote dependency. */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DemoRepositories(val database: AppDatabase, val session: DemoSessionManager) {
    companion object { const val UNAVAILABLE = "Unavailable in Local Demo Mode" }
    private fun <T> Flow<T>.result(): Flow<Result<T>> = map { Result.Success(it) }
    private fun listings(source: Flow<List<HarvestListing>>) = combine(source, flow {
        while (true) { emit(System.currentTimeMillis()); kotlinx.coroutines.delay(1000) }
    }) { value, _ -> Result.Success(value) }
    private val refreshNames = setOf("syncListings","syncBidsForListing","syncOrders","syncRequestsForListing",
        "syncRequestsForUser","syncReviewsForUser","syncLeaderboard")

    @Suppress("UNCHECKED_CAST")
    fun <T> repository(type: Class<T>): T = Proxy.newProxyInstance(type.classLoader,arrayOf(type)) { proxy, method, raw ->
        val args = raw.orEmpty()
        when (method.name) {
            "toString" -> "LocalRoomAdapter:${type.simpleName}"
            "hashCode" -> System.identityHashCode(proxy)
            "equals" -> proxy === args.firstOrNull()
            else -> {
                val continuation = args.lastOrNull() as? Continuation<Any?>
                if (continuation != null) {
                    val operation: suspend () -> Any? = { suspendRead(type, method.name, args) }
                    operation.startCoroutineUninterceptedOrReturn(continuation)
                } else read(type, method.name, args)
            }
        }
    } as T

    private fun read(type: Class<*>, name: String, a: Array<out Any?>): Any? {
        val id = a.firstOrNull() as? String ?: ""
        return when (type) {
            AuthRepository::class.java -> when (name) {
                "isLoggedIn" -> session.state.value.selectedDemoUserId != null
                "getIsLoggedIn" -> session.state.value.selectedDemoUserId != null
                "getCurrentUserId" -> session.state.value.selectedDemoUserId
                "getCurrentUserProfile" -> session.state.flatMapLatest { s ->
                    if (s.selectedDemoUserId == null) flowOf(Result.Success(null))
                    else database.userProfileDao().getUserProfile(s.selectedDemoUserId).result()
                }
                else -> error("Unsupported local auth read: $name")
            }
            HarvestListingRepository::class.java -> when (name) {
                "getAllListings" -> listings(database.harvestListingDao().getAllListings())
                "getListingsByFarmer" -> listings(database.harvestListingDao().getListingsByFarmer(id))
                "getDraftsByFarmer" -> listings(database.harvestListingDao().getDraftsByFarmer(id))
                "getListingsByStatus" -> listings(database.harvestListingDao().getListingsByStatus(a[0] as ListingStatus))
                "getListingById" -> database.harvestListingDao().getListingById(id).result()
                else -> error("Unsupported local listing read: $name")
            }
            BidRepository::class.java -> when (name) {
                "getBidsForListing" -> database.bidDao().getBidsForListing(id).result()
                "getBidsBySupplier" -> database.bidDao().getBidsBySupplier(id).result()
                "getBidById" -> database.bidDao().getBidById(id).result()
                else -> error("Unsupported local bid read: $name")
            }
            OrderRequestRepository::class.java -> when (name) {
                "getRequestsForFarmer", "observeRequestsForFarmer" -> database.orderRequestDao().getRequestsForFarmer(id).result()
                "getRequestsForSupplier" -> database.orderRequestDao().getRequestsForSupplier(id).result()
                "getRequestsForListing" -> database.orderRequestDao().getRequestsForListing(id).result()
                "getRequestById" -> database.orderRequestDao().getRequestById(id).result()
                else -> error("Unsupported local request read: $name")
            }
            OrderRepository::class.java -> when (name) {
                "getOrdersForFarmer" -> database.orderDao().getOrdersForFarmer(id).result()
                "getOrdersForSupplier" -> database.orderDao().getOrdersForSupplier(id).result()
                "getOrderById" -> database.orderDao().getOrderById(id).result()
                else -> error("Unsupported local order read: $name")
            }
            ReviewRepository::class.java -> when (name) {
                "getReviewsForUser" -> database.reviewDao().getReviewsForUser(id).result()
                "getReviewsForOrder" -> database.reviewDao().getReviewsByOrderId(id).result()
                else -> error("Unsupported local review read: $name")
            }
            LeaderboardRepository::class.java -> when (name) {
                "getFarmerLeaderboard" -> database.userProfileDao().getUsersByRoleSortedByPoints("FARMER").result()
                "getSupplierLeaderboard" -> database.userProfileDao().getUsersByRoleSortedByPoints("SUPPLIER").result()
                else -> error("Unsupported local leaderboard read: $name")
            }
            else -> error("No local adapter for ${type.name}")
        }
    }
    private suspend fun suspendRead(type: Class<*>, name: String, a: Array<out Any?>): Any? {
        if (name in refreshNames) return Result.Success(Unit) // Local refresh; never a transaction acknowledgement.
        return when {
            type == AuthRepository::class.java && name == "restoreSession" ->
                Result.Success(session.state.value.selectedDemoUserId?.let { database.userProfileDao().getUserProfile(it).first() })
            type == ReviewRepository::class.java && name == "hasUserReviewedOrder" ->
                Result.Success(database.reviewDao().getReviewByOrderAndReviewer(a[0] as String,a[1] as String) != null)
            type == LeaderboardRepository::class.java && name in setOf("getFarmerRank","getSupplierRank") -> {
                val role = if (name == "getFarmerRank") "FARMER" else "SUPPLIER"
                val index = database.userProfileDao().getUsersByRoleSortedByPoints(role).first().indexOfFirst { it.userId == a[0] }
                Result.Success(if (index < 0) null else index+1)
            }
            else -> Result.Error(UNAVAILABLE)
        }
    }
}
