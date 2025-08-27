package com.example.recuerdago.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material3.CardColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ModulesSection(
    userId: String,
    primaryColor: Color,
    accentColor: Color,
    cardColors: CardColors,
    isDarkTheme: Boolean,
    onTabSelected: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Módulos Principales",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ModuleCard(
            icon = Icons.Default.DirectionsCar,
            title = "Rutas Alternas",
            description = "Encuentra las mejores rutas con IA y análisis de patrones de movilidad",
            iconColor = Color(0xFF4CAF50),
            cardColors = cardColors,
            isDarkTheme = isDarkTheme,
            onClick = { onTabSelected(1) }
        )

        ModuleCard(
            icon = Icons.Default.NotificationImportant,
            title = "Recordatorios",
            description = "Alertas inteligentes basadas en tu ubicación geográfica",
            iconColor = accentColor,
            cardColors = cardColors,
            isDarkTheme = isDarkTheme,
            onClick = { onTabSelected(2) }
        )

        ModuleCard(
            icon = Icons.Default.Groups,
            title = "Grupos Colaborativos",
            description = "Monitoreo compartido de recorridos con tu equipo",
            iconColor = Color(0xFF9C27B0),
            cardColors = cardColors,
            isDarkTheme = isDarkTheme,
            onClick = { onTabSelected(3) }
        )
    }
}