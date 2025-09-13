package com.example.app.screen.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recuerdago.screens.tabs.ModuleCard

@Composable
fun ModulesSection(
    userId: String,
    accentColor: Color,
    onTabSelected: (Int) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            Text(
                text = "Módulos Disponibles",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        item {
            ModuleCard(
                title = "Rutas Alternas",
                description = "Gestiona tus rutas y ubicaciones",
                icon = Icons.Default.Map,
                onClick = { onTabSelected(1) }
            )
        }

        item {
            ModuleCard(
                title = "Recordatorios",
                description = "Configura alertas personalizadas",
                icon = Icons.Default.Notifications,
                onClick = { onTabSelected(2) }
            )
        }

        item {
            ModuleCard(
                title = "Grupos Colaborativos",
                description = "Colabora con otros usuarios",
                icon = Icons.Default.Group,
                onClick = { onTabSelected(3) }
            )
        }
    }
}