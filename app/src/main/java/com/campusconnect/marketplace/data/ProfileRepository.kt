package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.storage.upload
import kotlinx.serialization.Serializable

@Serializable
data class ProfileContact(
    val user_id: String,
    val phone: String? = null,
    val telegram_username: String? = null
)

class ProfileRepository {

    // 1. Fetch Profile
    suspend fun getMyProfile(): Profile? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return supabase.from("profiles")
            .select {
                filter {
                    eq("id", user.id)
                }
            }
            .decodeSingleOrNull<Profile>()
    }

    // 2. Fetch Contact Info
    suspend fun getMyContactInfo(): ProfileContact? {
        val user = supabase.auth.currentUserOrNull() ?: return null
        return supabase.from("profile_contacts")
            .select {
                filter {
                    eq("user_id", user.id)
                }
            }
            .decodeSingleOrNull<ProfileContact>()
    }

    // 3. Update Contact Info
    suspend fun updateContactInfo(phone: String?, telegram: String?) {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        supabase.from("profile_contacts").update(
            {
                set("phone", phone?.ifBlank { null })
                set("telegram_username", telegram?.ifBlank { null })
            }
        ) {
            filter {
                eq("user_id", user.id)
            }
        }
    }

    // 4. Update Full Name
    suspend fun updateFullName(newName: String) {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        supabase.from("profiles").update(
            {
                set("full_name", newName)
            }
        ) {
            filter {
                eq("id", user.id)
            }
        }
    }

    // 5. Upload Avatar Image
    suspend fun uploadAvatar(imageBytes: ByteArray) {
        val user = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")
        val fileName = "avatar_${System.currentTimeMillis()}.jpg"

        // Path MUST be: <user_id>/<filename> to match the RLS policy
        val path = "${user.id}/$fileName"

        // Upload to Storage (This will throw an error if it fails)
        supabase.storage.from("avatars").upload(path, imageBytes)

        // Get public URL
        val publicUrl = supabase.storage.from("avatars").publicUrl(path)

        // Save URL to profile table
        supabase.from("profiles").update(
            {
                set("avatar_url", publicUrl)
            }
        ) {
            filter {
                eq("id", user.id)
            }
        }
    }
}