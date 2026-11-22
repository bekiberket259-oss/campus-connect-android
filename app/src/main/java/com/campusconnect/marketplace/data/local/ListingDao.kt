package com.campusconnect.marketplace.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ListingDao {
    @Query("SELECT * FROM cached_listings WHERE status = 'active'")
    suspend fun getCachedListings(): List<ListingEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListings(listings: List<ListingEntity>)

    @Query("DELETE FROM cached_listings")
    suspend fun clearCache()
}