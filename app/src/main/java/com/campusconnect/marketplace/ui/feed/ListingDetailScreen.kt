package com.campusconnect.marketplace.ui.feed

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusconnect.marketplace.data.ChatRepository
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    navController: NavController,
    listing: Listing,
    onEditClick: (Listing) -> Unit,
    onSellerClick: (String) -> Unit,
    listingDetailViewModel: ListingDetailViewModel = viewModel()
) {
    val isActionLoading by listingDetailViewModel.isActionLoading.collectAsState()
    val sellerContact by listingDetailViewModel.sellerContact.collectAsState()
    val sellerInfo by listingDetailViewModel.sellerInfo.collectAsState()
    val contactError by listingDetailViewModel.contactError.collectAsState()
    val isFavorited by listingDetailViewModel.isFavorited.collectAsState()
    val isSubmittingReview by listingDetailViewModel.isSubmittingReview.collectAsState()

    val currentUser = supabase.auth.currentUserOrNull()
    val isOwner = currentUser?.id == listing.seller_id
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Chat Repository & State
    val chatRepo = remember { ChatRepository() }
    var isStartingChat by remember { mutableStateOf(false) }

    var showReportDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listing.id) {
        listingDetailViewModel.loadSellerInfo(listing.id, listing.seller_id)
    }

    if (showReportDialog) {
        ReportDialog(
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, comment ->
                scope.launch {
                    try {
                        listingDetailViewModel.reportListing(listing.id, reason, comment) {
                            showReportDialog = false
                        }
                    } catch (e: Exception) { }
                }
            }
        )
    }

    if (showReviewDialog) {
        ReviewDialog(
            sellerName = sellerInfo?.full_name ?: "Seller",
            isSubmitting = isSubmittingReview,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment ->
                listingDetailViewModel.submitReview(listing.seller_id, listing.id, rating, comment) {
                    showReviewDialog = false
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (listing.status == "active") {
                Surface(
                    shadowElevation = 16.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isOwner) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { onEditClick(listing) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text("Edit") }
                                Button(
                                    onClick = { listingDetailViewModel.markAsSold(listing.id) { navController.popBackStack() } },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isActionLoading
                                ) { Text("Mark Sold") }
                                IconButton(
                                    onClick = { listingDetailViewModel.deleteListing(listing.id) { navController.popBackStack() } },
                                    enabled = !isActionLoading
                                ) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
                            }
                        } else {
                            if (contactError != null) {
                                Text(
                                    text = contactError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            // --- CHAT BUTTON FOR BUYERS ---
                            Button(
                                onClick = {
                                    if (isStartingChat) return@Button
                                    isStartingChat = true
                                    scope.launch {
                                        try {
                                            val conversation = chatRepo.getOrCreateConversation(listing.id, listing.seller_id)
                                            navController.navigate("chat/${conversation.id}")
                                        } catch (e: Exception) {
                                            Log.e("ChatError", "Failed to create chat: ${e.message}")
                                            Toast.makeText(context, "Failed to open chat: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isStartingChat = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                                enabled = !isStartingChat
                            ) {
                                if (isStartingChat) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                                } else {
                                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Message")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Message Seller", fontWeight = FontWeight.Bold)
                                }
                            }

                            // Call & Telegram Buttons
                            if (sellerContact != null && (!sellerContact?.phone.isNullOrBlank() || !sellerContact?.telegram_username.isNullOrBlank())) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (!sellerContact?.telegram_username.isNullOrBlank()) {
                                        Button(
                                            onClick = {
                                                val username = sellerContact?.telegram_username?.removePrefix("@")
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/$username")))
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC))
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = "Telegram", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Telegram")
                                        }
                                    }
                                    if (!sellerContact?.phone.isNullOrBlank()) {
                                        Button(
                                            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${sellerContact?.phone}"))) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                        ) {
                                            Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Call")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- SWIPEABLE IMAGE CAROUSEL ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    if (listing.image_paths.isNotEmpty()) {
                        val pagerState = rememberPagerState(pageCount = { listing.image_paths.size })

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val imageUrl = supabase.storage.from("listing-images").publicUrl(listing.image_paths[page])
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Listing Image ${page + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        if (listing.image_paths.size > 1) {
                            Row(
                                Modifier
                                    .height(50.dp)
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                repeat(listing.image_paths.size) { iteration ->
                                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().background(Color.LightGray), contentAlignment = Alignment.Center) {
                            Text("No Image", color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                                )
                            )
                    )

                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .background(Color.White.copy(alpha = 0.9f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }

                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd)
                    ) {
                        if (!isOwner) {
                            IconButton(
                                onClick = { listingDetailViewModel.toggleFavorite(listing.id) },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.9f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isFavorited) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Favorite",
                                    tint = if (isFavorited) Color.Red else Color.Black
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { showReportDialog = true },
                                modifier = Modifier.background(Color.White.copy(alpha = 0.9f), CircleShape)
                            ) {
                                Icon(Icons.Default.Flag, contentDescription = "Report", tint = Color.Red)
                            }
                        }
                    }
                }
            }

            // --- CONTENT SHEET ---
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-32).dp)
                        .background(
                            color = MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                        )
                        .padding(24.dp)
                ) {
                    if (listing.status == "sold") {
                        Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(8.dp)) {
                            Text("SOLD", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text(
                        text = listing.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = listing.formattedPrice(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = listing.condition ?: "Used",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (listing.is_negotiable) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Negotiable",
                                    color = MaterialTheme.colorScheme.secondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // --- EXPIRY COUNTDOWN BADGE ---
                    val daysLeft = listing.getDaysRemaining()
                    if (listing.status == "active" && daysLeft > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        val isUrgent = daysLeft <= 3
                        val badgeColor = if (isUrgent) MaterialTheme.colorScheme.error else Color(0xFFF59E0B)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(badgeColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Time remaining",
                                tint = badgeColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isUrgent) "Expires in $daysLeft days (Hurry!)" else "Expires in $daysLeft days",
                                color = badgeColor,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // --- RATE SELLER BUTTON ---
                    if (listing.status == "sold" && !isOwner) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showReviewDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFC107),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Rate")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Rate this Seller", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // SELLER CARD
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSellerClick(listing.seller_id) }
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sellerInfo?.avatar_url != null) {
                                    AsyncImage(
                                        model = sellerInfo?.avatar_url,
                                        contentDescription = "Seller Avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = sellerInfo?.full_name?.firstOrNull()?.uppercase() ?: "S",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Seller", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Text(sellerInfo?.full_name ?: "Student", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View Profile", tint = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = listing.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = listing.pickup_location, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDialog(onDismiss: () -> Unit, onSubmit: (String, String) -> Unit) {
    var selectedReason by remember { mutableStateOf("scam_or_fraud") }
    var comment by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val reasons = mapOf(
        "prohibited_item" to "Prohibited Item",
        "scam_or_fraud" to "Scam or Fraud",
        "incorrect_info" to "Incorrect Information",
        "offensive_content" to "Offensive Content",
        "other" to "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Listing") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = reasons[selectedReason] ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Reason") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        reasons.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    selectedReason = key
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Additional Comments (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSubmit(selectedReason, comment) }) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewDialog(
    sellerName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String) -> Unit
) {
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rate $sellerName") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    (1..5).forEach { star ->
                        IconButton(onClick = { rating = star }) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Star $star",
                                tint = if (star <= rating) Color(0xFFFFC107) else Color.LightGray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Leave a comment (Optional)") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    maxLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, comment) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
            ) {
                if (isSubmitting) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                else Text("Submit Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}