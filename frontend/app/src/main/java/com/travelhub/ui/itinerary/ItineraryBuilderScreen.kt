package com.travelhub.ui.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.window.Dialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.*
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

private data class BuilderPoint(
    val lat: Double, val lng: Double,
    val name: String, val order: Int,
    val fromBooking: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryBuilderScreen(itineraryId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val token = TokenManager.getToken()

    var actualId by remember { mutableIntStateOf(itineraryId) }
    var itineraryDay by remember { mutableIntStateOf(1) }
    var points by remember { mutableStateOf<List<BuilderPoint>>(emptyList()) }
    var bookingIds by remember { mutableStateOf<List<Int>>(emptyList()) }
    var directions by remember { mutableStateOf<DirectionsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showAddPointDialog by remember { mutableStateOf(false) }
    var showAddBookingDialog by remember { mutableStateOf(false) }
    var showDayDialog by remember { mutableStateOf(itineraryId == 0) }
    var newDayText by remember { mutableStateOf("1") }
    var userBookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(19.4326, -99.1332), 10f)
    }

    fun load() {
        if (token == null) return
        scope.launch {
            isLoading = true
            errorMsg = null
            try {
                val itResp = ApiClient.api.getItinerary(token, actualId)
                if (itResp.isSuccessful) {
                    val it = itResp.body() ?: return@launch
                    itineraryDay = it.day
                    val rd = it.route_data ?: emptyMap()
                    val rawPoints = rd["points"] as? List<Map<String, Any>> ?: emptyList()
                    points = rawPoints.mapIndexed { i, p ->
                        BuilderPoint(
                            lat = (p["lat"] as? Number)?.toDouble() ?: 0.0,
                            lng = (p["lng"] as? Number)?.toDouble() ?: 0.0,
                            name = p["name"] as? String ?: "Punto ${i + 1}",
                            order = (p["order"] as? Number)?.toInt() ?: i,
                            fromBooking = false
                        )
                    }
                    bookingIds = (rd["booking_ids"] as? List<Number>)?.map { it.toInt() } ?: emptyList()

                    val bkResp = ApiClient.api.getItineraryBookings(token, actualId)
                    if (bkResp.isSuccessful) {
                        val bookings = bkResp.body() ?: emptyList()
                        for (b in bookings) {
                            if (!points.any { it.name == b.service_name && it.fromBooking }) {
                                val loc = parseLocation(b.service_location)
                                if (loc != null) {
                                    points = points + BuilderPoint(loc.first, loc.second, b.service_name, points.size, fromBooking = true)
                                }
                            }
                        }
                    }

                    if (points.isNotEmpty()) {
                        val avgLat = points.map { it.lat }.average()
                        val avgLng = points.map { it.lng }.average()
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(LatLng(avgLat, avgLng), 12f)
                    }
                }
            } catch (e: Exception) {
                errorMsg = "Error: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun save() {
        if (token == null) return
        scope.launch {
            isSaving = true
            try {
                val routeData = mapOf<String, Any>(
                    "points" to points.map { p ->
                        mapOf<String, Any>(
                            "lat" to p.lat, "lng" to p.lng,
                            "name" to p.name, "order" to p.order
                        )
                    },
                    "booking_ids" to bookingIds
                )
                if (directions != null) {
                    mapOf(
                        "total_distance_km" to directions!!.total_distance_km,
                        "total_duration_min" to directions!!.total_duration_min
                    )
                }
                ApiClient.api.updateItinerary(token, actualId, ItineraryUpdate(routeData))
                onBack()
            } catch (_: Exception) {} finally {
                isSaving = false
            }
        }
    }

    fun calculateRoute() {
        if (token == null || points.size < 2) return
        scope.launch {
            isLoading = true
            try {
                val pts = points.map { RoutePoint(it.lat, it.lng, it.name, it.order) }
                val req = DirectionsRequest(
                    origin = pts.first(),
                    destination = pts.last(),
                    waypoints = pts.drop(1).dropLast(1)
                )
                val resp = ApiClient.api.calculateDirections(token, actualId, req)
                if (resp.isSuccessful) {
                    directions = resp.body()
                }
            } catch (_: Exception) {} finally {
                isLoading = false
            }
        }
    }

    fun addPoint(lat: Double, lng: Double, name: String) {
        val newP = BuilderPoint(lat, lng, name, points.size, fromBooking = false)
        points = points + newP
        directions = null
    }

    fun removePoint(index: Int) {
        points = points.filterIndexed { i, _ -> i != index }.mapIndexed { i, p -> p.copy(order = i) }
        directions = null
    }

    fun addBookingToItinerary(booking: BookingResponse) {
        if (token == null) return
        scope.launch {
            try {
                ApiClient.api.addBookingToItinerary(token, actualId, booking.id)
                bookingIds = bookingIds + booking.id
                load()
            } catch (_: Exception) {}
        }
    }

    fun loadUserBookings() {
        if (token == null) return
        scope.launch {
            try {
                val resp = ApiClient.api.getBookings(token)
                if (resp.isSuccessful) {
                    userBookings = (resp.body() ?: emptyList()).filter { it.status == "confirmada" || it.status == "pendiente" }
                    showAddBookingDialog = true
                }
            } catch (_: Exception) {}
        }
    }

    fun createNewItinerary() {
        val day = newDayText.toIntOrNull() ?: return
        if (token == null) return
        scope.launch {
            isLoading = true
            try {
                val resp = ApiClient.api.createItinerary(token, ItineraryCreate(day, emptyMap()))
                if (resp.isSuccessful) {
                    val created = resp.body() ?: return@launch
                    actualId = created.id
                    itineraryDay = created.day
                    showDayDialog = false
                    load()
                }
            } catch (_: Exception) {} finally {
                isLoading = false
            }
        }
    }

    if (actualId != 0) {
        LaunchedEffect(actualId) { load() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Constructor de Ruta") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                actions = {
                    IconButton(onClick = { save() }, enabled = !isSaving) {
                        if (isSaving) CircularProgressIndicator(color = CremaCalido, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Save, "Guardar", tint = CremaCalido)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Terracota, titleContentColor = CremaCalido, navigationIconContentColor = CremaCalido)
            )
        },
        floatingActionButton = {
            if (actualId != 0) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallFloatingActionButton(
                        onClick = { showAddPointDialog = true },
                        containerColor = Terracota,
                        contentColor = CremaCalido
                    ) { Icon(Icons.Default.AddLocation, "Agregar punto") }
                    SmallFloatingActionButton(
                        onClick = { loadUserBookings() },
                        containerColor = TealProfundo,
                        contentColor = CremaCalido
                    ) { Icon(Icons.Default.BookOnline, "Agregar reserva") }
                    if (points.size >= 2) {
                        SmallFloatingActionButton(
                            onClick = { calculateRoute() },
                            containerColor = DoradoArena,
                            contentColor = MarronOscuro
                        ) { Icon(Icons.Default.Route, "Calcular ruta") }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(CremaCalido)) {
            if (isLoading && actualId == 0) {
                CircularProgressIndicator(color = Terracota, modifier = Modifier.align(Alignment.Center))
            } else if (actualId == 0) {
                Text("Creando itinerario...", modifier = Modifier.align(Alignment.Center), color = TextoClaro)
            } else {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            onMapClick = { latLng ->
                                showAddPointDialog = true
                                scope.launch {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 14f))
                                }
                            },
                            uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)
                        ) {
                            points.forEach { p ->
                                Marker(
                                    state = MarkerState(position = LatLng(p.lat, p.lng)),
                                    title = p.name,
                                    snippet = "${p.lat}, ${p.lng}",
                                    icon = if (p.fromBooking) BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                    else BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                                    onClick = { false }
                                )
                            }
                            if (directions != null && directions!!.polyline.isNotEmpty()) {
                                val decoded = decodePolyline(directions!!.polyline)
                                Polyline(points = decoded, color = TealProfundo, width = 5f)
                            }
                        }

                        if (points.isEmpty()) {
                            Box(Modifier.fillMaxSize().padding(32.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Superficie.copy(alpha = 0.9f),
                                    modifier = Modifier.align(Alignment.TopCenter)
                                ) {
                                    Text(
                                        "Toca el mapa para agregar puntos o presiona + para agregar reservas",
                                        modifier = Modifier.padding(16.dp),
                                        color = TextoOscuro
                                    )
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shadowElevation = 8.dp,
                        color = Superficie
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(shape = RoundedCornerShape(8.dp), color = Terracota) {
                                    Text("Día $itineraryDay", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = CremaCalido, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text("${points.size} punto(s)", style = MaterialTheme.typography.bodySmall, color = TextoClaro)
                                if (directions != null) {
                                    Text(" · ${directions!!.total_distance_km} km · ${directions!!.total_duration_text}",
                                        style = MaterialTheme.typography.bodySmall, color = TealProfundo)
                                }
                            }

                            if (points.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.heightIn(max = 150.dp)) {
                                    items(points) { p ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (p.fromBooking) Icons.Default.Bookmark else Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = if (p.fromBooking) VerdeExito else Terracota,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(p.name, style = MaterialTheme.typography.bodySmall, color = TextoOscuro, modifier = Modifier.weight(1f))
                                            Text("${p.lat},${p.lng}", style = MaterialTheme.typography.bodySmall, color = TextoClaro)
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

    if (showAddPointDialog) {
        var latText by remember { mutableStateOf("") }
        var lngText by remember { mutableStateOf("") }
        var nameText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPointDialog = false },
            title = { Text("Agregar Punto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = latText, onValueChange = { latText = it }, label = { Text("Latitud") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(value = lngText, onValueChange = { lngText = it }, label = { Text("Longitud") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lat = latText.toDoubleOrNull()
                        val lng = lngText.toDoubleOrNull()
                        if (lat != null && lng != null) {
                            addPoint(lat, lng, nameText.ifEmpty { "Punto ${points.size + 1}" })
                        }
                        showAddPointDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                ) { Text("Agregar") }
            },
            dismissButton = { TextButton(onClick = { showAddPointDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showAddBookingDialog) {
        AlertDialog(
            onDismissRequest = { showAddBookingDialog = false },
            title = { Text("Agregar Reserva") },
            text = {
                LazyColumn {
                    items(userBookings.filter { it.id !in bookingIds }) { booking ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { addBookingToItinerary(booking); showAddBookingDialog = false },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Superficie)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.BookOnline, null, tint = TealProfundo)
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Reserva #${booking.id}", fontWeight = FontWeight.Medium, color = TextoOscuro)
                                    Text(booking.date.take(10), style = MaterialTheme.typography.bodySmall, color = TextoClaro)
                                }
                                Text("\$${booking.total}", fontWeight = FontWeight.Bold, color = Terracota)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAddBookingDialog = false }) { Text("Cerrar") } }
        )
    }

    if (showDayDialog) {
        AlertDialog(
            onDismissRequest = { onBack() },
            title = { Text("Nuevo Itinerario") },
            text = {
                OutlinedTextField(
                    value = newDayText, onValueChange = { newDayText = it },
                    label = { Text("Número de día") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { createNewItinerary() },
                    enabled = newDayText.toIntOrNull() != null && !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                ) { Text("Crear") }
            },
            dismissButton = { TextButton(onClick = onBack) { Text("Cancelar") } }
        )
    }
}

private fun parseLocation(loc: String?): Pair<Double, Double>? {
    if (loc == null) return null
    val parts = loc.split(",").map { it.trim() }
    if (parts.size == 2) {
        val lat = parts[0].toDoubleOrNull()
        val lng = parts[1].toDoubleOrNull()
        if (lat != null && lng != null) return Pair(lat, lng)
    }
    return null
}

private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = mutableListOf<LatLng>()
    var index = 0
    var lat = 0
    var lng = 0
    while (index < encoded.length) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat
        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or ((b and 0x1f) shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng
        poly.add(LatLng(lat.toDouble() / 1e5, lng.toDouble() / 1e5))
    }
    return poly
}
