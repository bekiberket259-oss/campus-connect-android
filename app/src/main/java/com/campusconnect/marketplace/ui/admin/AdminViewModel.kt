package com.campusconnect.marketplace.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.AdminRepository
import com.campusconnect.marketplace.data.AdminUser
import com.campusconnect.marketplace.data.Category
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.data.Report
import com.campusconnect.marketplace.data.VerificationRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repository = AdminRepository()

    private val _requests = MutableStateFlow<List<VerificationRequest>>(emptyList())
    val requests: StateFlow<List<VerificationRequest>> = _requests

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports

    private val _users = MutableStateFlow<List<AdminUser>>(emptyList())
    val users: StateFlow<List<AdminUser>> = _users

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        fetchAllData()
    }

    fun fetchAllData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _requests.value = repository.getPendingRequests()
            } catch (e: Exception) { android.util.Log.e("AdminError", "Requests: ${e.message}") }
            try {
                _reports.value = repository.getOpenReports()
            } catch (e: Exception) { android.util.Log.e("AdminError", "Reports: ${e.message}") }
            try {
                _users.value = repository.getAllUsers()
            } catch (e: Exception) { android.util.Log.e("AdminError", "Users: ${e.message}") }
            try {
                _categories.value = repository.getAllCategories()
            } catch (e: Exception) { android.util.Log.e("AdminError", "Categories: ${e.message}") }
            try {
                _listings.value = repository.getAllListings()
            } catch (e: Exception) { android.util.Log.e("AdminError", "Listings: ${e.message}") }
            _isLoading.value = false
        }
    }

    fun approve(requestId: String) {
        viewModelScope.launch { repository.approveRequest(requestId); fetchAllData() }
    }

    fun reject(requestId: String) {
        viewModelScope.launch { repository.rejectRequest(requestId); fetchAllData() }
    }

    fun resolveReportAndRemoveListing(reportId: String, listingId: String) {
        viewModelScope.launch {
            repository.removeListing(listingId)
            repository.resolveReport(reportId)
            fetchAllData()
        }
    }

    fun dismissReport(reportId: String) {
        viewModelScope.launch { repository.resolveReport(reportId); fetchAllData() }
    }

    fun updateVerificationStatus(userId: String, newStatus: String) {
        viewModelScope.launch {
            try { repository.updateUserVerificationStatus(userId, newStatus); fetchAllData() }
            catch (e: Exception) { android.util.Log.e("AdminError", "Update: ${e.message}") }
        }
    }

    fun deleteUser(userId: String) {
        viewModelScope.launch {
            try { repository.deleteUser(userId); fetchAllData() }
            catch (e: Exception) { android.util.Log.e("AdminError", "Delete user failed: ${e.message}") }
        }
    }

    // UPDATED: Now accepts number of days
    fun toggleSuspension(userId: String, isSuspended: Boolean, days: Int? = null) {
        viewModelScope.launch {
            try {
                repository.toggleUserSuspension(userId, isSuspended, days)
                fetchAllData()
            } catch (e: Exception) {
                android.util.Log.e("AdminError", "Suspend failed: ${e.message}")
            }
        }
    }

    fun addCategory(nameEn: String, nameAm: String, slug: String) {
        viewModelScope.launch { repository.addCategory(nameEn, nameAm, slug); fetchAllData() }
    }

    fun toggleCategory(categoryId: String, currentStatus: Boolean) {
        viewModelScope.launch { repository.toggleCategoryStatus(categoryId, currentStatus); fetchAllData() }
    }
}