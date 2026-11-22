package com.campusconnect.marketplace.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.ListingRepository
import com.campusconnect.marketplace.data.ReviewRepository // <--- IMPORTANT
import com.campusconnect.marketplace.data.SellerContact
import com.campusconnect.marketplace.data.SellerInfo
import com.campusconnect.marketplace.util.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListingDetailViewModel : ViewModel() {
    private val repository = ListingRepository()
    private val reviewRepo = ReviewRepository() // <--- IMPORTANT

    private val _isActionLoading = MutableStateFlow(false)
    val isActionLoading: StateFlow<Boolean> = _isActionLoading

    private val _sellerContact = MutableStateFlow<SellerContact?>(null)
    val sellerContact: StateFlow<SellerContact?> = _sellerContact

    private val _sellerInfo = MutableStateFlow<SellerInfo?>(null)
    val sellerInfo: StateFlow<SellerInfo?> = _sellerInfo

    private val _contactError = MutableStateFlow<String?>(null)
    val contactError: StateFlow<String?> = _contactError

    private val _isFavorited = MutableStateFlow(false)
    val isFavorited: StateFlow<Boolean> = _isFavorited

    // THIS WAS MISSING
    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview

    fun loadSellerInfo(listingId: String, sellerId: String) {
        viewModelScope.launch {
            _contactError.value = null
            _sellerInfo.value = repository.getSellerInfo(sellerId)
            repository.incrementViewCount(listingId)
            try {
                _isFavorited.value = repository.isFavorited(listingId)
            } catch (e: Exception) { }
            try {
                _sellerContact.value = repository.getSellerContact(listingId)
                if (_sellerContact.value == null) {
                    _contactError.value = "Database returned an empty list."
                }
            } catch (e: Exception) {
                _contactError.value = ErrorUtils.getFriendlyMessage(e)
            }
        }
    }

    fun toggleFavorite(listingId: String) {
        viewModelScope.launch {
            val previousState = _isFavorited.value
            _isFavorited.value = !previousState
            try {
                val isNowFavorited = repository.toggleFavorite(listingId)
                _isFavorited.value = isNowFavorited
            } catch (e: Exception) {
                _isFavorited.value = previousState
            }
        }
    }

    // THIS WAS MISSING
    fun submitReview(sellerId: String, listingId: String, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmittingReview.value = true
            try {
                reviewRepo.submitReview(sellerId, listingId, rating, comment)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("ReviewError", "Failed to submit: ${e.message}")
            } finally {
                _isSubmittingReview.value = false
            }
        }
    }

    fun markAsSold(listingId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isActionLoading.value = true
            try {
                repository.markAsSold(listingId)
                onSuccess()
            } catch (e: Exception) { } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun deleteListing(listingId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isActionLoading.value = true
            try {
                repository.deleteListing(listingId)
                onSuccess()
            } catch (e: Exception) { } finally {
                _isActionLoading.value = false
            }
        }
    }

    fun reportListing(listingId: String, reason: String, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.reportListing(listingId, reason, comment)
                onSuccess()
            } catch (e: Exception) { }
        }
    }
}