package com.campusconnect.marketplace.ui.feed

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.Category
import com.campusconnect.marketplace.data.ListingRepository
import com.campusconnect.marketplace.util.ErrorUtils
import com.campusconnect.marketplace.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateListingViewModel : ViewModel() {
    private val repository = ListingRepository()

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                _categories.value = repository.getCategories()
            } catch (e: Exception) {
                _error.value = ErrorUtils.getFriendlyMessage(e)
            }
        }
    }

    fun submitListing(
        title: String,
        description: String,
        price: String,
        location: String,
        categoryId: String,
        isNegotiable: Boolean,
        condition: String,
        imageUris: List<Uri>, // <--- List of URIs
        context: Context,
        onSuccess: () -> Unit
    ) {
        val priceInt = price.toIntOrNull() ?: 0
        val priceCents = priceInt * 100

        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            try {
                // Compress all images in parallel/loop
                val imageBytesList = imageUris.mapNotNull { uri ->
                    withContext(Dispatchers.IO) {
                        ImageUtils.compressImage(context, uri)
                    }
                }

                repository.createListing(
                    title = title,
                    description = description,
                    priceCents = priceCents,
                    location = location,
                    categoryId = categoryId,
                    isNegotiable = isNegotiable,
                    condition = condition,
                    imageBytesList = imageBytesList // <--- Pass the list
                )

                onSuccess()
            } catch (e: Exception) {
                _error.value = ErrorUtils.getFriendlyMessage(e)
            } finally {
                _isSaving.value = false
            }
        }
    }
}