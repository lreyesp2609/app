package com.example.app.screen.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.screen.components.AppBackButton
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppTextField
import com.example.app.ui.theme.getBackgroundGradient
import com.example.app.viewmodel.AuthViewModel
import com.example.app.viewmodel.NotificationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasenia by remember { mutableStateOf("") }
    var confirmarContrasenia by remember { mutableStateOf("") }

    // Estados para notificaciones
    var showErrorNotification by remember { mutableStateOf(false) }
    var errorNotificationMessage by remember { mutableStateOf("") }
    var showSuccessNotification by remember { mutableStateOf(false) }
    var successNotificationMessage by remember { mutableStateOf("") }

    // Box principal para superponer notificaciones
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 🆕 Contenido principal con scroll que se activa cuando aparece el teclado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(getBackgroundGradient())
                .imePadding() // 🔑 Esto ajusta el padding cuando aparece el teclado
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()) // 🆕 Scroll habilitado para cuando hay teclado
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // 🆕 Espaciado uniforme
        ) {
            // Sección superior: Botón de volver + Logo + Títulos
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Botón de volver
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppBackButton(navController = navController)
                }

                // Logo
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.TopEnd) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Ubicación",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize()
                    )
                    Icon(
                        imageVector = Icons.Default.AccessAlarm,
                        contentDescription = "Alarma",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier
                            .size(28.dp)
                            .offset(x = (-6).dp, y = 6.dp)
                    )
                }

                // Títulos
                Text(
                    text = "RecuerdaGo",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Crea tu cuenta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Completa la información para comenzar",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }

            // Formulario con espaciado uniforme
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Nombre y Apellido en fila
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = "Nombre",
                        placeholder = "Tu nombre",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        enabled = !authViewModel.isLoading,
                        modifier = Modifier.weight(1f)
                    )

                    AppTextField(
                        value = apellido,
                        onValueChange = { apellido = it },
                        label = "Apellido",
                        placeholder = "Tu apellido",
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        enabled = !authViewModel.isLoading,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Correo electrónico
                AppTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    label = "Correo electrónico",
                    placeholder = "tu@email.com",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !authViewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Contraseña
                AppTextField(
                    value = contrasenia,
                    onValueChange = { contrasenia = it },
                    label = "Contraseña",
                    placeholder = "Mínimo 6 caracteres",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    enabled = !authViewModel.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirmar contraseña
                AppTextField(
                    value = confirmarContrasenia,
                    onValueChange = { confirmarContrasenia = it },
                    label = "Confirmar contraseña",
                    placeholder = "Repite tu contraseña",
                    leadingIcon = {
                        Icon(
                            Icons.Default.LockReset,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    isPassword = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    enabled = !authViewModel.isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (contrasenia == confirmarContrasenia && confirmarContrasenia.isNotEmpty())
                        Color.Green else MaterialTheme.colorScheme.primary
                )

                // Mensaje de error para contraseñas
                AnimatedVisibility(
                    visible = confirmarContrasenia.isNotEmpty() && contrasenia != confirmarContrasenia,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFF5722).copy(alpha = 0.1f)
                        ),
                        border = BorderStroke(1.dp, Color(0xFFFF5722).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFF5722),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Las contraseñas no coinciden",
                                color = Color(0xFFFF5722),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 🆕 Spacer para empujar el botón hacia abajo cuando no hay teclado
            Spacer(modifier = Modifier.height(24.dp))

            // Botón de registro
            AppButton(
                text = if (authViewModel.isLoading) "Creando cuenta..." else "Crear cuenta",
                isLoading = authViewModel.isLoading,
                onClick = {
                    when {
                        nombre.isBlank() || apellido.isBlank() || correo.isBlank() ||
                                contrasenia.isBlank() || confirmarContrasenia.isBlank() -> {
                            notificationViewModel.showError("Completa todos los campos")
                        }
                        contrasenia != confirmarContrasenia -> {
                            notificationViewModel.showError("Las contraseñas no coinciden")
                        }
                        contrasenia.length < 6 -> {
                            notificationViewModel.showError("La contraseña debe tener al menos 6 caracteres")
                        }
                        else -> {
                            authViewModel.registerUser(
                                nombre,
                                apellido,
                                correo,
                                contrasenia
                            ) { registroExitoso ->
                                if (registroExitoso) {
                                    notificationViewModel.showSuccess("¡Cuenta creada! Iniciando sesión...")

                                    authViewModel.login(correo, contrasenia) {
                                        navController.navigate("home?skipPermissions=true") {
                                            popUpTo("login") { inclusive = true }
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            )

            // 🆕 Spacer adicional para asegurar que el botón sea siempre visible
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Notificaciones superpuestas
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            // Notificación de error
            AnimatedVisibility(
                visible = showErrorNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF5722).copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = errorNotificationMessage,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { showErrorNotification = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Notificación de éxito
            AnimatedVisibility(
                visible = showSuccessNotification,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF4CAF50).copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = successNotificationMessage,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = { showSuccessNotification = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(authViewModel.errorMessage) {
        authViewModel.errorMessage?.let { error ->
            errorNotificationMessage = error
            showErrorNotification = true
            authViewModel.clearError()
        }
    }

    LaunchedEffect(showErrorNotification) {
        if (showErrorNotification) {
            delay(4000)
            showErrorNotification = false
        }
    }

    LaunchedEffect(showSuccessNotification) {
        if (showSuccessNotification) {
            delay(3000)
            showSuccessNotification = false
        }
    }
}