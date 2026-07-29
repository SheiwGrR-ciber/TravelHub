package com.travelhub.ui.provider

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.BookingResponse
import com.travelhub.data.model.BookingStatusUpdate
import com.travelhub.data.model.ServiceResponse
import com.travelhub.ui.theme.CremaCalido
import com.travelhub.ui.theme.DoradoArena
import com.travelhub.ui.theme.Error
import com.travelhub.ui.theme.MarronOscuro
import com.travelhub.ui.theme.TealProfundo
import com.travelhub.ui.theme.Terracota
import com.travelhub.ui.theme.TextoClaro
import com.travelhub.ui.theme.TextoOscuro
import com.travelhub.ui.theme.VerdeExito
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderBookingsScreen(
    onChat: (Int, String) -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val token = TokenManager.getToken()

    var bookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var servicesMap by remember { mutableStateOf<Map<Int, ServiceResponse>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var updatingId by remember { mutableStateOf<Int?>(null) }

    fun loadData() {
        if (token == null) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val svcResp = ApiClient.api.getServices()
                if (svcResp.isSuccessful) {
                    servicesMap = (svcResp.body() ?: emptyList()).associateBy { it.id }
                }

                val bookResp = ApiClient.api.getBookings(token)
                if (bookResp.isSuccessful) {
                    bookings = bookResp.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar reservas"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateStatus(bookingId: Int, newStatus: String) {
        if (token == null) return
        updatingId = bookingId
        scope.launch {
            try {
                val resp = ApiClient.api.updateBookingStatus(
                    token,
                    bookingId,
                    BookingStatusUpdate(newStatus)
                )
                if (resp.isSuccessful) {
                    loadData()
                } else {
                    errorMessage = "Error al actualizar reserva"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.message}"
            } finally {
                updatingId = null
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reservas Recibidas", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MarronOscuro,
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
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Terracota
                    )
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = Error)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { loadData() }) {
                            Text("Reintentar")
                        }
                    }
                }
                bookings.isEmpty() -> {
                    Text(
                        "No tienes reservas recibidas",
                        modifier = Modifier.align(Alignment.Center),
                        color = TextoClaro,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(bookings, key = { it.id }) { booking ->
                            val service = servicesMap[booking.service_id]
                            val serviceName = service?.name ?: "Servicio #${booking.service_id}"
                            val isUpdating = updatingId == booking.id

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = CremaCalido),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            serviceName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextoOscuro,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        StatusChip(booking.status)
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Text(
                                        "Reserva #${booking.id}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoClaro
                                    )
                                    Text(
                                        "Fecha: ${booking.date}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextoClaro
                                    )
                                    Text(
                                        "Total: \$${String.format("%.2f", booking.total)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = TextoOscuro
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    when (booking.status) {
                                        "pendiente" -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { updateStatus(booking.id, "confirmada") },
                                                    enabled = !isUpdating,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = VerdeExito
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    if (isUpdating) {
                                                        CircularProgressIndicator(
                                                            color = CremaCalido,
                                                            modifier = Modifier.height(18.dp)
                                                        )
                                                    } else {
                                                        Text("Aceptar", fontWeight = FontWeight.SemiBold)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = { updateStatus(booking.id, "cancelada") },
                                                    enabled = !isUpdating,
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text(
                                                        "Rechazar",
                                                        color = Error,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }
                                        "confirmada" -> {
                                            Button(
                                                onClick = { onChat(booking.id, serviceName) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = TealProfundo
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    Icons.Default.Chat,
                                                    contentDescription = null,
                                                    modifier = Modifier.height(18.dp)
                                                )
                                                Spacer(Modifier.width(6.dp))
                                                Text("Chat", fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                        "cancelada" -> {
                                            Text(
                                                "Reserva cancelada",
                                                color = Error,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        "completada" -> {
                                            Text(
                                                "Reserva completada",
                                                color = VerdeExito,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (label, bgColor, textColor) = when (status) {
        "pendiente" -> Triple("Pendiente", DoradoArena, TextoOscuro)
        "confirmada" -> Triple("Confirmada", TealProfundo, CremaCalido)
        "cancelada" -> Triple("Cancelada", Error, CremaCalido)
        "completada" -> Triple("Completada", VerdeExito, CremaCalido)
        else -> Triple(status, TextoClaro, CremaCalido)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bgColor
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
