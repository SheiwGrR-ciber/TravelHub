package com.travelhub.ui.provider

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.ServiceCreate
import com.travelhub.data.model.ServiceUpdate
import com.travelhub.ui.theme.CremaCalido
import com.travelhub.ui.theme.Error
import com.travelhub.ui.theme.MarronOscuro
import com.travelhub.ui.theme.TealProfundo
import com.travelhub.ui.theme.VerdeExito
import com.travelhub.ui.theme.Terracota
import com.travelhub.ui.theme.TextoClaro
import com.travelhub.ui.theme.TextoOscuro
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageServiceScreen(
    serviceId: Int,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val token = TokenManager.getToken()
    val isEditMode = serviceId > 0

    var type by remember { mutableStateOf("guia") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var available by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(serviceId) {
        if (isEditMode) {
            try {
                val resp = ApiClient.api.getService(serviceId)
                if (resp.isSuccessful) {
                    val s = resp.body()
                    if (s != null) {
                        type = s.type
                        name = s.name
                        description = s.description ?: ""
                        price = s.price.toString()
                        location = s.location ?: ""
                        available = s.available
                    }
                } else {
                    errorMessage = "Error al cargar el servicio"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexi\u00f3n: ${e.message}"
            } finally {
                isLoading = false
            }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "Editar Servicio" else "Crear Servicio",
                        fontWeight = FontWeight.SemiBold
                    )
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Terracota
                )
                return@Column
            }

            Spacer(Modifier.height(16.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf("guia", "hotel").forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                type = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripci\u00f3n") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = price,
                onValueChange = { newVal ->
                    if (newVal.isEmpty() || newVal.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        price = newVal
                    }
                },
                label = { Text("Precio") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                prefix = { Text("\$") }
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Ubicaci\u00f3n") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            if (isEditMode) {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Disponible",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextoOscuro
                    )
                    Switch(
                        checked = available,
                        onCheckedChange = { available = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = CremaCalido,
                            checkedTrackColor = VerdeExito
                        )
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            errorMessage?.let {
                Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    val priceVal = price.toDoubleOrNull()
                    if (name.isBlank() || description.isBlank() || location.isBlank() || priceVal == null || priceVal <= 0) {
                        errorMessage = "Completa todos los campos requeridos"
                        return@Button
                    }
                    if (token == null) return@Button
                    isSaving = true
                    errorMessage = null
                    scope.launch {
                        try {
                            val success = if (isEditMode) {
                                val update = ServiceUpdate(
                                    type = type,
                                    name = name,
                                    description = description.ifBlank { null },
                                    price = priceVal,
                                    location = location.ifBlank { null },
                                    available = available
                                )
                                ApiClient.api.updateService(token, serviceId, update).isSuccessful
                            } else {
                                val create = ServiceCreate(
                                    type = type,
                                    name = name,
                                    description = description,
                                    price = priceVal,
                                    location = location
                                )
                                val response = ApiClient.api.createService(token, create)
                                if (!response.isSuccessful) {
                                    errorMessage = "Error del servidor: ${response.code()} - ${response.errorBody()?.string()}"
                                }
                                response.isSuccessful
                            }
                            if (success) {
                                onSaved()
                            } else {
                                errorMessage = errorMessage ?: "Error al guardar el servicio"
                            }
                        } catch (e: Exception) {
                            errorMessage = "Error de conexión: ${e.message}"
                        } finally {
                            isSaving = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color = CremaCalido,
                        modifier = Modifier.height(20.dp)
                    )
                } else {
                    Text(
                        if (isEditMode) "Guardar Cambios" else "Crear Servicio",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancelar", color = TextoClaro)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
