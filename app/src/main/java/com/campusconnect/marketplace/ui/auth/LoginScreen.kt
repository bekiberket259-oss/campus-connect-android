package com.campusconnect.marketplace.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.campusconnect.marketplace.data.AuthRepository
import com.campusconnect.marketplace.navigation.Routes
import com.campusconnect.marketplace.supabase
import com.campusconnect.marketplace.ui.theme.GreenDark
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import com.campusconnect.marketplace.util.ErrorUtils
import com.campusconnect.marketplace.util.ValidationUtils

// --- CRITICAL IMPORTS FOR GOOGLE SIGN-IN ---
import io.github.jan.supabase.compose.auth.composeAuth
import io.github.jan.supabase.compose.auth.composable.NativeSignInResult
import io.github.jan.supabase.compose.auth.composable.rememberSignInWithGoogle
// ------------------------------------------

import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authRepository: AuthRepository,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val gradient = Brush.verticalGradient(colors = listOf(GreenPrimary, GreenDark))

    // --- GOOGLE SIGN-IN ACTION ---
    // The function is called on the `composeAuth` property.
    val googleSignInAction = supabase.composeAuth.rememberSignInWithGoogle(
        onResult = { result -> // The type is inferred from the function signature
            when (result) {
                is NativeSignInResult.Success -> onLoginSuccess()
                is NativeSignInResult.ClosedByUser -> errorMessage = "Google sign-in was cancelled."
                is NativeSignInResult.Error -> errorMessage = "Google sign-in failed: ${result.message}"
                is NativeSignInResult.NetworkError -> errorMessage = "Network error. Check your internet connection."
            }
        }
    )

    Box(
        modifier = Modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Login to your Campus Connect account",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 32.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Toggle password visibility")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                TextButton(
                    onClick = { navController.navigate(Routes.FORGOT_PASSWORD) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Forgot Password?", color = MaterialTheme.colorScheme.secondary)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                // Login Button
                Button(
                    onClick = {
                        if (!ValidationUtils.isValidEmail(email)) {
                            errorMessage = "Please enter a valid email address."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null
                        scope.launch {
                            try {
                                authRepository.login(email.trim(), password)
                                onLoginSuccess()
                            } catch (e: Exception) {
                                errorMessage = ErrorUtils.getFriendlyMessage(e)
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isLoading && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Login", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OR Divider
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                    Text("OR", modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Google Sign-In Button
                OutlinedButton(
                    onClick = { googleSignInAction.startFlow() },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Sign in with Google", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Create Account Link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Don't have an account? ", style = MaterialTheme.typography.bodyMedium)
                    TextButton(
                        onClick = { navController.navigate(Routes.REGISTER) },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Create account", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}