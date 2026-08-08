package com.travelhub.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onBack: () -> Unit, profileViewModel: ProfileViewModel = viewModel()) {
    val state by profileViewModel.uiState.collectAsStateWithLifecycle()
    var logoutDialog by remember { mutableStateOf(false) }

    if (logoutDialog) AlertDialog(
        onDismissRequest = { logoutDialog = false },
        title = { Text("Cerrar sesion") }, text = { Text("¿Deseas cerrar tu sesion?") },
        confirmButton = { Button(onClick = { TokenManager.logout(); logoutDialog = false; onBack() }) { Text("Cerrar sesion") } },
        dismissButton = { TextButton(onClick = { logoutDialog = false }) { Text("Cancelar") } }
    )

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Mi perfil") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Terracota, titleContentColor = CremaCalido, navigationIconContentColor = CremaCalido)
        )
    }) { padding ->
        if (state.isLoading) Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.padding(48.dp)) }
        else Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Datos personales", style = MaterialTheme.typography.titleLarge, color = TextoOscuro)
            ProfileField("Nombre completo", state.name, profileViewModel::setName)
            ProfileField("Correo electronico", state.email, {}, enabled = false)
            ProfileField("Telefono", state.phone, profileViewModel::setPhone, KeyboardType.Phone)
            ProfileField("Ciudad o ubicacion", state.location, profileViewModel::setLocation)
            ProfileField("Acerca de mi", state.bio, profileViewModel::setBio, minLines = 3)

            AssistChip(onClick = {}, label = { Text(if (state.role == "prestador") "Prestador" else if (state.role == "admin") "Administrador" else "Turista") })

            if (state.role == "prestador") {
                HorizontalDivider()
                Text("Datos del prestador", style = MaterialTheme.typography.titleLarge, color = TextoOscuro)
                ProfileField("Nombre comercial", state.businessName, profileViewModel::setBusinessName)
                Text("Categoria principal", style = MaterialTheme.typography.labelLarge)
                val types = listOf("guia" to "Guia", "hotel" to "Hotel", "restaurante" to "Restaurante", "traductor" to "Traductor", "transportista" to "Transportista")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    types.forEach { (value, label) -> FilterChip(selected = state.providerType == value, onClick = { profileViewModel.setProviderType(value) }, label = { Text(label) }) }
                }
                ProfileField("Anos de experiencia", state.experienceYears, profileViewModel::setExperience, KeyboardType.Number)
                Text(if (state.approved) "Cuenta aprobada por el administrador" else "Cuenta pendiente de aprobacion", color = if (state.approved) VerdeExito else Terracota)
            }

            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.success?.let { Text(it, color = VerdeExito) }

            Button(onClick = profileViewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else Text("Guardar cambios")
            }
            OutlinedButton(onClick = { logoutDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("Cerrar sesion")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProfileField(
    label: String, value: String, onChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text, enabled: Boolean = true, minLines: Int = 1
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), minLines = minLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = minLines == 1
    )
}
