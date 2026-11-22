package com.campusconnect.marketplace.ui.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.ui.theme.GreenDark
import com.campusconnect.marketplace.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedItemsScreen(
    onListingClick: (Listing) -> Unit,
    savedItemsViewModel: SavedItemsViewModel = viewModel()
) {
    val listings by savedItemsViewModel.savedListings.collectAsState()
    val isLoading by savedItemsViewModel.isLoading.collectAsState()

    // THIS IS THE FIX: Refresh the data every time this screen is opened
    LaunchedEffect(Unit) {
        savedItemsViewModel.fetchSavedListings()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GreenPrimary, GreenDark)))
                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
        ) {
            Column {
                Text(
                    text = "Saved Items",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "Items you loved and want to buy later",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )
            }
        }

        // --- CONTENT ---
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (listings.isEmpty()) {

            // --- BEAUTIFUL EMPTY STATE ---
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = "Empty Wishlist",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                    modifier = Modifier.size(140.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No saved items yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the heart on any listing to save it here for later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

        } else {
            // --- GRID OF SAVED ITEMS ---
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listings) { listing ->
                    GridListingCard(listing, onClick = { onListingClick(listing) })
                }
            }
        }
    }
}