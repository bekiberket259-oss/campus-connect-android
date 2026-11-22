package com.campusconnect.marketplace.ui.feed

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusconnect.marketplace.ui.theme.GreenPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(
    navController: NavController,
    createListingViewModel: CreateListingViewModel = viewModel()
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }

    // MULTIPLE IMAGES STATE
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    var isNegotiable by remember { mutableStateOf(true) }
    var condition by remember { mutableStateOf("Used") }
    var conditionExpanded by remember { mutableStateOf(false) }
    val conditions = listOf("New", "Like New", "Used", "For Parts")

    val categories by createListingViewModel.categories.collectAsState()
    val isSaving by createListingViewModel.isSaving.collectAsState()
    val error by createListingViewModel.error.collectAsState()

    val context = LocalContext.current

    // MULTIPLE IMAGE PICKER
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        // Limit to 5 images maximum
        selectedImageUris = (selectedImageUris + uris).take(5)
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<com.campusconnect.marketplace.data.Category?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Listing", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 16.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        if (selectedCategory == null) return@Button
                        createListingViewModel.submitListing(
                            title = title,
                            description = description,
                            price = price,
                            location = location,
                            categoryId = selectedCategory!!.id,
                            isNegotiable = isNegotiable,
                            condition = condition,
                            imageUris = selectedImageUris, // <--- Pass the list
                            context = context,
                            onSuccess = { navController.popBackStack() }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp)
                        .imePadding(),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSaving && title.isNotBlank() && price.isNotBlank() && selectedCategory != null
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Post Listing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- MULTIPLE IMAGE PICKER UI ---
            item {
                Text("Photos (${selectedImageUris.size}/5)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(selectedImageUris) { uri ->
                        Box(
                            modifier = Modifier.size(100.dp).clip(RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Remove Image Button
                            IconButton(
                                onClick = { selectedImageUris = selectedImageUris - uri },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Add Button (Only show if less than 5 images)
                    if (selectedImageUris.size < 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo", tint = GreenPrimary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Add", style = MaterialTheme.typography.labelMedium, color = GreenPrimary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What are you selling?") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = "Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price (ETB)") },
                    leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = "Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Handshake, contentDescription = "Negotiable", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Price Negotiable?", style = MaterialTheme.typography.bodyLarge)
                        }
                        Switch(
                            checked = isNegotiable,
                            onCheckedChange = { isNegotiable = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.onPrimary, checkedTrackColor = GreenPrimary)
                        )
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedCategory?.name_en ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = "Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name_en) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                ExposedDropdownMenuBox(
                    expanded = conditionExpanded,
                    onExpandedChange = { conditionExpanded = !conditionExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = condition,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Condition") },
                        leadingIcon = { Icon(Icons.Default.Build, contentDescription = "Condition") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = conditionExpanded,
                        onDismissRequest = { conditionExpanded = false }
                    ) {
                        conditions.forEach { cond ->
                            DropdownMenuItem(
                                text = { Text(cond) },
                                onClick = {
                                    condition = cond
                                    conditionExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Pickup Location (e.g., Library)") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = "Location") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (Condition, details, etc.)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (error != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}