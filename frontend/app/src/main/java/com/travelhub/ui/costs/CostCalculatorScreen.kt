package com.travelhub.ui.costs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.*
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CostCalculatorScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var bookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var selectedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isLoading by remember { mutableStateOf(true) }
    var isCalculating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var costResult by remember { mutableStateOf<CostResponse?>(null) }

    val token = TokenManager.getToken()

    fun loadBookings() {
        if (token == null) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.getBookings(token)
                if (response.isSuccessful) {
                    bookings = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar reservas: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun calculateCosts() {
        if (token == null || selectedIds.isEmpty()) return
        scope.launch {
            isCalculating = true
            errorMessage = null
            try {
                val response = ApiClient.api.calculateCosts(
                    token = token,
                    request = CostCalculateRequest(booking_ids = selectedIds.toList())
                )
                if (response.isSuccessful) {
                    costResult = response.body()
                } else {
                    errorMessage = "Error al calcular costos: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.localizedMessage}"
            } finally {
                isCalculating = false
            }
        }
    }

    LaunchedEffect(Unit) {
        loadBookings()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calculadora de Costos") },
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
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Terracota)
                }
            }
            errorMessage != null && bookings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage ?: "", color = Error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { loadBookings() },
                            colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            bookings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tienes reservas para calcular costos",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextoClaro
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(CremaClaro)
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "Selecciona las reservas para calcular",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextoOscuro,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(bookings) { booking ->
                            val isSelected = booking.id in selectedIds
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) TerracotaLight.copy(alpha = 0.15f) else Superficie
                                ),
                                onClick = {
                                    selectedIds = if (isSelected) {
                                        selectedIds - booking.id
                                    } else {
                                        selectedIds + booking.id
                                    }
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            selectedIds = if (isSelected) {
                                                selectedIds - booking.id
                                            } else {
                                                selectedIds + booking.id
                                            }
                                        },
                                        colors = CheckboxDefaults.colors(checkedColor = Terracota)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Reserva #${booking.id}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = TextoOscuro
                                        )
                                        Text(
                                            text = "${booking.date} - $${String.format("%.2f", booking.total)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextoClaro
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (costResult != null) {
                        CostResultCard(costResult = costResult!!)
                    }

                    Button(
                        onClick = { calculateCosts() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(56.dp),
                        enabled = !isCalculating && selectedIds.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealProfundo),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isCalculating) {
                            CircularProgressIndicator(
                                color = CremaCalido,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Calcular Costo",
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

@Composable
private fun CostResultCard(costResult: CostResponse) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = Superficie)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Resultado",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextoOscuro
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Text(
                    text = "$${String.format("%.2f", costResult.total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Terracota
                )
            }

            if (costResult.breakdown.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = TextoClaro.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Desglose por categor\u00eda",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoOscuro
                )
                Spacer(modifier = Modifier.height(12.dp))

                costResult.breakdown.forEach { (category, amount) ->
                    val categoryColor = when (category.lowercase()) {
                        "guia" -> TealProfundo
                        "hotel" -> DoradoOscuro
                        else -> Terracota
                    }
                    val fraction = if (costResult.total > 0) amount / costResult.total else 0.0

                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = category.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoClaro
                            )
                            Text(
                                text = "$${String.format("%.2f", amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextoOscuro
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { fraction.toFloat() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = categoryColor,
                            trackColor = categoryColor.copy(alpha = 0.15f),
                        )
                    }
                }
            }

            if (costResult.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider(color = TextoClaro.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Detalles",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextoOscuro
                )
                Spacer(modifier = Modifier.height(8.dp))

                costResult.details.forEach { detail ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = CremaCalido
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = detail.service_name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = TextoOscuro
                                )
                                Text(
                                    text = "${detail.category.replaceFirstChar { it.uppercase()}} - Reserva #${detail.booking_id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextoClaro
                                )
                            }
                            Text(
                                text = "$${String.format("%.2f", detail.price)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Terracota
                            )
                        }
                    }
                }
            }
        }
    }
}
