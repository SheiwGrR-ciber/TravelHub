package com.travelhub.ui.itinerary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.ItineraryCreate
import com.travelhub.data.model.ItineraryResponse
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var itineraries by remember { mutableStateOf<List<ItineraryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var newDay by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val token = TokenManager.getToken()

    fun loadItineraries() {
        if (token == null) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.getItineraries(token)
                if (response.isSuccessful) {
                    itineraries = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar itinerarios: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun createItinerary() {
        val day = newDay.toIntOrNull() ?: return
        if (token == null) return
        scope.launch {
            isSubmitting = true
            try {
                val response = ApiClient.api.createItinerary(
                    token = token,
                    itinerary = ItineraryCreate(
                        day = day,
                        route_data = emptyMap()
                    )
                )
                if (response.isSuccessful) {
                    showCreateForm = false
                    newDay = ""
                    loadItineraries()
                }
            } catch (_: Exception) {} finally {
                isSubmitting = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadItineraries()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Itinerario") },
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
        Column(
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
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = errorMessage ?: "", color = Error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { loadItineraries() },
                                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
                !showCreateForm && itineraries.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes itinerarios a\u00fan",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextoClaro
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(itineraries) { itinerary ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(3.dp),
                                colors = CardDefaults.cardColors(containerColor = Superficie)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Terracota
                                        ) {
                                            Text(
                                                text = "D\u00eda ${itinerary.day}",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = CremaCalido
                                            )
                                        }
                                    }

                                    if (itinerary.route_data != null && itinerary.route_data.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Ruta: ${itinerary.route_data.entries.joinToString(", ") { "${it.key}: ${it.value}" }}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextoClaro
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Sin datos de ruta",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextoClaro
                                        )
                                    }

                                    if (itinerary.created_at != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Creado: ${itinerary.created_at}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextoClaro.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (showCreateForm) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 8.dp,
                            color = Superficie
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Nuevo Itinerario",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextoOscuro
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = newDay,
                                    onValueChange = { newDay = it },
                                    label = { Text("N\u00famero de d\u00eda") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Terracota,
                                        focusedLabelColor = Terracota
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            showCreateForm = false
                                            newDay = ""
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Cancelar")
                                    }
                                    Button(
                                        onClick = { createItinerary() },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isSubmitting && newDay.toIntOrNull() != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isSubmitting) {
                                            CircularProgressIndicator(
                                                color = CremaCalido,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Crear", color = CremaCalido)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (!showCreateForm) {
                        Button(
                            onClick = { showCreateForm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Crear Itinerario",
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
