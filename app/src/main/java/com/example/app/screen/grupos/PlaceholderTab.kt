package com.example.app.screen.grupos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.app.screen.components.AppButton
import kotlinx.coroutines.delay

@Composable
fun CollaborativeGroupsScreen(
    token: String,
    modifier: Modifier = Modifier,
    navController: NavController
) {
    var showContent by remember { mutableStateOf(false) }

    // Animación de entrada
    LaunchedEffect(Unit) {
        delay(200)
        showContent = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header animado
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(600)) +
                        slideInVertically(initialOffsetY = { -it })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Grupos",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Grupos",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botón principal
                    AppButton(
                        text = "Crear grupo",
                        icon = Icons.Default.GroupAdd,
                        onClick = { navController.navigate("create_group") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }

            // Contenido principal
            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 300)) +
                        scaleIn(initialScale = 0.9f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Ilustración estilo Recordatorios
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Grupos",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Grupos Colaborativos",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Próximamente podrás crear y unirte a grupos",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }
}