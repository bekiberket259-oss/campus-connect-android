package com.campusconnect.marketplace.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.ChatRepository
import com.campusconnect.marketplace.data.ChatUser
import com.campusconnect.marketplace.data.Conversation
import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Helper data class for the UI
data class ConversationWithUser(
    val conversation: Conversation,
    val otherUser: ChatUser?
)

class InboxViewModel : ViewModel() {
    private val repository = ChatRepository()

    private val _conversations = MutableStateFlow<List<ConversationWithUser>>(emptyList())
    val conversations: StateFlow<List<ConversationWithUser>> = _conversations

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun fetchConversations() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val convos = repository.getMyConversations()
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                val result = mutableListOf<ConversationWithUser>()

                for (convo in convos) {
                    val otherUserId = if (convo.buyer_id == currentUserId) convo.seller_id else convo.buyer_id
                    val otherUser = repository.getUserProfile(otherUserId)
                    result.add(ConversationWithUser(convo, otherUser))
                }
                _conversations.value = result
            } catch (e: Exception) {
                _conversations.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}