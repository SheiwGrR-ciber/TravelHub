package com.travelhub.ui.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.travelhub.data.model.ReviewResponse
import com.travelhub.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(serviceId: Int, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var reviews by remember { mutableStateOf<List<ReviewResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadReviews() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.getServiceReviews(serviceId)
                if (response.isSuccessful) {
                    reviews = response.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar reseñas: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadReviews() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reseñas") },
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
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Terracota)
                }
            }
            errorMessage != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = errorMessage ?: "", color = Error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { loadReviews() }, colors = ButtonDefaults.buttonColors(containerColor = Terracota)) {
                            Text("Reintentar")
                        }
                    }
                }
            }
            reviews.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No hay reseñas para este servicio", style = MaterialTheme.typography.bodyLarge, color = TextoClaro)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).background(CremaClaro),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(reviews) { review ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Superficie)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    repeat(5) { i ->
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = if (i < review.rating) DoradoOscuro else TextoClaro.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (review.comment.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(text = review.comment, style = MaterialTheme.typography.bodyMedium, color = TextoOscuro)
                                }
                                if (review.created_at != null) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(text = review.created_at, style = MaterialTheme.typography.bodySmall, color = TextoClaro)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
