package com.example.app.screen.grupos.components

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PeopleOutline
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.app.models.IntegranteGrupo
import com.example.app.screen.components.AppBackButton
import com.example.app.utils.SessionManager
import com.example.app.viewmodel.IntegrantesViewModel
import com.example.app.viewmodel.IntegrantesViewModelFactory
import java.time.Instant
import kotlin.time.Duration

@Composable
fun ParticipantesScreen(
    grupoId: Int,
    grupoNombre: String,
    navController: NavController
) {
    val context = LocalContext.current
    val viewModel: IntegrantesViewModel = viewModel(
        factory = IntegrantesViewModelFactory(context)
    )

    val integrantes by viewModel.integrantes.collectAsState()
    val totalIntegrantes by viewModel.totalIntegrantes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    // 🆕 Obtener el ID del usuario actual
    val sessionManager = SessionManager.getInstance(context)
    val currentUserId = sessionManager.getUser()?.id ?: 0

    LaunchedEffect(grupoId) {
        viewModel.cargarIntegrantes(grupoId)
    }

    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            viewModel.limpiarError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ParticipantesTopBar(
                grupoNombre = grupoNombre,
                totalIntegrantes = totalIntegrantes,
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (integrantes.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PeopleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay participantes",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(integrantes) { integrante ->
                        ParticipanteItem(
                            integrante = integrante,
                            isCurrentUser = integrante.usuario_id == currentUserId
                        )
                        if (integrante != integrantes.last()) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantesTopBar(
    grupoNombre: String,
    totalIntegrantes: Int,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = grupoNombre,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$totalIntegrantes participantes",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
fun ParticipanteItem(
    integrante: IntegranteGrupo,
    isCurrentUser: Boolean = false  // 👈 AGREGADO
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: Ver perfil */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar con inicial
        Surface(
            shape = CircleShape,
            color = when {
                isCurrentUser -> MaterialTheme.colorScheme.tertiaryContainer  // 👈 CAMBIADO
                integrante.es_creador -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier.size(48.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = integrante.nombre.firstOrNull()?.uppercase() ?: "?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isCurrentUser -> MaterialTheme.colorScheme.onTertiaryContainer  // 👈 CAMBIADO
                        integrante.es_creador -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Información del participante
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isCurrentUser) "Tú" else integrante.nombre_completo,  // 👈 CAMBIADO
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (integrante.es_creador) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "Creador",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rol del usuario
                Text(
                    text = when (integrante.rol) {
                        "admin" -> "Administrador"
                        "moderador" -> "Moderador"
                        else -> "Miembro"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Indicador de estado
                if (!integrante.activo) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• Inactivo",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Icono de rol
        if (integrante.rol == "admin" || integrante.es_creador) {
            Icon(
                imageVector = Icons.Default.AdminPanelSettings,
                contentDescription = "Admin",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else if (integrante.rol == "moderador") {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Moderador",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}