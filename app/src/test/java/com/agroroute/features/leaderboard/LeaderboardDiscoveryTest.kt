package com.agroroute.features.leaderboard

import androidx.lifecycle.ViewModelStore
import com.agroroute.core.model.*
import com.agroroute.core.repository.*
import com.agroroute.core.util.Result
import com.agroroute.features.participants.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardDiscoveryTest {
    @Before fun main() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun reset() { Dispatchers.resetMain() }
    private val farmers = listOf(UserProfile(userId = "f1", role = UserRole.FARMER, points = 40), UserProfile(userId = "f2", role = UserRole.FARMER, points = 10))
    private val suppliers = listOf(UserProfile(userId = "s1", role = UserRole.SUPPLIER, points = 60), UserProfile(userId = "s2", role = UserRole.SUPPLIER, points = 40))
    @Test fun bothRolePreviewsReuseRankedFlowsAndOneRefreshWithoutRowQueries() = runTest {
        val repo = mockk<LeaderboardRepository>()
        val reviews = mockk<ReviewRepository>()
        val farmerFlow = MutableStateFlow<Result<List<UserProfile>>>(Result.Success(farmers))
        every { repo.getFarmerLeaderboard() } returns farmerFlow
        every { repo.getSupplierLeaderboard() } returns MutableStateFlow(Result.Success(suppliers))
        coEvery { repo.syncLeaderboard() } returns Result.Success(Unit)
        val vm = LeaderboardViewModel(repo, reviews); val store = ViewModelStore().apply { put("leaders", vm) }
        try {
            vm.loadCommunity(); vm.loadCommunity()
            assertEquals(farmers, vm.communityState.value.farmers)
            assertEquals(suppliers, vm.communityState.value.suppliers)
            assertTrue(vm.communityState.value.farmers.all { it.role == UserRole.FARMER })
            assertTrue(vm.communityState.value.suppliers.all { it.role == UserRole.SUPPLIER })
            coVerify(exactly = 1) { repo.syncLeaderboard() }
            verify(exactly = 1) { repo.getFarmerLeaderboard(); repo.getSupplierLeaderboard() }
            verify { reviews wasNot Called }
            val sameScoreOrder = farmers.map { it.copy(points = 40) }.reversed()
            farmerFlow.value = Result.Success(sameScoreOrder)
            assertEquals(sameScoreOrder, vm.communityState.value.farmers) // no new tie-break or ranking formula
        } finally { store.clear() }
    }
    @Test fun fullLeaderboardSwitchClearsPreviousRoleAndPreservesRepositoryOrder() = runTest {
        val repo = mockk<LeaderboardRepository>(); val reviews = mockk<ReviewRepository>()
        coEvery { repo.syncLeaderboard() } returns Result.Success(Unit)
        every { repo.getFarmerLeaderboard() } returns flowOf(Result.Success(farmers))
        val next = MutableSharedFlow<Result<List<UserProfile>>>()
        every { repo.getSupplierLeaderboard() } returns next
        every { reviews.getReviewsForUser(any()) } returns flowOf(Result.Success(emptyList()))
        val vm = LeaderboardViewModel(repo, reviews); val store = ViewModelStore().apply { put("leaders", vm) }
        try {
            vm.loadLeaderboard("FARMER"); assertEquals(farmers, vm.uiState.value.users)
            vm.loadLeaderboard("SUPPLIER"); assertTrue(vm.uiState.value.users.isEmpty())
            next.emit(Result.Success(suppliers)); assertEquals(suppliers, vm.uiState.value.users)
        } finally { store.clear() }
    }
    @Test fun refreshFailureKeepsSavedRankingsAndRetryRefreshesOnce() = runTest {
        val repo = mockk<LeaderboardRepository>()
        coEvery { repo.syncLeaderboard() } returns Result.Error("offline")
        every { repo.getFarmerLeaderboard() } returns MutableStateFlow(Result.Success(farmers))
        every { repo.getSupplierLeaderboard() } returns MutableStateFlow(Result.Success(suppliers))
        val vm = LeaderboardViewModel(repo, mockk()); val store = ViewModelStore().apply { put("leaders", vm) }
        try {
            vm.loadCommunity(); assertEquals(farmers, vm.communityState.value.farmers); assertNotNull(vm.communityState.value.error)
            vm.retryCommunity(); coVerify(exactly = 2) { repo.syncLeaderboard() }
        } finally { store.clear() }
    }
    @Test fun missingParticipantDoesNotBreakHistoricalOrderPresentation() = runTest {
        val people = mockk<ParticipantRepository>()
        coEvery { people.identity("deleted") } throws IllegalStateException("missing")
        val vm = OrderParticipantViewModel(people); vm.load("deleted")
        assertNull(vm.uiState.value.identity); assertFalse(vm.uiState.value.loading)
    }
}
