package com.agroroute.data.local.dao

import androidx.room.*
import com.agroroute.data.model.HarvestListing
import com.agroroute.data.model.ListingStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for all CRUD operations on the [HarvestListing] table.
 */
@Dao
interface HarvestListingDao {

    @Query("SELECT * FROM harvest_listings ORDER BY createdAt DESC")
    fun getAllListings(): Flow<List<HarvestListing>>

    @Query("SELECT * FROM harvest_listings WHERE farmerId = :farmerId ORDER BY createdAt DESC")
    fun getListingsByFarmer(farmerId: String): Flow<List<HarvestListing>>

    @Query("SELECT * FROM harvest_listings WHERE status = :status ORDER BY createdAt DESC")
    fun getListingsByStatus(status: ListingStatus): Flow<List<HarvestListing>>

    @Query("SELECT * FROM harvest_listings WHERE listingId = :listingId")
    fun getListingById(listingId: String): Flow<HarvestListing?>

    @Upsert
    suspend fun upsertListing(listing: HarvestListing)

    @Upsert
    suspend fun upsertListings(listings: List<HarvestListing>)

    @Query("DELETE FROM harvest_listings WHERE listingId = :listingId")
    suspend fun deleteListing(listingId: String)

    @Query("DELETE FROM harvest_listings")
    suspend fun deleteAll()
}
