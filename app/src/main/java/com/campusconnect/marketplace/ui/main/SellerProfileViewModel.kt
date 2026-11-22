package com.campusconnect.marketplace.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.ListingRepository
import com.campusconnect.marketplace.data.ReviewRepository
import com.campusconnect.marketplace.data.SellerInfo
import com.campusconnect.marketplace.data.SellerRating
import com.campusconnect.marketplace.data.SellerReview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SellerProfileViewModel : ViewModel() {
    private val listingRepo = ListingRepository()
    private val reviewRepo = ReviewRepository()

    private val _sellerInfo = MutableStateFlow<SellerInfo?>(null)
    val sellerInfo: StateFlow<SellerInfo?> = _sellerInfo

    private val _sellerRating = MutableStateFlow<SellerRating?>(null)
    val sellerRating: StateFlow<SellerRating?> = _sellerRating

    private val _sellerListings = MutableStateFlow<List<Listing>>(emptyList())
    val sellerListings: StateFlow<List<Listing>> = _sellerListings

    private val _sellerReviews = MutableStateFlow<List<SellerReview>>(emptyList())
    val sellerReviews: StateFlow<List<SellerReview>> = _sellerReviews

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview: StateFlow<Boolean> = _isSubmittingReview

    private var currentSellerId: String = ""

    fun loadSellerData(sellerId: String) {
        currentSellerId = sellerId
        viewModelScope.launch {
            _isLoading.value = true

            try { _sellerInfo.value = listingRepo.getSellerInfo(sellerId) } catch (e: Exception) { }
            try { _sellerListings.value = listingRepo.getListingsBySeller(sellerId) } catch (e: Exception) { }

            // Fetch reviews FIRST
            try {
                _sellerReviews.value = reviewRepo.getSellerReviews(sellerId)
            } catch (e: Exception) {
                _sellerReviews.value = emptyList()
            }

            // Compute rating from the reviews we just fetched (reliable fallback)
            val reviews = _sellerReviews.value
            if (reviews.isNotEmpty()) {
                val avg = reviews.map { it.rating }.average()
                _sellerRating.value = SellerRating(
                    average_rating = avg,
                    total_reviews = reviews.size.toLong()
                )
            } else {
                // Try the RPC as a last resort (works if you ever create it in Supabase)
                try {
                    _sellerRating.value = reviewRepo.getSellerRating(sellerId)
                } catch (e: Exception) {
                    _sellerRating.value = SellerRating(0.0, 0)
                }
            }

            _isLoading.value = false
        }
    }

    fun submitReview(listingId: String, rating: Int, comment: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSubmittingReview.value = true
            try {
                reviewRepo.submitReview(currentSellerId, listingId, rating, comment)
                loadSellerData(currentSellerId)
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("ReviewError", "Failed to submit: ${e.message}", e)
            } finally {
                _isSubmittingReview.value = false
            }
        }
    }
}