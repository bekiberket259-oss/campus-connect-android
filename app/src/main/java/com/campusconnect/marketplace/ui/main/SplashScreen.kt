package com.campusconnect.marketplace.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.campusconnect.marketplace.data.AuthRepository
import com.campusconnect.marketplace.supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    authRepository: AuthRepository,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    LaunchedEffect(Unit) {
        // 1. Wait for 1.5 seconds just for the branding animation
        delay(1500)

        try {
            // 2. CRITICAL: Wait for Supabase to load the saved session from the phone's storage
            supabase.auth.awaitInitialization()
        } catch (e: Exception) {
            // If initialization fails (e.g., severe storage error), proceed to login
            onNavigateToLogin()
            return@LaunchedEffect
        }

        // 3. Check if a session exists in memory now
        if (supabase.auth.currentUserOrNull() != null) {
            // User is already logged in!
            onNavigateToMain()
        } else {
            // No session found, go to login
            onNavigateToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Logo",
                tint = Color.White,
                modifier = Modifier.size(100.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Campus Connect",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Buy & Sell on Campus",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }
    }
}