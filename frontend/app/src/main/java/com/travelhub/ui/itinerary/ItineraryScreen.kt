package com.travelhub.ui.itinerary

import androidx.compose.foundation.background
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
fun ItineraryScreen(
    onBack: () -> Unit,
    onOpenBuilder: (itineraryId: Int) -> Unit
) {
    val scope = rememberCoroutineScope()
    var itineraries by remember { mutableStateOf<List<ItineraryResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateForm by remember { mutableStateOf(false) }
    var newDay by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var deletingId by remember { mutableStateOf<Int?>(null) }

    val token = TokenManager.getToken()

    fun load() {
        if (token == null) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.getItineraries(token)
                if (response.isSuccessful) {
                    itineraries = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
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
                    itinerary = ItineraryCreate(day = day, route_data = emptyMap())
                )
                if (response.isSuccessful) {
                    showCreateForm = false
                    newDay = ""
                    load()
                }
            } catch (_: Exception) {} finally {
                isSubmitting = false
            }
        }
    }

    fun deleteItinerary(id: Int) {
        if (token == null) return
        scope.launch {
            deletingId = id
            try {
                ApiClient.api.deleteItinerary(token, id)
                load()
            } catch (_: Exception) {} finally {
                deletingId = null
            }
        }
    }

    LaunchedEffect(Unit) { load() }

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
            modifier = Modifier.fillMaxSize().padding(padding).background(CremaClaro)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Terracota)
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = errorMessage ?: "", color = Error)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { load() },
                                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                            ) { Text("Reintentar") }
                        }
                    }
                }
                !showCreateForm && itineraries.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No tienes itinerarios aún", style = MaterialTheme.typography.bodyLarge, color = TextoClaro)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { showCreateForm = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                            ) { Text("Crear Itinerario") }
                        }
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
                                                text = "Día ${itinerary.day}",
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = CremaCalido
                                            )
                                        }
                                        Spacer(Modifier.weight(1f))
                                        IconButton(onClick = { deleteItinerary(itinerary.id) }) {
                                            if (deletingId == itinerary.id) {
                                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Error)
                                            } else {
                                                Icon(Icons.Default.Delete, "Eliminar", tint = Error)
                                            }
                                        }
                                    }

                                    val rd = itinerary.route_data
                                    val points = rd?.get("points") as? List<*> ?: emptyList<Any>()
                                    val bookingIds = rd?.get("booking_ids") as? List<*> ?: emptyList<Any>()

                                    if (points.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text("${points.size} punto(s) en ruta", style = MaterialTheme.typography.bodyMedium, color = TextoClaro)
                                    }
                                    if (bookingIds.isNotEmpty()) {
                                        Text("${bookingIds.size} reserva(s) incluidas", style = MaterialTheme.typography.bodySmall, color = TextoClaro)
                                    }
                                    if (points.isEmpty() && bookingIds.isEmpty()) {
                                        Text("Sin datos de ruta", style = MaterialTheme.typography.bodySmall, color = TextoClaro)
                                    }

                                    Spacer(Modifier.height(12.dp))
                                    Button(
                                        onClick = { onOpenBuilder(itinerary.id) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Map, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Construir Ruta", color = CremaCalido)
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
                                Text("Nuevo Itinerario", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextoOscuro)
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = newDay,
                                    onValueChange = { newDay = it },
                                    label = { Text("Número de día") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Terracota, focusedLabelColor = Terracota),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { showCreateForm = false; newDay = "" },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) { Text("Cancelar") }
                                    Button(
                                        onClick = { createItinerary() },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isSubmitting && newDay.toIntOrNull() != null,
                                        colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        if (isSubmitting) CircularProgressIndicator(color = CremaCalido, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        else Text("Crear", color = CremaCalido)
                                    }
                                }
                            }
                        }
                    }

                    if (!showCreateForm) {
                        Button(
                            onClick = { showCreateForm = true },
                            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Crear Itinerario", fontWeight = FontWeight.Bold, color = CremaCalido)
                        }
                    }
                }
            }
        }
    }
}
