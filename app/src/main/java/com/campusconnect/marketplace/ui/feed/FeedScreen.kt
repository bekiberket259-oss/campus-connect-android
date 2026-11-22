package com.campusconnect.marketplace.ui.feed

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campusconnect.marketplace.R
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.SortOption
import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import io.github.jan.supabase.storage.storage

// One consistent horizontal margin for header, search, chips and grid
private val ScreenPadding = 20.dp
private val HeaderCorner = 32.dp
// Half of the 56.dp search row: the search bar straddles the header's bottom edge
private val SearchOverlap = 28.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onListingClick: (Listing) -> Unit,
    onInboxClick: () -> Unit, // <--- NEW PARAMETER
    feedViewModel: FeedViewModel = viewModel()
) {
    val listings by feedViewModel.listings.collectAsState()
    val categories by feedViewModel.categories.collectAsState()
    val selectedCategoryId by feedViewModel.selectedCategoryId.collectAsState()

    val isLoading by feedViewModel.isLoading.collectAsState()
    val isRefreshing by feedViewModel.isRefreshing.collectAsState()
    val isLoadingMore by feedViewModel.isLoadingMore.collectAsState()
    val errorMessage by feedViewModel.errorMessage.collectAsState()
    val searchQuery by feedViewModel.searchQuery.collectAsState()

    val isOffline by feedViewModel.isOffline.collectAsState()

    val activeMinPrice by feedViewModel.minPrice.collectAsState()
    val activeMaxPrice by feedViewModel.maxPrice.collectAsState()
    val activeCondition by feedViewModel.selectedCondition.collectAsState()
    val currentSort by feedViewModel.sortOption.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val gridState = rememberLazyGridState()

    val filtersActive = activeMinPrice != null || activeMaxPrice != null || activeCondition != null
    val sortActive = currentSort != SortOption.NEWEST
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(gridState) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= listings.size - 2 && listings.isNotEmpty()) {
                    feedViewModel.loadMoreListings()
                }
            }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            currentMinPrice = activeMinPrice?.div(100)?.toString() ?: "",
            currentMaxPrice = activeMaxPrice?.div(100)?.toString() ?: "",
            currentCondition = activeCondition,
            onDismiss = { showFilterSheet = false },
            onApply = { min, max, cond ->
                feedViewModel.applyFilters(min, max, cond)
                showFilterSheet = false
            },
            onClear = {
                feedViewModel.clearFilters()
                showFilterSheet = false
            }
        )
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSort = currentSort,
            onDismiss = { showSortSheet = false },
            onSelect = { option ->
                feedViewModel.setSortOption(option)
                showSortSheet = false
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // HEADER + SEARCH (the colored header ends halfway down the search row,
        // so the search bar floats across the edge and stays fully tappable)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val corner = HeaderCorner.toPx()
                    // Extend upward by `corner` so only the bottom corners show as rounded
                    drawRoundRect(
                        color = primaryColor,
                        topLeft = Offset(0f, -corner),
                        size = Size(size.width, size.height - SearchOverlap.toPx() + corner),
                        cornerRadius = CornerRadius(corner, corner)
                    )
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, start = ScreenPadding, end = ScreenPadding, bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Discover",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Find the best deals on campus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // NEW: INBOX BUTTON
                    IconButton(
                        onClick = onInboxClick,
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Messages", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    IconButton(
                        onClick = { feedViewModel.refresh() },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            }

            // SEARCH BAR, FILTER & SORT
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { feedViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(6.dp, CircleShape),
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = { showFilterSheet = true },
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            color = if (filtersActive) GreenPrimary else MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filters",
                        tint = if (filtersActive) Color.White else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showSortSheet = true },
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(6.dp, CircleShape)
                        .background(
                            color = if (sortActive) GreenPrimary else MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = if (sortActive) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CATEGORY CHIPS
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ScreenPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { feedViewModel.selectCategory(null) },
                    label = { Text(stringResource(id = R.string.all), fontWeight = FontWeight.SemiBold) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategoryId == category.id,
                    onClick = { feedViewModel.selectCategory(category.id) },
                    label = { Text(category.name_en, fontWeight = FontWeight.SemiBold) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (isOffline) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You are offline. Showing cached listings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (errorMessage != null && listings.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = "😕", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Something went wrong",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = Color.Gray, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { feedViewModel.refresh() },
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try Again", fontWeight = FontWeight.Bold)
                }
            }
        } else if (listings.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SearchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No active listings found.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        } else {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { feedViewModel.refresh() },
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = ScreenPadding, end = ScreenPadding, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(listings) { listing ->
                        GridListingCard(listing, onClick = { onListingClick(listing) })
                    }
                    if (isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(32.dp), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onDismiss: () -> Unit,
    onSelect: (SortOption) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text("Sort By", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            SortOptionRow(title = "Newest First", icon = Icons.Default.Schedule, isSelected = currentSort == SortOption.NEWEST, onClick = { onSelect(SortOption.NEWEST) })
            SortOptionRow(title = "Price: Low to High", icon = Icons.Default.ArrowUpward, isSelected = currentSort == SortOption.PRICE_LOW_HIGH, onClick = { onSelect(SortOption.PRICE_LOW_HIGH) })
            SortOptionRow(title = "Price: High to Low", icon = Icons.Default.ArrowDownward, isSelected = currentSort == SortOption.PRICE_HIGH_LOW, onClick = { onSelect(SortOption.PRICE_HIGH_LOW) })

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SortOptionRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = if (isSelected) GreenPrimary else Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) GreenPrimary else MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        RadioButton(selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentMinPrice: String,
    currentMaxPrice: String,
    currentCondition: String?,
    onDismiss: () -> Unit,
    onApply: (String, String, String?) -> Unit,
    onClear: () -> Unit
) {
    var minPrice by remember { mutableStateOf(currentMinPrice) }
    var maxPrice by remember { mutableStateOf(currentMaxPrice) }
    var condition by remember { mutableStateOf(currentCondition) }

    val conditions = listOf("New", "Like New", "Used", "For Parts")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filters", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onClear) { Text("Clear All", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Price Range (ETB)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = minPrice, onValueChange = { minPrice = it }, label = { Text("Min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp))
                OutlinedTextField(value = maxPrice, onValueChange = { maxPrice = it }, label = { Text("Max") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f), singleLine = true, shape = RoundedCornerShape(14.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Condition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                conditions.take(2).forEach { cond ->
                    FilterChip(selected = condition == cond, onClick = { condition = if (condition == cond) null else cond }, label = { Text(cond, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                conditions.drop(2).forEach { cond ->
                    FilterChip(selected = condition == cond, onClick = { condition = if (condition == cond) null else cond }, label = { Text(cond, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f), colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenPrimary, selectedLabelColor = Color.White))
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { onApply(minPrice, maxPrice, condition) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp)) {
                Text("Apply Filters", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun GridListingCard(listing: Listing, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "card_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 2.dp else 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
            ) {
                if (listing.image_paths.isNotEmpty()) {
                    val imageUrl = supabase.storage.from("listing-images").publicUrl(listing.image_paths.first())
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Listing Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(id = android.R.drawable.ic_menu_gallery),
                        error = painterResource(id = android.R.drawable.ic_menu_report_image)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Text("No Image", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                    shadowElevation = 3.dp
                ) {
                    Text(
                        text = listing.formattedPrice(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (listing.status == "sold") {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.error) {
                            Text("SOLD", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    if (!listing.condition.isNullOrBlank()) {
                        Surface(shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.7f)) {
                            Text(listing.condition!!, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    if (listing.is_negotiable) {
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondary) {
                            Text("Nego", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.8f)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = listing.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = listing.pickup_location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}