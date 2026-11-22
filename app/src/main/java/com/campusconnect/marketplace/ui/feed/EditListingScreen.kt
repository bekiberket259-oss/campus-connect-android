package com.campusconnect.marketplace.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.campusconnect.marketplace.data.Category
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.ListingRepository
import com.campusconnect.marketplace.util.ErrorUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    navController: NavController,
    listing: Listing
) {
    val scope = rememberCoroutineScope()
    val repository = ListingRepository()

    var title by remember { mutableStateOf(listing.title) }
    var description by remember { mutableStateOf(listing.description) }
    var price by remember { mutableStateOf((listing.price_cents / 100).toString()) }
    var location by remember { mutableStateOf(listing.pickup_location) }
    var isNegotiable by remember { mutableStateOf(listing.is_negotiable) }

    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var expanded by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            categories = repository.getCategories()
            selectedCategory = categories.find { it.id == listing.seller_id } // Fallback
        } catch (e: Exception) {
            error = ErrorUtils.getFriendlyMessage(e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Listing") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price (ETB)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Price Negotiable?", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = isNegotiable, onCheckedChange = { isNegotiable = it })
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory?.name_en ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Pickup Location") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = {
                    if (selectedCategory == null) return@Button
                    isSaving = true
                    scope.launch {
                        try {
                            repository.editListing(
                                listingId = listing.id,
                                title = title,
                                description = description,
                                priceCents = (price.toIntOrNull() ?: 0) * 100,
                                location = location,
                                categoryId = selectedCategory!!.id,
                                isNegotiable = isNegotiable
                            )
                            navController.popBackStack()
                        } catch (e: Exception) {
                            error = ErrorUtils.getFriendlyMessage(e)
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving && title.isNotBlank() && price.isNotBlank() && selectedCategory != null
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}