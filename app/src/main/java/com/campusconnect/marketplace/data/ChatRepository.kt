package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow

class ChatRepository {

    suspend fun getOrCreateConversation(listingId: String, sellerId: String): Conversation {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
        val existing = supabase.from("conversations")
            .select { filter { eq("listing_id", listingId); eq("buyer_id", currentUser.id) } }
            .decodeSingleOrNull<Conversation>()
        if (existing != null) return existing

        val newConversation = Conversation(listing_id = listingId, buyer_id = currentUser.id, seller_id = sellerId)
        return supabase.from("conversations").insert(newConversation) { select() }.decodeSingle<Conversation>()
    }

    // NEW: Get a single conversation
    suspend fun getConversation(conversationId: String): Conversation? {
        return supabase.from("conversations")
            .select { filter { eq("id", conversationId) } }
            .decodeSingleOrNull<Conversation>()
    }

    // NEW: Get a user's profile
    suspend fun getUserProfile(userId: String): ChatUser? {
        return supabase.from("profiles")
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull<ChatUser>()
    }

    suspend fun getMessages(conversationId: String): List<Message> {
        return supabase.from("messages")
            .select { filter { eq("conversation_id", conversationId) }; order("created_at", Order.ASCENDING) }
            .decodeList<Message>()
    }

    suspend fun sendMessage(conversationId: String, content: String) {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
        val message = Message(conversation_id = conversationId, sender_id = currentUser.id, content = content)
        supabase.from("messages").insert(message)
    }

    suspend fun getMyConversations(): List<Conversation> {
        val currentUser = supabase.auth.currentUserOrNull() ?: return emptyList()
        return supabase.from("conversations")
            .select { filter { or { eq("buyer_id", currentUser.id); eq("seller_id", currentUser.id) } }; order("created_at", Order.DESCENDING) }
            .decodeList<Conversation>()
    }

    suspend fun subscribeToMessages(conversationId: String): Flow<PostgresAction.Insert> {
        val channel = supabase.channel("chat_$conversationId")
        val changeFlow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }
        channel.subscribe(blockUntilSubscribed = true)
        return changeFlow
    }
}