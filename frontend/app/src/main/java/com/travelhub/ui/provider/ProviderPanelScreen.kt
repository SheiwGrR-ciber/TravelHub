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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookOnline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
fun ProviderPanelScreen(
    onManageService: (Int) -> Unit,
    onCreateService: () -> Unit,
    onViewBookings: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val token = TokenManager.getToken()
    val currentUserId = TokenManager.getUserId()

    var services by remember { mutableStateOf<List<ServiceResponse>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ServiceResponse?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val svcResp = ApiClient.api.getServices()
                if (svcResp.isSuccessful) {
                    val allServices = svcResp.body() ?: emptyList()
                    services = allServices.filter { it.provider_id == currentUserId }
                }

                if (token != null) {
                    val bookResp = ApiClient.api.getBookings(token)
                    if (bookResp.isSuccessful) {
                        bookings = bookResp.body() ?: emptyList()
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Eliminar Servicio") },
            text = { Text("\u00bfEst\u00e1s seguro de eliminar \u201c${deleteTarget!!.name}\u201d?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = deleteTarget!!
                        deleteTarget = null
                        if (token != null) {
                            scope.launch {
                                try {
                                    val resp = ApiClient.api.deleteService(token, target.id)
                                    if (resp.isSuccessful) {
                                        services = services.filter { it.id != target.id }
                                    }
                                } catch (_: Exception) { }
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel del Prestador", fontWeight = FontWeight.SemiBold) },
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
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = TealProfundo),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "${services.size}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = CremaCalido,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Servicios",
                                            color = CremaCalido.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = DoradoArena),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            "${bookings.size}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = TextoOscuro,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "Reservas",
                                            color = TextoOscuro.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                "Mis Servicios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MarronOscuro
                            )
                        }

                        if (services.isEmpty()) {
                            item {
                                Text(
                                    "No tienes servicios registrados",
                                    color = TextoClaro,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        } else {
                            items(services, key = { it.id }) { service ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = CremaCalido),
                                    shape = RoundedCornerShape(12.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                service.name,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextoOscuro
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Text(
                                                "${service.type} \u2022 \$${String.format("%.2f", service.price)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextoClaro
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            if (service.available) {
                                                Text(
                                                    "Disponible",
                                                    color = VerdeExito,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            } else {
                                                Text(
                                                    "No disponible",
                                                    color = Error,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        IconButton(onClick = { onManageService(service.id) }) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Editar",
                                                tint = TealProfundo
                                            )
                                        }
                                        IconButton(onClick = { deleteTarget = service }) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Eliminar",
                                                tint = Error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = onCreateService,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Crear Nuevo Servicio", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = onViewBookings,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.BookOnline,
                                    contentDescription = null,
                                    tint = TealProfundo
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Ver Reservas",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TealProfundo
                                )
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}
