package com.campusconnect.marketplace.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.campusconnect.marketplace.data.AuthRepository
import com.campusconnect.marketplace.data.Listing
import com.campusconnect.marketplace.ui.admin.AdminDashboardScreen
import com.campusconnect.marketplace.ui.auth.ForgotPasswordScreen
import com.campusconnect.marketplace.ui.auth.LoginScreen
import com.campusconnect.marketplace.ui.auth.RegisterScreen
import com.campusconnect.marketplace.ui.auth.VerificationScreen
import com.campusconnect.marketplace.ui.chat.ChatScreen
import com.campusconnect.marketplace.ui.chat.InboxScreen
import com.campusconnect.marketplace.ui.feed.CreateListingScreen
import com.campusconnect.marketplace.ui.feed.EditListingScreen
import com.campusconnect.marketplace.ui.feed.ListingDetailScreen
import com.campusconnect.marketplace.ui.main.EditProfileScreen
import com.campusconnect.marketplace.ui.main.MainScreen
import com.campusconnect.marketplace.ui.main.SellerProfileScreen
import com.campusconnect.marketplace.ui.main.SettingsScreen
import com.campusconnect.marketplace.ui.main.SplashScreen
import com.campusconnect.marketplace.util.ThemeMode

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val MAIN = "main"
    const val CREATE_LISTING = "create_listing"
    const val LISTING_DETAIL = "listing_detail"
    const val EDIT_LISTING = "edit_listing"
    const val VERIFICATION = "verification"
    const val ADMIN = "admin"
    const val EDIT_PROFILE = "edit_profile"
    const val SETTINGS = "settings"
    const val SELLER_PROFILE = "seller_profile/{sellerId}"
    const val INBOX = "inbox"
    const val CHAT = "chat/{conversationId}"
}

@Composable
fun AppNavigation(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    val navController = rememberNavController()
    val authRepository = AuthRepository()

    val selectedListing = remember { mutableStateOf<Listing?>(null) }

    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            SplashScreen(
                authRepository = authRepository,
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController,
                authRepository = authRepository,
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(navController = navController, authRepository = authRepository)
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(navController = navController, authRepository = authRepository)
        }
        composable(Routes.MAIN) {
            MainScreen(
                authRepository = authRepository,
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                onCreateListingClick = { navController.navigate(Routes.CREATE_LISTING) },
                onListingClick = { listing ->
                    selectedListing.value = listing
                    navController.navigate(Routes.LISTING_DETAIL)
                },
                onVerifyClick = { navController.navigate(Routes.VERIFICATION) },
                onAdminClick = { navController.navigate(Routes.ADMIN) },
                onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                onViewPublicProfileClick = { userId ->
                    navController.navigate("seller_profile/$userId")
                },
                onInboxClick = { navController.navigate(Routes.INBOX) } // <--- NEW
            )
        }
        composable(Routes.CREATE_LISTING) {
            CreateListingScreen(navController = navController)
        }
        composable(Routes.LISTING_DETAIL) {
            selectedListing.value?.let { listing ->
                ListingDetailScreen(
                    navController = navController,
                    listing = listing,
                    onEditClick = { listingToEdit ->
                        selectedListing.value = listingToEdit
                        navController.navigate(Routes.EDIT_LISTING)
                    },
                    onSellerClick = { sellerId ->
                        navController.navigate("seller_profile/$sellerId")
                    }
                )
            }
        }
        composable(Routes.EDIT_LISTING) {
            selectedListing.value?.let { listing ->
                EditListingScreen(navController = navController, listing = listing)
            }
        }
        composable(Routes.VERIFICATION) {
            VerificationScreen(navController = navController)
        }
        composable(Routes.ADMIN) {
            AdminDashboardScreen(navController = navController)
        }
        composable(Routes.EDIT_PROFILE) {
            val currentUser = authRepository.getCurrentUser()
            val userName = currentUser?.userMetadata?.get("full_name")?.toString() ?: "Student"
            EditProfileScreen(navController = navController, currentName = userName)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                navController = navController,
                currentTheme = currentTheme,
                onThemeChange = onThemeChange
            )
        }
        composable(
            route = Routes.SELLER_PROFILE,
            arguments = listOf(navArgument("sellerId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            SellerProfileScreen(navController = navController, sellerId = sellerId)
        }
        composable(Routes.INBOX) {
            InboxScreen(navController = navController)
        }
        composable(
            route = Routes.CHAT,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            ChatScreen(navController = navController, conversationId = conversationId)
        }
    }
}