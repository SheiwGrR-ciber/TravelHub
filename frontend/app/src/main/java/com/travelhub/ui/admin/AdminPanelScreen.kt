package com.travelhub.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.PrestadorPending
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val token = TokenManager.getToken()
    var pendings by remember { mutableStateOf<List<PrestadorPending>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var actionMsg by remember { mutableStateOf<String?>(null) }

    fun load() {
        if (token == null) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val resp = ApiClient.api.getPendingPrestadores(token)
                if (resp.isSuccessful) {
                    pendings = resp.body() ?: emptyList()
                } else {
                    errorMessage = "Error al cargar: ${resp.code()}"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun approve(userId: Int) {
        if (token == null) return
        scope.launch {
            try {
                val resp = ApiClient.api.approvePrestador(token, userId)
                if (resp.isSuccessful) {
                    actionMsg = "Prestador aprobado"
                    load()
                } else {
                    actionMsg = "Error al aprobar"
                }
            } catch (e: Exception) {
                actionMsg = "Error: ${e.localizedMessage}"
            }
        }
    }

    fun reject(userId: Int) {
        if (token == null) return
        scope.launch {
            try {
                val resp = ApiClient.api.rejectPrestador(token, userId)
                if (resp.isSuccessful) {
                    actionMsg = "Prestador rechazado"
                    load()
                } else {
                    actionMsg = "Error al rechazar"
                }
            } catch (e: Exception) {
                actionMsg = "Error: ${e.localizedMessage}"
            }
        }
    }

    LaunchedEffect(Unit) { load() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Administración") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Terracota, titleContentColor = CremaCalido, navigationIconContentColor = CremaCalido
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(CremaCalido)) {
            if (actionMsg != null) {
                Text(text = actionMsg!!, color = VerdeExito, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
            }
            when {
                isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Terracota) }
                errorMessage != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage!!, color = Error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { load() }, colors = ButtonDefaults.buttonColors(containerColor = Terracota)) { Text("Reintentar") }
                    }
                }
                pendings.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay prestadores pendientes de aprobación", style = MaterialTheme.typography.bodyLarge, color = TextoClaro)
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(pendings) { p ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Superficie)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Terracota, modifier = Modifier.size(40.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = TextoOscuro)
                                    Text(p.email, style = MaterialTheme.typography.bodySmall, color = TextoClaro)
                                }
                                Row {
                                    IconButton(onClick = { approve(p.id) }) {
                                        Icon(Icons.Default.CheckCircle, "Aprobar", tint = VerdeExito)
                                    }
                                    IconButton(onClick = { reject(p.id) }) {
                                        Icon(Icons.Default.Cancel, "Rechazar", tint = Error)
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
