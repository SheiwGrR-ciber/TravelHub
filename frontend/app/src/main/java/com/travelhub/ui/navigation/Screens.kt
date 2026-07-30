package com.travelhub.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object Home : Screen("home")
    data object Profile : Screen("profile")
    data object Catalog : Screen("catalog")
    data object ServiceDetail : Screen("service_detail/{serviceId}") {
        fun createRoute(serviceId: Int) = "service_detail/$serviceId"
    }
    data object Bookings : Screen("bookings")
    data object NewBooking : Screen("new_booking/{serviceId}") {
        fun createRoute(serviceId: Int) = "new_booking/$serviceId"
    }
    data object Itinerary : Screen("itinerary")
    data object CreateItinerary : Screen("create_itinerary")
    data object CostCalculator : Screen("cost_calculator")
    data object Chat : Screen("chat/{bookingId}/{serviceName}") {
        fun createRoute(bookingId: Int, serviceName: String) = "chat/$bookingId/$serviceName"
    }
    data object ProviderPanel : Screen("provider_panel")
    data object ManageService : Screen("manage_service/{serviceId}") {
        fun createRoute(serviceId: Int = 0) = "manage_service/$serviceId"
    }
    data object ProviderBookings : Screen("provider_bookings")
    data object Reviews : Screen("reviews/{serviceId}") {
        fun createRoute(serviceId: Int) = "reviews/$serviceId"
    }
    data object VerifyEmail : Screen("verify_email/{email}/{code}") {
        fun createRoute(email: String, code: String) = "verify_email/$email/$code"
    }
}
