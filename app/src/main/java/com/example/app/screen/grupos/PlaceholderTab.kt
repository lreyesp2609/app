package com.example.app.screen.grupos

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app.models.GrupoResponse
import com.example.app.screen.components.AppButton
import com.example.app.viewmodel.GrupoState
import com.example.app.viewmodel.GrupoViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GroupOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.example.app.network.RetrofitClient
import com.example.app.repository.GrupoRepository
import com.example.app.screen.components.AppTextField
import com.example.app.screen.components.rememberAppSnackbarState
import com.example.app.screen.components.showSuccessSnackbar
import com.example.app.viewmodel.GrupoViewModelFactory
import com.example.app.screen.components.AppSnackbarHost
import com.example.app.screen.components.showErrorSnackbar
import com.example.app.viewmodel.NotificationViewModel
import com.example.app.websocket.NotificationWebSocketManager
import kotlinx.coroutines.launch

@Composable
fun CollaborativeGroupsScreen(
    token: String,
    navController: NavController,
    notificationViewModel: NotificationViewModel, // 🔥 Ahora es REQUERIDO, no opcional
    viewModel: GrupoViewModel = viewModel(
        factory = GrupoViewModelFactory(
            GrupoRepository(RetrofitClient.grupoService)
        )
    )
) {
    val grupoState by viewModel.grupoState.collectAsState()
    var showContent by remember { mutableStateOf(false) }
    var showStats by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }

    val unreadCounts by NotificationWebSocketManager.unreadCounts.collectAsState()

    // 🔥 Observar si viene de crear grupo
    val grupoCreado = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("grupo_creado", false)
        ?.collectAsState()

    // 🔥 Observar el mensaje del grupo creado
    val grupoMensaje = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("grupo_mensaje", "")
        ?.collectAsState()

    val volviendoDelChat = navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow("refresh_grupos", false)
        ?.collectAsState()

    LaunchedEffect(Unit, grupoCreado?.value, volviendoDelChat?.value) {
        viewModel.listarGrupos(token)
        delay(200)
        showContent = true
        delay(400)
        showStats = true

        // 🔥 Mostrar notificación si se creó un grupo
        if (grupoCreado?.value == true && !grupoMensaje?.value.isNullOrBlank()) {
            notificationViewModel.showSuccess(
                message = grupoMensaje?.value ?: "Grupo creado exitosamente"
            )

            // Limpiar los flags
            navController.currentBackStackEntry?.savedStateHandle?.apply {
                set("grupo_creado", false)
                set("grupo_mensaje", "")
            }
        }

        if (volviendoDelChat?.value == true) {
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.set("refresh_grupos", false)
        }
    }

    // 🔥 Manejar estados del ViewModel (para unirse a grupo)
    LaunchedEffect(grupoState) {
        when (val state = grupoState) {
            is GrupoState.JoinSuccess -> {
                notificationViewModel.showSuccess(state.message)
            }

            is GrupoState.Error -> {
                notificationViewModel.showError(state.message)
            }

            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 0.dp), // ✨ Cambiado a vertical = 0.dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header mejorado
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(initialOffsetY = { -it })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Grupos",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp) // ✨ Reducido de 32dp a 28dp
                        )
                        Spacer(modifier = Modifier.width(8.dp)) // ✨ Reducido de 12dp a 8dp
                        Text(
                            text = "Mis grupos",
                            fontSize = 22.sp, // ✨ Reducido de 24.sp a 22.sp
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp)) // ✨ Reducido de 16dp a 12dp

                    // Botón principal más destacado
                    AppButton(
                        text = "Crear grupo",
                        icon = Icons.Default.GroupAdd,
                        onClick = { navController.navigate("create_group") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(56.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botón secundario
                    AppButton(
                        text = "Unirse con código",
                        icon = Icons.Default.Login,
                        onClick = { showJoinDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        outlined = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Contenido principal
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 400)) +
                        slideInVertically(initialOffsetY = { it / 2 })
            ) {
                when (val state = grupoState) {
                    is GrupoState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Cargando grupos...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    is GrupoState.ListSuccess -> {
                        if (state.grupos.isEmpty()) {
                            EmptyGroupsMessage()
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(
                                    count = state.grupos.size,
                                    key = { index -> state.grupos[index].id }
                                ) { index ->
                                    val grupo = state.grupos[index]
                                    val mensajesNoLeidos =
                                        unreadCounts[grupo.id] ?: grupo.mensajesNoLeidos

                                    GrupoCard(
                                        grupo = grupo,
                                        mensajesNoLeidos = mensajesNoLeidos,
                                        onClick = {
                                            navController.navigate(
                                                "chat_grupo/${grupo.id}/${Uri.encode(grupo.nombre)}/${grupo.codigoInvitacion}" // ✅ Agregar código
                                            )
                                        }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }

                    is GrupoState.Error -> {
                        EmptyGroupsMessage()
                    }

                    else -> Unit
                }
            }
        }
    }

    if (showJoinDialog) {
        JoinGroupDialog(
            onDismiss = { showJoinDialog = false },
            onJoin = { codigo ->
                when {
                    codigo.isBlank() -> {
                        notificationViewModel.showError("Ingresa un código")
                    }

                    codigo.length != 8 -> {
                        notificationViewModel.showError("El código debe tener 8 caracteres")
                    }

                    else -> {
                        viewModel.unirseAGrupo(token, codigo)
                        showJoinDialog = false
                    }
                }
            }
        )
    }
}
@Composable
fun JoinGroupDialog(
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var codigoInvitacion by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Login,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Unirse a un grupo",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ingresa el código de invitación del grupo",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                AppTextField(
                    value = codigoInvitacion,
                    onValueChange = {
                        codigoInvitacion = it.uppercase().take(8)
                    },
                    label = "Código de invitación",
                    placeholder = "Ej: C6FB334B",
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "El código debe tener 8 caracteres",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Unirse",
                onClick = { onJoin(codigoInvitacion) },
                enabled = codigoInvitacion.length == 8,
                modifier = Modifier.width(120.dp)
            )
        },
        dismissButton = {
            AppButton(
                text = "Cancelar",
                onClick = onDismiss,
                outlined = true,
                modifier = Modifier.width(120.dp)
            )
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun GrupoCard(
    grupo: GrupoResponse,
    mensajesNoLeidos: Int = 0,
    onClick: () -> Unit = {}
) {
    var isLongPressing by remember { mutableStateOf(false) }
    var showCopiedMessage by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Animación de escala cuando se mantiene presionado
    val scale by animateFloatAsState(
        targetValue = if (isLongPressing) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    LaunchedEffect(showCopiedMessage) {
        if (showCopiedMessage) {
            delay(2000)
            showCopiedMessage = false
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        isLongPressing = true

                        // Copiar al portapapeles
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(grupo.codigoInvitacion))

                        // Mostrar mensaje
                        showCopiedMessage = true

                        // Feedback háptico (opcional, requiere permisos)
                        // context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator

                        kotlinx.coroutines.GlobalScope.launch {
                            delay(150)
                            isLongPressing = false
                        }
                    }
                )
            },
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box {
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

                            // Badge de notificaciones
                            if (mensajesNoLeidos > 0) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier
                                        .size(20.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 4.dp, y = (-4).dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Text(
                                            text = if (mensajesNoLeidos > 99) "99+" else mensajesNoLeidos.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onError
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = grupo.nombre,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!grupo.descripcion.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = grupo.descripcion,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }
            }

            // Mensaje flotante de "Código copiado"
            androidx.compose.animation.AnimatedVisibility(
                visible = showCopiedMessage,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { -it / 2 }),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.inverseSurface,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Código copiado",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun EmptyGroupsMessage() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // Ícono animado
        var iconScale by remember { mutableStateOf(0f) }
        LaunchedEffect(Unit) {
            iconScale = 1f
        }

        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(
                    animateFloatAsState(
                        targetValue = iconScale,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ), label = ""
                    ).value
                )
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GroupOff,
                contentDescription = "Sin grupos",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "No tienes grupos",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Crea un grupo para compartir con personas de confianza",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card informativo (NO CLICKEABLE)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header informativo
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "¿Qué puedes hacer?",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }

                // Características
                GroupFeatureRow(
                    icon = Icons.Default.GroupAdd,
                    title = "Crea grupos de confianza",
                    description = "Comparte con amigos, familia o compañeros"
                )

                Spacer(modifier = Modifier.height(16.dp))

                GroupFeatureRow(
                    icon = Icons.Default.Chat,
                    title = "Chatea en tiempo real",
                    description = "Comunícate al instante con tus contactos"
                )

                Spacer(modifier = Modifier.height(16.dp))

                GroupFeatureRow(
                    icon = Icons.Default.Share,
                    title = "Invita con códigos",
                    description = "Comparte códigos de forma segura"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun GroupFeatureRow(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                lineHeight = 16.sp
            )
        }
    }
}