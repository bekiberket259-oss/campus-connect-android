package com.campusconnect.marketplace.ui.main

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import com.campusconnect.marketplace.util.ValidationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    currentName: String,
    profileViewModel: ProfileViewModel = viewModel()
) {
    var name by remember { mutableStateOf(currentName) }
    var phone by remember { mutableStateOf("") }
    var telegram by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val profile by profileViewModel.profile.collectAsState()
    val contactInfo by profileViewModel.contactInfo.collectAsState()
    val isSaving by profileViewModel.isSaving.collectAsState()
    var showSuccess by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Real-time phone validation (only validate if the user typed something)
    val isPhoneValid = phone.isBlank() || ValidationUtils.isValidEthiopianPhone(phone)

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            profileViewModel.uploadProfilePicture(uri, context) { }
        }
    }

    LaunchedEffect(contactInfo) {
        phone = contactInfo?.phone ?: ""
        telegram = contactInfo?.telegram_username ?: ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 16.dp, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        profileViewModel.saveProfile(name, phone, telegram) {
                            showSuccess = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp).imePadding(),
                    shape = RoundedCornerShape(16.dp),
                    // DISABLE BUTTON IF PHONE IS INVALID
                    enabled = !isSaving && name.isNotBlank() && isPhoneValid
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Save Changes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- AVATAR UPLOAD SECTION ---
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = selectedImageUri?.toString() ?: profile?.avatar_url

                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile Picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(100.dp).clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier.size(100.dp).clip(CircleShape).background(GreenPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.firstOrNull()?.uppercase() ?: "S",
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Edit Icon Overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Avatar", tint = GreenPrimary, modifier = Modifier.size(18.dp))
                }
            }

            Text("Tap to change profile picture", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(top = 8.dp, bottom = 32.dp))

            if (showSuccess) {
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color(0xFF4CAF50))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Profile updated successfully!", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // --- PHONE NUMBER WITH VALIDATION ---
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                placeholder = { Text("e.g., +251911234567 or 0911234567") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                isError = !isPhoneValid,
                supportingText = {
                    if (!isPhoneValid) {
                        Text("Must start with +251 or 09", color = MaterialTheme.colorScheme.error)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = telegram,
                onValueChange = { telegram = it },
                label = { Text("Telegram Username (without @)") },
                leadingIcon = { Icon(Icons.Default.Send, contentDescription = "Telegram") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surface, focusedContainerColor = MaterialTheme.colorScheme.surface)
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}