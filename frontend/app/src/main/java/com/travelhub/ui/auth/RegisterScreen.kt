package com.travelhub.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.UserCreate
import com.travelhub.ui.theme.*
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onRegisterSuccess: (email: String, code: String) -> Unit,
    onGoToLogin: () -> Unit,
    registerViewModel: RegisterViewModel = viewModel()
) {
    val focusManager = LocalFocusManager.current
    val uiState by registerViewModel.uiState.collectAsStateWithLifecycle()

    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.registrationCompleted) {
        if (uiState.registrationCompleted) {
            val registeredEmail = uiState.email
            registerViewModel.consumeRegistrationCompleted()
            onRegisterSuccess(registeredEmail, "")
        }
    }

    val roles = listOf(
        "turista" to "Turista",
        "prestador" to "Prestador"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Terracota.copy(alpha = 0.15f),
                        CremaCalido,
                        CremaCalido
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "TravelHub",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Terracota,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Crear una cuenta",
                fontSize = 16.sp,
                color = TextoClaro,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Superficie),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Registro",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextoOscuro,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedTextField(
                        value = uiState.name,
                        onValueChange = registerViewModel::updateName,
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracota,
                            focusedLabelColor = Terracota,
                            cursorColor = Terracota
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.email,
                        onValueChange = registerViewModel::updateEmail,
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracota,
                            focusedLabelColor = Terracota,
                            cursorColor = Terracota
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = registerViewModel::updatePassword,
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracota,
                            focusedLabelColor = Terracota,
                            cursorColor = Terracota
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uiState.confirmPassword,
                        onValueChange = registerViewModel::updateConfirmPassword,
                        label = { Text("Confirmar contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracota,
                            focusedLabelColor = Terracota,
                            cursorColor = Terracota
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tipo de cuenta",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextoOscuro,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        roles.forEach { (value, label) ->
                            val isSelected = uiState.selectedRole == value
                            val icon: ImageVector = if (value == "turista") Icons.Default.Person else Icons.Default.Business
                            FilterChip(
                                selected = isSelected,
                                onClick = { registerViewModel.selectRole(value) },
                                label = { Text(label) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Terracota.copy(alpha = 0.15f),
                                    selectedLabelColor = Terracota
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (uiState.errorMessage != null) {
                        Text(
                            text = uiState.errorMessage.orEmpty(),
                            color = Error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            registerViewModel.register()
                        },
                        enabled = uiState.canSubmit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Terracota,
                            disabledContainerColor = Terracota.copy(alpha = 0.5f)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CremaCalido,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Crear cuenta",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onGoToLogin) {
                Text(
                    text = "¿Ya tienes cuenta? Inicia sesión",
                    color = TealProfundo,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private suspend fun performRegister(
    name: String,
    email: String,
    password: String,
    confirmPassword: String,
    role: String,
    onRegisterSuccess: (String, String) -> Unit,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit
) {
    setLoading(true)
    setError(null)
    try {
        if (password != confirmPassword) {
            setError("Las contraseñas no coinciden")
            setLoading(false)
            return
        }
        if (password.length < 6) {
            setError("La contraseña debe tener al menos 6 caracteres")
            setLoading(false)
            return
        }
        val response = ApiClient.api.register(UserCreate(name, email, password, role))
        if (response.isSuccessful) {
            onRegisterSuccess(email, "")
        } else {
            val code = response.code()
            val msg = when (code) {
                409 -> "El correo ya está registrado"
                422 -> "Datos inválidos"
                else -> "Error del servidor ($code)"
            }
            setError(msg)
        }
    } catch (e: Exception) {
        setError("Error de conexión: ${e.localizedMessage ?: "Intente nuevamente"}")
    } finally {
        setLoading(false)
    }
}
