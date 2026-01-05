package com.example.app.screen.grupos.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import com.example.app.models.GrupoCreate
import com.example.app.network.RetrofitClient
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.repository.GrupoRepository
import com.example.app.screen.components.AppSnackbarHost
import com.example.app.screen.components.AppTextField
import com.example.app.screen.components.rememberAppSnackbarState
import com.example.app.screen.components.showErrorSnackbar
import com.example.app.screen.components.showSuccessSnackbar
import com.example.app.viewmodel.GrupoState
import com.example.app.viewmodel.GrupoViewModel
import com.example.app.viewmodel.GrupoViewModelFactory
import com.example.app.viewmodel.NotificationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    token: String,
    modifier: Modifier = Modifier,
    navController: NavController,
    notificationViewModel: NotificationViewModel // 🔥 SIN valor por defecto
) {
    val viewModel: GrupoViewModel = viewModel(
        factory = GrupoViewModelFactory(GrupoRepository(RetrofitClient.grupoService))
    )

    val grupoState by viewModel.grupoState.collectAsState()
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var showContent by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    // 🔥 NAVEGACIÓN INMEDIATA - Notificación se muestra en destino
    LaunchedEffect(grupoState) {
        when (val state = grupoState) {
            is GrupoState.Success -> {
                // ✅ Guardar mensaje para mostrarlo en la pantalla anterior
                navController.previousBackStackEntry
                    ?.savedStateHandle?.apply {
                        set("grupo_creado", true)
                        set("grupo_mensaje", state.message)
                    }

                // Navegar inmediatamente
                navController.popBackStack()

                // Reset después de navegar
                viewModel.resetState()
            }
            is GrupoState.Error -> {
                // ⚠️ Los errores SÍ se muestran aquí porque el usuario sigue en la pantalla
                notificationViewModel.showError(state.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        @Suppress("UNUSED_EXPRESSION")
        paddingValues

        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppBackButton(navController = navController)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear Grupo",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Ícono decorativo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.CenterHorizontally)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Formulario
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Nombre del grupo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = "Nombre",
                        placeholder = "Ej: Familia, Amigos, Trabajo...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Descripción (opcional)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    AppTextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        label = "Descripción",
                        placeholder = "Describe el propósito del grupo...",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4
                    )
                }
            }

            // Botón crear
            AppButton(
                text = if (grupoState is GrupoState.Loading) "Creando..." else "Crear Grupo",
                icon = Icons.Default.Check,
                onClick = {
                    when {
                        groupName.isBlank() -> {
                            notificationViewModel.showError("El nombre del grupo no puede estar vacío")
                        }
                        grupoState is GrupoState.Loading -> {
                            notificationViewModel.showWarning("Espera, creando grupo...")
                        }
                        else -> {
                            val grupoCreate = GrupoCreate(
                                nombre = groupName.trim(),
                                descripcion = groupDescription.trim().ifBlank { null }
                            )
                            viewModel.createGrupo(token, grupoCreate)
                        }
                    }
                },
                enabled = groupName.isNotBlank() && grupoState !is GrupoState.Loading,
                isLoading = grupoState is GrupoState.Loading,
                modifier = Modifier.fillMaxWidth()
            )

            // Información adicional
            Card(
                modifier = Modifier.fillMaxWidth(),
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