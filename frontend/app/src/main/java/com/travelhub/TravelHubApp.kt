package com.travelhub

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.travelhub.ui.navigation.Screen
import com.travelhub.ui.auth.LoginScreen
import com.travelhub.ui.auth.RegisterScreen
import com.travelhub.ui.home.HomeScreen
import com.travelhub.ui.profile.ProfileScreen
import com.travelhub.ui.services.CatalogScreen
import com.travelhub.ui.services.ServiceDetailScreen
import com.travelhub.ui.bookings.BookingScreen
import com.travelhub.ui.bookings.NewBookingScreen
import com.travelhub.ui.itinerary.ItineraryScreen
import com.travelhub.ui.costs.CostCalculatorScreen
import com.travelhub.ui.chat.ChatScreen
import com.travelhub.ui.reviews.ReviewsScreen
import com.travelhub.ui.provider.ProviderPanelScreen
import com.travelhub.ui.provider.ManageServiceScreen
import com.travelhub.ui.provider.ProviderBookingsScreen
import com.travelhub.util.TokenManager

@Composable
fun TravelHubApp() {
    val navController = rememberNavController()
    val startDestination = if (TokenManager.isLoggedIn()) Screen.Home.route else Screen.Login.route

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onGoToRegister = { navController.navigate(Screen.Register.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onGoToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToCatalog = { navController.navigate(Screen.Catalog.route) },
                onNavigateToBookings = { navController.navigate(Screen.Bookings.route) },
                onNavigateToItinerary = { navController.navigate(Screen.Itinerary.route) },
                onNavigateToCostCalc = { navController.navigate(Screen.CostCalculator.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToProviderPanel = {
                    if (TokenManager.getUserRole() == "prestador") {
                        navController.navigate(Screen.ProviderPanel.route)
                    }
                },
                onLogout = {
                    TokenManager.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Catalog.route) {
            CatalogScreen(
                onServiceClick = { serviceId ->
                    navController.navigate(Screen.ServiceDetail.createRoute(serviceId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ServiceDetail.route) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: return@composable
            ServiceDetailScreen(
                serviceId = serviceId,
                onBook = { id ->
                    navController.navigate(Screen.NewBooking.createRoute(id))
                },
                onChat = { bookingId, serviceName ->
                    navController.navigate(Screen.Chat.createRoute(bookingId, serviceName))
                },
                onViewReviews = { id ->
                    navController.navigate(Screen.Reviews.createRoute(id))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Bookings.route) {
            BookingScreen(
                onChat = { bookingId, serviceName ->
                    navController.navigate(Screen.Chat.createRoute(bookingId, serviceName))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.NewBooking.route) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: return@composable
            NewBookingScreen(
                serviceId = serviceId,
                onBookingCreated = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Itinerary.route) {
            ItineraryScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CostCalculator.route) {
            CostCalculatorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Chat.route) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getString("bookingId")?.toIntOrNull() ?: return@composable
            val serviceName = backStackEntry.arguments?.getString("serviceName") ?: ""
            ChatScreen(
                bookingId = bookingId,
                serviceName = serviceName,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reviews.route) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: return@composable
            ReviewsScreen(serviceId = serviceId, onBack = { navController.popBackStack() })
        }
        composable(Screen.ProviderPanel.route) {
            ProviderPanelScreen(
                onManageService = { serviceId ->
                    navController.navigate(Screen.ManageService.createRoute(serviceId))
                },
                onCreateService = {
                    navController.navigate(Screen.ManageService.createRoute(0))
                },
                onViewBookings = { navController.navigate(Screen.ProviderBookings.route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ManageService.route) { backStackEntry ->
            val serviceId = backStackEntry.arguments?.getString("serviceId")?.toIntOrNull() ?: 0
            ManageServiceScreen(
                serviceId = serviceId,
                onSaved = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.ProviderBookings.route) {
            ProviderBookingsScreen(
                onChat = { bookingId, serviceName ->
                    navController.navigate(Screen.Chat.createRoute(bookingId, serviceName))
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
