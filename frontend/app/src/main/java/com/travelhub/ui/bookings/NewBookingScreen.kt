package com.travelhub.ui.bookings

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.BookingCreate
import com.travelhub.data.model.ServiceResponse
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewBookingScreen(
    serviceId: Int,
    onBookingCreated: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var service by remember { mutableStateOf<ServiceResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val calendar = remember { Calendar.getInstance() }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    var selectedDate by remember { mutableStateOf(dateFormat.format(calendar.time)) }

    val token = TokenManager.getToken()

    LaunchedEffect(serviceId) {
        isLoading = true
        try {
            val response = ApiClient.api.getService(serviceId)
            if (response.isSuccessful) {
                service = response.body()
            } else {
                errorMessage = "Error al cargar servicio: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexi\u00f3n: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    fun showDatePicker() {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDate = dateFormat.format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = System.currentTimeMillis()
        }.show()
    }

    fun createBooking() {
        if (token == null) return
        scope.launch {
            isSubmitting = true
            errorMessage = null
            try {
                val response = ApiClient.api.createBooking(
                    token = token,
                    booking = BookingCreate(service_id = serviceId, date = selectedDate)
                )
                if (response.isSuccessful) {
                    onBookingCreated()
                } else {
                    errorMessage = "Error al crear reserva: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.localizedMessage}"
            } finally {
                isSubmitting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nueva Reserva") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Terracota,
                    titleContentColor = CremaCalido,
                    navigationIconContentColor = CremaCalido
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CremaClaro)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Terracota)
                    }
                }
                errorMessage != null && service == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = errorMessage ?: "", color = Error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onBack() },
                                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                            ) {
                                Text("Volver")
                            }
                        }
                    }
                }
                service != null -> {
                    val s = service!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp),
                            colors = CardDefaults.cardColors(containerColor = Superficie)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Text(
                                    text = s.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextoOscuro
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = s.type.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextoClaro
                                )
                                if (!s.location.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = s.location,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextoClaro
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Total: $${String.format("%.2f", s.price)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Terracota
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "Fecha de la reserva",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoOscuro
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = selectedDate,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            readOnly = true,
                            label = { Text("Fecha") },
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker() }) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = "Seleccionar fecha",
                                        tint = Terracota
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Terracota,
                                focusedLabelColor = Terracota
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage ?: "",
                                color = Error,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { createBooking() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = !isSubmitting && s.available,
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = CremaCalido,
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = "Confirmar Reserva",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CremaCalido
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
