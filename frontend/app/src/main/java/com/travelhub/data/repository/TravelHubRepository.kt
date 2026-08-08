package com.travelhub.data.repository

import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.BookingResponse
import com.travelhub.data.model.BookingStatusUpdate
import com.travelhub.data.model.LoginRequest
import com.travelhub.data.model.ServiceResponse
import com.travelhub.data.model.UserResponse
import com.travelhub.data.model.UserCreate
import com.travelhub.data.model.VerifyRequest
import com.travelhub.data.model.ResendRequest
import com.travelhub.data.model.ServiceCreate
import com.travelhub.data.model.ServiceUpdate
import com.travelhub.data.model.ItineraryCreate
import com.travelhub.data.model.ItineraryResponse

sealed interface RepositoryResult<out T> {
    data class Success<T>(val value: T) : RepositoryResult<T>
    data class Error(val message: String, val statusCode: Int? = null) : RepositoryResult<Nothing>
}

class TravelHubRepository {
    suspend fun itineraries(token: String): RepositoryResult<List<ItineraryResponse>> = runRequest {
        val response = ApiClient.api.getItineraries(token)
        if (response.isSuccessful) RepositoryResult.Success(response.body().orEmpty())
        else RepositoryResult.Error("No se pudieron cargar los itinerarios", response.code())
    }

    suspend fun createItinerary(token: String, day: Int): RepositoryResult<ItineraryResponse> = runRequest {
        val response = ApiClient.api.createItinerary(token, ItineraryCreate(day, emptyMap()))
        val body = response.body()
        if (response.isSuccessful && body != null) RepositoryResult.Success(body)
        else RepositoryResult.Error("No se pudo crear el itinerario", response.code())
    }

    suspend fun deleteItinerary(token: String, itineraryId: Int): RepositoryResult<Unit> = runRequest {
        val response = ApiClient.api.deleteItinerary(token, itineraryId)
        if (response.isSuccessful) RepositoryResult.Success(Unit)
        else RepositoryResult.Error("No se pudo eliminar el itinerario", response.code())
    }

    suspend fun createService(token: String, service: ServiceCreate): RepositoryResult<ServiceResponse> = runRequest {
        val response = ApiClient.api.createService(token, service)
        val body = response.body()
        if (response.isSuccessful && body != null) RepositoryResult.Success(body)
        else RepositoryResult.Error("No se pudo crear el servicio", response.code())
    }

    suspend fun updateService(
        token: String,
        serviceId: Int,
        service: ServiceUpdate
    ): RepositoryResult<ServiceResponse> = runRequest {
        val response = ApiClient.api.updateService(token, serviceId, service)
        val body = response.body()
        if (response.isSuccessful && body != null) RepositoryResult.Success(body)
        else RepositoryResult.Error("No se pudo actualizar el servicio", response.code())
    }

    suspend fun deleteService(token: String, serviceId: Int): RepositoryResult<Unit> = runRequest {
        val response = ApiClient.api.deleteService(token, serviceId)
        if (response.isSuccessful) RepositoryResult.Success(Unit)
        else RepositoryResult.Error("No se pudo eliminar el servicio", response.code())
    }

    suspend fun service(serviceId: Int): RepositoryResult<ServiceResponse> = runRequest {
        val response = ApiClient.api.getService(serviceId)
        val service = response.body()
        if (response.isSuccessful && service != null) RepositoryResult.Success(service)
        else RepositoryResult.Error("No se pudo cargar el servicio", response.code())
    }

    suspend fun verifyEmail(email: String, code: String): RepositoryResult<Pair<String, UserResponse>> =
        runRequest {
            val response = ApiClient.api.verifyEmail(VerifyRequest(email, code))
            if (!response.isSuccessful) {
                return@runRequest RepositoryResult.Error(
                    if (response.code() == 400) "El codigo es incorrecto o ha expirado" else "No se pudo verificar el correo",
                    response.code()
                )
            }
            val token = response.body()?.get("access_token") as? String
                ?: return@runRequest RepositoryResult.Error("El servidor no devolvio una sesion valida")
            val profileResponse = ApiClient.api.getProfile("Bearer $token")
            val profile = profileResponse.body()
            if (!profileResponse.isSuccessful || profile == null) {
                return@runRequest RepositoryResult.Error("No se pudo recuperar el perfil verificado")
            }
            RepositoryResult.Success(token to profile)
        }

    suspend fun resendVerificationCode(email: String): RepositoryResult<Unit> = runRequest {
        val response = ApiClient.api.resendVerificationCode(ResendRequest(email))
        if (response.isSuccessful) RepositoryResult.Success(Unit)
        else RepositoryResult.Error("No se pudo reenviar el codigo", response.code())
    }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: String
    ): RepositoryResult<Unit> = runRequest {
        val response = ApiClient.api.register(UserCreate(name, email, password, role))
        if (response.isSuccessful) {
            RepositoryResult.Success(Unit)
        } else {
            val message = when (response.code()) {
                400, 409 -> "El correo ya esta registrado"
                422 -> "Revisa los datos ingresados"
                else -> "No se pudo crear la cuenta (${response.code()})"
            }
            RepositoryResult.Error(message, response.code())
        }
    }

    suspend fun login(email: String, password: String): RepositoryResult<Pair<String, UserResponse>> =
        runRequest {
            val loginResponse = ApiClient.api.login(LoginRequest(email, password))
            if (!loginResponse.isSuccessful) {
                return@runRequest RepositoryResult.Error(loginError(loginResponse.code()), loginResponse.code())
            }
            val token = loginResponse.body()?.access_token
                ?: return@runRequest RepositoryResult.Error("El servidor devolvio una respuesta vacia")
            val profileResponse = ApiClient.api.getProfile("Bearer $token")
            if (!profileResponse.isSuccessful) {
                return@runRequest RepositoryResult.Error("No se pudo obtener el perfil", profileResponse.code())
            }
            val profile = profileResponse.body()
                ?: return@runRequest RepositoryResult.Error("El perfil recibido esta vacio")
            RepositoryResult.Success(token to profile)
        }

    suspend fun services(type: String?): RepositoryResult<List<ServiceResponse>> = runRequest {
        val response = ApiClient.api.getServices(type = type)
        if (response.isSuccessful) {
            RepositoryResult.Success(response.body().orEmpty())
        } else {
            RepositoryResult.Error("No se pudieron cargar los servicios", response.code())
        }
    }

    suspend fun bookings(token: String): RepositoryResult<List<BookingResponse>> = runRequest {
        val response = ApiClient.api.getBookings(token)
        if (response.isSuccessful) {
            RepositoryResult.Success(response.body().orEmpty())
        } else {
            RepositoryResult.Error("No se pudieron cargar las reservas", response.code())
        }
    }

    suspend fun updateBookingStatus(
        token: String,
        bookingId: Int,
        status: String
    ): RepositoryResult<BookingResponse> = runRequest {
        val response = ApiClient.api.updateBookingStatus(token, bookingId, BookingStatusUpdate(status))
        if (response.isSuccessful && response.body() != null) {
            RepositoryResult.Success(response.body()!!)
        } else {
            RepositoryResult.Error("No se pudo actualizar la reserva", response.code())
        }
    }

    private suspend fun <T> runRequest(block: suspend () -> RepositoryResult<T>): RepositoryResult<T> =
        try {
            block()
        } catch (exception: Exception) {
            RepositoryResult.Error(exception.localizedMessage ?: "No se pudo conectar con el servidor")
        }

    private fun loginError(statusCode: Int): String = when (statusCode) {
        401 -> "Correo o contrasena incorrectos"
        403 -> "Debes verificar tu correo antes de iniciar sesion"
        422 -> "Los datos ingresados no son validos"
        else -> "No se pudo iniciar sesion ($statusCode)"
    }
}
