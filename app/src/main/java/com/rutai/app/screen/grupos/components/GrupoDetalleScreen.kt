package com.rutai.app.screen.grupos.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.rutai.app.R
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.rutai.app.network.RetrofitClient
import com.rutai.app.repository.GrupoRepository
import com.rutai.app.screen.components.AppBackButton
import com.rutai.app.utils.SessionManager
import com.rutai.app.viewmodel.GrupoViewModel
import com.rutai.app.viewmodel.GrupoViewModelFactory
import com.rutai.app.viewmodel.IntegrantesViewModel
import com.rutai.app.viewmodel.IntegrantesViewModelFactory

@Composable
fun GrupoDetalleScreen(
    grupoId: Int,
    grupoNombre: String,
    codigoInvitacion: String,
    navController: NavController
) {
    val context = LocalContext.current
    val grupoRepository = GrupoRepository(RetrofitClient.grupoService)
    var showInviteDialog by remember { mutableStateOf(false) }

    val grupoViewModel: GrupoViewModel = viewModel(
        factory = GrupoViewModelFactory(context, grupoRepository)
    )

    val viewModel: IntegrantesViewModel = viewModel(
        factory = IntegrantesViewModelFactory(context)
    )

    val integrantes by viewModel.integrantes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val sessionManager = SessionManager.getInstance(context)
    val currentUserId = sessionManager.getUser()?.id ?: 0
    val token = sessionManager.getAccessToken() ?: ""

    val isCreator = integrantes.firstOrNull { it.usuario_id == currentUserId }?.es_creador ?: false

    val mensajeSalida by grupoViewModel.mensajeSalida.collectAsState()
    val mensajeEliminacion by grupoViewModel.mensajeEliminacion.collectAsState() // ✅ Agregar

    // ✅ Agregar estado para el diálogo de confirmación
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showInviteDialog) {
        InvitacionDialog(
            grupoNombre = grupoNombre,
            codigoInvitacion = codigoInvitacion, // ✅ Ahora usa el código real
            onDismiss = { showInviteDialog = false }
        )
    }

    LaunchedEffect(grupoId) {
        viewModel.cargarIntegrantes(grupoId)
    }

    LaunchedEffect(mensajeSalida) {
        mensajeSalida?.let { mensaje ->
            Toast.makeText(context, R.string.group_exit_success, Toast.LENGTH_SHORT).show()
            grupoViewModel.resetMensajeSalida()
            navController.navigate("home?tab=3") {
                popUpTo("home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    // ✅ Agregar LaunchedEffect para eliminar grupo
    LaunchedEffect(mensajeEliminacion) {
        mensajeEliminacion?.let { mensaje ->
            Toast.makeText(context, R.string.group_deleted_success, Toast.LENGTH_SHORT).show()
            grupoViewModel.resetMensajeEliminacion()
            navController.navigate("home?tab=3") {
                popUpTo("home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    // ✅ Agregar el diálogo de confirmación
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.delete_group_dialog_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(stringResource(R.string.delete_group_dialog_message, grupoNombre))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        grupoViewModel.eliminarGrupo(token, grupoId)
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            GrupoDetalleTopBar(
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header con foto del grupo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(120.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Group,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(30.dp)
                    )
                }
            }

            // Nombre del grupo
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = grupoNombre,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.group_id_label, grupoId),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Opciones del grupo
            GrupoOpcion(
                icon = Icons.Default.People,
                titulo = stringResource(R.string.group_option_participants),
                subtitulo = stringResource(R.string.group_option_participants_sub),
                onClick = {
                    navController.navigate("participantes/$grupoId/$grupoNombre")
                }
            )

            GrupoOpcion(
                icon = Icons.Default.PersonAdd,
                titulo = stringResource(R.string.group_option_invite),
                subtitulo = stringResource(R.string.group_option_invite_sub),
                onClick = { showInviteDialog = true }
            )

            /* GrupoOpcion(
                icon = Icons.Default.Notifications,
                titulo = "Notificaciones",
                subtitulo = "Configurar alertas del grupo",
                onClick = { /* TODO: Configurar notificaciones */ }
            ) */

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Mostrar loading mientras carga la info del grupo
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else {
                if (isCreator) {
                    GrupoOpcion(
                        icon = Icons.Default.Delete,
                        titulo = stringResource(R.string.group_option_delete),
                        subtitulo = stringResource(R.string.group_option_delete_sub),
                        onClick = { showDeleteDialog = true }, // ✅ Mostrar diálogo
                        isDestructive = true
                    )
                } else {
                    GrupoOpcion(
                        icon = Icons.Default.ExitToApp,
                        titulo = stringResource(R.string.group_option_exit),
                        subtitulo = stringResource(R.string.group_option_exit_sub),
                        onClick = {
                            grupoViewModel.salirDelGrupo(token, grupoId)
                        },
                        isDestructive = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrupoDetalleTopBar(
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.group_info_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
        },
        navigationIcon = {
            AppBackButton(
                navController = rememberNavController(), // Se ignora si usas onClick personalizado
                onClick = onBackClick, // Usa el callback que ya tienes
                modifier = Modifier.padding(start = 8.dp),
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                iconColor = MaterialTheme.colorScheme.primary
            )
        },

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
fun GrupoOpcion(
    icon: ImageVector,
    titulo: String,
    subtitulo: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDestructive)
                    MaterialTheme.colorScheme.errorContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isDestructive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = titulo,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitulo,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}