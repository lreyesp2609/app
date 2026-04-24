package com.example.app.screen.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.app.R
import com.example.app.models.User
import com.example.app.screen.components.AppButton
import com.example.app.screen.components.LanguageSelector

@Composable
fun SettingsScreen(
    userState: User?,
    onLogout: () -> Unit,
    onProfileUpdated: (String, String) -> Unit = { _, _ -> }
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Campos editables
    var nombre by remember(userState) { mutableStateOf(userState?.nombre ?: "") }
    var apellido by remember(userState) { mutableStateOf(userState?.apellido ?: "") }
    val correo = userState?.correo ?: ""

    // Detectar si hay cambios respecto al estado original
    val hasChanges = nombre.trim() != (userState?.nombre ?: "") ||
            apellido.trim() != (userState?.apellido ?: "")

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LanguageSelector(
                modifier = Modifier.align(Alignment.TopEnd),
                isDark = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Avatar con iniciales
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buildString {
                    if (nombre.isNotEmpty()) append(nombre.first().uppercaseChar())
                    if (apellido.isNotEmpty()) append(apellido.first().uppercaseChar())
                },
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${userState?.nombre ?: ""} ${userState?.apellido ?: ""}".trim(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Título sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.settings_personal_info),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Nombre
        OutlinedTextField(
            value = nombre,
            onValueChange = {
                nombre = it
                saveError = null
            },
            label = { Text(stringResource(R.string.settings_label_first_name)) },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Input Apellido
        OutlinedTextField(
            value = apellido,
            onValueChange = {
                apellido = it
                saveError = null
            },
            label = { Text(stringResource(R.string.settings_label_last_name)) },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Correo (solo lectura)
        OutlinedTextField(
            value = correo,
            onValueChange = {},
            label = { Text(stringResource(R.string.settings_label_email)) },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            },
            trailingIcon = {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = stringResource(R.string.settings_not_editable),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        // Error mensaje
        if (saveError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                saveError!!,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Botón guardar — solo aparece si hay cambios
        if (hasChanges) {
            val emptyFieldsError = stringResource(R.string.settings_error_empty_fields)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (nombre.isBlank() || apellido.isBlank()) {
                        saveError = emptyFieldsError
                        return@Button
                    }
                    isSaving = true
                    saveError = null
                    onProfileUpdated(nombre.trim(), apellido.trim())
                    isSaving = false
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isSaving && nombre.isNotBlank() && apellido.isNotBlank(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.settings_save_changes), fontWeight = FontWeight.Bold)
                }
            }

            // Botón descartar cambios
            TextButton(
                onClick = {
                    nombre = userState?.nombre ?: ""
                    apellido = userState?.apellido ?: ""
                    saveError = null
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    stringResource(R.string.settings_discard_changes),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Botón cerrar sesión
        AppButton(
            text = stringResource(R.string.settings_logout),
            onClick = { showLogoutDialog = true },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(50.dp),
            leadingIcon = {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = Color.White)
            },
            outlined = false,
            enabled = true
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Diálogo confirmación logout
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = {
                Icon(
                    Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text(stringResource(R.string.settings_logout_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.settings_logout_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text(stringResource(R.string.settings_logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
