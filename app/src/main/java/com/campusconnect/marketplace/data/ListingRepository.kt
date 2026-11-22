package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.data.local.ListingDao
import com.campusconnect.marketplace.data.local.toDomain
import com.campusconnect.marketplace.data.local.toEntity
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.serialization.Serializable

// Enum for sorting
enum class SortOption {
    NEWEST, PRICE_LOW_HIGH, PRICE_HIGH_LOW
}

@Serializable
data class CreateListingRequest(
    val seller_id: String,
    val category_id: String,
    val title: String,
    val description: String,
    val price_cents: Int,
    val pickup_location: String,
    val status: String = "active",
    val is_negotiable: Boolean = true,
    val condition: String = "Used"
)

@Serializable
data class SellerContact(
    val phone: String? = null,
    val telegram_username: String? = null
)

@Serializable
data class SellerInfo(
    val full_name: String = "Student",
    val avatar_url: String? = null
)

@Serializable
data class SavedListing(
    val id: String? = null,
    val user_id: String? = null,
    val listing_id: String? = null,
    val created_at: String? = null
)

class ListingRepository(private val listingDao: ListingDao? = null) {

    suspend fun getActiveListings(
        searchQuery: String = "",
        categoryId: String? = null,
        minPriceCents: Int? = null,
        maxPriceCents: Int? = null,
        condition: String? = null,
        sortBy: SortOption = SortOption.NEWEST,
        limit: Int = 10,
        offset: Int = 0
    ): List<Listing> {
        return try {
            // 1. Try to fetch from Supabase (Internet)
            val remoteListings = supabase.from("listings")
                .select {
                    filter {
                        eq("status", "active")
                        if (categoryId != null) eq("category_id", categoryId)
                        if (searchQuery.isNotBlank()) ilike("title", "%$searchQuery%")
                        if (minPriceCents != null) gte("price_cents", minPriceCents)
                        if (maxPriceCents != null) lte("price_cents", maxPriceCents)
                        if (condition != null) eq("condition", condition)
                    }
                    when (sortBy) {
                        SortOption.NEWEST -> order("created_at", Order.DESCENDING)
                        SortOption.PRICE_LOW_HIGH -> order("price_cents", Order.ASCENDING)
                        SortOption.PRICE_HIGH_LOW -> order("price_cents", Order.DESCENDING)
                    }
                    range(offset.toLong(), (offset + limit - 1).toLong())
                }
                .decodeList<Listing>()

            // 2. Save to Room Cache (Safely)
            if (listingDao != null && offset == 0 && searchQuery.isBlank() && categoryId == null && condition == null) {
                try {
                    listingDao.clearCache()
                    listingDao.insertListings(remoteListings.map { it.toEntity() })
                } catch (cacheError: Exception) {
                    // If Room fails, we don't crash the app. We just log it and move on.
                    android.util.Log.e("CacheError", "Failed to save to cache", cacheError)
                }
            }

            remoteListings
        } catch (e: Exception) {
            // 3. If internet fails, try to read from Room Cache
            if (listingDao != null && offset == 0) {
                try {
                    val cachedListings = listingDao.getCachedListings().map { it.toDomain() }
                    if (cachedListings.isNotEmpty()) {
                        return cachedListings
                    }
                } catch (cacheError: Exception) {
                    android.util.Log.e("CacheError", "Failed to read from cache", cacheError)
                }
            }
            throw e // If cache is empty and internet failed, throw the original error
        }
    }

    suspend fun getListingsBySeller(sellerId: String): List<Listing> {
        return supabase.from("listings")
            .select {
                filter {
                    eq("seller_id", sellerId)
                    neq("status", "removed")
                }
            }
            .decodeList<Listing>()
    }

    suspend fun getCategories(): List<Category> {
        return supabase.from("categories")
            .select()
            .decodeList<Category>()
    }

    suspend fun createListing(
        title: String,
        description: String,
        priceCents: Int,
        location: String,
        categoryId: String,
        isNegotiable: Boolean,
        condition: String,
        imageBytesList: List<ByteArray>?
    ) {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("User not logged in")

        val request = CreateListingRequest(
            seller_id = currentUser.id,
            category_id = categoryId,
            title = title,
            description = description,
            price_cents = priceCents,
            pickup_location = location,
            is_negotiable = isNegotiable,
            condition = condition
        )

        val insertedListing = supabase.from("listings").insert(request) {
            select()
        }.decodeSingle<Listing>()

        if (!imageBytesList.isNullOrEmpty()) {
            val uploadedPaths = mutableListOf<String>()

            for ((index, bytes) in imageBytesList.withIndex()) {
                val fileName = "image_${System.currentTimeMillis()}_$index.jpg"
                val path = "${currentUser.id}/${insertedListing.id}/$fileName"

                supabase.storage.from("listing-images").upload(path, bytes)
                uploadedPaths.add(path)
            }

            supabase.from("listings").update(
                {
                    set("image_paths", uploadedPaths)
                }
            ) {
                filter {
                    eq("id", insertedListing.id)
                }
            }
        }
    }

    suspend fun editListing(
        listingId: String,
        title: String,
        description: String,
        priceCents: Int,
        location: String,
        categoryId: String,
        isNegotiable: Boolean
    ) {
        supabase.from("listings").update(
            {
                set("title", title)
                set("description", description)
                set("price_cents", priceCents)
                set("pickup_location", location)
                set("category_id", categoryId)
                set("is_negotiable", isNegotiable)
            }
        ) {
            filter {
                eq("id", listingId)
            }
        }
    }

    suspend fun markAsSold(listingId: String) {
        supabase.from("listings").update(
            {
                set("status", "sold")
            }
        ) {
            filter {
                eq("id", listingId)
            }
        }
    }

    suspend fun deleteListing(listingId: String) {
        supabase.from("listings").update(
            {
                set("status", "removed")
            }
        ) {
            filter {
                eq("id", listingId)
            }
        }
    }

    suspend fun getSellerContact(listingId: String): SellerContact? {
        val result = supabase.postgrest.rpc(
            function = "get_seller_contact",
            parameters = mapOf("p_listing_id" to listingId)
        )
        return result.decodeList<SellerContact>().firstOrNull()
    }

    suspend fun getSellerInfo(sellerId: String): SellerInfo {
        return try {
            val profile = supabase.from("profiles")
                .select {
                    filter { eq("id", sellerId) }
                }
                .decodeSingleOrNull<SellerInfo>()
            profile ?: SellerInfo()
        } catch (e: Exception) {
            SellerInfo()
        }
    }

    suspend fun reportListing(listingId: String, reason: String, comment: String) {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        val report = Report(
            listing_id = listingId,
            reporter_id = currentUser.id,
            reason = reason,
            comment = comment.ifBlank { null }
        )

        supabase.from("reports").insert(report)
    }

    suspend fun getMyListings(): List<Listing> {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        return supabase.from("listings")
            .select {
                filter {
                    eq("seller_id", currentUser.id)
                    neq("status", "removed")
                }
            }
            .decodeList<Listing>()
    }

    // --- FAVORITES ---

    suspend fun incrementViewCount(listingId: String) {
        try {
            supabase.postgrest.rpc(
                function = "increment_listing_views",
                parameters = mapOf("p_listing_id" to listingId)
            )
        } catch (e: Exception) { /* Ignore if it fails */ }
    }

    suspend fun toggleFavorite(listingId: String): Boolean {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("User not logged in")

        val existing = supabase.from("saved_listings")
            .select { filter { eq("listing_id", listingId); eq("user_id", user.id) } }
            .decodeList<SavedListing>()

        if (existing.isNotEmpty()) {
            supabase.from("saved_listings").delete {
                filter {
                    eq("listing_id", listingId)
                    eq("user_id", user.id)
                }
            }
            return false
        } else {
            supabase.from("saved_listings").insert(
                mapOf(
                    "listing_id" to listingId,
                    "user_id" to user.id
                )
            )
            return true
        }
    }

    suspend fun isFavorited(listingId: String): Boolean {
        val user = supabase.auth.currentUserOrNull() ?: return false
        return try {
            val res = supabase.from("saved_listings")
                .select { filter { eq("listing_id", listingId); eq("user_id", user.id) } }
                .decodeList<SavedListing>()
            res.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getSavedListings(): List<Listing> {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        val saved = supabase.from("saved_listings")
            .select { filter { eq("user_id", user.id) } }
            .decodeList<SavedListing>()

        if (saved.isEmpty()) return emptyList()

        val listingIds = saved.mapNotNull { it.listing_id }
        return supabase.from("listings")
            .select { filter { isIn("id", listingIds) } }
            .decodeList<Listing>()
    }
}