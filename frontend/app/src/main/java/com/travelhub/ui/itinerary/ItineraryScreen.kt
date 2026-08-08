package com.travelhub.ui.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    onBack: () -> Unit,
    onOpenBuilder: (Int) -> Unit,
    itineraryViewModel: ItineraryViewModel = viewModel()
) {
    val state by itineraryViewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Itinerarios") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Terracota,
                    titleContentColor = CremaCalido,
                    navigationIconContentColor = CremaCalido
                )
            )
        },
        floatingActionButton = {
            if (!state.showCreateForm) FloatingActionButton(
                onClick = { itineraryViewModel.showCreateForm(true) },
                containerColor = Terracota,
                contentColor = CremaCalido
            ) { Icon(Icons.Default.Add, "Crear itinerario") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(CremaClaro)) {
            state.errorMessage?.let {
                Text(it, color = Error, modifier = Modifier.fillMaxWidth().padding(16.dp))
            }
            if (state.showCreateForm) {
                Card(Modifier.fillMaxWidth().padding(16.dp), colors = CardDefaults.cardColors(containerColor = Superficie)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Nuevo itinerario", fontWeight = FontWeight.Bold, color = TextoOscuro)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = state.newDay,
                            onValueChange = itineraryViewModel::updateDay,
                            label = { Text("Número de día") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton({ itineraryViewModel.showCreateForm(false) }, Modifier.weight(1f)) { Text("Cancelar") }
                            Button(
                                itineraryViewModel::create,
                                Modifier.weight(1f),
                                enabled = !state.isSubmitting && state.newDay.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                            ) {
                                if (state.isSubmitting) CircularProgressIndicator(Modifier.size(18.dp), color = CremaCalido, strokeWidth = 2.dp)
                                else Text("Crear")
                            }
                        }
                    }
                }
            }
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Terracota)
                }
                state.itineraries.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tienes itinerarios aún", color = TextoClaro)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.itineraries, key = { it.id }) { itinerary ->
                        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Superficie)) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Día ${itinerary.day}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextoOscuro)
                                    Spacer(Modifier.weight(1f))
                                    IconButton({ itineraryViewModel.delete(itinerary.id) }) {
                                        if (state.deletingId == itinerary.id) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                        else Icon(Icons.Default.Delete, "Eliminar", tint = Error)
                                    }
                                }
                                val points = itinerary.route_data?.get("points") as? List<*>
                                val bookings = itinerary.route_data?.get("booking_ids") as? List<*>
                                Text("${points?.size ?: 0} puntos · ${bookings?.size ?: 0} reservas", color = TextoClaro)
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { onOpenBuilder(itinerary.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Terracota)
                                ) {
                                    Icon(Icons.Default.Map, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Construir ruta")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
