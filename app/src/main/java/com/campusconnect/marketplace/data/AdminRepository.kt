package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class AdminUser(
    val id: String,
    val full_name: String? = "Unknown",
    val verification_status: String? = "pending_verification",
    val avatar_url: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val is_suspended: Boolean? = false,
    val suspended_until: String? = null // <--- NEW FIELD
)

class AdminRepository {

    // ==========================================
    // VERIFICATION REQUESTS
    // ==========================================
    suspend fun getPendingRequests(): List<VerificationRequest> {
        return supabase.from("student_verification_requests")
            .select { filter { eq("status", "pending") } }
            .decodeList<VerificationRequest>()
    }

    suspend fun approveRequest(requestId: String) {
        val adminId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
        supabase.from("student_verification_requests").update(
            { set("status", "approved"); set("reviewed_by", adminId) }
        ) { filter { eq("id", requestId) } }
    }

    suspend fun rejectRequest(requestId: String) {
        val adminId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("Not logged in")
        supabase.from("student_verification_requests").update(
            { set("status", "rejected"); set("reviewed_by", adminId) }
        ) { filter { eq("id", requestId) } }
    }

    // ==========================================
    // REPORTS & MODERATION
    // ==========================================
    suspend fun getOpenReports(): List<Report> {
        return supabase.from("reports")
            .select { filter { eq("status", "open") } }
            .decodeList<Report>()
    }

    suspend fun resolveReport(reportId: String) {
        supabase.from("reports").update({ set("status", "resolved") }) {
            filter { eq("id", reportId) }
        }
    }

    suspend fun removeListing(listingId: String) {
        supabase.from("listings").update({ set("status", "removed") }) {
            filter { eq("id", listingId) }
        }
    }

    // ==========================================
    // USER MANAGEMENT
    // ==========================================
    suspend fun getAllUsers(): List<AdminUser> {
        return supabase.from("profiles")
            .select()
            .decodeList<AdminUser>()
    }

    suspend fun updateUserVerificationStatus(userId: String, status: String) {
        supabase.postgrest.rpc(
            function = "admin_set_verification_status",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_status", status)
            }
        )
    }

    suspend fun deleteUser(userId: String) {
        supabase.postgrest.rpc(
            function = "admin_delete_user",
            parameters = buildJsonObject {
                put("p_user_id", userId)
            }
        )
    }

    // UPDATED: Now accepts number of days
    suspend fun toggleUserSuspension(userId: String, isSuspended: Boolean, days: Int? = null) {
        supabase.postgrest.rpc(
            function = "admin_toggle_user_suspension",
            parameters = buildJsonObject {
                put("p_user_id", userId)
                put("p_is_suspended", isSuspended)
                if (days != null) {
                    put("p_days", days)
                }
            }
        )
    }

    // ==========================================
    // ANALYTICS
    // ==========================================
    suspend fun getAllListings(): List<Listing> {
        return supabase.from("listings").select().decodeList<Listing>()
    }

    // ==========================================
    // CATEGORY MANAGEMENT
    // ==========================================
    suspend fun getAllCategories(): List<Category> {
        return supabase.from("categories").select().decodeList<Category>()
    }

    suspend fun addCategory(nameEn: String, nameAm: String, slug: String) {
        supabase.from("categories").insert(
            mapOf("name_en" to nameEn, "name_am" to nameAm, "slug" to slug, "is_active" to true)
        )
    }

    suspend fun toggleCategoryStatus(categoryId: String, currentStatus: Boolean) {
        supabase.from("categories").update({ set("is_active", !currentStatus) }) {
            filter { eq("id", categoryId) }
        }
    }
}