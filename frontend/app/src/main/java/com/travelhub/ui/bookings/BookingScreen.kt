package com.travelhub.ui.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travelhub.data.model.BookingResponse
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    onChat: (Int, String) -> Unit,
    onBack: () -> Unit,
    bookingViewModel: BookingViewModel = viewModel()
) {
    val uiState by bookingViewModel.uiState.collectAsStateWithLifecycle()
    val userRole = TokenManager.getUserRole()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Reservas") },
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
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Terracota)
                }
            }
            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = uiState.errorMessage.orEmpty(), color = Error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = bookingViewModel::loadBookings,
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            uiState.bookings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes reservas a\u00fan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextoClaro
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(CremaClaro),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.bookings, key = { it.id }) { booking ->
                        BookingCard(
                            booking = booking,
                            userRole = userRole,
                            onChat = { onChat(booking.id, "Servicio #${booking.service_id}") },
                            onAccept = { bookingViewModel.updateStatus(booking.id, "confirmada") },
                            onCancel = { bookingViewModel.updateStatus(booking.id, "cancelada") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookingCard(
    booking: BookingResponse,
    userRole: String?,
    onChat: () -> Unit,
    onAccept: () -> Unit,
    onCancel: () -> Unit
) {
    val statusColor = when (booking.status.lowercase()) {
        "pendiente" -> DoradoArena
        "confirmada" -> VerdeExito
        "cancelada" -> Error
        else -> TextoClaro
    }
    val statusBg = statusColor.copy(alpha = 0.12f)
    val statusLabel = when (booking.status.lowercase()) {
        "pendiente" -> "Pendiente"
        "confirmada" -> "Confirmada"
        "cancelada" -> "Cancelada"
        else -> booking.status
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Servicio #${booking.service_id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoOscuro,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Fecha: ${booking.date}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoClaro
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Total: $${String.format("%.2f", booking.total)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = Terracota
            )

            if (booking.status.lowercase() == "confirmada") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onChat,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TealProfundo)
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat", fontWeight = FontWeight.Medium)
                }
            }

            if (userRole == "prestador" && booking.status.lowercase() == "pendiente") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VerdeExito)
                    ) {
                        Text("Aceptar", color = CremaCalido)
                    }
                }
            }
        }
    }
}
