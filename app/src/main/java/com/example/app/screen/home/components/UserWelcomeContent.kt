package com.example.recuerdago.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun UserWelcomeContent(
    userState: Any,
    textColor: Color,
    primaryColor: Color,
    cardColors: CardColors
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = cardColors,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "¡Bienvenido!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )

            val userName = try {
                val nombreField = userState::class.java.getDeclaredField("nombre")
                val apellidoField = userState::class.java.getDeclaredField("apellido")
                nombreField.isAccessible = true
                apellidoField.isAccessible = true
                val nombre = nombreField.get(userState) as? String ?: ""
                val apellido = apellidoField.get(userState) as? String ?: ""
                "$nombre $apellido"
            } catch (e: Exception) {
                "Usuario"
            }

            Text(
                text = userName,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = primaryColor.copy(alpha = 0.3f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val userId = try {
                    val idField = userState::class.java.getDeclaredField("id")
                    idField.isAccessible = true
                    idField.get(userState).toString()
                } catch (e: Exception) {
                    "N/A"
                }

                InfoChip(
                    icon = Icons.Default.Badge,
                    text = "ID: $userId",
                    primaryColor = primaryColor
                )

                val isActive = try {
                    val activoField = userState::class.java.getDeclaredField("activo")
                    activoField.isAccessible = true
                    activoField.get(userState) as? Boolean ?: false
                } catch (e: Exception) {
                    false
                }

                InfoChip(
                    icon = Icons.Default.CheckCircle,
                    text = if (isActive) "Activo" else "Inactivo",
                    primaryColor = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }

            val userRol = try {
                val rolField = userState::class.java.getDeclaredField("rol")
                rolField.isAccessible = true
                rolField.get(userState) as? String ?: "N/A"
            } catch (e: Exception) {
                "N/A"
            }

            InfoChip(
                icon = Icons.Default.Person,
                text = "Rol: $userRol",
                primaryColor = primaryColor
            )
        }
    }
}