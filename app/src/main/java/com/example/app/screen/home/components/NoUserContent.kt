package com.example.recuerdago.screens.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.app.viewmodel.AuthViewModel

@Composable
fun NoUserContent(authViewModel: AuthViewModel, primaryColor: Color, textColor: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "No se pudo cargar el usuario",
            fontSize = 18.sp,
            color = textColor
        )
        Button(
            onClick = { authViewModel.getCurrentUser() },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Text("Cargar usuario", color = Color.White)
        }
    }
}