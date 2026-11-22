package com.campusconnect.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import com.campusconnect.marketplace.navigation.AppNavigation
import com.campusconnect.marketplace.ui.theme.CampusConnectTheme
import com.campusconnect.marketplace.util.ThemeMode
import com.campusconnect.marketplace.util.ThemePreferences

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Read the saved theme
        val initialTheme = ThemePreferences.getTheme(this)

        setContent {
            // Hold the theme state so the app recomposes when it changes
            var currentTheme by remember { mutableStateOf(initialTheme) }

            CampusConnectTheme(themeMode = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        currentTheme = currentTheme,
                        onThemeChange = { newTheme ->
                            currentTheme = newTheme
                            ThemePreferences.saveTheme(this, newTheme)
                        }
                    )
                }
            }
        }
    }
}