package com.example.app.screen.rutas.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.screen.mapa.RutaMapa
import com.example.app.ui.theme.getBackgroundGradient

@Composable
fun RutasScreen(
    primaryColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onButton1Click: () -> Unit = {},
    onButton2Click: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(getBackgroundGradient(isDarkTheme))
    ) {
        RutaMapa(
            modifier = Modifier.fillMaxSize()
        )

        // Botones en la parte inferior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onButton1Click,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Botón 1", color = Color.White)
            }

            Button(
                onClick = onButton2Click,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Botón 2", color = Color.White)
            }
        }
    }
}
