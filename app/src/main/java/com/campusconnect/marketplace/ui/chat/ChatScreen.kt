
package com.campusconnect.marketplace.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    navController: NavController,
    conversationId: String,
    chatViewModel: ChatViewModel = viewModel()
) {
    val messages by chatViewModel.messages.collectAsState()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }

    val currentUserId = supabase.auth.currentUserOrNull()?.id

    LaunchedEffect(conversationId) {
        chatViewModel.initChat(conversationId)
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chat",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {

            // MESSAGE LIST
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { message ->

                    val isMine =
                        message.sender_id == currentUserId

                    MessageBubble(
                        message = message,
                        isMine = isMine
                    )
                }
            }

            // INPUT BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                    },
                    placeholder = {
                        Text("Type a message...")
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )

                Spacer(
                    modifier = Modifier.width(8.dp)
                )

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            chatViewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    containerColor = GreenPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: com.campusconnect.marketplace.data.Message,
    isMine: Boolean
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMine) {
            Alignment.CenterEnd
        } else {
            Alignment.CenterStart
        }
    ) {

        Surface(
            color = if (isMine) {
                GreenPrimary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMine) 16.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 16.dp
            )
        ) {

            Text(
                text = message.content,
                color = if (isMine) {
                    Color.White
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                fontSize = 16.sp,
                modifier = Modifier.padding(
                    horizontal = 16.dp,
                    vertical = 10.dp
                )
            )
        }
    }
}
