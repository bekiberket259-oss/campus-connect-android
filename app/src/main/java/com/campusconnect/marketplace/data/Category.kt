package com.campusconnect.marketplace.data

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name_en: String,
    val name_am: String? = null,
    val slug: String? = null,
    val is_active: Boolean? = true
)