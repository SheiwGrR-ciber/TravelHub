package com.travelhub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.VerifyRequest
import com.travelhub.data.model.ResendRequest
import com.travelhub.ui.theme.*
import com.travelhub.util.TokenManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerifyEmailScreen(
    email: String,
    onVerificationSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var verificationCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    fun verify() {
        if (verificationCode.length != 6) {
            errorMessage = "Ingresa el código de 6 dígitos"
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.verifyEmail(VerifyRequest(email, verificationCode))
                if (response.isSuccessful) {
                    val body = response.body()
                    val token = body?.get("access_token") as? String
                    if (token != null) {
                        TokenManager.saveToken(token)
                        TokenManager.saveUser(0, email, "turista")
                    }
                    onVerificationSuccess()
                } else {
                    errorMessage = "Código incorrecto o expirado"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    fun resendCode() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.api.resendVerificationCode(ResendRequest(email))
                if (response.isSuccessful) {
                    successMessage = "Código reenviado a tu correo"
                } else {
                    errorMessage = "Error al reenviar código"
                }
            } catch (e: Exception) {
                errorMessage = "Error de conexión: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Terracota.copy(alpha = 0.15f), CremaCalido, CremaCalido)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            Icon(
                Icons.Default.Email,
                contentDescription = null,
                tint = Terracota,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Verifica tu correo",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Terracota,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Enviamos un código a\n$email",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoClaro,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = verificationCode,
                onValueChange = { newVal ->
                    if (newVal.length <= 6) verificationCode = newVal.filter(Char::isDigit)
                },
                label = { Text("Código de 6 dígitos") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Terracota,
                    focusedLabelColor = Terracota,
                    cursorColor = Terracota
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (errorMessage != null) {
                Text(text = errorMessage!!, color = Error, style = MaterialTheme.typography.bodySmall)
            }
            if (successMessage != null) {
                Text(text = successMessage!!, color = VerdeExito, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { verify() },
                enabled = !isLoading && verificationCode.length == 6,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Terracota),
                shape = MaterialTheme.shapes.medium
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = CremaCalido, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text("Verificar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = { resendCode() }, enabled = !isLoading) {
                Text("Reenviar código", color = TealProfundo, fontWeight = FontWeight.Medium)
            }
        }
    }
}
