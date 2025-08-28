package com.example.app.screen.rutas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.ui.theme.getBackgroundGradient
import com.example.app.viewmodel.UbicacionesViewModel

@Composable
fun AlternateRoutesScreen(
    token: String,
    isDarkTheme: Boolean,
    primaryColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val viewModel = remember { UbicacionesViewModel(token) }

    LaunchedEffect(Unit) {
        viewModel.cargarUbicaciones()
    }

    val ubicaciones = viewModel.ubicaciones
    val isLoading = viewModel.isLoading

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(getBackgroundGradient(isDarkTheme))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Directions,
                contentDescription = "Rutas Alternas",
                tint = primaryColor,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Módulo de Rutas Alternas",
                    color = textColor,
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Próximamente con integración de mapas y ML",
                    color = textColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón para agregar ubicación
        Button(
            onClick = { navController.navigate("mapa") },
            modifier = Modifier.fillMaxWidth(0.7f),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Agregar ubicación",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Agregar Ubicación", color = Color.White)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Lista de ubicaciones
        if (isLoading) {
            Text(text = "Cargando ubicaciones...", color = textColor)
        } else if (ubicaciones.isEmpty()) {
            Text(text = "No hay ubicaciones guardadas.", color = textColor.copy(alpha = 0.7f))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ubicaciones.size) { index ->
                    val ubicacion = ubicaciones[index]
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { println("Ubicación seleccionada: ${ubicacion.nombre}") },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkTheme) Color(0xFF2D2D44) else Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = ubicacion.nombre,
                                color = textColor,
                                fontSize = 16.sp,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ubicacion.direccion_completa,
                                color = textColor.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 🚀 Aquí pasamos el ID en vez de lat/lon
                                Button(
                                    onClick = {
                                        navController.navigate("rutas_screen/${ubicacion.id}")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Ver Ruta", color = Color.White)
                                }

                                Button(
                                    onClick = { println("Editar ${ubicacion.nombre}") },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(text = "Editar", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
