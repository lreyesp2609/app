package com.rutai.app.screen.components

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.LocaleListCompat

@Composable
fun LanguageSelector(
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Detectar el idioma actual (ya sea el configurado manualmente o el del sistema)
    val configuration = LocalConfiguration.current
    val currentLocale = configuration.locales[0].language

    Box(modifier = modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Cambiar idioma",
                tint = if (isDark) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.primary
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Español") },
                onClick = {
                    expanded = false
                    changeLanguage("es")
                },
                leadingIcon = {
                    RadioButton(selected = currentLocale == "es", onClick = null)
                }
            )
            DropdownMenuItem(
                text = { Text("English") },
                onClick = {
                    expanded = false
                    changeLanguage("en")
                },
                leadingIcon = {
                    RadioButton(selected = currentLocale == "en", onClick = null)
                }
            )
        }
    }
}

private fun changeLanguage(languageCode: String) {
    val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
    AppCompatDelegate.setApplicationLocales(appLocale)
}
