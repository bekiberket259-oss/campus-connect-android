package com.campusconnect.marketplace.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.campusconnect.marketplace.R
import com.campusconnect.marketplace.data.AuthRepository
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.ui.feed.FeedScreen
import com.campusconnect.marketplace.ui.feed.MyListingsScreen
import com.campusconnect.marketplace.ui.feed.SavedItemsScreen
import com.campusconnect.marketplace.ui.theme.GreenDark
import com.campusconnect.marketplace.ui.theme.GreenPrimary
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onCreateListingClick: () -> Unit,
    onListingClick: (Listing) -> Unit,
    onVerifyClick: () -> Unit,
    onAdminClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewPublicProfileClick: (String) -> Unit,
    onInboxClick: () -> Unit // <--- NEW PARAMETER TO FIX THE ERROR
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        floatingActionButton = {
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = onCreateListingClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Listing")
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(stringResource(id = R.string.feed), fontWeight = FontWeight.Medium) },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "My Listings") },
                    label = { Text(stringResource(id = R.string.my_items), fontWeight = FontWeight.Medium) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Saved") },
                    label = { Text("Saved", fontWeight = FontWeight.Medium) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text(stringResource(id = R.string.profile), fontWeight = FontWeight.Medium) },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                0 -> FeedScreen(
                    onListingClick = onListingClick,
                    onInboxClick = onInboxClick // <--- PASSED TO FEED SCREEN
                )
                1 -> MyListingsScreen(onListingClick = onListingClick)
                2 -> SavedItemsScreen(onListingClick = onListingClick)
                3 -> ProfileTab(
                    authRepository,
                    onLogout,
                    onVerifyClick,
                    onAdminClick,
                    onEditProfileClick,
                    onSettingsClick,
                    onViewPublicProfileClick
                )
            }
        }
    }
}

@Composable
fun ProfileTab(
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onVerifyClick: () -> Unit,
    onAdminClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onViewPublicProfileClick: (String) -> Unit,
    profileViewModel: ProfileViewModel = viewModel()
) {
    val user = authRepository.getCurrentUser()
    val profile by profileViewModel.profile.collectAsState()
    val scope = rememberCoroutineScope()
    var isAdmin by remember { mutableStateOf(false) }
    var isCheckingRole by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isAdmin = authRepository.isAdmin()
        profileViewModel.loadProfile()
        isCheckingRole = false
    }

    if (isCheckingRole) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val statusTextRes = when (profile?.verification_status) {
        "verified" -> R.string.verified_student
        "pending_verification" -> R.string.pending_verification
        "rejected" -> R.string.rejected_verification
        else -> R.string.unknown_status
    }
    val statusColor = when (profile?.verification_status) {
        "verified" -> Color(0xFF4CAF50)
        "pending_verification" -> Color(0xFFFF9800)
        "rejected" -> Color(0xFFF44336)
        else -> Color.Gray
    }

    val initial = profile?.full_name?.firstOrNull()?.uppercase() ?: "S"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // COVER HEADER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isAdmin) 170.dp else 210.dp)
                .background(Brush.verticalGradient(listOf(GreenPrimary, GreenDark)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (isAdmin) 0.dp else 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    if (profile?.avatar_url != null) {
                        AsyncImage(
                            model = profile?.avatar_url,
                            contentDescription = "Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(initial, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = GreenPrimary)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(profile?.full_name ?: "Loading...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                Text(user?.email ?: "Unknown", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
            }
        }

        // FLOATING STATUS CARD (Only shown if NOT admin)
        if (!isAdmin) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .offset(y = (-30).dp),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (profile?.verification_status == "verified") Icons.Default.Verified else Icons.Default.Info,
                        contentDescription = "Status",
                        tint = statusColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Account Status", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Text(stringResource(id = statusTextRes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = statusColor)
                    }
                }
            }
        }

        // MENU ITEMS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset(y = if (isAdmin) 16.dp else (-16).dp)
        ) {
            ProfileMenuItem(
                icon = Icons.Default.Person,
                title = "View My Public Profile",
                onClick = {
                    profile?.id?.let { userId -> onViewPublicProfileClick(userId) }
                }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            ProfileMenuItem(icon = Icons.Default.Edit, title = stringResource(id = R.string.edit_profile), onClick = onEditProfileClick)

            if (!isAdmin && profile?.verification_status != "verified") {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ProfileMenuItem(icon = Icons.Default.Security, title = stringResource(id = R.string.submit_verification), onClick = onVerifyClick)
            }

            if (isAdmin) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                ProfileMenuItem(icon = Icons.Default.AdminPanelSettings, title = stringResource(id = R.string.admin_dashboard), onClick = onAdminClick, iconTint = MaterialTheme.colorScheme.tertiary)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = onSettingsClick
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

            ProfileMenuItem(
                icon = Icons.AutoMirrored.Filled.ExitToApp,
                title = stringResource(id = R.string.logout),
                onClick = { scope.launch { authRepository.logout(); onLogout() } },
                iconTint = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, color = textColor, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Go", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}