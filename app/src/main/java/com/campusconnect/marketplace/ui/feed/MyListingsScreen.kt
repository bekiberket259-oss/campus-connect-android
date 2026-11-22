package com.campusconnect.marketplace.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.ui.theme.GreenDark
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import io.github.jan.supabase.storage.storage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListingsScreen(
    onListingClick: (Listing) -> Unit,
    myListingsViewModel: MyListingsViewModel = viewModel()
) {
    val listings by myListingsViewModel.myListings.collectAsState()
    val isLoading by myListingsViewModel.isLoading.collectAsState()

    // Calculate Analytics
    val totalViews = listings.sumOf { it.views_count }
    val totalListings = listings.size
    val activeListings = listings.count { it.status == "active" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- SELLER ANALYTICS HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GreenPrimary, GreenDark)))
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Column {
                Text(
                    text = "Seller Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Track your listing performance",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // STATS ROW
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCard(title = "Total Views", value = totalViews.toString(), icon = Icons.Default.Visibility)
                    StatCard(title = "Active", value = activeListings.toString(), icon = Icons.Default.CheckCircle)
                    StatCard(title = "Total", value = totalListings.toString(), icon = Icons.Default.Inventory)
                }
            }
        }

        // --- LISTINGS LIST OR EMPTY STATE ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (listings.isEmpty()) {

            // BEAUTIFUL EMPTY STATE
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), // Faint gray
                    modifier = Modifier.size(140.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No listings yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Items you post will appear here. Tap the + button to create your first listing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

        } else {
            // GRID/LIST OF LISTINGS
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listings) { listing ->
                    AdvancedMyListingCard(listing, onClick = { onListingClick(listing) })
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = title, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun AdvancedMyListingCard(listing: Listing, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            ) {
                if (listing.image_paths.isNotEmpty()) {
                    val imageUrl = supabase.storage.from("listing-images").publicUrl(listing.image_paths.first())
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Listing Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listing.formattedPrice(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // SHOW ENGAGEMENT STATS
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Visibility, contentDescription = "Views", modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Text(" ${listing.views_count}", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(modifier = Modifier.width(12.dp))

                    val (statusText, statusColor) = when (listing.status) {
                        "active" -> "ACTIVE" to Color(0xFF4CAF50)
                        "sold" -> "SOLD" to MaterialTheme.colorScheme.error
                        else -> "UNKNOWN" to Color.Gray
                    }
                    Surface(color = statusColor.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "View", tint = Color.LightGray)
        }
    }
}