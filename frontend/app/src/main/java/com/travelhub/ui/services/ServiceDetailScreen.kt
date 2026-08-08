package com.travelhub.ui.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.BookingResponse
import com.travelhub.data.model.ServiceResponse
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch
import java.net.URLEncoder
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceDetailScreen(
    serviceId: Int,
    onBook: (Int) -> Unit,
    onChat: (Int, String) -> Unit,
    onViewReviews: (Int) -> Unit,
    onBack: () -> Unit,
    serviceDetailViewModel: ServiceDetailViewModel = viewModel()
) {
    val uiState by serviceDetailViewModel.uiState.collectAsStateWithLifecycle()
    val userRole = TokenManager.getUserRole()

    LaunchedEffect(serviceId) {
        serviceDetailViewModel.load(serviceId)
    }

    val service = uiState.service
    val confirmedBookingForService = uiState.confirmedBooking(serviceId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(service?.name ?: "Detalle del Servicio") },
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
                            onClick = { serviceDetailViewModel.load(serviceId, force = true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            service != null -> {
                val s = service!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .background(CremaClaro)
                        .padding(24.dp)
                ) {
                    val typeColor = when (s.type.lowercase()) {
                        "guia" -> TealProfundo
                        "hotel" -> DoradoOscuro
                        else -> TextoClaro
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = s.type.replaceFirstChar { it.uppercase() },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = typeColor,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = s.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextoOscuro
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (i < s.rating.toInt()) DoradoArena else TextoClaro.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${String.format("%.1f", s.rating)} / 5.0",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoClaro
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!s.location.isNullOrBlank()) {
                        DetailRow(label = "Ubicaci\u00f3n", value = s.location)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    DetailRow(label = "Precio", value = "$${String.format("%.2f", s.price)}")
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (s.available) VerdeExito.copy(alpha = 0.1f) else Error.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = if (s.available) "Disponible" else "No disponible",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (s.available) VerdeExito else Error,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (!s.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Descripci\u00f3n",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextoOscuro
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextoClaro
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onViewReviews(serviceId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DoradoArena),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Ver rese\u00f1as",
                            color = TextoOscuro,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    if (userRole == "turista") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onBook(serviceId) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                            shape = RoundedCornerShape(12.dp),
                            enabled = s.available
                        ) {
                            Text(
                                text = "Reservar ahora",
                                color = CremaCalido,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }

                    if (confirmedBookingForService != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = {
                                val encodedName = URLEncoder.encode(s.name, "UTF-8")
                                onChat(confirmedBookingForService.id, s.name)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealProfundo)
                        ) {
                            Text(
                                text = "Enviar mensaje",
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextoClaro
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextoOscuro
        )
    }
}
