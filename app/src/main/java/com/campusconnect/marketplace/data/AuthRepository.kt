package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth // <--- FIXED
import io.github.jan.supabase.auth.providers.builtin.Email // <--- FIXED
import io.github.jan.supabase.auth.user.UserInfo // <--- FIXED
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    suspend fun login(email: String, password: String) {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun register(email: String, password: String, fullName: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = buildJsonObject {
                put("full_name", fullName)
            }
        }
    }

    suspend fun resetPassword(email: String) {
        supabase.auth.resetPasswordForEmail(email)
    }

    suspend fun logout() {
        supabase.auth.signOut()
    }

    fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    fun getCurrentUser(): UserInfo? {
        return supabase.auth.currentUserOrNull()
    }

    suspend fun isAdmin(): Boolean {
        val user = supabase.auth.currentUserOrNull() ?: return false
        return try {
            val roles = supabase.from("user_roles").select {
                filter {
                    eq("user_id", user.id)
                    eq("role", "admin")
                }
            }.decodeList<Map<String, String>>()
            roles.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}