package com.campusconnect.marketplace.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusconnect.marketplace.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InboxScreen(
    navController: NavController,
    inboxViewModel: InboxViewModel = viewModel()
) {
    val conversations by inboxViewModel.conversations.collectAsState()
    val isLoading by inboxViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        inboxViewModel.fetchConversations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Messages", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),            // FIX: respect Scaffold insets
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else if (conversations.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)             // FIX: respect Scaffold insets
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = "No Messages",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    modifier = Modifier.size(120.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No messages yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center   // FIX: center text
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your conversations will appear here.",
                    color = Color.Gray,
                    textAlign = TextAlign.Center   // FIX: center text
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)              // FIX: respect Scaffold insets
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(conversations) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("chat/${item.conversation.id}")
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()    // FIX: row spans the full card
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(GreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.otherUser?.avatar_url != null) {
                                    AsyncImage(
                                        model = item.otherUser?.avatar_url,
                                        contentDescription = "Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = item.otherUser?.full_name?.firstOrNull()?.uppercase() ?: "U",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(
                                modifier = Modifier.weight(1f),       // FIX: text fills remaining width
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = item.otherUser?.full_name ?: "Student",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Listing ID: ${item.conversation.listing_id.take(8)}...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}