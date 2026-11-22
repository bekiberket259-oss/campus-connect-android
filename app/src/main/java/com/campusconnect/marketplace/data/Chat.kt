package com.campusconnect.marketplace.data

import kotlinx.serialization.Serializable

@Serializable
data class Conversation(
    val id: String? = null,
    val listing_id: String,
    val buyer_id: String,
    val seller_id: String,
    val created_at: String? = null
)

@Serializable
data class Message(
    val id: String? = null,
    val conversation_id: String,
    val sender_id: String,
    val content: String,
    val created_at: String? = null,

)

// NEW: Model to hold the other person's profile info
@Serializable
data class ChatUser(
    val id: String,
    val full_name: String? = "Student",
    val avatar_url: String? = null
)