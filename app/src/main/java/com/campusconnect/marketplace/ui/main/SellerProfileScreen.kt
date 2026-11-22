package com.campusconnect.marketplace.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.SellerReview
import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.ui.feed.GridListingCard
import com.campusconnect.marketplace.ui.theme.GreenDark
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import io.github.jan.supabase.auth.auth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    navController: NavController,
    sellerId: String,
    sellerProfileViewModel: SellerProfileViewModel = viewModel()
) {
    val sellerInfo by sellerProfileViewModel.sellerInfo.collectAsState()
    val sellerRating by sellerProfileViewModel.sellerRating.collectAsState()
    val sellerListings by sellerProfileViewModel.sellerListings.collectAsState()
    val sellerReviews by sellerProfileViewModel.sellerReviews.collectAsState()
    val isLoading by sellerProfileViewModel.isLoading.collectAsState()
    val isSubmittingReview by sellerProfileViewModel.isSubmittingReview.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var listingToReview by remember { mutableStateOf<Listing?>(null) }
    val currentUserId = supabase.auth.currentUserOrNull()?.id

    LaunchedEffect(sellerId) {
        sellerProfileViewModel.loadSellerData(sellerId)
    }

    if (listingToReview != null) {
        ReviewDialog(
            sellerName = sellerInfo?.full_name ?: "Seller",
            isSubmitting = isSubmittingReview,
            onDismiss = { listingToReview = null },
            onSubmit = { rating, comment ->
                sellerProfileViewModel.submitReview(listingToReview!!.id, rating, comment) {
                    listingToReview = null
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Brush.verticalGradient(listOf(GreenPrimary, GreenDark)))
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.padding(16.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(Color.White),
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
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(sellerInfo?.full_name ?: "Seller", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Star", tint = Color(0xFFFFC107), modifier = Modifier.size(18.dp))
                    Text(
                        text = "${sellerRating?.average_rating ?: 0.0} (${sellerRating?.total_reviews ?: 0} reviews)",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // TABS
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = GreenPrimary
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Listings (${sellerListings.size})", fontWeight = FontWeight.Bold) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reviews (${sellerReviews.size})", fontWeight = FontWeight.Bold) })
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GreenPrimary)
            }
        } else {
            if (selectedTab == 0) {
                // TAB 1: LISTINGS
                if (sellerListings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("This seller has no listings.", color = Color.Gray)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sellerListings) { listing ->
                            Column {
                                GridListingCard(listing, onClick = { /* Navigate to detail (optional) */ })

                                if (listing.status == "sold" && currentUserId != listing.seller_id) {
                                    Button(
                                        onClick = { listingToReview = listing },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFC107), contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Star, contentDescription = "Rate", modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Rate Seller", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // TAB 2: REVIEWS
                if (sellerReviews.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No reviews yet.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(sellerReviews) { review ->
                            ReviewCard(review)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewCard(review: SellerReview) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star",
                        tint = if (index < review.rating) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (!review.comment.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "\"${review.comment}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
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