package com.example.app.screen.grupos.components

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.app.screen.components.AppBackButton
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.IntegrantesViewModel
import com.example.app.viewmodel.IntegrantesViewModelFactory

@Composable
fun GrupoDetalleScreen(
    grupoId: Int,
    grupoNombre: String,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: IntegrantesViewModel = viewModel(
        factory = IntegrantesViewModelFactory(context)
    )

    val integrantes by viewModel.integrantes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Obtener el usuario actual
    val sessionManager = SessionManager.getInstance(context)
    val currentUserId = sessionManager.getUser()?.id ?: 0

    // Verificar si el usuario actual es el creador
    val isCreator = integrantes.firstOrNull { it.usuario_id == currentUserId }?.es_creador ?: false

    LaunchedEffect(grupoId) {
        viewModel.cargarIntegrantes(grupoId)
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
                    text = "Grupo #$grupoId",
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
                titulo = "Participantes",
                subtitulo = "Ver todos los miembros del grupo",
                onClick = {
                    navController.navigate("participantes/$grupoId/$grupoNombre")
                }
            )

            GrupoOpcion(
                icon = Icons.Default.Image,
                titulo = "Archivos, enlaces y docs",
                subtitulo = "Ver contenido multimedia",
                onClick = { /* TODO: Ver multimedia */ }
            )

            GrupoOpcion(
                icon = Icons.Default.Notifications,
                titulo = "Notificaciones",
                subtitulo = "Configurar alertas del grupo",
                onClick = { /* TODO: Configurar notificaciones */ }
            )

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
                // Opción de salir o eliminar según sea creador
                if (isCreator) {
                    GrupoOpcion(
                        icon = Icons.Default.Delete,
                        titulo = "Eliminar grupo",
                        subtitulo = "Esta acción no se puede deshacer",
                        onClick = { /* TODO: Eliminar grupo */ },
                        isDestructive = true
                    )
                } else {
                    GrupoOpcion(
                        icon = Icons.Default.ExitToApp,
                        titulo = "Salir del grupo",
                        subtitulo = "Ya no recibirás mensajes de este grupo",
                        onClick = { /* TODO: Salir del grupo */ },
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
                text = "Información del grupo",
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