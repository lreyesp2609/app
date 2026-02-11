package com.example.app.screen.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.AppTextField
import com.example.app.ui.theme.getBackgroundGradient
import com.example.app.viewmodel.AuthViewModel
import com.example.app.viewmodel.NotificationViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    notificationViewModel: NotificationViewModel
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Observa el usuario
    LaunchedEffect(authViewModel.user) {
        if (authViewModel.user != null) {
            navController.navigate("home") {
                popUpTo("login") { inclusive = true }
            }
        }
    }

    // 🆕 Observar errores del AuthViewModel y mostrarlos con notificaciones
    LaunchedEffect(authViewModel.errorMessage) {
        authViewModel.errorMessage?.let { error ->
            notificationViewModel.showError(error)
            authViewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(getBackgroundGradient())
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo sin caja - solo los íconos
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.TopEnd) {
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
                    .size(36.dp)
                    .offset(x = (-10).dp, y = 10.dp)
            )
        }

        Text(
            text = "RememberGo",
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Inicia sesión para continuar",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Campo de correo electrónico
        AppTextField(
            value = email,
            onValueChange = { email = it },
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

        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña",
            placeholder = "Tu contraseña",
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
                imeAction = ImeAction.Done
            ),
            enabled = !authViewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        AppButton(
            text = if (authViewModel.isLoading) "Iniciando sesión..." else "Iniciar sesión",
            isLoading = authViewModel.isLoading,
            onClick = {
                when {
                    email.isBlank() && password.isBlank() -> {
                        notificationViewModel.showError("Por favor completa todos los campos")
                    }
                    email.isBlank() -> {
                        notificationViewModel.showError("Ingresa tu correo electrónico")
                    }
                    password.isBlank() -> {
                        notificationViewModel.showError("Ingresa tu contraseña")
                    }
                    else -> {
                        authViewModel.login(email, password) { loginExitoso ->
                            if (loginExitoso) {
                                notificationViewModel.showSuccess("¡Bienvenido de nuevo!")
                            }
                        }
                    }
                }
            }
        )


        Spacer(modifier = Modifier.weight(1f))

        AppButton(
            text = "Crear cuenta nueva",
            icon = Icons.Default.PersonAdd,
            outlined = true,
            onClick = { navController.navigate("register") },
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}