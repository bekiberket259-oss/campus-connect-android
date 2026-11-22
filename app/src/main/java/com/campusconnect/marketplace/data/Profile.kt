package com.campusconnect.marketplace.data

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val full_name: String,
    val verification_status: String,
    val avatar_url: String? = null,
    val is_suspended: Boolean? = false, // <--- NEW
    val suspended_until: String? = null // <--- NEW
)