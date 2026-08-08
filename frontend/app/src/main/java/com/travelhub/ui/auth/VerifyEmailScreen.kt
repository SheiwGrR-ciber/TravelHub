package com.travelhub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhub.ui.theme.CremaCalido
import com.travelhub.ui.theme.Error
import com.travelhub.ui.theme.TealProfundo
import com.travelhub.ui.theme.Terracota
import com.travelhub.ui.theme.TextoClaro
import com.travelhub.ui.theme.VerdeExito

@Composable
fun VerifyEmailScreen(
    email: String,
    initialCode: String = "",
    onVerificationSuccess: () -> Unit,
    onBack: () -> Unit,
    verifyEmailViewModel: VerifyEmailViewModel = viewModel()
) {
    val uiState by verifyEmailViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(initialCode) {
        if (initialCode.isNotBlank() && uiState.code.isBlank()) {
            verifyEmailViewModel.updateCode(initialCode)
        }
    }
    LaunchedEffect(uiState.verificationCompleted) {
        if (uiState.verificationCompleted) {
            verifyEmailViewModel.consumeVerificationCompleted()
            onVerificationSuccess()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Terracota.copy(alpha = 0.15f), CremaCalido, CremaCalido))
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(80.dp))
            Icon(Icons.Default.Email, null, tint = Terracota, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(16.dp))
            Text("Verifica tu correo", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Terracota)
            Text(
                "Enviamos un código a\n$email",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoClaro,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Spacer(Modifier.height(32.dp))
            OutlinedTextField(
                value = uiState.code,
                onValueChange = verifyEmailViewModel::updateCode,
                label = { Text("Código de 6 dígitos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracota,
                    focusedLabelColor = Terracota,
                    cursorColor = Terracota
                )
            )
            Spacer(Modifier.height(8.dp))
            uiState.errorMessage?.let {
                Text(it, color = Error, style = MaterialTheme.typography.bodySmall)
            }
            uiState.successMessage?.let {
                Text(it, color = VerdeExito, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { verifyEmailViewModel.verify(email) },
                enabled = !uiState.isLoading && uiState.code.length == 6,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracota)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(color = CremaCalido, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Verificar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { verifyEmailViewModel.resend(email) }, enabled = !uiState.isLoading) {
                Text("Reenviar código", color = TealProfundo, fontWeight = FontWeight.Medium)
            }
        }
    }
}
