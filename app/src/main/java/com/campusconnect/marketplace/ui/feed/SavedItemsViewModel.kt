package com.campusconnect.marketplace.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SavedItemsViewModel : ViewModel() {
    private val repository = ListingRepository()

    private val _savedListings = MutableStateFlow<List<Listing>>(emptyList())
    val savedListings: StateFlow<List<Listing>> = _savedListings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchSavedListings()
    }

    fun fetchSavedListings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _savedListings.value = repository.getSavedListings()
            } catch (e: Exception) {
                _savedListings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}