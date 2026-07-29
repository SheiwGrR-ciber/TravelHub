package com.travelhub.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.BookingResponse
import com.travelhub.data.model.ServiceResponse
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

data class ActionCard(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCatalog: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToItinerary: () -> Unit,
    onNavigateToCostCalc: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToProviderPanel: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val userRole = TokenManager.getUserRole() ?: "turista"
    val userEmail = TokenManager.getUserEmail() ?: "Usuario"

    var recentBookings by remember { mutableStateOf<List<BookingResponse>>(emptyList()) }
    var recentServices by remember { mutableStateOf<List<ServiceResponse>>(emptyList()) }
    var isLoadingData by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            val token = TokenManager.getToken()
            if (token != null) {
                if (userRole == "prestador") {
                    val servicesResp = ApiClient.api.getServices()
                    if (servicesResp.isSuccessful) {
                        recentServices = (servicesResp.body() ?: emptyList()).take(5)
                    }
                } else {
                    val bookingsResp = ApiClient.api.getBookings(token)
                    if (bookingsResp.isSuccessful) {
                        recentBookings = (bookingsResp.body() ?: emptyList()).take(5)
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            isLoadingData = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "TravelHub",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = "Hola, $userEmail",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Terracota,
                    titleContentColor = CremaCalido,
                    actionIconContentColor = CremaCalido
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CremaCalido),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = if (userRole == "prestador") "Panel de Proveedor" else "Descubre viajes increíbles",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoOscuro,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (userRole == "prestador") {
                item {
                    ProviderQuickActions(
                        onNavigateToProviderPanel = onNavigateToProviderPanel,
                        onNavigateToBookings = onNavigateToBookings,
                        onNavigateToCatalog = onNavigateToCatalog
                    )
                }
            } else {
                item {
                    TouristQuickActions(
                        onNavigateToCatalog = onNavigateToCatalog,
                        onNavigateToBookings = onNavigateToBookings,
                        onNavigateToItinerary = onNavigateToItinerary,
                        onNavigateToCostCalc = onNavigateToCostCalc
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (userRole == "prestador") "Tus servicios" else "Actividad reciente",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextoOscuro,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (isLoadingData) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Terracota)
                    }
                }
            } else if (userRole == "prestador") {
                if (recentServices.isEmpty()) {
                    item {
                        EmptyStateCard("Aún no tienes servicios creados")
                    }
                } else {
                    items(recentServices) { service ->
                        ServiceCard(service = service)
                    }
                }
            } else {
                if (recentBookings.isEmpty()) {
                    item {
                        EmptyStateCard("No tienes reservas activas")
                    }
                } else {
                    items(recentBookings) { booking ->
                        BookingCard(booking = booking)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MarronOscuro),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar Sesión", fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun TouristQuickActions(
    onNavigateToCatalog: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToItinerary: () -> Unit,
    onNavigateToCostCalc: () -> Unit
) {
    val cards = listOf(
        ActionCard("Buscar Servicios", "Explora alojamientos, tours y más", Icons.Default.Search, onNavigateToCatalog),
        ActionCard("Mis Reservas", "Revisa tus reservaciones activas", Icons.Default.BookOnline, onNavigateToBookings),
        ActionCard("Mi Itinerario", "Planifica tu viaje día a día", Icons.Default.Map, onNavigateToItinerary),
        ActionCard("Calculadora de Costos", "Estima el costo total de tu viaje", Icons.Default.Calculate, onNavigateToCostCalc)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { card ->
                    ActionCardItem(
                        card = card,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCards.size < 2) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ProviderQuickActions(
    onNavigateToProviderPanel: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToCatalog: () -> Unit
) {
    val cards = listOf(
        ActionCard("Gestionar Servicios", "Administra tus servicios activos", Icons.Default.MiscellaneousServices, onNavigateToProviderPanel),
        ActionCard("Ver Reservas", "Reservas de tus servicios", Icons.Default.BookOnline, onNavigateToBookings),
        ActionCard("Catálogo", "Explora servicios disponibles", Icons.Default.Storefront, onNavigateToCatalog)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.take(2).forEach { card ->
                ActionCardItem(
                    card = card,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cards.drop(2).forEach { card ->
                ActionCardItem(
                    card = card,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ActionCardItem(card: ActionCard, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { card.onClick() },
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Terracota.copy(alpha = 0.2f), TealProfundo.copy(alpha = 0.1f))
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = card.icon,
                    contentDescription = null,
                    tint = Terracota,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextoOscuro,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextoClaro,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BookingCard(booking: BookingResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = when (booking.status) {
                            "confirmada" -> VerdeExito.copy(alpha = 0.15f)
                            "pendiente" -> DoradoArena.copy(alpha = 0.3f)
                            else -> TextoClaro.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = when (booking.status) {
                        "confirmada" -> VerdeExito
                        "pendiente" -> DoradoOscuro
                        else -> TextoClaro
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Reserva #${booking.id}",
                    fontWeight = FontWeight.Medium,
                    color = TextoOscuro
                )
                Text(
                    text = "Fecha: ${booking.date} · ${booking.status.replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoClaro
                )
                Text(
                    text = "Total: $${"%.2f".format(booking.total)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TealProfundo,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ServiceCard(service: ServiceResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Terracota.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Terracota,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    fontWeight = FontWeight.Medium,
                    color = TextoOscuro
                )
                Text(
                    text = "${service.type.replaceFirstChar { it.uppercase() }} · ${service.location ?: "Ubicación no especificada"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextoClaro
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$${"%.2f".format(service.price)}",
                    fontWeight = FontWeight.Bold,
                    color = Terracota
                )
                Text(
                    text = "★ ${"%.1f".format(service.rating)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = DoradoOscuro
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Superficie),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoClaro,
                textAlign = TextAlign.Center
            )
        }
    }
}
