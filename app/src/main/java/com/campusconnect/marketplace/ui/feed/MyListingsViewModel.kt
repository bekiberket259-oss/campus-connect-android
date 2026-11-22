package com.campusconnect.marketplace.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.ListingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MyListingsViewModel : ViewModel() {
    private val repository = ListingRepository()

    private val _myListings = MutableStateFlow<List<Listing>>(emptyList())
    val myListings: StateFlow<List<Listing>> = _myListings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchMyListings()
    }

    fun fetchMyListings() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _myListings.value = repository.getMyListings()
            } catch (e: Exception) {
                _myListings.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}