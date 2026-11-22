package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class SellerReview(
    val id: String? = null,
    val seller_id: String,
    val reviewer_id: String,
    val listing_id: String,
    val rating: Int,
    val comment: String? = null,
    val created_at: String? = null
)

@Serializable
data class SellerRating(
    val average_rating: Double? = 0.0,
    val total_reviews: Long = 0
)

class ReviewRepository {

    // 1. Fetch Average Rating
    suspend fun getSellerRating(sellerId: String): SellerRating {
        return try {
            val result = supabase.postgrest.rpc(
                function = "get_seller_rating",
                parameters = buildJsonObject { put("p_seller_id", sellerId) }
            )
            result.decodeList<SellerRating>().firstOrNull() ?: SellerRating()
        } catch (e: Exception) {
            SellerRating()
        }
    }

    // 2. Fetch Reviews
    suspend fun getSellerReviews(sellerId: String): List<SellerReview> {
        return try {
            supabase.from("seller_reviews")
                .select {
                    filter { eq("seller_id", sellerId) }
                }
                .decodeList<SellerReview>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 3. Submit a new Review (THIS WAS MISSING)
    suspend fun submitReview(sellerId: String, listingId: String, rating: Int, comment: String) {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        val review = SellerReview(
            seller_id = sellerId,
            reviewer_id = currentUser.id,
            listing_id = listingId,
            rating = rating,
            comment = comment.ifBlank { null }
        )
        supabase.from("seller_reviews").insert(review)
    }
}