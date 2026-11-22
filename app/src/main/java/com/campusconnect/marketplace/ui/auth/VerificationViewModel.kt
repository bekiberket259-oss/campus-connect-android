package com.campusconnect.marketplace.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.VerificationRepository
import com.campusconnect.marketplace.util.ErrorUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VerificationViewModel : ViewModel() {
    private val repository = VerificationRepository()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun submitRequest(studentId: String, fullName: String, department: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                repository.submitVerification(studentId, fullName, department)
                onSuccess()
            } catch (e: Exception) {
                // USE THE UTILITY HERE
                _error.value = ErrorUtils.getFriendlyMessage(e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}