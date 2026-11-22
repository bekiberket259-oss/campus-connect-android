package com.campusconnect.marketplace.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.campusconnect.marketplace.data.Listing

@Entity(tableName = "cached_listings")
data class ListingEntity(
    @PrimaryKey val id: String,
    val seller_id: String,
    val category_id: String,
    val title: String,
    val description: String,
    val price_cents: Int,
    val pickup_location: String,
    val status: String,
    val image_paths: String, // Stored as a comma-separated string
    val created_at: String?,
    val expires_at: String?,
    val is_negotiable: Boolean,
    val views_count: Int,
    val condition: String?
)

// Maps the Room Entity back to the App's Data Model
fun ListingEntity.toDomain(): Listing {
    return Listing(
        id = id,
        seller_id = seller_id,
        category_id = category_id,
        title = title,
        description = description,
        price_cents = price_cents,
        pickup_location = pickup_location,
        status = status,
        image_paths = if (image_paths.isEmpty()) emptyList() else image_paths.split(","),
        created_at = created_at,
        expires_at = expires_at,
        is_negotiable = is_negotiable,
        views_count = views_count,
        condition = condition
    )
}

// Maps the App's Data Model to the Room Entity
fun Listing.toEntity(): ListingEntity {
    return ListingEntity(
        id = id,
        seller_id = seller_id,
        category_id = category_id,
        title = title,
        description = description,
        price_cents = price_cents,
        pickup_location = pickup_location,
        status = status,
        image_paths = image_paths.joinToString(","),
        created_at = created_at,
        expires_at = expires_at,
        is_negotiable = is_negotiable,
        views_count = views_count,
        condition = condition
    )
}