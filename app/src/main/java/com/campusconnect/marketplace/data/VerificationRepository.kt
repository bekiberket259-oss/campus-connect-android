package com.campusconnect.marketplace.data

import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from

class VerificationRepository {
    suspend fun submitVerification(studentId: String, fullName: String, department: String) {
        val currentUser = supabase.auth.currentUserOrNull() ?: throw Exception("Not logged in")

        val request = VerificationRequest(
            user_id = currentUser.id,
            student_id_number = studentId,
            full_name = fullName,
            department = department
        )

        // Insert into the verification table
        supabase.from("student_verification_requests").insert(request)
    }
}