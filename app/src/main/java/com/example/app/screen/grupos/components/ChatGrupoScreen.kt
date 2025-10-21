package com.example.app.screen.grupos.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ChatGrupoScreen(
    grupoId: Int,
    grupoNombre: String,
    navController: NavController
) {
    var mensajeTexto by remember { mutableStateOf("") }
    var mensajes by remember { mutableStateOf(getMensajesDemoData()) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                grupoNombre = grupoNombre,
                onBackClick = { navController.popBackStack() }
            )
        },
        bottomBar = {
            ChatInputBar(
                mensaje = mensajeTexto,
                onMensajeChange = { mensajeTexto = it },
                onEnviarClick = {
                    if (mensajeTexto.isNotBlank()) {
                        // Agregar mensaje temporalmente (luego lo harás con WebSocket)
                        mensajes = mensajes + MensajeDemo(
                            id = mensajes.size + 1,
                            contenido = mensajeTexto,
                            esMio = true,
                            hora = "Ahora",
                            leido = false
                        )
                        mensajeTexto = ""
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0) // ✅ Dejar que bottomBar maneje los insets
    ) { paddingValues ->
        ChatMessageList(
            mensajes = mensajes,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    grupoNombre: String,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = grupoNombre,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "5 miembros",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver"
                )
            }
        },
        actions = {
            IconButton(onClick = { /* TODO: Más opciones */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Más opciones"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ChatMessageList(
    mensajes: List<MensajeDemo>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Auto-scroll al último mensaje
    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Fondo con patrón sutil
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pattern = 20.dp.toPx()
            for (i in 0..(size.width / pattern).toInt()) {
                for (j in 0..(size.height / pattern).toInt()) {
                    drawCircle(
                        color = Color.Gray.copy(alpha = 0.03f),
                        radius = 1.dp.toPx(),
                        center = Offset(i * pattern, j * pattern)
                    )
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(mensajes.size) { index ->
                MensajeBubble(mensaje = mensajes[index])
            }
        }
    }
}

@Composable
fun MensajeBubble(mensaje: MensajeDemo) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (mensaje.esMio) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mensaje.esMio) 16.dp else 4.dp,
                bottomEnd = if (mensaje.esMio) 4.dp else 16.dp
            ),
            color = if (mensaje.esMio)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 1.dp,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .animateContentSize()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Nombre del remitente (si no es mío)
                if (!mensaje.esMio) {
                    Text(
                        text = mensaje.nombreRemitente ?: "Usuario",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Contenido del mensaje
                Text(
                    text = mensaje.contenido,
                    fontSize = 15.sp,
                    color = if (mensaje.esMio)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Hora y estado de lectura
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mensaje.hora,
                        fontSize = 11.sp,
                        color = if (mensaje.esMio)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (mensaje.esMio) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (mensaje.leido) Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = if (mensaje.leido) "Leído" else "Enviado",
                            tint = if (mensaje.leido)
                                Color(0xFF34B7F1) // Azul WhatsApp
                            else
                                MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputBar(
    mensaje: String,
    onMensajeChange: (String) -> Unit,
    onEnviarClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding() // ✅ Respeta barra de navegación y gestos
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Campo de texto
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { /* TODO: Abrir selector de emoji */ }
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    BasicTextField(
                        value = mensaje,
                        onValueChange = onMensajeChange,
                        modifier = Modifier.weight(1f),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        decorationBox = { innerTextField ->
                            if (mensaje.isEmpty()) {
                                Text(
                                    text = "Escribe un mensaje...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            innerTextField()
                        },
                        maxLines = 5
                    )

                    IconButton(
                        onClick = { /* TODO: Adjuntar archivo */ },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Adjuntar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón enviar
            FloatingActionButton(
                onClick = onEnviarClick,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Enviar",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

// 📦 Data class temporal para demo
data class MensajeDemo(
    val id: Int,
    val contenido: String,
    val esMio: Boolean,
    val hora: String,
    val leido: Boolean,
    val nombreRemitente: String? = null
)

// 🎨 Datos de prueba
fun getMensajesDemoData() = listOf(
    MensajeDemo(1, "Hola a todos! 👋", false, "10:30", true, "Juan"),
    MensajeDemo(2, "Hey! ¿Cómo están?", true, "10:31", true),
    MensajeDemo(3, "Todo bien por aquí, ¿y tú?", false, "10:32", true, "María"),
    MensajeDemo(4, "Excelente! Tengo una idea para el proyecto", true, "10:33", true),
    MensajeDemo(5, "Cuéntanos! 🤔", false, "10:34", false, "Pedro"),
    MensajeDemo(6, "¿Qué tal si nos reunimos mañana para discutirlo?", true, "10:35", false)
)