package com.example.app.screen.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope

enum class SnackbarType {
    SUCCESS,    // Verde - Operación exitosa
    ERROR,      // Rojo - Error
    WARNING,    // Amarillo/Naranja - Advertencia
    INFO        // Azul - Información
}

/**
 * Host global para Snackbars personalizados
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.navigationBarsPadding()
    ) { data ->
        AppSnackbar(snackbarData = data)
    }
}

/**
 * Snackbar personalizado con estilos
 */
@Composable
fun AppSnackbar(
    snackbarData: SnackbarData
) {
    val message = snackbarData.visuals.message
    val type = detectSnackbarType(message)

    val (containerColor, contentColor, icon) = when (type) {
        SnackbarType.SUCCESS -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            Icons.Default.CheckCircle
        )
        SnackbarType.ERROR -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            Icons.Default.Error
        )
        SnackbarType.WARNING -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            Icons.Default.Warning
        )
        SnackbarType.INFO -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            Icons.Default.Info
        )
    }

    Snackbar(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = containerColor,
        contentColor = contentColor,
        dismissAction = if (snackbarData.visuals.withDismissAction) {
            {
                IconButton(
                    onClick = { snackbarData.dismiss() },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = contentColor
            )
            Text(
                text = message,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Detecta automáticamente el tipo de Snackbar basado en el mensaje
 */
private fun detectSnackbarType(message: String): SnackbarType {
    return when {
        message.contains("exitosamente", ignoreCase = true) ||
                message.contains("éxito", ignoreCase = true) ||
                message.contains("creado", ignoreCase = true) ||
                message.contains("guardado", ignoreCase = true) ||
                message.contains("actualizado", ignoreCase = true) ||
                message.contains("eliminado", ignoreCase = true) ||
                message.contains("completado", ignoreCase = true) -> SnackbarType.SUCCESS

        message.contains("error", ignoreCase = true) ||
                message.contains("falló", ignoreCase = true) ||
                message.contains("fallo", ignoreCase = true) ||
                message.contains("incorrecto", ignoreCase = true) ||
                message.contains("inválido", ignoreCase = true) -> SnackbarType.ERROR

        message.contains("advertencia", ignoreCase = true) ||
                message.contains("atención", ignoreCase = true) ||
                message.contains("cuidado", ignoreCase = true) -> SnackbarType.WARNING

        else -> SnackbarType.INFO
    }
}

/**
 * Funciones de extensión para mostrar Snackbars fácilmente
 */
suspend fun SnackbarHostState.showSuccessSnackbar(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    showSnackbar(
        message = message,
        duration = duration
    )
}

suspend fun SnackbarHostState.showErrorSnackbar(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Long,
    withDismissAction: Boolean = true
) {
    showSnackbar(
        message = message,
        duration = duration,
        withDismissAction = withDismissAction
    )
}

suspend fun SnackbarHostState.showWarningSnackbar(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Long
) {
    showSnackbar(
        message = message,
        duration = duration
    )
}

suspend fun SnackbarHostState.showInfoSnackbar(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    showSnackbar(
        message = message,
        duration = duration
    )
}

/**
 * Función helper para mostrar Snackbars desde composables
 */
@Composable
fun rememberAppSnackbarState(): Pair<SnackbarHostState, CoroutineScope> {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    return snackbarHostState to scope
}
