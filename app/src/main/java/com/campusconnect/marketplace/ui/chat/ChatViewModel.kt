package com.campusconnect.marketplace.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.ChatRepository
import com.campusconnect.marketplace.data.ChatUser
import com.campusconnect.marketplace.data.Message
import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.decodeRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages

    // NEW: State to hold the other user's profile
    private val _otherUser = MutableStateFlow<ChatUser?>(null)
    val otherUser: StateFlow<ChatUser?> = _otherUser

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private var currentConversationId: String? = null

    fun initChat(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // 1. Fetch conversation to find out who the other person is
                val conversation = repository.getConversation(conversationId)
                val currentUserId = supabase.auth.currentUserOrNull()?.id
                if (conversation != null && currentUserId != null) {
                    val otherUserId = if (conversation.buyer_id == currentUserId) {
                        conversation.seller_id
                    } else {
                        conversation.buyer_id
                    }
                    _otherUser.value = repository.getUserProfile(otherUserId)
                }

                // 2. Load old messages
                _messages.value = repository.getMessages(conversationId)
                _isLoading.value = false

                // 3. Start listening for real-time updates
                listenForNewMessages(conversationId)
            } catch (e: Exception) {
                _isLoading.value = false
                e.printStackTrace()
            }
        }
    }

    private fun listenForNewMessages(conversationId: String) {
        viewModelScope.launch {
            repository.subscribeToMessages(conversationId).collect { action ->
                val newMessage = action.decodeRecord<Message>()
                if (_messages.value.none { it.id == newMessage.id }) {
                    _messages.value = _messages.value + newMessage
                }
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                val optimisticMessage = Message(
                    id = "temp_${System.currentTimeMillis()}",
                    conversation_id = conversationId,
                    sender_id = "me",
                    content = content
                )
                _messages.value = _messages.value + optimisticMessage
                repository.sendMessage(conversationId, content)
            } catch (e: Exception) {
                _messages.value = _messages.value.filter { !it.id!!.startsWith("temp_") }
                e.printStackTrace()
            }
        }
    }
}