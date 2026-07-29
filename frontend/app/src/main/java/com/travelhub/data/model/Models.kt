package com.travelhub.data.model

data class LoginRequest(val email: String, val password: String)
data class TokenResponse(val access_token: String, val token_type: String)
data class UserCreate(val name: String, val email: String, val password: String, val role: String)
data class UserResponse(val id: Int, val email: String, val role: String)
data class ServiceCreate(
    val type: String, val name: String, val description: String,
    val price: Double, val location: String
)
data class ServiceUpdate(
    val type: String? = null, val name: String? = null,
    val description: String? = null, val price: Double? = null,
    val location: String? = null, val available: Boolean? = null
)
data class ServiceResponse(
    val id: Int, val provider_id: Int, val type: String, val name: String,
    val description: String?, val price: Double, val location: String?,
    val rating: Double, val available: Boolean, val created_at: String?
)
data class BookingCreate(val service_id: Int, val date: String)
data class BookingResponse(
    val id: Int, val tourist_id: Int, val service_id: Int,
    val date: String, val status: String, val total: Double,
    val created_at: String?
)
data class BookingStatusUpdate(val status: String)
data class ItineraryCreate(val day: Int, val route_data: Map<String, Any>)
data class ItineraryResponse(
    val id: Int, val tourist_id: Int, val day: Int,
    val route_data: Map<String, Any>?, val created_at: String?
)
data class MessageCreate(val receiver_id: Int, val booking_id: Int, val content: String)
data class MessageResponse(
    val id: Int, val sender_id: Int, val receiver_id: Int,
    val booking_id: Int, val content: String, val read: Boolean,
    val timestamp: String?
)
data class ReviewCreate(val booking_id: Int, val rating: Int, val comment: String)
data class ReviewResponse(
    val id: Int, val booking_id: Int, val tourist_id: Int,
    val service_id: Int, val rating: Int, val comment: String,
    val created_at: String?
)
data class CostCalculateRequest(val booking_ids: List<Int>)
data class CostResponse(
    val total: Double, val breakdown: Map<String, Double>,
    val details: List<CostDetail>
)
data class CostDetail(
    val booking_id: Int, val service_name: String,
    val category: String, val price: Double
)
