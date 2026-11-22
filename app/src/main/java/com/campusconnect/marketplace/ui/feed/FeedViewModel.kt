package com.campusconnect.marketplace.ui.feed

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.Category
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.ListingRepository
import com.campusconnect.marketplace.data.SortOption
import com.campusconnect.marketplace.data.local.AppDatabase
import com.campusconnect.marketplace.util.ErrorUtils
import com.campusconnect.marketplace.util.NetworkUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).listingDao()
    private val repository = ListingRepository(dao)

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId

    private val _minPrice = MutableStateFlow<Int?>(null)
    val minPrice: StateFlow<Int?> = _minPrice
    private val _maxPrice = MutableStateFlow<Int?>(null)
    val maxPrice: StateFlow<Int?> = _maxPrice
    private val _selectedCondition = MutableStateFlow<String?>(null)
    val selectedCondition: StateFlow<String?> = _selectedCondition

    private val _sortOption = MutableStateFlow(SortOption.NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    private var currentPage = 0
    private val pageSize = 10
    private var hasMore = true

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private var searchJob: Job? = null

    init {
        loadCategories()
        fetchListings(reset = true)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            while (true) {
                try {
                    _categories.value = repository.getCategories()
                    break // Success, break loop
                } catch (e: Exception) {
                    if (!NetworkUtils.isOnline(getApplication())) break
                    delay(1500)
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        triggerSearch()
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = if (_selectedCategoryId.value == categoryId) null else categoryId
        triggerSearch()
    }

    fun applyFilters(minPriceEtb: String, maxPriceEtb: String, condition: String?) {
        _minPrice.value = minPriceEtb.toIntOrNull()?.times(100)
        _maxPrice.value = maxPriceEtb.toIntOrNull()?.times(100)
        _selectedCondition.value = condition
        currentPage = 0
        hasMore = true
        fetchListings(reset = true)
    }

    fun clearFilters() {
        _minPrice.value = null
        _maxPrice.value = null
        _selectedCondition.value = null
        currentPage = 0
        hasMore = true
        fetchListings(reset = true)
    }

    fun setSortOption(option: SortOption) {
        if (_sortOption.value == option) return
        _sortOption.value = option
        currentPage = 0
        hasMore = true
        fetchListings(reset = true)
    }

    private fun triggerSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500)
            currentPage = 0
            hasMore = true
            fetchListings(reset = true)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _errorMessage.value = null
            val hasExistingData = _listings.value.isNotEmpty()
            if (!hasExistingData) _isLoading.value = true else _isRefreshing.value = true

            currentPage = 0
            hasMore = true

            while (true) {
                try {
                    val newItems = repository.getActiveListings(
                        _searchQuery.value, _selectedCategoryId.value,
                        _minPrice.value, _maxPrice.value, _selectedCondition.value,
                        _sortOption.value, pageSize, 0
                    )
                    _listings.value = newItems
                    _isOffline.value = !NetworkUtils.isOnline(getApplication())
                    break // Success!
                } catch (e: Exception) {
                    if (!NetworkUtils.isOnline(getApplication())) {
                        _isOffline.value = true
                        _errorMessage.value = ErrorUtils.getFriendlyMessage(e)
                        break
                    }
                    delay(1500) // Wait and retry
                }
            }
            _isRefreshing.value = false
            _isLoading.value = false
        }
    }

    fun loadMoreListings() {
        if (_isLoading.value || _isRefreshing.value || _isLoadingMore.value || !hasMore || _isOffline.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true
            currentPage++
            val offset = currentPage * pageSize

            try {
                val newItems = repository.getActiveListings(
                    _searchQuery.value, _selectedCategoryId.value,
                    _minPrice.value, _maxPrice.value, _selectedCondition.value,
                    _sortOption.value, pageSize, offset
                )
                if (newItems.isNotEmpty()) _listings.value = _listings.value + newItems
                hasMore = newItems.size == pageSize
            } catch (e: Exception) {
                _errorMessage.value = ErrorUtils.getFriendlyMessage(e)
                hasMore = false
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    // THE MAGIC FIX: Infinite Retry Loop
    private fun fetchListings(reset: Boolean = false) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            while (true) {
                try {
                    val offset = currentPage * pageSize
                    val newItems = repository.getActiveListings(
                        _searchQuery.value, _selectedCategoryId.value,
                        _minPrice.value, _maxPrice.value, _selectedCondition.value,
                        _sortOption.value, pageSize, offset
                    )

                    _isOffline.value = !NetworkUtils.isOnline(getApplication())
                    if (reset) _listings.value = newItems else _listings.value = _listings.value + newItems
                    hasMore = newItems.size == pageSize

                    break // It worked! Break the loop.
                } catch (e: Exception) {
                    // If the phone is ACTUALLY offline, stop and show error
                    if (!NetworkUtils.isOnline(getApplication())) {
                        _isOffline.value = true
                        _errorMessage.value = ErrorUtils.getFriendlyMessage(e)
                        break
                    }
                    // If the phone is ONLINE, it was just a cold-start glitch. Wait 1.5s and retry silently.
                    delay(1500)
                }
            }
            _isLoading.value = false
        }
    }
}