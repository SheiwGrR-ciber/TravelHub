package com.travelhub.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhub.data.api.ApiClient
import com.travelhub.data.model.MessageCreate
import com.travelhub.data.model.MessageResponse
import com.travelhub.ui.theme.CremaCalido
import com.travelhub.ui.theme.Error
import com.travelhub.ui.theme.Terracota
import com.travelhub.ui.theme.TextoClaro
import com.travelhub.ui.theme.TextoOscuro
import com.travelhub.util.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    bookingId: Int,
    serviceName: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val currentUserId = TokenManager.getUserId()
    val token = TokenManager.getToken()

    var messages by remember { mutableStateOf<List<MessageResponse>>(emptyList()) }
    var otherUserId by remember { mutableStateOf<Int?>(null) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadMessages() {
        if (token == null) return
        scope.launch {
            try {
                val response = ApiClient.api.getMessages(token, bookingId)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        messages = body
                        if (otherUserId == null && body.isNotEmpty()) {
                            val first = body.first()
                            otherUserId = if (first.sender_id == currentUserId) first.receiver_id else first.sender_id
                        }
                    }
                    errorMessage = null
                } else {
                    errorMessage = "Error al cargar mensajes"
                }
            } catch (e: Exception) {
                if (messages.isEmpty()) {
                    errorMessage = "Error de conexi\u00f3n: ${e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun determineOtherPartyFromBooking() {
        if (token == null || otherUserId != null) return
        scope.launch {
            try {
                val bookingResp = ApiClient.api.getBooking(token, bookingId)
                if (bookingResp.isSuccessful) {
                    val booking = bookingResp.body() ?: return@launch
                    if (booking.tourist_id == currentUserId) {
                        val serviceResp = ApiClient.api.getService(booking.service_id)
                        if (serviceResp.isSuccessful) {
                            otherUserId = serviceResp.body()?.provider_id
                        }
                    } else {
                        otherUserId = booking.tourist_id
                    }
                }
            } catch (_: Exception) { }
        }
    }

    LaunchedEffect(bookingId) {
        loadMessages()
        while (true) {
            delay(5000)
            loadMessages()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(serviceName, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Terracota,
                    titleContentColor = CremaCalido,
                    navigationIconContentColor = CremaCalido
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Escribe un mensaje...") },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Terracota,
                            cursorColor = Terracota
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    FilledIconButton(
                        onClick = {
                            val text = inputText.trim()
                            val receiver = otherUserId
                            if (text.isNotEmpty() && receiver == null) {
                                determineOtherPartyFromBooking()
                            }
                            if (text.isNotEmpty() && otherUserId != null && !isSending && token != null) {
                                isSending = true
                                scope.launch {
                                    try {
                                        val resp = ApiClient.api.sendMessage(
                                            token,
                                            MessageCreate(otherUserId!!, bookingId, text)
                                        )
                                        if (resp.isSuccessful) {
                                            inputText = ""
                                            loadMessages()
                                        } else {
                                            errorMessage = "Error al enviar mensaje"
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error de conexi\u00f3n: ${e.message}"
                                    } finally {
                                        isSending = false
                                    }
                                }
                            }
                        },
                        enabled = inputText.isNotBlank() && !isSending,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = Terracota,
                            contentColor = CremaCalido
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(CremaCalido)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Terracota
                    )
                }
                errorMessage != null && messages.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(errorMessage!!, color = Error)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { isLoading = true; loadMessages() }) {
                            Text("Reintentar")
                        }
                    }
                }
                messages.isEmpty() -> {
                    Text(
                        "No hay mensajes. Env\u00eda el primero!",
                        modifier = Modifier.align(Alignment.Center),
                        color = TextoClaro,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            val isMine = msg.sender_id == currentUserId
                            val bubbleColor = if (isMine) Terracota else MaterialTheme.colorScheme.surfaceVariant
                            val textColor = if (isMine) CremaCalido else TextoOscuro

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMine) 16.dp else 4.dp,
                                        bottomEnd = if (isMine) 4.dp else 16.dp
                                    ),
                                    color = bubbleColor,
                                    shadowElevation = 2.dp
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                                        Text(
                                            text = msg.content,
                                            color = textColor,
                                            fontSize = 15.sp
                                        )
                                        msg.timestamp?.let { ts ->
                                            val formatted = try {
                                                val cleanTs = ts.replace("Z", "").substringBefore("+").substringBeforeLast(".")
                                                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                                                val date = parser.parse(cleanTs)
                                                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                                                formatter.format(date!!)
                                            } catch (_: Exception) { "" }
                                            if (formatted.isNotEmpty()) {
                                                Text(
                                                    text = formatted,
                                                    color = if (isMine) CremaCalido.copy(alpha = 0.7f) else TextoClaro,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.align(Alignment.End)
                                                )
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
    }
}
