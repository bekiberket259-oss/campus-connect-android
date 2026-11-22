package com.campusconnect.marketplace.data

import kotlinx.serialization.Serializable

@Serializable
data class Report(
    val id: String? = null,
    val listing_id: String,
    val reporter_id: String,
    val reason: String,
    val comment: String? = null,
    val status: String = "open"
)