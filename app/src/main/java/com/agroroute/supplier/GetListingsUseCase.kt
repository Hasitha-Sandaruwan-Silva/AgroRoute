package com.agroroute.supplier

import com.agroroute.common.model.HarvestListing
import com.agroroute.common.model.ListingStatus
import com.agroroute.common.util.Result
import com.agroroute.farmer.HarvestListingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Retrieves harvest listings — all listings or filtered by farmer / status depending on params.
 */
class GetListingsUseCase @Inject constructor(
    private val repository: HarvestListingRepository
) {
    /** Returns all available (OPEN) listings for the supplier marketplace. */
    fun getOpenListings(): Flow<Result<List<HarvestListing>>> =
        repository.getListingsByStatus(ListingStatus.OPEN)

    /** Returns all listings created by a specific farmer. */
    fun getByFarmer(farmerId: String): Flow<Result<List<HarvestListing>>> =
        repository.getListingsByFarmer(farmerId)

    /** Returns all listings regardless of status. */
    fun getAll(): Flow<Result<List<HarvestListing>>> =
        repository.getAllListings()
}
