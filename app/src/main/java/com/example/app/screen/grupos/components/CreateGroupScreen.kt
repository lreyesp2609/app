package com.example.app.screen.grupos.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.app.models.GrupoCreate
import com.example.app.network.RetrofitClient
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.viewmodel.GrupoRepository
import com.example.app.viewmodel.GrupoState
import com.example.app.viewmodel.GrupoViewModel
import com.example.app.viewmodel.GrupoViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun CreateGroupScreen(
    token: String,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    // ViewModel
    val viewModel: GrupoViewModel = viewModel(
        factory = GrupoViewModelFactory(GrupoRepository(RetrofitClient.grupoService))
    )

    // Estados
    val grupoState by viewModel.grupoState.collectAsState()
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }

    // SnackbarHostState para manejar el Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    // Observar el estado del grupo
    LaunchedEffect(grupoState) {
        when (val state = grupoState) {
            is GrupoState.Success -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                delay(2000)
                navController.popBackStack()
            }
            is GrupoState.Error -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Long,
                    withDismissAction = true
                )
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = when {
                        data.visuals.message.contains("exitosamente", ignoreCase = true) ||
                                data.visuals.message.contains("éxito", ignoreCase = true) ||
                                data.visuals.message.contains("creado", ignoreCase = true) ->
                            MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    },
                    contentColor = when {
                        data.visuals.message.contains("exitosamente", ignoreCase = true) ||
                                data.visuals.message.contains("éxito", ignoreCase = true) ||
                                data.visuals.message.contains("creado", ignoreCase = true) ->
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onErrorContainer
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                // Botón de retroceso
                AppBackButton(navController = navController)

                Spacer(modifier = Modifier.height(16.dp))

                // Contenido animado
                AnimatedVisibility(
                    visible = showContent,
                    enter = fadeIn(animationSpec = tween(600)) +
                            slideInVertically(initialOffsetY = { it / 2 })
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.GroupAdd,
                                contentDescription = "Crear Grupo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Crear Grupo",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Ícono decorativo
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Formulario
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp)
                            ) {
                                // Campo: Nombre del grupo
                                Text(
                                    text = "Nombre del grupo",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = groupName,
                                    onValueChange = { groupName = it },
                                    placeholder = {
                                        Text(
                                            "Ej: Familia, Amigos, Trabajo...",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Campo: Descripción
                                Text(
                                    text = "Descripción (opcional)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = groupDescription,
                                    onValueChange = { groupDescription = it },
                                    placeholder = {
                                        Text(
                                            "Describe el propósito del grupo...",
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    ),
                                    maxLines = 4
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        // Botón crear
                        AppButton(
                            text = if (grupoState is GrupoState.Loading) "Creando..." else "Crear Grupo",
                            icon = Icons.Default.Check,
                            onClick = {
                                val grupoCreate = GrupoCreate(
                                    nombre = groupName.trim(),
                                    descripcion = groupDescription.trim().ifBlank { null }
                                )
                                viewModel.createGrupo(token, grupoCreate)
                            },
                            enabled = groupName.isNotBlank() && grupoState !is GrupoState.Loading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                        )

                        // Mostrar loading
                        if (grupoState is GrupoState.Loading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Información adicional
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Podrás invitar miembros después de crear el grupo",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}