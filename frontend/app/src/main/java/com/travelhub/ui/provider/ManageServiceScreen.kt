package com.travelhub.ui.provider

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ManageServiceScreen(
    serviceId: Int,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    manageServiceViewModel: ManageServiceViewModel = viewModel()
) {
    val uiState by manageServiceViewModel.uiState.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(serviceId) { manageServiceViewModel.initialize(serviceId) }
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            manageServiceViewModel.consumeSaved()
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Editar servicio" else "Crear servicio", fontWeight = FontWeight.SemiBold) },
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
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Terracota)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = uiState.type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    listOf("guia", "hotel", "restaurante", "traductor", "transportista").forEach { option ->
                        DropdownMenuItem(text = { Text(option.replaceFirstChar { it.uppercase() }) }, onClick = {
                            manageServiceViewModel.updateType(option)
                            expanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(uiState.name, manageServiceViewModel::updateName, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                uiState.description,
                manageServiceViewModel::updateDescription,
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                uiState.price,
                manageServiceViewModel::updatePrice,
                label = { Text("Precio") },
                prefix = { Text("S/ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                uiState.location,
                manageServiceViewModel::updateLocation,
                label = { Text("Ubicación") },
                modifier = Modifier.fillMaxWidth()
            )
            if (uiState.isEditMode) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Servicio disponible", color = TextoOscuro)
                    Switch(
                        checked = uiState.available,
                        onCheckedChange = manageServiceViewModel::updateAvailable,
                        colors = SwitchDefaults.colors(checkedTrackColor = VerdeExito)
                    )
                }
            }
            uiState.errorMessage?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = manageServiceViewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
            ) {
                if (uiState.isSaving) CircularProgressIndicator(Modifier.size(20.dp), color = CremaCalido, strokeWidth = 2.dp)
                else Text(if (uiState.isEditMode) "Guardar cambios" else "Crear servicio", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancelar") }
            Spacer(Modifier.height(32.dp))
        }
    }
}
