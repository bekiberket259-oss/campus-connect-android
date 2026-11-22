package com.campusconnect.marketplace.ui.main

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.campusconnect.marketplace.data.Profile
import com.campusconnect.marketplace.data.ProfileContact
import com.campusconnect.marketplace.data.ProfileRepository
import com.campusconnect.marketplace.util.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile

    private val _contactInfo = MutableStateFlow<ProfileContact?>(null)
    val contactInfo: StateFlow<ProfileContact?> = _contactInfo

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    init {
        loadProfile()
        loadContactInfo()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                _profile.value = repository.getMyProfile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadContactInfo() {
        viewModelScope.launch {
            try {
                _contactInfo.value = repository.getMyContactInfo()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveProfile(name: String, phone: String, telegram: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                repository.updateFullName(name)
                repository.updateContactInfo(phone, telegram)
                loadProfile()
                loadContactInfo()
                onSuccess()
            } catch (e: Exception) {
                android.util.Log.e("ProfileError", "Failed to save profile: ${e.message}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun uploadProfilePicture(imageUri: Uri, context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val imageBytes = withContext(Dispatchers.IO) {
                    ImageUtils.compressImage(context, imageUri)
                }

                if (imageBytes != null) {
                    repository.uploadAvatar(imageBytes)
                    loadProfile() // Refresh UI so the new image appears instantly
                    onSuccess()
                } else {
                    android.util.Log.e("AvatarError", "Compressed image bytes were null")
                }
            } catch (e: Exception) {
                android.util.Log.e("AvatarError", "Failed to upload avatar: ${e.message}")
                e.printStackTrace()
            } finally {
                _isSaving.value = false
            }
        }
    }
}