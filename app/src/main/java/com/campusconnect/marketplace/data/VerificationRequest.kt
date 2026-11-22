package com.campusconnect.marketplace.data

import kotlinx.serialization.Serializable

@Serializable
data class VerificationRequest(
    val id: String? = null,
    val user_id: String,
    val student_id_number: String,
    val full_name: String,
    val department: String,
    val status: String = "pending"
)