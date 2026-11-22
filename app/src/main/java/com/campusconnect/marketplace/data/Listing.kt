package com.campusconnect.marketplace.data

import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.Duration
import java.util.Locale // <--- IMPORTANT FOR FORMATTING

@Serializable
data class Listing(
    val id: String,
    val seller_id: String,
    val category_id: String,
    val title: String,
    val description: String,
    val price_cents: Int,
    val pickup_location: String,
    val status: String,
    val image_paths: List<String> = emptyList(),
    val created_at: String? = null,
    val expires_at: String? = null,
    val is_negotiable: Boolean = true,
    val views_count: Int = 0,
    val condition: String? = "Used"
) {

    // UPDATED: Now includes comma separation AND decimal places
    // Example: 23000 cents -> "ETB 230.00"
    // Example: 2300000 cents -> "ETB 23,000.00"
    fun formattedPrice(): String {
        return String.format(Locale.US, "ETB %,.2f", price_cents / 100.0)
    }

    fun formatDate(isoString: String?): String {
        if (isoString == null) return "Unknown"
        return try {
            isoString.take(10)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    fun getDaysRemaining(): Long {
        if (expires_at == null) return 0
        return try {
            val expiryDate = OffsetDateTime.parse(expires_at)
            val now = OffsetDateTime.now()
            val duration = Duration.between(now, expiryDate)
            duration.toDays()
        } catch (e: Exception) {
            0L
        }
    }
}