package com.campusconnect.marketplace.util

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ErrorUtils {
    fun getFriendlyMessage(exception: Throwable?): String {
        val rawMessage = exception?.message ?: "Unknown error"

        return when {
            // Network Errors
            exception is UnknownHostException || exception is ConnectException ||
                    rawMessage.contains("Failed to connect", ignoreCase = true) ||
                    rawMessage.contains("Unable to resolve host", ignoreCase = true) -> {
                "No internet connection. Please check your network and try again."
            }

            exception is SocketTimeoutException || rawMessage.contains("timeout", ignoreCase = true) -> {
                "The request took too long. Please try again."
            }

            // Authentication Errors
            rawMessage.contains("Invalid login credentials", ignoreCase = true) -> {
                "Incorrect email or password. Please try again."
            }
            rawMessage.contains("User already registered", ignoreCase = true) -> {
                "An account with this email already exists."
            }
            rawMessage.contains("Password should be at least 6 characters", ignoreCase = true) -> {
                "Your password is too short (minimum 6 characters)."
            }

            // Security / RLS Errors
            rawMessage.contains("row-level security", ignoreCase = true) -> {
                "Permission denied. You do not have access to perform this action."
            }
            rawMessage.contains("Only verified students", ignoreCase = true) -> {
                "Only verified students can view contact information or post listings."
            }
            rawMessage.contains("duplicate key value", ignoreCase = true) -> {
                "You have already submitted this action."
            }

            // Fallback
            else -> {
                "Something went wrong. Please try again later."
            }
        }
    }
}