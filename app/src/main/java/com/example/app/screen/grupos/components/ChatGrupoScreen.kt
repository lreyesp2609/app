package com.example.app.screen.grupos.components

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.app.models.EstadoMensaje
import com.example.app.models.MensajeUI
import com.example.app.screen.components.AppBackButton
import com.example.app.services.MyFirebaseMessagingService
import com.example.app.viewmodel.ChatGrupoViewModel
import com.example.app.viewmodel.ChatGrupoViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun ChatGrupoScreen(
    grupoId: Int,
    grupoNombre: String,
    codigoInvitacion: String,
    navController: NavController
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("recuerdago_prefs", Context.MODE_PRIVATE)

    DisposableEffect(grupoId) {
        prefs.edit().putInt("current_chat_grupo_id", grupoId).apply()
        MyFirebaseMessagingService.clearNotificationHistory(context, grupoId)
        Log.d("ChatGrupo", "✅ Usuario entró al chat $grupoId - historial limpiado")

        onDispose {
            prefs.edit().putInt("current_chat_grupo_id", -1).apply()
            Log.d("ChatGrupo", "🔒 Usuario salió del chat $grupoId")
        }
    }

    val viewModel: ChatGrupoViewModel = viewModel(
        factory = ChatGrupoViewModelFactory(context)
    )

    val mensajes by viewModel.mensajes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    var mensajeTexto by remember { mutableStateOf("") }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(grupoId) {
        viewModel.cargarMensajes(grupoId)
    }

    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.limpiarError()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            ChatTopBar(
                grupoNombre = grupoNombre,
                isConnected = isConnected,
                onBackClick = { navController.popBackStack() },
                onGrupoClick = {
                    navController.navigate("grupo_detalle/$grupoId/$grupoNombre/$codigoInvitacion")
                }
            )
        },
        bottomBar = {
            if (pagerState.currentPage == 0) {
                ChatInputBar(
                    mensaje = mensajeTexto,
                    onMensajeChange = { mensajeTexto = it },
                    onEnviarClick = {
                        if (mensajeTexto.isNotBlank()) {
                            viewModel.enviarMensaje(grupoId, mensajeTexto)
                            mensajeTexto = ""
                        }
                    },
                    onMapClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    enabled = isConnected
                )
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                userScrollEnabled = pagerState.currentPage == 0
            ) { page ->
                when (page) {
                    0 -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isLoading && mensajes.isEmpty()) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else {
                                ChatMessageList(
                                    mensajes = mensajes,
                                    onMensajeVisible = { mensajeId ->
                                        viewModel.marcarComoLeido(grupoId, mensajeId)
                                    }
                                )
                            }
                        }
                    }

                    1 -> {
                        GrupoMapScreen(
                            navController = navController,
                            grupoId = grupoId,
                            onLocationSelected = { lat, lon, address ->
                                viewModel.enviarMensaje(
                                    grupoId,
                                    "📍 Ubicación compartida: $address"
                                )
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            },
                            onBackToChat = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(0)
                                }
                            }
                        )
                    }
                }
            }

            // 🔥 BOTÓN FLOTANTE SOLO EN EL MAPA (lado izquierdo)
            if (pagerState.currentPage == 1) {
                MiniSwipeIndicator(
                    currentPage = pagerState.currentPage,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                )
            }

            // 🔥 PageIndicator ELIMINADO
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(
    grupoNombre: String,
    isConnected: Boolean,
    onBackClick: () -> Unit,
    onGrupoClick: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGrupoClick() }
                    .padding(vertical = 4.dp)
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

                Text(
                    text = grupoNombre,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            AppBackButton(
                navController = rememberNavController(),
                onClick = onBackClick,
                modifier = Modifier.padding(start = 8.dp),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                iconColor = MaterialTheme.colorScheme.primary
            )
        },
        // 🔥 ACTIONS ELIMINADO (3 puntitos)
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun ChatInputBar(
    mensaje: String,
    onMensajeChange: (String) -> Unit,
    onEnviarClick: () -> Unit,
    onMapClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 🗺️ BOTÓN DEL MAPA (izquierda)
            FloatingActionButton(
                onClick = onMapClick,
                modifier = Modifier.size(48.dp),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 4.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = "Abrir mapa",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 💬 CAMPO DE TEXTO (centro)
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
                        enabled = true,
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
                        maxLines = 5,
                        // 🔥 PRIMERA LETRA EN MAYÚSCULA
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (mensaje.isNotBlank()) {
                                    onEnviarClick()
                                }
                            }
                        )
                    )

                    // 🔥 ÍCONO DE DOCUMENTO ELIMINADO
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ✉️ BOTÓN DE ENVIAR (derecha)
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

@Composable
fun ChatMessageList(
    mensajes: List<MensajeUI>,
    onMensajeVisible: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(mensajes.size) {
        if (mensajes.isNotEmpty()) {
            listState.animateScrollToItem(mensajes.size - 1)
        }
    }

    val dotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)

    Box(modifier = modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val pattern = 20.dp.toPx()
            for (i in 0..(size.width / pattern).toInt()) {
                for (j in 0..(size.height / pattern).toInt()) {
                    drawCircle(
                        color = dotColor,
                        radius = 1.dp.toPx(),
                        center = Offset(i * pattern, j * pattern)
                    )
                }
            }
        }

        if (mensajes.isEmpty()) {
            Text(
                text = "No hay mensajes aún.\n¡Sé el primero en escribir! 💬",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var fechaAnterior: String? = null

                mensajes.forEachIndexed { index, mensaje ->
                    val fechaActual = formatearFechaHeader(mensaje.fechaCreacion)

                    if (fechaActual != fechaAnterior) {
                        fechaAnterior = fechaActual
                        item {
                            FechaHeader(fechaActual)
                        }
                    }

                    item {
                        if (!mensaje.esMio && !mensaje.leido) {
                            LaunchedEffect(mensaje.id) {
                                onMensajeVisible(mensaje.id)
                            }
                        }
                        MensajeBubble(mensaje = mensaje)
                    }
                }
            }
        }
    }
}

@Composable
fun MensajeBubble(
    mensaje: MensajeUI,
    onReintentarEnvio: ((String) -> Unit)? = null
) {
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
                MaterialTheme.colorScheme.primary
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
                if (!mensaje.esMio && mensaje.nombreRemitente != null) {
                    Text(
                        text = mensaje.nombreRemitente,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                Text(
                    text = mensaje.contenido,
                    fontSize = 15.sp,
                    color = if (mensaje.esMio)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = mensaje.hora,
                        fontSize = 11.sp,
                        color = if (mensaje.esMio)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    if (mensaje.esMio) {
                        Spacer(modifier = Modifier.width(4.dp))

                        when (mensaje.estado) {
                            EstadoMensaje.ENVIANDO -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                                )
                            }

                            EstadoMensaje.ERROR -> {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error al enviar",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable {
                                            mensaje.tempId?.let { onReintentarEnvio?.invoke(it) }
                                        }
                                )
                            }

                            EstadoMensaje.LEIDO -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Leído por ${mensaje.leidoPor}",
                                    tint = if (mensaje.esMio) Color(0xFF81D4FA) else Color(0xFF34B7F1),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            EstadoMensaje.ENTREGADO -> {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Entregado",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            EstadoMensaje.ENVIADO -> {
                                Icon(
                                    imageVector = Icons.Default.Done,
                                    contentDescription = "Enviado",
                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (mensaje.estado == EstadoMensaje.ERROR) {
            Text(
                text = "No se pudo enviar. Toca para reintentar.",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(top = 4.dp, end = 8.dp)
                    .clickable {
                        mensaje.tempId?.let { onReintentarEnvio?.invoke(it) }
                    }
            )
        }
    }
}

@Composable
fun FechaHeader(texto: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = texto,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}