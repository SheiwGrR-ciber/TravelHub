package com.travelhub.data.api

import com.travelhub.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface TravelHubApi {
    @POST("auth/register")
    suspend fun register(@Body user: UserCreate): Response<Map<String, Any>>

    @POST("auth/login")
    suspend fun login(@Body login: LoginRequest): Response<TokenResponse>

    @POST("auth/verify")
    suspend fun verifyEmail(@Body request: VerifyRequest): Response<Map<String, Any>>

    @POST("auth/resend-code")
    suspend fun resendVerificationCode(@Body request: ResendRequest): Response<Map<String, Any>>

    @POST("auth/google")
    suspend fun googleAuth(@Body request: GoogleAuthRequest): Response<Map<String, Any>>

    @GET("auth/me")
    suspend fun getProfile(@Header("Authorization") token: String): Response<UserResponse>

    @PUT("auth/me")
    suspend fun updateProfile(
        @Header("Authorization") token: String,
        @Body profile: UserProfileUpdate
    ): Response<UserResponse>

    @GET("services")
    suspend fun getServices(
        @Query("type") type: String? = null,
        @Query("location") location: String? = null,
        @Query("min_price") minPrice: Double? = null,
        @Query("max_price") maxPrice: Double? = null,
        @Query("min_rating") minRating: Double? = null,
        @Query("available") available: Boolean? = null
    ): Response<List<ServiceResponse>>

    @GET("services/{id}")
    suspend fun getService(@Path("id") id: Int): Response<ServiceResponse>

    @POST("services")
    suspend fun createService(
        @Header("Authorization") token: String,
        @Body service: ServiceCreate
    ): Response<ServiceResponse>

    @PUT("services/{id}")
    suspend fun updateService(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body service: ServiceUpdate
    ): Response<ServiceResponse>

    @DELETE("services/{id}")
    suspend fun deleteService(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Map<String, Any>>

    @GET("bookings")
    suspend fun getBookings(
        @Header("Authorization") token: String
    ): Response<List<BookingResponse>>

    @POST("bookings")
    suspend fun createBooking(
        @Header("Authorization") token: String,
        @Body booking: BookingCreate
    ): Response<BookingResponse>

    @GET("bookings/{id}")
    suspend fun getBooking(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<BookingResponse>

    @PUT("bookings/{id}/status")
    suspend fun updateBookingStatus(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body status: BookingStatusUpdate
    ): Response<BookingResponse>

    @GET("itineraries")
    suspend fun getItineraries(
        @Header("Authorization") token: String
    ): Response<List<ItineraryResponse>>

    @POST("itineraries")
    suspend fun createItinerary(
        @Header("Authorization") token: String,
        @Body itinerary: ItineraryCreate
    ): Response<ItineraryResponse>

    @GET("itineraries/{id}")
    suspend fun getItinerary(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<ItineraryResponse>

    @POST("itineraries/{id}/add-booking/{booking_id}")
    suspend fun addBookingToItinerary(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Path("booking_id") bookingId: Int
    ): Response<Map<String, Any>>

    @PUT("itineraries/{id}")
    suspend fun updateItinerary(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body itinerary: ItineraryUpdate
    ): Response<ItineraryResponse>

    @DELETE("itineraries/{id}")
    suspend fun deleteItinerary(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<Map<String, Any>>

    @POST("itineraries/{id}/remove-booking/{booking_id}")
    suspend fun removeBookingFromItinerary(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Path("booking_id") bookingId: Int
    ): Response<Map<String, Any>>

    @GET("itineraries/{id}/bookings")
    suspend fun getItineraryBookings(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<List<ItineraryBooking>>

    @POST("itineraries/{id}/directions")
    suspend fun calculateDirections(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body request: DirectionsRequest
    ): Response<DirectionsResponse>

    @POST("messages/")
    suspend fun sendMessage(
        @Header("Authorization") token: String,
        @Body message: MessageCreate
    ): Response<MessageResponse>

    @GET("messages/booking/{booking_id}")
    suspend fun getMessages(
        @Header("Authorization") token: String,
        @Path("booking_id") bookingId: Int
    ): Response<List<MessageResponse>>

    @POST("reviews/")
    suspend fun createReview(
        @Header("Authorization") token: String,
        @Body review: ReviewCreate
    ): Response<ReviewResponse>

    @GET("reviews/service/{service_id}")
    suspend fun getServiceReviews(
        @Path("service_id") serviceId: Int
    ): Response<List<ReviewResponse>>

    @POST("costs/calculate")
    suspend fun calculateCosts(
        @Header("Authorization") token: String,
        @Body request: CostCalculateRequest
    ): Response<CostResponse>

    @GET("admin/prestadores-pendientes")
    suspend fun getPendingPrestadores(
        @Header("Authorization") token: String
    ): Response<List<PrestadorPending>>

    @POST("admin/aprobar/{user_id}")
    suspend fun approvePrestador(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Response<Map<String, Any>>

    @POST("admin/rechazar/{user_id}")
    suspend fun rejectPrestador(
        @Header("Authorization") token: String,
        @Path("user_id") userId: Int
    ): Response<Map<String, Any>>
}
