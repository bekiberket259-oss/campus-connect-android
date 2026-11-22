package com.campusconnect.marketplace.util

object ValidationUtils {

    // Standard email regex
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // Ethiopian Phone Regex (Matches +2519... or 09...)
    // Allows optional spaces or dashes for user friendliness
    private val ETHIOPIAN_PHONE_REGEX = Regex("^(\\+251|0)9[0-9]{8}$")

    fun isValidEmail(email: String): Boolean {
        return EMAIL_REGEX.matches(email.trim())
    }

    // NEW: Validate Ethiopian phone numbers
    fun isValidEthiopianPhone(phone: String): Boolean {
        // Remove spaces and dashes before checking
        val cleanPhone = phone.replace(" ", "").replace("-", "")
        return ETHIOPIAN_PHONE_REGEX.matches(cleanPhone)
    }

    fun getPasswordError(password: String): String? {
        return runCatching {
            require(password.length >= 8) { "Password must be at least 8 characters long." }
            require(password.none { it.isWhitespace() }) { "Password must not contain spaces." }
            require(password.any { it.isUpperCase() }) { "Password must contain at least one uppercase letter." }
            require(password.any { it.isLowerCase() }) { "Password must contain at least one lowercase letter." }
            require(password.any { it.isDigit() }) { "Password must contain at least one number." }
            require(password.any { !it.isLetterOrDigit() }) { "Password must contain at least one special character." }
        }.exceptionOrNull()?.message
    }
}